package org.web3j;

import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.ens.NameHash;
import org.web3j.generated.contracts.Create2Factory;
import org.web3j.generated.contracts.HelloWorld;
import org.web3j.generated.contracts.NameWrapper;
import org.web3j.generated.contracts.ReverseRegistrar;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class EnsContractNaming {
    private static final String infuraSepolia = "";
    private static final String infuraMainnet = "";

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

    private static final Web3j web3j = Web3j.build(new HttpService(infuraMainnet));

    public static void main(String[] args) throws Exception {
        Credentials credentials = Credentials.create("");
        NameWrapper nameWrapper = NameWrapper.load("",web3j, credentials, new DefaultGasProvider());
        Create2Factory create2Factory = Create2Factory.load("",web3j, credentials, new DefaultGasProvider());
        ReverseRegistrar reverseRegistrar = ReverseRegistrar.load("",web3j, credentials, new DefaultGasProvider());

        String helloWorldByteCode = HelloWorld.BINARY;
        BigInteger salt = BigInteger.valueOf(1234);

        String deployAddress = create2Factory.computeAddress(salt, helloWorldByteCode.getBytes()).send();

        String label = "";
        String resolver = "";
        BigInteger ttl = BigInteger.ZERO;
        BigInteger fuses = BigInteger.valueOf(4);
        BigInteger expiry = BigInteger.ZERO;

        byte[] parentNode = NameHash.nameHash("web3labs.eth").getBytes();

        TransactionReceipt receiptSubnodeRecord = nameWrapper.setSubnodeRecord(parentNode, label, credentials.getAddress(), resolver, ttl, fuses, expiry).send();
        System.out.println(receiptSubnodeRecord);

        List<Type> nonIndexedValues = FunctionReturnDecoder.decode(
                receiptSubnodeRecord.getLogs().get(3).getData(),
                TRANSFER_SINGLE_EVENT.getNonIndexedParameters()
        );

        BigInteger id = new BigInteger(nonIndexedValues.get(0).getValue().toString());


        TransactionReceipt receiptTransfer = nameWrapper.safeTransferFrom(credentials.getAddress(), deployAddress, id, BigInteger.ONE, null).send();
        System.out.println(receiptTransfer);

        String subname = "";
        TransactionReceipt receiptSetPrimaryName = reverseRegistrar.setNameForAddr(deployAddress, credentials.getAddress(), resolver, subname).send();
        System.out.println(receiptSetPrimaryName);


    }
}
