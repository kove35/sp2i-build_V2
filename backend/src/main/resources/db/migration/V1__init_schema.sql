CREATE TABLE capex_project (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE capex_item (
    id SERIAL PRIMARY KEY,
    project_id INTEGER REFERENCES capex_project(id),
    lot VARCHAR(100),
    famille VARCHAR(100),
    batiment VARCHAR(100),
    niveau VARCHAR(100),
    cout_local NUMERIC,
    cout_import NUMERIC,
    quantite NUMERIC
);
