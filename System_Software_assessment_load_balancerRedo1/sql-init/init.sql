CREATE TABLE IF NOT EXISTS nodes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    node_name VARCHAR(50),
    address VARCHAR(100),
    status VARCHAR(20),
    load_count INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(256),
    role VARCHAR(20) DEFAULT 'STANDARD'
);

CREATE TABLE IF NOT EXISTS file_metadata (
    id INT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(200),
    owner VARCHAR(50),
    size BIGINT,
    chunks INT DEFAULT 1,
    storage_node VARCHAR(100)
);

INSERT INTO nodes (node_name, address, status) VALUES
('storage-node-1', 'lbc_storage_01', 'ONLINE'),
('storage-node-2', 'lbc_storage_02', 'ONLINE'),
('storage-node-3', 'lbc_storage_03', 'ONLINE'),
('storage-node-4', 'lbc_storage_04', 'ONLINE');
