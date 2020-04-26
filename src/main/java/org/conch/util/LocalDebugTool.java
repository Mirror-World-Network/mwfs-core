package org.conch.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.conch.account.Account;
import org.conch.env.RuntimeEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

/**
 * Used to local debug, contains all debug dirty code
 */
public class LocalDebugTool {

    private static final String LOCAL_DEBUG_CONFIG = "local-debug.properties";
    private static Properties localDebugConfig = new Properties();
    private static final String SEPARATOR = ",";
    static {
        Path confDir = Paths.get(".", "conf");
        Path propPath = Paths.get(confDir.toString()).resolve(Paths.get(LOCAL_DEBUG_CONFIG));
        if (Files.isReadable(propPath)) {
            System.out.printf("Loading %s from dir %s\n", localDebugConfig, confDir);
            try {
                localDebugConfig.load(Files.newInputStream(propPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final String DEBUG_PROPERTY = "debug.check.poc.accounts";

    /**
     * used to local debug
     * @return true： local debug mode
     */
    public static boolean isLocalDebug(){
        String localDebugEnv = System.getProperty(RuntimeEnvironment.LOCALDEBUG_ARG);
        return StringUtils.isNotEmpty(localDebugEnv) ? Boolean.parseBoolean(localDebugEnv) : false;
    }

    // read from the debug config
    private static final List<String> checkPocAccounts = Lists.newArrayList(localDebugConfig.getProperty(DEBUG_PROPERTY,"").split(SEPARATOR));

    public static boolean isCheckPocAccount(String rsAccount){
       return isLocalDebug() && checkPocAccounts.contains(rsAccount);
    }

    public static boolean isCheckPocAccount(long accountId){
       return isLocalDebug() && checkPocAccounts.contains(Account.rsAccount(accountId));
    }
}
