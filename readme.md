- Add .env file inside src/main/resources/ with 

```text
PRIVATE_KEY=  # Priv key of account who is the owner of parent ENS node
RPC_URL=  # Sepolia RPC endpoint
```
- Deploy Web3LabsContract by running main function inside Web3LabsContractDeployer if not already deployed


- Inside EnsContractNaming Class update web3LabsContractAddress

```java
private static final String web3LabsContractAddress = "<Web3LabsContract address>";
```
- Inside main function of EnsContractNaming Class update label and salt

```java
BigInteger salt = BigInteger.valueOf("<rnadom integer>"); // change for every run
String label = "<any label>"; // change for every run
```
- Run the main function of EnsContractNaming