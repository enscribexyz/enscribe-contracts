// SPDX-License-Identifier: GPL-3.0
pragma solidity ^0.8.0;

import "./Ownable.sol";

contract HelloWorld is Ownable {
    string greetings = "default";

    constructor() Ownable () {}

    function set(string memory greet) public {
        greetings = greet;
    }

    function retrieve() public view returns (string memory) {
        return greetings;
    }
}