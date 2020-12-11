package org.conch.security;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.conch.chain.CheckSumValidator;
import org.conch.common.Constants;
import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Logger;
import sun.net.util.IPAddressUtil;

import java.text.Format;
import java.text.SimpleDateFormat;
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
    /**
     * 若读取配置为空，则使用默认配置 * 放大倍率
     */
    public static final int MULTIPLE = 2;
    /**
     * Guard策略配置
     */
    public static int MAX_VICIOUS_COUNT_PER_SAME_HOST = 50;
    public static int FREQUENCY = 6 * MULTIPLE;
    public static int FREQUENCY_TO_BLACK = 20 * MULTIPLE;
    public static int MAX_THRESHOLD_PER_HOUR = 1 * MULTIPLE;
    public static int MAX_TOTAL_CONNECT_COUNT_PER_DAY = 500 * MULTIPLE;

    private static Integer threshold = 0;
    private static final Integer ONE_HOUR = 1000 * 60 * 60;
    private static long lastTime = System.currentTimeMillis();
    private static String lastDate = getCurrentDate(new Date());

    public static String getCurrentDate(Date date) {
        SimpleDateFormat ft = new SimpleDateFormat("yyyyMMdd");
        return ft.format(date);
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

    public static void main(String[] args) {
    }

    /**
     * 单个IP连接频率统计 & 防护
     * <p>
     * 定义频率阈值 FREQUENCY = 6次/min, 超过20次/min直接拉入黑名单
     * 每小时可超过阈值次数 threshold <= MAX_THRESHOLD_PER_HOUR
     * 每小时后将超过阈值次数 threshold 置零
     * 计算得出单日最大连接数 total = ( 20 * 1 + 6 * 59 ) * 24 == 8976
     * total值仍然过大，因此设定单日最大值 MAX_TOTAL_CONNECT_COUNT_PER_DAY = 500
     * <p>
     * 以上规则仅在本次程序生命周期有效，重启后会重新载入 & 统计
     * TODO 需完善重启后数据的持久化（考虑将数据存入数据库）
     * <p>
     * 目前黑名单策略：一旦触发便加入黑名单（过期10min），再次触发：
     * 1. 若未过期则更新黑名单开始时间
     * 2. 若过期，则重新加入黑名单
     *
     * @param host
     */
    public static void connectFrequencyStatistics(String host) {
        long startTime = System.currentTimeMillis();
        String startDate = getCurrentDate(new Date());
        try {
            // 检查是否 关闭状态
            if (FREQUENCY == -1 || Constants.isCloseGuard) {
                return;
            }
            if ("127.0.0.1".equals(host)
                    || "localhost".equals(host) || Constants.isDevnet() ? false : internalIp(host)) {
                // don't guard the local request
                return;
            }
            // 距离上一次执行该函数超过一天
            if (!startDate.equals(lastDate)) {
                // 将该日数据存储到指定文件
//                ConcurrentMap<String, JSONObject> map = Maps.newConcurrentMap();
//                map.put(lastDate, JSONObject.parseObject(JSON.toJSONString(BLACK_PEERS_MAP_2)));
//                JSONObject parseObject = JSONObject.parseObject(JSON.toJSONString(map));
//                org.conch.util.JSON.JsonAppendAlibaba(parseObject, "conf/guardData.json");
                // TODO 每日将数据存入数据库
                BLACK_PEERS_MAP_2 = Maps.newConcurrentMap();
            }
            JSONObject accessPeerObj = BLACK_PEERS_MAP_2.get(host);
            if (accessPeerObj == null) {
                accessPeerObj = new JSONObject();
                // 记录第一次访问时间
                accessPeerObj.put(FIRST_ACCESS_TIME_KEY, System.currentTimeMillis());
                // 赋值第一次访问时间
                accessPeerObj.put(LAST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY));
                // 赋值第一次访问时间
                accessPeerObj.put(LATEST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY));
                accessPeerObj.put(ACCESS_COUNT_KEY, 1);
            } else {
                // 上一次访问时间
                accessPeerObj.put(LAST_ACCESS_TIME_KEY, accessPeerObj.getLongValue(LATEST_ACCESS_TIME_KEY));
                // 更新这一次访问时间
                accessPeerObj.put(LATEST_ACCESS_TIME_KEY, System.currentTimeMillis());
                accessPeerObj.put(ACCESS_COUNT_KEY, accessPeerObj.getIntValue(ACCESS_COUNT_KEY) + 1);
            }
            // 将更新的内容存入MAP
            BLACK_PEERS_MAP_2.put(host, accessPeerObj);
            if (accessPeerObj.getLongValue(LATEST_ACCESS_TIME_KEY) > accessPeerObj.getLongValue(LAST_ACCESS_TIME_KEY)) {
                // 因初期时间间隔不足1min时，会导致分母过小致使frequency的值会过大，设定一个平均频率稳定期 stablePeriod = 1min
                float frequency = 0;
                long intervalTime = accessPeerObj.getLongValue(LATEST_ACCESS_TIME_KEY) - accessPeerObj.getLongValue(FIRST_ACCESS_TIME_KEY);
                if (intervalTime > 1000 * 60) {
                    // 计算平均连接频率
                    frequency = (accessPeerObj.getLongValue(ACCESS_COUNT_KEY) * 1000 * 60) / intervalTime;
                }
                if (startTime - lastTime > ONE_HOUR) {
                    // 距离上次执行该函数超过一小时
                    threshold = 0;
                }
                int total;
                if (frequency > FREQUENCY_TO_BLACK) {
                    // 拉入黑名单
                    blackPeer(host, String.format("Exceed the access max frequency number %d", FREQUENCY_TO_BLACK));
                } else if (frequency > FREQUENCY) {
                    // 记录 threshold
                    threshold++;
                    if (threshold > MAX_THRESHOLD_PER_HOUR) {
                        // 拉入黑名单
                        blackPeer(host, String.format("Exceed the access max frequency count %d", threshold));
                    }
                } else {
                    total = accessPeerObj.getIntValue(ACCESS_COUNT_KEY);
                    if (total > MAX_TOTAL_CONNECT_COUNT_PER_DAY) {
                        // 拉入黑名单
                        blackPeer(host, String.format("Exceed the access max count %d at one day", MAX_TOTAL_CONNECT_COUNT_PER_DAY));
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
