package com.example.getbtname;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MainActivity extends BaseActivity {

    private static final int GET_INFOMATION = 1;
    private static final int CONNECT_BT = 2;
    private static final int NOTIFY_INFOMATION = 3;
    private static final int SET_VOLUME_INFO = 4;
    private static final int SET_EQ_MODE = 5;
    private static final int SET_SLEEP_MODE = 6;
    private static final long SCAN_PERIOD = 10000;
    private static final int WRITE_DATA_LENGTH = 8;
    private static final int EXTRA_INFO_DATA_LENGTH = 8;
    private static final int WRITE_INFO_MAX_COUNT = 0;
    private static int sWriteEqCount = 0;
    private static int sWriteVolCount = 0;
    private static int sWriteSleepCount = 0;
    private static int sSetEqMode = -1;
    private static int sSetVolume = -1;
    private static int sSetSleepMode = -1;
    private ListView mScanListView;
    private RelativeLayout mMainScanView;
    private Button mScanViewBackButton;
    private Button mScanViewScanButton;
    private boolean mScanning;
    private HashMap<String, BluetoothGattCharacteristic> characteristicHashMap = new HashMap<String, BluetoothGattCharacteristic>();
    private ArrayList<Byte> receiveValue = new ArrayList<Byte>();
    private ArrayList<Byte> receiveHeadSetValue = new ArrayList<Byte>();

    Button mConnectBtn;
    TextView mEqModeText;
    TextView mSleepModeText;

    int mPreVolume;
    int mPreEqMode;
    int mPreSleepMode;
    BluetoothDevice myDevice;

    private BluetoothManagerHelper bluetoothManagerHelper;
    private ScanAndConnectBleDevice scanAndConnectBleDevice;

    byte[] clearVoice = {90, -69, 0, 26, 26, 26, 2, 101};
    byte[] universal = {90, -69, 0, 26, 26, 26, 1, 100};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
        scanAndConnectBleDevice = new ScanAndConnectBleDevice(this);


        Button clearVoiceBtn = findViewById(R.id.clear_voice);
        Button universalBtn = findViewById(R.id.universal);


    }

    @Override
    protected void onResume() {
        super.onResume();
        myDevice = scanAndConnectBleDevice.scanDevice(scanAndConnectBleDevice);
        connectDevice(myDevice);

        if (getBluetoothLeService() != null) {
            if (getBluetoothLeService().getConnect()) {

                if (mHandler.hasMessages(GET_INFOMATION)) {
                    mHandler.removeMessages(GET_INFOMATION);
                }
                //sReadInfomationCount = 0;
                mHandler.sendEmptyMessageDelayed(GET_INFOMATION, 800);
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void getInformation() { //llx need check
        if (getBluetoothLeService() != null) {
            BluetoothGattCharacteristic mWriteBaseCharacteristic = characteristicHashMap.get(BluetoothGattAttributes.HEADSET_BASE_WRITE_CONFIG_CHARACTERISTIC);
            if (mWriteBaseCharacteristic != null) {
                byte[] setValue = new byte[WRITE_DATA_LENGTH];
                setValue[0] = 0x5a;
                setValue[1] = (byte) 0xaa;
                setValue[2] = 0x00;
                setValue[3] = 0x00;
                setValue[4] = 0x00;
                setValue[5] = 0x00;
                setValue[6] = 0x04;
                setValue[7] = (byte)(((Byte)setValue[0]).intValue() + ((Byte)setValue[1]).intValue() + ((Byte)setValue[2]).intValue()
                        + ((Byte)setValue[3]).intValue() + ((Byte)setValue[4]).intValue() +((Byte)setValue[5]).intValue()
                        + ((Byte)setValue[6]).intValue());
                mWriteBaseCharacteristic.setValue(setValue);
                getBluetoothLeService().writeCharacteristic(mWriteBaseCharacteristic);
            }
        }
    }


    private void parserBleReceiveData(BluetoothGattCharacteristic characteristic) {
        String characteristicuuid = characteristic.getUuid().toString();
        if (characteristicuuid.equals(BluetoothGattAttributes.HEADSET_BASE_NOTIFY_CONFIG_CHARACTERISTIC)) {
            int templength = 0;
            byte[] val = characteristic.getValue();
            for (byte byteval : val) {
                receiveValue.add(byteval);
            }
            for (int i = 0; i < receiveValue.size(); i++) {
                byte byteval = receiveValue.get(i);
                if (byteval == 0x5a) {
                    Log.i(TAG, "parserBleReceiveData byteval  " + Integer.toHexString(byteval & 0xff) + " i " + i);
                    if ((i + WRITE_DATA_LENGTH) <= receiveValue.size()) {
                        byte byteSum = 0;
                        if (receiveValue.get(i + 1) == (byte)0xaa) {
                            //byteSum = receiveValue.get(i + WRITE_DATA_LENGTH - 1);
                            //templength = WRITE_DATA_LENGTH;
                        } else {
                            byteSum = receiveValue.get(i + WRITE_DATA_LENGTH - 1);
                            templength = WRITE_DATA_LENGTH;
                        }
                        if (templength > 0) {
                            byte byteRealSum = 0;
                            receiveHeadSetValue.clear();
                            for (int j = i; j < (i + templength - 1); j++) {
                                byteRealSum = (byte)(receiveValue.get(j) + byteRealSum);
                                receiveHeadSetValue.add(receiveValue.get(j));
                                Log.i(TAG, "parserBleReceiveData byteRealSum  " + byteRealSum + " byteSum " + byteSum + " receiveHeadSetValue[" + j + "]=" + receiveHeadSetValue.get(j - i));
                            }
                            if (byteSum != byteRealSum) {
                                receiveHeadSetValue.clear();
                            } else {
                                receiveValue.clear();
                                break;
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < receiveHeadSetValue.size(); i++) {
                Log.i(TAG, "parserBleReceiveData receiveValue[" + i + " ] = " + Integer.toHexString(receiveHeadSetValue.get(i)&0xff));
            }
            if (templength > 0) {
                if (receiveHeadSetValue.size() >= (templength - 1)) {
                    receiveValue.clear();
                    int volume = (receiveHeadSetValue.get(WRITE_DATA_LENGTH - 1 - 3)).intValue();
                    int sleepmode = receiveHeadSetValue.get(WRITE_DATA_LENGTH - 1 - 2).intValue();
                    int eqmode = (receiveHeadSetValue.get(WRITE_DATA_LENGTH - 1 - 1)).intValue();
                    if ((sSetEqMode != -1) && (sSetEqMode == eqmode)) {
                        sSetEqMode = -1;
                        mHandler.removeMessages(SET_EQ_MODE);
                    }
                    if ((sSetVolume != -1) && (sSetVolume == volume)) {
                        sSetVolume = -1;
                        mHandler.removeMessages(SET_VOLUME_INFO);
                    }
                    //Log.i(TAG, "SET_SLEEP_MODE sleepmode " + sleepmode + " sSetSleepMode " + sSetSleepMode);
                    if ((sSetSleepMode != -1) && (sSetSleepMode == sleepmode)) {
                        sSetSleepMode = -1;
                        //Log.i(TAG, "remove SET_SLEEP_MODE " );
                        mHandler.removeMessages(SET_SLEEP_MODE);
                    }
                    mHandler.sendEmptyMessage(NOTIFY_INFOMATION);
                }
            }
        }
    }




    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // do something
            switch (msg.what) {
                case GET_INFOMATION: {
                    getInformation();
                    //if (sReadInfomationCount < READ_INFO_MAX_COUNT) {
                    //    sReadInfomationCount++;
                    //    mHandler.sendEmptyMessageDelayed(GET_INFOMATION, 200);
                    //}
                }
                break;
                case CONNECT_BT: {
                    connectDevice();

                }
                break;
                case NOTIFY_INFOMATION: {
                    int volume = (receiveHeadSetValue.get(WRITE_DATA_LENGTH - 1 - 3)).intValue();
                    if (volume <= 0xf) {
                        mPreVolume = volume;
                        Log.i(TAG, "GET_VOLUME_INFO vol " + volume);

                    }
                    int mode = receiveHeadSetValue.get(WRITE_DATA_LENGTH - 1 - 2).intValue();
                    Log.i(TAG, "NOTIFY_INFOMATION  sleepmode " + mode);

                    int eqmode = (receiveHeadSetValue.get(WRITE_DATA_LENGTH - 1 - 1)).intValue();
                    Log.i(TAG, "NOTIFY_INFOMATION  eqmode " + eqmode);


                }
                break;
                case SET_EQ_MODE: {
                    byte[] setValue = new byte[WRITE_DATA_LENGTH];
                    setValue[0] = 0x5a;
                    setValue[1] = (byte) 0xbb;
                    setValue[2] = 0x00;
                    setValue[3] = 0x1a;
                    setValue[4] = 0x1a;
                    setValue[5] = 0x1a;
                    setValue[6] = (byte) mPreEqMode;
                    setValue[7] = (byte)(((Byte)setValue[0]).intValue() + ((Byte)setValue[1]).intValue() + ((Byte)setValue[2]).intValue()
                            + ((Byte)setValue[3]).intValue() + ((Byte)setValue[4]).intValue() +((Byte)setValue[5]).intValue()
                            + ((Byte)setValue[6]).intValue());
                    sSetEqMode = mPreEqMode;
                    if (getBluetoothLeService() != null) {
                        BluetoothGattCharacteristic mWriteBaseCharacteristic = characteristicHashMap.get(BluetoothGattAttributes.HEADSET_BASE_WRITE_CONFIG_CHARACTERISTIC);
                        if (mWriteBaseCharacteristic != null) {
                            mWriteBaseCharacteristic.setValue(setValue);
                            getBluetoothLeService().writeCharacteristic(mWriteBaseCharacteristic);
                        }
                    }
                    if (sWriteEqCount < WRITE_INFO_MAX_COUNT) {
                        sWriteEqCount++;
                        mHandler.sendEmptyMessageDelayed(SET_EQ_MODE, 800);
                    } else {
                        sSetEqMode = -1;
                    }
                }
                break;
                case SET_SLEEP_MODE: {
                    byte[] setValue = new byte[WRITE_DATA_LENGTH];
                    setValue[0] = 0x5a;
                    setValue[1] = (byte) 0xbb;
                    setValue[2] = 0x00;
                    setValue[3] = 0x1a;
                    setValue[4] = 0x1a;
                    setValue[5] = (byte)mPreSleepMode;
                    setValue[6] = 0x1a;
                    setValue[7] = (byte)(((Byte)setValue[0]).intValue() + ((Byte)setValue[1]).intValue() + ((Byte)setValue[2]).intValue()
                            + ((Byte)setValue[3]).intValue() + ((Byte)setValue[4]).intValue() +((Byte)setValue[5]).intValue()
                            + ((Byte)setValue[6]).intValue());
                    sSetSleepMode = mPreSleepMode;
                    if (getBluetoothLeService() != null) {
                        BluetoothGattCharacteristic mWriteBaseCharacteristic = characteristicHashMap.get(BluetoothGattAttributes.HEADSET_BASE_WRITE_CONFIG_CHARACTERISTIC);
                        if (mWriteBaseCharacteristic != null) {
                            mWriteBaseCharacteristic.setValue(setValue);
                            getBluetoothLeService().writeCharacteristic(mWriteBaseCharacteristic);
                        }
                    }
                    Log.i(TAG, "SET_SLEEP_MODE mPreSleepMode " + mPreSleepMode);
                    if (sWriteSleepCount < WRITE_INFO_MAX_COUNT) {
                        sWriteSleepCount++;
                        mHandler.sendEmptyMessageDelayed(SET_SLEEP_MODE, 800);
                    } else {
                        sSetSleepMode = -1;
                    }
                }
                break;
                case SET_VOLUME_INFO: {
                    byte[] setValue = new byte[WRITE_DATA_LENGTH];
                    setValue[0] = 0x5a;
                    setValue[1] = (byte) 0xbb;
                    setValue[2] = 0x00;
                    setValue[3] = 0x1a;
                    setValue[4] = (byte)mPreVolume;
                    setValue[5] = 0x1a;
                    setValue[6] = 0x1a;
                    setValue[7] = (byte)(((Byte)setValue[0]).intValue() + ((Byte)setValue[1]).intValue() + ((Byte)setValue[2]).intValue()
                            + ((Byte)setValue[3]).intValue() + ((Byte)setValue[4]).intValue() +((Byte)setValue[5]).intValue()
                            + ((Byte)setValue[6]).intValue());
                    sSetVolume = mPreVolume;
                    if (getBluetoothLeService() != null) {
                        BluetoothGattCharacteristic mWriteBaseCharacteristic = characteristicHashMap.get(BluetoothGattAttributes.HEADSET_BASE_WRITE_CONFIG_CHARACTERISTIC);
                        if (mWriteBaseCharacteristic != null) {
                            mWriteBaseCharacteristic.setValue(setValue);
                            getBluetoothLeService().writeCharacteristic(mWriteBaseCharacteristic);
                        }
                    }
                    if (sWriteVolCount < WRITE_INFO_MAX_COUNT) {
                        sWriteVolCount++;
                        mHandler.sendEmptyMessageDelayed(SET_VOLUME_INFO, 800);
                    } else {
                        sSetVolume = -1;
                    }
                }
                break;
            }
            return true;
        }
    });


    @Override
    public void OnServiceDiscover(BluetoothGatt gatt) {
        if (getBluetoothLeService() != null) {
            mSupportGattService = getBluetoothLeService().getSupportedGattServices();
            for (BluetoothGattService gattService : mSupportGattService) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    characteristicHashMap.put(gattCharacteristic.getUuid().toString(), gattCharacteristic);
                    List<BluetoothGattDescriptor> descriptors = gattCharacteristic.getDescriptors();
                    Log.i(TAG, "OnServiceDiscover uuid " + gattCharacteristic.getUuid().toString());
                    for (BluetoothGattDescriptor descriptor : descriptors) {
                        Log.i(TAG, "OnServiceDiscover uuid " + gattCharacteristic.getUuid().toString() + "  " + descriptor.getUuid() + " " + descriptor.getPermissions()+ "  " + descriptor.getValue());
                    }
                }
            }
            BluetoothGattCharacteristic mNotifyBaseCharacteristic = characteristicHashMap.get(BluetoothGattAttributes.HEADSET_BASE_NOTIFY_CONFIG_CHARACTERISTIC);
            if (mNotifyBaseCharacteristic != null) {
                final int charaProp = mNotifyBaseCharacteristic.getProperties();
                Log.i(TAG, "getInformation charaProp 0x" + Integer.toHexString(charaProp));
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    getBluetoothLeService().setCharacteristicNotification(mNotifyBaseCharacteristic, true);
                }
            }
            BluetoothGattCharacteristic mWriteBaseCharacteristic = characteristicHashMap.get(BluetoothGattAttributes.HEADSET_BASE_WRITE_CONFIG_CHARACTERISTIC);
            if (mWriteBaseCharacteristic != null) {
                final int charaProp = mWriteBaseCharacteristic.getProperties();
                Log.i(TAG, "getInformation charaProp 0x" + Integer.toHexString(charaProp));
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    mWriteBaseCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                }
            }
            //sReadInfomationCount = 0;
            mHandler.sendEmptyMessageDelayed(GET_INFOMATION,1000);
        }
    }
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.i(TAG, "onCharacteristicRead str = " + characteristic.getUuid() + "  " + characteristic.getValue().length + "  " + status);
        byte[] bytes = characteristic.getValue();
        for (int i = 0; i < bytes.length; i++) {
            Log.i(TAG, "onCharacteristicRead bytes [ " + i + " ] = " + bytes[i]);
        }
        if (status == BluetoothGatt.GATT_SUCCESS) {
            parserBleReceiveData(characteristic);
            mHandler.removeMessages(GET_INFOMATION);
        }
    }
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // Log.i(TAG, "onCharacteristicRead str = " + str);
        parserBleReceiveData(characteristic);
    }
    @Override
    public void onConnect(BluetoothGatt gatt) {
        // TODO Auto-generated method stub
        Log.i(TAG, "llx MainActivity onConnect " + gatt);
        if (mHandler.hasMessages(CONNECT_BT)) {
            mHandler.removeMessages(CONNECT_BT);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "toast_connection_success", Toast.LENGTH_LONG).show();
            }
        });

        //mHandler.sendEmptyMessageDelayed(CONNECT_BT, 1000);
    }
    @Override
    public  void onDisconnect(BluetoothGatt gatt) {
        Log.i(TAG, "llx MainActivity onDisconnect " + gatt);
        super.onDisconnect(gatt);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //showScanView(false);
                Toast.makeText(getApplicationContext(), "toast_not_connected", Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public void onBluetoothEnabled() {
        // TODO Auto-generated method stub
        super.onBluetoothEnabled();
        if (mHandler.hasMessages(CONNECT_BT)) {
            mHandler.removeMessages(CONNECT_BT);
        }

        mHandler.sendEmptyMessageDelayed(CONNECT_BT, 1500);
        Log.i(TAG, "onBluetoothEnabled");
    }
    @Override
    public void onBluetoothDisabled() {
        super.onBluetoothDisabled();

        Log.i(TAG, "onBluetoothDisabled");
    }



    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };
    private static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    private void checkPermissions(){
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN);
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1
            );
        } else if (permission2 != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_LOCATION,
                    1
            );
        }
    }
}