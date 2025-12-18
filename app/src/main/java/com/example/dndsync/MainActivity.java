package com.example.dndsync;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.view.Gravity;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.EditText;
import android.content.SharedPreferences;

public class MainActivity extends Activity {

    public static final String PREF_NAME = "DndConfig";
    public static final String KEY_TARGET_NAME = "target_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView tv = new TextView(this);
        tv.setText(R.string.permissionName);
        tv.setGravity(Gravity.CENTER);

        // 【新增 1】输入框
        EditText etDeviceName = new EditText(this);
        etDeviceName.setHint(R.string.watchExample);
        // 读取之前保存的名字（如果有的话）
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedName = prefs.getString(KEY_TARGET_NAME, "");
        etDeviceName.setText(savedName);

        // 【新增 2】保存按钮
        Button btnSave = new Button(this);
        btnSave.setText(R.string.saveDeviceName);
        btnSave.setOnClickListener(v -> {
            String input = etDeviceName.getText().toString().trim();
            // 保存到 SharedPreferences
            prefs.edit().putString(KEY_TARGET_NAME, input).apply();
            Toast.makeText(this, getString(R.string.saveTarget) + (input.isEmpty() ? getString(R.string.allDevice) : input), Toast.LENGTH_SHORT).show();
        });

        Button btn = new Button(this);
        btn.setText(R.string.checkPermissionAndRun);
        btn.setOnClickListener(v -> checkPermissions());

        btn.setOnClickListener(v -> {
            if (checkPermissions()) {
                // 新增：启动手机端的监听服务
                startForegroundService(new Intent(this, BluetoothServerService.class));
            }
        });

        layout.addView(tv);
        layout.addView(etDeviceName); // 加入输入框
        layout.addView(btnSave);      // 加入保存按钮
        layout.addView(btn);
        setContentView(layout);
    }

    private boolean checkPermissions() {
        // 1. 检查蓝牙权限 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                return false;
            }
        }

        // 2. 检查勿扰权限
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
            return false;
        }

        Toast.makeText(this, R.string.allReady, Toast.LENGTH_LONG).show();
        return true;
    }
}
