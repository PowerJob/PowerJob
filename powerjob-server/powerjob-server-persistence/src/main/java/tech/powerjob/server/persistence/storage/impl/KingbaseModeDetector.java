package tech.powerjob.server.persistence.storage.impl;

/**
 * Kingbase 模式检测工具类
 *
 * 示例: jdbc:kingbase8://localhost:54321/powerjob-daily?productName=MySQL
 */
public class KingbaseModeDetector {

    public static String detectMode(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "unknown";
        }
        String lower = jdbcUrl.toLowerCase();
        int idx = lower.indexOf("productname=");
        if (idx == -1) {
            return "unknown";
        }
        String mode = lower.substring(idx + "productname=".length());
        if (mode.contains("&")) {
            mode = mode.substring(0, mode.indexOf("&"));
        }
        return mode.trim().toLowerCase();
    }
}
