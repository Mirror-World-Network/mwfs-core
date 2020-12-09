package org.conch.security;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Logger;
import sun.net.util.IPAddressUtil;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Used to guard the client to avoid the viciously tcp/ip connect,
 * api request and others resources consumption
 */
public class Guard {

    //TODO time based block list
    // black peer validation in : org.conch.peer.PeerServlet.process
    private static final int EXPIRED_TIME = 4 * (60 * 60 * 1000); //4hours
    private static final String FIRST_ACCESS_TIME_KEY = "firstAccessTime";
    private static final String LAST_ACCESS_TIME_KEY = "lastAccessTime";
    private static final String LATEST_ACCESS_TIME_KEY = "latestAccessTime";
    private static final String ACCESS_COUNT_KEY = "accessCount";
    private static Map<String, JSONObject> BLACK_PEERS_MAP = Maps.newConcurrentMap();
    private static Map<String, JSONObject> BLACK_PEERS_MAP_2 = Maps.newConcurrentMap();
    private static final int MAX_VICIOUS_COUNT_PER_SAME_HOST = 50;
    private static final int FREQUENCY = 6; // 6 times per minute
    private static final int FREQUENCY_GO_BLACK = 20;
    private static final int THRESHOLD_MAX = 1;
    private static final int TOTAL_MAX = 20;

    private static Integer threshold = 0;
    private static long lastTime = System.currentTimeMillis();
    private static String lastDate = getCurrentDate(new Date());

    public static String getCurrentDate(Date date) {
        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
        return ft.format(date);
    }

    public static void main(String[] args) {

    }

    public static boolean internalIp(String ip) {
        if (ip == null) {
            return false;
        }
        Logger.logDebugMessage("Verify LAN IP: " + ip);
        byte[] addr = IPAddressUtil.textToNumericFormatV4(ip);
        return internalIp(addr);
    }

