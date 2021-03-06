/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.neptuNE;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.ActivityCompat;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;
import com.yonsei.dclab.OSEAFactory;
import com.yonsei.dclab.file.DeviceSetter;
import com.yonsei.dclab.file.FileManager;
import com.yonsei.dclab.packet.Packet;
import com.yonsei.dclab.packet.PacketParser;
import com.yonsei.dclab.processing.QRSDetector2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bolts.Task;

//import com.yonsei.dclab.chart.BIA_Chart;
//import com.yonsei.dclab.chart.ECG_Chart;
//import com.yonsei.dclab.chart.Moi_Chart;

/**
 * 디바이스와 연결 된 뒤에 실행되는 메인 클래스
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    int sampleRate = 256;
    private boolean isCommunicated = false;

    String version_num = "  v1.67";
    String patient_num = null;

    public static final String EXTRAS_DEVICE_NAME = "NE_BELT";
    public static final String EXTRAS_DEVICE_ADDRESS = "98:2D:68:2D:60:00";
    public static int reconnectFlags = 0;

    //MetaWear
    private static String[] deviceUUIDs = {"", ""};//"FD:0F:59:E2:F4:C5" "D4:25:5C:D6:2E:F5"
    private BtleService.LocalBinder serviceBinder;

    //data catch map for accel & gyro scope
    private Map<String, Accelerometer> accelerometerSensors = new HashMap<>();
    private Map<String, GyroBmi160> gyroSensors = new HashMap<>();

    private Map<String, TextView> sensorOutputs = new HashMap<>();
    private Map<String, TextView> gyrosensorOutputs = new HashMap<>();

    CountDownTimer cTimer = null;
    public int DisconnectCounter = 0;
    public int DisconnectThreshold = 8;
    private int reconnect_call = 1000;

    public int SeqCounter = 0;
    public int IntervalCounter = 0;
    public int IntervalIndex = -1;
    public int IntervalBuf = 0;
    public int SeqLossThreshold = 8;

    public int LastSeqNum = -2;
    public int NowSeqNum = -1;
    public DeviceSetter mdevicesetter;
    private Route streamRoute;
    private int mfile_Num = 0;
    private TextView mSaveView;
    private Button mSaveButton;
    private Button mStartButton;
    private Button mUploadButton;
    //    private ImageButton mHomePageButton;
    private Button mOffNeAlarmButton;

    private String mDeviceName;
    private String mDeviceAddress;

    private TextView mTextView_Heartrate;
    private TextView mTextView_BodyImpedance;

    private TextView mTextView_Leftfoot;
    private TextView mTextView_Rightfoot;
    private Handler mUpdateDataHandler;

    private Handler mRotationHandler;
    private Handler startSeqHandler;

    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private boolean mConnected = false;
    private boolean mMetaWearConnected = false;
    private FileManager mFileManager = new FileManager();
    private PacketParser mPacketParser;
    private Vibrator vibe;
    private SoundPool sound;
    private int music;
    private int NE_event;
    private int ne_event_lock = 0;
    private int minute_now = 60;
    private int start_state = 0;
    private int start_button_counter = 0;
    private String getHz_l_a = "";
//    private String getHz_l_g = "";
//    private String getHz_r_a = "";
//    private String getHz_r_g = "";


    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic_W;
    private BluetoothGattCharacteristic mNotifyCharacteristic_C;

    private ArrayList<Integer> mBiaDataList = new ArrayList<Integer>();
    private ArrayList<Integer> mEcgDataList = new ArrayList<Integer>();
    private ArrayList<Integer> mMoiDataList = new ArrayList<Integer>();
    private ArrayList<Integer> RR_buf = new ArrayList<Integer>();
    private int MULDataListSize = 64;

    // 256Hz(64*4), 20sec (10*2)
    private int BiaDataListSize = 64 * 4 * 10 * 2;
    private int EcgDataListSize = 64 * 4 * 10 * 2;
    private int MoiDataListSize = 64 * 4 * 10 * 2;

    private String Posture = "NA";
    private int NEventMarker = 0;
    private int BiaMarker = 0;
    private int Heartrate = 0;

    private String ChargeStatus;
    private int BatteryStatus = -1;

    private int bell_max = 5;
    private int bell_min = 8150;

    //    private int rot_state = 0;
//    private int rot_st;
//    private int rot_ed;
    private int gain_st = (byte) 0x01;
    private int gain_ed = (byte) 0x07;

    private TextView mNow_state;
    private TextView mNow_guide;

    private int[] biaDataArray = new int[BiaDataListSize];
    private int[] ecgDataArray = new int[EcgDataListSize];
    private int[] moiDataArray = new int[MoiDataListSize];


    private static String[] STORAGE_PERMISSION = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * Code to manage Service lifecycle.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.i(TAG, "Main service connect");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final ServiceConnection meta_ServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "meta service connect");
            serviceBinder = (BtleService.LocalBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
    /**
     * BLE의 상태에 따라 지시를 내려주는 함수
     * <p>
     * BLE 연결됨
     * BLE 끊김
     * BLE 찾음--> GATT서비스를 설정
     * BLE 데이터를 받음 --> 패킷을 뜯고 화면에 출력함
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) { //연결됨
                mFileManager.saveLogData("mGattUpdateReceiver", "NEWear connected:" + mDeviceAddress);
                mConnected = true;
                DisconnectCounter = 0;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { // 연결 끊김
                mFileManager.saveLogData("mGattUpdateReceiver", "NEWear disconnected:" + mDeviceAddress);
                mConnected = false;
                invalidateOptionsMenu();
                Log.e(TAG, "DIsconnected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) { //BLE를 찾은 뒤
                // Show all the supported services and characteristics on the user interface.
                mFileManager.saveLogData("mGattUpdateReceiver", "NEWear discovered" + mDeviceAddress);
                getGattServices(mBluetoothLeService.getSupportedGattServices());//GATT설정
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) { // 데이터가 교환중임
                isCommunicated = true;
//                Log.d(TAG, String.format("MyDEBUG: mGattUpdateReceiver[2] = %s", action));
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.ACTION_DATA_AVAILABLE); //교환된 데이터를 받음

                if (data != null) { //교환된 데이터가 존재할때
//                    Log.d(TAG, String.format("MyDEBUG: mGattUpdateReceiver[3] = %s", action));
                    try { //연결이 끊길때 수행하면 에러가 날 수 있으므로 try로 전환함
                        if (false) {
                            Log.d(TAG, String.format("DATA_AVAILABLE = %d", data.length));
                            final StringBuilder stringBuilder = new StringBuilder(data.length);
                            for (byte byteChar : data)
                                stringBuilder.append(String.format("%02X ", byteChar));
                            Log.d(TAG, "DATA_AVAILABLE = " + stringBuilder.toString());
                        }

                        //패킷을 뜯음
                        mPacketParser.add(data, 0, data.length);
                        //데이터에 뜯어진 패킷을 덮어씌움
                        data = mPacketParser.get();
                        //앱에서 만든 패킷 클래스에 다시 저장함
                        Packet packet_v0 = new Packet(mDeviceName, mDeviceAddress, data);

                        NowSeqNum = packet_v0.seqNum;
                        if (packet_v0.isMULdata()) {//받은 패킷이 3채널 데이터일때 아래 함수 실행
//                            Log.e(TAG,"Get the data");
                            receivedData(packet_v0);
                        }

                    } catch (Exception e) {

                    }
                }
            }
        }
    };

    /**
     * 버튼이 눌리면 반응하는 함수
     * start_button : 시작
     * bia_rot : bia interval을 시작
     */

    private long oneTimePressed;
    private long twicePressed;

    Button.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) { // 아이템의 ID로 식별함
                case R.id.start_button: // Start버튼이 눌리면, 기기로부터 얻
                    if (System.currentTimeMillis() - backPressed < 5000) {
                        backtwicePressed = System.currentTimeMillis();
                        if (System.currentTimeMillis() - backtwicePressed < 5000) {
                            backthirdPressed = System.currentTimeMillis();
                            Log.d(TAG, "Upload start");
                            upload();
                            if (System.currentTimeMillis() - backthirdPressed < 5000) {
                                setResult(RESULT_OK, null);
                                finish();
                            }
                        }
                    }

                    Toast.makeText(DeviceControlActivity.this, "5초 이내에 3번 클릭하시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
                    backPressed = System.currentTimeMillis();
                    vibe.vibrate(40);
//                    Log.d(TAG, "start");
//                    start_sequence();
//                    if (deviceUUIDs[0] != null) {
//                        SystemClock.sleep(1000);
//                        connectToMetawear(deviceUUIDs[0]);
//                        SystemClock.sleep(1000);
//                        connectToMetawear(deviceUUIDs[1]);
//                    }
//                    if (start_button_counter == 0) {
//                        Toast.makeText(getApplicationContext(), "시작 버튼이 눌렸어요", Toast.LENGTH_LONG).show();
//                        start_button_counter = 1;
//                    }
                    break;

                case R.id.ne_upload:
                    if (System.currentTimeMillis() - oneTimePressed < 5000) {
                        twicePressed = System.currentTimeMillis();
                        if (System.currentTimeMillis() - twicePressed < 5000) {
                            Log.d(TAG, "Upload start");
                            Toast.makeText(getApplicationContext(), "데이터가 삭제됩니다.", Toast.LENGTH_LONG).show();
                            mFileManager.saveLogData("mClickListener", "Uploading data");
//                    mFileManager.uploadAll();
                            SystemClock.sleep(5000);
                            mFileManager.delete();
                        }
                    }
                    Toast.makeText(DeviceControlActivity.this, "5초 이내에 3번 클릭하시면 삭제됩니다.", Toast.LENGTH_SHORT).show();
                    oneTimePressed = System.currentTimeMillis();
                    break;

//                case R.id.homepage_img:
//                    goToUrl ( "http://dclab.yonsei.ac.kr/neptune/");
//                    vibe.vibrate(40);
//                    break;
                case R.id.ne_alram:
                    if (NE_event == 1) {
                        // sound.stop(music);
                        // sound.release();
                        sound.autoPause();
                        vibe.cancel();
                        NE_event = 0;// 0 = normal state
                        ne_event_lock = 1;
                        minute_now = mFileManager.getMinute();
                        Toast.makeText(getApplicationContext(), "알람이 꺼졌어요", Toast.LENGTH_LONG).show();
                        mFileManager.saveLogData("mClickListener", "Alaram stop button");
                        mOffNeAlarmButton.setBackgroundColor(Color.LTGRAY);
                        vibe.vibrate(40);
                    } else {
                        Toast.makeText(getApplicationContext(), "알람이 울릴때 눌려주세요", Toast.LENGTH_LONG).show();
                        vibe.vibrate(40);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 받은 데이터를 처리하는 함수
     * 스트리밍이 될때마다 호출이 되며
     * packet의 속도가 1/4초에 1번씩 보내므로 1/4초마다 한번씩 실행됨
     */
    public void receivedData(Packet packet) {
        if (mFileManager.getHours() == 1 || mfile_Num == 0) {
            //Battery part
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);

            // Are we charging / charged?
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            // How are we charging?
            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level / (float) scale;
            BatteryStatus = (int) batteryPct * 100;

            if (isCharging == true) {
                if (usbCharge == true) {
                    ChargeStatus = "U";
                } else if (acCharge == true) {
                    ChargeStatus = "A";
                }
            } else {
                ChargeStatus = "N";
            }

            if (mfile_Num != 0) {
                upload();
            } else if (mfile_Num == 0) {
                mFileManager.uploadLogFile();
            }
            mFileManager.saveLogData("receivedData", "Try create File");
            mFileManager.createLogFile(patient_num, String.valueOf(mfile_Num));
            mFileManager.createFile(patient_num, String.valueOf(mfile_Num), ChargeStatus, String.valueOf(BatteryStatus));
            mFileManager.createMoFile(patient_num, String.valueOf(mfile_Num), ChargeStatus, String.valueOf(BatteryStatus));
            mfile_Num++;
        }
        int fileKb = (int) (mFileManager.getFileSize() / 1000);
        ImageView hrimg = (ImageView) findViewById(R.id.heart_img);
        ImageView biamg = (ImageView) findViewById(R.id.bia_img);
        hrimg.setImageResource(R.drawable.ne_heart);
        biamg.setImageResource(R.drawable.ne_bia);

        //패킷에서 얻은 데이터를 List로 생성함
        for (int i = 0; i < MULDataListSize; i++) {
            mEcgDataList.add(packet.rawData.get(0).get(i));
            mBiaDataList.add(packet.rawData.get(1).get(i));
            mMoiDataList.add(packet.rawData.get(2).get(i));
        }
        //데이터의 frequency에 맞지 않게 들어온 경우 삭제함(20초 기준)
        while (mEcgDataList.size() > EcgDataListSize) {
            mBiaDataList.remove(0);
            mEcgDataList.remove(0);
            mMoiDataList.remove(0);
        }
        //얻은 List를 20초 간 활성화 시킴
        if (mEcgDataList.size() >= EcgDataListSize) { //초기 20초 이전에 돌아가는 함수
            for (int i = 0; i < EcgDataListSize; i++) {// 10sec
                biaDataArray[i] = mBiaDataList.get(i);
                ecgDataArray[i] = mEcgDataList.get(i);
                moiDataArray[i] = mMoiDataList.get(i);
            }
        } else { //20초 이후에 돌아가는 함수
            for (int i = 0; i < mEcgDataList.size(); i++) {
                biaDataArray[i] = mBiaDataList.get(i);
                ecgDataArray[i] = mEcgDataList.get(i);
                moiDataArray[i] = mMoiDataList.get(i);
            }
        }

        int tempCounter = 0;
        int tempMeanCounter = 0;
        if (moiDataArray.length > (15 * 256)) { //mainly moiDataArray.length = 256 * 20
            for (int i = moiDataArray.length; i > (5 * 256); i = i - (3 * 128)) {
                int[] tempMoi = Arrays.copyOfRange(moiDataArray, i - (3 * 128), i);
                double tempMean = getMean(tempMoi);
                double tempStd = getStd(tempMoi);
                if ((tempStd < bell_max) && (fileKb > 100)) {//&& tempMean > 8000 && tempMean < 8500
                    if (tempMean > 8000 && tempMean < 8500){
                        tempMeanCounter += 1;
                    }
                    tempCounter += 1;
                }
            }
        }
        if ( tempCounter > 8) {// && tempMeanCounter > 2
            if (NE_event == 0 && ne_event_lock == 0) {
                vibe.vibrate(100000); //If NE event detected vibrate
                music = sound.load(this, R.raw.sample, 1);
                sound.play(music, 1, 1, 0, -1, 1);
                sound.autoResume();
                NE_event = 1; //Mark the event
                NEventMarker = 1;
                try {
                    mFileManager.uploadFile();
                    mFileManager.uploadMoFile();
                    mFileManager.uploadLogFile();
                } catch (Exception e) {
                    Log.e(TAG, "Upload Failed");
                    mFileManager.saveLogData("receivedData-NE", "Save failed");
                }
                mNow_state.setText("> 야뇨가 감지되었습니다");
                mFileManager.saveLogData("receivedData", "NE Detected");
                mNow_guide.setText("\n\n1. 알람을 끄기위해 화면 중앙에 위치한 '알람 끄기' 버튼을 눌러주세요\n\n2.아이를 화장실로 데려가 잔뇨를 볼 수 있도록 도와주세요");
                mOffNeAlarmButton.setBackgroundColor(Color.RED);
            }
        }

//        //알람이 울림 128*2*20
//        int[] temp1 = Arrays.copyOfRange(moiDataArray, 128 * 34, 128 * 37);
//        int[] temp2 = Arrays.copyOfRange(moiDataArray, 128 * 37, moiDataArray.length);
//        double std1 = getStd(temp1);
//        double std2 = getStd(temp2);
//        if ((std1 < bell_max) && (fileKb > 100)) {
//            if (std2 < bell_max) {
//                if (NE_event == 0 && ne_event_lock == 0) {
//                    vibe.vibrate(100000); //If NE event detected vibrate
//                    music = sound.load(this, R.raw.sample, 1);
//                    sound.play(music, 1, 1, 0, -1, 1);
//                    sound.autoResume();
//                    NE_event = 1; //Mark the event
//                    NEventMarker = 1;
//                    try {
//                        mFileManager.uploadFile();
//                        mFileManager.uploadMoFile();
//                        mFileManager.uploadLogFile();
//                    } catch (Exception e) {
//                        Log.e(TAG, "Upload Failed");
//                        mFileManager.saveLogData("receivedData-NE", "Save failed");
//                    }
//                    mNow_state.setText("> 야뇨가 감지되었습니다");
//                    mFileManager.saveLogData("receivedData", "NE Detected");
//                    mNow_guide.setText("\n\n1. 알람을 끄기위해 화면 중앙에 위치한 '알람 끄기' 버튼을 눌러주세요\n\n2.아이를 화장실로 데려가 잔뇨를 볼 수 있도록 도와주세요");
//                    mOffNeAlarmButton.setBackgroundColor(Color.RED);
//                }
//            }
//        }

//        for (int i = 37; i < 40; i++) {
//            int[] temp = Arrays.copyOfRange(moiDataArray, 128 * i, moiDataArray.length);
//            double std = getStd(temp);
//            if (std < bell_max) {
//                if (NE_event == 0 && ne_event_lock == 0) {
//                    vibe.vibrate(100000); //If NE event detected vibrate
//                    music = sound.load(this, R.raw.sample, 1);
//                    sound.play(music, 1, 1, 0, -1, 1);
//                    sound.autoResume();
//                    NE_event = 1; //Mark the event
//                    NEventMarker = 1;
//                    try {
//                        mFileManager.uploadFile();
//                        mFileManager.uploadMoFile();
//                        mFileManager.uploadLogFile();
//                    } catch (Exception e) {
//                        Log.e(TAG, "Upload Failed");
//                        mFileManager.saveLogData("receivedData-NE", "Save failed");
//                    }
//                    mNow_state.setText("> 야뇨가 감지되었습니다");
//                    mFileManager.saveLogData("receivedData", "NE Detected");
//                    mNow_guide.setText("\n\n1. 알람을 끄기위해 화면 중앙에 위치한 '알람 끄기' 버튼을 눌러주세요\n\n2.아이를 화장실로 데려가 잔뇨를 볼 수 있도록 도와주세요");
//                    mOffNeAlarmButton.setBackgroundColor(Color.RED);
//                }
//
//            }

            //Set Urine bell
        for (int i = 37; i < 40; i++) {
            if (moiDataArray[128 * i] <= bell_max && moiDataArray[128 * i] >= bell_min) {
                if (moiDataArray[128 * i + 64] <= bell_max && moiDataArray[128 * i + 64] >= bell_min) { //Ring alert when moiData range is shorted
                    if (NE_event == 0 && ne_event_lock == 0) {
                        vibe.vibrate(100000); //If NE event detected vibrate
                        music = sound.load(this, R.raw.sample, 1);
                        sound.play(music, 1, 1, 0, -1, 1);
                        sound.autoResume();
                        NE_event = 1; //Mark the event
                        NEventMarker = 1;
                        try {
                            mFileManager.uploadFile();
                            mFileManager.uploadMoFile();
                            mFileManager.uploadLogFile();
                        } catch (Exception e) {
                            Log.e(TAG, "Upload Failed");
                            mFileManager.saveLogData("receivedData-NE", "Save failed");
                        }
                        mNow_state.setText("> 야뇨가 감지되었습니다");
                        mFileManager.saveLogData("receivedData", "NE Detected");
                        mNow_guide.setText("\n\n1. 알람을 끄기위해 화면 중앙에 위치한 '알람 끄기' 버튼을 눌러주세요\n\n2.아이를 화장실로 데려가 잔뇨를 볼 수 있도록 도와주세요");
                        mOffNeAlarmButton.setBackgroundColor(Color.RED);
                    }
                }
            }
        }

        //Detect qrs pulse
        QRSDetector2 qrsDetector = OSEAFactory.createQRSDetector2(sampleRate);
        int beat_temp_buf = 0;
        int hr = 0;
        //make HR_buf

        for (int i = 0; i < ecgDataArray.length; i++) {
            int result = qrsDetector.QRSDet(ecgDataArray[i]);
            if (result != 0) {
                beat_temp_buf = (i - result) - beat_temp_buf;
                hr = (60 * sampleRate) / beat_temp_buf;
                if (hr > 40 && hr < 200) {
                    RR_buf.add(hr);
                }
                ecgDataArray[i - result] = 0;
            }
        }

        //get the HR
        int buf_size = RR_buf.size();
        if (RR_buf.size() > 3) {
            int sum = 0;
            int count = 0;
            for (int i = 1; i < buf_size; i++) {
                sum += RR_buf.get(i);
                count += 1;
            }
            Heartrate = (int) sum / count;
            mTextView_Heartrate.setText(String.format("심박 측정중"));
            RR_buf.clear();
        }

        //화면에 BIA, MOI 패킷의 맨 마지막을 출력함 1/4초마다 출력됨
        if (mTextView_BodyImpedance != null)
            mTextView_BodyImpedance.setText(String.format("임피던스: " + "%d", packet.rawData.get(1).get(MULDataListSize - 1)));

        if (NE_event == 0 && (packet.rawData.get(1).get(MULDataListSize - 1) != null)) {
            mNow_state.setText("> 정상적으로 연결되어 측정 중입니다");
            mNow_guide.setText("\n\n1. 아이가 올바르게 기기를 착용하고 자도록 지도해주세요 \n\n2. 스마트폰을 충전기와 연결하여 사용하는것을 권장합니다");
        }
        mFileManager.saveData(packet, NEventMarker, BiaMarker, Heartrate, Posture);
        NEventMarker = 0;
        mSaveView.setText((mfile_Num - 1) + "h" + mFileManager.getStorageTime());

        if ((mFileManager.getMinute() != minute_now) && ((mFileManager.getMinute() % 1) == 0)) {
            ne_event_lock = 0;
            //music = sound.load(this, R.raw.sample, 1);
        }

    }

    private double getStd(int[] data) {
        double sum = 0.0;
        double temp = 0.0;
        double mean;

        for (double number : data) {
            sum += number;
        }
        mean = sum / data.length;


        for (double number : data) {
            temp += (number - mean) * (number - mean);
        }
        return Math.sqrt(temp / (data.length - 1));
    }

    private double getMean(int[] data) {
        double sum = 0.0;
        double temp = 0.0;
        double mean;

        for (double number : data) {
            sum += number;
        }
        return sum / data.length;
    }

    /**
     * BLE GATT 설정하는 함수
     */
    private void getGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG, String.format("BluetoothGattService = %s", gattService.getUuid().toString()));
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                if (gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_READ)) {
                    Log.d(TAG, "read_checked");
                    mNotifyCharacteristic = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                } else if (gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_FLOW_CTRL)) {
                    Log.d(TAG, "ctrl_checked");
                    mNotifyCharacteristic_C = gattCharacteristic;
                } else if (gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_WRITE)) {
                    Log.d(TAG, "write_checked");
                    mNotifyCharacteristic_W = gattCharacteristic;
                }
            }
        }
    }

    //start timer function
    void startTimer() {
        cTimer = new CountDownTimer(5000, 1000) {
            TextView timer = (TextView) findViewById(R.id.start_time_count);

            public void onTick(long millisUntilFinished) {
                if (!isCommunicated) {
                    timer.setText((millisUntilFinished / 1000) + "초 후 시작");
                }
            }

            public void onFinish() {
                if (!isCommunicated) {
                    timer.setText("이제   시작");
                    start_sequence();
//                    SystemClock.sleep(5000);
                }
                if (deviceUUIDs[0] != null) {
                    SystemClock.sleep(1000);
                    connectToMetawear(deviceUUIDs[0]);
                    SystemClock.sleep(1000);
                    connectToMetawear(deviceUUIDs[1]);
                }
                SystemClock.sleep(100);
                startSeqHandler.postDelayed(reconnectMethod, 0);
                cancelTimer();
            }
        };
        cTimer.start();
    }


    //cancel timer
    void cancelTimer() {
        if (cTimer != null)
            cTimer.cancel();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.gatt_services_characteristics);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        final Intent intent = getIntent();
        reconnectFlags = intent.getIntExtra("reconnect_flag", 0);
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //set by last 2 character of device address
        String mIndexId = mDeviceAddress.substring(mDeviceAddress.length() - 2);
        mdevicesetter = new DeviceSetter();
        mdevicesetter.setter(mIndexId);
        patient_num = mdevicesetter.getPatientNum();
        deviceUUIDs = new String[]{mdevicesetter.getLeftNum(), mdevicesetter.getRightNum()};
