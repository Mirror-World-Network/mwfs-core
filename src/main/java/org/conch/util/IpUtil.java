package org.conch.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/12/18
 */
public class IpUtil {

    /**
     * @param url don't include the port
     * @return
     */
    public static String checkOrToIp(String url) {
        try {
            if(StringUtils.isEmpty(url) 
            || "undefined".equalsIgnoreCase(url)){
                return "";    
            }
            
            if(!isDomain(url)) return url;
            return InetAddress.getByName(url).getHostAddress();

        } catch (UnknownHostException e) {
            Logger.logErrorMessage("can't finish checkOrToIp with url[" + url + "] caused by " + e.getMessage());
        }
        return "";
    }
    
    private final static List<String> natServers = Lists.newArrayList(
            "nat.mw.run",
            "nat.mwfs.io",
            "nat.sharder.network",
            "nat.sharder.org",
            "nat.sharder.io");
    
    private static String checkOrParseNatUrl(String url){
        if(StringUtils.isEmpty(url)) return "";
        for(String natServer : natServers) {
            if(!url.contains(natServer)) continue;
            
            // get domain
            int portIndex = url.indexOf(":");
            if(portIndex > 0) {
               return url.substring(0,portIndex);
            }
        }
        return "";
    }

    /**
     * @param url can't include the port
     */
    public static String getHost(String url){
        if(StringUtils.isEmpty(url)) return "";
        
        String parsedUrl = checkOrParseNatUrl(url);
        if(StringUtils.isNotBlank(parsedUrl)) return parsedUrl;
        
        try {
            return InetAddress.getByName(url).getHostName();
        } catch (UnknownHostException e) {
            Logger.logErrorMessage("can't finish getHost with url[" + url + "] caused by " + e.getMessage());
        }
        return "";
    }

    public static String getSenderIp(HttpServletRequest req) {
        return req.getHeader("x-forwarded-for") == null ? req.getRemoteAddr() : req.getHeader("x-forwarded-for");
    }

    public static boolean matchHost(HttpServletRequest req, String host) {
        String senderIp = getSenderIp(req);
        return senderIp.equals(checkOrToIp(host));
    }
    
    public static boolean matchHost(String host, String ip) {
        if(StringUtils.isEmpty(ip)) return false;
        return ip.equals(checkOrToIp(host));
    }
    
    public static boolean isFoundationDomain(String host){
        if(StringUtils.isEmpty(host)) return false;
        
        if(host.endsWith("sharder.io") 
        || host.endsWith("mwfs.io")
        || host.endsWith("mw.run")
        || host.endsWith("sharder.network")){
            return true;
        }
        
        return false;
    }

    /**
     * public ip -> internal ip
     * @return
     */
    public static String getNetworkIp() {
        try{
            String localIp = null;
            String netIp = null;

            Enumeration<NetworkInterface> netInterfaces = null;
            netInterfaces = NetworkInterface.getNetworkInterfaces();

            InetAddress ip = null;
            boolean fined = false;// 
            while (netInterfaces.hasMoreElements() && !fined) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> address = ni.getInetAddresses();
                while (address.hasMoreElements()) {
                    ip = address.nextElement();
                    if (!ip.isSiteLocalAddress() && !ip.isLoopbackAddress() && ip.getHostAddress().indexOf(":") == -1) {
                        netIp = ip.getHostAddress();
                        fined = true;
                        break;
                    } else if (ip.isSiteLocalAddress() && !ip.isLoopbackAddress()
                            && ip.getHostAddress().indexOf(":") == -1) {
                        // 
                        localIp = ip.getHostAddress();
                    }
                }
            }

            if (netIp != null && !"".equals(netIp)) {
                return netIp;
            } else {
                return localIp;
            }
        }catch (Exception e) {
            Logger.logErrorMessage("can't finish getNetworkIp caused by " + e.getMessage());
            return "";
        }
    }

    /* domain pattern */
    public static String PATTERN_L2DOMAIN = "\\w*\\.\\w*:";
    //    public static String PATTERN_IP = "(\\d*\\.){3}\\d*";
    public static String PATTERN_IP = "((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?)";

    /**
     * url whether is domain
     * @param url 
     * @return
     */
    public static boolean isDomain(String url){
        if(StringUtils.isEmpty(url)) return false;
        
        Pattern ipPattern = Pattern.compile(PATTERN_IP);
        Matcher matcher = ipPattern.matcher(url);
        return !matcher.find();
    }

    /**
     * get host from url
     * @param url domain ot url contains port
     * @return host: domain or public ip
     */
    public static String getHostFromUrl(String url) {
        if(isDomain(url) 
            && !url.contains(":") 
            && !url.contains("/") 
            && !url.contains("http")) {
            return url;
        }
        
        // return ip if ip pattern matched
        Pattern ipPattern = Pattern.compile(PATTERN_IP);
        Matcher matcher = ipPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }

        // return domain if ip pattern matched
        Pattern pattern = Pattern.compile(PATTERN_L2DOMAIN);
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            int endIndex = url.lastIndexOf(":");
            return url.substring(0, endIndex);
        }
        return null;
    }

    /**
     * get external ip from url
     * @param url domain ot url contains port
     * @return public network ip
     */
    public static String getIpFromUrl(String url) {
        String host = getHostFromUrl(url);
        return checkOrToIp(host);
    }
}
