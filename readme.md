- Add .env file inside src/main/resources/ with 

```text
PRIVATE_KEY=  # Priv key of account who is the owner of parent ENS node
RPC_URL=  # Sepolia RPC endpoint
```
- Deploy Web3LabsContract by running main function inside Web3LabsContractDeployer if not already deployed


- Inside EnsNamed Class update args of any one of the function call you want to run

```java
web3LabsDomain(helloWorldByteCode, "v1");
userDomain(helloWorldByteCode, "v1", "test6.web3labs2.eth");
```

- Run the main function of EnsNamed