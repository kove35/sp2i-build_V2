ALTER TABLE capex_item
    ADD COLUMN duree_estimee INTEGER NULL,
    ADD COLUMN ordre_execution INTEGER NULL,
    ADD COLUMN priorite VARCHAR(20) NULL,
    ADD COLUMN dependances TEXT NULL;
