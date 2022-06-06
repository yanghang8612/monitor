package me.asuka.monitor.util;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class NetUtils {

    private static final String TAG = NetUtils.class.getSimpleName();

    private static final Response.ErrorListener ERROR_LISTENER = error -> {
        Log.i(TAG, "net error: " + error.toString());
    };

    private static final String URL = "http://106.37.201.139:8000";

    public static Request<JSONObject> getRegisterParam(
            String username, String password, String id, Response.Listener<JSONObject> listener) {
        Map<String, Object> map = new HashMap<>();
        map.put("phone_number", username);
        map.put("password", password);
        map.put("identity_id", id);
        JSONObject jsonObj = new JSONObject(map);
        return new JsonObjectRequest(Request.Method.POST, URL + "/register",
                jsonObj, listener, ERROR_LISTENER);
    }

    public static Request<JSONObject> getLoginParam(
            String username, String password, Response.Listener<JSONObject> listener) {
        Map<String, Object> map = new HashMap<>();
        map.put("phone_number", username);
        map.put("password", password);
        JSONObject jsonObj = new JSONObject(map);
        return new JsonObjectRequest(Request.Method.POST, URL + "/login",
                jsonObj, listener, ERROR_LISTENER);
    }

    public static void reportUserLocation(
            Context context, Location loc, Response.Listener<JSONObject> listener) {
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", PreferenceUtils.getUserID());
        map.put("timestamp", System.currentTimeMillis());
        map.put("longitude", loc.getLongitude());
        map.put("latitude", loc.getLatitude());
        map.put("altitude", loc.getAltitude());
        map.put("speed", loc.getSpeed());
        JSONObject jsonObj = new JSONObject(map);
        AppSingleton.getInstance(context).addToRequestQueue(
                new JsonObjectRequest(Request.Method.POST, URL + "/user_position",
                        jsonObj, listener, ERROR_LISTENER));
    }

    public static void reportUserBLETop3(
            Context context, String[] top3Address, Response.Listener<JSONObject> listener) {
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", PreferenceUtils.getUserID());
        map.put("timestamp", System.currentTimeMillis());
        map.put("blue_id_1", top3Address[0]);
        map.put("blue_id_2", top3Address[1]);
        map.put("blue_id_3", top3Address[2]);
        JSONObject jsonObj = new JSONObject(map);
        AppSingleton.getInstance(context).addToRequestQueue(
                new JsonObjectRequest(Request.Method.POST, URL + "/user_bluet",
                        jsonObj, listener, ERROR_LISTENER));
    }

    public static void queryGPSReportFreq(
            Context context, Response.Listener<JSONArray> listener) {
        AppSingleton.getInstance(context).addToRequestQueue(
                new JsonArrayRequest(Request.Method.POST, URL + "/user_bluet",
                        null, listener, ERROR_LISTENER));
    }
}
