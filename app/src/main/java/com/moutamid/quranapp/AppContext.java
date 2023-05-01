package com.moutamid.quranapp;

import android.app.Application;

import com.fxn.stash.Stash;

public class AppContext extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //Add this line in ApplicationContext.java
        Stash.init(this);
    }
}
