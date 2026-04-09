ALTER TABLE capex_item
    ALTER COLUMN quantite TYPE DOUBLE PRECISION USING quantite::double precision,
    ALTER COLUMN cout_local TYPE DOUBLE PRECISION USING cout_local::double precision,
    ALTER COLUMN cout_import TYPE DOUBLE PRECISION USING cout_import::double precision;
