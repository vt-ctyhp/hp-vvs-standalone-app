ALTER TABLE master
    ADD COLUMN IF NOT EXISTS in_production_status TEXT,
    ADD COLUMN IF NOT EXISTS next_steps TEXT,
    ADD COLUMN IF NOT EXISTS three_d_deadline DATE,
    ADD COLUMN IF NOT EXISTS three_d_deadline_moves INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS production_deadline DATE,
    ADD COLUMN IF NOT EXISTS production_deadline_moves INTEGER DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'master_root_appt_unique') THEN
        ALTER TABLE master ADD CONSTRAINT master_root_appt_unique UNIQUE (root_appt_id);
    END IF;
END$$;

ALTER TABLE client_status_log
    ADD COLUMN IF NOT EXISTS sales_stage TEXT,
    ADD COLUMN IF NOT EXISTS conversion_status TEXT,
    ADD COLUMN IF NOT EXISTS custom_order_status TEXT,
    ADD COLUMN IF NOT EXISTS in_production_status TEXT,
    ADD COLUMN IF NOT EXISTS center_stone_order_status TEXT,
    ADD COLUMN IF NOT EXISTS next_steps TEXT,
    ADD COLUMN IF NOT EXISTS assisted_rep TEXT,
    ADD COLUMN IF NOT EXISTS deadline_type TEXT,
    ADD COLUMN IF NOT EXISTS deadline_date DATE,
    ADD COLUMN IF NOT EXISTS move_count INTEGER;

CREATE TABLE IF NOT EXISTS per_client_reports (
    root_appt_id TEXT PRIMARY KEY,
    client_name TEXT,
    brand TEXT,
    assigned_rep TEXT,
    assisted_rep TEXT,
    so_number TEXT,
    sales_stage TEXT,
    conversion_status TEXT,
    custom_order_status TEXT,
    in_production_status TEXT,
    center_stone_order_status TEXT,
    next_steps TEXT,
    updated_by TEXT,
    updated_at TIMESTAMPTZ,
    order_date DATE,
    three_d_deadline DATE,
    three_d_deadline_moves INTEGER DEFAULT 0,
    production_deadline DATE,
    production_deadline_moves INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    last_modified TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS per_client_entries (
    id SERIAL PRIMARY KEY,
    root_appt_id TEXT NOT NULL,
    log_date DATE,
    sales_stage TEXT,
    conversion_status TEXT,
    custom_order_status TEXT,
    center_stone_order_status TEXT,
    next_steps TEXT,
    deadline_type TEXT,
    deadline_date DATE,
    move_count INTEGER,
    assisted_rep TEXT,
    updated_by TEXT,
    updated_at TIMESTAMPTZ,
    FOREIGN KEY (root_appt_id) REFERENCES master(root_appt_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_per_client_entries_root ON per_client_entries(root_appt_id);
