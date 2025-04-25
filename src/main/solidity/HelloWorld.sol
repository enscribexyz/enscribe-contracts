// SPDX-License-Identifier: GPL-3.0
pragma solidity =0.8.24;

import "./utils/Ownable.sol";

contract HelloWorld is Ownable {
    string greetings = "default";

    constructor() Ownable (msg.sender) {}

    function retrieve() public view returns (string memory) {
        return greetings;
    }
}