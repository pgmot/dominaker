package com.pgmot.dominaker;

import android.app.Application;

import com.deploygate.sdk.DeployGate;

/**
 * Created by mot on 11/3/15.
 */
public class DominakerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DeployGate.install(this);
    }
}