//        bell_max = mdevicesetter.getAlarmThreshold() + 50;
//        bell_min = mdevicesetter.getAlarmThreshold() - 50;
        ActivityCompat.requestPermissions(DeviceControlActivity.this, STORAGE_PERMISSION, 1);

        mFileManager.createLogFile(patient_num, "init");

        sound = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        music = sound.load(this, R.raw.sample, 1);
        NE_event = 0;

        vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mTextView_Rightfoot = (TextView) findViewById(R.id.right_foot);
        mTextView_Leftfoot = (TextView) findViewById(R.id.left_foot);

        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(mClickListener);

        mUploadButton = (Button) findViewById(R.id.ne_upload);
        mUploadButton.setOnClickListener(mClickListener);

        mOffNeAlarmButton = (Button) findViewById(R.id.ne_alram);
        mOffNeAlarmButton.setOnClickListener(mClickListener);

//        mHomePageButton = (ImageButton) findViewById(R.id.homepage_img);
//        mHomePageButton.setOnClickListener(mClickListener);

        mTextView_BodyImpedance = (TextView) findViewById(R.id.bia_view);
        mTextView_Heartrate = (TextView) findViewById(R.id.hr_view);
        mSaveView = (TextView) findViewById(R.id.start_time_count);
        mNow_state = (TextView) findViewById(R.id.now_state);
        mNow_guide = (TextView) findViewById(R.id.now_guide);
        getActionBar().setTitle("NETcher - 우리 아이 야뇨 측정");//+ version_num
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        getApplicationContext().bindService(new Intent(this, BtleService.class), meta_ServiceConnection, BIND_AUTO_CREATE);


        mPacketParser = new PacketParser();
