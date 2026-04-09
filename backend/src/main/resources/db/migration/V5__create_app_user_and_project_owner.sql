CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

ALTER TABLE capex_project
    ADD COLUMN user_id BIGINT NULL;

ALTER TABLE capex_project
    ADD CONSTRAINT fk_capex_project_user
    FOREIGN KEY (user_id) REFERENCES app_user(id);
