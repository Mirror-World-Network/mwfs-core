package org.conch.consensus.poc;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.chain.*;
import org.conch.common.Constants;
import org.conch.consensus.genesis.GenesisRecipient;
import org.conch.consensus.genesis.SharderGenesis;
import org.conch.consensus.poc.db.PocDb;
import org.conch.consensus.poc.tx.PocTxBody;
import org.conch.consensus.poc.tx.PocTxWrapper;
import org.conch.consensus.reward.RewardCalculator;
import org.conch.db.Db;
import org.conch.db.DbIterator;
import org.conch.db.DbUtils;
import org.conch.mint.pool.PoolRule;
import org.conch.peer.CertifiedPeer;
import org.conch.peer.Peer;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.tx.TransactionImpl;
import org.conch.tx.TransactionType;
import org.conch.util.DiskStorageUtil;
import org.conch.util.LocalDebugTool;
import org.conch.util.Logger;
import org.conch.util.ThreadPool;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2018/11/27
 */
public class PocProcessorImpl implements PocProcessor {

    /**
     * !! Don't use this instance directly,
     * please call org.conch.Conch#getPocProcessor() to get instance
     **/

    public static PocProcessorImpl instance = getOrCreate();
    // execute once when restart the cos application
    private static boolean oldPocTxsProcess = false;

//    private static final int peerSynThreadInterval = 600;
    private static final int pocTxSynThreadInterval = 60;

    private static final String LOCAL_STORAGE_POC_CALCULATOR = "StoredPocCalculator";

    // height : { accountId : account }
    private static Map<Integer, Map<Long, Account>> balanceChangedMap = Maps.newConcurrentMap();


    private PocProcessorImpl() {
    }

    private static synchronized PocProcessorImpl getOrCreate() {
        return instance != null ? instance : new PocProcessorImpl();
    }

    /**
     * process the balance map which recorded in the payment or coinbase
     * the map recorded in the method: org.conch.consensus.poc.PocProcessorImpl#putInBalanceChangedAccount
     * the potential logic is: received Account.Event.BALANCE firstly, then received Event.AFTER_BLOCK_ACCEPT
     * @param height
     */
    private static synchronized void balanceChangeMapProcessing(int height){
        boolean someAccountBalanceChanged = balanceChangedMap.containsKey(height) && balanceChangedMap.get(height).size() > 0;
        if (someAccountBalanceChanged) {
            for (Account account : balanceChangedMap.get(height).values()) {
                balanceChangedProcess(height, account);
            }

            balanceChangedMap.get(height).clear();
            balanceChangedMap.remove(height);
        }
    }

    static {
        // new block accepted
        Conch.getBlockchainProcessor().addListener((Block block) -> {
            // balance hold score re-calculate
            balanceChangeMapProcessing(block.getHeight());
        }, BlockchainProcessor.Event.AFTER_BLOCK_ACCEPT);

        // balance changed event
        Account.addListener((Account account) -> {
            putInBalanceChangedAccount(Conch.getBlockchain().getHeight(), account, Account.Event.BALANCE);
        }, Account.Event.BALANCE);

        // unconfirmed balance changed event
        Account.addListener((Account account) -> {
            putInBalanceChangedAccount(Conch.getBlockchain().getHeight(), account, Account.Event.UNCONFIRMED_BALANCE);
        }, Account.Event.UNCONFIRMED_BALANCE);

        // poc changed event
        Account.addListener((Account account) -> {
            putInBalanceChangedAccount(Conch.getBlockchain().getHeight(), account, Account.Event.POC);
        }, Account.Event.POC);

        instance.loadFromDisk();
    }

