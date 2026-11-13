MERGE INTO permission (resource, action) KEY (resource, action) VALUES ('USER', 'MANAGE');
MERGE INTO permission (resource, action) KEY (resource, action) VALUES ('ACCOUNT', 'MANAGE');
MERGE INTO permission (resource, action) KEY (resource, action) VALUES ('TRIP', 'VIEW');
MERGE INTO permission (resource, action) KEY (resource, action) VALUES ('TRIP', 'CREATE');
MERGE INTO permission (resource, action) KEY (resource, action) VALUES ('FUEL', 'VIEW');

MERGE INTO role (name, description) KEY (name) VALUES ('SUPER_ADMIN', 'Full system access');
MERGE INTO role (name, description) KEY (name) VALUES ('DISPATCHER', 'Dispatch access');
MERGE INTO role (name, description) KEY (name) VALUES ('MANAGER', 'Operational management access');

MERGE INTO app_user (email, password_hash, enabled, username, status_notes, created_at, updated_at)
KEY (email)
VALUES ('admin@logistics.local', '$2a$12$Q7Qz8x.tW8DOdki1Py3mau397uoGzAWXWVHuupcG.ByojTtuNRJ82', TRUE, 'admin', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

MERGE INTO user_role (user_id, role_id) KEY (user_id, role_id)
VALUES ((SELECT id FROM app_user WHERE email = 'admin@logistics.local'),
        (SELECT id FROM role WHERE name = 'SUPER_ADMIN'));

MERGE INTO role_permission (role_id, permission_id) KEY (role_id, permission_id)
VALUES ((SELECT id FROM role WHERE name = 'SUPER_ADMIN'), (SELECT id FROM permission WHERE resource = 'USER' AND action = 'MANAGE'));
MERGE INTO role_permission (role_id, permission_id) KEY (role_id, permission_id)
VALUES ((SELECT id FROM role WHERE name = 'SUPER_ADMIN'), (SELECT id FROM permission WHERE resource = 'ACCOUNT' AND action = 'MANAGE'));
MERGE INTO role_permission (role_id, permission_id) KEY (role_id, permission_id)
VALUES ((SELECT id FROM role WHERE name = 'SUPER_ADMIN'), (SELECT id FROM permission WHERE resource = 'TRIP' AND action = 'VIEW'));
MERGE INTO role_permission (role_id, permission_id) KEY (role_id, permission_id)
VALUES ((SELECT id FROM role WHERE name = 'SUPER_ADMIN'), (SELECT id FROM permission WHERE resource = 'TRIP' AND action = 'CREATE'));
MERGE INTO role_permission (role_id, permission_id) KEY (role_id, permission_id)
VALUES ((SELECT id FROM role WHERE name = 'SUPER_ADMIN'), (SELECT id FROM permission WHERE resource = 'FUEL' AND action = 'VIEW'));
