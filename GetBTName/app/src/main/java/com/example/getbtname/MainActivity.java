package com.example.getbtname;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

  //  private ScanAndConnectBleDevice scanAndConnectBleDevice;
    private BluetoothManagerHelper bluetoothManagerHelper;

    byte[] clearVoice = {90, -69, 0, 26, 26, 26, 2, 101};
    byte[] universal = {90, -69, 0, 26, 26, 26, 1, 100};

    private final String xcelDevice = "Xcel BT Muff v2_BLE";
    private final String digitalBtDevice = "Digital BT Muff v2(BLE)";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermissions();
       // scanAndConnectBleDevice = new ScanAndConnectBleDevice(this);
       // scanAndConnectBleDevice.scanDevice(scanAndConnectBleDevice);

        bluetoothManagerHelper = new BluetoothManagerHelper(this);
        bluetoothManagerHelper.startScanningForDevice(xcelDevice);

        Button clearVoiceBtn = findViewById(R.id.clear_voice);
        Button universalBtn = findViewById(R.id.universal);

        clearVoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("EMN", "Clear Voice Button Click");
                sendBluetoothMessage(clearVoice);
            }
        });

        universalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("EMN", "Universal Button Click");
                sendBluetoothMessage(universal);
            }
        });




    }

    @Override
    protected void onPause() {
        super.onPause();
        bluetoothManagerHelper.stopScanning();
        bluetoothManagerHelper.disconnectGatt();
    }
    // Veri göndermek için bir örnek
    private void sendBluetoothMessage(byte[] message) {
        bluetoothManagerHelper.sendMessage(message); // İstenilen mesajı gönderin
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