    /**
     * put the account balance change event into map
     * @param height
     * @param account
     * @param event
     */
    static void putInBalanceChangedAccount(int height, Account account, Account.Event event) {
        if (account == null || account.getId() == -1 || event == null) return;
        long accountId = account.getId();

        // check current height when event is BALANCE changed
        if (Account.Event.BALANCE == event) {
            if (!balanceChangedMap.containsKey(height)) {
                balanceChangedMap.put(height, Maps.newHashMap());
            }

            if (!balanceChangedMap.get(height).containsKey(accountId)) {
                balanceChangedMap.get(height).put(accountId, account);
            }
        }

        // check future confirmed height
        int confirmedHeight = height + Constants.GUARANTEED_BALANCE_CONFIRMATIONS;
        if (!balanceChangedMap.containsKey(confirmedHeight)) {
            balanceChangedMap.put(confirmedHeight, Maps.newHashMap());
        }

        if (!balanceChangedMap.get(confirmedHeight).containsKey(accountId)) {
            balanceChangedMap.get(confirmedHeight).put(accountId, account);
        }

        // check current height when event is POC
        if (Account.Event.POC == event) {
            if (!balanceChangedMap.containsKey(height)) {
                balanceChangedMap.put(height, Maps.newHashMap());
            }

            if (!balanceChangedMap.get(height).containsKey(accountId)) {
                balanceChangedMap.get(height).put(accountId, account);
            }
        }
    }

    /**
     * PoC tx process
     *
     * @param tx poc tx
     * @return
     */
    @Override
    public boolean pocTxProcess(Transaction tx) {
        if (TransactionType.TYPE_POC != tx.getType().getType()) {
            return true;
        }

        boolean success = false;
        if (PocTxWrapper.SUBTYPE_POC_WEIGHT_TABLE == tx.getType().getSubtype()) {
            PocTxBody.PocWeightTable weightTable = (PocTxBody.PocWeightTable) tx.getAttachment();
            PocCalculator.inst.setCurWeightTable(weightTable, tx.getHeight());
            success = true;
        } else {
            if (PocTxWrapper.SUBTYPE_POC_NODE_TYPE == tx.getType().getSubtype()) {
                success = nodeTypeTxProcess(tx.getHeight(), tx);
            } else if (PocTxWrapper.SUBTYPE_POC_NODE_CONF == tx.getType().getSubtype()) {
                success = nodeConfTxProcess(tx.getHeight(), (PocTxBody.PocNodeConf) tx.getAttachment());
            } else if (PocTxWrapper.SUBTYPE_POC_ONLINE_RATE == tx.getType().getSubtype()) {
                success = onlineRateTxProcess(tx.getHeight(), (PocTxBody.PocOnlineRate) tx.getAttachment());
            } else if (PocTxWrapper.SUBTYPE_POC_BLOCK_MISSING == tx.getType().getSubtype()) {
                success = blockMissingTxProcess(tx.getHeight(), (PocTxBody.PocGenerationMissing) tx.getAttachment());
            }
        }
        
        if (success) {
            PocHolder.updateHeight(tx.getHeight());
        }else{
            // process later
            PocHolder.addDelayPocTx(tx);
        }
        return success;
    }

    @Override
    public PocScore calPocScore(Account account, int height) {
        PocScore pocScore = PocHolder.getPocScore(height, account.getId());
        return pocScore;
    }

    @Override
    public PocTxBody.PocWeightTable getPocWeightTable(Long version) {
        return PocHolder.getPocWeightTable();
    }

    @Override
    public void notifySynTxNow() {
        oldPocTxsProcess = true;
    }

    /**
     * account whether bound to certified peer
     *
     * @param accountId
     * @return
     */
    @Override
    public boolean isCertifiedPeerBind(long accountId, int height) {
        boolean hubBindAccount = PocHolder.isBoundPeer(Peer.Type.SOUL, accountId);
        boolean communityBindAccount = PocHolder.isBoundPeer(Peer.Type.COMMUNITY, accountId);
        boolean foundationBindAccount = PocHolder.isBoundPeer(Peer.Type.FOUNDATION, accountId);
        boolean isGenesisAccount = SharderGenesis.isGenesisCreator(accountId) || SharderGenesis.isGenesisRecipients(accountId);

        // height for certified peers
        return hubBindAccount || communityBindAccount || foundationBindAccount || isGenesisAccount;
    }

    /**
     * check whether a account bounded to a certified peer
     *
     * @param accountId
     * @return
     */
    @Override
    public CertifiedPeer getBoundedPeer(long accountId, int height) {
        return PocHolder.getBoundPeer(accountId, height);
    }

    @Override
    public boolean resetCertifiedPeers() {
        return PocHolder.resetCertifiedPeers();
    }

    @Override
    public boolean pocTxsProcessed(int height) {
        // poc isn't processed: whether contains delayed poc txs or old poc txs need to process
        return !oldPocTxsProcess && PocHolder.countDelayPocTxs(height) <= 0;
    }

