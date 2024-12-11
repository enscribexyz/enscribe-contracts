package org.web3j;

import org.web3j.crypto.Credentials;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>This is the generated class for <code>web3j new helloworld</code></p>
 * <p>It deploys the Hello World contract in src/main/solidity/ and prints its address</p>
 * <p>For more information on how to run this project, please refer to our <a href="https://docs.web3j.io/latest/command_line_tools/#running-your-application">documentation</a></p>
 */
public class Web3LabsContractDeployer {
   private static final String infuraSepolia = getEnvVariable("RPC_URL");

   public static void main(String[] args) throws Exception {
        Credentials credentials = Credentials.create(getEnvVariable("PRIVATE_KEY"));
        Web3j web3j = Web3j.build(new HttpService(infuraSepolia));
        System.out.println("Deploying Web3 Labs contract ...");
        Web3LabsContract web3LabsContract = Web3LabsContract.deploy(web3j, credentials, new DefaultGasProvider()).send();
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

