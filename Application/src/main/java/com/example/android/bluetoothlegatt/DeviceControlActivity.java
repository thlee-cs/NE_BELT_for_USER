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

package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v13.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.NumberPicker;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.yonsei.dclab.OSEAFactory;
import com.yonsei.dclab.chart.BIA_Chart;
import com.yonsei.dclab.file.FileManager;
import com.yonsei.dclab.packet.Packet;
import com.yonsei.dclab.packet.PacketParser;
import com.yonsei.dclab.classification.BeatDetectionAndClassification;

import static com.yonsei.dclab.classification.ECGCODES.NORMAL;
import static com.yonsei.dclab.classification.ECGCODES.PVC;
import static com.yonsei.dclab.classification.ECGCODES.UNKNOWN;
/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    int sampleRate = 256;

    public static final String EXTRAS_DEVICE_NAME = "NE_BELT1";
    public static final String EXTRAS_DEVICE_ADDRESS = "98:2D:68:2D:60:05";

    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mSaveView;
    private Button mSaveButton;
    private Button mPostureset;
    private Button mStartButton;
    private EditText mWriteport;
    private String mDeviceName;
    private String mDeviceAddress;
    private BIA_Chart mBIA_Chart;
    private TextView mTextView_BodyImpedance;
    private TextView mTextView_MoistureSensor;
    private Handler mUpdateDataHandler;

//    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
//    private BluetoothGattCharacteristic mGattCharMargauxLiteWrite;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private boolean mConnected = false;

    private FileManager mFileManager;
    private PacketParser mPacketParser;

    private Vibrator vibe;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mNotifyCharacteristic_W;
    private BluetoothGattCharacteristic mNotifyCharacteristic_C;

//    private ArrayList<ArrayList<Integer>> mMULDataList = new ArrayList<ArrayList<Integer>>();
//    private ArrayList<Integer> mMULDataListTemp = new ArrayList<Integer>();
    private ArrayList<Integer> mBiaDataList = new ArrayList<Integer>();
    private ArrayList<Integer> mEcgDataList = new ArrayList<Integer>();
    private ArrayList<Integer> mMoiDataList = new ArrayList<Integer>();
    private int MULDataListSize = 64;
    // 256Hz, 10sec
    private int BiaDataListSize = 64 * 4 * 10;
    private int EcgDataListSize = 64 * 4 * 10;
    private int MoiDataListSize = 64 * 4 * 10;
    private int NEventMarker = 0;


    private int[] biaDataArray = new int[BiaDataListSize];
    private int[] ecgDataArray = new int[EcgDataListSize];
    private int[] moiDataArray = new int[MoiDataListSize];



    private static String[] STORAGE_PERMISSION = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
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

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                getGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                Log.d(TAG, String.format("MyDEBUG: mGattUpdateReceiver[2] = %s", action));
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.ACTION_DATA_AVAILABLE);
                if (data != null) {
//                    Log.d(TAG, String.format("MyDEBUG: mGattUpdateReceiver[3] = %s", action));
                    try {
                        if (false) {
                            Log.d(TAG, String.format("DATA_AVAILABLE = %d", data.length));
                            final StringBuilder stringBuilder = new StringBuilder(data.length);
                            for (byte byteChar : data)
                                stringBuilder.append(String.format("%02X ", byteChar));
                            Log.d(TAG, "DATA_AVAILABLE = " + stringBuilder.toString());
                        }


                        mPacketParser.add(data, 0, data.length);
                        data = mPacketParser.get();
                        Packet packet_v0 = new Packet(mDeviceName, mDeviceAddress, data);
                        if (packet_v0.isMULdata()){
//                            Log.d(TAG, "isBIAdata");
//                            Log.d(TAG, String.format("BIA data length = %d ", packet_v0.rawData.size()));
                            receivedData(packet_v0);
                        }

//                        if (getSyncEndIndex(data)>0){
//                            byte[] Pdata = mPacketParser.get();

//                            final StringBuilder stringBuilder = new StringBuilder(data.length);
//                            for (byte byteChar : data)
//                                stringBuilder.append(String.format("%02X ", byteChar));
//                            Log.d(TAG, "DATA_AVAILABLE = " + stringBuilder.toString());

//                            Packet packet_v0 = new Packet(mDeviceName, mDeviceAddress, Pdata);
//                            Log.d(TAG, String.format("ECG data length = %d ", packet_v0.rawData.get(0).size()));
//                            Log.d(TAG, String.format("BIA data length = %d ", packet_v0.rawData.get(1).size()));
//                            Log.d(TAG, String.format("Moi data length = %d ", packet_v0.rawData.get(2).size()));

//                            receivedData(packet_v0);
//                            displayData("%d, %d, %d", packet_v0.rawData.get(0).get(0), packet_v0.rawData.get(1).get(0), packet_v0.rawData.get(2).get(0));
//                        }

                    } catch (Exception e) {

                    }
                }
            }
        }
    };
    Button.OnClickListener mClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
