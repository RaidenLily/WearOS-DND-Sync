package com.example.dndsync;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String PREF_NAME = "DndConfig";
    public static final String KEY_TARGET_NAME = "target_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText(R.string.tvText);
        Button btn = new Button(this);
        btn.setText(R.string.openService);

        // 【新增 1】输入框
        EditText etDeviceName = new EditText(this);
        etDeviceName.setHint(R.string.examplePhone);
        // 读取之前保存的名字（如果有的话）
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedName = prefs.getString(KEY_TARGET_NAME, "");
        etDeviceName.setText(savedName);

        // 【新增 2】保存按钮
        Button btnSave = new Button(this);
        btnSave.setText(R.string.saveDevice);
        btnSave.setOnClickListener(v -> {
            String input = etDeviceName.getText().toString().trim();
            // 保存到 SharedPreferences
            prefs.edit().putString(KEY_TARGET_NAME, input).apply();
            Toast.makeText(this, getString(R.string.saveTarget) + (input.isEmpty() ? getString(R.string.addDevice) : input), Toast.LENGTH_SHORT).show();
        });
        btn.setOnClickListener(v -> {
            // 检查权限
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                return;
            }
            // 启动前台服务
            Intent serviceIntent = new Intent(this, BluetoothServerService.class);
            startForegroundService(serviceIntent);
            Toast.makeText(this, R.string.serviceOnline, Toast.LENGTH_SHORT).show();
        });

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(tv);
        layout.addView(etDeviceName); // 加入输入框
        layout.addView(btnSave);      // 加入保存按钮
        layout.addView(btn);
        setContentView(layout);
    }
}
