// SPDX-License-Identifier: GPL-3.0
pragma solidity =0.8.24;

contract HelloWorldNoOwnable {
    string greetings = "default";

    function set(string memory greet) public {
        greetings = greet;
    }

    function retrieve() public view returns (string memory) {
        return greetings;
    }
}