    @Override
    public boolean processDelayedPocTxs(int height) {

        if (!Conch.reachLastKnownBlock()) return false;

        // delayed poc txs 
        List<Long> delayedPocTxs = PocHolder.delayPocTxs(height);
        Logger.logDebugMessage("process delayed poc txs[size=%d]", delayedPocTxs.size());
        Set<Long> processedTxs = Sets.newHashSet();
        delayedPocTxs.forEach(txid -> {
            if (instance.pocTxProcess(txid)) {
                processedTxs.add(txid);
            }
        });

        // remove processed txs
        if (processedTxs.size() > 0) {
            Logger.logInfoMessage("success to process delayed poc txs[processed size=%d, wish size=%d]", processedTxs.size(), delayedPocTxs.size());
            Logger.logDebugMessage("processed poc txs detail => " + Arrays.toString(processedTxs.toArray()));
            PocHolder.removeProcessedTxs(processedTxs);
        } else if (processedTxs.size() <= 0 && delayedPocTxs.size() > 0) {
            Logger.logWarningMessage("[WARN] delayed poc txs process failed, wish to process %d poc txs %s", delayedPocTxs.size(), Arrays.toString(delayedPocTxs.toArray()));
        }
        return PocHolder.countDelayPocTxs(height) <= 0;
    }

    @Override
    public boolean removeDelayedPocTxs(Set<Long> txIds) {
        PocHolder.removeProcessedTxs(txIds);
        return true;
    }

    @Override
    public Map<Long, CertifiedPeer> getCertifiedPeers(){
        return PocHolder.inst.certifiedPeers;
    }

    @Override
    public Map<Integer, Map<Long,CertifiedPeer>> getHistoryCertifiedPeers() {
        return PocHolder.inst.historyCertifiedPeers;
    }

    @Override
    public boolean rollbackTo(int height) {
        try {
            int currentHeight = Conch.getHeight();
            // rollback the poc score map
            synchronized (PocHolder.inst.historyScore) {
                for(int i = height + 1; i <= currentHeight ; i++){
                    if(PocHolder.inst.historyScore.containsKey(i)){
                        PocHolder.inst.historyScore.remove(i);
                    }
                }
                //rollback the db
                PocDb.rollback(height);
            }

            // reset the score map
            synchronized (PocHolder.inst.scoreMap) {
                PocHolder.inst.scoreMap.clear();
                PocHolder.inst.scoreMap = PocDb.listAll();
            }

            // rollback the history certified peers
            synchronized (PocHolder.inst.historyCertifiedPeers) {
                for (int i = height + 1; i <= currentHeight; i++) {
                    // rollback the poc score map
                    if (PocHolder.inst.historyCertifiedPeers.containsKey(i)) {
                        PocHolder.inst.historyCertifiedPeers.remove(i);
                    }
                }
            }

            // reset the certified peers
            // TODO


            // set the latest certified peer
            PocHolder.inst.updateHeight(height);

        } finally {

        }
        return true;
    }

    /**
     * load the poc holder backup from local disk
     */
    private void loadFromDisk() {
        Logger.logInfoMessage("load exist poc calculator instance from local disk[" + DiskStorageUtil.getLocalStoragePath(LOCAL_STORAGE_POC_CALCULATOR) + "]");
        Object calcObj = DiskStorageUtil.getObjFromFile(LOCAL_STORAGE_POC_CALCULATOR);
        if (calcObj != null) {
            PocCalculator.inst = (PocCalculator) calcObj;
        }

        //load and process the poc txs from history blocks
        if (PocHolder.inst != null && PocHolder.inst.lastHeight <= Conch.getBlockchain().getHeight()) {
            oldPocTxsProcess = true;
        }
    }