//        mFileManager.createFile(patient_num, String.valueOf(mfile_Num),ChargeStatus, String.valueOf(BatteryStatus));
//        mFileManager.createMoFile(patient_num, String.valueOf(mfile_Num),ChargeStatus, String.valueOf(BatteryStatus));
        Arrays.fill(biaDataArray, 0);
        Arrays.fill(ecgDataArray, 0);
        Arrays.fill(moiDataArray, 0);
        mRotationHandler = new Handler();
        startSeqHandler = new Handler();
        startTimer();
//        startSeqHandler.postDelayed(startMethod,10000);


        /**
         * left metawear setting
         */
        String left_device = deviceUUIDs[0];
        Switch metawearSwitch_l = (Switch) findViewById(R.id.switchMetawear_l);
        TextView sensorOutput_l = (TextView) findViewById(R.id.txtSensorOutput_l);
        TextView gyrosensorOutput_l = (TextView) findViewById(R.id.txtGyroSensorOutput_l);

        sensorOutputs.put(left_device, sensorOutput_l);
        gyrosensorOutputs.put(left_device, gyrosensorOutput_l);

        metawearSwitch_l.setOnCheckedChangeListener((compoundButton, enable) -> {
            if (enable) {
//                connectToMetawear(left_device);
            } else {
//                stopAccelerometer(left_device);
//                stopGyro(left_device);
            }
        });

        metawearSwitch_l.setText("Device " + left_device);
        sensorOutput_l.setText("- -");
        gyrosensorOutput_l.setText("- -");

        /**
         * right metawear setting
         */

        String right_device = deviceUUIDs[1];
        Switch metawearSwitch_r = (Switch) findViewById(R.id.switchMetawear_r);
        TextView sensorOutput_r = (TextView) findViewById(R.id.txtSensorOutput_r);
        TextView gyrosensorOutput_r = (TextView) findViewById(R.id.txtGyroSensorOutput_r);

        sensorOutputs.put(right_device, sensorOutput_r);
        gyrosensorOutputs.put(right_device, gyrosensorOutput_r);

        metawearSwitch_r.setOnCheckedChangeListener((compoundButton, enable) -> {
            if (enable) {
//                connectToMetawear(right_device);
            } else {
//                stopAccelerometer(right_device);
//                stopGyro(right_device);
            }
        });
        metawearSwitch_r.setText("Device " + right_device);
        sensorOutput_r.setText("- -");
        gyrosensorOutput_r.setText("- -");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //Stop Handler
        try {
            mFileManager.saveLogData("ondestroy", "end");

            stopAccelerometer(deviceUUIDs[0]);
            stopAccelerometer(deviceUUIDs[1]);
            stopGyro(deviceUUIDs[0]);
            stopGyro(deviceUUIDs[1]);

            //Unbind BLE intent
            unregisterReceiver(mGattUpdateReceiver);
            unbindService(mServiceConnection);

            //Disable bluetooth
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
            if (biaMethodFlag == 1) {
                mRotationHandler.removeCallbacks(biaONrotationMethod);
            } else if (biaMethodFlag == -1) {
                mRotationHandler.removeCallbacks(biaOFFrotationMethod);
            }
            if (recMethodFlag == 1) {
                startSeqHandler.removeCallbacks(reconnectMethod);
            } else if (recMethodFlag == -1) {
                startSeqHandler.removeCallbacks(startMethod);
            }
        } catch (Exception e) {

        }
        mBluetoothLeService = null;
    }

