import global from "../utils/common";

const SERVER_API = "http://localhost:7216";
// const SERVER_API = "https://biz.sharder.io";
// const SERVER_API_DEV = "47.107.188.3:8215";
// const SERVER_API_DEV = "http://localhost:8215";
// const SERVER_API_TEST = "http://localhost:8215";
const SERVER_API_DEV = "http://localhost:9216";
const SERVER_API_TEST = "http://localhost:7216";
const SHARDER_URL = process.env.NODE_ENV === 'development' ? "http://localhost:8080" : (global.projectName === 'mw' ? "https://mw.run" : "https://sharder.org");
const MGR_URL = process.env.NODE_ENV === 'development' ? "http://192.168.0.104:8080" : "https://mw.run/admin";
const HECO_URL = process.env.NODE_ENV === 'development' ? "https://testnet.hecoinfo.com" : "https://hecoinfo.com";

function getUrl() {
    if (window.location.port === '4000') {
        return SERVER_API_TEST;
    } else {
        return "";
    }
}

const api = {
    apiUrl: getUrl(),
    sharderUrl: getUrl() + "/sharder",
    sharderProxyUrl: getUrl() + "/sharder-proxy",
    sharderExchange: SHARDER_URL + "/official/exchange.ss",
    ssContactAmount: SHARDER_URL + "/official/getExchangeAmount.ss",
    sharderExchangeRS: SHARDER_URL + "/official/exchange/rs.ss",
    sharderExchangeSSA: SHARDER_URL + "/official/address/ssa.ss",
    simulatedPositioningUrl:SHARDER_URL +"/coordinates/getSPUrl.ss",

    //跨链请求
    getAccountInfoUrl: MGR_URL + "/api/front/acrossChain/getAccountInfo",
    updateChainAccountUrl: MGR_URL + "/api/front/acrossChain/updateChainAccount",
    getRecordUrl: MGR_URL + "/api/front/acrossChain/getRecord",
    getAddress: MGR_URL + "/api/front/acrossChain/getAddress",

    //查询heco交易信息
    getHecoInfo:HECO_URL + "/tx/"
};
window.api = api;

export default getUrl();
