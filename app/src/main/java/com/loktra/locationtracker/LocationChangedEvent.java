package com.loktra.locationtracker;

import android.location.Location;

/**
 * Created by Mihir on 4/27/2017.
 */

public class LocationChangedEvent {
    public final Location location;

    public LocationChangedEvent(Location location) {
        this.location = location;
    }
}
