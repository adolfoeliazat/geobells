package com.patil.geobells.lite;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.patil.geobells.lite.R;
import com.patil.geobells.lite.data.Place;
import com.patil.geobells.lite.data.Reminder;
import com.patil.geobells.lite.utils.Constants;
import com.patil.geobells.lite.utils.GeobellsDataManager;

import java.util.ArrayList;

public class ViewRemindersMapActivity extends Activity {

    GeobellsDataManager dataManager;
    GoogleMap mapView;
    LinearLayout keyLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reminders_map);
        dataManager = new GeobellsDataManager(this);
        ArrayList<Reminder> reminders = dataManager.getSavedReminders();
        mapView = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        keyLayout = (LinearLayout) findViewById(R.id.layout_key);
        final ArrayList<ArrayList<Marker>> markersList = new ArrayList<ArrayList<Marker>>();
        final ArrayList<ArrayList<Circle>> circlesList = new ArrayList<ArrayList<Circle>>();
        int numMarkers = 0;
        for(int i = 0; i < reminders.size(); i++) {
            final int index = i;
            Reminder reminder = reminders.get(i);
            float hue = indexToHue(i, reminders.size());
            ArrayList<Marker> markers = new ArrayList<Marker>();
            ArrayList<Circle> circles = new ArrayList<Circle>();
            if(!reminder.completed) {
                if(reminder.type == Constants.TYPE_FIXED) {
                    LatLng markerPosition = new LatLng(reminder.latitude, reminder.longitude);
                    Marker marker = mapView.addMarker(new MarkerOptions().title(reminder.title).snippet(reminder.address).position(markerPosition).icon(BitmapDescriptorFactory.defaultMarker(hue)));
                    markers.add(marker);
                    CircleOptions circleOptions = new CircleOptions().center(new LatLng(reminder.latitude, reminder.longitude)).radius(reminder.proximity);
                    Circle circle = mapView.addCircle(circleOptions);
                    circle.setStrokeWidth(2);
                    circle.setStrokeColor(Color.HSVToColor(130, new float[] {hue, 1, 1}));
                    circle.setFillColor(Color.HSVToColor(130, new float[] {hue, 1, 1}));
                    circles.add(circle);
                    numMarkers++;
                } else {
                    for(Place place : reminder.places) {
                        LatLng markerPosition = new LatLng(place.latitude, place.longitude);
                        Marker marker = mapView.addMarker(new MarkerOptions().title(reminder.title).snippet(reminder.business).position(markerPosition).icon(BitmapDescriptorFactory.defaultMarker(hue)));
                        markers.add(marker);

                        CircleOptions circleOptions = new CircleOptions().center(new LatLng(place.latitude, place.longitude)).radius(reminder.proximity);
                        Circle circle = mapView.addCircle(circleOptions);
                        circle.setStrokeWidth(2);
                        circle.setStrokeColor(Color.HSVToColor(130, new float[] {hue, 1, 1}));
                        circle.setFillColor(Color.HSVToColor(130, new float[] {hue, 1, 1}));
                        numMarkers++;
                        circles.add(circle);
                    }
                }
                markersList.add(markers);
                circlesList.add(circles);
                View keyView = getLayoutInflater().inflate(R.layout.map_reminder_key, null);
                CheckBox checkBox = (CheckBox) keyView.findViewById(R.id.key_checkbox);
                checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ArrayList<Circle> circlesies = circlesList.get(index);
                        ArrayList<Marker> markersies = markersList.get(index);
                        for(Marker marker : markersies) {
                            if(marker.isVisible()) {
                                marker.setVisible(false);
                            } else {
                                marker.setVisible(true);
                            }
                        }
                        for(Circle circle : circlesies) {
                            if(circle.isVisible()) {
                                circle.setVisible(false);
                            } else {
                                circle.setVisible(true);
                            }
                        }
                    }
                });
                checkBox.setText(reminder.title);
                ImageView colorBox = (ImageView) keyView.findViewById(R.id.view_colorbox);
                colorBox.setBackgroundColor(Color.HSVToColor(new float[] {hue, 1, 1}));
                keyLayout.addView(keyView);
            }
        }
        final int finalNumMarkers = numMarkers;
        final ArrayList<ArrayList<Marker>> finalMarkers = markersList;
        mapView.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                if(finalNumMarkers == 0) {

                } else if(finalNumMarkers == 1) {
                    mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(finalMarkers.get(0).get(0).getPosition().latitude, finalMarkers.get(0).get(0).getPosition().longitude), 11));
                } else {
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (ArrayList<Marker> markers : finalMarkers) {
                        for(Marker marker : markers) {
                            builder.include(marker.getPosition());
                        }
                    }
                    LatLngBounds bounds = builder.build();
                    int padding = 30; // offset from edges of the map in pixels
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mapView.moveCamera(cu);
                }
            }
        });
    }

    public float indexToHue(int index, int total) {
        double ratio = (double)index / total;
        return (float) ratio * 360;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.view_reminders_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}