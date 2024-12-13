package org.web3j;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.ens.EnsResolver;
import org.web3j.ens.NameHash;
import org.web3j.generated.contracts.ETHRegistrarController;
import org.web3j.generated.contracts.NameWrapper;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>This is the generated class for <code>web3j new helloworld</code></p>
 * <p>It deploys the Hello World contract in src/main/solidity/ and prints its address</p>
 * <p>For more information on how to run this project, please refer to our <a href="https://docs.web3j.io/latest/command_line_tools/#running-your-application">documentation</a></p>
 */
public class Web3LabsContractDeployer {
    static final String infuraSepolia = getEnvVariable("RPC_URL");
    static final Web3j web3j = Web3j.build(new HttpService(infuraSepolia));
    static final Credentials credentials = Credentials.create(getEnvVariable("PRIVATE_KEY"));
    static final BigInteger gasLimit = BigInteger.valueOf(10_000_000);
    static final BigInteger gasPrice;
    static {
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice().add(BigInteger.valueOf(1_000_000_000L));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
       System.out.println("Deploying Web3 Labs contract ...");
       Web3LabsContract web3LabsContract = Web3LabsContract.deploy(web3j, credentials, new StaticGasProvider(gasPrice, gasLimit)).send();
       System.out.println("Contract address: " + web3LabsContract.getContractAddress());

   }

   static String getEnvVariable(String key)  {
        try{
            // Load .env file from resources
            InputStream inputStream = Web3LabsContractDeployer.class
                    .getClassLoader()
                    .getResourceAsStream(".env");

            if (inputStream == null) {
                throw new RuntimeException(".env file not found in resources");
            }

            // Parse .env file
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Map<String, String> envVariables = new HashMap<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    envVariables.put(parts[0].trim(), parts[1].trim());
                }
            }
            // Get key
            return envVariables.get(key);
        } catch (Exception e) {
            throw new RuntimeException("Env Key not found in .env File - " + e);
        }
   }

//    static final ETHRegistrarController ethRegistrarController = ETHRegistrarController.load("0xa9283239537a72f172a99ca9ed85c12cc8458b29", web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
//    static final NameWrapper nameWrapper = NameWrapper.load("0x50dA5fd0fc8233a25FF87Fd5339cFCE930785f07", web3j, credentials, new StaticGasProvider(gasPrice, gasLimit));
//        String label = "testing";
//        String parentName = "testing.eth";
//       byte[] commitment = ethRegistrarController.makeCommitment(label, credentials.getAddress(), BigInteger.valueOf(31536000),
//               Numeric.hexStringToByteArray("0x8afb221c262ffdde4da3c7dbedbf4768fb1da38505129c5cf51224e56af6fdc1"),
//               "0x4c46a5b0db2e97d9dae693fede8b2050f5afdc91", Collections.emptyList(), false, BigInteger.ZERO).send();
//
//       ethRegistrarController.commit(commitment).send();
//
//       Thread.sleep(60000);
//       TransactionReceipt receipt = ethRegistrarController.register(label, credentials.getAddress(), BigInteger.valueOf(31536000),
//               Numeric.hexStringToByteArray("0x8afb221c262ffdde4da3c7dbedbf4768fb1da38505129c5cf51224e56af6fdc1"),
//               "0x4c46a5b0db2e97d9dae693fede8b2050f5afdc91", Collections.emptyList(), false, BigInteger.ZERO, BigInteger.valueOf(3187500000003559L)).send();
//
//        String nameHashHex = NameHash.nameHash(name); // Example: "0x1234abcd..."
//        BigInteger tokenId = new BigInteger(nameHashHex.substring(2), 16); // Remove "0x" and parse as base-16
//
//        System.out.println("Namehash (hex): " + nameHashHex);
//        System.out.println("Namehash (decimal): " + tokenId);

//        String subLabel = "v1";
//
//        nameWrapper.setSubnodeRecord(NameHash.nameHashAsBytes(parentName), subLabel, credentials.getAddress(), "0x4c46a5b0db2e97d9dae693fede8b2050f5afdc91", BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO).send();

//        String subname = subLabel + "." + parentName;
//        String nameHashHex = NameHash.nameHash(subname);
//        BigInteger tokenId = new BigInteger(nameHashHex.substring(2), 16);
//
//        nameWrapper.safeTransferFrom(credentials.getAddress(), "0x094cf897a2a79a6208f102774904657b8edf1ca7", tokenId, BigInteger.ONE, new byte[0]).send();
//       EnsResolver ensResolver = new EnsResolver(web3j);
//       System.out.println("ENS address = " + ensResolver.resolve("web3labs.eth"));
}

