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

                new GenesisRecipient(3790328149872734783L, 5000000000L,
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

        //10 billion -> Miners: 7 billion, R&D: 1.2 billion, Investors: 1 billion, Foundation: 0.8 billion
        List<GenesisRecipient> testnetRecipients = Lists.newArrayList(
                // boot node
                new GenesisRecipient(-7290871798082871685L, 10000000L,
                        new byte[]{-97, 2, 82, -64, 4, -28, 127, 17, 85, -128, -118, 12, 15, -65, 108, -94, -47, -16, -58, -63, 1, 10, -125, 125, 40, 119, 2, 26, -20, -109, 81, 92},
                        new byte[]{123, 94, 91, 116, -70, -98, -47, -102, 101, 76, -9, 87, -32, -8, -23, -98, 39, -80, 123, -99, 121, -110, -71, 4, -91, 102, 51, 20, 61, -110, -121, -108}),

                // foundation account
                new GenesisRecipient(1868021154578573726L, 70000000L,
                        new byte[]{-113, 80, -73, 17, 35, 120, -115, -44, 17, -24, -60, -93, 25, 33, 112, -60, 90, -10, -23, -7, -20, -30, -8, 88, -50, 74, 48, 125, 104, -5, -109, 0},
                        new byte[]{-98, 41, -110, -57, 87, -117, -20, 25, -117, 10, 73, -39, 105, -79, 63, -103, 94, -87, -113, 79, -61, 109, 108, 52, -80, -124, -84, 117, -110, -115, 76, 77}),

                // r&d account
                new GenesisRecipient(7907279213654684859L, 500000L,
                        new byte[]{112, 40, 96, 57, 120, -8, 48, 88, -63, -91, -94, -100, 75, -58, -117, 53, 38, -112, 45, 121, -45, -123, -15, -29, 6, 68, 102, -45, -10, -59, -51, 72},
                        new byte[]{-69, -76, -73, -35, -116, 76, -68, 109, -91, -112, -92, -49, 3, 111, 111, 118, 57, 102, -92, 57, 124, -116, 73, 52, 67, 104, 80, 64, -62, -38, -19, -45}),

                // investor account
                new GenesisRecipient(6190761055601299540L, 500000L,
                        new byte[]{-111, -80, -37, -35, 126, 118, 88, -112, -45, 120, -78, -14, -47, -64, 1, -32, 117, -22, -121, -13, 78, 92, 23, -43, 41, -4, 127, 26, 107, -84, 122, 110},
                        new byte[]{84, 16, 112, 72, 97, 0, -22, 85, 52, 97, 18, -76, 120, -110, 88, 43, 8, 54, -21, 45, 37, -22, 85, -57, -7, 58, 26, -108, 120, 22, -49, -46}),

                // tx creator
                new GenesisRecipient(-3033725234207221433L, 50077L,
                        new byte[]{31, -128, -119, -5, -20, 57, -38, 95, -37, -46, -70, 116, 78, -41, 53, 71, 39, 81, -110, 96, -17, 68, 7, 57, 61, 31, 103, -55, -49, -33, 35, 89},
                        new byte[]{71, -103, -16, 12, -7, 10, -26, -43, -34, 24, -114, 21, 26, -87, -5, 82, -77, -31, 63, -82, 71, -91, -13, -20, 104, -74, 85, 64, 39, -23, 3, 40})
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