// SPDX-License-Identifier: MIT
pragma solidity =0.8.24;

interface IExtendedResolver {
    function resolve(
        bytes memory name,
        bytes memory data
    ) external view returns (bytes memory);
}
