package com.pgmot.dominaker.gameobject;

import android.util.Log;

import com.pgmot.dominaker.activity.IkaActivity;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by mot on 1/19/16.
 */
public class Stage {
    private final static double LAT_PER1 = 0.000008983148616;
    private final static double LNG_PER1 = 0.000010966382364;

    //    private final static double LAT_START = 34.978691;
//    private final static double LNG_START = 135.961200;
//    private final static double LAT_END = 34.984252;
//    private final static double LNG_END = 135.965040;
    private final static double LAT_START = 35.0237464;
    private final static double LNG_START = 135.9473997;
    private final static double LAT_END = LAT_START + 0.02;
    private final static double LNG_END = LNG_START + 0.02;

    private final static double GRID_SIZE = 3.0;

    private ConcurrentHashMap<Integer, Grid> grids;

    public class Grid {
        public int id;
        public double swLat;
        public double neLat;
        public double swLng;
        public double neLng;
        public int color;

        public Grid(int id, double swLat, double neLat, double swLng, double neLng, int color) {
            this.id = id;
            this.swLat = swLat;
            this.neLat = neLat;
            this.swLng = swLng;
            this.neLng = neLng;
            this.color = color;
        }
    }

    public Stage() {
        grids = new ConcurrentHashMap<>();

        double lat = LAT_START;
        double lng = LNG_START;
        int gridId = 0;
        int defaultColor = -1;

        while (lat + LAT_PER1 * GRID_SIZE <= LAT_END) {
            while (lng + LNG_PER1 * GRID_SIZE <= LNG_END) {
                Grid grid = new Grid(gridId, lat, lat + LAT_PER1 * GRID_SIZE, lng, lng + LNG_PER1 * GRID_SIZE, defaultColor);
                lng += LNG_PER1 * GRID_SIZE;
                gridId += 1;
                grids.put(grid.id, grid);
            }
            lat += LAT_PER1 * GRID_SIZE;
            lng = LNG_START;
        }
    }

    public ConcurrentHashMap<Integer, Grid> getGrids() {
        return grids;
    }

    public void updateMap(ArrayList<IkaActivity.MapStateClass> statusList) {
        for (IkaActivity.MapStateClass status : statusList) {
            try {
                Grid g = grids.get(status.id);
                if(g == null){
                    Log.d("goge", "g is null");
                }
                g.color = status.team_id;
                grids.put(status.id, g);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
