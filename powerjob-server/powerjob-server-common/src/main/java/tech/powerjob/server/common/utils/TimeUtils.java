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

    /**
     * 根据蔡勒公式计算任意一个日期是星期几
     * @param year 年
     * @param month 月
     * @param day 日
     * @return 中国星期
     */
    public static int calculateWeek(int year, int month, int day) {
        if (month == 1) {
            month = 13;
            year--;
        }
        if (month == 2) {
            month = 14;
            year--;
        }
        int y = year % 100;
        int c = year /100 ;
        int h = (y + (y / 4) + (c / 4) - (2 * c) + ((26 * (month + 1)) / 10) + day - 1) % 7;
        //可能是负值，因此计算除以7的余数之后需要判断是大于等于0还是小于0，如果小于0则将余数加7。
        if (h < 0){
            h += 7;
        }

        // 国内理解中星期日为 7
        if (h == 0) {
            return 7;
        }
        return h;
    }

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
