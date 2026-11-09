

CREATE TABLE IF NOT EXISTS onchain_proof (
  id SERIAL PRIMARY KEY,
  generation_record_id BIGINT NULL,
  content_hash VARCHAR(66) NOT NULL,
  tx_hash VARCHAR(66),
  contract_address VARCHAR(42),
  status VARCHAR(20) DEFAULT 'pending',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);


CREATE INDEX IF NOT EXISTS idx_onchain_proof_tx_hash ON onchain_proof(tx_hash);
CREATE INDEX IF NOT EXISTS idx_onchain_proof_generation_record_id ON onchain_proof(generation_record_id);
CREATE INDEX IF NOT EXISTS idx_onchain_proof_content_hash ON onchain_proof(content_hash);
CREATE INDEX IF NOT EXISTS idx_onchain_proof_status ON onchain_proof(status);


COMMENT ON TABLE onchain_proof IS 'Proof records with content hashes and blockchain transaction data';
COMMENT ON COLUMN onchain_proof.generation_record_id IS 'Related generation record ID';
COMMENT ON COLUMN onchain_proof.content_hash IS 'Keccak-256 content hash with 0x prefix';
COMMENT ON COLUMN onchain_proof.tx_hash IS 'Blockchain transaction hash';
COMMENT ON COLUMN onchain_proof.contract_address IS 'Smart contract address';
COMMENT ON COLUMN onchain_proof.status IS 'Proof status: pending, succeeded, or failed';







