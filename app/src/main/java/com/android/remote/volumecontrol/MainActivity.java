package com.android.remote.volumecontrol;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
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
    private String roomIdFront = "";
    private String roomIdRear = "";

    //Settings to Store
    public Integer changeVolumeDiff = 0;
    public Integer keyPressWait = 0;
    public String alexaLoginUrl = "alexa.amazon.de";
    public String alexaBaseURI = "alexa.amazon.de";
    private String roomNameFront = "";
    private String roomNameRear = "";
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
                                        JSONObject objDeviceList = new JSONObject(html);
                                        deviceList = objDeviceList.getJSONArray("devices");

                                        JSONArray arrRoomDevices = null;
                                        for(int i=0; i<deviceList.length(); i++){
                                            JSONObject o = deviceList.getJSONObject(i);
                                            if(o.getString("accountName").equals(roomNameFront)){
                                                roomIdFront = o.getString("serialNumber") +";"+ o.getString("deviceType");
                                            } else if(o.getString("accountName").equals(roomNameRear)){
                                                roomIdRear = o.getString("serialNumber") +";"+ o.getString("deviceType");
                                            }

                                            if(roomIdFront != "" && roomIdRear != "")
                                                break;
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

    public void ChangeVolume(Integer volume,String type) {

        try{
            if(roomIdFront == null || roomIdFront == "" || volumeCmd == "")
                return;

            String jsonString = "";
            if(roomIdFront != null && roomIdFront != ""){
                String[] arr = roomIdFront.split(";");

                if(volumeCmd.contains("fixed")){
                    jsonString ="{\"type\":\"VolumeLevelCommand\",\"volumeLevel\":"+String.valueOf(volume)+",\"contentFocusClientId\":\"Default\"}";
                }else{
                    jsonString ="{\"type\":\"VolumeAdjustCommand\",\"volumeAdjustment\":"+String.valueOf(volume)+",\"contentFocusClientId\":\"Default\"}";
                }
                sendAlexaCommand(arr[0],arr[1],jsonString);
            }
            if(roomIdRear != null && roomIdRear != ""){
                String[] arr = roomIdRear.split(";");

                if(type.equals("inc") || type.equals("dec")){
                    Integer tmpVol = volume - changeVolumeDiff;
                    if(tmpVol <= 0)
                        volume = changeVolumeDiff;
                    else
                        volume = volume - changeVolumeDiff;
                }

                if(volumeCmd.contains("fixed")){
                    jsonString ="{\"type\":\"VolumeLevelCommand\",\"volumeLevel\":"+String.valueOf(volume)+",\"contentFocusClientId\":\"Default\"}";
                }else{
                    jsonString ="{\"type\":\"VolumeAdjustCommand\",\"volumeAdjustment\":"+String.valueOf(volume)+",\"contentFocusClientId\":\"Default\"}";
                }
                sendAlexaCommand(arr[0],arr[1],jsonString);
            }

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
        roomNameFront = settings.getString("roomNameFront", "WohnzimmerFront");
        roomNameRear = settings.getString("roomNameRear", "WohnzimmerRear");
        volumeCmd = settings.getString("volumeCmd","fixed");
        changeVolumeDiff = settings.getInt("changeVolumeDiff", 0);
        keyPressWait = settings.getInt("keyPressWait",250);

        EditText txtusername = (EditText)findViewById(R.id.editTextUsername);
        EditText txtpassword = (EditText)findViewById(R.id.editTextPassword);
        EditText txtRoomNameFront = (EditText)findViewById(R.id.editTextRoomNameFront);
        EditText txtroomNameRear = (EditText)findViewById(R.id.editTextRoomNameRear);
        EditText txtchangeVolumeDiff = (EditText)findViewById(R.id.editTextVolumeDiff);
        EditText txtKeyPressWait = (EditText)findViewById(R.id.editTextKeyPressWait);
        RadioButton rdVolumeFixed = (RadioButton)findViewById(R.id.radioButtonVolumeFixed);
        RadioButton rdVolumeAdjusted = (RadioButton)findViewById(R.id.radioButtonVolumeAdjusted);

        txtusername.setText(username);
        txtpassword.setText(password);
        txtRoomNameFront.setText(roomNameFront);
        txtroomNameRear.setText(roomNameRear);
        txtKeyPressWait.setText(String.valueOf(keyPressWait));
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
        EditText txtroomNameFront = (EditText)findViewById(R.id.editTextRoomNameFront);
        EditText txtroomNameRear = (EditText)findViewById(R.id.editTextRoomNameRear);
        EditText txtchangeVolumeDiff = (EditText)findViewById(R.id.editTextVolumeDiff);
        EditText txtKeyPressWait = (EditText)findViewById(R.id.editTextKeyPressWait);
        RadioButton rdVolumeFixed = (RadioButton)findViewById(R.id.radioButtonVolumeFixed);
        RadioButton rdVolumeAdjusted = (RadioButton)findViewById(R.id.radioButtonVolumeAdjusted);

        username = txtusername.getText().toString();
        password = txtpassword.getText().toString();
        roomNameFront = txtroomNameFront.getText().toString();
        roomNameRear = txtroomNameRear.getText().toString();
        changeVolumeDiff = Integer.valueOf(txtchangeVolumeDiff.getText().toString());
        keyPressWait = Integer.valueOf(txtKeyPressWait.getText().toString());
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
        editor.putString("roomNameFront", roomNameFront);
        editor.putString("roomNameRear", roomNameRear);
        editor.putString("volumeCmd", volumeCmd);
        editor.putInt("changeVolumeDiff", changeVolumeDiff);
        editor.putInt("keyPressWait", keyPressWait);
        editor.commit();

        DoLogin(view);
    }

    public void ShowAbout(View view){
        alexaViewer.loadData("<h1>Alexa Volume Control</h1><h2>Version: "+BuildConfig.VERSION_NAME+"</h2>","text/html","UTF-8");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getApplicationContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }
}
