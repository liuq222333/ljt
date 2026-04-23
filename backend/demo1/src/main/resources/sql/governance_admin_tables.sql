CREATE TABLE IF NOT EXISTS governance_admin_states (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_key VARCHAR(128) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_governance_admin_states_store_key (store_key)
);

CREATE TABLE IF NOT EXISTS governance_admin_state_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_key VARCHAR(128) NOT NULL,
    entry_type VARCHAR(128) NOT NULL,
    entry_key VARCHAR(128) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_governance_admin_state_entries_store_type_key (store_key, entry_type, entry_key),
    KEY idx_governance_admin_state_entries_store_type (store_key, entry_type)
);
