package com.android.remote.volumecontrol;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class AlexaCommands {

    private static Boolean sendAlexaCommand(String DeviceSerial, String DeviceType,Integer Volume) {

        try {
            String jsonString = "";
            if(MainActivity.getInstance().volumeCmd.contains("fixed")){
                jsonString ="{\"type\":\"VolumeLevelCommand\",\"volumeLevel\":"+Volume+",\"contentFocusClientId\":\"Default\"}";
            }else{
                jsonString ="{\"type\":\"VolumeAdjustCommand\",\"volumeAdjustment\":"+Volume+",\"contentFocusClientId\":\"Default\"}";
            }

            URL url = new URL("https://"+MainActivity.alexaBaseURI+"/api/np/command?deviceSerialNumber="+DeviceSerial+"&deviceType="+DeviceType);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Cookie",MainActivity.getInstance().Cookies);
            conn.setRequestProperty("csrf",MainActivity.getInstance().Csrf);
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

                if(MainActivity.getInstance().debugOutPut)
                    Log.d(MainActivity.getInstance().TAG,mes);
            }

            return mes != null && !mes.isEmpty();

        }catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void ChangeVolume(Integer volume,String type) {

        try{
            //Volume Boost for All
            if(type.equals("inc") || type.equals("dec")) {
                if (MainActivity.getInstance().volumeBoostGroup.equals("All")) {
                    if(MainActivity.getInstance().volumeBoostType.equals("Add")){
                        volume = (int)Math.round(volume + MainActivity.getInstance().volumeBoost);
                    }else if(MainActivity.getInstance().volumeBoostType.equals("Sub")){
                        volume = (int)Math.round(volume - MainActivity.getInstance().volumeBoost);
                    }else if(MainActivity.getInstance().volumeBoostType.equals("Divide")){
                        volume = (int)Math.round(volume / MainActivity.getInstance().volumeBoost);
                    } else if(MainActivity.getInstance().volumeBoostType.equals("Mulitply")){
                        volume = (int)Math.round(volume * MainActivity.getInstance().volumeBoost);
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
                if (MainActivity.getInstance().volumeBoostGroup.equals("Front")) {
                    if(MainActivity.getInstance().volumeBoostType.equals("Add")){
                        volumeFront = (int)Math.round(volumeFront + MainActivity.getInstance().volumeBoost);
                    }else if(MainActivity.getInstance().volumeBoostType.equals("Sub")){
                        volumeFront = (int)Math.round(volumeFront - MainActivity.getInstance().volumeBoost);
                    }else if(MainActivity.getInstance().volumeBoostType.equals("Divide")){
                        volumeFront = (int)Math.round(volumeFront / MainActivity.getInstance().volumeBoost);
                    } else if(MainActivity.getInstance().volumeBoostType.equals("Mulitply")){
                        volumeFront = (int)Math.round(volumeFront * MainActivity.getInstance().volumeBoost);
                    }

                    if(volumeFront <= 0)
                        volumeFront = 1;
                }

                if(volumeFront > 100)
                    volumeFront = 99;
            }

            for (Map.Entry<String, String> entry : MainActivity.getInstance().frontDevices.entrySet()) {
                Integer finalVolume = volumeFront;
                new Thread(() -> sendAlexaCommand(entry.getKey() , entry.getValue(), finalVolume)).start();
            }

            //Rear Devices
            //Volume Difference to front
            Integer volumeRear = volume;
            if(type.equals("inc") || type.equals("dec")){
                if(MainActivity.getInstance().volumeDiffType.equals("Add")){
                    volumeRear = (int)Math.round(volumeRear + MainActivity.getInstance().changeVolumeDiff);
                }else if(MainActivity.getInstance().volumeDiffType.equals("Sub")){
                    volumeRear = (int)Math.round(volumeRear - MainActivity.getInstance().changeVolumeDiff);
                } else if(MainActivity.getInstance().volumeDiffType.equals("Divide")){
                    volumeRear = (int)Math.round(volumeRear / MainActivity.getInstance().changeVolumeDiff);
                } else if(MainActivity.getInstance().volumeDiffType.equals("Mulitply")){
                    volumeRear = (int)Math.round(volumeRear * MainActivity.getInstance().changeVolumeDiff);
                }

                if(volumeRear < 0)
                    volumeRear = 1;

                if(volumeRear > 100)
                    volumeRear = 99;
            }

            for (Map.Entry<String, String> entry : MainActivity.getInstance().rearDevices.entrySet()) {
                Integer finalVolume = volumeRear;
                new Thread(() -> sendAlexaCommand(entry.getKey() , entry.getValue(), finalVolume)).start();
            }

        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}