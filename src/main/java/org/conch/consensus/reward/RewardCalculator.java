package org.conch.consensus.reward;

import com.google.common.collect.Maps;
import org.conch.Conch;
import org.conch.account.Account;
import org.conch.account.AccountLedger;
import org.conch.common.Constants;
import org.conch.mint.pool.PoolRule;
import org.conch.tx.Attachment;
import org.conch.tx.Transaction;
import org.conch.util.Logger;

import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019/1/8
 */
public class RewardCalculator {

    /**
     * Reward definition, amount is the reward amount
     */
    public enum RewardDef {
        MINT(1333);

        private final long amount;

        public long getAmount() {
            return amount;
        }

        RewardDef(long amount) {
            this.amount = amount;
        }
        
    }
    
//    private static final int HALVE_COUNT = 210240;
    public static final int MINER_JOINING_PHASE = 2600; // around '2020-05-06 21:00'
    /**
     * how much one block reward
     * @return
     */
    public static long blockReward() {
        /**
         * halving logic
        double turn = 0d;
        if(Conch.getBlockchain().getHeight() > HALVE_COUNT){
            turn = Conch.getBlockchain().getHeight() / HALVE_COUNT;
        }
        double rate = Math.pow(0.5d,turn);
        return (long)(Constants.ONE_SS * RewardDef.MINT.getAmount() * rate);
        **/

        // No block rewards in the miner joining phase
        if(Conch.getHeight() <= MINER_JOINING_PHASE) return 1L;

        return Constants.ONE_SS * RewardDef.MINT.getAmount();
    }

    /**
     * read the current qualified miners and calculate the reward distribution according to poc score rate:
     * - qualified condition:
     * a) create a tx to declared the node;
     * b) lined the mining address;
     * c) balance at current height > 1064 MW（8T staking amount）;
     * @return map: miner's account id : poc score
     */
    public static Map<Long, Long> generateCrowdMinerRewardMap(){
        Map<Long, Long> crowdMinerRewardMap = Maps.newHashMap();
        // read the qualified miner list

        // generate the poc score map

        return crowdMinerRewardMap;
    }

    /**
     * reward calculation algo.: miner's PoC score / total miner's PoC score * 667 MW
     * @param account
     * @param transaction
     * @param amount
     * @param stageTwo
     */
    private static void calAndSetCrowdMinerReward(Account account, Transaction transaction, long amount, boolean stageTwo){

    }

    /**
     *
     * @param account
     * @param transaction
     * @param amount
     * @param stageTwo
     */
    private static void calAndSetMiningReward(Account account, Transaction transaction, long amount, boolean stageTwo){
        if(!stageTwo) {
            account.addBalanceAddUnconfirmed(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), amount);
            account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), amount);
            Logger.logDebugMessage("[Stage One]add mining rewards %d to %s unconfirmed balance and freeze it of tx %d at height %d",
                    amount, account.getRsAddress(), transaction.getId() , transaction.getHeight());
        }else{
            if(Constants.isTestnet() && account.getFrozenBalanceNQT() <= 0) {
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), account.getFrozenBalanceNQT());
            }else if(Constants.isTestnet() && account.getFrozenBalanceNQT() <= amount){
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), -account.getFrozenBalanceNQT());
            }else{
                account.addFrozen(AccountLedger.LedgerEvent.BLOCK_GENERATED, transaction.getId(), -amount);
            }
            account.addMintedBalance(amount);
            account.pocChanged();
            Logger.logDebugMessage("[Stage Two]unfreeze mining rewards %d of %s and add it in mined amount of tx %d at height %d",
                    amount, account.getRsAddress(), transaction.getId() , transaction.getHeight());
        }
    }

    /**
     * total 2 stages:
     * stage one is in tx accepted, the rewards need be lock;
     * stage two is the block confirmations reached, means unlock the rewards and record mined amount
     * @param tx reward tx
     * @param stageTwo true - stage two; false - stage one
     * @return
     */
    public static long blockRewardDistribution(Transaction tx, boolean stageTwo) {
        Attachment.CoinBase coinBase = (Attachment.CoinBase) tx.getAttachment();
        Account senderAccount = Account.getAccount(tx.getSenderId());
        Map<Long, Long> consignors = coinBase.getConsignors();

        if (consignors.size() == 0) {
            calAndSetMiningReward(senderAccount, tx, tx.getAmountNQT(), stageTwo);
        } else {
            Map<Long, Long> rewardList = PoolRule.calRewardMapAccordingToRules(senderAccount.getId(), coinBase.getGeneratorId(), tx.getAmountNQT(), consignors);
            for (long id : rewardList.keySet()) {
                Account account = Account.getAccount(id);
                calAndSetMiningReward(account, tx, rewardList.get(id), stageTwo);
            }
        }
        return tx.getAmountNQT();
    }

}