package com.example.getbtname;

public class BluetoothGattAttributes {
    private static final String BLE_UUID_END = "0000-1000-8000-00805f9b34fb";

    public static final String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public static final String HEADSET_BASE_CONFIG_SERVICE = "0000ffc0-0000-1000-8000-00805f9b34fb";
    public static final String HEADSET_BASE_NOTIFY_CONFIG_CHARACTERISTIC = "0000ffc2-0000-1000-8000-00805f9b34fb";  //llx need check
    public static final String HEADSET_BASE_WRITE_CONFIG_CHARACTERISTIC = "0000ffc1-0000-1000-8000-00805f9b34fb";
//    public static final String HEADSET_SLEEP_MODE_CHARACTERISTIC = "0000ffc2-0000-1000-8000-00805f9b34fb";  //llx need check
//    public static final String HEADSET_EQ_MODE_CHARACTERISTIC = "0000ffc3-0000-1000-8000-00805f9b34fb";     //llx need check
//    public static final String RESERVE_BASE_CONFIG_SERVICE = "0000ffc1-0000-1000-8000-00805f9b34fb";
}
