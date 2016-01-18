package com.pgmot.dominaker.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.pgmot.dominaker.R;
import com.pgmot.dominaker.controller.LocationController;
import com.pgmot.dominaker.controller.MapsController;
import com.pgmot.dominaker.gameobject.Ika;
import com.pgmot.dominaker.gameobject.Stage;
import com.pgmot.dominaker.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private GoogleMap googleMap;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient httpClient;
    //private static final String SERVER_URL = "133.19.60.87:4567";
    private static final String SERVER_URL = "192.168.0.12:4567";
    private Button drawButton;
    private boolean isDrawButtonTap = false;
    private Stage stage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        drawButton = (Button) findViewById(R.id.button);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        uuid = Util.getDeviceUUID(this);

        locationController = new LocationController(this);
        locationController.addListener(this);

        httpClient = new OkHttpClient();
        stage = new Stage();

        drawButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    isDrawButtonTap = true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    isDrawButtonTap = false;
                }
                return false;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                teamID = register(uuid);
                connectWebsocket();
            }
        }).start();
    }


    static class RegisterRequestClass {
        public String uuid;

        public RegisterRequestClass(String uuid) {
            this.uuid = uuid;
        }
    }


    private int register(String uuid) {
        Gson gson = new Gson();

        RequestBody requestBody = RequestBody.create(JSON, gson.toJson(new RegisterRequestClass(uuid), RegisterRequestClass.class));
        Request request = new Request.Builder()
                .url("http://" + SERVER_URL + "/register")
                .post(requestBody)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            String responseString = response.body().string();
            Log.d("dominaker-log", responseString);
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

    public static class MapStateClass {
        public int id;
        public int team_id;

        public MapStateClass(int id, int team_id) {
            this.id = id;
            this.team_id = team_id;
        }
    }

    private void connectWebsocket() {
        try {
            websocket = new WebSocketFactory().createSocket("ws://" + SERVER_URL);
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
                    websocket = websocket.recreate().connect();
                    isWebsocketConnected = false;
                }

                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    Log.d("log", message);

                    Request request = new Request.Builder()
                            .url("http://" + SERVER_URL + "/map")
                            .get()
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    String responseString = response.body().string();
                    ArrayList<MapStateClass> mapStatus = new Gson().fromJson(responseString, new TypeToken<List<MapStateClass>>() {
                    }.getType());
                    stage.updateMap(mapStatus);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ConcurrentHashMap<Integer, Stage.Grid> grids = stage.getGrids();
                            for (Map.Entry<Integer, Stage.Grid> grid : grids.entrySet()) {
                                Stage.Grid g = grid.getValue();
                                if (g.color == -1) {
                                    continue;
                                }
                                mapsController.drawRectangle(grid.getKey(), g.swLat, g.swLng, g.neLat, g.neLng, g.color == 0 ? Color.BLUE : Color.RED);
                            }
                        }
                    });

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
        this.googleMap = googleMap;

        googleMap.moveCamera(CameraUpdateFactory.zoomTo(20));
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

    static class LocationRequestClass {
        public String uuid;
        public double lat;
        public double lng;
        public boolean draw_flag;

        public LocationRequestClass(String uuid, double lat, double lng, boolean draw_flag) {
            this.uuid = uuid;
            this.lat = lat;
            this.lng = lng;
            this.draw_flag = draw_flag;
        }
    }

    @Override
    public void onLocationChanged(double latitude, double longitude) {
        Gson gson = new Gson();

        setCurrentPosition(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));

        if (isWebsocketConnected && websocket.isOpen()) {
            String message = gson.toJson(new LocationRequestClass(uuid, latitude, longitude, isDrawButtonTap), LocationRequestClass.class);
            Log.d("dominaker-log", message);
            websocket.sendText(message);
        }
    }
}
