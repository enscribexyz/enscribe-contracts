package org.web3j;

import org.web3j.ens.NameHash;
import org.web3j.ens.contracts.generated.ENSRegistryWithFallbackContract;
import org.web3j.generated.contracts.HelloWorld;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import static org.web3j.Web3LabsContractDeployer.credentials;
import static org.web3j.Web3LabsContractDeployer.gasLimit;
import static org.web3j.Web3LabsContractDeployer.web3j;

public class EnsNamed {
    private static final String web3LabsContractAddress = "0x5CEDDD691070082e7106e8d4ECf0896F9D9930D8";
    private static final String ensRegistryContractAddress = "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e";
    private static final String web3LabsEns = "testapp.eth";


    public static void main(String[] args) throws Exception {

        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFee =
                web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                        .send()
                        .getBlock()
                        .getBaseFeePerGas();
        BigInteger maxFeePerGas = baseFee.multiply(BigInteger.valueOf(2)).add(maxPriorityFeePerGas);
        StaticEIP1559GasProvider eip1559GasProvider = new StaticEIP1559GasProvider(11155111L, maxFeePerGas, maxPriorityFeePerGas, gasLimit);

        Web3LabsContract web3LabsContract = Web3LabsContract.load(web3LabsContractAddress, web3j, credentials, eip1559GasProvider);
        ENSRegistryWithFallbackContract ensRegistry = ENSRegistryWithFallbackContract.load(ensRegistryContractAddress, web3j, credentials, eip1559GasProvider);

        byte[] helloWorldByteCode = Numeric.hexStringToByteArray("0x" + HelloWorld.BINARY);

        // 1. Use our ENS parent -> just give label and bytecode
        // 2. Use their own ENS parent -> give label, parentEns and bytecode
        //    Note: Transfer managership to our contract and then call setNameAndDeploy

        // First use case (using web3Labs domain)
//         web3LabsDomain(helloWorldByteCode, "v2", web3LabsContract);

        // second use case (using their own parent domain)
         userDomain(helloWorldByteCode, "v1", "sub.named.eth", web3LabsContract, ensRegistry);

        // third use case (When contract is already deployed)
//         contractDeployed("0xF28789Dc7bFBAE61221f08196C5aF0016AA8bB62", "v2", web3LabsContract);


        //0x1aebfef28030ff72d3aa5caeb0fbbfd893063387d3a375a94046f137dae9fa04
//        EthGetTransactionReceipt receipt = web3j.ethGetTransactionReceipt("0x1aebfef28030ff72d3aa5caeb0fbbfd893063387d3a375a94046f137dae9fa04").send();
//        System.out.println(receipt.getTransactionReceipt().get());

    }

    static void web3LabsDomain(byte[] contractByteCode, String label, Web3LabsContract web3LabsContract) throws Exception {
        final String parent = web3LabsEns;
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(contractByteCode, label, parent, parentNode, new BigInteger("100000000000000")).send();
        System.out.println("receipt = " + receipt);
    }

    static void userDomain(byte[] contractByteCode, String label, String parent, Web3LabsContract web3LabsContract, ENSRegistryWithFallbackContract ensRegistry) throws Exception {
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        // check for current manager
        if(!ensRegistry.owner(parentNode).send().equalsIgnoreCase(web3LabsContractAddress)) {
            TransactionReceipt receiptChangeManager = ensRegistry.setOwner(parentNode, web3LabsContractAddress).send();
            System.out.println("Manager parent receipt = " + receiptChangeManager);
            // 3LD parent can get back using setSubnodeOwner(bytes32 node,bytes32 label,address owner) on ENS registry
            // 2LD parent can get back using reclaim(uint256 id, address owner) on ENS BaseRegistrarImplementation, id is tokenID
        }
        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(contractByteCode, label, parent, parentNode, new BigInteger("100000000000000")).send();
        System.out.println("receipt = " + receipt);
    }

    static void contractDeployed(String contractAddress, String label, Web3LabsContract web3LabsContract) throws Exception {
        final String parent = web3LabsEns;
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        TransactionReceipt receipt =  web3LabsContract.setName(contractAddress, label, parent, parentNode).send();
        System.out.println("receipt = " + receipt);
    }
}
