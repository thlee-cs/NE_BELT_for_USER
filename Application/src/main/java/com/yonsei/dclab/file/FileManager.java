package com.yonsei.dclab.file;

import android.os.Environment;
import android.text.format.Time;
import android.util.Log;

import com.yonsei.dclab.packet.Packet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Yeop_DCLab on 2017-08-23.
 */

public class FileManager {
    public static final String STRSAVEPATH = Environment.getExternalStorageDirectory()+"/NE BELT/";
    public String filename;
    public long startTimeMillis;
    public long updateTimeMillis;
    private int packetLookup = -1;

    public static final String TAG = "FileManager";

    public FileManager() {

    }

    public void createFile(String name) {
        Log.e(TAG,"creating File");
        File dir = makeDirectory(STRSAVEPATH);
//        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"NE BELT");
//        dir.mkdirs();
        Calendar c = Calendar.getInstance();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMdd'_'HHmmss");

        for(int i = 0; i < 1000; i++)    {
            //String fileNum = String.format("BIA%03d", i);
            filename = String.format("NE_" + dateFormat.format(c.getTime())+"_"+ name  +".csv", i);
            File file = new File(STRSAVEPATH+filename);
            if (isFileExist(file) == false) {
                makeFile(dir, (STRSAVEPATH+filename));
                startTimeMillis = System.currentTimeMillis();
                saveString(String.format("DATE TIME = %s\n", getStartTime2()));
                break;
            }
        }
    }

    public String getStartTime() {
        Time now = new Time();
        now.set(startTimeMillis);
        return now.format("%m-%d %H:%M");
    }

    public String getStartTime2() {
        Time now = new Time();
        now.set(startTimeMillis);
        return now.format("%Y-%m-%d %H:%M:%S");
    }

    public String getStorageTime() {
        long storageTime = updateTimeMillis - startTimeMillis;
        int seconds = (int)(storageTime / 1000) % 60 ;
        int minutes = (int)((storageTime / (1000*60)) % 60);
        int hours = (int)((storageTime / (1000*60*60)) % 24);
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    public long getFileSize() {
        File file = file = new File(STRSAVEPATH+filename);
        return file.length();
    }

    public void saveData(Packet packet, int NEventMarker, int BiaMarker) {//, String strVolume
        FileOutputStream fos;
        if(packetLookup != packet.seqNum){
            packetLookup = packet.seqNum;
            try {
                fos = new FileOutputStream((STRSAVEPATH+filename), true);
                Time now = new Time();
                now.set(System.currentTimeMillis());
                String text = "";
                text += String.format("\nSEQ=%d, NE=%d, Bia=%d", packet.seqNum, NEventMarker, BiaMarker);
                for (int i = 0; i < packet.rawData.get(0).size(); i++) {//rawData.get(x).size() == 64
                    text += String.format("\n%d, %d, %d", packet.rawData.get(0).get(i), packet.rawData.get(1).get(i), packet.rawData.get(2).get(i));
                }
                fos.write(text.getBytes());
                fos.close();
                updateTimeMillis = System.currentTimeMillis();
            }
            catch (IOException e) {
                Log.w(TAG, "saveData");
            }
        }
    }

    public void saveFile(float[] data) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream((STRSAVEPATH+filename), true);
            String text = "";
            for (int i = 0; i < data.length; i++) {
                text += String.valueOf((int) data[i]) + "\n";
            }
            fos.write(text.getBytes());
            fos.close();
        }
        catch (IOException e) {
            Log.d(TAG, "saveFile");
        }

    }

    public void saveString(String data) {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream((STRSAVEPATH+filename), true);
            String text = data;
            fos.write(text.getBytes());
            fos.close();
        }
        catch (IOException e) {
            Log.d(TAG, "saveString");
        }
    }

    public void insertString(String data) {
        BufferedReader reader = null;
        BufferedWriter writer = null;
        ArrayList list = new ArrayList();
        try {
            reader = new BufferedReader(new FileReader((STRSAVEPATH+filename)));
            String tmp;
            while ((tmp = reader.readLine()) != null) list.add(tmp);
            reader.close();

            list.add(0, data);

            writer = new BufferedWriter(new FileWriter((STRSAVEPATH+filename)));
            for (int i = 0; i < list.size(); i++) writer.write(list.get(i) + "\n");
            writer.close();
        }
        catch (IOException e) {
            Log.d(TAG, "insertString");
        }
    }

    private File makeDirectory(String dir_path){
        File dir = new File(dir_path);
        if (!dir.exists())
        {
            Log.d(TAG,"mkdir");
            boolean su = dir.mkdirs();
            if(su == true){
                Log.d(TAG,"success");
            }else if (su == false){
                Log.d(TAG,"fail");
            }else{
                Log.d(TAG,"I don't know anymore");
            }
        }else{
        }

        return dir;
    }

    private File makeFile(File dir , String file_path){
        File file = null;
        boolean isSuccess = false;
        if(dir.isDirectory()){
            file = new File(file_path);
            if(file!=null&&!file.exists()){
                try {
                    isSuccess = file.createNewFile();
                } catch (IOException e) {
                    Log.w(TAG,"failed create file");
                } finally{

                }
            }else{

            }
        }
        return file;
    }

    private String getAbsolutePath(File file){
        return ""+file.getAbsolutePath();
    }

    private boolean isFile(File file){
        boolean result;
        if(file!=null&&file.exists()&&file.isFile()){
            result=true;
        }else{
            result=false;
        }
        return result;
    }

    private boolean isDirectory(File dir){
        boolean result;
        if(dir!=null&&dir.isDirectory()){
            result=true;
        }else{
            result=false;
        }
        return result;
    }

    private boolean isFileExist(File file){
        boolean result;
        if(file!=null&&file.exists()){
            result=true;
        }else{
            result=false;
        }
        return result;
    }

    private boolean writeFile(File file , byte[] file_content){
        boolean result;
        FileOutputStream fos;
        if(file!=null&&file.exists()&&file_content!=null){
            try {
                fos = new FileOutputStream(file);
                try {
                    fos.write(file_content);
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {

            }
            result = true;
        }else{
            result = false;
        }
        return result;
    }


}