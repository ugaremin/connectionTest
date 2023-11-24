package com.example.getbtname;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;



public class ScanAndConnectBleDevice {

    private String TAG = ScanAndConnectBleDevice.class.getName();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private String[] targetDeviceNames = {"Xcel BT Muff v2_BLE", "Digital BT Muff v2(BLE)"};
    private String targetDevice;
    BluetoothDevice myDevice;


    public ScanAndConnectBleDevice(Context context) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public void startScanningForDevice(String targetDeviceName, ScanCallback callback) {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanCallback = callback;

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setDeviceName(targetDeviceName)
                .build();

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bluetoothLeScanner.startScan(Arrays.asList(scanFilter), scanSettings, scanCallback);
    }

    public void stopScanning() {
        if (bluetoothLeScanner != null && scanCallback != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }

    // Diğer Bluetooth işlemleri ve bağlantıları burada ekleyebilirsiniz

    public void connectToGattServer(BluetoothDevice device) {
        // GATT sunucusuna bağlanma işlemi burada gerçekleştirilebilir
        Log.d(TAG, "Connection is ready for " + device.getName());

    }

    public BluetoothDevice scanDevice(ScanAndConnectBleDevice scanAndConnectBleDevice){

        ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                BluetoothDevice device = result.getDevice();
                Log.d(TAG, "Devices " + device.getName());
                if (device.getName() != null && Arrays.asList(targetDeviceNames).contains(device.getName())) {
                    targetDevice = device.getName();
                    myDevice = device;
                    Log.d(TAG, "Device Found!!! " + targetDevice);
                    // Hedef cihaz bulundu, işlemlerinizi burada gerçekleştirin
                    scanAndConnectBleDevice.stopScanning();
                 //   scanAndConnectBleDevice.connectToGattServer(device);
                }else{
                    Log.d(TAG, "Device Not Found!!!");
                }
            }
        };

        scanAndConnectBleDevice.startScanningForDevice(targetDevice, scanCallback);
        return myDevice;

    }
}
