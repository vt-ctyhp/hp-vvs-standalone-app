CREATE TABLE IF NOT EXISTS master (
    id SERIAL PRIMARY KEY,
    visit_date DATE,
    root_appt_id TEXT NOT NULL,
    customer TEXT,
    phone TEXT,
    email TEXT,
    visit_type TEXT,
    visit_number TEXT,
    so_number TEXT,
    brand TEXT,
    sales_stage TEXT,
    conversion_status TEXT,
    custom_order_status TEXT,
    center_stone_order_status TEXT,
    assigned_rep TEXT,
    assisted_rep TEXT,
    headers_json JSONB DEFAULT '[]'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_master_root_appt_id ON master (root_appt_id);
CREATE INDEX IF NOT EXISTS idx_master_visit_date ON master (visit_date);
CREATE INDEX IF NOT EXISTS idx_master_brand ON master (LOWER(brand));
CREATE INDEX IF NOT EXISTS idx_master_assigned_rep ON master (LOWER(assigned_rep));

CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    root_appt_id TEXT,
    so_number TEXT,
    payment_datetime TIMESTAMP WITH TIME ZONE,
    amount_net NUMERIC(12,2),
    doc_type TEXT,
    doc_status TEXT,
    headers_json JSONB DEFAULT '[]'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_payments_root_appt_id ON payments (root_appt_id);
CREATE INDEX IF NOT EXISTS idx_payments_datetime ON payments (payment_datetime);

CREATE TABLE IF NOT EXISTS client_status_entries (
    id SERIAL PRIMARY KEY,
    root_appt_id TEXT NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_client_status_root_appt_id ON client_status_entries (root_appt_id);

CREATE TABLE IF NOT EXISTS meta (
    key TEXT PRIMARY KEY,
    value TEXT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
