CREATE TABLE portal_demand_sequence (
    business_date DATE NOT NULL,
    last_number SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (business_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE portal_demand (
    id CHAR(36) NOT NULL,
    number CHAR(13) NOT NULL,
    name TEXT NOT NULL,
    category VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    applicant_id VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL,
    create_time DATETIME(6) NOT NULL,
    update_time DATETIME(6) NOT NULL,
    form_json JSON NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_portal_demand_number (number),
    KEY idx_portal_demand_created (create_time, id),
    KEY idx_portal_demand_applicant_created (applicant_id, create_time, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE portal_demand_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    demand_id CHAR(36) NOT NULL,
    operation_time DATETIME(6) NOT NULL,
    operator_id VARCHAR(128) NOT NULL,
    previous_status VARCHAR(16) NULL,
    action VARCHAR(16) NOT NULL,
    opinion VARCHAR(1000) NULL,
    next_status VARCHAR(16) NOT NULL,
    next_operator_id VARCHAR(128) NULL,
    stage_seconds BIGINT UNSIGNED NULL,
    PRIMARY KEY (id),
    KEY idx_portal_demand_log_time (demand_id, operation_time, id),
    CONSTRAINT fk_portal_demand_log_request FOREIGN KEY (demand_id) REFERENCES portal_demand (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

