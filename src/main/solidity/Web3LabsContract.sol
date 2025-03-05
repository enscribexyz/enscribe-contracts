// SPDX-License-Identifier: MIT
pragma solidity =0.8.24;

import "./ReverseRegistrar.sol" as RR;
import "./NameWrapper.sol" as NW;
import "./PublicResolver.sol" as PR;
import "./openzeppelin/token/ERC1155/IERC1155Receiver.sol";
import "./openzeppelin/access/Ownable.sol";

contract Web3LabsContract is IERC1155Receiver, Ownable {

    address public constant REVERSE_REGISTRAR_ADDRESS = 0xCF75B92126B02C9811d8c632144288a3eb84afC8;
    address public constant NAME_WRAPPER_ADDRESS = 0x0635513f179D50A207757E05759CbD106d7dFcE8;
    address public constant PUBLIC_RESOLVER_ADDRESS = 0x8948458626811dd0c23EB25Cc74291247077cC51;

    event ContractDeployed(address contractAddress);
    event SubnameCreated(bytes32 parentHash, string label);
    event SetAddrSuccess(bytes32 subnameHash, bytes encodedAddress);
    event SetPrimaryNameSuccess(address deployedAddress, string subname);
    event ContractOwnershipTransferred(address deployedAddress, address owner);
    event NameOwnershipTransferred(uint256 parentTokenId, address owner);
    event EtherReceived(address sender, uint256 amount);

    /**
     * @notice Compute the address of a contract deployed via CREATE2
     * @param salt A user-defined value to influence the deterministic address
     * @param bytecode The bytecode of the contract to deploy
     * @return computedAddress The deterministic address of the contract
     */
    function computeAddress(uint256 salt, bytes memory bytecode) public view returns (address computedAddress) {
        bytes32 bytecodeHash = keccak256(bytecode);
        bytes32 saltEncoded = keccak256(abi.encodePacked(salt));
        computedAddress = address(
            uint160(uint256(keccak256(abi.encodePacked(
                bytes1(0xff),
                address(this),
                saltEncoded,
                bytecodeHash
            ))))
        );
    }

    // Function to be called when Deploy contract and set primary ENS name
    function setNameAndDeploy(bytes memory bytecode, string calldata label, string calldata parentName, bytes32 parentNode) public payable returns (address deployedAddress) {
        uint256 gasStart = gasleft(); // Capture initial gas amount

        bytes32 labelHash = keccak256(bytes(label));
        string memory subname = string(abi.encodePacked(label, ".", parentName));
        bytes32 node = keccak256(abi.encodePacked(parentNode, labelHash));
        uint256 salt = uint256(node);
        deployedAddress = computeAddress(salt, bytecode);

        require(_createSubname(parentNode, label, address(this), PUBLIC_RESOLVER_ADDRESS, uint64(0), uint32(0), uint64(0)), "Failed to create subname");
        emit SubnameCreated(parentNode, label);

        bytes memory encodedAddress = abi.encodePacked(deployedAddress);
        require(_setAddr(node, uint256(60), encodedAddress), "failed to setAddr");
        emit SetAddrSuccess(node, encodedAddress);

        _deploy(salt, bytecode);

        require(_setPrimaryNameForContract(deployedAddress, address(this), PUBLIC_RESOLVER_ADDRESS, subname), "failed to set primary name");
        emit SetPrimaryNameSuccess(deployedAddress, subname);

        _transferContractOwnership(deployedAddress, msg.sender);
        emit ContractOwnershipTransferred(deployedAddress, msg.sender);

        uint256 gasUsed = gasStart - gasleft(); // Calculate gas used
        uint256 estimatedTxCost = gasUsed * tx.gasprice; // Estimate transaction cost
        uint256 requiredEther = estimatedTxCost / 10; // 10% of estimated cost

        require(msg.value >= requiredEther, "Insufficient Ether: Send at least 10% of estimated tx cost");
        emit EtherReceived(msg.sender, msg.value);
    }

    // Function to be called when contract is already deployed and just set primary ENS name
    function setName(address contractAddress, string calldata label, string calldata parentName, bytes32 parentNode) public returns (bool success) {
        bytes32 labelHash = keccak256(bytes(label));
        string memory subname = string(abi.encodePacked(label, ".", parentName));
        bytes32 node = keccak256(abi.encodePacked(parentNode, labelHash));
        success = false;
        _checkOwnership(contractAddress);

        require(_createSubname(parentNode, label, address(this), PUBLIC_RESOLVER_ADDRESS, uint64(0), uint32(0), uint64(0)), "Failed to create subname");
        emit SubnameCreated(parentNode, label);

        bytes memory encodedAddress = abi.encodePacked(contractAddress);
        require(_setAddr(node, uint256(60), encodedAddress), "failed to setAddr");
        emit SetAddrSuccess(node, encodedAddress);

        require(_setPrimaryNameForContract(contractAddress, address(this), PUBLIC_RESOLVER_ADDRESS, subname), "failed to set primary name");
        emit SetPrimaryNameSuccess(contractAddress, subname);

        _transferContractOwnership(contractAddress, msg.sender);
        emit ContractOwnershipTransferred(contractAddress, msg.sender);

        success = true;
    }

    /**
     * @dev Allows contract owner to withdraw received Ether.
     */
    function withdraw() external onlyOwner {
        payable(owner()).transfer(address(this).balance);
    }

    /**
     * @dev Fallback function to accept Ether.
     */
    receive() external payable {
        emit EtherReceived(msg.sender, msg.value);
    }

    /**
     * @dev Fallback function to accept calls without data.
     */
    fallback() external payable {
        emit EtherReceived(msg.sender, msg.value);
    }

    function _deploy(uint256 salt, bytes memory bytecode) private returns (address deployedAddress) {
        require(bytecode.length != 0, "Bytecode cannot be empty");
        bytes32 saltEncoded = keccak256(abi.encodePacked(salt));
        assembly {
            deployedAddress := create2(0, add(bytecode, 0x20), mload(bytecode), saltEncoded)
        }

        require(deployedAddress != address(0), "Contract deployment failed");
        emit ContractDeployed(deployedAddress);
    }

    function _transferContractOwnership(address contractAddress, address owner) private {
        (bool success, ) = contractAddress.call(
            abi.encodeWithSignature("transferOwnership(address)", owner)
        );
        require(success, "Ownership transfer failed");
    }

    function _setPrimaryNameForContract(address contractAddr, address owner, address resolver, string memory subName) private returns (bool success) {
        RR.ReverseRegistrar reverseRegistrar = RR.ReverseRegistrar(REVERSE_REGISTRAR_ADDRESS);
        try reverseRegistrar.setNameForAddr(contractAddr, owner, resolver, subName) {
            success = true;
        } catch {
            success = false;
        }
    }

    function _createSubname(
        bytes32 parentNode,
        string calldata label,
        address owner,
        address resolver,
        uint64 ttl,
        uint32 fuses,
        uint64 expiry
    ) private returns (bool success) {
        NW.NameWrapper nameWrapper = NW.NameWrapper(NAME_WRAPPER_ADDRESS);
        try nameWrapper.setSubnodeRecord(parentNode, label, owner, resolver, ttl, fuses, expiry) {
            success = true;
        } catch {
            success = false;
        }
    }

    function _setAddr(
        bytes32 node, uint256 coinType, bytes memory a
    ) private returns (bool success) {
        PR.PublicResolver publicResolverContract = PR.PublicResolver(PUBLIC_RESOLVER_ADDRESS);
        try publicResolverContract.setAddr(node, coinType, a) {
            success = true;
        } catch {
            success=false;
        }
    }

    function _checkOwnership(address contractAddress) private {
        (bool success, bytes memory data) = contractAddress.call(
            abi.encodeWithSignature("owner()")
        );
        require(success, "Failed to call owner() function");

        address owner;
        if (data.length > 0) {
            owner = abi.decode(data, (address));
        }

        require(owner == address(this), "Web3LabsContract is not set as an owner, can't proceed");
    }

    /**
     * @dev Handles the receipt of a single ERC1155 token type.
     * This function is called at the end of a `safeTransferFrom` after the balance has been updated.
     * @return bytes4 Returns `IERC1155Receiver.onERC1155Received.selector` if the transfer is allowed.
     */
    function onERC1155Received(
        address operator,
        address from,
        uint256 id,
        uint256 value,
        bytes calldata data
    ) external pure override returns (bytes4) {
        // Accept the transfer
        return IERC1155Receiver.onERC1155Received.selector;
    }

    /**
     * @dev Handles the receipt of multiple ERC1155 token types.
     * This function is called at the end of a `safeBatchTransferFrom` after the balances have been updated.
     * @return bytes4 Returns `IERC1155Receiver.onERC1155BatchReceived.selector` if the transfer is allowed.
     */
    function onERC1155BatchReceived(
        address operator,
        address from,
        uint256[] calldata ids,
        uint256[] calldata values,
        bytes calldata data
    ) external pure override returns (bytes4) {
        // Accept the transfer
        return IERC1155Receiver.onERC1155BatchReceived.selector;
    }

    /**
     * @dev See {IERC165-supportsInterface}.
     */
    function supportsInterface(bytes4 interfaceId) external pure override returns (bool) {
        return interfaceId == type(IERC1155Receiver).interfaceId || interfaceId == type(IERC165).interfaceId;
    }
}