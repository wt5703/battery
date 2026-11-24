-- Verify the live DDL before use. This script assumes index_path already exists,
-- as shown in the original AppMapper.xml. Preserve the existing widths and defaults
-- for context_path and index_path if the live schema differs.
ALTER TABLE portal_app
    MODIFY COLUMN context_path VARCHAR(80) NULL,
    MODIFY COLUMN index_path VARCHAR(80) NULL,
    ADD COLUMN business_bp VARCHAR(128) NULL,
    ADD COLUMN description VARCHAR(100) NULL,
    ADD COLUMN icon TEXT NULL,
    ADD COLUMN business_scene VARCHAR(128) NULL,
    ADD COLUMN capability_tag VARCHAR(128) NULL,
    ADD COLUMN source VARCHAR(20) NULL,
    ADD COLUMN updated_by VARCHAR(128) NULL;

CREATE TABLE portal_app_write_lock (
    id TINYINT NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

INSERT INTO portal_app_write_lock (id) VALUES (1);

