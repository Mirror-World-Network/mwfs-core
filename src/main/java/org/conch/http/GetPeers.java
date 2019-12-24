/*
 *  Copyright © 2017-2018 Sharder Foundation.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  version 2 as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, you can visit it at:
 *  https://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 *
 *  This software uses third party libraries and open-source programs,
 *  distributed under licenses described in 3RD-PARTY-LICENSES.
 *
 */

package org.conch.http;

import org.conch.peer.Peer;
import org.conch.peer.Peers;
import org.conch.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;

public final class GetPeers extends APIServlet.APIRequestHandler {
    static final HashMap CoordinatesMap = new HashMap();
    static final HashMap tempCoordinatesMap = new HashMap();
    static final GetPeers instance = new GetPeers();

    private GetPeers() {
        super(new APITag[] {APITag.NETWORK}, "active", "state", "service", "service", "service", "includePeerInfo", "includeOwn");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {

        boolean active = "true".equalsIgnoreCase(req.getParameter("active"));
        String stateValue = Convert.emptyToNull(req.getParameter("state"));
        String[] serviceValues = req.getParameterValues("service");
        boolean includePeerInfo = "true".equalsIgnoreCase(req.getParameter("includePeerInfo"));
        boolean includeOwn = "true".equalsIgnoreCase(req.getParameter("includeOwn"));
        Peer.State state;
        if (stateValue != null) {
            try {
                state = Peer.State.valueOf(stateValue);
            } catch (RuntimeException exc) {
                return JSONResponses.incorrect("state", "- '" + stateValue + "' is not defined");
            }
        } else {
            state = null;
        }
        long serviceCodes = 0;
        if (serviceValues != null) {
            for (String serviceValue : serviceValues) {
                try {
                    serviceCodes |= Peer.Service.valueOf(serviceValue).getCode();
                } catch (RuntimeException exc) {
                    return JSONResponses.incorrect("service", "- '" + serviceValue + "' is not defined");
                }
            }
        }

        Collection<? extends Peer> peers = active ? Peers.getActivePeers() : state != null ? Peers.getPeers(state) : Peers.getAllPeers();
        JSONArray peersJSON = new JSONArray();
        if (serviceCodes != 0) {
            final long services = serviceCodes;
            if (includePeerInfo) {
                peers.forEach(peer -> {
                    if (peer.providesServices(services)) {
                        peersJSON.add(JSONData.peer(peer));
                    }
                });
            } else {
                peers.forEach(peer -> {
                    if (peer.providesServices(services)) {
                        peersJSON.add(peer.getHost());
                    }
                });
            }
        } else {
            if (includePeerInfo) {
                peers.forEach(peer -> peersJSON.add(JSONData.peer(peer)));
            } else {
                peers.forEach(peer -> peersJSON.add(peer.getHost()));
            }
        }
        
        if(includeOwn) {
            JSONObject myPeerInfoJson = Peers.generateMyPeerJson();
            myPeerInfoJson.put("isOwn", "true");
            peersJSON.add(myPeerInfoJson);
        }
        
        // return my address which the peer list size is 0
        if(peersJSON.size() <= 0) {
            peersJSON.add(Peers.getMyAddress());
        }
        String startThis =  req.getParameter("startThis");
        JSONObject response = new JSONObject();
        tempCoordinatesMap.putAll(CoordinatesMap);
        if (startThis != null){
            if (CoordinatesMap.size() == 0  || (CoordinatesMap.get("peersLength") != null && (int)CoordinatesMap.get("peersLength") != peersJSON.size())){
                new Thread("换ip地址"){
                    public void run(){
                        final String result = byIPtoCoordinates("https://mwfs.io/api/front/coordinates/ip",JSONArray.toJSONString(peersJSON));
                        if (result.substring(0,8).equals("ErrorInfo")){
                            return;
                        }else{
                            CoordinatesMap.put("CoordinatesList",result);
                            CoordinatesMap.put("peersLength",peersJSON.size());
                            tempCoordinatesMap.putAll(CoordinatesMap);
                        }
                    }
                }.start();
            }
            response.put("coordinates",tempCoordinatesMap.get("CoordinatesList"));
        }
        response.put("peers", peersJSON);
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    public static String byIPtoCoordinates(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("content-type","application/json;charset=UTF-8");

            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            System.out.println(in);

            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！"+e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally{
            try{
                if(out!=null){
                    out.close();
                }
                if(in!=null){
                    in.close();
                }
            }
            catch(IOException ex){
                ex.printStackTrace();
            }
        }
        String ipXY = ",\"180.149.130.27\":{\"X\":\"39.9289\",\"Y\":\"116.3883\"},\"114.80.68.233\":{\"X\":\"31.0456\",\"Y\":\"121.3997\"}"
                +",\"223.104.96.113\":{\"X\":\"28.1375\",\"Y\":\"106.8200\"},\"14.215.160.13\":{\"X\":\"23.0268\",\"Y\":\"113.1315\"}"
                +",\"123.151.77.71\":{\"X\":\"39.1422\",\"Y\":\"117.1767\"},\"112.66.1.203\":{\"X\":\"20.0458\",\"Y\":\"110.3417\"}"
                +",\"113.116.52.10\":{\"X\":\"22.5333\",\"Y\":\"114.1333\"},\"49.83.135.23\":{\"X\":\"32.0617\",\"Y\":\"118.7778\"}"
                +",\"36.149.20.90\":{\"X\":\"39.9289\",\"Y\":\"116.3883\"},\"223.240.28.101\":{\"X\":\"31.8639\",\"Y\":\"117.2808\"}"
                +",\"23.251.52.66\":{\"X\":\"22.3310\",\"Y\":\"114.1592\"},\"113.76.101.170\":{\"X\":\"22.2769\",\"Y\":\"113.5678\"}"
                +"}";
        result = result.substring(0,result.length()-1)+ipXY;
        return result;
    }

    public static void main(String[] args){
        JSONArray peersJSON = new JSONArray();
        peersJSON.add("cn.testnat.sharder.io:8926");
        peersJSON.add("116.8.37.150");
        System.out.println(peersJSON.size());
        System.out.println(CoordinatesMap.toString());
        System.out.println(CoordinatesMap.get("peersLength"));
        String result= byIPtoCoordinates("http://localhost:8080/api/front/coordinates/ip",JSONArray.toJSONString(peersJSON));
        System.out.println("jiegou:"+result);
        result = result.substring(0,result.length()-1)+",\"116.8.37.150\":{\"X\":\"22.81667\",\"Y\":\"108.31667\"}}";
        System.out.println(result);

      /*  String result= byIPtoCoordinates("https://mwfs.io/api/front/coordinates/ip",JSONArray.toJSONString(peersJSON));
        System.out.println("lengh:"+CoordinatesMap.get("peersLengh"));
        System.out.println("jiegou:"+result);
        System.out.println(peersJSON.size());*/
    }
}
