package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import org.web3j.generated.contracts.HelloWorld;

/**
 * <p>This is the generated class for <code>web3j new helloworld</code></p>
 * <p>It deploys the Hello World contract in src/main/solidity/ and prints its address</p>
 * <p>For more information on how to run this project, please refer to our <a href="https://docs.web3j.io/latest/command_line_tools/#running-your-application">documentation</a></p>
 */
public class Web3App {
   private static final String infuraSepolia = "";

   public static void main(String[] args) throws Exception {
        Credentials credentials = Credentials.create("");
        Web3j web3j = Web3j.build(new HttpService(infuraSepolia));
        System.out.println("Deploying Web3 Labs contract ...");
        Web3LabsContract web3LabsContract = Web3LabsContract.deploy(web3j, credentials, new DefaultGasProvider()).send();
        System.out.println("Contract address: " + web3LabsContract.getContractAddress());
       }
}

