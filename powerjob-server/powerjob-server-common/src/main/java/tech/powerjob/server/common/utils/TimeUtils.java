package tech.powerjob.server.common.utils;

import tech.powerjob.common.RemoteConstant;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;
import java.util.List;

/**
 * 时间工具
 *
 * @author tjq
 * @since 2020/5/19
 */
@Slf4j
public class TimeUtils {

    /**
     * NTP 授时服务器（阿里云 -> 交大 -> 水果）
     */
    private static final List<String> NTP_SERVER_LIST = Lists.newArrayList("ntp.aliyun.com", "ntp.sjtu.edu.cn", "time1.apple.com");
    /**
     * 最大误差 5S
     */
    private static final long MAX_OFFSET = 5000;

    public static void check() throws TimeCheckException {

        NTPUDPClient timeClient = new NTPUDPClient();

        try {
            timeClient.setDefaultTimeout((int) RemoteConstant.DEFAULT_TIMEOUT_MS);
            for (String address : NTP_SERVER_LIST) {
                try {
                    TimeInfo t = timeClient.getTime(InetAddress.getByName(address));
                    NtpV3Packet ntpV3Packet = t.getMessage();
                    log.info("[TimeUtils] use ntp server: {}, request result: {}", address, ntpV3Packet);
                    // RFC-1305标准：https://tools.ietf.org/html/rfc1305
                    // 忽略传输误差吧...也就几十毫秒的事（阿里云给力啊！）
                    long local = System.currentTimeMillis();
                    long ntp = ntpV3Packet.getTransmitTimeStamp().getTime();
                    long offset =  local - ntp;
                    if (Math.abs(offset) > MAX_OFFSET) {
                        String msg = String.format("inaccurate server time(local:%d, ntp:%d), please use ntp update to calibration time", local, ntp);
                        throw new TimeCheckException(msg);
                    }
                    return;
                }catch (Exception ignore) {
                    log.warn("[TimeUtils] ntp server: {} may down!", address);
                }
            }
            throw new TimeCheckException("no available ntp server, maybe alibaba, sjtu and apple are both collapse");
        }finally {
            timeClient.close();
        }
    }



    public static final class TimeCheckException extends RuntimeException {
        public TimeCheckException(String message) {
            super(message);
        }
    }
}
