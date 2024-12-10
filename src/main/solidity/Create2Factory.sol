// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract Create2Factory {
    event ContractDeployed(address contractAddress);

    /**
     * @notice Deploy a new contract using CREATE2
     * @param salt A user-defined value to influence the deterministic address
     * @param bytecode The bytecode of the contract to deploy
     * @return deployedAddress The address of the deployed contract
     */
    function deploy(uint256 salt, bytes memory bytecode) public returns (address deployedAddress) {
        require(bytecode.length != 0, "Bytecode cannot be empty");
        bytes32 saltEncoded = keccak256(abi.encodePacked(salt));
        assembly {
            deployedAddress := create2(0, add(bytecode, 0x20), mload(bytecode), saltEncoded)
        }

        require(deployedAddress != address(0), "Contract deployment failed");

        emit ContractDeployed(deployedAddress);
    }

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
}