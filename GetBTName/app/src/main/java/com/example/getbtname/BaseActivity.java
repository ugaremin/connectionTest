package com.example.getbtname;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.getbtname.BroadcastReceiverExtended.BroadcastReceiverListener;

import java.util.ArrayList;
import java.util.List;


public class BaseActivity extends Activity implements BroadcastReceiverListener {
    public static final String TAG = "btheadsetMainActivity";
    public List<BluetoothGattService> mSupportGattService;
    private BroadcastReceiverExtended mBroadcastReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothScanner;
    private boolean mBlueToothEnabled = false;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothManager mBluetoothManager;
    private static final int REQUEST_PERMISSION_CODE = 1001;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int CHECK_BT_ENABLE = 1;
    private static final int BLUETOOTH_DISABLE = 2;
    private static String mReconnectAddr = null;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CHECK_BT_ENABLE:
                    checkEnableBt();
                    break;
                case BLUETOOTH_DISABLE:
                    onDisconnect(null);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        int flag= WindowManager.LayoutParams. FLAG_FULLSCREEN ;
        // 获得窗口对象
        Window myWindow= this.getWindow();
        // 设置 Flag 标识
        myWindow.setFlags(flag,flag);
        checkBleSupport();
        getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.LOCATION_MODE), true, settingsObserver);
    }
    ContentObserver settingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            // 设置发生变化时的处理逻辑
            // 在这里可以执行相应的操作
            Log.i(TAG, "settingsObserver onChange " + selfChange);
            if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 0) == 0) {
                showOpenLocationDialog();
            }
        }
    };
    public boolean recheckBluetoothEnable() {
        if (!mBlueToothEnabled) {
            mHandler.removeMessages(CHECK_BT_ENABLE);
            mHandler.sendEmptyMessage(CHECK_BT_ENABLE);
        }
        return mBlueToothEnabled;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "llx onResume");
        checkLocationPermission();
        checkBluetoothPermissionAndBluetoothEnable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "llx onPause");
        try {
            if (mBroadcastReceiver != null) {
                unregisterReceiver(mBroadcastReceiver);
                mBroadcastReceiver = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothLeService != null) {
            //mBluetoothLeService.disconnect();
            unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
    }

    private void serviceInit() {
        Log.i(TAG, "service_init");
        Intent bindIntent = new Intent(getApplicationContext(), BluetoothLeService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mBluetoothLeService);
            mBluetoothLeService.setOnConnectListener(mOnConnectListener);
            mBluetoothLeService.setOnServiceDiscoverListener(mOnServiceDiscover);
            mBluetoothLeService.setOnDataAvailableListener(mOnDataAvailable);
            mBluetoothLeService.setOnDisconnectListener(mOnDisconnectListener);
        }
        public void onServiceDisconnected(ComponentName classname) {
            Log.d(TAG, "onServiceDisconnected mService= " + classname);
            mBluetoothLeService.disconnect();
            mBluetoothLeService = null;
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Log.d(TAG, "onNullBinding name " + name);
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.d(TAG, "onBindingDied name " + name);
        }
    };

    /**
     * 搜索到BLE终端服务的事�?
     */
    private BluetoothLeService.OnServiceDiscoverListener mOnServiceDiscover = new BluetoothLeService.OnServiceDiscoverListener() {
        @Override
        public void onServiceDiscover(BluetoothGatt gatt) {
            if (mBluetoothLeService != null) {
                // displayGattServices(mService.getSupportedGattServices());
                BaseActivity.this.OnServiceDiscover(gatt);
            }
        }
    };

    /**
     * 收到BLE终端数据交互的事�?
     */
    private final BluetoothLeService.OnDataAvailableListener mOnDataAvailable = new BluetoothLeService.OnDataAvailableListener() {

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            BaseActivity.this.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            BaseActivity.this.onCharacteristicWrite(gatt, characteristic);
        }
    };

    private BluetoothLeService.OnConnectListener mOnConnectListener = new BluetoothLeService.OnConnectListener() {
        @Override
        public void onConnect(BluetoothGatt gatt) {
            Log.i(TAG, "llx onConnect " + gatt);
            BaseActivity.this.onConnect(gatt);
        }
    };
    private BluetoothLeService.OnDisconnectListener mOnDisconnectListener = new BluetoothLeService.OnDisconnectListener() {
        @Override
        public void onDisconnect(BluetoothGatt gatt) {
            Log.i(TAG, "llx onDisconnect " + gatt);
            BaseActivity.this.onDisconnect(gatt);
        }
    };

    public BluetoothLeService getBluetoothLeService() {
        return mBluetoothLeService;
    }

    public void OnServiceDiscover(BluetoothGatt gatt) {

    }

    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        // Log.i(TAG, "onCharacteristicRead str = " + str);
    }


    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        // Log.i(TAG, "onCharacteristicRead str = " + str);
    }

    public void onConnect(BluetoothGatt gatt) {
        Log.i(TAG, "llx BaseActivity onConnect " + gatt);
        // TODO Auto-generated method stub
    }

    public void onDisconnect(BluetoothGatt gatt) {
        // TODO Auto-generated method stub
        if (!mBlueToothEnabled) {
            unbindService(mServiceConnection);
            //mBluetoothLeService = null;
        }
        Log.i(TAG, "llx BaseActivity onDisconnect " + gatt);
    }

    public void registReceiver() {
        try {
            if (mBroadcastReceiver == null) {
                mBroadcastReceiver = new BroadcastReceiverExtended(this);
                IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                //filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
                this.registerReceiver(mBroadcastReceiver, filter);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkBleSupport() {
        // 手机硬件支持蓝牙
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "ble_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                List<String> mPermissionList = new ArrayList<>();
                mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                mPermissionList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                ActivityCompat.requestPermissions(this, mPermissionList.toArray(new String[0]), REQUEST_PERMISSION_CODE);
            }
        }
    }

    private void checkBluetoothPermissionAndBluetoothEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                List<String> mPermissionList = new ArrayList<>();
                mPermissionList.add(Manifest.permission.BLUETOOTH_SCAN);
                mPermissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
                mPermissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
                ActivityCompat.requestPermissions(this, mPermissionList.toArray(new String[0]), REQUEST_PERMISSION_CODE);
                return;
            }
        }
        int mode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 0);
        Log.i(TAG, "location mode " + mode);
        if (Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE, 0) == 0) {
            showOpenLocationDialog();
        }
        mHandler.removeMessages(CHECK_BT_ENABLE);
        mHandler.sendEmptyMessageDelayed(CHECK_BT_ENABLE, 1000);
        registReceiver();
    }
    AlertDialog mAlertDialog;
    private void showOpenLocationDialog() {
        if (mAlertDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("warning")
                    .setMessage("location_not_open")
                    .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 点击确定按钮后的操作
                            dialog.dismiss();
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("no", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 点击取消按钮后的操作
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            mAlertDialog = null;
                        }
                    });
            mAlertDialog = builder.create();
            mAlertDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable  Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT: {
                if (resultCode == RESULT_OK) {
                    onBluetoothEnabled();
                }
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Display a dialog requesting Bluetooth to be enabled if it isn't already.
     * Otherwise this method update the list to the list view. The list view
     * needs to be ready when this method is called.
     */
    private void checkEnableBt() {
        if (mBluetoothAdapter == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        if (mBluetoothScanner == null) {
            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        if ((mBluetoothAdapter != null) && (!mBluetoothAdapter.isEnabled())) {
            Log.i(TAG, "llx checkEnableBt ");
            mBluetoothAdapter.enable();
        } else if (mBluetoothAdapter != null) {
            onBluetoothEnabled();
        }
    }
    public BluetoothLeScanner getBluetoothLeScanner() {
        if (mBluetoothAdapter == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        if (mBluetoothScanner == null) {
            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        return mBluetoothScanner;
    }
    @Override
    public void onBluetoothEnabled() {
        // TODO Auto-generated method stub
        Log.i(TAG, "onBluetoothEnabled  mBlueToothEnabled " + mBlueToothEnabled + " " + mBluetoothLeService);
        if ((!mBlueToothEnabled) || (mBluetoothLeService == null)) {
            mBlueToothEnabled = true;
            serviceInit();
        }
    }
    @Override
    public void onBluetoothDisabled() {
        mBlueToothEnabled = false;
        Log.i(TAG, "onBluetoothDisabled mBlueToothEnabled " + mBlueToothEnabled);
        if (mBluetoothLeService != null) {
            Log.i(TAG, "onBluetoothDisabled unbindService " + mBluetoothLeService.isConnedted);
            if (mBluetoothLeService.isConnedted) {
                mReconnectAddr = mBluetoothLeService.mBluetoothDeviceAddress;
                //mBluetoothLeService.disconnect();
                //mBluetoothLeService.close();
                stopService(new Intent(this, BluetoothLeService.class));
                mHandler.sendEmptyMessageDelayed(BLUETOOTH_DISABLE, 1000);
            }
            //unbindService(mServiceConnection);
            //mBluetoothLeService = null;
        }
    }
    public BluetoothDevice getConnectBt() {
        Log.i(TAG, "getConnectBt");
        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Initialisation of the Bluetooth Adapter failed: unable to initialize BluetoothManager.");
            return null;
        }
        if (mReconnectAddr != null) {
            BluetoothDevice device =  mBluetoothManager.getAdapter().getRemoteDevice(mReconnectAddr);
            mReconnectAddr = null;
            return device;
        }
        /*
        List<BluetoothDevice> devices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
        Log.i(TAG,"devices:"+devices.size());
        for(BluetoothDevice device : devices){
            //boolean isConnected = (boolean) device.;
            int type = device.getType();
            String name=device.getName();
            String alisName=device.getAlias();
            String address = device.getAddress();
            if (name.contains("Digital BT Muff")) {
                Log.i(TAG, "connected:"+address + " type " + type + "  name " + name);
                if (!TextUtils.isEmpty(address)) {
                    return device;
                }
            }
        }*/
        return null;
    }
    public void connectDevice() {
        Log.i(TAG, "connectDevice");
 //llx need check
        BluetoothDevice mDevice = getConnectBt();
        if ((mDevice != null) && (mBluetoothLeService != null) && (!mBluetoothLeService.getConnect())) {
            mBluetoothLeService.connect(mDevice);
        }
    }
    public void connectDevice(BluetoothDevice device) {
        if ((device != null) && (mBluetoothLeService != null)) {
            mBluetoothLeService.connect(device);
        }
    }
}