package com.qrilt.roadrunner;

import android.app.Application;
import android.graphics.Camera;

import com.parse.Parse;

public class RoadrunnerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //setup parse
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("eatzieAppId")
                .server("http://35.238.152.97:1337/eatzie")
                .build());
    }

    //static method to get camera instance
    public static void getCameraInstance() {
        Camera c = null;
    }
}
