package com.pgmot.dominaker.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.gson.Gson;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.pgmot.dominaker.R;
import com.pgmot.dominaker.controller.LocationController;
import com.pgmot.dominaker.controller.MapsController;
import com.pgmot.dominaker.gameobject.Ika;
import com.pgmot.dominaker.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IkaActivity extends FragmentActivity implements OnMapReadyCallback, LocationController.LocationControllerListener {

    private static final String LOG_TAG = "dominaker_log";
    private WebSocket websocket;
    private boolean isWebsocketConnected = false;
    private String uuid;
    private int teamID = -1;
    private String myMarkerUUID = "";
    private ConcurrentHashMap<String, Ika> ikaConcurrentHashMap = new ConcurrentHashMap<>();
    private ArrayList<Ika> ikas = new ArrayList<>();
    private LocationController locationController;
    private MapsController mapsController;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient httpClient;
    private static final String SERVER_URL = "dominaker-staging.herokuapp.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        uuid = Util.getDeviceUUID(this);

        locationController = new LocationController(this);
        locationController.addListener(this);

        httpClient = new OkHttpClient();

        new Thread(new Runnable() {
            @Override
            public void run() {
                teamID = register(uuid);
                connectWebsocket();
            }
        }).start();
    }

    private int register(String uuid) {
        Gson gson = new Gson();

        class RegisterRequestClass {
            public String uuid;

            public RegisterRequestClass(String uuid) {
                this.uuid = uuid;
            }
        }

        RequestBody requestBody = RequestBody.create(JSON, "{\"uuid\":\"" + uuid + "\"}");
        Request request = new Request.Builder()
                .url("https://" + SERVER_URL + "/register")
                .post(requestBody)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            String responseString = response.body().string();
            return gson.fromJson(responseString, RegisterResponseClass.class).team_id;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    static class RegisterResponseClass {
        public int team_id;

        public RegisterResponseClass(int team_id) {
            this.team_id = team_id;
        }
    }

    private void connectWebsocket() {
        try {
            websocket = new WebSocketFactory().createSocket("wss://" + SERVER_URL);
            websocket.addListener(new WebSocketAdapter() {
                @Override
                public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                    super.onConnected(websocket, headers);
                    Log.v(LOG_TAG, "onConnected");

                    isWebsocketConnected = true;
                }

                @Override
                public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                    super.onConnectError(websocket, exception);
                    Log.v(LOG_TAG, "onConnectError");

                    isWebsocketConnected = false;
                }

                @Override
                public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                    super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
                    Log.v(LOG_TAG, "onDisconnected");

                    // 再接続処理
                    websocket.recreate().connect();
                    isWebsocketConnected = false;
                }

                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    String[] split = message.split(",");
                    String uuid = split[0];
                    double latitude = Double.valueOf(split[1]);
                    double longitude = Double.valueOf(split[2]);

                    if (Objects.equals(IkaActivity.this.uuid, uuid)) {
                        return;
                    }
                    Log.d(LOG_TAG, message);

                    setAnotherUserPosition(uuid, latitude, longitude);
                }
            });

            websocket.connect();
        } catch (IOException | WebSocketException e) {
            e.printStackTrace();
        }
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
