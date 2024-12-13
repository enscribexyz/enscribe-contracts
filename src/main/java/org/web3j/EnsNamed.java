package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.ens.NameHash;
import org.web3j.generated.contracts.HelloWorld;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;

import static org.web3j.Web3LabsContractDeployer.credentials;
import static org.web3j.Web3LabsContractDeployer.gasLimit;
import static org.web3j.Web3LabsContractDeployer.gasPrice;
import static org.web3j.Web3LabsContractDeployer.getEnvVariable;
import static org.web3j.Web3LabsContractDeployer.infuraSepolia;
import static org.web3j.Web3LabsContractDeployer.web3j;

public class EnsNamed {
    private static final String web3LabsContractAddress = "0x49b66baecd21cc87a8300f70d917617a227b78b8";
    private static final Web3LabsContract web3LabsContract = Web3LabsContract.load(web3LabsContractAddress, web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
    private static final String parentEnsName = "testfinal2.web3labs2.eth";
    private static final String label = "v1";

    public static void main(String[] args) throws Exception {

        byte[] helloWorldByteCode = Numeric.hexStringToByteArray("0x" + HelloWorld.BINARY);
        byte[] parentNode = NameHash.nameHashAsBytes(parentEnsName);

//        // 1. Use our ENS parent -> setNameAndDeploy
//        // 2. Use their own ENS parent -> (setNameAndDeploy, parentEnsName)
//        //    Trasnfer Ownershhip to our contract and then call setNameAndDeploy

        TransactionReceipt receipt =  web3LabsContract.setNameAndDeploy(helloWorldByteCode, label, parentEnsName, parentNode).send();
    }
}
