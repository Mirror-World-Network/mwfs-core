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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.http.biz.BizParameterRequestWrapper;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.conch.http.JSONResponses.incorrect;
import static org.conch.util.JSON.readJsonFile;

public final class BatchSendMoney extends CreateTransaction {

    static final BatchSendMoney instance = new BatchSendMoney();
    static class BatchTransfer {
        public String recipient;
        public String amountNQT;
        public String recipientPublicKey;

        BatchTransfer() {}

        @Override
        public String toString() {
            return "BatchTransfer{" +
                    "recipient='" + recipient + '\'' +
                    ", amountNQT='" + amountNQT + '\'' +
                    ", recipientPublicKey='" + recipientPublicKey + '\'' +
                    '}';
        }
    }

    private static String defaulPathName = Conch.getStringProperty("sharder.airdrop.pathName");


    private static List<String> validKeys = Conch.getStringListProperty("sharder.airdrop.validKeys");


    private BatchSendMoney() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "pathAndFileName", "key");
    }

    private boolean verifyKey(String key) {
        for (String validKey : validKeys) {
            if (validKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        org.json.simple.JSONObject response = new org.json.simple.JSONObject();

        String pathAndFileName = req.getParameter("pathAndFileName");
        String key = req.getParameter("key");
        String pathStr;

        if (!verifyKey(key)) {
            throw new ParameterException(incorrect("key", String.format("key %s is incorrect", key)));
        }

        pathStr = pathAndFileName==null?defaulPathName:pathAndFileName;
        // parse file
        String jsonStr = readJsonFile(pathStr);
        JSONObject jobj = JSON.parseObject(jsonStr);

        Map<String, String[]> paramter = Maps.newHashMap();
        paramter.put("secretPhrase", new String[]{jobj.getString("secretPhrase")});
        paramter.put("feeNQT", new String[]{jobj.getString("feeNQT")});
        paramter.put("deadline", new String[]{jobj.getString("deadline")});

        JSONArray list = jobj.getJSONArray("list");
        List<BatchTransfer> transferList = JSONObject.parseArray(list.toJSONString(), BatchTransfer.class);
        List<JSONStreamAware> transactionList = new ArrayList<>();
        JSONArray failTransferArray = new JSONArray();
        for (BatchTransfer batchTransfer : transferList) {
            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            try {
                paramter.put("recipient", new String[]{batchTransfer.recipient});
                paramter.put("recipientPublicKey", new String[]{batchTransfer.recipientPublicKey});
                paramter.put("amountNQT", new String[]{batchTransfer.amountNQT});
                BizParameterRequestWrapper reqWrapper = new BizParameterRequestWrapper(req, req.getParameterMap(), paramter);
                Account account = ParameterParser.getSenderAccount(reqWrapper);

                long recipient = ParameterParser.getAccountId(reqWrapper, "recipient", true);
                long amountNQT = ParameterParser.getAmountNQT(reqWrapper);
                JSONStreamAware transaction = createTransaction(reqWrapper, account, recipient, amountNQT);
                if (null != transaction) {
                    org.json.simple.JSONObject transactionJsonObject = (org.json.simple.JSONObject) transaction;
                    if (transactionJsonObject.get("broadcasted").equals(true)) {
                        // transaction was created successfully and broadcast
                        // TODO remove non-essential attributes to simplify the return results
                        transactionList.add(transaction);
                    } else {
                        jsonObject.put("transfer", JSON.toJSON(batchTransfer));
                        jsonObject.put("errorResponse", transaction);
                        failTransferArray.add(jsonObject);
                    }
                } else {
                    jsonObject.put("transfer", JSON.toJSON(batchTransfer));
                    jsonObject.put("errorResponse", transaction);
                    failTransferArray.add(jsonObject);
                }
            }catch (ParameterException e) {
                jsonObject.put("transfer", JSON.toJSON(batchTransfer));
                org.json.simple.JSONObject errorResponse = (org.json.simple.JSONObject) JSONValue.parse(org.conch.util.JSON.toString(e.getErrorResponse()));
                jsonObject.put("errorResponse", errorResponse);
                failTransferArray.add(jsonObject);
            }catch (ConchException e) {
                e.printStackTrace();
            } finally {
            }
        }
        response.put("transferSuccessList", transactionList);
        response.put("transferFailList", failTransferArray);
        response.put("transferTotalCount", list.size());
        response.put("transferSuccessCount", transactionList.size());
        return response;
    }
}
