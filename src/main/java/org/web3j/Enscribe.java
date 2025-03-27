package org.web3j;

import org.web3j.crypto.Hash;
import org.web3j.ens.NameHash;
import org.web3j.ens.contracts.generated.ENSRegistryWithFallbackContract;
import org.web3j.generated.contracts.BaseRegistrarImplementation;
import org.web3j.generated.contracts.EnscribeSepolia;
import org.web3j.generated.contracts.HelloWorld;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static org.web3j.EnscribeContractDeployer.credentials;
import static org.web3j.EnscribeContractDeployer.gasLimit;
import static org.web3j.EnscribeContractDeployer.web3j;
import static org.web3j.ens.NameHash.normalise;

public class Enscribe {
    private static final byte[] EMPTY = new byte[32];
    private static final String enscribeContractAddress = "0xe480db220661Ae181F122d2544683e405006c2eD";
    private static final String ensRegistryContractAddress = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e";
    private static final String baseRegistrarImplementationContractAddress = "0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85";
    private static final String web3LabsEns = "testapp.eth";
    private static final BigInteger etherValue = new BigInteger("100000000000000");


    public static void main(String[] args) throws Exception {

        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFee =
                web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                        .send()
                        .getBlock()
                        .getBaseFeePerGas();
        BigInteger maxFeePerGas = baseFee.multiply(BigInteger.valueOf(2)).add(maxPriorityFeePerGas);
        StaticEIP1559GasProvider eip1559GasProvider = new StaticEIP1559GasProvider(11155111L, maxFeePerGas, maxPriorityFeePerGas, gasLimit);

        EnscribeSepolia web3LabsContract = EnscribeSepolia.load(enscribeContractAddress, web3j, credentials, eip1559GasProvider);
        ENSRegistryWithFallbackContract ensRegistry = ENSRegistryWithFallbackContract.load(ensRegistryContractAddress, web3j, credentials, eip1559GasProvider);
        BaseRegistrarImplementation baseRegistrarImplementation = BaseRegistrarImplementation.load(baseRegistrarImplementationContractAddress, web3j, credentials, eip1559GasProvider);

        byte[] helloWorldByteCode = Numeric.hexStringToByteArray("0x" + HelloWorld.BINARY);

        // 1. Use our ENS parent -> just give label and bytecode
        // 2. Use their own ENS parent -> give label, parentEns and bytecode
        //    Note: Transfer managership to our contract and then call setNameAndDeploy

        // First use case (using web3Labs domain)
        // unwrapped - testapp.eth
//         web3LabsDomain(helloWorldByteCode, "v6", "testapp.eth", web3LabsContract);

         // wrapped - web3labs2.eth
//        web3LabsDomain(helloWorldByteCode, "v1", "v1.web3labs2.eth", web3LabsContract);

        // second use case (using their own parent domain)
//         userDomain(helloWorldByteCode, "v1", "named.eth", web3LabsContract, ensRegistry, baseRegistrarImplementation);

//         third use case (When contract is already deployed)
//         contractDeployed("0xF28789Dc7bFBAE61221f08196C5aF0016AA8bB62", "v2", web3LabsContract);


        //0x1aebfef28030ff72d3aa5caeb0fbbfd893063387d3a375a94046f137dae9fa04
//        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt("0x1aebfef28030ff72d3aa5caeb0fbbfd893063387d3a375a94046f137dae9fa04").send();
//        System.out.println(receipt.getTransactionReceipt().get());


    }

    static void web3LabsDomain(byte[] contractByteCode, String label, String parent, EnscribeSepolia web3LabsContract) throws Exception {
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(contractByteCode, label, parent, parentNode, etherValue).send();
        System.out.println("receipt = " + receipt);
    }

    static void userDomain(byte[] contractByteCode, String label, String parent, EnscribeSepolia web3LabsContract, ENSRegistryWithFallbackContract ensRegistry, BaseRegistrarImplementation baseRegistrarImplementation) throws Exception {
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        // check for current manager ENS Registry owner
        if(!ensRegistry.owner(parentNode).send().equalsIgnoreCase(enscribeContractAddress)) {
            // 3LD to give manager access call setOwner in ENS registry
            // 2LD to give manager access call reclaim(uint256 id, address owner) on ENS BaseRegistrarImplementation\

            if(parent.chars().filter(ch -> ch == '.').count() == 1L) {
                byte[] labelHash = labelHashAsBytes(parent);
                BigInteger tokenId = new BigInteger(Numeric.toHexString(labelHash).substring(2), 16);

                TransactionReceipt receiptChangeManager = baseRegistrarImplementation.reclaim(tokenId, enscribeContractAddress).send();
                System.out.println("2LD Manager parent receipt = " + receiptChangeManager);
            }
            else {
                TransactionReceipt receiptChangeManager = ensRegistry.setOwner(parentNode, enscribeContractAddress).send();
                System.out.println("3LD Manager parent receipt = " + receiptChangeManager);
            }

            // 3LD parent can get back using setSubnodeOwner(bytes32 node,bytes32 label,address owner) on ENS registry
            // 2LD parent can get back using reclaim(uint256 id, address owner) on ENS BaseRegistrarImplementation, id is tokenID
        }
        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(contractByteCode, label, parent, parentNode, etherValue).send();
        System.out.println("receipt = " + receipt);
    }

    static void contractDeployed(String contractAddress, String label, EnscribeSepolia web3LabsContract) throws Exception {
        final String parent = web3LabsEns;
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        TransactionReceipt receipt =  web3LabsContract.setName(contractAddress, label, parent, parentNode, etherValue).send();
        System.out.println("receipt = " + receipt);
    }

    public static String labelHash(String ensName) {
        if (ensName.isEmpty()) {
            return Numeric.toHexString(EMPTY);
        } else {
            String normalisedEnsName = normalise(ensName);
            return Numeric.toHexString(Hash.sha3(normalisedEnsName.split("\\.")[0].getBytes(StandardCharsets.UTF_8)));
        }
    }

    public static byte[] labelHashAsBytes(String ensName) {
        return Numeric.hexStringToByteArray(labelHash(ensName));
    }
}
