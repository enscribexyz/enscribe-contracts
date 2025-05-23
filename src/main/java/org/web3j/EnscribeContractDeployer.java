package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.generated.contracts.EnscribeBase;
import org.web3j.generated.contracts.EnscribeLineaSepolia;
import org.web3j.generated.contracts.EnscribeSepolia;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticEIP1559GasProvider;

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
public class EnscribeContractDeployer {
    static final String infuraAPI = getEnvVariable("RPC_URL");
    static final Web3j web3j = Web3j.build(new HttpService(infuraAPI));
    static final Credentials credentials = Credentials.create(getEnvVariable("PRIVATE_KEY"));
    static final BigInteger gasLimit = BigInteger.valueOf(2_000_000);
    static final BigInteger gasPrice;
    static {
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice().add(BigInteger.valueOf(1_000_000_000L));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
       System.out.println("Deploying Enscribe contract ...");
        BigInteger maxPriorityFeePerGas = web3j.ethMaxPriorityFeePerGas().send().getMaxPriorityFeePerGas();
        BigInteger baseFee =
                web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false)
                        .send()
                        .getBlock()
                        .getBaseFeePerGas();
        BigInteger maxFeePerGas = baseFee.multiply(BigInteger.valueOf(2)).add(maxPriorityFeePerGas);
       EnscribeBase enscribe = EnscribeBase.deploy(web3j, credentials, new StaticEIP1559GasProvider(84532L, maxFeePerGas, maxPriorityFeePerGas, gasLimit)).send();
       System.out.println("Contract address: " + enscribe.getContractAddress());
   }

   static String getEnvVariable(String key)  {
        try{
            // Load .env file from resources
            InputStream inputStream = EnscribeContractDeployer.class
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

