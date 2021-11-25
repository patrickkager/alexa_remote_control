package com.android.remote.volumecontrol;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class MyApp extends Application {
    public static final String PREFER_NAME = "settingsMain";
    public static String alexaLoginUrl = "alexa.amazon.de";
    public static String alexaBaseURI = "alexa.amazon.de";
    public static final String RESTART_INTENT = "com.android.remote.volumecontrol";
    private static MyApp instance;
    private static Context mContext;
    private static SharedPreferences settings;

    public static MyApp getInstance() {
        return instance;
    }

    public static Context getContext() {
        return mContext;
    }

    public static boolean debugOutPut(){return true;}

    public static SharedPreferences getSettings(){ return settings; }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mContext = getApplicationContext();
        settings = getSharedPreferences(MyApp.PREFER_NAME, MODE_PRIVATE);
    }


    public static String MapToJson(Map<String,String> map)  {

        if(map == null)
            return "";

        try {
            JSONObject obj = new JSONObject(map);
            return obj.toString();
        } catch (Exception e) {
           return "";
        }
    }

    public static Map<String,String> JsonToMap(String jsonString){
        if(jsonString.isEmpty())
            return new HashMap<>();

        HashMap<String, String> map = new HashMap<String, String>();
        try{
            JSONObject jObject = new JSONObject(jsonString);
            Iterator<?> keys = jObject.keys();

            while( keys.hasNext() ){
                String key = (String)keys.next();
                String value = jObject.getString(key);
                map.put(key, value);
            }
        }catch (JSONException e){ }

        return map;
    }
}