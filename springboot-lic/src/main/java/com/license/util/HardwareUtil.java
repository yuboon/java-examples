package com.license.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 硬件指纹获取工具类
 * 支持Windows和Linux系统的主板序列号获取
 */
@Component
public class HardwareUtil {

    private static final Logger logger = LoggerFactory.getLogger(HardwareUtil.class);

    /**
     * 获取主板序列号
     * @return 主板序列号，获取失败返回"UNKNOWN"
     */
    public String getMotherboardSerial() {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("windows")) {
                return getWindowsMotherboardSerial();
            } else if (os.contains("linux")) {
                return getLinuxMotherboardSerial();
            } else {
                logger.warn("不支持的操作系统: {}", os);
                return "UNKNOWN";
            }
        } catch (Exception e) {
            logger.error("获取主板序列号失败", e);
            return "UNKNOWN";
        }
    }

    /**
     * 获取Windows系统主板序列号
     */
    private String getWindowsMotherboardSerial() {
        try {
            Process process = Runtime.getRuntime().exec("wmic baseboard get serialnumber");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.equals("SerialNumber")) {
                    logger.debug("Windows主板序列号: {}", line);
                    return line;
                }
            }

            reader.close();
            process.waitFor();

        } catch (Exception e) {
            logger.error("获取Windows主板序列号失败", e);
        }

        return "UNKNOWN";
    }

    /**
     * 获取Linux系统主板序列号
     */
    private String getLinuxMotherboardSerial() {
        try {
            // 尝试通过dmidecode命令获取
            Process process = Runtime.getRuntime().exec("sudo dmidecode -s baseboard-serial-number");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null && !line.trim().isEmpty() && !line.contains("Not Specified")) {
                logger.debug("Linux主板序列号: {}", line.trim());
                return line.trim();
            }

            // 如果dmidecode失败，尝试读取/sys/class/dmi/id/board_serial
            return getLinuxMotherboardFromSys();

        } catch (Exception e) {
            logger.error("获取Linux主板序列号失败", e);
            return getLinuxMotherboardFromSys();
        }
    }

    /**
     * 从/sys/class/dmi/id/board_serial读取主板序列号
     */
    private String getLinuxMotherboardFromSys() {
        try {
            Process process = Runtime.getRuntime().exec("cat /sys/class/dmi/id/board_serial");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line = reader.readLine();
            reader.close();
            process.waitFor();

            if (line != null && !line.trim().isEmpty()) {
                logger.debug("Linux主板序列号(从sys读取): {}", line.trim());
                return line.trim();
            }

        } catch (Exception e) {
            logger.warn("从/sys/class/dmi/id/board_serial读取失败", e);
        }

        return "UNKNOWN";
    }

    /**
     * 获取系统信息摘要（用于调试）
     */
    public String getSystemInfo() {
        return String.format("OS: %s, Arch: %s, Motherboard: %s",
            System.getProperty("os.name"),
            System.getProperty("os.arch"),
            getMotherboardSerial()
        );
    }
}