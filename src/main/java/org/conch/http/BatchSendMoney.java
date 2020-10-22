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
import java.util.List;
import java.util.Map;

import static org.conch.http.JSONResponses.*;
import static org.conch.util.JSON.JsonWrite;
import static org.conch.util.JSON.readJsonFile;

public final class BatchSendMoney extends CreateTransaction {

    static final BatchSendMoney instance = new BatchSendMoney();
    static class TransferInfo {
        public String recipient;
        public String amountNQT;
        public String recipientPublicKey;
        public String errorDescription; // create transaction failed to write to this value
        public String transactionID; // create transaction succeed to write to this value

        TransferInfo() {}
    }
    // default airdrop JSON fileName
    private static String defaulPathName = Conch.getStringProperty("sharder.airdrop.pathName");
    // list of valid keys used for validation
    private static List<String> validKeys = Conch.getStringListProperty("sharder.airdrop.validKeys");
    // airdrop switch
    private static final boolean enableAirdrop = Conch.getBooleanProperty("sharder.airdrop.enable");
    // airdrop append Mode
    private static final boolean isAppendMode = Conch.getBooleanProperty("sharder.airdrop.isAppendMode");

    private BatchSendMoney() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "pathName", "key");
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
        String pathName = req.getParameter("pathName");
        String key = req.getParameter("key");
        if (!enableAirdrop) {
            return ACCESS_CLOSED;
        }
        if (!verifyKey(key)) {
            throw new ParameterException(incorrect("key", String.format("key %s is incorrect", key)));
        }

        // parse file
        pathName = pathName==null?defaulPathName:pathName;
        String jsonStr = readJsonFile(pathName);
        JSONObject jobj = JSON.parseObject(jsonStr);

        // read file error
        if (jobj.get("error") != null) {
            return JSONResponses.fileNotFound(pathName.split("/")[1] != null?pathName.split("/")[1]:pathName);
        }

        Map<String, String[]> paramter = Maps.newHashMap();
        paramter.put("secretPhrase", new String[]{jobj.getString("secretPhrase")});
        paramter.put("feeNQT", new String[]{jobj.getString("feeNQT")});
        paramter.put("deadline", new String[]{jobj.getString("deadline")});

        JSONArray listOrigin = jobj.getJSONArray("list");
        JSONArray doneListOrigin = jobj.getJSONArray("doneList");
        JSONArray failListOrigin = jobj.getJSONArray("failList");
        if (listOrigin == null) {
            return MISSING_TRANSACTION;
        }
        if (!isAppendMode) {
            doneListOrigin = null;
            failListOrigin = null;
        }
        List<TransferInfo> list = JSONObject.parseArray(listOrigin.toJSONString(), TransferInfo.class);
        // record existing lists, append pattern
        List<TransferInfo> doneList = doneListOrigin == null?new ArrayList<>():JSONObject.parseArray(doneListOrigin.toJSONString(), TransferInfo.class);
        List<TransferInfo> failList = failListOrigin == null?new ArrayList<>():JSONObject.parseArray(failListOrigin.toJSONString(), TransferInfo.class);
        // record the lists that unhandled the exception
        List<TransferInfo> pendingList = new ArrayList<>();

        JSONArray transferSuccessList = new JSONArray();
        JSONArray transferFailList = new JSONArray();
        for (TransferInfo info : list) {
            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            try {
                paramter.put("recipient", new String[]{info.recipient});
                paramter.put("recipientPublicKey", new String[]{info.recipientPublicKey});
                paramter.put("amountNQT", new String[]{info.amountNQT});
                paramter.put("transactionID", new String[]{info.transactionID});
                paramter.put("errorDescription", new String[]{info.errorDescription});

                BizParameterRequestWrapper reqWrapper = new BizParameterRequestWrapper(req, req.getParameterMap(), paramter);
                Account account = ParameterParser.getSenderAccount(reqWrapper);

                long recipient = ParameterParser.getAccountId(reqWrapper, "recipient", true);
                long amountNQT = ParameterParser.getAmountNQT(reqWrapper);

                JSONStreamAware transaction = createTransaction(reqWrapper, account, recipient, amountNQT);
                org.json.simple.JSONObject transactionJsonObject = (org.json.simple.JSONObject) transaction;
                if (transactionJsonObject.get("broadcasted") != null && transactionJsonObject.get("broadcasted").equals(true)) {
                    // transaction was created successfully and broadcast
                    transferSuccessList.add(transaction);
                    // write info to the doneList
                    info.transactionID = (String) transactionJsonObject.get("transaction");
                    doneList.add(info);
                } else {
                    jsonObject.put("transfer", JSON.toJSON(info));
                    jsonObject.put("errorResponse", transaction);
                    transferFailList.add(jsonObject);
                    // write info to failList
                    info.errorDescription = (String) transactionJsonObject.get("errorDescription");
                    failList.add(info);
                }

            }catch (ParameterException e) {
                org.json.simple.JSONObject errorResponse = (org.json.simple.JSONObject) JSONValue.parse(org.conch.util.JSON.toString(e.getErrorResponse()));

                jsonObject.put("transfer", JSON.toJSON(info));
                jsonObject.put("errorResponse", errorResponse);
                transferFailList.add(jsonObject);
                info.errorDescription = (String) errorResponse.get("errorDescription");
                failList.add(info);
            }catch (ConchException e) {
                e.printStackTrace();

                jsonObject.put("transfer", JSON.toJSON(info));
                jsonObject.put("errorResponse", e.getMessage());
                transferFailList.add(jsonObject);

                info.errorDescription = e.getMessage();
                pendingList.add(info);
            }catch (Exception e) {
                // catch all exception, ensure that processing does not break
                e.printStackTrace();

                jsonObject.put("transfer", JSON.toJSON(info));
                jsonObject.put("errorResponse", e.getMessage());
                transferFailList.add(jsonObject);

                info.errorDescription = e.getMessage();
                pendingList.add(info);
            }
        }
        JSONStreamAware write = writeToJSON(doneList, failList, pendingList, jobj, pathName);
        if (write != null) {
            response.put("writeError", JSONValue.parse(org.conch.util.JSON.toString(write)));
        }
        response.put("transferSuccessList", transferSuccessList);
        response.put("transferFailList", transferFailList);
        response.put("transferTotalCount", list.size());
        response.put("transferSuccessCount", transferSuccessList.size());

        return response;
    }

    /**
     * write the return result to the specified JSON file, containing：
     *  1. doneList: (createTransaction success)
     *      public String recipient;
     *      public String amountNQT;
     *      public String recipientPublicKey;
     *      public String transactionID;
     *  2. failList: (createTransaction fail)
     *      public String recipient;
     *      public String amountNQT;
     *      public String recipientPublicKey;
     *      public String errorDescription;
     *  3. list: (exception not handled)
     *      public String recipient;
     *      public String amountNQT;
     *      public String recipientPublicKey;
     *  4. basic information：
     *      "secretPhrase": "***",
     *      "feeNQT": "0",
     *      "deadline": "1440",
     *
     */
    private JSONStreamAware writeToJSON(List<TransferInfo> doneList, List<TransferInfo> failList, List<TransferInfo> pendingList, JSONObject jobj, String pathName) {
        org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();

        jsonObject.put("secretPhrase", jobj.getString("secretPhrase"));
        jsonObject.put("feeNQT", jobj.getString("feeNQT"));
        jsonObject.put("deadline", jobj.getString("deadline"));

        jsonObject.put("doneList", JSON.toJSON(doneList));
        jsonObject.put("failList", JSON.toJSON(failList));
        jsonObject.put("list", JSON.toJSON(pendingList));

        // write to json file
        String jsonWrite = JsonWrite(jsonObject, pathName);
        JSONObject jsonObjectWrite = JSON.parseObject(jsonWrite);

        // read file error
        if (jsonObjectWrite != null && jsonObjectWrite.get("error") != null) {
            return JSONResponses.writeFileFail(pathName.split("/")[1] != null?pathName.split("/")[1]:pathName);
        } else {
            return null;
        }
    }
}
