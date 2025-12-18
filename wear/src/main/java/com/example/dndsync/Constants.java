package com.example.dndsync;
import java.util.UUID;

public class Constants {
    // 手机 -> 手表 的通道
    public static final UUID UUID_TO_WATCH = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // 手表 -> 手机 的通道 (改最后一位 B -> C)
    public static final UUID UUID_TO_PHONE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FC");
}