    public static boolean internalIp(byte[] addr) {
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        //10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        //172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        //192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0) {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4) {
                    return true;
                }
            case SECTION_5:
                switch (b1) {
                    case SECTION_6:
                        return true;
                }
            default:
                return false;
        }
    }

    /**
     * 单个IP连接频率统计 & 防护
     * <p>
     * 定义频率阈值 FREQUENCY = 6次/min, 超过20次/min直接拉入黑名单
     * 每小时可超过阈值次数 threshold <= THRESHOLD_MAX
     * 每小时后将超过阈值次数 threshold 置零
     * 计算得出单日最大连接数 total = ( 20 * 1 + 6 * 59 ) * 24 == 8976
     * total值仍然过大，因此设定单日最大值 TOTAL_MAX = 500
     * <p>
     * 以上规则仅在本次程序生命周期有效，重启后会重新载入 & 统计
     * TODO 需完善重启后数据的持久化（考虑将数据存入数据库）
     *
     * 目前黑名单策略：一旦触发便加入黑名单（过期10min），再次触发：
     *  1. 若未过期则更新黑名单开始时间
     *  2. 若过期，则重新加入黑名单
     *
     * @param host
     */
    public static void connectFrequencyStatistics(String host) {
        long startTime = System.currentTimeMillis();
        String startDate = getCurrentDate(new Date());
        try {
            if ("127.0.0.1".equals(host)
                    || "localhost".equals(host) || Conch.getNetworkType() == "dev" ? false : internalIp(host)) {
                // don't guard the local request
                return;
            }
            if (!startDate.equals(lastDate)) {
                // 距离上一次执行该函数超过一天， map置空
                BLACK_PEERS_MAP_2 = Maps.newConcurrentMap();
            }
            JSONObject accessPeerObj = BLACK_PEERS_MAP_2.get(host);
            if (accessPeerObj == null) {
                accessPeerObj = new JSONObject();
                accessPeerObj.put(FIRST_ACCESS_TIME_KEY, System.currentTimeMillis()); // 记录第一次访问时间
                accessPeerObj.put(LAST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY)); // 赋值第一次访问时间
                accessPeerObj.put(LATEST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY)); // 赋值第一次访问时间
                accessPeerObj.put(ACCESS_COUNT_KEY, 1);
            } else {
                accessPeerObj.put(LAST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(LATEST_ACCESS_TIME_KEY)); // 上一次访问时间
                accessPeerObj.put(LATEST_ACCESS_TIME_KEY, System.currentTimeMillis()); // 更新这一次访问时间
                accessPeerObj.put(ACCESS_COUNT_KEY, accessPeerObj.getIntValue(ACCESS_COUNT_KEY) + 1);
            }
            // 将更新的内容存入MAP
            BLACK_PEERS_MAP_2.put(host, accessPeerObj);
            if (accessPeerObj.getLongValue(LATEST_ACCESS_TIME_KEY) > accessPeerObj.getLongValue(LAST_ACCESS_TIME_KEY)) {
                // 计算第一次访问到这一次访问的时间段内,IP的连接频率
                long frequency = (accessPeerObj.getLongValue(ACCESS_COUNT_KEY) * 1000 * 60) / (accessPeerObj.getLongValue(LATEST_ACCESS_TIME_KEY) - accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY));
                if (startTime - lastTime > 1000 * 60 * 60) {
                    // 距离上次执行该函数超过一小时
                    threshold = 0;
                }
                int total;
                if (frequency > FREQUENCY_GO_BLACK) {
                    // 拉入黑名单
                    blackPeer(host, String.format("Exceed the access max frequency number %d", FREQUENCY_GO_BLACK));
                } else if (frequency > FREQUENCY) {
                    // 记录 threshold
                    threshold++;
                    if (threshold > THRESHOLD_MAX) {
                        // 拉入黑名单
                        blackPeer(host, String.format("Exceed the access max frequency count %d", threshold));
                    }
                } else {
                    total = accessPeerObj.getIntValue(ACCESS_COUNT_KEY);
                    if (total > TOTAL_MAX) {
                        // 拉入黑名单
                        blackPeer(host, String.format("Exceed the access max count %d at one day", TOTAL_MAX));
                    }
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } finally {
            // 替换上一次执行时间
            lastTime = startTime;
            lastDate = startDate;
        }

    }


    public static void viciousAccess(String host) {
        if ("127.0.0.1".equals(host)
                || "localhost".equals(host) || internalIp(host)) {
            // don't guard the local request
            return;
        }

        JSONObject accessPeerObj = BLACK_PEERS_MAP.get(host);
        if (accessPeerObj == null) {
            accessPeerObj = new JSONObject();
            accessPeerObj.put(FIRST_ACCESS_TIME_KEY, System.currentTimeMillis());
            accessPeerObj.put(ACCESS_COUNT_KEY, 1);
        } else {
            accessPeerObj.put(LAST_ACCESS_TIME_KEY, System.currentTimeMillis());
            accessPeerObj.put(ACCESS_COUNT_KEY, accessPeerObj.getIntValue(ACCESS_COUNT_KEY) + 1);
        }
//        else if (accessPeerObj.getLong(ACCESS_TIME_KEY) + EXPIRED_TIME > System.currentTimeMillis()) {
//            return accessPeerObj;
//        }

        if (accessPeerObj.getIntValue(ACCESS_COUNT_KEY) >= MAX_VICIOUS_COUNT_PER_SAME_HOST) {
            blackPeer(host, String.format("Exceed the vicious access max count %d", MAX_VICIOUS_COUNT_PER_SAME_HOST));
        }
    }

    public static boolean blackPeer(String host, String cause) {
        Peer peer = Peers.getPeer(host, true);
        if (peer == null) {
            rejectPeer(host);
        } else {
            peer.blacklist(String.format("Black the peer %s[%s] caused by %s", peer.getAnnouncedAddress(), peer.getHost(), cause));
        }
        return true;
    }

    public static boolean rejectPeer(String host) {
        addRejectRuleIntoFirewall(host);
        return true;
    }

    /**
     * call the shell to add reject rule into firewall of OS
     * - just support the CentOS and firewalld
     */
    private static void addRejectRuleIntoFirewall(String host) {
        Logger.logInfoMessage("Not implement addRejectRuleIntoFirewall now");
    }
}
