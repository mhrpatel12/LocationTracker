package com.loktra.locationtracker;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Mihir on 4/27/2017.
 */

public class LocationChangedEvent {
    public final ArrayList<LatLng> points;

    public LocationChangedEvent(ArrayList<LatLng> points) {
        this.points = points;
    }
}
