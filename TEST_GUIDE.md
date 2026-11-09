# Integration test guide

## Setup

Start the backend with the configuration in `README.md`. Create a new local test PDF; do not commit uploaded documents. Use a test wallet, not the wallet whose key appeared in old commits.

## Wallet login

1. Call `POST /api/v1/auth/nonce` with JSON `{ "wallet_address": "0x..." }`.
2. Open `sign.html` from a local HTTP server, connect MetaMask, enter the nonce, and sign it.
3. Call `POST /api/v1/auth/login` with `wallet_address`, `nonce`, and `signature`. Save the returned `access_token` locally.
4. Send `Authorization: Bearer <access_token>` on protected requests.

## Proof flow

1. Create a session with `POST /api/v1/sessions`.
2. Send a text or image request to `POST /api/v1/chat/completions` with `session_id`, `prompt`, `model`, and `mode` (`text` or `image`).
3. Upload a document with multipart form field `file` to `POST /api/v1/documents/upload`.
4. The API may return before a blockchain transaction is confirmed. Query `generation_records`, `documents`, and `onchain_proof` to follow the asynchronous status. A missing `tx_hash` can indicate pending work or a failed submission.
5. Test missing or invalid tokens and a second user's session/document IDs. They must not reveal chat history or document bytes.

The PowerShell scripts in the repository are local examples. Supply your own wallet address and test file; avoid printing tokens in logs.
