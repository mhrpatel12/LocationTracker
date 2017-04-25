package com.loktra.locationtracker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import fr.quentinklein.slt.LocationTracker;
import fr.quentinklein.slt.TrackerSettings;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Context mContext;
    private ArrayList<LatLng> points; //added
    Polyline line; //added
    LocationTracker tracker;

    private LatLng latLngFromService;

    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        IntentFilter filter = new IntentFilter();
        filter.addAction("SOME_ACTION");
        filter.addAction("SOME_OTHER_ACTION");
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((intent.getStringExtra("lat") != null) && (intent.getStringExtra("lng") != null)) {
                    String lat = intent.getStringExtra("lat");
                    String lng = intent.getStringExtra("lng");
                    latLngFromService = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                    points.add(latLngFromService); //added

                    redrawLine(); //added
                }
            }
        };
        registerReceiver(broadcastReceiver, filter);

/*        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ((intent.getStringExtra("lat") != null) && (intent.getStringExtra("lng") != null)) {
                    String lat = intent.getStringExtra("lat");
                    String lng = intent.getStringExtra("lng");
                    latLngFromService = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));

                    points.add(latLngFromService); //added

                    redrawLine(); //added
                }
            }
        };*/

        mContext = this;
        points = new ArrayList<LatLng>(); //added

        final TrackerSettings settings =
                new TrackerSettings()
                        .setUseGPS(true)
                        .setUseNetwork(true)
                        .setUsePassive(true)
                        .setTimeBetweenUpdates(15000)
                        .setMetersBetweenUpdates(10);

        if ((ContextCompat.checkSelfPermission(MapsActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
                ||
                (ContextCompat.checkSelfPermission(MapsActivity.this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)
                ) {

            // Should we show an explanation?
            if ((ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION))
                    &&
                    (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION))) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        6);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // You need to ask the user to enable the permissions
                } else {
                    Intent intent = new Intent(mContext, LocationTrackerService.class);
                    startService(intent);
/*                    tracker = new LocationTracker(mContext, settings) {
                        @Override
                        public void onLocationFound(Location location) {
                            LatLng sydney = new LatLng(location.getLatitude(), location.getLongitude());
                            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(sydney, 17);
                            mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
                            mMap.moveCamera(center);

                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            LatLng latLng = new LatLng(latitude, longitude); //you already have this

                            points.add(latLng); //added

                            redrawLine(); //added
                        }

                        @Override
                        public void onTimeout() {

                        }
                    };
                    tracker.startListening();
               */
                }
            }
        });

        findViewById(R.id.btnEnd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tracker.stopListening();
            }
        });
    }

    private void redrawLine() {

        mMap.clear();  //clears all Markers and Polylines

        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);

        for (int i = 0; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }
        line = mMap.addPolyline(options); //add Polyline
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));

        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    protected void onStart() {
        super.onStart();
        //LocalBroadcastManager.getInstance(this).registerReceiver((broadcastReceiver), new IntentFilter(LocationTrackerService.COPA_RESULT));
    }

    @Override
    protected void onStop() {
        unregisterReceiver(broadcastReceiver);
        super.onStop();
    }
}
