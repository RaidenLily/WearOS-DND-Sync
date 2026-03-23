package com.example.dndsync;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    public static final String PREF_NAME = "DndConfig";
    public static final String KEY_TARGET_NAME = "target_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ==========================================
        // 1. 创建最外层的 ScrollView (赋予界面滑动能力)
        // ==========================================
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        scrollView.setFillViewport(true); // 保证内容不足时也能撑满全屏

        // ==========================================
        // 2. 创建内层的 LinearLayout (存放所有控件)
        // ==========================================
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        // 🌟 极其重要：内层的高度必须是 WRAP_CONTENT，否则 ScrollView 无法滑动
        layout.setLayoutParams(new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        layout.setGravity(Gravity.CENTER_HORIZONTAL); // 让所有控件在手表中间对齐

        // 🌟 针对圆形表盘的“防切边”处理
        int paddingVertical = dpToPx(30); // 上下各留出 60dp 的安全距离
        int paddingHorizontal = dpToPx(12); // 左右稍微留一点呼吸感
        layout.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);

        // ==========================================
        // 3. 实例化你的各个 UI 控件 (原版逻辑保留)
        // ==========================================
        TextView tv = new TextView(this);
        tv.setText(R.string.tvText);
        // 给文字稍微加点下边距，别跟输入框挨太紧
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvParams.bottomMargin = dpToPx(5);
        tv.setLayoutParams(tvParams);

        Button btn = new Button(this);
        btn.setText(R.string.openService);
        btn.setTextColor(Color.DKGRAY);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        EditText etDeviceName = new EditText(this);
        etDeviceName.setHint(R.string.examplePhone);
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedName = prefs.getString(KEY_TARGET_NAME, "");
        etDeviceName.setText(savedName);

        Button btnSave = new Button(this);
        btnSave.setText(R.string.saveDevice);
        btnSave.setTextColor(Color.DKGRAY);
        btnSave.setOnClickListener(v -> {
            String input = etDeviceName.getText().toString().trim();
            prefs.edit().putString(KEY_TARGET_NAME, input).apply();
            Toast.makeText(this, getString(R.string.saveTarget) + (input.isEmpty() ? getString(R.string.addDevice) : input), Toast.LENGTH_SHORT).show();
        });
        btnSave.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

        btn.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                return;
            }
            Intent serviceIntent = new Intent(this, BluetoothServerService.class);
            startForegroundService(serviceIntent);
            Toast.makeText(this, R.string.serviceOnline, Toast.LENGTH_SHORT).show();
        });

        Switch switchVibrate = new Switch(this);
        switchVibrate.setText(R.string.vibrateNotification);
        switchVibrate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        LinearLayout.LayoutParams paramsVibrate = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, // 改为 WRAP_CONTENT 更贴合
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paramsVibrate.topMargin = dpToPx(10);
        switchVibrate.setLayoutParams(paramsVibrate);

        SharedPreferences checkedPrefs = getSharedPreferences("DndSyncSettings", MODE_PRIVATE);
        boolean isVibrateEnabled = checkedPrefs.getBoolean("enable_vibrate", true);
        switchVibrate.setChecked(isVibrateEnabled);
        switchVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkedPrefs.edit().putBoolean("enable_vibrate", isChecked).apply();
        });

        // ==========================================
        // 4. 将所有控件按顺序装入 LinearLayout
        // ==========================================
        layout.addView(tv);
        layout.addView(etDeviceName);
        layout.addView(btnSave);
        layout.addView(btn);
        layout.addView(switchVibrate);

        // ==========================================
        // 5. 将 LinearLayout 装入 ScrollView，并显示
        // ==========================================
        scrollView.addView(layout);
        setContentView(scrollView);
    }

    // 辅助方法：将 dp 转换为 px
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
