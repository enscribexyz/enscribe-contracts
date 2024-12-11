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
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.web3j.Web3LabsContractDeployer.getEnvVariable;

public class EnsContractNaming {
    private static final String infuraSepolia = getEnvVariable("RPC_URL");
    private static final String nameWrapperAddress = "0x0635513f179D50A207757E05759CbD106d7dFcE8";
    private static final String web3LabsContractAddress = "0x3fb589e1dd2b51cee708a4b490f392a2f8afc121";
    private static final Web3j web3j = Web3j.build(new HttpService(infuraSepolia));
    private static final Credentials credentials = Credentials.create(getEnvVariable("PRIVATE_KEY"));
    private static final BigInteger gasPrice;
    static {
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice().add(BigInteger.valueOf(3_000_000_000L));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static final NameWrapper nameWrapper = NameWrapper.load(nameWrapperAddress, web3j, credentials, new StaticGasProvider(gasPrice, BigInteger.valueOf(500_000)));
    private static final Web3LabsContract web3LabsContract = Web3LabsContract.load(web3LabsContractAddress, web3j, credentials, new StaticGasProvider(gasPrice, BigInteger.valueOf(500_000)));
    private static final String parentEnsName = "web3labs2.eth";

    public static void main(String[] args) throws Exception {

        // Set Contract Bytecode, Salt, and subname label
        byte[] helloWorldByteCode = Numeric.hexStringToByteArray("0x" + HelloWorld.BINARY);
        BigInteger salt = BigInteger.valueOf(12345);
        String label = "test4";
        String resolver = "0x8948458626811dd0c23EB25Cc74291247077cC51";

        // Compute Deploy address for the contract
        String deployAddress = getComputeAddress(salt, helloWorldByteCode);

        //  Create new subname
        TransactionReceipt receiptSubnodeRecord = setSubnodeRecord(label, resolver);
        BigInteger id = getTokenId(receiptSubnodeRecord);

        // Transfer subname to the contract deploy address
        transferSubname(deployAddress, id);

        // Deploy contract using Web3 Labs contract
        deployContract(salt, helloWorldByteCode);

        // Set Primary Name for contract deployed, needs to Ownable or reverseClaimer
        String subname = label + "." + parentEnsName;
        setPrimaryNameWhenOwnable(deployAddress, resolver, subname);

    }

    static String getComputeAddress(BigInteger salt, byte[] helloWorldByteCode) throws Exception {
        String deployAddress = web3LabsContract.computeAddress(salt, helloWorldByteCode).send();
        System.out.println("Computed Deploy Address = " + deployAddress);
        return deployAddress;
    }

    static TransactionReceipt setSubnodeRecord (String label, String resolver) throws Exception {

        BigInteger ttl = BigInteger.ZERO;
        BigInteger fuses = BigInteger.ZERO;
        BigInteger expiry = BigInteger.ZERO;
        byte[] parentNode = Numeric.hexStringToByteArray(NameHash.nameHash(parentEnsName));

        TransactionReceipt receiptSubnodeRecord = nameWrapper.setSubnodeRecord(parentNode, label, credentials.getAddress(), resolver, ttl, fuses, expiry).send();
        System.out.println("receiptSubnodeRecord = " + receiptSubnodeRecord);
        return receiptSubnodeRecord;

//        Function function = new Function(
//                "SetSubnodeRecord",
//                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Bytes32(parentNode),
//                        new org.web3j.abi.datatypes.Utf8String(label),
//                        new org.web3j.abi.datatypes.Address(160, credentials.getAddress()),
//                        new org.web3j.abi.datatypes.Address(160, resolver),
//                        new org.web3j.abi.datatypes.generated.Uint64(ttl),
//                        new org.web3j.abi.datatypes.generated.Uint32(fuses),
//                        new org.web3j.abi.datatypes.generated.Uint64(expiry)),
//                Collections.<TypeReference<?>>emptyList());
//
//        String encodedFunction = FunctionEncoder.encode(function);
//
//        BigInteger maxPriorityFeePerGas = BigInteger.valueOf(2_000_000_000L); // 2 Gwei
//        BigInteger maxFeePerGas = BigInteger.valueOf(20_000_000_000L);
//
//        RawTransaction rawTransaction = RawTransaction.createTransaction(
//                11155111L,
//                web3j.ethGetTransactionCount(credentials.getAddress(), LATEST).send().getTransactionCount(),
//                BigInteger.valueOf(500_000),
//                "0x0635513f179D50A207757E05759CbD106d7dFcE8",
//                BigInteger.ZERO,
//                encodedFunction,
//                maxPriorityFeePerGas,
//                maxFeePerGas
//        );
//        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, 11155111L, credentials);
//        String hexValue = Numeric.toHexString(signedMessage);
//        EthSendTransaction response = web3j.ethSendRawTransaction(hexValue).send();
//
    }

    static BigInteger getTokenId(TransactionReceipt receiptSubnodeRecord) {
        final Event TRANSFER_SINGLE_EVENT = new Event(
                "TransferSingle",
                Arrays.asList(
                        TypeReference.create(Address.class, true),  // operator (indexed)
                        TypeReference.create(Address.class, true),  // from (indexed)
                        TypeReference.create(Address.class, true),  // to (indexed)
                        TypeReference.create(Uint256.class),        // id
                        TypeReference.create(Uint256.class)         // value
                )
        );
        Log firstLog = receiptSubnodeRecord.getLogs().stream()
                .filter(log -> log.getAddress().toLowerCase().equalsIgnoreCase(nameWrapperAddress))
                .findFirst().get();

        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                firstLog.getData(),
                TRANSFER_SINGLE_EVENT.getNonIndexedParameters()
        );
        BigInteger id = new BigInteger(nonIndexedValues.get(0).getValue().toString());
        System.out.println("Token ID = " + id);
        return id;
    }
    static TransactionReceipt transferSubname(String deployAddress, BigInteger id) throws Exception {
        TransactionReceipt receiptTransfer = nameWrapper.safeTransferFrom(credentials.getAddress(), deployAddress, id, BigInteger.ONE, new byte[0]).send();
        System.out.println("receiptTransfer of name = " + receiptTransfer);
        return receiptTransfer;
    }

    static TransactionReceipt deployContract(BigInteger salt, byte[] helloWorldByteCode) throws Exception {
        TransactionReceipt receiptDeployContrat = web3LabsContract.deploy(salt, helloWorldByteCode).send();
        System.out.println("receiptDeployContrat = " + receiptDeployContrat);
        return receiptDeployContrat;
    }

    static TransactionReceipt setPrimaryNameWhenOwnable (String deployAddress, String resolver, String subname) throws Exception {
        TransactionReceipt receiptSetPrimaryName = web3LabsContract.setPrimaryNameForContract(deployAddress, credentials.getAddress(), resolver, subname).send();
        System.out.println("receiptSetPrimaryName = " + receiptSetPrimaryName);
        return receiptSetPrimaryName;
    }
}
