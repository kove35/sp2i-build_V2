ALTER TABLE capex_project
ADD COLUMN currency_code VARCHAR(3);

UPDATE capex_project
SET currency_code = 'XAF'
WHERE currency_code IS NULL OR TRIM(currency_code) = '';

ALTER TABLE capex_project
ALTER COLUMN currency_code SET DEFAULT 'XAF';

ALTER TABLE capex_project
ALTER COLUMN currency_code SET NOT NULL;
