package org.web3j;

import org.web3j.ens.NameHash;
import org.web3j.generated.contracts.HelloWorld;
import org.web3j.generated.contracts.NameWrapper;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import static org.web3j.Web3LabsContractDeployer.credentials;
import static org.web3j.Web3LabsContractDeployer.gasLimit;
import static org.web3j.Web3LabsContractDeployer.gasPrice;
import static org.web3j.Web3LabsContractDeployer.web3j;

public class EnsNamed {
    private static final String web3LabsContractAddress = "0x3e71bC0e1729c111dd3E6aaB923886d0A7FeD437";
    private static final String nameWrapperContractAddress = "0x0635513f179D50A207757E05759CbD106d7dFcE8";
    private static final Web3LabsContract web3LabsContract = Web3LabsContract.load(web3LabsContractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
    private static final NameWrapper nameWrapper = NameWrapper.load(nameWrapperContractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
    private static final String web3LabsEns = "named.web3labs2.eth";


    public static void main(String[] args) throws Exception {

        byte[] helloWorldByteCode = Numeric.hexStringToByteArray("0x" + HelloWorld.BINARY);

        // 1. Use our ENS parent -> just give label and bytecode
        // 2. Use their own ENS parent -> give label, parentEns and bytecode
        //    Note: Transfer Ownership to our contract and then call setNameAndDeploy

        // First use case (using web3Labs domain)
        // web3LabsDomain(helloWorldByteCode, "v4");

        // second use case (using their own parent domain)
        // userDomain(helloWorldByteCode, "greeter", "enscribe.eth");

        // third use case (When contract is already deployed)
        // contractDeployed("0xF28789Dc7bFBAE61221f08196C5aF0016AA8bB62", "v2");
    }

    static void web3LabsDomain(byte[] contractByteCode, String label) throws Exception {
        final String parent = web3LabsEns;
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(contractByteCode, label, parent, parentNode, BigInteger.ONE).send();
        System.out.println("receipt = " + receipt);
    }

    static void userDomain(byte[] contractByteCode, String label, String parent) throws Exception {
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
//        BigInteger tokenId = new BigInteger(Numeric.toHexString(parentNode).substring(2), 16);
//        TransactionReceipt receiptTransfer = nameWrapper.safeTransferFrom(credentials.getAddress(), web3LabsContractAddress, tokenId, BigInteger.ONE, new byte[0]).send();
//        System.out.println("Transfer parent receipt = " + receiptTransfer);
        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(contractByteCode, label, parent, parentNode, BigInteger.ONE).send();
        System.out.println("receipt = " + receipt);
    }

    static void contractDeployed(String contractAddress, String label) throws Exception {
        final String parent = web3LabsEns;
        final byte[] parentNode = NameHash.nameHashAsBytes(parent);
        TransactionReceipt receipt =  web3LabsContract.setName(contractAddress, label, parent, parentNode).send();
        System.out.println("receipt = " + receipt);
    }
}
