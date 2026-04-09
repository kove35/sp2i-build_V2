ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS location VARCHAR(255);

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS type VARCHAR(100);

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS surface NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS budget NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS transport_rate NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS douane_rate NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS port_rate NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS local_rate NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS margin_rate NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS risk_rate NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS import_threshold NUMERIC;

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS strategy_mode VARCHAR(100);

ALTER TABLE capex_project
    ADD COLUMN IF NOT EXISTS structure_json TEXT;
