package com.redur.electra;

import android.app.Application;

import com.redur.electra.core.logger.ActivityLifecycleLogger;
import com.redur.electra.core.logger.LoggingInitializer;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class ElectraApp extends Application {

    @Inject
    LoggingInitializer loggingInitializer;

    @Override
    public void onCreate() {
        super.onCreate();
        loggingInitializer.init();
        registerActivityLifecycleCallbacks(new ActivityLifecycleLogger());
    }
}
