package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticEIP1559GasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.HashMap;
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
        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFee =
                web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                        .send()
                        .getBlock()
                        .getBaseFeePerGas();
        BigInteger maxFeePerGas = baseFee.multiply(BigInteger.valueOf(2)).add(maxPriorityFeePerGas);
       Web3LabsContract web3LabsContract = Web3LabsContract.deploy(web3j, credentials, new StaticEIP1559GasProvider(11155111L, maxFeePerGas, maxPriorityFeePerGas, gasLimit)).send();
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

}