//                case R.id.makefile:
//                    mFileManager.createFile("example");
//                    break;
                case R.id.save:
                    mFileManager.createFile("NE");
                    break;
                case R.id.start_button:
                    Log.d(TAG,"start");
                    SystemClock.sleep(100);
                    request_basic();
                    SystemClock.sleep(100);
                    request_Impedance();
                    SystemClock.sleep(100);
                    request_Initial();
                    SystemClock.sleep(100);
                    request_pm();
                    SystemClock.sleep(100);
                    request_some();
                    SystemClock.sleep(100);
                    request_start();
                    SystemClock.sleep(100);
//                    mBluetoothLeService.readCharacteristic(mNotifyCharacteristic);
                    break;
                default:
                    break;
            }
        }
    };

    public int getSyncEndIndex(byte[] mBuffer) {
        for(int i = 0; i < mBuffer.length - 3; i++) {
            if (mBuffer[i] == (byte) 0x44 & mBuffer[i + 1] == (byte) 0x99 & mBuffer[i + 2] == (byte) 0xee & mBuffer[i + 3] == (byte) 0xee) {
                return i;
            }
        }
        return -1;
    }

    public void receivedData(Packet packet) {
//        for (int j = 0; j < packet.cfgNumCh; j++){
//            mMULDataListTemp = new ArrayList<Integer>();
//            for(int i = 0; i < packet.rawData.get(j).size(); i++) mMULDataListTemp.add(packet.rawData.get(j).get(i));
//            mMULDataList.add(mMULDataListTemp);
//            while(mMULDataList.get(j).size()>MULDataListSize) mMULDataList.get(j).remove(0);
//        }

//        int[] ecgDataArray = new int[MULDataListSize];
//        int[] biaDataArray = new int[MULDataListSize];
//        int[] moiDataArray = new int[MULDataListSize];
//        for (int i = 0; i < MULDataListSize; i++) {
//            mEcgDataList.add(packet.rawData.get(0).get(i));
//            mBiaDataList.add(packet.rawData.get(1).get(i));
//            mMoiDataList.add(packet.rawData.get(2).get(i));
//        }

        for (int i = 0; i < MULDataListSize; i++) {
            mEcgDataList.add(packet.rawData.get(0).get(i));
            mBiaDataList.add(packet.rawData.get(1).get(i));
            mMoiDataList.add(packet.rawData.get(2).get(i));
        }
        while(mBiaDataList.size() > BiaDataListSize){
            mBiaDataList.remove(0);
            mEcgDataList.remove(0);
            mMoiDataList.remove(0);
        }
        if(mBiaDataList.size()>=BiaDataListSize){
            for (int i = 0; i < BiaDataListSize; i++) {// 10sec
                biaDataArray[i] = mBiaDataList.get(i);
                ecgDataArray[i] = mEcgDataList.get(i);
                moiDataArray[i] = mMoiDataList.get(i);
            }
        }else{
            for (int i = 0; i < mBiaDataList.size(); i++){
                biaDataArray[i] = mBiaDataList.get(i);
                ecgDataArray[i] = mEcgDataList.get(i);
                moiDataArray[i] = mMoiDataList.get(i);
            }
        }
        //Set Urine bell
        if((moiDataArray[0] <= 8300 && moiDataArray[0] >= 7900)||(moiDataArray[255] <= 8300 && moiDataArray[255] >= 7900)||(moiDataArray[511] <= 8300 && moiDataArray[511] >= 7900)||(moiDataArray[766] <= 8300 && moiDataArray[766] >= 7900)||(moiDataArray[1023] <= 8300 && moiDataArray[1023] >= 7900)||(moiDataArray[1278] <= 8300 && moiDataArray[1278] >= 7900)||(moiDataArray[1533] <= 8300 && moiDataArray[1533] >= 7900)||(moiDataArray[1788] <= 8300 && moiDataArray[1788] >= 7900)){
            if((moiDataArray[4] <= 8300 && moiDataArray[4] >= 7900)||(moiDataArray[259] <= 8300 && moiDataArray[259] >= 7900)||(moiDataArray[515] <= 8300 && moiDataArray[515] >= 7900)||(moiDataArray[770] <= 8300 && moiDataArray[770] >= 7900)||(moiDataArray[1027] <= 8300 && moiDataArray[1027] >= 7900)||(moiDataArray[1282] <= 8300 && moiDataArray[1282] >= 7900)||(moiDataArray[1537] <= 8300 && moiDataArray[1537] >= 7900)||(moiDataArray[1792] <= 8300 && moiDataArray[1792] >= 7900))
                vibe.vibrate(3000);
            //log urine event
        }

        //Set Heart rate
        BeatDetectionAndClassification bdac = OSEAFactory.createBDAC(sampleRate, sampleRate/2);
        for (int j = 0; j < ecgDataArray.length; j++) {
            BeatDetectionAndClassification.BeatDetectAndClassifyResult result = bdac.BeatDetectAndClassify(ecgDataArray[j]);
            if (result.samplesSinceRWaveIfSuccess != 0) {
                int qrsPosition =  j - result.samplesSinceRWaveIfSuccess;
                if (result.beatType == UNKNOWN) {
                    Log.e(TAG,"A unknown beat type was detected at sample: " + qrsPosition);
                } else if (result.beatType == NORMAL) {
                    Log.e(TAG,"A normal beat type was detected at sample: " + qrsPosition);
                } else if (result.beatType == PVC) {
                    Log.e(TAG,"A premature ventricular contraction was detected at sample: " + qrsPosition);
                }
            }
        }


//        if(mChartSelect.equals("BIA")) {
//            if (mBIA_Chart != null) {
//               // mBIA_Chart.setPoint(biaDataArray[0]); // set BIA Chart Y scale
//               // mBIA_Chart.buildRenderer(0xff7d7d7d);
//                mBIA_Chart.updateChart(biaDataArray);
//                mUpdateDataHandler = new Handler();
//                mUpdateDataHandler.postDelayed(updateDataMethod, 5000);
//            }
//        }

        if (mBIA_Chart != null) {
            // mBIA_Chart.setPoint(ecgDataArray[0]); // set ECG Chart Y scale
            // mBIA_Chart.buildRenderer(0xff7d7d7d);
            mBIA_Chart.updateChart(ecgDataArray);
            mUpdateDataHandler = new Handler();
            mUpdateDataHandler.postDelayed(updateDataMethod, 5000);
        }

//        else if(mChartSelect.equals("MOISTURE")) {
//            if (mBIA_Chart != null) {
//                //mBIA_Chart.setPoint(moiDataArray[0]); // set Moisture Chart Y scale
//               // mBIA_Chart.buildRenderer(0xff7d7d7d);
//                mBIA_Chart.updateChart(moiDataArray);
//                mUpdateDataHandler = new Handler();
//                mUpdateDataHandler.postDelayed(updateDataMethod, 5000);
//            }
//        }
        mFileManager.saveData(packet, NEventMarker);
        NEventMarker = 0;

        int fileKb = (int) (mFileManager.getFileSize()/1000);
        String tFilesize = String.valueOf(fileKb);
        mSaveView.setText("Storage Time : " + mFileManager.getStorageTime()+"File Size : "+fileKb+" KB");
    }

    private void getGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG, String.format("BluetoothGattService = %s", gattService.getUuid().toString()));
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                if(gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_READ)) {
                    Log.d(TAG,"read_checked");
                    mNotifyCharacteristic = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                }
                else if(gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_FLOW_CTRL)) {
                    Log.d(TAG,"ctrl_checked");
                    mNotifyCharacteristic_C = gattCharacteristic;
                }
                else if(gattCharacteristic.getUuid().toString().equals(BleUuid.CHAR_MARGAUXL_WRITE)) {
                    Log.d(TAG,"write_checked");
                    mNotifyCharacteristic_W = gattCharacteristic;
                }
            }
        }
    }


    private void clearUI() {
//        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
//        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        ActivityCompat.requestPermissions(DeviceControlActivity.this, STORAGE_PERMISSION, 1);

        mBIA_Chart = (BIA_Chart) findViewById(R.id.bia_chart);
        mBIA_Chart.setColor(0xff7d7d7d);

        vibe = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        mTextView_BodyImpedance = (TextView) findViewById(R.id.textView_bodyImpedance);
        mTextView_MoistureSensor = (TextView) findViewById(R.id.textView_moisturesensor);

        mFileManager = new FileManager();
        // Sets up UI references.
        //((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
//        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
//        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        //mConnectionState = (TextView) findViewById(R.id.connection_state);
        //mDataField = (TextView) findViewById(R.id.data_value);
        //mWriteport = (EditText) findViewById(R.id.write_port);

        //mMakefile = (Button) findViewById(R.id.makefile);
        //mMakefile.setOnClickListener(mClickListener);
        mSaveButton = (Button) findViewById(R.id.save);
        mSaveButton.setOnClickListener(mClickListener);
        mStartButton = (Button) findViewById(R.id.start_button);
        mStartButton.setOnClickListener(mClickListener);

        mSaveView = (TextView) findViewById(R.id.save_view);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mPacketParser = new PacketParser();

        mFileManager.createFile("Sample");
        Arrays.fill(biaDataArray, 0);
        Arrays.fill(ecgDataArray, 0);
        Arrays.fill(moiDataArray, 0);


    }

    @Override
    protected void onResume() {
        super.onResume();
//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if (mBluetoothLeService != null) {
//            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
//            Log.d(TAG, "Connect request result=" + result);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    private void updateConnectionState(final int resourceId) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mConnectionState.setText(resourceId);
//            }
//        });
//    }
//
//    private void displayData(String data, Integer integer, Integer integer1, Integer integer2) {
//        if (data != null) {
//            mDataField.setText(data);
//        }
//    }

    private Runnable updateDataMethod = new Runnable() {
        public void run() {

            if (mTextView_BodyImpedance != null) mTextView_BodyImpedance.setText(String.format("%,d", mBiaDataList.get(mBiaDataList.size() - 1)));
            if (mTextView_MoistureSensor != null) mTextView_MoistureSensor.setText(String.format("%,d", mMoiDataList.get(mMoiDataList.size() - 1)));
            mUpdateDataHandler.postDelayed(updateDataMethod, 5000);
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
//    private void displayGattServices(List<BluetoothGattService> gattServices) {
//        if (gattServices == null) return;
//        String uuid = null;
//        String unknownServiceString = getResources().getString(R.string.unknown_service);
//        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
//        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
//        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
//                = new ArrayList<ArrayList<HashMap<String, String>>>();
//        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
//
//        // Loops through available GATT Services.
//        for (BluetoothGattService gattService : gattServices) {
//            Log.d(TAG, String.format("BluetoothGattService = %s", gattService.getUuid().toString()));
//            HashMap<String, String> currentServiceData = new HashMap<String, String>();
//            uuid = gattService.getUuid().toString();
//            currentServiceData.put(
//                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
//            currentServiceData.put(LIST_UUID, uuid);
//
//            if (SampleGattAttributes.lookup(uuid, unknownServiceString) != getResources().getString(R.string.unknown_service)) {
//                gattServiceData.add(currentServiceData);
//
//                ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
//                        new ArrayList<HashMap<String, String>>();
//                List<BluetoothGattCharacteristic> gattCharacteristics =
//                        gattService.getCharacteristics();
//                ArrayList<BluetoothGattCharacteristic> charas =
//                        new ArrayList<BluetoothGattCharacteristic>();
//
//
//                // Loops through available Characteristics.
//                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
//                    charas.add(gattCharacteristic);
//                    HashMap<String, String> currentCharaData = new HashMap<String, String>();
//                    uuid = gattCharacteristic.getUuid().toString();
//                    currentCharaData.put(
//                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
//                    currentCharaData.put(LIST_UUID, uuid);
//                    gattCharacteristicGroupData.add(currentCharaData);
//                }
//                mGattCharacteristics.add(charas);
//                gattCharacteristicData.add(gattCharacteristicGroupData);
//            }
//
//
//        }
//
//        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
//                this,
//                gattServiceData,
//                android.R.layout.simple_expandable_list_item_2,
//                new String[] {LIST_NAME, LIST_UUID},
//                new int[] { android.R.id.text1, android.R.id.text2 },
//                gattCharacteristicData,
//                android.R.layout.simple_expandable_list_item_2,
//                new String[] {LIST_NAME, LIST_UUID},
//                new int[] { android.R.id.text1, android.R.id.text2 }
//        );
//        mGattServicesList.setAdapter(gattServiceAdapter);
//    }
//
//

    public void request_Impedance() {
        Log.d(TAG, String.format("0b 74 0500"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x74, (byte) 0x00, (byte) 0x05,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});

    }

    public void request_Initial(){
        Log.d(TAG, String.format("0b 70 0001"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x70, (byte) 0x01, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});

    }
    public void request_start() {
        Log.d(TAG, String.format("0b 70 0002"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x70, (byte) 0x02, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }
    public void request_pm() {
        Log.d(TAG, String.format("0b 13 0004"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0B, (byte) 0x13, (byte) 0x04, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }
    public void request_some() {
        Log.d(TAG, String.format("1B 4001A104 00000006"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x1B, (byte) 0x04, (byte) 0xA1, (byte) 0x01, (byte) 0x40,
                (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }
    public void request_basic() {
        Log.d(TAG, String.format("0a 00 0000"));
        setMargauxLWrite(new byte[]{(byte) 0x55, (byte) 0xaa, (byte) 0xff, (byte) 0xff,
                (byte) 0x04, (byte) 0x00, (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x44, (byte) 0x99, (byte) 0xee, (byte) 0xee});
    }

    private void setMargauxLWrite(byte[] val)
    {
        if (mNotifyCharacteristic_W != null) {
            mNotifyCharacteristic_W.setValue(val);
            mBluetoothLeService.writeCharacteristic(mNotifyCharacteristic_W);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        return intentFilter;
    }
}
