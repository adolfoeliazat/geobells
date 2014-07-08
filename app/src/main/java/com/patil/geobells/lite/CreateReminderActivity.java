package com.patil.geobells.lite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.patil.geobells.lite.asynctask.PlacesAPIAsyncTask;
import com.patil.geobells.lite.asynctask.PlacesAsyncTaskCompleteListener;
import com.patil.geobells.lite.data.Place;
import com.patil.geobells.lite.data.Reminder;
import com.patil.geobells.lite.utils.Config;
import com.patil.geobells.lite.utils.Constants;
import com.patil.geobells.lite.utils.GeobellsDataManager;
import com.patil.geobells.lite.utils.GeobellsPreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class CreateReminderActivity extends Activity implements AdapterView.OnItemSelectedListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, PlacesAsyncTaskCompleteListener<String> {

    EditText titleBox;
    RadioButton specificRadioButton;
    RadioButton dynamicRadioButton;
    AutoCompleteTextView businessBox;
    AutoCompleteTextView addressBox;
    RadioButton enterRadioButton;
    RadioButton exitRadioButton;
    CheckBox repeatCheckBox;
    CheckBox airplaneCheckBox;
    CheckBox silenceCheckBox;
    TextView proximityPrompt;
    RelativeLayout specificLayout;
    RelativeLayout dynamicLayout;
    RelativeLayout advancedLayout;
    Button advancedButton;
    Spinner proximitySpinner;

    GeobellsPreferenceManager preferenceManager;
    GeobellsDataManager dataManager;


    // Generic Reminder data
    String title;
    boolean completed;
    boolean repeat;
    boolean[] days = new boolean[7];
    int proximity;
    boolean toggleAirplane;
    boolean silencePhone;
    long timeCreated;
    long timeCompleted;
    int transition;

    // Fixed reminder
    String address;
    double latitude;
    double longitude;

    // Dynamic reminder
    String business;
    ArrayList<Place> places;

    LocationClient locationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_reminder);
        setupViews();
        for(int i = 0; i < 7; i++) {
            days[i] = true;
        }
        locationClient = new LocationClient(this, this, this);
        preferenceManager = new GeobellsPreferenceManager(this);
        dataManager = new GeobellsDataManager(this);
        setupSpinner();
        setupAutocomplete();
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationClient.connect();
    }

    @Override
    protected void onStop() {
        locationClient.disconnect();
        super.onStop();
    }

    public void setupSpinner() {
        ArrayAdapter<CharSequence> adapter;
        if(preferenceManager.isMetricEnabled()) {
            adapter = ArrayAdapter.createFromResource(this,
                    R.array.spinner_proximity_metric, android.R.layout.simple_spinner_dropdown_item);
        } else {
            adapter = ArrayAdapter.createFromResource(this, R.array.spinner_proximity, android.R.layout.simple_spinner_dropdown_item);
        }
        proximitySpinner.setAdapter(adapter);
        proximitySpinner.setSelection(Constants.PROXIMITY_DISTANCES_DEFAULT_INDEX);
        proximity = Constants.PROXIMITY_DISTANCES[Constants.PROXIMITY_DISTANCES_DEFAULT_INDEX];
    }

    public void setupAutocomplete() {
        addressBox.setAdapter(new PlacesAutoCompleteAddressAdapter(this, R.layout.list_item_autocomplete));
        addressBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String str = (String) adapterView.getItemAtPosition(position);
            }
        });
        if(Config.AUTOCOMPLETE_BUSINESSES) {
            businessBox.setAdapter(new PlacesAutoCompleteBusinessAdapter(this, R.layout.list_item_autocomplete));
            businessBox.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                    String str = (String) adapterView.getItemAtPosition(position);
                }
            });
        }
    }

    public void setupViews() {
        titleBox = (EditText) findViewById(R.id.reminder_title);
        specificRadioButton = (RadioButton) findViewById(R.id.radiobutton_reminder_type_specific);
        dynamicRadioButton = (RadioButton) findViewById(R.id.radiobutton_reminder_type_dynamic);
        businessBox = (AutoCompleteTextView) findViewById(R.id.reminder_business);
        addressBox = (AutoCompleteTextView) findViewById(R.id.reminder_address);
        enterRadioButton = (RadioButton) findViewById(R.id.radiobutton_reminder_transition_enter);
        exitRadioButton = (RadioButton) findViewById(R.id.radiobutton_reminder_transition_exit);
        proximitySpinner = (Spinner) findViewById(R.id.spinner_reminder_proximity);
        repeatCheckBox = (CheckBox) findViewById(R.id.checkbox_reminder_repeat);
        airplaneCheckBox = (CheckBox) findViewById(R.id.checkbox_reminder_airplane);
        silenceCheckBox = (CheckBox) findViewById(R.id.checkbox_reminder_silence);
        proximityPrompt = (TextView) findViewById(R.id.prompt_reminder_proximity);
        specificLayout = (RelativeLayout) findViewById(R.id.layout_reminder_specific);
        dynamicLayout = (RelativeLayout) findViewById(R.id.layout_reminder_dynamic);
        advancedLayout = (RelativeLayout) findViewById(R.id.layout_advanced_options);
        advancedButton = (Button) findViewById(R.id.button_advanced_options);
    }

    public void onTypeSpecificClick(View v) {
        dynamicLayout.setVisibility(View.GONE);
        specificLayout.setVisibility(View.VISIBLE);
        advancedButton.setVisibility(View.VISIBLE);
    }

    public void onTypeDynamicClick(View v) {
        specificLayout.setVisibility(View.GONE);
        dynamicLayout.setVisibility(View.VISIBLE);
        advancedButton.setVisibility(View.VISIBLE);
    }

    public void onAdvancedOptionsClick(View v) {
        if(advancedLayout.getVisibility() == View.VISIBLE) {
            advancedLayout.setVisibility(View.GONE);
        } else {
            advancedLayout.setVisibility(View.VISIBLE);
        }
    }

    public void onChooseDaysClick(View v) {
        final CharSequence[] displayDays = {getString(R.string.sunday), getString(R.string.monday), getString(R.string.tuesday), getString(R.string.wednesday), getString(R.string.thursday), getString(R.string.friday), getString(R.string.saturday)};
        // arraylist to keep the selected items
        final boolean[] selectedItems = days;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.prompt_reminder_days));
        builder.setMultiChoiceItems(displayDays, selectedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    // indexSelected contains the index of item (of which checkbox checked)
                    @Override
                    public void onClick(DialogInterface dialog, int indexSelected,
                                        boolean isChecked) {
                        selectedItems[indexSelected] = isChecked;
                    }
                })
                // Set the action buttons
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        CreateReminderActivity.this.days = selectedItems;
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });

        builder.create().show();
    }

    public void onBusinessViewPlacesClick(View v) {

    }


    public void onAddressSearchClick(View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.dialog_title_address_search));

        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Search", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                Location lastLocation = locationClient.getLastLocation();
                new PlacesAPIAsyncTask(CreateReminderActivity.this, CreateReminderActivity.this).execute(value, String.valueOf(lastLocation.getLatitude()), String.valueOf(lastLocation.getLongitude()));
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        alert.show();
    }

    public void onAddressMapClick(View v) {

    }

    // TODO
    public boolean isNecessaryFieldsCompleted() {
        if(titleBox.getText().toString() != null && titleBox.getText().toString().length() > 0) {
            if(enterRadioButton.isChecked() || exitRadioButton.isChecked()) {
                if (specificRadioButton.isChecked()) {
                    if (addressBox.getText().toString() != null && addressBox.getText().toString().length() > 0) {
                        return true;
                    }
                } else if (dynamicRadioButton.isChecked()) {
                    if (businessBox.getText().toString() != null && businessBox.getText().toString().length() > 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // TODO
    public double[] getCoordsFromAddress(String address) {
        return new double[] {0, 0};
    }

    // TODO
    public ArrayList<Place> getPlacesFromBusiness(String business) {
        Place place = new Place();
        ArrayList<Place> places = new ArrayList<Place>();
        places.add(place);
        return places;
    }

    public void createReminder() {
        if(isNecessaryFieldsCompleted()) {
            title = titleBox.getText().toString();
            completed = false;
            repeat = repeatCheckBox.isChecked();
            // days is already set through dialog
            // proximity is already set through dialog
            toggleAirplane = airplaneCheckBox.isChecked();
            silencePhone = silenceCheckBox.isChecked();
            timeCreated = System.currentTimeMillis();
            timeCompleted = Constants.TIME_COMPLETED_DEFAULT;
            if (enterRadioButton.isChecked()) {
                transition = Constants.TRANSITION_ENTER;
            } else {
                transition = Constants.TRANSITION_EXIT;
            }

            Log.d("GeobellsCards", "Saving reminder with title " + title);
            ArrayList<Reminder> reminders = dataManager.getSavedReminders();
            Reminder reminder = new Reminder();
            reminder.title = title;
            reminder.completed = completed;
            reminder.repeat = repeat;
            reminder.days = days;
            reminder.proximity = proximity;
            reminder.toggleAirplane = toggleAirplane;
            reminder.silencePhone = silencePhone;
            reminder.timeCreated = timeCreated;
            reminder.timeCompleted = timeCompleted;
            reminder.transition = transition;
            if (specificRadioButton.isChecked()) {
                address = addressBox.getText().toString();
                double[] coords = getCoordsFromAddress(address);
                latitude = coords[0];
                longitude = coords[1];
                reminder.type = Constants.TYPE_FIXED;
                reminder.address = address;
                reminder.latitude = latitude;
                reminder.longitude = longitude;
            } else if (dynamicRadioButton.isChecked()) {
                business = businessBox.getText().toString();
                places = getPlacesFromBusiness(business);
                reminder.type = Constants.TYPE_DYNAMIC;
                reminder.business = business;
                reminder.places = places;
            }
            reminders.add(reminder);
            dataManager.saveReminders(reminders);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.prompt_fill_info), Toast.LENGTH_SHORT).show();
        }
    }


    public ArrayList<String> autocomplete(String input, String type) {
        ArrayList<String> resultList = null;
        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            Location currentLocation = locationClient.getLastLocation();
            StringBuilder sb = new StringBuilder(Constants.PLACES_API_BASE + Constants.PLACES_TYPE_AUTOCOMPLETE + Constants.PLACES_OUT_JSON);
            sb.append("?key=" + Constants.PLACES_API_KEY);
            sb.append("&location=" +  currentLocation.getLatitude() + "," + currentLocation.getLongitude());
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));
            sb.append("&type=" + type);

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e("Autocomplete", "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e("Autocomplete", "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList<String>(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e("Autocomplete", "Cannot process JSON results", e);
        }

        return resultList;
    }

    @Override
    public void onTaskComplete(ArrayList<Place> places) {
        createAddressPlaceSearchDialog(places);
    }

    private class PlacesAutoCompleteAddressAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public PlacesAutoCompleteAddressAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString(), Constants.PLACES_AUTOCOMPLETE_TYPE_ADDRESS);

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }
                }};
            return filter;
        }
    }

    private class PlacesAutoCompleteBusinessAdapter extends ArrayAdapter<String> implements Filterable {
        private ArrayList<String> resultList;

        public PlacesAutoCompleteBusinessAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString(), Constants.PLACES_AUTOCOMPLETE_TYPE_BUSINESS);

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    }
                    else {
                        notifyDataSetInvalidated();
                    }
                }};
            return filter;
        }
    }


    public void createAddressPlaceSearchDialog(ArrayList<Place> places) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_title_results));

        ListView modeList = new ListView(this);

        final String[] stringArray = new String[places.size()];
        for(int i = 0; i < places.size(); i++) {
            stringArray[i] = places.get(i).title + " - " + places.get(i).address;
        }
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
        modeList.setAdapter(modeAdapter);

        builder.setView(modeList);
        final Dialog dialog = builder.create();

        modeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                addressBox.setText(stringArray[position]);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.create_reminder, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_done:
                createReminder();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
        proximity = Constants.PROXIMITY_DISTANCES[pos];
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        proximity = Constants.PROXIMITY_DISTANCES[Constants.PROXIMITY_DISTANCES_DEFAULT_INDEX];
    }


    @Override
    public void onConnected(Bundle dataBundle) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
