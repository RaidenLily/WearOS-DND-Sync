package com.example.dndsync;

import android.Manifest;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DndReceiver extends BroadcastReceiver {
    private static final String TAG = "DndReceiverBT";

    private static final ExecutorService taskQueue = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED.equals(intent.getAction())) {
            if (BluetoothServerService.remoteChangeCount.get() > 0) {
                BluetoothServerService.remoteChangeCount.decrementAndGet();
                return;
            }

            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int currentFilter = nm.getCurrentInterruptionFilter();
            Log.d(TAG, "DND Changed to: " + currentFilter);
            taskQueue.execute(() -> sendToWatch(context, currentFilter));
        }
    }

    private void sendToWatch(Context context, int state) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "No Bluetooth Permission!");
                return;
            }
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;

        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREF_NAME, Context.MODE_PRIVATE);
        String targetNameFilter = prefs.getString(MainActivity.KEY_TARGET_NAME, "");

        // 查找已配对设备
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (!targetNameFilter.isEmpty()) {
                if (device.getName() == null || !device.getName().contains(targetNameFilter)) {
                    continue;
                }
            }
            Log.d(TAG, "Trying device: " + device.getName());
            // 【核心修改点】 使用 Insecure Socket
            try (BluetoothSocket socket = device.createInsecureRfcommSocketToServiceRecord(Constants.UUID_TO_WATCH)) {
                socket.connect();
                if (socket.isConnected()) {
                    OutputStream os = socket.getOutputStream();
                    os.write(state);
                    os.flush();

                    Log.d(TAG, "Data written to buffer, waiting for transmission...");
                    // 【关键修改】增加 500ms 延迟，防止 socket 关闭太快
                    Thread.sleep(500);
                    Log.d(TAG, "Sent success to " + device.getName());
                    // 发送成功后直接退出循环，不用试其他设备了
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Connect failed to " + device.getName() + ": " + e.getMessage());
            }
        }
    }
}
