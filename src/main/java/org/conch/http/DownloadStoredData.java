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

import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.common.ConchException;
import org.conch.storage.Ssid;
import org.conch.storage.tx.StorageTxProcessorImpl;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.util.Convert;
import org.conch.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.conch.http.JSONResponses.*;

public final class DownloadStoredData extends APIServlet.APIRequestHandler {

    static final DownloadStoredData instance = new DownloadStoredData();

    private DownloadStoredData() {
        super(new APITag[] {APITag.DATA_STORAGE}, "transaction","ssid");
    }

    /**
     * Used to fetch the ssid details before download the stored object
     * @param ssid
     * @return
     */
    private JSONStreamAware fetchSsidInfo(String ssid){
        if (ssid == null) return null;

        JSONObject json = new JSONObject();
        json.put("Code", 200);
        json.put("ipfsHashId",Ssid.decode(ssid));
        json.put("port",Conch.getStringProperty("sharder.storage.ipfs.gateway.port"));
        return JSON.prepare(json);
    }

    /**
     * Fetch the ss object and return the stream to response
     * @param request
     * @param response
     * @return
     * @throws ParameterException
     */
    private JSONStreamAware downloadSsObejctStream(HttpServletRequest request, HttpServletResponse response) throws ParameterException {
        String transactionIdString = Convert.emptyToNull(request.getParameter("transaction"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }
        long transactionId = 0;
        Transaction transaction;
        try {
            if (transactionIdString != null) {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Conch.getBlockchain().getTransaction(transactionId);
            } else {
                return UNKNOWN_TRANSACTION;
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }
        Attachment.DataStorageUpload attachment = (Attachment.DataStorageUpload) transaction.getAttachment();

        if (attachment == null) {
            return JSONResponses.incorrect("transaction", "stored data not found");
        }

        //Retrieve the data by ssid
        byte[] data;
        try {
            data = StorageTxProcessorImpl.getInstance().getData(transactionId);
        } catch (IOException e) {
            return JSONResponses.error("stored data not found");
        }
        if (!attachment.getType().equals("")) {
            response.setContentType(attachment.getType());
        } else {
            response.setContentType("application/octet-stream");
        }


        String filename = attachment.getName().trim();
        String contentDisposition = "attachment";
        try {
            URI uri = new URI(null, null, filename, null);
            contentDisposition += "; filename*=UTF-8''" + uri.toASCIIString();
        } catch (URISyntaxException ignore) {

        }

        // write the data into response
        response.setHeader("Content-Disposition", contentDisposition);
        response.setContentLength(data.length);
        try (OutputStream out = response.getOutputStream()) {
            try {
                out.write(data);
            } catch (IOException e) {
                throw new ParameterException(JSONResponses.RESPONSE_WRITE_ERROR);
            }
        } catch (IOException e) {
            throw new ParameterException(JSONResponses.RESPONSE_STREAM_ERROR);
        }

        return null;
    }

//    /**
//     * Forward the download request to ipfs server.
//     * e.g. localhost/downloadStoredData/ssid -> localhost:8088/ipfs/{ssid}
//     * @param ssid
//     * @return
//     */
//    private void forwardDownloadLink(HttpServletResponse response){
//        response.sendRedirect("/ipfs/");
//    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request, HttpServletResponse response) throws ConchException {
        String ssid = Convert.emptyToNull(request.getParameter("ssid"));
        boolean justFetch = StringUtils.isNotEmpty(ssid);

        if(justFetch){
            return fetchSsidInfo(ssid);
        }else {
            return downloadSsObejctStream(request, response);
        }
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws ConchException {
        throw new UnsupportedOperationException();
    }

}
