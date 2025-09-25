CREATE TABLE IF NOT EXISTS payments_ledger (
    id BIGSERIAL PRIMARY KEY,
    doc_number TEXT NOT NULL,
    doc_role TEXT NOT NULL,
    anchor_type TEXT NOT NULL,
    root_appt_id TEXT,
    so_number TEXT,
    basket_id TEXT,
    doc_type TEXT NOT NULL,
    doc_status TEXT NOT NULL DEFAULT 'ISSUED',
    payment_datetime TIMESTAMPTZ,
    method TEXT,
    reference TEXT,
    notes TEXT,
    amount_gross NUMERIC(14, 2) NOT NULL,
    fee_percent NUMERIC(8, 5),
    fee_amount NUMERIC(14, 2),
    subtotal NUMERIC(14, 2),
    amount_net NUMERIC(14, 2) NOT NULL,
    allocated_to_so NUMERIC(14, 2),
    lines_json JSONB,
    order_total_so NUMERIC(14, 2),
    paid_to_date_so NUMERIC(14, 2),
    balance_so NUMERIC(14, 2),
    submitted_by TEXT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    request_hash TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_ledger_doc_number ON payments_ledger (doc_number);
CREATE UNIQUE INDEX IF NOT EXISTS ux_payments_ledger_anchor_hash ON payments_ledger (anchor_type, request_hash) WHERE request_hash IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payments_ledger_so_number ON payments_ledger (so_number);
CREATE INDEX IF NOT EXISTS idx_payments_ledger_root_appt ON payments_ledger (root_appt_id);
