CREATE TABLE IF NOT EXISTS diamonds_orders_200 (
    id SERIAL PRIMARY KEY,
    root_appt_id TEXT NOT NULL,
    stone_reference TEXT,
    stone_type TEXT,
    stone_status TEXT,
    order_status TEXT,
    ordered_by TEXT,
    ordered_date DATE,
    memo_invoice_date DATE,
    return_due_date DATE,
    decided_by TEXT,
    decided_date DATE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_diamonds_orders_root_order_status
    ON diamonds_orders_200 (root_appt_id, order_status);

CREATE INDEX IF NOT EXISTS idx_diamonds_orders_root_stone_status
    ON diamonds_orders_200 (root_appt_id, stone_status);

CREATE TABLE IF NOT EXISTS diamonds_summary_100 (
    root_appt_id TEXT PRIMARY KEY,
    center_stone_order_status TEXT,
    total_count INTEGER DEFAULT 0,
    proposing_count INTEGER DEFAULT 0,
    not_approved_count INTEGER DEFAULT 0,
    on_the_way_count INTEGER DEFAULT 0,
    delivered_count INTEGER DEFAULT 0,
    in_stock_count INTEGER DEFAULT 0,
    keep_count INTEGER DEFAULT 0,
    return_count INTEGER DEFAULT 0,
    replace_count INTEGER DEFAULT 0,
    summary_json TEXT,
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_diamonds_summary_center_status
    ON diamonds_summary_100 (center_stone_order_status);
