CREATE TABLE server_folders (
    id       BIGSERIAL PRIMARY KEY,
    name     TEXT NOT NULL,
    user_id  BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX server_folders_user_id_name_idx ON server_folders (user_id, LOWER(name));

CREATE TABLE server_folder_assignments (
    id        BIGSERIAL PRIMARY KEY,
    folder_id BIGINT NOT NULL REFERENCES server_folders (id) ON DELETE CASCADE,
    server_id BIGINT NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    UNIQUE (folder_id, server_id)
);

CREATE INDEX server_folder_assignments_server_id_idx ON server_folder_assignments (server_id);