//    protected void destroyBLE() {
//        //Unbind BLE intant
//        unregisterReceiver(mGattUpdateReceiver);
//        unbindService(mServiceConnection);
//
//        //Disable bluetooth
//        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter.isEnabled()) {
//            mBluetoothAdapter.disable();
//        }
//        //Stop Handler
//        try {
//            mRotationHandler.removeCallbacks(biaONrotationMethod);
//            mRotationHandler.removeCallbacks(biaOFFrotationMethod);
//            startSeqHandler.removeCallbacks(reconnectMethod);
//        } catch (Exception e) {
//
//        }
//        mBluetoothLeService = null;
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            Toast.makeText(getApplicationContext(), "기기와 연결에 성공", Toast.LENGTH_LONG).show();
            mNow_state.setText("> 기기와 연결됨");
            mNow_guide.setText("\n\n1. 측정이 시작될 때까지 기다려주세요\n\n*매일 취침 전 야뇨 경보기 배터리 교체를 권장합니다");
            start_state = 1;
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            if (start_state == 0) {
                mNow_state.setText("> 기기와 연결을 시도합니다");
                mNow_guide.setText("\n\n잠시만 기다려주세요");
            } else {
                Toast.makeText(getApplicationContext(), "기기와 연결 상태를 확인해주세요", Toast.LENGTH_LONG).show();
                mNow_state.setText("> 기기와 연결이 되어있지않습니다");
                mNow_guide.setText("\n\n1. 기기의 전원 상태를 확인해주세요\n2. 우측 상단의 'CONNECT' 버튼을 눌러주세요\n3. 기기와 스마트폰을 가까이 위치해주세요\n4. 해당 문제가 반복되면 기기와 앱을 종료 후 다시 켜주세요");
            }
        }
        return true;
    }

    private long backPressed;
    private long backtwicePressed;
    private long backthirdPressed;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - backPressed < 5000) {
            backtwicePressed = System.currentTimeMillis();
//            Toast.makeText(getApplicationContext(), "한번 더 누르시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
            if (System.currentTimeMillis() - backtwicePressed < 5000) {
                backthirdPressed = System.currentTimeMillis();
                Log.d(TAG, "Upload start");
                upload();
//                final Intent intent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
//                intent.putExtra(DeviceScanActivity.OFFMSG, "YES");
//                startActivity(intent);
                if (System.currentTimeMillis() - backthirdPressed < 5000) {
                    setResult(RESULT_OK, null);
                    finish();
                }
            }
        }
        Toast.makeText(DeviceControlActivity.this, "5초 이내에 3번 클릭하시면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show();
        backPressed = System.currentTimeMillis();
        vibe.vibrate(40);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                Toast.makeText(getApplicationContext(), "기기와 연결을 시도합니다", Toast.LENGTH_LONG).show();
                vibe.vibrate(40);
                return true;
            case R.id.menu_disconnect:
//                mBluetoothLeService.disconnect();
//                Toast.makeText(getApplicationContext(), "연결 해제 버튼 (비활성화 상태)", Toast.LENGTH_LONG).show();
//                vibe.vibrate(40);
                return true;
            case android.R.id.home:
//             onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * BIA interval을 하기 위한 메서드
     */
    private int biaMethodFlag = 0;
    private Runnable biaOFFrotationMethod = new Runnable() {
        public void run() {
            biaMethodFlag = -1;
            request_bia_off();
            SystemClock.sleep(100);
            request_ecg_ctrl((byte) gain_st);
            SystemClock.sleep(100);
            mFileManager.saveLogData("BIA on/off", "BIA OFF");
            mRotationHandler.postDelayed(biaONrotationMethod, 15000);//26000
            IntervalIndex = 0;
        }
    };
    private Runnable biaONrotationMethod = new
            Runnable() {
                public void run() {
                    biaMethodFlag = 1;
                    request_bia_on();
                    SystemClock.sleep(100);
                    request_ecg_ctrl((byte) gain_ed);
                    SystemClock.sleep(100);
                    mFileManager.saveLogData("BIA on/off", "BIA ON");
                    mRotationHandler.postDelayed(biaOFFrotationMethod, 15000);//4000
                    IntervalIndex = 1;
                }
            };

    private int recMethodFlag = 0;
    private Runnable startMethod = new Runnable() {
        public void run() {
            recMethodFlag = -1;
            SystemClock.sleep(100);
            start_sequence();
            SystemClock.sleep(100);
            startSeqHandler.postDelayed(reconnectMethod, 3000);
            mFileManager.saveLogData("startMethod", "Start method started");

            if (mConnected == false) {
                Log.d(TAG, "Start Method: try reconnect");
                mFileManager.saveLogData("startMethod", "Disconnect detected");
                SystemClock.sleep(100);
                mFileManager.saveLogData("startMethod", "Try reconnect");
                mBluetoothLeService.connect(mDeviceAddress);
                mBluetoothLeService.connect(mDeviceAddress);
                SystemClock.sleep(100);

            }
        }
    };

    private Runnable reconnectMethod = new Runnable() {
        public void run() {
            if (IntervalIndex != IntervalBuf){
                IntervalBuf = IntervalIndex;
                IntervalCounter = 0;
            }else{
                IntervalCounter += 1;
                mFileManager.saveLogData("reconnectMethod", "Interval counting,"+String.valueOf(IntervalCounter));
            }

            if (IntervalCounter > 30){
                mFileManager.saveLogData("reconnectMethod", "Interval does not working, counter#:"+String.valueOf(IntervalCounter));
                try{
                    if (biaMethodFlag == 1) {
                        mRotationHandler.removeCallbacks(biaONrotationMethod);
                        mFileManager.saveLogData("reconnectMethod", "Shut the Bia ON method");
                    } else if (biaMethodFlag == -1) {
                        mRotationHandler.removeCallbacks(biaOFFrotationMethod);
                        mFileManager.saveLogData("reconnectMethod", "Shut the Bia OFF method");
                    }
                    mRotationHandler.postDelayed(biaONrotationMethod, 0);
                    mFileManager.saveLogData("reconnectMethod", "Turn ON BIA thread");
                    IntervalCounter = 0;
                }catch (Exception e) {
                    mFileManager.saveLogData("reconnectMethod", "Failed to shut the BIA on/off thread");
                }

            }

            recMethodFlag = 1;
            if (LastSeqNum != NowSeqNum) {
                LastSeqNum = NowSeqNum;
                SeqCounter = 0;
            } else if (LastSeqNum == NowSeqNum) {
                SeqCounter += 1;
                mFileManager.saveLogData("reconnectMethod", "Seq loss detected");
                mFileManager.saveLogData("reconnectMethod",
                        "SEQ:" + String.valueOf(LastSeqNum) + "->" + String.valueOf(NowSeqNum) + ";counter: " + String.valueOf(SeqCounter));
            }

            if (mConnected == false) {
//                start_sequence();
                Log.d(TAG, "Reconnect Method: try reconnect");
                mFileManager.saveLogData("reconnectMethod", "Disconnect detected");
                try {
                    mRotationHandler.removeCallbacks(biaONrotationMethod);
                    mRotationHandler.removeCallbacks(biaOFFrotationMethod);
                } catch (Exception e) {

                }
                SystemClock.sleep(100);
                mBluetoothLeService.connect(mDeviceAddress);
                mBluetoothLeService.connect(mDeviceAddress);
                SystemClock.sleep(100);
                DisconnectCounter += 1;
                reconnect_call += DisconnectCounter * 1000;
                startSeqHandler.postDelayed(startMethod, reconnect_call);
            } else {
                startSeqHandler.postDelayed(reconnectMethod, 3000);
            }



//            if (mMetaWearConnected == false) {
//                mFileManager.saveLogData("MetaWear", "Try reconnect");
//                if (deviceUUIDs[0] != null) {
//                    SystemClock.sleep(100);
//                    connectToMetawear(deviceUUIDs[0]);
//                    SystemClock.sleep(100);
//                    connectToMetawear(deviceUUIDs[1]);
//                }
//
//            }
            //upload files before disconnected
            if (DisconnectCounter > DisconnectThreshold - 1) {//||SeqCounter > SeqLossThreshold - 1
                mFileManager.saveLogData("reconnectMethod", "Uploading data before disconnected");
                upload();
            }
            if (SeqCounter == 15) {
                mFileManager.saveLogData("reconnectMethod", "Uploading data when NO seq in 72sec");
                upload();
            }
            //back to the Sacn Activity
            if (DisconnectCounter > DisconnectThreshold) {// || SeqCounter > SeqLossThreshold
                mFileManager.saveLogData("reconnectMethod", "BLE lost & scan activity start");
                DisconnectCounter = 0; //for initiate the value
//                final Intent intent = new Intent(DeviceControlActivity.this, DeviceScanActivity.class);
//                intent.putExtra("pass_reconnect_flag", reconnectFlags+1);
//                startActivity(intent);
                System.exit(0);
            }
            mFileManager.saveLogData("reconnectMethod", "Disconnect counter : " + String.valueOf(DisconnectCounter));
            mFileManager.saveLogData("reconnectMethod", "Sequence counter : " + String.valueOf(SeqCounter));
            Log.d(TAG, "Disconnect counter : " + String.valueOf(DisconnectCounter));
            Log.d(TAG, "Sequence counter : " + String.valueOf(SeqCounter));
            Log.d(TAG, "SEQ:" + String.valueOf(LastSeqNum) + "->" + String.valueOf(NowSeqNum) + ";counter: " + String.valueOf(SeqCounter));
        }
    };

    /**
     * 패킷 송신관련
     */
    //BIA, MOI, ECG 3채널을 요청
    public void request_3ch() {
        Log.d(TAG, String.format("0b 74 0500"));
        BiaMarker = 1;
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x74, (byte) 0x00, (byte) 0x05,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //디바이스가 기존에 갖고있던 설정을 초기화함
    public void request_Initial() {
        Log.d(TAG, String.format("0b 70 0001"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x70, (byte) 0x01, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //streaming을 시작함
    public void request_start() {
        Log.d(TAG, String.format("0b 70 0002"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x70, (byte) 0x02, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //Powermanager 를 실행함
    public void request_pm() {
        Log.d(TAG, String.format("0b 13 0004"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x13, (byte) 0x04, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    public void request_some() {
        Log.d(TAG, String.format("1B 4001A104 00000001"));
//        mEcgctrl.setText(String.format("1B 4001A104 000000 01"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x04, (byte) 0xA1, (byte) 0x01, (byte) 0x40,
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //ECG gain을 07(가장큼)로 설정함
    public void request_ecg_gain() {
        Log.d(TAG, String.format("1B 4001A104 00000007_ecg_gain"));
//        mEcgctrl.setText(String.format("1B 4001A104 000000 07"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x04, (byte) 0xA1, (byte) 0x01, (byte) 0x40,
                (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //input을 받아 해당 수를 ECG gain으로 설정함 (01~07까지 가능)
    public void request_ecg_ctrl(byte input) {
        Log.d(TAG, String.format("1B 4001A104 000000 %02X", input));
//        mEcgctrl.setText(String.format("1B 4001A104 000000 %02X", input));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x04, (byte) 0xA1, (byte) 0x01, (byte) 0x40,
                input, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //BIA를 끄도록 요청함
    public void request_bia_off() {
        Log.d(TAG, String.format("1B 4001A348 00000000_bia_off"));
//        mBiactrl.setText(String.format("1B 4001A348 000000_00"));
        BiaMarker = 0;
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x48, (byte) 0xA3, (byte) 0x01, (byte) 0x40,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    //BIA를 켜도록 요청함
    public void request_bia_on() {
        Log.d(TAG, String.format("1B 4001A348 00000003_bia_on"));
//        mBiactrl.setText(String.format("1B 4001A348 000000_03"));
        BiaMarker = 1;
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x09, (byte) 0x00, (byte) 0x1B, (byte) 0x48, (byte) 0xA3, (byte) 0x01, (byte) 0x40,
                (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    /**
     * 지정 패킷을 BLE로 보내서 write하는 함수
     * input은 패킷이어야함
     */
    private void setMargauxLWrite(byte[] val) {
        try {
            if (mNotifyCharacteristic_W != null) {
                mNotifyCharacteristic_W.setValue(val);
                mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic_W);
            }
        } catch (Exception e) {

        }
    }

    /**
     * GATT intent를 관리하는 함수
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        return intentFilter;
    }

    /**
     * Meta wear multi connection을 위한 함수
     */
    public static Task<Void> reconnect(final MetaWearBoard board) {
        return board.connectAsync()
                .continueWithTask(task -> {
                    if (task.isFaulted()) {
                        return reconnect(board);
                    } else if (task.isCancelled()) {
                        return task;
                    }
                    return Task.forResult(null);
                });
    }

    /**
     * mbient Connect
     *
     * @param deviceUUID
     */
    public void connectToMetawear(String deviceUUID) {
        if (deviceUUID != null) {
            BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            BluetoothDevice btDevice = btManager.getAdapter().getRemoteDevice(deviceUUID);
            MetaWearBoard mwBoard = serviceBinder.getMetaWearBoard(btDevice);
            mFileManager.saveLogData("connectToMetawear", "try connect metawear" + deviceUUIDs[0] + ":" + deviceUUIDs[1]);

            mwBoard.connectAsync()
                    .continueWithTask(task -> {
                        if (task.isCancelled()) {
                            return task;
                        }
                        return task.isFaulted() ? reconnect(mwBoard) : Task.forResult(null);
                    })
                    .continueWith(task -> {
                        if (!task.isCancelled()) {
                            mMetaWearConnected = true;
                            startAccelerometer(mwBoard);
                            startGyro(mwBoard);
                            if (mwBoard.getMacAddress() == deviceUUIDs[0]) {
//                            mTextView_Leftfoot.setTextColor(Color.parseColor("#ff8800"));
                                ImageView leftimg = (ImageView) findViewById(R.id.left_foot_img);
                                leftimg.setImageResource(R.drawable.left_foot);
//                            mTextView_Leftfoot.setText(String.format("측정중"));
                            } else if (mwBoard.getMacAddress() == deviceUUIDs[1]) {
//                            mTextView_Rightfoot.setTextColor(Color.parseColor("#008b8b"));
                                ImageView leftimg = (ImageView) findViewById(R.id.right_foot_img);
                                leftimg.setImageResource(R.drawable.right_foot);
//                            mTextView_Rightfoot.setText(String.format("측정중"));
                            }
                        }
                        return null;
                    });

            mwBoard.onUnexpectedDisconnect(new MetaWearBoard.UnexpectedDisconnectHandler() {
                @Override
                public void disconnected(int status) {
                    mFileManager.saveLogData("MetaWear", "disconnected");
                    mMetaWearConnected = false;
                }
            });
        }
    }

    /**
     * Acc Sensor
     *
     * @param mwBoard
     */
    private void startAccelerometer(MetaWearBoard mwBoard) {
        Accelerometer accelerometer = accelerometerSensors.get(mwBoard.getMacAddress());
        if (accelerometer == null) {
            accelerometer = mwBoard.getModule(Accelerometer.class);
            accelerometerSensors.put(mwBoard.getMacAddress(), accelerometer);
        }

        getHz_l_a = String.valueOf(accelerometer.getOdr());
        TextView sensorOutput = sensorOutputs.get(mwBoard.getMacAddress());

        accelerometer.acceleration().addRouteAsync(source -> source.stream((data, env) -> {
            final Acceleration value = data.value(Acceleration.class);
            if (NowSeqNum != LastSeqNum) {
//                Log.d(TAG, "SEQ:"+String.valueOf(LastSeqNum)+"->"+String.valueOf(NowSeqNum)+";counter: "+String.valueOf(SeqCounter));
                runOnUiThread(() -> sensorOutput.setText(getHz_l_a + "HZ : " + value.x() + ", " + value.y() + ", " + value.z()));
                if (mwBoard.getMacAddress() == deviceUUIDs[0]) {
                    mFileManager.saveData("0", value.x(), value.y(), value.z(), "accel");
                } else if (mwBoard.getMacAddress() == deviceUUIDs[1]) {
                    mFileManager.saveData("1", value.x(), value.y(), value.z(), "accel");
                }
            } else {
                runOnUiThread(() -> sensorOutput.setText("NEWear를 기다리는중"));
            }
        })).continueWith(task -> {
            streamRoute = task.getResult();
            accelerometerSensors.get(mwBoard.getMacAddress()).acceleration().start();
            accelerometerSensors.get(mwBoard.getMacAddress()).start();
            return null;
        });
    }

    protected void stopAccelerometer(String deviceUUID) {
        Accelerometer accelerometer = accelerometerSensors.get(deviceUUID);
        accelerometer.stop();
        accelerometer.acceleration().stop();
        if (streamRoute != null) {
            streamRoute.remove();
        }
    }

    /**
     * Gyro Sensor
     *
     * @param mwBoard
     */
    private void startGyro(MetaWearBoard mwBoard) {
        GyroBmi160 gyroBmi160 = gyroSensors.get(mwBoard.getMacAddress());
        if (gyroBmi160 == null) {

            gyroBmi160 = mwBoard.getModule(GyroBmi160.class);
            gyroSensors.put(mwBoard.getMacAddress(), gyroBmi160);
        }
        TextView gyrosensorOutput = gyrosensorOutputs.get(mwBoard.getMacAddress());

        gyroBmi160.angularVelocity().addRouteAsync(source -> source.stream((data, env) -> {
            if (NowSeqNum != LastSeqNum) {
//                Log.d(TAG, "SEQ:"+String.valueOf(LastSeqNum)+"->"+String.valueOf(NowSeqNum)+";counter: "+String.valueOf(SeqCounter));
                final AngularVelocity value = data.value(AngularVelocity.class);
                runOnUiThread(() -> gyrosensorOutput.setText(value.x() + ", " + value.y() + ", " + value.z()));
                if (mwBoard.getMacAddress() == deviceUUIDs[0]) {
                    mFileManager.saveData("0", value.x(), value.y(), value.z(), "gyro");
                } else if (mwBoard.getMacAddress() == deviceUUIDs[1]) {
                    mFileManager.saveData("1", value.x(), value.y(), value.z(), "gyro");
                }
            } else {
                runOnUiThread(() -> gyrosensorOutput.setText("NEWear를 기다리는 중"));
            }
        })).continueWith(task -> {
            streamRoute = task.getResult();
            gyroSensors.get(mwBoard.getMacAddress()).angularVelocity().start();
            gyroSensors.get(mwBoard.getMacAddress()).start();
            return null;
        });

    }

    private void goToUrl(String url) {
        Uri uriUrl = Uri.parse(url);
        Intent WebView = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(WebView);
    }

    protected void stopGyro(String deviceUUID) {
        GyroBmi160 gyroBmi160 = gyroSensors.get(deviceUUID);
        gyroBmi160.stop();
        gyroBmi160.angularVelocity().stop();
        if (streamRoute != null) {
            streamRoute.remove();
        }
    }

    private void start_sequence() {
        mFileManager.saveLogData("start_sequence", "start data");
        SystemClock.sleep(500);
        request_3ch();
        SystemClock.sleep(100);
        request_Initial();
        SystemClock.sleep(100);
        request_pm();
        SystemClock.sleep(100);
        request_ecg_gain();
        SystemClock.sleep(100);
        request_start();
        SystemClock.sleep(100);
        vibe.vibrate(40);

        gain_st = Integer.parseInt("01");
        gain_ed = Integer.parseInt("07");
        mRotationHandler.postDelayed(biaOFFrotationMethod, 30000);
    }

    private void upload() {
        try {
            mFileManager.uploadFile();
            mFileManager.uploadMoFile();
            mFileManager.uploadLogFile();
        } catch (Exception e) {
            Log.e(TAG, "Upload Failed");
            mFileManager.saveLogData("upload", "Save failed");
        }
    }

}