ALTER TABLE capex_project
    ALTER COLUMN id TYPE BIGINT;

ALTER TABLE capex_item
    ALTER COLUMN project_id TYPE BIGINT;
