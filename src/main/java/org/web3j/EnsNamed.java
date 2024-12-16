package org.web3j;

import org.web3j.ens.NameHash;
import org.web3j.generated.contracts.HelloWorld;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import static org.web3j.Web3LabsContractDeployer.credentials;
import static org.web3j.Web3LabsContractDeployer.gasLimit;
import static org.web3j.Web3LabsContractDeployer.gasPrice;
import static org.web3j.Web3LabsContractDeployer.web3j;

public class EnsNamed {
    private static final String web3LabsContractAddress = "0x49b66baecd21cc87a8300f70d917617a227b78b8";
    private static final Web3LabsContract web3LabsContract = Web3LabsContract.load(web3LabsContractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));

    public static void main(String[] args) throws Exception {

        byte[] helloWorldByteCode = Numeric.hexStringToByteArray("0x" + HelloWorld.BINARY);

        // 1. Use our ENS parent -> just give label and bytecode
        // 2. Use their own ENS parent -> give label, parentEns and bytecode
        //    Note: Transfer Ownership to our contract and then call setNameAndDeploy

        // First use case (using web3Labs domain)
        web3LabsDomain(helloWorldByteCode, "v1");

        // second use case (using their own parent domain)
        userDomain(helloWorldByteCode, "v1", "test6.web3labs2.eth");


    }

    static void web3LabsDomain(byte[] contractByteCode, String label) throws Exception {
        final String parent = "web3labs2.eth";
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(contractByteCode, label, parent, parentNode).send();
        System.out.println("receipt = " + receipt);
    }

    static void userDomain(byte[] contractByteCode, String label, String parent) throws Exception {
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(contractByteCode, label, parent, parentNode).send();
        System.out.println("receipt = " + receipt);
    }
}
