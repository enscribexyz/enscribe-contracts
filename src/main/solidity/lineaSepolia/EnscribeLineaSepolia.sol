// SPDX-License-Identifier: MIT
pragma solidity =0.8.24;

import "../ens/ReverseRegistrar.sol" as RR;
import "../ens/NameWrapper.sol" as NW;
import "../ens/ENSRegistry.sol" as ER;
import "../ens/PublicResolver.sol" as PR;
import "../openzeppelin/token/ERC1155/IERC1155Receiver.sol";
import "../openzeppelin/access/Ownable.sol";

contract EnscribeLineaSepolia is Ownable, IERC1155Receiver {

    address public constant REVERSE_REGISTRAR_ADDRESS = 0x4aAA964D8EB65508ca3DA3b0A3C060c16059E613;
    address public constant ENS_REGISTRY_ADDRESS = 0x5B2636F0f2137B4aE722C01dd5122D7d3e9541f7;
    address public constant PUBLIC_RESOLVER_ADDRESS = 0xA2008916Ed2d7ED0Ecd747a8a5309267e42cf1f1;
    address public constant NAME_WRAPPER_ADDRESS = 0xF127De9E039a789806fEd4C6b1C0f3aFfeA9425e;

    uint256 public pricing = 0.0001 ether;
    string public defaultParent = "repo.enscribe.linea-sepolia.eth";

    event ContractDeployed(address contractAddress);
    event SubnameCreated(bytes32 parentHash, string label);
    event SetAddrSuccess(address indexed contractAddress, string subname);
    event SetPrimaryNameSuccess(address indexed deployedAddress, string subname);
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
        bytes32 labelHash = keccak256(bytes(label));
        string memory subname = string(abi.encodePacked(label, ".", parentName));
        bytes32 node = keccak256(abi.encodePacked(parentNode, labelHash));
        uint256 salt = uint256(node);
        deployedAddress = computeAddress(salt, bytecode);

        _deploy(salt, bytecode);

        require(setName(deployedAddress, label, parentName, parentNode), "Failed to create subname and set forward resolution");

        require(_setPrimaryNameForContract(deployedAddress, address(this), PUBLIC_RESOLVER_ADDRESS, subname), "failed to set primary name");
        emit SetPrimaryNameSuccess(deployedAddress, subname);

        _transferContractOwnership(deployedAddress, msg.sender);
        emit ContractOwnershipTransferred(deployedAddress, msg.sender);

        require(msg.value >= pricing, "Insufficient Ether Sent: Check the pricing");
        emit EtherReceived(msg.sender, msg.value);
    }

    // Function to be called when contract is already deployed and just set forward resolve ENS name
    function setName(address contractAddress, string calldata label, string calldata parentName, bytes32 parentNode) public payable returns (bool success) {
        bytes32 labelHash = keccak256(bytes(label));
        bytes32 node = keccak256(abi.encodePacked(parentNode, labelHash));
        string memory subname = string(abi.encodePacked(label, ".", parentName));
        success = false;

        if (checkWrapped(parentNode)) {
            if (keccak256(abi.encodePacked(parentName)) != keccak256(abi.encodePacked(defaultParent))) {
                require(_isSenderOwnerWrapped(parentNode), "Sender is not the owner of Wrapped parent node, can't create subname");
            }
            require(_createSubnameWrapped(parentNode, label, address(this), PUBLIC_RESOLVER_ADDRESS, uint64(0), uint32(0), uint64(0)), "Failed to create subname, check if contract is given isApprovedForAll role for Wrapped Name");
        } else {
            if (keccak256(abi.encodePacked(parentName)) != keccak256(abi.encodePacked(defaultParent))) {
                require(_isSenderOwnerUnwrapped(parentNode), "Sender is not the owner of Unwrapped parent node, can't create subname");
            }
            require(_createSubnameUnwrapped(parentNode, labelHash, address(this), PUBLIC_RESOLVER_ADDRESS, uint64(0)), "Failed to create subname, check if contract is given isApprovedForAll role for Unwrapped Name");
        }
        emit SubnameCreated(parentNode, label);

        bytes memory encodedAddress = abi.encodePacked(contractAddress);
        require(_setAddr(node, uint256(60), encodedAddress), "failed to setAddr");
        emit SetAddrSuccess(contractAddress, subname);

        require(msg.value >= pricing, "Insufficient Ether Sent: Check the pricing");
        emit EtherReceived(msg.sender, msg.value);

        success = true;
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

    function _createSubnameUnwrapped(
        bytes32 parentNode,
        bytes32 labelHash,
        address owner,
        address resolver,
        uint64 ttl
    ) private returns (bool success) {
        ER.ENSRegistry ensRegistry = ER.ENSRegistry(ENS_REGISTRY_ADDRESS);
        try ensRegistry.setSubnodeRecord(parentNode, labelHash, owner, resolver, ttl) {
            success = true;
        } catch {
            success = false;
        }
    }

    function _createSubnameWrapped(
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

    function checkWrapped(bytes32 parentNode) public view returns (bool) {
        NW.NameWrapper nameWrapper = NW.NameWrapper(NAME_WRAPPER_ADDRESS);
        try nameWrapper.isWrapped(parentNode) returns (bool wrapped) {
            return wrapped;
        } catch {
            return false;
        }
    }

    function _isSenderOwnerWrapped(bytes32 parentNode) private view returns (bool) {
        return NW.NameWrapper(NAME_WRAPPER_ADDRESS).ownerOf(uint256(parentNode)) == msg.sender;
    }

    function _isSenderOwnerUnwrapped(bytes32 parentNode) private view returns (bool) {
        return ER.ENSRegistry(ENS_REGISTRY_ADDRESS).owner(parentNode) == msg.sender;
    }

    function updatePricing(uint256 updatedPrice) public onlyOwner {
        require(updatedPrice > 0, "Price must be greater than zero");
        pricing = updatedPrice;
    }

    function updateDefaultParent(string calldata updatedParent) public onlyOwner {
        defaultParent = updatedParent;
    }

    /**
     * @dev To withdraw received Ether.
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