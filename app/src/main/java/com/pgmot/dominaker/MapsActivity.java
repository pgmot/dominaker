package com.pgmot.dominaker;

import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = "dominaker_log";
    private GoogleMap map;
    private Marker currentPositionMarker;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
    private LocationRequest locationRequest;
    private WebSocket websocket;
    private boolean isWebsocketConnected = false;
    private String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        uuid = getUUID();

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = "wss://dominaker-staging.herokuapp.com/";
                try {
                    websocket = new WebSocketFactory().createSocket(url);
                    websocket.connect();
                    websocket.addListener(new WebSocketAdapter() {
                        @Override
                        public void onTextMessage(WebSocket websocket, String message) throws Exception {
                            String[] split = message.split(",");
                            String uuid = split[0];
                            double latitude = Double.valueOf(split[1]);
                            double longitude = Double.valueOf(split[2]);

                            if (Objects.equals(MapsActivity.this.uuid, uuid)) {
                                return;
                            }
                            Log.d(LOG_TAG, message);
                        }
                    });

                    isWebsocketConnected = true;
                } catch (IOException | WebSocketException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        map = googleMap;
    }

    private void setCurrentPosition(double latitude, double longitude) {
        if (currentPositionMarker != null) {
            currentPositionMarker.remove();
        }

        currentPositionMarker = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)));
    }

    private void moveCamera(double latitude, double longitude) {
        float zoom = 16.0f;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom));
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        Log.d(LOG_TAG, "onLocationChanged: " + latitude + ", " + longitude);

        setCurrentPosition(latitude, longitude);
        moveCamera(latitude, longitude);

        if (isWebsocketConnected && websocket.isOpen()) {
            websocket.sendText(uuid + "," + latitude + "," + longitude);
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

    private String getUUID() {
        final String uuidKey = "uuid";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String uuid = sharedPreferences.getString(uuidKey, "");

        if (uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(uuidKey, uuid).apply();
        }
        return uuid;
    }
}
