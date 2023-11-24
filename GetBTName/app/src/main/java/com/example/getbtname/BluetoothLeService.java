package com.example.getbtname;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BluetoothLeService extends  Service {
    private final static String TAG = "BluetoothLeService";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    public String mBluetoothDeviceAddress;
    public BluetoothGatt mBluetoothGatt;
    private static String mReconnectAddr = null;
    private static final int BLUE_RECONNECT = 1;

    private Map<String, BluetoothGattCharacteristic> mCharacter = new HashMap<String, BluetoothGattCharacteristic>();
    // 是否连接上
    public boolean isConnedted = false;

    //llx need check
    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // do something
            switch (msg.what) {
                case BLUE_RECONNECT: {
                    if ((!isConnedted) && (mReconnectAddr != null)) {
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mReconnectAddr);
                        if (device != null) {
                            connect(device);
                        }
                        //mHandler.sendEmptyMessageDelayed(BLUE_RECONNECT, 4000);
                    }
                }
                break;
            }
            return true;
        }
    });

    public interface OnConnectListener {
        public void onConnect(BluetoothGatt gatt);
    }

    public interface OnDisconnectListener {
        public void onDisconnect(BluetoothGatt gatt);
    }

    public interface OnReadRemoteRssiListener {
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi);
    }

    public interface OnServiceDiscoverListener {
        public void onServiceDiscover(BluetoothGatt gatt);
    }

    public interface OnDataAvailableListener {
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status);

        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic);
    }
    public BluetoothLeService() {

    }
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    private OnConnectListener mOnConnectListener;
    private OnDisconnectListener mOnDisconnectListener;
    private OnServiceDiscoverListener mOnServiceDiscoverListener;
    private OnDataAvailableListener mOnDataAvailableListener;

    private Context mContext;

    public void setOnConnectListener(OnConnectListener l) {
        mOnConnectListener = l;
    }

    public void setOnDisconnectListener(OnDisconnectListener l) {
        mOnDisconnectListener = l;
    }

    public void setOnServiceDiscoverListener(OnServiceDiscoverListener l) {
        mOnServiceDiscoverListener = l;
    }

    public void setOnDataAvailableListener(OnDataAvailableListener l) {
        mOnDataAvailableListener = l;
    }

    public BluetoothLeService(Context c) {
        mContext = c;
    }

    public boolean getConnect() {
        return isConnedted;
    }

    // Implements callback methods for GATT events that the app cares about. For
    // example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.i(TAG, "onConnectionStateChange " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                isConnedted = true;
                mReconnectAddr = null;

                if (mOnConnectListener != null)
                    mOnConnectListener.onConnect(gatt);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to
                // discover
                // services
                // after
                // successful
                // connection.
                Log.i(TAG, "Attempting to start service discovery");
                // 发现设备端服务
                mBluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (mOnDisconnectListener != null) {
                    Log.i(TAG, "llx mOnDisconnectListener onDisconnect");
                    mOnDisconnectListener.onDisconnect(gatt);
                }
                Log.i(TAG, "llx Disconnected from GATT server.");
                if (isConnedted) {
                    mReconnectAddr = mBluetoothDeviceAddress;
                    mHandler.sendEmptyMessageDelayed(BLUE_RECONNECT, 4000);
                }

                isConnedted = false;
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS
                    && mOnServiceDiscoverListener != null) {
                mOnServiceDiscoverListener.onServiceDiscover(gatt);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicRead " + characteristic.getUuid() + "  " + characteristic.getValue());
            if (mOnDataAvailableListener != null)
                mOnDataAvailableListener.onCharacteristicRead(gatt,
                        characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (mOnDataAvailableListener != null)
                mOnDataAvailableListener.onCharacteristicWrite(gatt,
                        characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

        }
    };

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address
     *            The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The
     *         connection result is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(BluetoothDevice device) {
        if (mBluetoothAdapter == null || device == null) {
            Log.w(TAG,
                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        Log.d(TAG, "Trying to connect " + device + " mBluetoothDeviceAddress " + mBluetoothDeviceAddress + " " + mBluetoothGatt);
        mHandler.removeMessages(BLUE_RECONNECT);
        mHandler.sendEmptyMessageDelayed(BLUE_RECONNECT, 4000);

        // Previously connected device. Try to reconnect.
        if (mBluetoothDeviceAddress != null
                && device.getAddress().equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG,
                    "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(mContext, false
                , mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = device.getAddress();
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.w(TAG, "disconnect() ");
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        mBluetoothManager = null;
        mBluetoothAdapter = null;
        mBluetoothDeviceAddress = null;
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic
     *            The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic
     *            Characteristic to act on.
     * @param enabled
     *            If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (enabled == true) {
            Log.i(TAG, "Enable Notification");
            mBluetoothGatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic
                    .getDescriptor(UUID.fromString(BluetoothGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                descriptor
                        .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        } else {
            Log.i(TAG, "Disable Notification");
            mBluetoothGatt.setCharacteristicNotification(characteristic, false);
            BluetoothGattDescriptor descriptor = characteristic
                    .getDescriptor(UUID.fromString(BluetoothGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            if (descriptor != null) {
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind " + intent);
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        Log.i(TAG, "onUnbind " + intent);
        close();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
    }

    private final IBinder mBinder = new LocalBinder();

}
