CREATE TABLE t_param_import (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(255) NOT NULL,
    rate NUMERIC NOT NULL
);

INSERT INTO t_param_import (code, label, rate) VALUES
('TRANSPORT', 'Transport maritime', 0.08),
('ASSURANCE', 'Assurance', 0.02),
('DOUANE', 'Douane', 0.15),
('PORT', 'Frais portuaires', 0.05),
('LOCAL', 'Transport local Pointe-Noire', 0.05),
('RISQUE', 'Marge / risque import', 0.10);
