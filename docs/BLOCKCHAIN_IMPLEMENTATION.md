# Blockchain proof implementation

`ProofRegistry.sol` stores content hashes on Ethereum Sepolia. The backend uses `ProofService` to submit hashes asynchronously after generated content or uploaded documents have been saved. `onchain_proof` records submission status and the transaction hash.

## Configuration

The backend reads `RPC_URL`, `PRIVATE_KEY`, and `CONTRACT_ADDRESS` from its local environment. If any is empty, proof submission is disabled. Contract deployment has a separate `chain/.env` file with `RPC_URL` and `DEPLOYER_PRIVATE_KEY`. Copy the corresponding example files and supply newly issued credentials.

## Data path

1. A chat or upload request stores a database record.
2. The service computes a content hash and sends a contract transaction in a background task.
3. The transaction hash and final status are stored in `onchain_proof`; related records are updated when available.
4. `GET /api/v1/verify/tx/{tx_hash}` returns proof metadata. Public verification responses include a short content preview, so avoid placing sensitive prompts on a public chain workflow.

Apply `db/migrations/2025-12-19_add_onchain_proof.sql` after the base schema. Use a new Sepolia wallet: the old deployer key appeared in Git history and must never be reused.
