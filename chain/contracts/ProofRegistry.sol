// SPDX-License-Identifier: MIT
pragma solidity ^0.8.17;

contract ProofRegistry {
    struct Proof {
        address uploader;
        uint256 timestamp;
    }

    // hash => Proof
    mapping(bytes32 => Proof) public proofs;

    event ProofStored(bytes32 indexed hash, address indexed uploader, uint256 timestamp);

    function storeProof(bytes32 hash) external {
        require(proofs[hash].timestamp == 0, "Proof already exists");

        proofs[hash] = Proof({
            uploader: msg.sender,
            timestamp: block.timestamp
        });

        emit ProofStored(hash, msg.sender, block.timestamp);
    }

    function verifyProof(bytes32 hash) external view returns (address uploader, uint256 timestamp) {
        Proof memory p = proofs[hash];
        require(p.timestamp != 0, "Proof not found");
        return (p.uploader, p.timestamp);
    }
}
