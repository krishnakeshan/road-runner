package com.qrilt.roadrunner;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    public static int PLACE_PICKER_REQUEST_CODE = 1;

    private SensorManager sensorManager;
    private Sensor sensor;
    private GoogleMap mMap;
    private Place destination;

    ArrayList<ParseObject> potholes = new ArrayList<>();
    boolean collecting = false;
    long startingTime = Calendar.getInstance().getTimeInMillis();
    float startingValue = 0f, endingValue = 0f;

    //views
    @BindView(R.id.activity_map_camera_view)
    CameraView cameraView;
    @BindView(R.id.activity_map_destination_card_view)
    CardView destinationCardView;
    @BindView(R.id.activity_map_destination_text_view)
    TextView destinationTextView;
    @BindView(R.id.activity_maps_start_button)
    Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ButterKnife.bind(this);

        //check if authenticated user or not
        if(ParseUser.getCurrentUser() == null) {
            Intent authIntent = new Intent(MapsActivity.this, MainActivity.class);
            startActivity(authIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map Ready", Toast.LENGTH_SHORT).show();
        mMap = googleMap;

        //call setup
        setup();

        // Add a marker in Sydney and move the camera
        LatLng cmrit = new LatLng(12.9667214, 77.7103016);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(cmrit));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
    }

    private void setup() {
        //initialize sensor and sensormanager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);

        //setup start button
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
//                startActivity(intent);
            }
        });

        //setup place picker button
        destinationCardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(intentBuilder.build(MapsActivity.this), PLACE_PICKER_REQUEST_CODE);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //handle place picker request
        if (requestCode == PLACE_PICKER_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                destination = PlacePicker.getPlace(this, data);

                //setup destination
                destinationTextView.setText(destination.getName());

                //get potholes
                getPotholes();
            }
        }
    }

    //override sensor event methods
    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("DebugK", "Accuracy Changed " + accuracy);
    }

    @Override
    public final void onSensorChanged(SensorEvent sensorEvent) {
//        Log.d("DebugK", "" + sensorEvent.values[2]);

        if(Math.abs(startingValue - sensorEvent.values[2]) > 2) {
            Log.d("DebugK", "Pothole Detected");
        }

        startingValue = sensorEvent.values[2];
    }

    //method to get Bitmap from drawable
    public BitmapDescriptor bitmapDescriptorFromDrawable(int resId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, resId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    //method to get all potholes data
    public void getPotholes() {
        ParseQuery<ParseObject> potholesQuery = ParseQuery.getQuery("Pothole");
        potholesQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    potholes.clear();
                    potholes.addAll(objects);

                    //show on map
                    for (ParseObject pothole : potholes) {
                        ParseGeoPoint geoPoint = pothole.getParseGeoPoint("location");
                        String snippet = "Lat: " + geoPoint.getLatitude() + "\nLong: " + geoPoint.getLongitude();
                        LatLng location = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                        mMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title("Pothole")
                                .snippet(snippet)
                                .icon(bitmapDescriptorFromDrawable(R.drawable.ic_brightness_1_alert_24dp)));
                    }
                }
            }
        });
    }
}