    /**
     * update the recipient id of the old  poc txs
     */
    public static void updateRecipientIdIntoOldPocTxs() {
        if(Conch.getHeight() > Constants.POC_TX_ALLOW_RECIPIENT) {
            return;
        }
        Logger.logInfoMessage("[PocTxCorrect] update the recipient id of the old  poc txs");
        DbIterator<? extends Transaction> iterator = null;
        Connection updateConnection = null;
        try {
            iterator = Conch.getBlockchain().getTransactions(GenesisRecipient.POC_TX_CREATOR_ID, TransactionType.TYPE_POC, true, 0, Integer.MAX_VALUE);
            updateConnection = Db.db.getConnection();

            int i = 0;
            while (iterator.hasNext()) {
                Transaction transaction = iterator.next();
                Attachment attachment = transaction.getAttachment();
                if(PocTxWrapper.SUBTYPE_POC_NODE_TYPE == attachment.getTransactionType().getSubtype()
                        && (transaction.getRecipientId() == -1 || transaction.getRecipientId() == 0)) {
                    long accountIdOfAttachment = -1L;
                    if(attachment instanceof PocTxBody.PocNodeTypeV3){
                        accountIdOfAttachment = ((PocTxBody.PocNodeTypeV3) attachment).getAccountId();
                    }else if(attachment instanceof PocTxBody.PocNodeTypeV2){
                        accountIdOfAttachment = ((PocTxBody.PocNodeTypeV2) attachment).getAccountId();
                    }

                    try (PreparedStatement pstmt = updateConnection.prepareStatement("UPDATE transaction SET recipient_id = ? WHERE id = ?")) {
                        pstmt.setLong(1, accountIdOfAttachment);
                        pstmt.setLong(2, transaction.getId());
                        pstmt.executeUpdate();
                        i++;
                    }
                }
            }
            Logger.logInfoMessage("[PocTxCorrect] update finished. update count is " + i);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DbUtils.close(iterator);
            DbUtils.close(updateConnection);
        }
    }

    private static boolean pocDbBeReset = false;
    /**
     * - reset the poc table to avoid the poc score wrong
     * - close this processing after
     */
    public static void checkAndResetPocDb() {
        BlockImpl lastBlock = BlockDb.findLastBlock();
        if(lastBlock != null
        && lastBlock.getHeight() > RewardCalculator.MINER_JOINING_PHASE) {
            return;
        }

        try {
            Logger.logInfoMessage("[ResetPocDb] reset the poc db");
            if (!Db.db.isInTransaction()) {
                Db.db.beginTransaction();
            }
            int count = PocDb.rollback(0);

            Db.db.clearCache();
            Db.db.commitTransaction();
            pocDbBeReset = true;
            Logger.logInfoMessage("[ResetPocDb] reset the poc db finished, reset count is " + count);
        } catch (RuntimeException e) {
            Logger.logErrorMessage("Error reset the poc db, " + e.toString());
            Db.db.rollbackTransaction();
            throw e;
        }
    }

    public static void init() {
        checkAndResetPocDb();
        ThreadPool.scheduleThread("OldPocTxsProcessThread", oldPocTxsProcessThread, 1, TimeUnit.MINUTES);
        ThreadPool.scheduleThread("DelayedPocTxsProcessThread", delayedPocTxsProcessThread, pocTxSynThreadInterval, TimeUnit.SECONDS);
        //updateRecipientIdIntoOldPocTxs();
    }

