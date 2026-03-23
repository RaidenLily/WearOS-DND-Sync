package com.example.dndsync;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class BluetoothServerService extends Service {
    private static final String TAG = "DndServer";
    private volatile boolean isRunning = false;
    private Thread listeningThread;
    public static AtomicInteger remoteChangeCount = new AtomicInteger(0);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 创建通知渠道 (Android 8+ 要求)
        NotificationChannel channel = new NotificationChannel("dnd_sync", "DND Sync Service", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        // 显示前台通知，防止被杀
        Notification notification = new Notification.Builder(this, "dnd_sync")
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.contentText))
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .build();
        startForeground(1, notification);

        if (!isRunning || (listeningThread != null && !listeningThread.isAlive())) {
            isRunning = true;
            listeningThread = new Thread(() -> {
                startListening();
                // 线程结束时（比如异常退出），记得把 isRunning 置为 false
                // 这样下次 startService 才能重启它
                isRunning = false;
            });
            listeningThread.start();
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getApplicationContext(), getString(R.string.listenThread), Toast.LENGTH_SHORT).show();
            });
        }
        return START_STICKY;
    }

    private void startListening() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return;

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No Permission");
            return;
        }

        // 【核心修改点】 使用 listenUsingInsecureRfcommWithServiceRecord
        try (BluetoothServerSocket serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord("DndSyncPhone", Constants.UUID_TO_PHONE)) {
            Log.d(TAG, "Server started. Waiting for connection...");

            while (true) {
                try (BluetoothSocket socket = serverSocket.accept()) {
                    if (socket != null) {
                        Log.d(TAG, "Phone Connected!");
                        handleConnection(socket);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Accept loop error: " + e.getMessage());
                    // 发生错误时不要死循环太快
                    try { Thread.sleep(1000); } catch (InterruptedException ie) {}
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Server socket creation failed", e);
        }
    }

    private void handleConnection(BluetoothSocket socket) {
        try {
            InputStream is = socket.getInputStream();
            int state = is.read(); // 读取一个字节
            Log.d(TAG, "Received DND State: " + state);

            // 设置勿扰模式
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.isNotificationPolicyAccessGranted()) {
                int currentState = nm.getCurrentInterruptionFilter();
                if (currentState == state) {
                    return;
                }
                nm.setInterruptionFilter(state);
                remoteChangeCount.incrementAndGet();
            }
            while (is.read() != -1) {
                // 什么都不用做，只是为了维持连接，直到对方挂断
            }
        } catch (IOException e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("socket closed") || msg.contains("return: -1"))) {
                // 恭喜！这是正常的关闭信号，不是真正的错误
                Log.d(TAG, "Sender closed gracefully (Caught Exception: " + msg + ")");
            } else {
                Log.e(TAG, "Read failed", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Read failed", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}