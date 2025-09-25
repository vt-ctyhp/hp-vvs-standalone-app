CREATE TABLE IF NOT EXISTS dashboard_stage_weights (
    stage TEXT PRIMARY KEY,
    weight NUMERIC(6,4) NOT NULL
);

INSERT INTO dashboard_stage_weights(stage, weight) VALUES
    ('LEAD', 0.10),
    ('HOT LEAD', 0.20),
    ('CONSULT', 0.30),
    ('DIAMOND VIEWING', 0.50),
    ('DEPOSIT', 0.90),
    ('ORDER COMPLETED', 0.00)
ON CONFLICT (stage) DO NOTHING;
