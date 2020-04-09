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
                new GenesisRecipient(-5748075279486471936L, 191952L,
                        new byte[]{23, -109, -96, -121, -32, -57, -19, -98, -80, 23, 111, 71, 89, 0, -64, -44, 76, -15, 18, -86, 16, 50, 78, -92, 120, -9, 98, -73, 50, 14, -29, 98},
                        new byte[]{0, 49, 1, -24, -4, -69, 58, -80, -6, -12, 18, -76, 2, -56, 70, -21, -8, 46, -125, 107, -43, 126, 12, -124, 82, -8, 45, 73, 88, -62, -67, 91}),

                // r&d account
                new GenesisRecipient(2254931919665477521L, 120000000L,
                        new byte[]{30, 121, -12, -20, -39, -73, 91, 33, -111, 112, 60, -21, 23, 122, -8, 32, 8, -125, 17, -20, -20, 36, -50, -54, 111, -42, -35, -128, -30, 121, 80, 75},
                        new byte[]{-111, -73, 89, 64, -94, 32, 75, 31, 7, 32, -32, 96, -112, -13, -104, 123, 95, -9, 2, -8, 40, -77, -125, -116, 40, 92, -53, -58, 72, -109, 111, 90}),

                // investor account
                new GenesisRecipient(-3743057762140472722L, 100000000L,
                        new byte[]{70, 49, -76, 125, 35, -52, -28, -110, -57, -39, 52, -81, 110, 71, 118, -32, 94, 26, 4, -33, -73, 61, 95, -44, 4, -119, -56, 81, -8, 4, -6, 83},
                        new byte[]{110, 58, -39, 1, -52, -4, 13, -52, 112, -113, 64, 42, 40, 63, -44, -51, 35, 29, 92, -112, -17, 3, -85, -127, 118, 65, -69, 51, -104, 108, 29, -76}),

                // foundation account
                new GenesisRecipient(1424584630071542656L, 79949923L,
                        new byte[]{75, 7, -35, 40, -86, -95, -24, -12, -28, -118, -93, 69, -14, 15, -95, -74, -76, 47, 111, 100, -13, 122, 84, 119, 28, -100, 18, -64, 52, 112, -81, 63},
                        new byte[]{-128, 67, -27, 91, 44, 36, -59, 19, -101, -83, -45, -86, -23, -34, 46, -107, -1, -53, -9, 24, -84, -127, -122, 38, -33, 45, 6, 67, -15, 49, -125, -2}),

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