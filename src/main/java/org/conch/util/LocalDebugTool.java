package org.conch.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.conch.account.Account;
import org.conch.env.RuntimeEnvironment;

import java.util.List;

/**
 * Used to local debug, contains all debug dirty code
 */
public class LocalDebugTool {
    /**
     * used to local debug
     * @return true： local debug mode
     */
    public static boolean isLocalDebug(){
        String localDebugEnv = System.getProperty(RuntimeEnvironment.LOCALDEBUG_ARG);
        return StringUtils.isNotEmpty(localDebugEnv) ? Boolean.parseBoolean(localDebugEnv) : false;
    }

    // read from the debug config
    private static final List<String> checkPocAccounts = Lists.newArrayList(

    );

    public static boolean isCheckPocAccount(String rsAccount){
       return isLocalDebug() && checkPocAccounts.contains(rsAccount);
    }

    public static boolean isCheckPocAccount(long accountId){
       return isLocalDebug() && checkPocAccounts.contains(Account.rsAccount(accountId));
    }
}
