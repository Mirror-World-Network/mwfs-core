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
import org.conch.common.ConchException;
import org.conch.http.biz.BizParameterRequestWrapper;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.conch.http.JSONResponses.*;
import static org.conch.util.JSON.readJsonFile;

public final class AirdropDetection extends CreateTransaction {

    static final AirdropDetection instance = new AirdropDetection();

    /**
     *  default airdrop JSON fileName
     */
    private static final String DEFAULT_PATH_NAME = Conch.getStringProperty("sharder.airdrop.pathName");
    /**
     * list of valid keys used for validation
     */
    private static final List<String> VALID_KEYS = Conch.getStringListProperty("sharder.airdrop.validKeys");
    /**
     * airdrop switch
     */
    private static final boolean ENABLE_AIRDROP = Conch.getBooleanProperty("sharder.airdrop.enable");

    private AirdropDetection() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "pathAndFileName", "key");
    }

    private boolean verifyKey(String key) {
        for (String validKey : VALID_KEYS) {
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
        if (!ENABLE_AIRDROP) {
            return ACCESS_CLOSED;
        }
        if (!verifyKey(key)) {
            throw new ParameterException(incorrect("key", String.format("key %s is incorrect", key)));
        }

        // parse file
        pathName = pathName == null ? DEFAULT_PATH_NAME : pathName;
        String jsonStr = readJsonFile(pathName);
        JSONObject jobj = JSON.parseObject(jsonStr);

        // read file error
        if (jobj.get("error") != null) {
            return JSONResponses.fileNotFound(pathName.split("/")[1] != null ? pathName.split("/")[1] : pathName);
        }

        JSONArray doneListOrigin = jobj.getJSONArray("doneList");
        List<Airdrop.TransferInfo> doneList = doneListOrigin == null ? new ArrayList<>() : JSONObject.parseArray(doneListOrigin.toJSONString(), Airdrop.TransferInfo.class);
        if (doneList.isEmpty()) {
            return MISSING_TRANSACTION;
        }
        JSONArray detectionResponse = new JSONArray();
        for (Airdrop.TransferInfo info : doneList) {
            org.json.simple.JSONObject jsonObject = new org.json.simple.JSONObject();
            Map<String, String[]> paramter = Maps.newHashMap();
            paramter.put("transactionID", new String[]{info.transactionID});
            jsonObject.put("transactionID", info.transactionID);
            BizParameterRequestWrapper reqWrapper = new BizParameterRequestWrapper(req, req.getParameterMap(), paramter);

            String transactionIdString = Convert.emptyToNull(reqWrapper.getParameter("transactionID"));
            if (transactionIdString == null) {
                jsonObject.put("errorResponse", MISSING_TRANSACTION);
            }
            boolean includePhasingResult = "true".equalsIgnoreCase(reqWrapper.getParameter("includePhasingResult"));

            long transactionId = 0;
            Transaction transaction = null;
            try {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Conch.getBlockchain().getTransaction(transactionId);
            } catch (RuntimeException e) {
                jsonObject.put("errorResponse", JSONValue.parse(org.conch.util.JSON.toString(INCORRECT_TRANSACTION)));
            }

            if (transaction == null) {
                transaction = Conch.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
                if (transaction == null) {
                    jsonObject.put("errorResponse", JSONValue.parse(org.conch.util.JSON.toString(UNKNOWN_TRANSACTION)));
                } else {
                    jsonObject.put("unconfirmedTransaction", JSONData.unconfirmedTransaction(transaction));
                }
            } else {
                jsonObject.put("confirmedTransaction", JSONData.transaction(transaction, includePhasingResult));
            }
            detectionResponse.add(jsonObject);
        }

        response.put("detectionCount", detectionResponse.size());
        response.put("detectionResponse", detectionResponse);
        return response;
    }
}
