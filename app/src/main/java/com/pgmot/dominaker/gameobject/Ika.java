package com.pgmot.dominaker.gameobject;

/**
 * Created by mot on 11/4/15.
 */
public class Ika {
    private String UUID;
    private String markerUUID;

    public Ika(String UUID, String markerUUID){
        this.UUID = UUID;
        this.markerUUID = markerUUID;
    }

    public String getUUID(){
        return UUID;
    }

    public String getMarkerUUID(){
        return markerUUID;
    }
}
