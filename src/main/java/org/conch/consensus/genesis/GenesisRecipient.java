package org.conch.consensus.genesis;

import com.google.common.collect.Lists;
import org.conch.common.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:xy@sharder.org">Ben</a>
 * @since 2019-01-28
 */
public class GenesisRecipient {
    public long id;
    public long amount;
    public byte[] publicKey;
    public byte[] signature;

    GenesisRecipient(long id, long amount, byte[] publicKey, byte[] signature) {
        this.id = id;
        this.amount = amount;
        this.publicKey = publicKey;
        this.signature = signature;
    }
    static Map<Constants.Network, List<GenesisRecipient>> genesisRecipients = new HashMap<>();
    static {
        List<GenesisRecipient> devnetRecipients = Lists.newArrayList(
                new GenesisRecipient(6219247923802955552L, 5000000000L,
                        new byte[]{-71, -79, -127, -96, -9, -33, -46, -124, 122, 42, -79, 20, -45, -4, 72, 83, -69, 78, 65, 64, 31, 71, 33, -22, -80, 68, -12, -10, -115, 22, -40, 118},
                        new byte[]{-28,-84,16,39,-23,-81,-10,-22,-18,-60,-10,11,123,119,-79,73,-120,-123,41,97,67,39,-69,31,36,125,26,-124,35,-10,27,0,-116,-89,-110,77,-79,97,-83,127,-17,
                                65,-40,72,-125,-7,55,32,-31,67,-120,23,-45,122,-52,15,27,-95,-105,-118,12,-75,-104,66}),

                new GenesisRecipient(3790328149872734783L, 50000000L,
                        new byte[]{30, 5, 14, 84, -15, -59, 81, 105, 88, -43, 119, 56, 10, 34, 62, -93, 23, -122, 95, -14, 21, 35, 8, 58, -62, 6, -114, 52, -72, 35, 120, 17},
                        new byte[]{61,-84,-113,35,-123,61,101,17,-105,-6,93,53,116,-94,-51,-108,108,2,120,45,100,51,75,38,-29,-32,-102,-83,61,-100,-13,13,-99,-110,68,-103,-64,27,-70,86,-29,
                                -15,12,42,-4,-62,-77,-111,-48,19,-14,81,-25,-46,38,-94,57,63,-18,-112,-74,-108,-85,67}),

                new GenesisRecipient(-90778548339644322L, 50000000L,
                        new byte[]{-84, 106, -61, 126, 13, 13, -39, 70, 49, -2, 113, 70, 87, 27, -120, 118, -72, 21, 39, 30, 87, 120, 45, 48, -101, -35, 33, -83, -12, 60, -22, 97},
                        new byte[]{-105,109,105,55,104,-74,27,2,40,82,102,-59,-66,-54,50,-91,-24,106,-52,-29,-69,9,111,55,31,-43,-117,-66,-19,-124,-48,3,91,-37,-54,-17,-107,-82,22,-61,-49,
                                -25,10,55,-70,22,-26,92,-17,-125,-59,101,-128,-72,24,-8,-15,25,-64,-12,108,-106,-65,-112}),

                new GenesisRecipient(-8419238846229198038L, 50000000L,
                        new byte[]{112, -49, 57, 82, 38, -96, 51, -94, 84, 62, 44, -11, 39, 32, 27, -62, 59, -52, -8, -5, -81, -45, 99, 8, -16, -71, -80, -46, -49, -74, -21, 88},
                        new byte[]{-7,-120,-45,0,-45,-119,111,31,17,-86,-42,-41,-60,-108,103,-119,65,-72,-27,106,109,118,28,95,-7,-108,-28,-83,-66,-36,-16,11,33,-70,-64,74,103,-99,35,2,38,
                                -61,44,88,-31,-39,88,48,66,71,-48,-113,37,-12,97,-53,13,-124,-62,-92,39,-10,81,-59}),

                new GenesisRecipient(9011521658538046719L, 100000000L,
                        new byte[]{45, -47, 43, 69, 124, 115, -15, -34, -45, -65, 5, 101, 3, 76, 24, 67, -20, -128, 72, -93, -39, -106, 78, -22, 41, -34, 85, -118, -16, 50, 8, 89},
                        new byte[]{116,14,124,-89,-84,-28,-35,53,-108,-43,105,-51,-124,-57,-98,-2,-29,97,-108,-69,115,-20,108,22,-52,82,-69,100,-2,72,10,11,50,21,-29,-74,-79,80,-13,-67,69,
                                96,-113,-69,-43,112,85,13,83,58,34,-32,77,-93,-76,-54,108,-41,-104,39,-112,103,74,126})
        );
        
        List<GenesisRecipient> testnetRecipients = Lists.newArrayList(
                // airdrop (exchange the TSS to hub owner or other users)
                new GenesisRecipient(-8649917005023082368L, 100000000L,
                        new byte[]{-123, 13, -107, -90, 26, -80, -120, -102, -99, 113, -54, -21, -88, -48, -98, 108, -24, 127, 9, 103, 77, -119, 91, -80, 63, 88, -48, -19, 35, -53, -84, 123},
                        new byte[]{-44,60,115,-78,-32,27,-111,57,102,-61,28,62,-35,101,113,27,54,114,-51,32,-18,47,43,-86,-32,53,115,103,-42,-118,-94,7,-52,-124,-70,58,26,
                                -11,103,-2,-42,-53,-48,-69,77,59,56,95,-7,-25,-28,-60,65,-8,10,124,-100,27,84,-20,77,-110,-23,46}),
                // boot node
                new GenesisRecipient(-4542396882408079631L, 5000000L,
                        new byte[]{23, 39, 60, 120, -28, -42, -69, -9, -31, 62, 76, 69, 99, 36, 100, -17, -45, -7, 77, 106, 82, -19, 124, -113, 105, 38, -114, 59, 23, 32, 100, 114},
                        new byte[]{98,-101,-84,48,53,115,103,-11,18,10,-77,37,56,-122,-112,-13,-17,101,57,27,67,-6,-65,87,82,-35,78,87,-40,-103,-16,0,63,76,-5,2,107,-126,33,98,-68,81,116,-110,
                                -70,10,87,-81,-44,-45,119,-25,23,-22,26,120,-110,-36,-26,-125,62,2,76,79}),
                // foundation node
                new GenesisRecipient(-6802345313304048560L, 500000L,
                        new byte[]{-118, -11, -40, -124, -42, 82, 12, -42, 51, 42, 100, -16, -114, 106, 115, -43, -95, -111, 115, -114, -69, -95, -57, -119, -59, 55, 48, -122, 120, 33, 36, 87},
                        new byte[]{-54,-36,16,127,40,-34,-70,-96,-63,35,-105,-88,49,-74,-24,-41,109,27,-58,20,113,65,77,39,-100,-9,98,-62,-117,56,117,10,60,-30,110,-54,3,94,19,-79,56,-108,50,
                                51,-17,-118,-21,110,-75,83,-39,-93,47,76,60,-35,1,-53,5,-123,-103,25,-8,96}),
                // foundation node
                new GenesisRecipient(6066546424236439063L, 500000L,
                        new byte[]{107, 97, -16, 73, 67, 52, -19, 96, -80, 48, -78, 98, -76, -83, 92, -29, 62, 96, 67, -95, 50, -114, 68, 99, -96, 20, -39, -66, -104, -104, -104, 17},
                        new byte[]{-22,52,-81,30,126,-96,73,80,52,-68,-32,-27,-95,61,45,-95,53,-126,85,53,76,45,-37,-9,-78,-72,-21,112,79,80,103,8,-59,10,92,109,-88,-55,-57,-22,105,101,47,-17,
                                -28,-123,127,-85,-57,-5,2,32,58,-96,-54,41,-81,6,96,58,-20,72,67,-86}),
                // hub
                new GenesisRecipient(4691986734623238501L, 500000L, 
                        new byte[]{14, 107, -59, -118, 6, -38, 105, 93, 14, 36, 33, 101, 48, 49, -68, 56, -121, -52, 55, -86, 25, 122, 72, -67, -35, 21, -68, -42, -58, -42, -86, 40},
                        new byte[]{-115,-55,-113,98,-118,92,1,-103,9,77,-96,-117,116,-54,-48,113,-33,26,-103,54,-106,0,38,36,-75,74,118,53,63,-125,-44,12,31,-62,-101,39,46,-84,-23,102,46,25,
                                -125,-49,-128,-17,-70,28,20,30,-55,-127,106,-88,-99,85,87,-102,-87,-43,53,112,77,9})
        );

        List<GenesisRecipient> mainnetRecipients = Lists.newArrayList(

        );
        
        genesisRecipients.put(Constants.Network.DEVNET,devnetRecipients);
        genesisRecipients.put(Constants.Network.TESTNET,testnetRecipients);
        genesisRecipients.put(Constants.Network.MAINNET,mainnetRecipients);
    }
    
    public static GenesisRecipient getByAccountId(long accountId) {
        for(GenesisRecipient recipient : getAll()){
            if(recipient.id == accountId){
                return recipient;
            }
        }
        return null;
    }

    public static List<GenesisRecipient> getAll(){
        return genesisRecipients.get(Constants.getNetwork());
    }
}