package org.web3j;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.ens.NameHash;
import org.web3j.generated.contracts.HelloWorld;
import org.web3j.generated.contracts.NameWrapper;
import org.web3j.generated.contracts.ReverseRegistrar;
import org.web3j.generated.contracts.Web3LabsContract;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.web3j.Web3LabsContractDeployer.getEnvVariable;

public class EnsContractNaming {
    private static final String infuraSepolia = getEnvVariable("RPC_URL");
    public static final Event TRANSFER_SINGLE_EVENT = new Event(
            "TransferSingle",
            Arrays.asList(
                    TypeReference.create(Address.class, true),  // operator (indexed)
                    TypeReference.create(Address.class, true),  // from (indexed)
                    TypeReference.create(Address.class, true),  // to (indexed)
                    TypeReference.create(Uint256.class),        // id
                    TypeReference.create(Uint256.class)         // value
            )
    );

    private static final Web3j web3j = Web3j.build(new HttpService(infuraSepolia));
    private static final Credentials credentials = Credentials.create(getEnvVariable("PRIVATE_KEY"));
    private static final NameWrapper nameWrapper = NameWrapper.load("0x0635513f179D50A207757E05759CbD106d7dFcE8", web3j, credentials, new StaticGasProvider(BigInteger.valueOf(10_100_000_000L), BigInteger.valueOf(9_000_000)));
    private static final Web3LabsContract web3LabsContract = Web3LabsContract.load("0x3fb589e1dd2b51cee708a4b490f392a2f8afc121", web3j, credentials, new DefaultGasProvider());
    private static final ReverseRegistrar reverseRegistrar = ReverseRegistrar.load("0xCF75B92126B02C9811d8c632144288a3eb84afC8", web3j, credentials, new DefaultGasProvider());
    private static final String parentEnsName = "web3labs.eth";

    public static void main(String[] args) throws Exception {

        // Set Contract Bytecode, Salt, and subname label
        byte[] helloWorldByteCode = HelloWorld.BINARY.getBytes();
        BigInteger salt = BigInteger.valueOf(1234);
        String label = "test";
        String resolver = "0x8948458626811dd0c23EB25Cc74291247077cC51";

        // Compute Deploy address for the contract
        String deployAddress = getComputeAddress(salt, helloWorldByteCode);

        // Create new subname
        TransactionReceipt receiptSubnodeRecord = setSubnodeRecord(label, resolver);
        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                receiptSubnodeRecord.getLogs().get(3).getData(),
                TRANSFER_SINGLE_EVENT.getNonIndexedParameters()
        );
        BigInteger id = new BigInteger(nonIndexedValues.get(0).getValue().toString());

        // Transfer subname to the contract deploy address
        transferSubname(deployAddress, id);

        // Deploy contract using Web3 Labs contract
        deployContract(salt, helloWorldByteCode);

        // Set Primary Name for contract deployed, needs to Ownable or reverseClaimer
        String subname = label + parentEnsName;
        setPrimaryNameWhenOwnable(deployAddress, resolver, subname);

    }

    static String getComputeAddress(BigInteger salt, byte[] helloWorldByteCode) throws Exception {
        String deployAddress = web3LabsContract.computeAddress(salt, helloWorldByteCode).send();
        System.out.println("Computed Deploy Address = " + deployAddress);
        return deployAddress;
    }

    static TransactionReceipt setSubnodeRecord (String label, String resolver) throws Exception {

        BigInteger ttl = BigInteger.ZERO;
        BigInteger fuses = BigInteger.valueOf(4);
        BigInteger expiry = BigInteger.ZERO;
        byte[] parentNode = Numeric.hexStringToByteArray(NameHash.nameHash(parentEnsName));
        System.out.println("Name hash calculated = " + NameHash.nameHash(parentEnsName));

        TransactionReceipt receiptSubnodeRecord = nameWrapper.setSubnodeRecord(parentNode, label, credentials.getAddress(), resolver, ttl, fuses, expiry).send();
        System.out.println(receiptSubnodeRecord);
        return receiptSubnodeRecord;
    }

    static TransactionReceipt transferSubname(String deployAddress, BigInteger id) throws Exception {
        TransactionReceipt receiptTransfer = nameWrapper.safeTransferFrom(credentials.getAddress(), deployAddress, id, BigInteger.ONE, null).send();
        System.out.println(receiptTransfer);
        return receiptTransfer;
    }

    static TransactionReceipt deployContract(BigInteger salt, byte[] helloWorldByteCode) throws Exception {
        TransactionReceipt receiptDeployContrat = web3LabsContract.deploy(salt, helloWorldByteCode).send();
        System.out.println(receiptDeployContrat);
        return receiptDeployContrat;
    }

    static TransactionReceipt setPrimaryNameWhenOwnable (String deployAddress, String resolver, String subname) throws Exception {
        TransactionReceipt receiptSetPrimaryName = web3LabsContract.setPrimaryNameForContract(deployAddress, credentials.getAddress(), resolver, subname).send();
        System.out.println(receiptSetPrimaryName);
        return receiptSetPrimaryName;
    }
}
