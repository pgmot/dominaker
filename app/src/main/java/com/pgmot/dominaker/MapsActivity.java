package com.pgmot.dominaker;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationController.LocationControllerListener {

    private static final String LOG_TAG = "dominaker_log";
    private WebSocket websocket;
    private boolean isWebsocketConnected = false;
    private String uuid;
    private String myMarkerUUID = "";
    private ConcurrentHashMap<String, Ika> ikaConcurrentHashMap = new ConcurrentHashMap<>();
    private ArrayList<Ika> ikas = new ArrayList<>();
    private LocationController locationController;
    private MapsController mapsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        uuid = Util.getDeviceUUID(this);

        locationController = new LocationController(this);
        locationController.addListener(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = "ws://192.168.5.72:4567";
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

                            setAnotherUserPosition(uuid, latitude, longitude);
                        }
                    });

                    isWebsocketConnected = true;
                } catch (IOException | WebSocketException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapsController = new MapsController(googleMap);
    }

    private void setCurrentPosition(double latitude, double longitude) {
        if (myMarkerUUID.isEmpty()) {
            myMarkerUUID = mapsController.addMarker(latitude, longitude);
            return;
        }

        mapsController.moveMarker(myMarkerUUID, latitude, longitude);
    }

    private void setAnotherUserPosition(final String uuid, final double latitude, final double longitude) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Ika ika = ikaConcurrentHashMap.get(uuid);
                if (ika == null) {
                    String markerUUID = mapsController.addMarker(latitude, longitude);
                    ikaConcurrentHashMap.put(uuid, new Ika(uuid, markerUUID));
                    return;
                }

                mapsController.moveMarker(ika.getMarkerUUID(), latitude, longitude);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        locationController.startLocationController();
    }

    @Override
    protected void onStop() {
        locationController.stopLocationController();

        super.onStop();
    }

    @Override
    public void onLocationChanged(double latitude, double longitude) {
        setCurrentPosition(latitude, longitude);

        if (isWebsocketConnected && websocket.isOpen()) {
            websocket.sendText(uuid + "," + latitude + "," + longitude);
        }
    }
}
