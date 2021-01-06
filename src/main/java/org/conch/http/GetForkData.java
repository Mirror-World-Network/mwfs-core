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
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.http.biz.domain.Block;
import org.conch.http.biz.domain.ForkObj;
import org.conch.http.biz.domain.Peer;
import org.conch.peer.Peers;
import org.conch.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * GetForkData
 *
 * @author bowen
 * @date 2021/1/5
 */

public class GetForkData extends APIServlet.APIRequestHandler {

    enum Level {
        // 18
        SMALL,
        // 144
        MEDIUM,
        // 432
        LONG
    }
    /**
     * ignore multiple forks, only think of the height and difficulty
     */


    static final GetForkData instance = new GetForkData();

    private GetForkData() {
        super(new APITag[] {APITag.ACCOUNTS}, "secretPhrase", "publicKey");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        int latestNum = 144;
        JSONObject response = new JSONObject();
        // through bootNode get whole network peers that are active and public network IP
        Set<String> filteredPeerHosts = Peers.getWholeNetActiveAndPublicNetIPPeersByBootNode();
        // get blocks info of filteredPeers, and blocks data struct convert to forkObj
        Map<String, ForkObj> forkObjMap = Peers.getForkObjMap(filteredPeerHosts, latestNum);
        response.put("forkObjs", forkObjMap);
        return response;
    }

}
