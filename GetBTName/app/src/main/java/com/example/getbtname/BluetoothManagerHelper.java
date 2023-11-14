package com.example.getbtname;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothManagerHelper {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattCallback gattCallback;

    public BluetoothManagerHelper(Context context) {
        this.context = context;

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // Cihaz Bluetooth'u desteklemiyor veya kapalı, gerekli aksiyonları al
        }

        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    // Bağlantı başarılı bir şekilde kuruldu
                    gatt.discoverServices();
                    Log.d("EMN", "Connected to device");
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    // Bağlantı kesildi
                    // Gerekli işlemler yapılabilir
                    Log.d("EMN", "Disconnected");
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                BluetoothGattService service = gatt.getService(UUID.fromString("0000ffc0-0000-1000-8000-00805f9b34fb"));
                if (service != null) {
                    characteristic = service.getCharacteristic(UUID.fromString("0000ffc1-0000-1000-8000-00805f9b34fb"));
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                byte[] data = characteristic.getValue();
                String receivedMessage = new String(data, Charset.forName("UTF-8"));
                // Alınan veriyi kullanın
                Log.d("EMN", "Received Message!! " + receivedMessage);
            }
        };
    }

    public void startScanningForDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bluetoothLeScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                if (device != null && device.getName() != null && device.getName().equals(deviceName)) {
                    Log.d("EMN", "Device Found!" + deviceName);
                    stopScanning();
                    connectToDevice(device);
                }
            }
        });
    }

    public void stopScanning() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bluetoothLeScanner.stopScan(new ScanCallback() {
            // Taratmayı durdurmak için gerekli işlemler
        });
    }

    public void connectToDevice(BluetoothDevice device) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }

    public void disconnectGatt() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }

    public void sendMessage(byte[] message) {
        if (bluetoothGatt != null && characteristic != null) {
            characteristic.setValue(message);
            bluetoothGatt.writeCharacteristic(characteristic);
            Log.d("EMN", "Message sended " + message);
        }else {
            Log.d("EMN", "Message send fail ");
        }
    }
}
