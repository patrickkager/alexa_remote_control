package com.android.remote.volumecontrol;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AlexaCommands {

    private static final String TAG = "AlexaCommands";

    private static Boolean sendAlexaCommand(String DeviceSerial, String DeviceType,Integer Volume) {

        String volumeCmd = MyApp.getSettings().getString("volumeCmd","fixed");
        String Cookies = MyApp.getSettings().getString("Cookies","");
        String Csrf = MyApp.getSettings().getString("Csrf","");
        boolean debugOutPut = MyApp.getSettings().getBoolean("debugMode", false);

        try {
            String jsonString = "";
            if(volumeCmd.contains("fixed")){
                jsonString ="{\"type\":\"VolumeLevelCommand\",\"volumeLevel\":"+Volume+",\"contentFocusClientId\":\"Default\"}";
            }else{
                jsonString ="{\"type\":\"VolumeAdjustCommand\",\"volumeAdjustment\":"+Volume+",\"contentFocusClientId\":\"Default\"}";
            }

            URL url = new URL("https://"+MyApp.alexaBaseURI+"/api/np/command?deviceSerialNumber="+DeviceSerial+"&deviceType="+DeviceType);
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
                String alexPostResult = mes+"#"+conn.getResponseCode()+"<br/>";

                if(debugOutPut)
                    Log.d(TAG,mes);
            }

            return mes != null && !mes.isEmpty();

        }catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void ChangeVolume(Integer volume,String type) {

        float changeVolumeDiff = MyApp.getSettings().getFloat("changeVolumeDiff", 3.5F);
        float volumeBoost  = MyApp.getSettings().getFloat("volumeBoost",0);
        String volumeBoostType = MyApp.getSettings().getString("volumeBoostType", "Add");
        String volumeDiffType = MyApp.getSettings().getString("volumeDiffType", "Divide");
        String volumeBoostGroup = MyApp.getSettings().getString("volumeBoostGroup", "All");
        Map<String,String> frontDevices = MyApp.JsonToMap(MyApp.getSettings().getString("frontDevices", ""));
        Map<String,String> rearDevices =  MyApp.JsonToMap(MyApp.getSettings().getString("rearDevices", ""));

        try{
            //Volume Boost for All
            if(type.equals("inc") || type.equals("dec")) {
                if (volumeBoostGroup.equals("All")) {
                    if(volumeBoostType.equals("Add")){
                        volume = (int)Math.round(volume + volumeBoost);
                    }else if(volumeBoostType.equals("Sub")){
                        volume = (int)Math.round(volume - volumeBoost);
                    }else if(volumeBoostType.equals("Divide")){
                        volume = (int)Math.round(volume / volumeBoost);
                    } else if(volumeBoostType.equals("Mulitply")){
                        volume = (int)Math.round(volume * volumeBoost);
                    }

                    if(volume <= 0)
                        volume = 1;
                }

                if(volume > 100)
                    volume = 99;
            }

            //Front Devices
            //Volume Boost Only for Front
            Integer volumeFront = volume;
            if(type.equals("inc") || type.equals("dec")) {
                if (volumeBoostGroup.equals("Front")) {
                    if(volumeBoostType.equals("Add")){
                        volumeFront = (int)Math.round(volumeFront + volumeBoost);
                    }else if(volumeBoostType.equals("Sub")){
                        volumeFront = (int)Math.round(volumeFront - volumeBoost);
                    }else if(volumeBoostType.equals("Divide")){
                        volumeFront = (int)Math.round(volumeFront / volumeBoost);
                    } else if(volumeBoostType.equals("Mulitply")){
                        volumeFront = (int)Math.round(volumeFront * volumeBoost);
                    }

                    if(volumeFront <= 0)
                        volumeFront = 1;
                }

                if(volumeFront > 100)
                    volumeFront = 99;
            }

            for (Map.Entry<String, String> entry : frontDevices.entrySet()) {
                Integer finalVolume = volumeFront;
                new Thread(() -> sendAlexaCommand(entry.getKey() , entry.getValue(), finalVolume)).start();
            }

            //Rear Devices
            //Volume Difference to front
            Integer volumeRear = volume;
            if(type.equals("inc") || type.equals("dec")){
                if(volumeDiffType.equals("Add")){
                    volumeRear = (int)Math.round(volumeRear + changeVolumeDiff);
                }else if(volumeDiffType.equals("Sub")){
                    volumeRear = (int)Math.round(volumeRear - changeVolumeDiff);
                } else if(volumeDiffType.equals("Divide")){
                    volumeRear = (int)Math.round(volumeRear / changeVolumeDiff);
                } else if(volumeDiffType.equals("Mulitply")){
                    volumeRear = (int)Math.round(volumeRear * changeVolumeDiff);
                }

                if(volumeRear < 0)
                    volumeRear = 1;

                if(volumeRear > 100)
                    volumeRear = 99;
            }

            for (Map.Entry<String, String> entry : rearDevices.entrySet()) {
                Integer finalVolume = volumeRear;
                new Thread(() -> sendAlexaCommand(entry.getKey() , entry.getValue(), finalVolume)).start();
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}