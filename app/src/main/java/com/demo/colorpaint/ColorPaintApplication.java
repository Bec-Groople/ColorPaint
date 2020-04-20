package com.demo.colorpaint;

import android.app.Application;
import android.content.Context;

/**
 * Application
 *
 * @date: 2020-01-30
 * @author: 山千
 */
public class ColorPaintApplication extends Application {
    private static Context mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = this;
    }

    public static Context getAppContext() {
        return mAppContext;
    }
}
