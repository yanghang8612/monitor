package me.asuka.monitor;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Application extends android.app.Application {

    private static Application mApp;

    private static ScheduledExecutorService mExecutor;

    private SharedPreferences mPrefs;

    public static Application get() {
        return mApp;
    }

    public static ScheduledExecutorService getExecutor() {return mExecutor;}

    public static SharedPreferences getPrefs() {
        return get().mPrefs;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = this;
        mExecutor = Executors.newScheduledThreadPool(5);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mApp = null;
    }
}
