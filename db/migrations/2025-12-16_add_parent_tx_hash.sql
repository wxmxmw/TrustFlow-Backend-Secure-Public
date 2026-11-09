BEGIN;
ALTER TABLE public.generation_records ADD COLUMN IF NOT EXISTS parent_tx_hash VARCHAR(128);
COMMIT;

