package org.conch.chain;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.common.Constants;
import org.conch.common.UrlManager;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.crypto.Crypto;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.mint.pool.SharderPoolProcessor;
import org.conch.peer.Peer;
import org.conch.tx.TransactionImpl;
import org.conch.tx.TransactionType;
import org.conch.util.*;

import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/11
 */
public class CheckSumValidator {

    private static final byte[] CHECKSUM_POC_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    110, -1, -56, -56, -58, 48, 43, 12, -41, -37, 90, -93, 80, 20, 3, -76, -84, -15, -113,
                    -34, 30, 32, 57, 85, -30, 16, -10, 127, -101, 17, 121, 124
            }
                    : new byte[] {
                    -90, -42, -57, -76, 88, -49, 127, 6, -47, -72, -39, -56, 51, 90, -90, -105,
                    121, 71, -94, -97, 49, -24, -12, 86, 7, -48, 90, -91, -24, -105, -17, -104
            };
    
    // not opened yet
    private static final byte[] CHECKSUM_PHASING_BLOCK =
            Constants.isTestnet()
                    ? new byte[] {
                    -1
            }
                    : new byte[] {
                   -1
            };

    private static final CheckSumValidator inst = new CheckSumValidator();

    public static CheckSumValidator getInst(){
        return inst;
    }

    /**
     * validate checksum after Event.BLOCK_SCANNED or Event.BLOCK_PUSHED
     * @return
     */
    public static Listener<Block> eventProcessor(){
        return inst.checksumListener;
    }
    
    // pop off to previous right height when checksum validation failed
    private final Listener<Block> checksumListener = block -> {
        if (block.getHeight() == Constants.POC_BLOCK_HEIGHT) {
            if (!verifyChecksum(CHECKSUM_POC_BLOCK, 0, Constants.POC_BLOCK_HEIGHT)) {
                Conch.getBlockchainProcessor().popOffTo(0);
            }
        } else if (block.getHeight() == Constants.PHASING_BLOCK_HEIGHT) {
            if (!verifyChecksum(CHECKSUM_PHASING_BLOCK, Constants.POC_BLOCK_HEIGHT, Constants.PHASING_BLOCK_HEIGHT)) {
                Conch.getBlockchainProcessor().popOffTo(Constants.POC_BLOCK_HEIGHT);
            }
        }
    };


    private boolean verifyChecksum(byte[] validChecksum, int fromHeight, int toHeight) {
        MessageDigest digest = Crypto.sha256();
        Connection con = null;
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM transaction WHERE height > ? AND height <= ? ORDER BY id ASC, timestamp ASC");
            pstmt.setInt(1, fromHeight);
            pstmt.setInt(2, toHeight);
            DbIterator<TransactionImpl> iterator = null;
            try {
                iterator = BlockchainImpl.getInstance().getTransactions(con, pstmt);
                while (iterator.hasNext()) {
                    digest.update(iterator.next().getBytes());
                }
            }finally {
                DbUtils.close(iterator);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
        
        byte[] checksum = digest.digest();
        if (validChecksum == null) {
            Logger.logMessage("Checksum calculated:\n" + Arrays.toString(checksum));
            return true;
        } else if (!Arrays.equals(checksum, validChecksum)) {
            Logger.logErrorMessage("Checksum failed at block " + Conch.getBlockchain().getHeight() + ": " + Arrays.toString(checksum));
            return false;
        } else {
            Logger.logMessage("Checksum passed at block " + Conch.getBlockchain().getHeight());
            return true;
        }
    }


    /** DEBUG **/
    static Set<Long> debugAccounts = Sets.newHashSet(
//            3960463107034192150L,
//            2792673654720227339L
//            963382008953913442L
    );
    public static boolean isDebugPoint(long accountId){
        return debugAccounts.contains(accountId);
    }
    /** DEBUG **/

    static int badCount = 0;
    static boolean synIgnoreBlock = false;
    // known ignore blocks
    private static Map<Long,JSONObject> ignoreBlockMap = loadDefaultKnownIgnoreBlocks();
    
    private static Set<Long> knownIgnoreTxs = Sets.newHashSet();
    private static Map<Integer,Set<Long>> knownDirtyPoolTxs = Maps.newConcurrentMap();
    private static Map<Integer,Set<Long>> knownDirtyPocTxs = Maps.newConcurrentMap();
    
    static Map<Integer, Map<Long, PocTxBody.PocNodeTypeV2>> pocNodeTypeTxsMap = Maps.newHashMap();
    
    public static final int CHECK_INTERVAL_IN_MINUTES = Conch.getIntProperty("sharder.knownBlockCheckInterval", 30);

    private static final Runnable updateKnownIgnoreBlocksThread = () -> {
        try {
            updateKnownIgnoreBlocks();
        }catch (Exception e) {
            Logger.logMessage("Error updateKnownIgnoreBlocksThread", e);
        }catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString());
            t.printStackTrace();
            System.exit(1);
        }
    };

    private static Map<Long,JSONObject> loadDefaultKnownIgnoreBlocks(){
        Map<Long,JSONObject> defaultMap = Maps.newConcurrentMap();

        JSONArray defaultIgnoreBlocks = JSON.parseArray("["
                +"{\"checksum\":\"9b6557deca53712337e330968fe7f98709ee0bdcd9ad1a60fbf0f50d7963650b4c4cfd4cf1a5a776c860b025055d4ef2f08aad609bb7fc908734bd1b79d8ef29\",\"height\":678,\"id\":-6134666443975256414,\"network\":\"testnet\"},"
                +"{\"checksum\":\"c2850ad0cf9bc1c6ab7b6d5db35b1d2f0e428b821c43e0e9f7678dc3f7d8280474c943bd5dab40afcf01f92b56c5466812455bfb38ec110898302b4f39e4c72f\",\"height\":799,\"id\":6339680013261852298,\"network\":\"testnet\"},"
                +"{\"checksum\":\"8aa9e20a3d162508fd8858ed9e3848c6ff0617c68ca88560cf4ca688122f710da13d28dded44938361f3fcdc00d58ac4d601b6abcafb2537732f0da73e6a0753\",\"height\":877,\"id\":6905135350706525967,\"network\":\"testnet\"},"
                +"{\"checksum\":\"b452cac8b47a1b8fae47161459ed7e7ca276504ab294766cc274d5858312390cf333f745c2d14f56827472a48502102c3fbf922ed0b1acbd915e347bfc81fb23\",\"height\":970,\"id\":4458139556799716747,\"network\":\"testnet\"},"
                +"{\"checksum\":\"e27b835b3d6038e9a3037cc0db54fa091f3c612d2edd332d89e91adef8fda502e47a02839a8a9f5433863c217cf669935bb2da3f045b26b37a11bbf994322148\",\"height\":998,\"id\":-4865758625654783967,\"network\":\"testnet\"},"
                +"{\"checksum\":\"b7e464b8654c944558838b147e34fcb25e15a402cdd346670cb43853b6ae6d0df49960e119095b578fb173a42ce75ae480a73fd61bedc0cf042a38a2bd1849ef\",\"height\":1063,\"id\":-3012267055101997835,\"network\":\"testnet\"},"
                +"{\"checksum\":\"bd6e73527f339a8f9aad85bf2b1f976165044ac529e121ee8ae192fb63b9010d98ad2275ecb659e3703c0d5b5e354ccbe8a5a33bc762c5eaa70e2472f976e527\",\"height\":1112,\"id\":-7149370974605872622,\"network\":\"testnet\"},"
                +"{\"checksum\":\"2827a6c278f06480bdeb6517f620bced2730733f773e8a8ba31e5ed99766ba02248cac0b193fc54ae2f9d91057327f90806a70cd418b263d46fd4a39ce269a9f\",\"height\":1115,\"id\":5887081329944171985,\"network\":\"testnet\"},"
                +"{\"checksum\":\"4eede1273aae0bedcb1d6936de84c8340521c5b20d70036e96b7c1341fe07705fb87a47ed3fd53563b5a457a7c0e6756e1b265b93467ec85c35b2a2f6b72eb94\",\"height\":1119,\"id\":-7896951537039525109,\"network\":\"testnet\"},"
                +"{\"checksum\":\"89d019f87ee3b90be9a15eb01313c4563bf793e4df0626cafd779dd335bd7200da9dd1c0281921dfa697c3eb2657d880c6bbda9b21987b4feecab8f17f1e3853\",\"height\":1155,\"id\":1148021195993014747,\"network\":\"testnet\"},"
                +"{\"checksum\":\"4a7c93851d7e914ff946bf56362d516b032913a6d647fad29d93e17bc117c70569069da6bf499248dd8030ebc6b1d74bae8e6a606088c88b6c5d369b11431852\",\"height\":1156,\"id\":-8753131244970894655,\"network\":\"testnet\"},"
                +"{\"checksum\":\"7fd89c33a7e4c38d258ef7c7beacece0997c8820a1947b2d261b6b68c52879061243064de23dae35ff2f86abb146f27027120ea215f0721a1c53088f67a5f712\",\"height\":1158,\"id\":696644979490831399,\"network\":\"testnet\"},"
                +"{\"checksum\":\"425ca7f71efc1b167489d68ddd03adcdbb5020e861b41ed636e2c2b5dae55700c70bc3cdbb4e55e01406fc87b2049959151c1639d737bcc62a4fb830f93e1f22\",\"height\":1159,\"id\":3864168661394784115,\"network\":\"testnet\"},"
                +"{\"checksum\":\"0957add71f02fea8d1e652a0d67a277ab147eaedb3f2355aaded802b87c28101e5921027f65bb0f9809ce53e66260d4f4a0a947b064124347f1bf4abcc8ce90e\",\"height\":1166,\"id\":-3573769334261041570,\"network\":\"testnet\"},"
                +"{\"checksum\":\"fe464ce3ddb8ee79ad5aa57439bdc99e1a2a86210da728f003c8883fc31d8e078b6c9ab45d09ae9d077f93be79ff63d6cc4176c7eb7aaf66f1a9e6947f134e4c\",\"height\":1185,\"id\":9129871367547934878,\"network\":\"testnet\"},"
                +"{\"checksum\":\"c36a3b6e2eebee3f9290c69ede923a4717e9a508deacde2ce2c48889ed2e950b4a517f9499fb0356b6492ac806f81f0a4521aaeecf54b17b1f64eef1ab3ea6c3\",\"height\":1201,\"id\":-7598243277999089715,\"network\":\"testnet\"},"
                +"{\"checksum\":\"3f5b8bea800980258cf7436edc0c791f43a47e772636d344ef88f406ea4013076f4e3112fddae7dd0d538b9763cf21245ffc50aa695d1b97edead27cd6d5fcd4\",\"height\":1214,\"id\":3581379360558433946,\"network\":\"testnet\"},"
                +"{\"checksum\":\"3e858f17456b6d766fecaa56820fb0857e83c629b77e7e8010f1c312c588c109440f2786dde698761dc1e6dbc66844901411a4057e816ba47fd8ca0f9b2faefb\",\"height\":1221,\"id\":-994067865277961691,\"network\":\"testnet\"},"
                +"{\"checksum\":\"face0faf3b54aa4bbe83db1072a2a3adc6db39efce1ff7869832b3362e9e610f148ba35923e797c73fe906cd4a72f7163c9f3cae75c44bc012e3fa2b63e32a5b\",\"height\":1226,\"id\":1817944799216451822,\"network\":\"testnet\"},"
                +"{\"checksum\":\"f0811e3656a73264d1a7cae7c8aaeb6443450dc67a1521ec7ecc3abf4e1caf0f7ec2c11129d2949f7a7134f56faff9182c643cf1a8b6969a6ba7ef82813125c4\",\"height\":1229,\"id\":-7868159284092505886,\"network\":\"testnet\"},"
                +"{\"checksum\":\"d1866610e5e1df0d6eefeebfcd38d47954631aae1b95ba5b265255c1a260b20f34881e8572981e00e6d66648c78cc596cafb38580575743788052444bbc57c65\",\"height\":1241,\"id\":-3638349952186553240,\"network\":\"testnet\"},"
                +"{\"checksum\":\"b88309aee0080239dd27de41d07762e81790eae1469aa23119cd41dbfee0a60e36649a94144d51c15c690a8e4e97d3591bf8b066ac5171237e0ef582281fe43e\",\"height\":1246,\"id\":-976907644395184550,\"network\":\"testnet\"},"
                +"{\"checksum\":\"da3878c64d2767949b74221dda2b81a4a5efa58cb0cd6b0f1d445c26009c780234eade1764969386cc9e29f031e28aaa2649715e10effc06db70c49ef9af5275\",\"height\":1247,\"id\":-8678979702008352375,\"network\":\"testnet\"},"
                +"{\"checksum\":\"7e800dfed94e37988828666e85a9b977099d88969454cb7e96496f29c2a0060877120a47ef3dda734ebb499ec65190fc7b3570ccf160dd5d6cab757779f94e57\",\"height\":1248,\"id\":3604378399882971790,\"network\":\"testnet\"},"
                +"{\"checksum\":\"918609d27c5d1670e099ddaec965beac9dc79d55d69109f77662ae154f280209bb9df96b037c987bbf172899c3ca0577118ac09590b1c17c5c1262009e997999\",\"height\":1256,\"id\":8641760579230450904,\"network\":\"testnet\"},"
                +"{\"checksum\":\"1309f08bdd03d373e68bb3ada3a3bde5f1e94f77f77ce9b8e90c6504cf20d200674b98e26cda81b6db72944602cc77d88a33cdda6e67185519cfbe9d08489dcf\",\"height\":1261,\"id\":7555373913379387387,\"network\":\"testnet\"},"
                +"{\"checksum\":\"103e72a628cb185cdff41156b44e7dc368f32e33a95d8b6bf0a437239e6dfa0ad50f0fd0da2b29979f24f37b9913ac7299ea216a04753ec48a80c62a0c826e9e\",\"height\":1273,\"id\":-1372155688900590877,\"network\":\"testnet\"},"
                +"{\"checksum\":\"ecc251009085a5d70418b711a943d966653cabe99fb03d526080aeede2cf9102514d2150d6caecdd319289cc46f13c1d55dc296dc262f3c0926723e38221e769\",\"height\":1274,\"id\":5403481402719957504,\"network\":\"testnet\"},"
                +"{\"checksum\":\"f329f1f872cfd2f94b91a943ebcf354fa2f804e875bfc73f11b7e25b8e59cf01e5b8ac67e4998193cd74e490cdd27d7d42e3cee100dc9073837c7127d23f5a84\",\"height\":1278,\"id\":5116093752876365444,\"network\":\"testnet\"},"
                +"{\"checksum\":\"0768683c01edb3dc5e425b72c2b53c27a01ca3173283081ba00a29e496c8ca04a78502e2b20e03b5947953bcb5f7d36167351ffbca405fe36085b7a82051d819\",\"height\":1282,\"id\":-5788607661834386108,\"network\":\"testnet\"},"
                +"{\"checksum\":\"ed58d4a0c0080bf544d0e04d5e89f266a1a4645d9f412c842908af2d2ec0c704b9fd93a377e6df67ffcf893391329e117c0487f6536f643ff57c1ee34ad504fe\",\"height\":1301,\"id\":-2992327726064179542,\"network\":\"testnet\"},"
                +"{\"checksum\":\"14d502ef9355b13bbcab33871717452fc083b4daf688f69a344bd8668fdf92059022e9c95cc10dc15a37fc6d293e65cdafa3d0a1894df44dcefb7b731179ca2b\",\"height\":1317,\"id\":-7875777179434378219,\"network\":\"testnet\"},"
                +"{\"checksum\":\"7c82df171be6b61d61e15841fcd9af4214c92fdfa5b3f84afc8356f92803c1058964e508e8f20adf7cb5277e6da0b0293d278c161301be9aa9da48c7ea9d844f\",\"height\":1320,\"id\":-8461370022448259167,\"network\":\"testnet\"},"
                +"{\"checksum\":\"c021a9fc8d59e47fe7134ee1931dce95ac563bb290fe50abed52dc2b4b821e0340f9a89c270734e70cc63ab549988d8ce68269746fbeb910ec3b360fa4413cd9\",\"height\":1321,\"id\":-2230739330748807031,\"network\":\"testnet\"},"
                +"{\"checksum\":\"9513981b71a8376f0cfbcbf7ac93ab4303a750c2a76377fa05e82c837bcd9409c41fcc070b4ae2f7dd3901469913e31773e32f5a68e69ec28e8fc1f6548ea8d8\",\"height\":1328,\"id\":7473504520841874191,\"network\":\"testnet\"},"
                +"{\"checksum\":\"cc6a7b5fbe20454c5118c63d0294d2953fc97cf705a8f6e45faf7992d7cac3032cc527b29ed207f2c578d87555fefb9a0538105964afa8a10f7cff392125cdbb\",\"height\":1343,\"id\":3641908066420742419,\"network\":\"testnet\"},"
                +"{\"checksum\":\"dba7fcd8ffd535fd950826f46665daabdfe3ed8bd85bc05cefdc985fc0fa970b7606cbffb77792f773ed22d1ae277d687aec71412bde36b829786a5087a672d8\",\"height\":1350,\"id\":8882002081341257697,\"network\":\"testnet\"},"
                +"{\"checksum\":\"3ce1424195e73f1a0018a15a93647c6bb47cf6d3a7bb9faf6551e81cbb6f610932ac7d5b652194046e0804d6cc3c7a44ddb6f2d2ee402f5d714374975b1dc266\",\"height\":1369,\"id\":6552110492655861457,\"network\":\"testnet\"},"
                +"{\"checksum\":\"1dbf7440a2bbf0ee9df66d62cb3e858909695334be51d2958a2c364e3ab49304d5819f0f604af28429809c4c56d1189877a4b2dcf74fae0df0cbe3b465132022\",\"height\":1390,\"id\":-3318284808878241769,\"network\":\"testnet\"},"
                +"{\"checksum\":\"b5da577c146a0be1138bfed1976be181d8d0cd0d4d2b98270e213010756e40026c627c2df2d3aed9931c5456a5b02059e518f7c3fa0224487b6b16c9d4568bbf\",\"height\":1396,\"id\":-1864144628520431314,\"network\":\"testnet\"},"
                +"{\"checksum\":\"c74a81193b553651e90ffa26ff34fff05876a98bc3bc71d10c3a29d3f70bc70b9974cf77a7ddf50154b63e752731bccba360253c1991208a53c8849d82787db4\",\"height\":1405,\"id\":6272874939080731257,\"network\":\"testnet\"},"
                +"{\"checksum\":\"19957dafa66e4b05505ccda03065005f79b094147acb6503abaf3acf3e53950f033efd257430a627dd120c8829ccc361219aeca0a6cf769994633a3a5ccf43df\",\"height\":5130,\"id\":3627531186248768236,\"network\":\"testnet\"},"
                +"{\"checksum\":\"7d7eeb8549239f717802ba0af1a5af35b61b622ea612702dc67602ef04d9a50ebbe71db149f90fbe01668b30777f9d25afa4fb3db139b42aba66276be788475b\",\"height\":5316,\"id\":6735474350291239541,\"network\":\"testnet\"},"
                +"{\"checksum\":\"3a0dbfcef1d5c5b50e979e54038e1291fae84ed95f9504635cccef9c1c6f7e0633b6884ad145ba31c7ed0ebf3f1ed74997f806f2abc3318243d7da1a58984204\",\"height\":5487,\"id\":986005735749606277,\"network\":\"testnet\"},"
                +"{\"checksum\":\"7f056e9b7b529a7350cb7a41124fe870c1beb6ff87d216fcdb35a9129affed0158a3d75c419c0bafb4a0447cb299051f14a885d35cebf763e16cde2e6a4117d4\",\"height\":5549,\"id\":-6491792554689411068,\"network\":\"testnet\"},"
                +"{\"checksum\":\"62dd9a649f76ecbe5f7bfac3e3f0237768cbbf3bd87750939dddf3a530f49b06f362cf7040cbda3cfc2b13953e9de2a26a515d255917fa30814d540993c30df8\",\"height\":15483,\"id\":1476485356832204,\"network\":\"testnet\"},"
                + "]"
        );

        if(defaultIgnoreBlocks.size() <= 0) return defaultMap;

        for(int i = 0; i < defaultIgnoreBlocks.size(); i++) {
            JSONObject ignoreBlock = defaultIgnoreBlocks.getJSONObject(i);
            defaultMap.put(ignoreBlock.getLong("id"), ignoreBlock);
        }
        return defaultMap;
    }

    static {
        ThreadPool.scheduleThread("UpdateKnownIgnoreBlocksThread", updateKnownIgnoreBlocksThread, CHECK_INTERVAL_IN_MINUTES, TimeUnit.MINUTES);
    }

    public static boolean isKnownIgnoreBlock(long blockId, byte[] blockSignature){
        if(!ignoreBlockMap.containsKey(blockId)) return false;

        // checksum compare
        JSONObject ignoreBlock = ignoreBlockMap.get(blockId);
        return  StringUtils.equals(Convert.toHexString(blockSignature), ignoreBlock.getString("checksum"));
    }

    public static boolean isDoubleSpendingIgnoreTx(TransactionImpl tx){
        if(tx.getType() != null 
        && TransactionType.TYPE_SHARDER_POOL == tx.getType().getType()){
            return true;
        }
        return false;
    }
    
    public static boolean isKnownIgnoreTx(long txId){
        boolean result = knownIgnoreTxs.contains(txId);
        
        return result;
    }

    public static boolean isDirtyPoolTx(int height, long accountId){
        if(!knownDirtyPoolTxs.containsKey(height)) {
            return false;
        }

        boolean result = knownDirtyPoolTxs.get(height).contains(accountId);
        
        if(result){
            Logger.logDebugMessage("found a known dirty pool tx[account id=%s] at height %d, ignore this tx" , accountId, height);
        }
        return result;
    }
    

    /**
     * 
     * [POLYFILL]
     * @param accountId
     * @param height
     * @return
     */
    public static PocTxBody.PocNodeTypeV2 isPreAccountsInTestnet(long accountId, int height){
        if(Constants.isTestnet()
            && pocNodeTypeTxsMap.containsKey(height)) {
            return pocNodeTypeTxsMap.get(height).get(accountId);
        }
        return null;
    }

    public static PocTxBody.PocNodeTypeV2 isPreAccountsInTestnet(String host, int height){
        NavigableSet<Integer> heightSet = Sets.newTreeSet(pocNodeTypeTxsMap.keySet()).descendingSet();
        for(Integer historyHeight : heightSet) {
            if(historyHeight <= height) {
                Map<Long, PocTxBody.PocNodeTypeV2> peerMap = pocNodeTypeTxsMap.get(historyHeight);
                Collection<PocTxBody.PocNodeTypeV2> nodeTypeTxs = peerMap.values();
                for(PocTxBody.PocNodeTypeV2 nodeTypeTx : nodeTypeTxs){
                    if(StringUtils.equals(host,nodeTypeTx.getIp())){
                        return nodeTypeTx;
                    }  
                }
            }
        }
        return null;
    }
    
    
    static private boolean closeIgnore = false;
    
    private static boolean updateSingle(JSONObject object){
        try{
            if(closeIgnore) return true;
            
            try{
                if(object.containsKey("id") && object.getLong("id") != -1L) {
                    long blockId = object.getLong("id");
                    if (!ignoreBlockMap.containsKey(blockId)) {
                        ignoreBlockMap.put(blockId, object);
                    }
                }
            } catch(Exception e){
                Logger.logErrorMessage("parsed known ignore blocks error caused by " + e.getMessage());
            }

            try{
                if(object.containsKey("txs")){
                    com.alibaba.fastjson.JSONArray array = object.getJSONArray("txs");
                    synchronized (knownIgnoreTxs){
                        for(int i = 0; i < array.size(); i++) {
                            Long txid = array.getLong(i);
                            if(!knownIgnoreTxs.contains(txid)) {
                                knownIgnoreTxs.add(txid);
                            }
                        }
                    }
                }
            } catch(Exception e){
                Logger.logErrorMessage("parsed known ignore txs error caused by " + e.getMessage());
            }
            

            try{
                if(object.containsKey("dirtyPoolAccounts")){
                    Integer height = object.getInteger("height");
                    synchronized (knownDirtyPoolTxs){
                        if(!knownDirtyPoolTxs.containsKey(height)) {
                            knownDirtyPoolTxs.put(height, Sets.newHashSet());
                        }

                        Set<Long> dirtyPoolAccounts = knownDirtyPoolTxs.get(height);

                        com.alibaba.fastjson.JSONArray array = object.getJSONArray("dirtyPoolAccounts");
                        for(int i = 0; i < array.size(); i++) {
                            dirtyPoolAccounts.add(array.getLong(i));
                        }
                    }
                }
            } catch(Exception e){
                Logger.logErrorMessage("parsed known dirty pool accounts error caused by " + e.getMessage());
            }
            
            
            try{
                if(object.containsKey("dirtyPocTxs")){
                    Integer height = object.getInteger("height");
                    synchronized (knownDirtyPocTxs) {
                        if(!knownDirtyPocTxs.containsKey(height)) {
                            knownDirtyPocTxs.put(height, Sets.newHashSet());
                        }
                        Set<Long> dirtyPocTxs = knownDirtyPocTxs.get(height);

                        com.alibaba.fastjson.JSONArray array = object.getJSONArray("dirtyPocTxs");
                        for(int i = 0; i < array.size(); i++) {
                            dirtyPocTxs.add(array.getLong(i));
                        }
                    }
                }
            } catch(Exception e){
                Logger.logErrorMessage("parsed known dirty poc txs error caused by " + e.getMessage());
            }
            
            
            try{
                if(object.containsKey("pocNodeTypeTxsV1")){
                    MultiValueMap pocNodeTypeTxsV1Map = JSONObject.parseObject(object.getString("pocNodeTypeTxsV1"), MultiValueMap.class);
                    Set<Integer> heightSet = pocNodeTypeTxsV1Map.keySet();

                    synchronized (pocNodeTypeTxsMap) {
                        for(Integer height : heightSet){
                            Collection collection = pocNodeTypeTxsV1Map.getCollection(height);

                            if(!pocNodeTypeTxsMap.containsKey(height)) {
                                pocNodeTypeTxsMap.put(height, Maps.newHashMap());
                            }
                            Map<Long, PocTxBody.PocNodeTypeV2> pocNodeTypeV2Map = pocNodeTypeTxsMap.get(height);

                            for (Iterator it = collection.iterator(); it.hasNext(); ) {
                                Object attachment = it.next();
                                if(attachment instanceof com.alibaba.fastjson.JSONArray) {
                                    JSONArray jsonArray = (JSONArray) attachment;

                                    for(int i = 0; i < jsonArray.size(); i++) {
                                        JSONObject jsonObject = null;
                                        try{
                                            jsonObject = jsonArray.getJSONObject(i);
                                            String ip = jsonObject.getString("ip");
                                            String accountRs = jsonObject.getString("accountRs");
                                            int type = jsonObject.getIntValue("type");
                                            Byte version = jsonObject.getByte("version");
                                            Long accountId = Account.rsAccountToId(accountRs);
                                            //String ip, Peer.Type type, long accountId
                                            PocTxBody.PocNodeTypeV2 pocNodeTypeV2 = new PocTxBody.PocNodeTypeV2(ip, Peer.Type.getByCode(type), accountId);
                                            pocNodeTypeV2Map.put(accountId, pocNodeTypeV2);
                                        } catch(Exception e){
                                            Logger.logErrorMessage("Poc node type tx convert failed caused by[%s] and detail is %s" + e.getMessage(), jsonObject == null ? "null" : jsonObject.toString() );
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }catch(Exception e){
                Logger.logErrorMessage("parsed known poc node type v1 txs error caused by " + e.getMessage());
            }
           
            
        }catch(Exception e){
            Logger.logErrorMessage("parsed and set single ignore block error caused by " + e.getMessage());
            return false;
        }
        return true;
    }


    public static void updateKnownIgnoreBlocks(){
        RestfulHttpClient.HttpResponse response = null;
        String url = UrlManager.KNOWN_IGNORE_BLOCKS;
        try {
            response = RestfulHttpClient.getClient(url).get().request();
            if(response == null) return;
            
            String content = response.getContent();
            String totalIgnoreBlocks = "\n\r";
            if(content.startsWith("[")) {
                com.alibaba.fastjson.JSONArray array = JSON.parseArray(content);
                for(int i = 0; i < array.size(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    totalIgnoreBlocks += object.toString() + "\n\r";
                    updateSingle(object);
                }
            }else if(content.startsWith("{")){
                com.alibaba.fastjson.JSONObject object = JSON.parseObject(content);
                totalIgnoreBlocks += object.toString() + "\n\r";
                updateSingle(object);
            }else{
                Logger.logWarningMessage("not correct known ignore block get from " + url + " : " + content);
                return ;
            }
//            if(totalIgnoreBlocks.length() > 4){
//                Logger.logDebugMessage("total ignore blocks get from %s as follow:" + totalIgnoreBlocks, url);
//            }
  
            // remove the dirty poc txs
            if(knownDirtyPocTxs.size() > 0) {
                Set<Long> dirtyPocTxs = Sets.newHashSet();
                knownDirtyPocTxs.values().forEach(ids -> dirtyPocTxs.addAll(ids));
                Conch.getPocProcessor().removeDelayedPocTxs(dirtyPocTxs);
            }

            // remove the dirty pools
            if(knownDirtyPoolTxs.size() > 0) {
                SharderPoolProcessor.removePools(knownDirtyPoolTxs);
            }
            
            if(!synIgnoreBlock) synIgnoreBlock = true;
        } catch (IOException e) {
           Logger.logErrorMessage("Can't get known ignore blocks from " + url + " caused by " + e.getMessage());
        }
    }
    
    public static JSONObject generateIgnoreBlock(long id, byte[] checksum, String network){
        if(StringUtils.isEmpty(network)) network = "testnet";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id",id);
        jsonObject.put("checksum",Convert.toString(checksum, false));
        jsonObject.put("network",network);
        return jsonObject;
    }

    public static void main(String[] args) {
        //updateKnownIgnoreBlocks();
        Map<Long,JSONObject> map = loadDefaultKnownIgnoreBlocks();
    }

}
