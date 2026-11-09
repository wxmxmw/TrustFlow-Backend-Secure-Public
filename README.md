# TrustFlow Backend

TrustFlow is a Spring Boot backend for wallet based authentication, retrieval augmented generation, image generation, and blockchain proof submission. The contract project in `chain/` targets Ethereum Sepolia.

## Stack

Java 21, Spring Boot 3.3.5, PostgreSQL with pgvector, Web3j, Hardhat, and the SiliconFlow API.

## Run locally

1. Create a PostgreSQL database and install the `vector` extension. Apply `db/init.sql`, then the migrations in `db/migrations/` in date order. Review the SQL before applying it to an existing database: `db/init.sql` contains a `DROP TABLE` statement.
2. Copy `.env.example` to `.env`. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (at least 32 characters), and `SILICONFLOW_API_KEY`. The Spring dotenv dependency loads the local file. Keep it untracked.
3. To submit blockchain proofs, also set `RPC_URL`, `PRIVATE_KEY`, and `CONTRACT_ADDRESS`. Leave all three empty to disable proof submission. Use a newly created, limited testnet wallet.
4. Run `mvn test` and `mvn spring-boot:run` with Java 21.

To deploy the contract, run `npm ci` in `chain/`, copy `chain/.env.example` to `chain/.env`, set a new RPC URL and deployer key, then run `npx hardhat run scripts/deploy.js --network sepolia`. Never commit either `.env` file.

## API

The API is under `/api/v1`. Authentication uses `POST /auth/nonce` and `POST /auth/login`; sign the nonce with a wallet using `personal_sign`. Protected endpoints require `Authorization: Bearer <token>`.

| Feature | Endpoint |
| --- | --- |
| Sessions | `GET/POST /sessions`, `GET /sessions/{id}/history` |
| Chat and generation | `POST /chat/completions`, `GET /chat/history/{sessionId}` |
| Documents | `GET /documents`, `POST /documents/upload`, `DELETE /documents/{id}`, `GET /documents/{id}/preview` |
| Verification | `POST /verify/check`, `GET /verify/tx/{tx_hash}` |

Document preview requires the owner's bearer token, including when opened from a citation link. Public verification responses contain a short content preview; do not treat an on-chain transaction hash as a private identifier.

## Before making a repository public

The previous Git history contains a wallet private key, API key, RPC provider key, JWT signing secret, server address, and uploaded files. Removing them from the latest files does **not** remove them from existing commits, forks, caches, or provider logs.

1. Revoke and replace the SiliconFlow key and RPC provider key. Move any Sepolia funds/assets controlled by the exposed private key to a new wallet; never reuse that wallet. Replace the JWT secret and invalidate issued tokens. Rotate the database password and restrict database network access.
2. Publish only a clean snapshot with no parent commits, or rewrite all Git refs and coordinate with every clone. Do not make the existing `main` history public. Check branches, tags, pull requests, release assets, and GitHub caches separately.
3. Run a secret scanner against the new snapshot and inspect all included binaries before publishing. Keep uploads, generated images, reports, and local environment files outside Git.
4. Add a license only after confirming that all contributors and third-party material can be redistributed.

The repository cleanup changes the working tree and index; it does not rotate credentials or remove remote history.
