package com.pgmot.dominaker.controller;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.pgmot.dominaker.util.Util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mot on 11/4/15.
 */
public class MapsController {
    private GoogleMap map;
    private ConcurrentHashMap<String, Marker> markerConcurrentHashMap = new ConcurrentHashMap<>();

    public MapsController(GoogleMap map) {
        this.map = map;
    }

    private void moveCamera(double latitude, double longitude) {
        float zoom = 16.0f;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom));
    }

    public String addMarker(double latitude, double longitude) {
        return addMarker(latitude, longitude, BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
    }

    public String addMarker(double latitude, double longitude, BitmapDescriptor icon) {
        String uuid = Util.getUUID();
        Marker marker = map.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).icon(icon));

        markerConcurrentHashMap.put(uuid, marker);

        return uuid;
    }

    public boolean moveMarker(String uuid, double latitude, double longitude) {
        Marker marker = markerConcurrentHashMap.get(uuid);
        if (marker == null) {
            return false;
        }

        marker.setPosition(new LatLng(latitude, longitude));
        return true;
    }

    public boolean removeMarker(String uuid) {
        Marker marker = markerConcurrentHashMap.get(uuid);
        if (marker == null) {
            return false;
        }

        marker.remove();
        markerConcurrentHashMap.remove(uuid);
        return true;
    }

    public void drawRectangle(int id, double lat1, double lng1, double lat2, double lng2, int color) {
        Polygon polygon = polygonHashMap.get(id);
        if (polygon != null) {
            polygon.setFillColor(color);
            return;
        }

        polygon = map.addPolygon(
                new PolygonOptions()
                        .add(new LatLng(lat1, lng1), new LatLng(lat1, lng2), new LatLng(lat2, lng2), new LatLng(lat2, lng1))
                        .strokeColor(color)
                        .fillColor(color)
        );
        polygonHashMap.put(id, polygon);
    }

    private ConcurrentHashMap<Integer, Polygon> polygonHashMap = new ConcurrentHashMap<>();
}
