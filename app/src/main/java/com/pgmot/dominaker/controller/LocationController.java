package com.pgmot.dominaker.controller;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

/**
 * Created by mot on 11/4/15.
 */
public class LocationController implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private static final String LOG_TAG = "LocationController";

    private GoogleApiClient googleApiClient;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
    private LocationRequest locationRequest;
    private static final int LOCATION_REQUEST_INTERVAL = 1000;

    private ArrayList<LocationControllerListener> locationControllerListeners = new ArrayList<>();

    public LocationController(Context context) {
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(LOCATION_REQUEST_INTERVAL);

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void addListener(LocationControllerListener listener){
        locationControllerListeners.add(listener);
    }

    public void removeListener(LocationControllerListener listener){
        locationControllerListeners.remove(listener);
    }

    public void startLocationController() {
        googleApiClient.connect();
    }

    public void stopLocationController() {
        googleApiClient.disconnect();
    }


    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Log.d(LOG_TAG, "onLocationChanged: " + latitude + ", " + longitude);

        for(LocationControllerListener listener : locationControllerListeners){
            listener.onLocationChanged(latitude, longitude);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "onConnected");

        fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed");
    }

    public interface LocationControllerListener{
        void onLocationChanged(double latitude, double longitude);
    }
}