    private static final Runnable oldPocTxsProcessThread = () -> {
        try {

            if (!oldPocTxsProcess) {
                Logger.logDebugMessage("[OldPocTxs] all old poc txs be processed yet, sleep for the next round check...");
                return;
            }
            
            // old poc txs process: a) miss the poc tx, b) restart the cos client
            if (oldPocTxsProcess) {
                // total poc txs from last height
                int fromHeight = 0;
                if(PocHolder.inst.lastHeight > 0) {
                    fromHeight = PocHolder.inst.lastHeight;
                }
                int toHeight = Conch.getHeight();

                BlockchainImpl.getInstance().writeLock();
                DbIterator<BlockImpl> blocks = null;
                try {
                    if(pocDbBeReset){
                        Logger.logInfoMessage("[OldPocTxs] process old poc txs from %d to %d when poc db be reset...", 0, toHeight);
                        blocks = BlockchainImpl.getInstance().getAllBlocks();
                    }else{
                        Logger.logInfoMessage("[OldPocTxs] process old poc txs from %d to %d ...", fromHeight, toHeight);
                        blocks = BlockchainImpl.getInstance().getBlocks(fromHeight, toHeight);
                    }
                    int count = 0;
                    for (BlockImpl block : blocks) {
                        count += instance.pocSeriesTxProcess(block);
                    }
                    Logger.logInfoMessage("[OldPocTxs] old poc txs processed[from %d to %d] [processed size=%d]", fromHeight, toHeight, count);
                    oldPocTxsProcess = false;
                } finally {
                    DbUtils.close(blocks);
                    BlockchainImpl.getInstance().writeUnlock();
                }
            }

        } catch (Exception e) {
            Logger.logErrorMessage("[OldPocTxs] old poc txs processing thread interrupted", e);
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    };

    private static final Runnable delayedPocTxsProcessThread = () -> {
        try {
            int currentHeight = Conch.getBlockchain().getHeight();
            if (instance.processDelayedPocTxs(currentHeight)) {
                Logger.logDebugMessage("no needs to syn and process poc serial txs now, sleep %d seconds...", pocTxSynThreadInterval);
                return;
            }

            if (!Conch.reachLastKnownBlock()) {
                return;
            }

            try {
                instance.processDelayedPocTxs(currentHeight);
            } catch (Exception e) {
                Logger.logErrorMessage("Process delayed poc txs failed caused by [%s]", e.getMessage());
            }

        } catch (Exception e) {
            Logger.logErrorMessage("delayed poc txs process thread interrupted", e);
        } catch (Throwable t) {
            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
            System.exit(1);
        }
    };

//    private static final Runnable peerSynThread = () -> {
//        try {
//
//            if (PocHolder.synPeers().size() <= 0) {
//                Logger.logInfoMessage("no needs to syn peer, sleep %d seconds...", peerSynThreadInterval);
//            }
//
//            Set<String> connectedPeers = Sets.newHashSet();
//            for (String peerAddress : PocHolder.synPeers()) {
//                try {
//                    Peer peer = Peers.findOrCreatePeer(peerAddress, Peers.isUseNATService(peerAddress), true);
//                    if (peer != null) {
//                        Peers.addPeer(peer, peerAddress);
//                        Peers.connectPeer(peer);
//                    }
//                    peer = Peers.getPeer(peerAddress, true);
////          _updateCertifiedNodes(peer.getHost(), peer.getType(), -1);
//                    connectedPeers.add(peer.getHost());
//                } catch (Exception e) {
//                    if (Logger.printNow(PocProcessorImpl.class.getName(), 200)) {
//                        Logger.logDebugMessage("can't connect peer[%s] in peerSynThread, caused by %s", peerAddress, e.getMessage());
//                    }
//                    continue;
//                }
//            }
//
//            if (connectedPeers.size() > 0) {
//                PocHolder.removeConnectedPeers(connectedPeers);
////                DiskStorageUtil.saveObjToFile(PocHolder.inst, LOCAL_STORAGE_POC_HOLDER);
//            }
//
//        } catch (Exception e) {
//            Logger.logErrorMessage("peer syn thread interrupted caused by %s", e.getMessage());
//        } catch (Throwable t) {
//            Logger.logErrorMessage("CRITICAL ERROR. PLEASE REPORT TO THE DEVELOPERS.\n" + t.toString(), t);
//            System.exit(1);
//        }
//    };

    /**

     */
    /**
     * process poc txs of block
     *
     * @param block block
     * @return processed count
     */
    private int pocSeriesTxProcess(Block block) {
        int count = 0;
        //@link: org.conch.chain.BlockchainProcessorImpl.autoExtensionAppend update the ext tag
        List<? extends Transaction> txs = block.getTransactions();
//        Boolean containPoc = block.getExtValue(BlockImpl.ExtensionEnum.CONTAIN_POC);
//        if (txs == null || txs.size() <= 0 || containPoc == null || !containPoc) {
//            return count;
//        }

        //just process poc tx
        for (Transaction tx : txs) {
            if (TransactionType.TYPE_POC  == tx.getType().getType()) {
                if(pocTxProcess(tx)) count++;
            } else if(TransactionType.TYPE_PAYMENT  == tx.getType().getType()){
                // payment tx processing
                Account recipientAccount = Account.getAccount(tx.getRecipientId());
                Account senderAccount = Account.getAccount(tx.getSenderId());
//                balanceChangedProcess(block.getHeight(), senderAccount);
//                balanceChangedProcess(block.getHeight(), recipientAccount);
                putInBalanceChangedAccount(block.getHeight(), senderAccount, Account.Event.BALANCE);
                putInBalanceChangedAccount(block.getHeight(), recipientAccount, Account.Event.BALANCE);
                count++;
            } else if(TransactionType.TYPE_COIN_BASE  == tx.getType().getType()){
                // coinbase tx processing
                Attachment.CoinBase coinBase = (Attachment.CoinBase) tx.getAttachment();
                Account senderAccount = Account.getAccount(tx.getSenderId());
                Map<Long, Long> consignors = coinBase.getConsignors();

                if (consignors.size() == 0) {
//                    balanceChangedProcess(block.getHeight(), senderAccount);
                    putInBalanceChangedAccount(block.getHeight(), senderAccount, Account.Event.POC);
                } else {
                    Map<Long, Long> rewardList = PoolRule.calRewardMapAccordingToRules(senderAccount.getId(), coinBase.getGeneratorId(), tx.getAmountNQT(), consignors);
                    for (long id : rewardList.keySet()) {
                        Account account = Account.getAccount(id);
//                        balanceChangedProcess(block.getHeight(), account);
                            putInBalanceChangedAccount(block.getHeight(), senderAccount, Account.Event.POC);
                    }
                }
                count++;
            }
        }

        balanceChangeMapProcessing(block.getHeight());

        return count;
    }

    /**
     * process poc txs according to specified height area
     * @param fromHeight
     * @param toHeight
     * @return
     */
    private int pocSeriesTxProcess(int fromHeight, int toHeight) {
        Connection con = null;
        List<Transaction> txList = Lists.newArrayList();
        try {
            con = Db.db.getConnection();
            PreparedStatement pstmt = con.prepareStatement("SELECT * FROM TRANSACTION t WHERE t.TYPE=12 AND HEIGHT >= ? AND HEIGHT <= ? ORDER BY HEIGHT ASC");
            try{
                pstmt.setInt(1,fromHeight);
                pstmt.setInt(2,toHeight);
                DbIterator<TransactionImpl> transactions =  BlockchainImpl.getInstance().getTransactions(con,pstmt);
                while (transactions.hasNext()) {
                    txList.add(transactions.next());
                }
            }finally {
                DbUtils.close(con);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }finally {
            DbUtils.close(con);
        }
        
        int count = 0;
        for (Transaction tx : txList) {
            if (pocTxProcess(tx)) count++;
        }

        return count;
    }

    /**
     * PoC tx process
     *
     * @param txid poc tx id
     * @return
     */
    private boolean pocTxProcess(Long txid) {
        Transaction tx = Conch.getBlockchain().getTransaction(txid);
        if (tx == null) return false;
        return pocTxProcess(tx);
    }

    /**
     * process the node type tx of poc series
     *
     * @param height      block height that included this tx
     * @param tx          transaction
     * @return
     */
    private static boolean nodeTypeTxProcess(int height, Transaction tx) {
        if (tx == null)  return false;

        PocTxBody.PocNodeTypeV2 nodeTypeV2 = null;
        PocTxBody.PocNodeTypeV3 nodeTypeV3 = null;
        Attachment attachment = tx.getAttachment();
        String summary = "";
        if(attachment instanceof PocTxBody.PocNodeTypeV3){
            nodeTypeV3 = (PocTxBody.PocNodeTypeV3) attachment;
            summary = "V3 host=" + nodeTypeV3.getIp() + ",type=" + nodeTypeV3.getType().getName() + ",accountId=" + nodeTypeV3.getAccountId() + ", disk capacity=" + nodeTypeV3.getDiskCapacity();
        }
        else if(nodeTypeV3 == null && attachment instanceof PocTxBody.PocNodeTypeV2){
            nodeTypeV2 = (PocTxBody.PocNodeTypeV2) attachment;
            summary = "V2 host=" + nodeTypeV2.getIp() + ",type=" + nodeTypeV2.getType().getName() + ",accountId=" + nodeTypeV2.getAccountId();
        }
        else if(nodeTypeV2 == null && attachment instanceof PocTxBody.PocNodeType) {
            PocTxBody.PocNodeType nodeType = (PocTxBody.PocNodeType) attachment;
            summary = "V1 host=" + nodeType.getIp() + ",type=" + nodeType.getType().getName();
            nodeTypeV2 = CheckSumValidator.isPreAccountsInTestnet(nodeType.getIp(), height);
        }
        
        if(nodeTypeV3 == null && nodeTypeV2 == null) {
            Logger.logWarningMessage("NodeType tx[id=%d,height=%d] summary[%s] is v1 that missing the account id, can't process it correctly", tx.getId(), tx.getHeight(), summary);
            return false;
        }
        long accountId = -1;
        String ip = null;
        Peer.Type type = null;

        if(nodeTypeV3 != null ) {
            accountId =  nodeTypeV3.getAccountId() ;
            ip = nodeTypeV3.getIp();
            type = nodeTypeV3.getType();
        }else if(nodeTypeV2 != null){
            accountId =  nodeTypeV2.getAccountId() ;
            ip = nodeTypeV2.getIp();
            type = nodeTypeV2.getType();
        }

        if(LocalDebugTool.isCheckPocAccount(accountId)){
            Logger.logDebugMessage("[LocalDebugMode] node statement address %s is in the poc tx processing ", Account.rsAccount(accountId));
        }

        PocScore pocScoreToUpdate = PocHolder.getPocScore(height, accountId);
        PocHolder.saveOrUpdate(pocScoreToUpdate.setHeight(height).nodeTypeCal(nodeTypeV3 != null ? nodeTypeV3 : nodeTypeV2));
        
        if(StringUtils.isEmpty(ip) || type == null) {
            Logger.logWarningMessage("NodeType tx[id=%d,height=%d,summary=%s] is a bad tx, don't add the certified peer", tx.getId(), tx.getHeight(), summary);
        }else{
            PocHolder.addCertifiedPeer(height, type, ip, accountId);
        }
        return true;
    }


    /**
     * process the node conf tx of poc series
     *
     * @param height      block height that included this tx
     * @param pocNodeConf PocNodeConf tx
     * @return
     */
    private static boolean nodeConfTxProcess(int height, PocTxBody.PocNodeConf pocNodeConf) {
        CertifiedPeer certifiedPeer = PocHolder.getBoundPeer(pocNodeConf.getHost(), height);
        if(certifiedPeer == null) return false;

        PocScore pocScoreToUpdate = PocHolder.getPocScore(height, certifiedPeer.getBoundAccountId());
        PocHolder.saveOrUpdate(pocScoreToUpdate.setHeight(height).nodeConfCal(pocNodeConf));

        return true;
    }

    /**
     * process the online rate tx of poc series
     *
     * @param height     block height that included this tx
     * @param onlineRate OnlineRate tx
     * @return
     */
    private static boolean onlineRateTxProcess(int height, PocTxBody.PocOnlineRate onlineRate) {
        CertifiedPeer certifiedPeer = PocHolder.getBoundPeer(onlineRate.getHost(), height);
        if(certifiedPeer == null) return false;

        PocScore pocScoreToUpdate = PocHolder.getPocScore(height, certifiedPeer.getBoundAccountId());
        PocHolder.saveOrUpdate(pocScoreToUpdate.setHeight(height).onlineRateCal(certifiedPeer.getType(), onlineRate));

        return true;
    }

    /**
     * process the block miss tx of poc series
     *
     * @param height          block height that included this tx
     * @param pocBlockMissing PocBlockMissing tx
     * @return
     */
    private static boolean blockMissingTxProcess(int height, PocTxBody.PocGenerationMissing pocBlockMissing) {

        List<Long> missAccountIds = pocBlockMissing.getMissingAccountIds();
        for (Long missAccountId : missAccountIds) {
            PocScore pocScoreToUpdate = PocHolder.getPocScore(height, missAccountId);
            PocHolder.saveOrUpdate(pocScoreToUpdate.setHeight(height).blockMissCal(pocBlockMissing));
        }
        return true;
    }

    /**
     * process the balance, poc  of account changed
     *
     * @param height  block height that included this tx
     * @param account which balance is changed
     * @return
     */
    private static boolean balanceChangedProcess(int height, Account account) {
        if (account == null) {
            return false;
        }
        long accountId = account.getId();
        if(LocalDebugTool.isCheckPocAccount(accountId)) {
            Logger.logDebugMessage("[LocalDebugMode] " + Account.rsAccount(accountId) + "'s balance is changed at height " + height);
        }
        PocScore pocScoreToUpdate = PocHolder.getPocScore(height, accountId);
        PocHolder.saveOrUpdate(pocScoreToUpdate.setHeight(height).ssCal());
        return true;
    }



}
