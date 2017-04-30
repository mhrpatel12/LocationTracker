package com.loktra.locationtracker.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.loktra.locationtracker.R;
import com.loktra.locationtracker.event.LocationChangedEvent;
import com.loktra.locationtracker.service.LocationTrackerService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Minutes;

import java.util.ArrayList;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    Polyline line;
    boolean isServiceRunning = false;
    private GoogleMap mMap;
    private Context mContext;
    private ArrayList<LatLng> points;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ((ContextCompat.checkSelfPermission(MapsActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
                ||
                (ContextCompat.checkSelfPermission(MapsActivity.this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED)
                ) {
            if ((ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION))
                    &&
                    (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION))) {

            } else {
                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        6);
            }
        }

        setContentView(R.layout.activity_map);

        mContext = this;
        points = new ArrayList<LatLng>(); //added

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        isServiceRunning = false;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationTrackerService.class.getName().equals(service.service.getClassName())) {
                isServiceRunning = true;
            }
        }
        if (isServiceRunning) {
            ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setProgress(100);
            ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setThumb(getResources().getDrawable(R.drawable.ic_left_black_24dp));
            ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setBackground(getResources().getDrawable(R.drawable.background_seekbar_green));
            ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setProgressDrawable(getResources().getDrawable(R.drawable.background_seekbar_green));
            ((TextView) findViewById(R.id.txtSwitch)).setText(getString(R.string.switch_end));
        } else {
            ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setProgress(0);
            ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setThumb(getResources().getDrawable(R.drawable.ic_right_black_24dp));
            ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setBackground(getResources().getDrawable(R.drawable.background_seekbar_red));
            ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setProgressDrawable(getResources().getDrawable(R.drawable.background_seekbar_red));
            ((TextView) findViewById(R.id.txtSwitch)).setText(getString(R.string.switch_start));
        }

        ((AppCompatSeekBar) findViewById(R.id.seek_bar_start_end)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 50) {
                    seekBar.setProgress(0);
                    seekBar.setThumb(getResources().getDrawable(R.drawable.ic_right_black_24dp));
                    seekBar.setBackground(getResources().getDrawable(R.drawable.background_seekbar_red));
                    seekBar.setProgressDrawable(getResources().getDrawable(R.drawable.background_seekbar_red));

                    long shiftStartTime = Long.parseLong(PreferenceManager.getDefaultSharedPreferences(MapsActivity.this).getString("serviceStartTime", ""));
                    DateTime dateStart = new DateTime(shiftStartTime);
                    DateTime dateEnd = new DateTime(System.currentTimeMillis());
                    ((LinearLayout) findViewById(R.id.layout_shift_time)).setVisibility(View.VISIBLE);
                    ((TextView) findViewById(R.id.txtServiceTime)).setText(Hours.hoursBetween(dateStart, dateEnd).getHours() % 24 + " h " + Minutes.minutesBetween(dateStart, dateEnd).getMinutes() % 60 + " m");
                    ((TextView) findViewById(R.id.txtSwitch)).setText(getString(R.string.switch_start));

                    Drawable circleDrawable = getResources().getDrawable(R.drawable.ic_plot_end_24dp);
                    BitmapDescriptor markerIcon = getMarkerIconFromDrawable(circleDrawable);
                    if (points.size() > 0) {
                        MarkerOptions markerOptions = new MarkerOptions().position(points.get(points.size() - 1)).icon(markerIcon);
                        mMap.addMarker(markerOptions);
                    }

                    stopService(new Intent(MapsActivity.this, LocationTrackerService.class));
                    EventBus.getDefault().unregister(this);
                } else {
                    ((TextView) findViewById(R.id.txtSwitch)).setText(getString(R.string.switch_end));
                    ((LinearLayout) findViewById(R.id.layout_shift_time)).setVisibility(View.GONE);
                    if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    } else {
                        seekBar.setProgress(100);
                        seekBar.setThumb(getResources().getDrawable(R.drawable.ic_left_black_24dp));
                        seekBar.setBackground(getResources().getDrawable(R.drawable.background_seekbar_green));
                        seekBar.setProgressDrawable(getResources().getDrawable(R.drawable.background_seekbar_green));
                        points.add(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
                        redrawLine();
                        PreferenceManager.getDefaultSharedPreferences(MapsActivity.this).edit().putString("serviceStartTime", System.currentTimeMillis() + "").apply();
                        Intent intent = new Intent(mContext, LocationTrackerService.class);
                        startService(intent);
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults[0] == PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (mLastLocation != null) {
                LatLng ltLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                CameraUpdate center = CameraUpdateFactory.newLatLngZoom(ltLng, 15);
                if (!isServiceRunning) {
                    mMap.addMarker(new MarkerOptions().position(ltLng).title("Your Location"));
                }
                mMap.moveCamera(center);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void redrawLine() {
        mMap.clear();  //clears all Markers and Polylines
        Drawable circleDrawable = getResources().getDrawable(R.drawable.ic_plot_start_24dp);
        BitmapDescriptor markerIcon = getMarkerIconFromDrawable(circleDrawable);
        MarkerOptions markerOptions = new MarkerOptions().position(points.get(0)).icon(markerIcon);
        mMap.addMarker(markerOptions);

        PolylineOptions options = new PolylineOptions().width(10).color(Color.BLUE).geodesic(true);

        for (int i = 1; i < points.size(); i++) {
            LatLng point = points.get(i);
            options.add(point);
        }
        CameraUpdate center = CameraUpdateFactory.newLatLngZoom(points.get(points.size() - 1), 15);
        mMap.moveCamera(center);
        line = mMap.addPolyline(options); //add Polyline
    }

    // This method will be called when a MessageEvent is posted (in the UI thread)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(LocationChangedEvent locationChangedEvent) {
        if (locationChangedEvent != null) {
            points = locationChangedEvent.points;
            redrawLine(); //added
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {

            LatLng ltLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            CameraUpdate center = CameraUpdateFactory.newLatLngZoom(ltLng, 15);
            if (!isServiceRunning) {
                mMap.addMarker(new MarkerOptions().position(ltLng).title("Your Location"));
            }
            mMap.moveCamera(center);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

}
