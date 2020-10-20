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
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.ConchException;
import org.conch.consensus.genesis.GenesisRecipient;
import org.conch.http.biz.BizParameterRequestWrapper;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;
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

    private static String defaul = Conch.getStringProperty("sharder.batchTransfer.path");

    private BatchSendMoney() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "pathAndFileName");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ConchException {
        org.json.simple.JSONObject response = new org.json.simple.JSONObject();

        String pathAndFileName = req.getParameter("pathAndFileName");
        String pathStr;

        pathStr = pathAndFileName==null?defaul:pathAndFileName;
        // parse file
        String jsonStr = readJsonFile(pathStr);
        JSONObject jobj = JSON.parseObject(jsonStr);
        // Get common property
        Map<String, String[]> paramter = new HashMap(16);
        paramter.put("secretPhrase", new String[]{jobj.getString("secretPhrase")});
        paramter.put("feeNQT", new String[]{jobj.getString("feeNQT")});
        paramter.put("deadline", new String[]{jobj.getString("deadline")});

        JSONArray list = jobj.getJSONArray("list");
        List<BatchTransfer> transferList = JSONObject.parseArray(list.toJSONString(), BatchTransfer.class);
        List<JSONStreamAware> transactionList = new ArrayList<>();
        JSONArray failTransferArray = new JSONArray();
        for (BatchTransfer batchTransfer : transferList) {
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
                        transactionList.add(transaction);
                    } else {
                        Object json = JSON.toJSON(batchTransfer);
                        transactionJsonObject.put("transfer", json);
                        failTransferArray.add(transactionJsonObject);
                    }
                } else {
                    failTransferArray.add(JSON.toJSON(batchTransfer));
                }
            } catch (ConchException e) {
                e.printStackTrace();
                failTransferArray.add(JSON.toJSON(batchTransfer));
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
