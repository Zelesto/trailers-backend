ALTER TABLE IF EXISTS rates
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS sow_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    scope_of_work TEXT NOT NULL,
    job_instructions TEXT,
    deliverables TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sow_templates_name
    ON sow_templates (name);

CREATE TABLE IF NOT EXISTS work_orders (
    id BIGSERIAL PRIMARY KEY,
    work_order_number VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    customer_id BIGINT,
    scope_of_work TEXT NOT NULL,
    job_instructions TEXT,
    deliverables TEXT,
    assigned_driver_id BIGINT,
    assigned_vehicle_id BIGINT,
    team_name VARCHAR(150),
    due_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sow_template_id BIGINT REFERENCES sow_templates(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_work_orders_status ON work_orders (status);
CREATE INDEX IF NOT EXISTS idx_work_orders_due_date ON work_orders (due_date);
CREATE INDEX IF NOT EXISTS idx_work_orders_customer ON work_orders (customer_id);

CREATE TABLE IF NOT EXISTS work_order_proofs (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES work_orders(id) ON DELETE CASCADE,
    proof_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(150),
    file_url TEXT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_work_order_proofs_work_order
    ON work_order_proofs (work_order_id);
