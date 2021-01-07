package com.android.remote.volumecontrol;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioPlaybackConfiguration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends Activity {

    private static final String PREFER_NAME = "settingsMain";
    private SettingsContentObserver mSettingsContentObserver;
    private WebView alexaViewer;
    private LinearLayout settingsView;
    private String TAG = "MainActivity";
    private boolean settingsVisible = false;
    private String Cookies = "";
    private String Csrf = "";
    private JSONArray deviceList;
    private Map<String,String> roomDevices;
    private Map<String,String> multiRooms;

    //Settings to Store
    public Integer changeVolumeDiff = 0;
    public String alexaLoginUrl = "alexa.amazon.de";
    public String alexaBaseURI = "alexa.amazon.de";
    private String roomName = "";
    private String username = "";
    private String password = "";
    private String volumeCmd = "";


    @SuppressLint("JavascriptInterface")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        settingsView = (LinearLayout) findViewById(R.id.layoutSettings);
        alexaViewer = (WebView)findViewById(R.id.alexaWebViewer);

        alexaViewer.setVisibility(View.VISIBLE);
        settingsView.setVisibility(View.GONE);

        alexaViewer.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
        alexaViewer.getSettings().setJavaScriptEnabled(true);
        alexaViewer.getSettings().setLoadWithOverviewMode(true);
        alexaViewer.getSettings().setUseWideViewPort(true);

        alexaViewer.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {

                if(url.contains("/ap/signin")){
                    String js = "javascript:(function(){"+
                            "document.getElementById('ap_email').value = '"+username+"';"+
                            "document.getElementById('ap_password').value = '"+password+"';"+
                            "document.getElementById('signInSubmit').click();"+
                            "})()";
                    alexaViewer.evaluateJavascript(js, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {
                            String result = s;
                        }
                    });
                }else if(url.contains("/spa/index.html")){
                    Map<String,String> httpHeaders = new HashMap<>();
                    httpHeaders.put("Content-Type","application/json");
                    alexaViewer.loadUrl("https://"+alexaBaseURI+"/api/devices-v2/device",httpHeaders);
                }else if(url.contains("/devices-v2/device")){
                    Cookies = CookieManager.getInstance().getCookie(url);
                    Csrf =  Cookies.substring(Cookies.lastIndexOf("csrf="));
                    Csrf = Csrf.replace("csrf=","");
                    alexaViewer.evaluateJavascript("(function(){return JSON.parse(document.body.innerText)})();",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String html) {
                                    try {
                                        multiRooms = new HashMap<>();

                                        JSONObject objDeviceList = new JSONObject(html);
                                        deviceList = objDeviceList.getJSONArray("devices");

                                        JSONArray arrRoomDevices = null;
                                        for(int i=0; i<deviceList.length(); i++){
                                            JSONObject o = deviceList.getJSONObject(i);
                                            if(o.getString("accountName").contains(roomName)){
                                                multiRooms.put(o.getString("serialNumber"),o.getString("deviceType"));
                                                arrRoomDevices = o.getJSONArray("clusterMembers");
                                                break;
                                            }
                                        }

                                        roomDevices = new HashMap<>();
                                        for(int j=0; j<arrRoomDevices.length(); j++) {
                                            String serialNumber = arrRoomDevices.getString(j);
                                            for(int i=0; i<deviceList.length(); i++){
                                                JSONObject o = deviceList.getJSONObject(i);
                                                if(o.getString("serialNumber").contains(serialNumber)){
                                                    roomDevices.put(serialNumber,o.getString("deviceType"));
                                                    break;
                                                }
                                            }
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    alexaViewer.loadData("<div style=\"padding-top: 100px ; text-align:center; color: red; font-size:medium;\">\n" +
                                            " Get Cookie done.<br />\n" +
                                            "Get Rooms done.<br />\n" +
                                            "Get Devices done.<br />\n" +
                                            "Ready for use!<br />\n" +
                                            "<br/>\n" +
                                            "Try:<br/>\n" +
                                            "Volume + / Volume - / Mute\n" +
                                            "</div>","text/html","UTF-8");
                                }
                    });
                }
            }
        });

        mSettingsContentObserver = new SettingsContentObserver(this,new Handler());
        this.getApplicationContext().getContentResolver().registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true,
                mSettingsContentObserver);

        LoadSettings();

        if(username != "" && password != ""){
            DoLogin(null);
        }else{
            ShowSettings(null);
        }

    }


    private Boolean sendAlexaCommand(String DeviceSerial, String DeviceType,String jsonString) {
        try {
            URL url = new URL("https://"+alexaBaseURI+"/api/np/command?deviceSerialNumber="+DeviceSerial+"&deviceType="+DeviceType);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Cookie",Cookies);
            conn.setRequestProperty("csrf",Csrf);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            OutputStream os = conn.getOutputStream();
            os.write(jsonString.getBytes());
            os.flush();

            BufferedReader br;
            if(conn.getResponseCode() == 200) {
                br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            }else {
                br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
            }

            String output;
            StringBuilder response = new StringBuilder();
            while ((output = br.readLine()) != null) {
                response.append(output);
                response.append('\r');
            }
            String mes = response.toString();
            conn.disconnect();

            if(conn.getResponseCode() != 200) {
                alexaViewer.loadData(mes,"text/html","UTF-8");
                Log.d(TAG,"Could not change volume for type: "+DeviceType+" serial:"+DeviceSerial);
            }else{
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                String formattedDate = df.format(c.getTime());
                alexaViewer.loadData("<h1>"+formattedDate+" Volume ("+volumeCmd+") Changed for type: "+DeviceType+" serial:"+DeviceSerial+"</h1>","text/html","UTF-8");
                Log.d(TAG,"Volume Changed for type: "+DeviceType+" serial:"+DeviceSerial);
            }

            if (mes!=null && !mes.isEmpty()){
                return true;
            }else {
                return false;
            }

        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void ChangeVolume(String volume) {

        try{
            if(multiRooms == null || volumeCmd == "")
                return;

            String jsonString = "";
            if(volumeCmd.contains("fixed")){
                jsonString ="{\"type\":\"VolumeLevelCommand\",\"volumeLevel\":"+volume+",\"contentFocusClientId\":\"Default\"}";
            }else{
                jsonString ="{\"type\":\"VolumeAdjustCommand\",\"volumeAdjustment\":"+volume+",\"contentFocusClientId\":\"Default\"}";
            }

            for (Map.Entry<String, String> entry : multiRooms.entrySet()) {
                sendAlexaCommand(entry.getKey(),entry.getValue(),jsonString);
            }

            /*
            //Change Volume for Devices
            String jsonStringSingle ="{\"type\":\"VolumeLevelCommand\",\"volumeLevel\":"+volume+",\"contentFocusClientId\":null}";
            for (Map.Entry<String, String> entry : roomDevices.entrySet()) {
                sendAlexaCommand(entry.getKey(),entry.getValue(),jsonStringSingle);
            }
            */
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void DoLogin(View view) {
        alexaViewer.loadUrl("https://"+alexaLoginUrl);
    }

    public void ClickSubmit(View view){
        String js = "javascript:(function(){"+
                "document.getElementById('auth-signin-button').click();"+
                "})()";
        alexaViewer.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                String result = s;
            }
        });
    }

    public void ShowSettings(View view){
        settingsVisible = !settingsVisible;

        if(settingsVisible) {
            settingsView.setVisibility(View.VISIBLE);
            alexaViewer.setVisibility(View.GONE);
        }else{
            alexaViewer.setVisibility(View.VISIBLE);
            settingsView.setVisibility(View.GONE);
        }
    }

    private void LoadSettings(){
        SharedPreferences settings = getSharedPreferences(PREFER_NAME, MODE_PRIVATE);
        username = settings.getString("username", "");
        password = settings.getString("password", "");
        roomName = settings.getString("roomName", "Wohnzimmer");
        volumeCmd = settings.getString("volumeCmd","fixed");
        changeVolumeDiff = settings.getInt("changeVolumeDiff", 250);

        EditText txtusername = (EditText)findViewById(R.id.editTextUsername);
        EditText txtpassword = (EditText)findViewById(R.id.editTextPassword);
        EditText txtroomName = (EditText)findViewById(R.id.editTextRoomName);
        EditText txtchangeVolumeDiff = (EditText)findViewById(R.id.editTextVolumeDiff);
        RadioButton rdVolumeFixed = (RadioButton)findViewById(R.id.radioButtonVolumeFixed);
        RadioButton rdVolumeAdjusted = (RadioButton)findViewById(R.id.radioButtonVolumeAdjusted);

        txtusername.setText(username);
        txtpassword.setText(password);
        txtroomName.setText(roomName);
        txtchangeVolumeDiff.setText(String.valueOf(changeVolumeDiff));
        if(volumeCmd.contains("fixed")){
            rdVolumeFixed.setChecked(true);
            rdVolumeAdjusted.setChecked(false);
        }else{
            rdVolumeFixed.setChecked(false);
            rdVolumeAdjusted.setChecked(true);
        }
    }

    public void SaveSettings(View view){
        ShowSettings(view);

        //Save and Reload ....
        EditText txtusername = (EditText)findViewById(R.id.editTextUsername);
        EditText txtpassword = (EditText)findViewById(R.id.editTextPassword);
        EditText txtroomName = (EditText)findViewById(R.id.editTextRoomName);
        EditText txtchangeVolumeDiff = (EditText)findViewById(R.id.editTextVolumeDiff);
        RadioButton rdVolumeFixed = (RadioButton)findViewById(R.id.radioButtonVolumeFixed);
        RadioButton rdVolumeAdjusted = (RadioButton)findViewById(R.id.radioButtonVolumeAdjusted);

        username = txtusername.getText().toString();
        password = txtpassword.getText().toString();
        roomName = txtroomName.getText().toString();
        changeVolumeDiff = Integer.valueOf(txtchangeVolumeDiff.getText().toString());
        volumeCmd = "fixed";
        if(rdVolumeFixed.isChecked()){
            volumeCmd = "fixed";
        }else if(rdVolumeAdjusted.isChecked()){
            volumeCmd = "adjusted";
        }

        SharedPreferences settings = getSharedPreferences(PREFER_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.putString("roomName", roomName);
        editor.putString("volumeCmd", volumeCmd);
        editor.putInt("changeVolumeDiff", changeVolumeDiff);
        editor.commit();

        DoLogin(view);
    }

    public void ShowAbout(View view){
        alexaViewer.loadData("<h1>Alexa Volume Control</h1><h2>Version: "+Build.VERSION.CODENAME+"</h2>","text/html","UTF-8");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }
}
