package com.example.dndsync;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class KeepAliveAccessibilityService extends AccessibilityService {

    // 【核心修改】当无障碍服务被系统连接（或复活）时，自动调用此方法
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("DndSyncAlive", "Accessibility Service Connected! Resurrecting Bluetooth Service...");

        Intent intent = new Intent(this, BluetoothServerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 这里不需要做任何事，我们只是为了借用它的“不死金牌”
    }

    @Override
    public void onInterrupt() {
        // 服务中断时的回调，留空即可
    }
}
