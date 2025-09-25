CREATE TABLE IF NOT EXISTS master (
    id SERIAL PRIMARY KEY,
    visit_date DATE,
    root_appt_id TEXT NOT NULL,
    customer_name TEXT,
    phone TEXT,
    phone_normalized TEXT,
    email TEXT,
    email_lower TEXT,
    visit_type TEXT,
    visit_number INTEGER,
    so_number TEXT,
    brand TEXT,
    sales_stage TEXT,
    conversion_status TEXT,
    custom_order_status TEXT,
    center_stone_order_status TEXT,
    assigned_rep TEXT,
    assisted_rep TEXT,
    headers_json TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    root_appt_id TEXT NOT NULL,
    payment_datetime TIMESTAMPTZ,
    doc_type TEXT,
    amount_net NUMERIC(12,2),
    doc_status TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS client_status_log (
    id SERIAL PRIMARY KEY,
    root_appt_id TEXT,
    log_date DATE,
    log_type TEXT,
    notes TEXT,
    updated_by TEXT,
    updated_at TIMESTAMPTZ,
    raw_payload TEXT
);

CREATE TABLE IF NOT EXISTS meta (
    meta_key TEXT PRIMARY KEY,
    meta_value TEXT
);

CREATE INDEX IF NOT EXISTS idx_master_root_appt ON master (root_appt_id);
CREATE INDEX IF NOT EXISTS idx_master_visit_date ON master (visit_date);
CREATE INDEX IF NOT EXISTS idx_master_brand ON master (brand);
CREATE INDEX IF NOT EXISTS idx_master_assigned_rep ON master (assigned_rep);
