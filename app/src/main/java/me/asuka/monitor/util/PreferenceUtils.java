package me.asuka.monitor.util;

import android.content.SharedPreferences;

import me.asuka.monitor.Application;

/**
 * A class containing utility methods related to preferences
 */
public class PreferenceUtils {

    public static int getUserID() {
        return getInt("user_id", -1);
    }

    public static void saveString(SharedPreferences prefs, String key, String value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(key, value);

        edit.apply();
    }

    public static void saveString(String key, String value) {
        saveString(Application.getPrefs(), key, value);
    }

    public static void saveInt(SharedPreferences prefs, String key, int value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putInt(key, value);

        edit.apply();
    }

    public static void saveInt(String key, int value) {
        saveInt(Application.getPrefs(), key, value);
    }

    public static void saveLong(SharedPreferences prefs, String key, long value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(key, value);

        edit.apply();
    }

    public static void saveLong(String key, long value) {
        saveLong(Application.getPrefs(), key, value);
    }

    public static void saveBoolean(SharedPreferences prefs, String key, boolean value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(key, value);

        edit.apply();
    }

    public static void saveBoolean(String key, boolean value) {
        saveBoolean(Application.getPrefs(), key, value);
    }

    public static void saveFloat(SharedPreferences prefs, String key, float value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putFloat(key, value);

        edit.apply();
    }

    public static void saveFloat(String key, float value) {
        saveFloat(Application.getPrefs(), key, value);
    }

    public static void saveDouble(SharedPreferences prefs, String key, double value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putLong(key, Double.doubleToRawLongBits(value));

        edit.apply();
    }

    public static void saveDouble(String key, double value) {
        saveDouble(Application.getPrefs(), key, value);
    }

    /**
     * Gets a double for the provided key from preferences, or the default value if the preference
     * doesn't currently have a value
     *
     * @param key          key for the preference
     * @param defaultValue the default value to return if the key doesn't have a value
     * @return a double from preferences, or the default value if it doesn't exist
     */
    public static Double getDouble(String key, double defaultValue) {
        if (!Application.getPrefs().contains(key)) {
            return defaultValue;
        }
        return Double.longBitsToDouble(Application.getPrefs().getLong(key, 0));
    }

    public static String getString(String key) {
        return Application.getPrefs().getString(key, null);
    }

    public static int getInt(String key, int defaultValue) {
        return Application.getPrefs().getInt(key, defaultValue);
    }

    public static long getLong(String key, long defaultValue) {
        return Application.getPrefs().getLong(key, defaultValue);
    }

    public static float getFloat(String key, float defaultValue) {
        return Application.getPrefs().getFloat(key, defaultValue);
    }

    /**
     * Removes the specified preference by deleting it
     * @param key
     */
    public static void remove(String key) {
        SharedPreferences.Editor edit = Application.getPrefs().edit();
        edit.remove(key).apply();
    }
}
