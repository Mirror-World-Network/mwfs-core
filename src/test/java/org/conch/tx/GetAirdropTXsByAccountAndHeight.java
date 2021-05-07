package org.conch.tx;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.db.Db;
import org.conch.db.DbUtils;
import org.conch.http.Airdrop;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 生成指定高度、指定账户的空投缺漏文件
 * eg： 在某高度原空投文件 是 空投给117个账户，实际只空投了112个账户，缺漏了5个账户。通过该方法可以自动生成缺漏账户的空投文件，生成后可直接进行补充性空投
 * @author bowen
 * @date 2021/05/06
 */
public class GetAirdropTXsByAccountAndHeight {
    /**
     *
     * @param address 空投发出地址
     * @param queryHeight 空投发生的高度
     * @param airdropFilename 空投原文件
     * @param missFilename 生成的空投缺漏文件
     */
    public static void getTxsByAccountAndHeight(String address, int queryHeight, String airdropFilename, String missFilename) {
        Connection con = null;
        PreparedStatement pstmt = null;
        String sqlTransfer = "SELECT * FROM TRANSACTION WHERE VERSION>=3 AND TYPE=0 AND SUBTYPE=0 AND HEIGHT=? AND SENDER_ID=?";
        // 获取空投文件的所有账户，组成账户列表
        com.alibaba.fastjson.JSONObject parseObject = com.alibaba.fastjson.JSON.parseObject(JSON.readJsonFile(airdropFilename));
        com.alibaba.fastjson.JSONArray listOrigin = parseObject.getJSONArray("list");
        List<Airdrop.TransferInfo> list = com.alibaba.fastjson.JSONObject.parseArray(listOrigin.toJSONString(), Airdrop.TransferInfo.class);
        ArrayList<Object> arrayList = Lists.newArrayList();
        HashMap<Object, Object> newHashMap = Maps.newHashMap();
        for (Airdrop.TransferInfo info : list) {
            String recipientRS = info.getRecipientRS();
            arrayList.add(recipientRS);
            newHashMap.put(recipientRS, info);
        }
        try {
            con = Db.db.getConnection();
            // transfer statistics
            pstmt = con.prepareStatement(sqlTransfer);
            pstmt.setInt(1, queryHeight);
            pstmt.setLong(2, Account.rsAccountToId(address));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    long recipientId = rs.getLong("RECIPIENT_ID");
                    String rsAccount = Account.rsAccount(recipientId);
                    if (arrayList.contains(rsAccount)) {
                        // 删除该账户的空投记录
                        list.remove(newHashMap.get(rsAccount));
                    }
                }
            }
            // write jsonArray to jsonFile
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("list", com.alibaba.fastjson.JSON.toJSON(list));
            jsonObject.put("secretPhrase", parseObject.getString("secretPhrase"));
            jsonObject.put("feeNQT", parseObject.getString("feeNQT"));
            jsonObject.put("deadline", parseObject.getString("deadline"));
            JSON.JsonWrite(jsonObject, missFilename);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            DbUtils.close(con, pstmt);
        }
    }

    public static void main(String[] args) {
        Db.init();
        String address = "CDW-U6AA-JDEU-HG85-AWB8F";
        int queryHeight = 25471;
        String missFilename = "conf/airdropMiss.json";
        String airdropFilename = "conf/investbatchTransfer.json";
        getTxsByAccountAndHeight(address, queryHeight, airdropFilename, missFilename);
    }
}
