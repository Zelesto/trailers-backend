package com.pgsa.trailers.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/**
 * Database Initializer matching actual production schema
 */
@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Value("${app.db.init.enabled:false}")
    private boolean enabled;

    @Value("${app.db.init.drop:false}")
    private boolean drop;

    @Value("${app.db.init.seed:false}")
    private boolean seed;

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void run(String... args) {
        if (!enabled) {
            logger.info("Database initializer is disabled");
            return;
        }

        try {
            logger.info("Starting database initialization...");

            if (drop) {
                dropAll();
            }

            createEnums();
            createTables();
            createConstraints();
            createIndexes();

            if (seed) {
                seedData();
            }

            logger.info("Database initialization completed successfully");

        } catch (Exception e) {
            logger.error("Database initialization failed: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    /* ===================== DROP ALL ===================== */
    private void dropAll() {
        logger.info("Dropping all existing tables and enums...");
        jdbcTemplate.execute("""
            DO $$ DECLARE
                r RECORD;
            BEGIN
                -- Drop tables in reverse dependency order
                DROP TABLE IF EXISTS finance_reconciliation_pending CASCADE;
                DROP TABLE IF EXISTS finance_reconciliation_running_balance CASCADE;
                DROP TABLE IF EXISTS finance_reconciliation_view CASCADE;
                DROP TABLE IF EXISTS reconciliation CASCADE;
                DROP TABLE IF EXISTS payment_allocation CASCADE;
                DROP TABLE IF EXISTS payment CASCADE;
                DROP TABLE IF EXISTS invoice CASCADE;
                DROP TABLE IF EXISTS trip_metrics CASCADE;
                DROP TABLE IF EXISTS trip CASCADE;
                DROP TABLE IF EXISTS vehicle_metrics CASCADE;
                DROP TABLE IF EXISTS vehicle CASCADE;
                DROP TABLE IF EXISTS driver_metrics CASCADE;
                DROP TABLE IF EXISTS driver CASCADE;
                DROP TABLE IF EXISTS fuel_slip CASCADE;
                DROP TABLE IF EXISTS fuel_source CASCADE;
                DROP TABLE IF EXISTS stock_movement CASCADE;
                DROP TABLE IF EXISTS stock_count_line CASCADE;
                DROP TABLE IF EXISTS stock_count CASCADE;
                DROP TABLE IF EXISTS inventory_item CASCADE;
                DROP TABLE IF EXISTS inventory_location CASCADE;
                DROP TABLE IF EXISTS load CASCADE;
                DROP TABLE IF EXISTS account_transaction CASCADE;
                DROP TABLE IF EXISTS account_statement CASCADE;
                DROP TABLE IF EXISTS account CASCADE;
                DROP TABLE IF EXISTS suppliers CASCADE;
                DROP TABLE IF EXISTS user_role CASCADE;
                DROP TABLE IF EXISTS role_permission CASCADE;
                DROP TABLE IF EXISTS permission CASCADE;
                DROP TABLE IF EXISTS role CASCADE;
                DROP TABLE IF EXISTS app_user CASCADE;

                -- Drop enums
                DROP TYPE IF EXISTS account_type CASCADE;
                DROP TYPE IF EXISTS account_statement_status CASCADE;
                DROP TYPE IF EXISTS payment_status CASCADE;
                DROP TYPE IF EXISTS reconciliation_status CASCADE;
                DROP TYPE IF EXISTS stock_count_status CASCADE;
                DROP TYPE IF EXISTS stock_movement_type CASCADE;
                DROP TYPE IF EXISTS inventory_location_type CASCADE;
                DROP TYPE IF EXISTS load_status CASCADE;
                DROP TYPE IF EXISTS driver_status CASCADE;
                DROP TYPE IF EXISTS invoice_status CASCADE;
            END $$;
        """);
    }

    /* ===================== ENUMS ===================== */
    private void createEnums() {
        logger.info("Creating enum types...");

        List<String> enums = Arrays.asList(
                "account_type",
                "account_statement_status",
                "payment_status",
                "reconciliation_status",
                "stock_count_status",
                "stock_movement_type",
                "inventory_location_type",
                "load_status",
                "driver_status",
                "invoice_status"
        );

        List<List<String>> enumValues = Arrays.asList(
                Arrays.asList("'FUEL'", "'BANK'", "'CASH'", "'SUPPLIER'"),
                Arrays.asList("'OPEN'", "'CLOSED'"),
                Arrays.asList("'CAPTURED'", "'ALLOCATED'", "'POSTED'", "'FAILED'"),
                Arrays.asList("'BALANCED'", "'UNBALANCED'"),
                Arrays.asList("'DRAFT'", "'IN_PROGRESS'", "'COMPLETED'", "'ADJUSTED'", "'POSTED'"),
                Arrays.asList("'IN'", "'OUT'", "'ADJUSTMENT'"),
                Arrays.asList("'WAREHOUSE'", "'YARD'", "'TRUCK'", "'SITE'"),
                Arrays.asList("'PENDING'", "'LOADED'", "'UNLOADED'"),
                Arrays.asList("'ACTIVE'", "'INACTIVE'"),
                Arrays.asList("'PENDING'", "'PAID'", "'CANCELLED'")
        );

        for (int i = 0; i < enums.size(); i++) {
            createEnumIfNotExists(enums.get(i), enumValues.get(i));
        }
    }

    private void createEnumIfNotExists(String enumName, List<String> values) {
        try {
            String checkSql = "SELECT EXISTS (SELECT 1 FROM pg_type WHERE typname = ?)";
            Boolean exists = jdbcTemplate.queryForObject(checkSql, Boolean.class, enumName);

            if (exists != null && !exists) {
                String valuesList = String.join(",", values);
                String createSql = String.format("CREATE TYPE %s AS ENUM (%s)", enumName, valuesList);
                jdbcTemplate.execute(createSql);
                logger.info("Created enum type: {}", enumName);
            }
        } catch (Exception e) {
            logger.warn("Could not create enum type {}: {}", enumName, e.getMessage());
        }
    }

    /* ===================== TABLE CREATION ===================== */
    private void createTables() {
        logger.info("Creating tables...");

        // Create app_user table with all columns from production
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGSERIAL PRIMARY KEY,
                email VARCHAR(255) NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                enabled BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                username VARCHAR(255),
                full_name VARCHAR(255),
                designation VARCHAR(255),
                phone_number VARCHAR(50),
                address VARCHAR(255),
                city VARCHAR(100),
                state VARCHAR(100),
                country VARCHAR(100),
                license_number VARCHAR(100),
                license_expiry DATE,
                onboard_date DATE,
                incidents_logged INTEGER DEFAULT 0,
                trips_completed INTEGER DEFAULT 0,
                hours_active INTEGER DEFAULT 0,
                km_travelled INTEGER DEFAULT 0,
                tasks_completed INTEGER DEFAULT 0,
                overall_score NUMERIC(5,2),
                emergency_contact VARCHAR(255),
                department VARCHAR(100),
                manager_id BIGINT,
                profile_image_url VARCHAR(500),
                documents_uploaded JSON,
                last_login TIMESTAMP,
                status_notes TEXT,
                CHECK (id IS NOT NULL),
                CHECK (email IS NOT NULL),
                CHECK (password_hash IS NOT NULL)
            );
        """);

        // Create role table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS role (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                description VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (name IS NOT NULL)
            );
        """);

        // Create permission table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS permission (
                id BIGSERIAL PRIMARY KEY,
                resource VARCHAR(100) NOT NULL,
                action VARCHAR(50) NOT NULL,
                CHECK (id IS NOT NULL),
                CHECK (resource IS NOT NULL),
                CHECK (action IS NOT NULL)
            );
        """);

        // Create role_permission junction table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS role_permission (
                role_id BIGINT NOT NULL,
                permission_id BIGINT NOT NULL,
                PRIMARY KEY (role_id, permission_id),
                CHECK (role_id IS NOT NULL),
                CHECK (permission_id IS NOT NULL)
            );
        """);

        // Create user_role junction table
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS user_role (
                user_id BIGINT NOT NULL,
                role_id BIGINT NOT NULL,
                PRIMARY KEY (user_id, role_id),
                CHECK (user_id IS NOT NULL),
                CHECK (role_id IS NOT NULL)
            );
        """);

        // Create account table with all production columns
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS account (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                type account_type NOT NULL,
                provider VARCHAR(100),
                balance DECIMAL(15,2) DEFAULT 0.00,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                currency VARCHAR(3),
                account_number VARCHAR(100),
                credit_limit DECIMAL(15,2),
                status VARCHAR(50) DEFAULT 'ACTIVE',
                contact_person VARCHAR(255),
                contact_phone VARCHAR(50),
                contact_email VARCHAR(255),
                billing_cycle VARCHAR(50),
                last_recon_date TIMESTAMP,
                next_recon_due TIMESTAMP,
                notes TEXT,
                audit_trail JSON,
                CHECK (id IS NOT NULL),
                CHECK (name IS NOT NULL),
                CHECK (type IS NOT NULL)
            );
        """);

        // Create suppliers table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS suppliers (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                contact_person VARCHAR(255),
                email VARCHAR(255),
                phone VARCHAR(50),
                address TEXT,
                tax_number VARCHAR(100),
                status VARCHAR(50) DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (name IS NOT NULL)
            );
        """);

        // Create driver table with all production columns
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS driver (
                id BIGSERIAL PRIMARY KEY,
                first_name VARCHAR(100) NOT NULL,
                last_name VARCHAR(100) NOT NULL,
                license_number VARCHAR(100),
                license_expiry DATE,
                phone_number VARCHAR(50),
                email VARCHAR(255),
                status driver_status DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                hire_date DATE,
                license_type VARCHAR(50),
                app_user_id BIGINT,
                employment_type VARCHAR(50),
                shift_pattern VARCHAR(50),
                assigned_vehicle_id BIGINT,
                training_completed BOOLEAN DEFAULT FALSE,
                training_certificates JSON,
                medical_clearance_date DATE,
                next_medical_due DATE,
                incidents_logged INTEGER DEFAULT 0,
                total_trips INTEGER DEFAULT 0,
                total_km_travelled INTEGER DEFAULT 0,
                total_hours_active INTEGER DEFAULT 0,
                performance_score NUMERIC(5,2),
                notes TEXT,
                audit_trail JSON,
                CHECK (id IS NOT NULL),
                CHECK (first_name IS NOT NULL),
                CHECK (last_name IS NOT NULL)
            );
        """);

        // Create vehicle table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS vehicle (
                id BIGSERIAL PRIMARY KEY,
                registration_number VARCHAR(50) NOT NULL,
                vin VARCHAR(100),
                make VARCHAR(100),
                model VARCHAR(100),
                year INTEGER,
                fuel_type VARCHAR(50),
                current_mileage DECIMAL(10,2) DEFAULT 0,
                status VARCHAR(50) DEFAULT 'ACTIVE',
                assigned_driver_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (registration_number IS NOT NULL)
            );
        """);

        // Create load table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS load (
                id BIGSERIAL PRIMARY KEY,
                load_number VARCHAR(100) NOT NULL,
                description TEXT,
                weight_kg DECIMAL(10,2),
                volume_cubic_m DECIMAL(10,2),
                loading_date TIMESTAMP,
                unloading_date TIMESTAMP,
                status load_status DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (load_number IS NOT NULL)
            );
        """);

        // Create trip table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS trip (
                id BIGSERIAL PRIMARY KEY,
                trip_number VARCHAR(100) NOT NULL,
                vehicle_id BIGINT,
                driver_id BIGINT,
                load_id BIGINT,
                start_date TIMESTAMP NOT NULL,
                end_date TIMESTAMP,
                origin_location VARCHAR(255),
                destination_location VARCHAR(255),
                status VARCHAR(50) DEFAULT 'PENDING',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CHECK (id IS NOT NULL),
                CHECK (trip_number IS NOT NULL),
                CHECK (start_date IS NOT NULL)
            );
        """);

        // Create account_statement table with all production columns
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS account_statement (
                id BIGSERIAL PRIMARY KEY,
                account_id BIGINT,
                statement_date DATE NOT NULL,
                opening_balance DECIMAL(15,2) DEFAULT 0.00,
                closing_balance DECIMAL(15,2) DEFAULT 0.00,
                total_debits DECIMAL(15,2) DEFAULT 0.00,
                total_credits DECIMAL(15,2) DEFAULT 0.00,
                status account_statement_status DEFAULT 'OPEN',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                period_start DATE,
                period_end DATE,
                currency VARCHAR(3),
                recon_date TIMESTAMP,
                recon_by BIGINT,
                variance_amount DECIMAL(15,2),
                variance_notes TEXT,
                supporting_document_url VARCHAR(500),
                audit_trail JSON,
                CHECK (id IS NOT NULL),
                CHECK (statement_date IS NOT NULL)
            );
        """);

        // Create account_transaction table with all production columns
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS account_transaction (
                id BIGSERIAL PRIMARY KEY,
                account_id BIGINT NOT NULL,
                transaction_date TIMESTAMP NOT NULL,
                posting_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                amount DECIMAL(15,2) NOT NULL,
                direction VARCHAR(10) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
                source_type VARCHAR(50) NOT NULL,
                source_id BIGINT NOT NULL,
                description TEXT,
                currency VARCHAR(3),
                account_statement_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (account_id IS NOT NULL),
                CHECK (amount IS NOT NULL),
                CHECK (direction IS NOT NULL),
                CHECK (source_type IS NOT NULL),
                CHECK (source_id IS NOT NULL),
                CHECK (transaction_date IS NOT NULL)
            );
        """);

        // Create fuel_source table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS fuel_source (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                source_type VARCHAR(50) NOT NULL,
                account_id BIGINT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CHECK (id IS NOT NULL),
                CHECK (name IS NOT NULL),
                CHECK (source_type IS NOT NULL)
            );
        """);

        // Create fuel_slip table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS fuel_slip (
                id BIGSERIAL PRIMARY KEY,
                slip_number VARCHAR(100) NOT NULL,
                transaction_date TIMESTAMP NOT NULL,
                vehicle_id BIGINT,
                driver_id BIGINT,
                fuel_source_id BIGINT,
                quantity DECIMAL(10,2) NOT NULL,
                unit_price DECIMAL(10,2) NOT NULL,
                total_amount DECIMAL(15,2),
                odometer_reading DECIMAL(10,2),
                location VARCHAR(255),
                station_name VARCHAR(255),
                pump_number VARCHAR(50),
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                finalized BOOLEAN DEFAULT false,
                CHECK (id IS NOT NULL),
                CHECK (slip_number IS NOT NULL),
                CHECK (quantity IS NOT NULL),
                CHECK (unit_price IS NOT NULL),
                CHECK (transaction_date IS NOT NULL)
            );
        """);

        // Create inventory_location table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS inventory_location (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                type inventory_location_type NOT NULL,
                address TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CHECK (id IS NOT NULL),
                CHECK (name IS NOT NULL),
                CHECK (type IS NOT NULL)
            );
        """);

        // Create inventory_item table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS inventory_item (
                id BIGSERIAL PRIMARY KEY,
                sku VARCHAR(100) NOT NULL,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                unit VARCHAR(50) DEFAULT 'EA',
                unit_cost DECIMAL(15,2) DEFAULT 0.00,
                category VARCHAR(100),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (sku IS NOT NULL),
                CHECK (name IS NOT NULL)
            );
        """);

        // Create stock_count table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS stock_count (
                id BIGSERIAL PRIMARY KEY,
                count_date DATE NOT NULL,
                location_id BIGINT,
                status stock_count_status DEFAULT 'DRAFT',
                total_items INTEGER DEFAULT 0,
                counted_items INTEGER DEFAULT 0,
                variance_count INTEGER DEFAULT 0,
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (count_date IS NOT NULL)
            );
        """);

        // Create stock_count_line table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS stock_count_line (
                id BIGSERIAL PRIMARY KEY,
                stock_count_id BIGINT,
                product_id BIGINT,
                system_quantity DECIMAL(10,2) DEFAULT 0,
                counted_quantity DECIMAL(10,2) DEFAULT 0,
                variance DECIMAL(10,2),
                variance_percentage DECIMAL(5,2),
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CHECK (id IS NOT NULL)
            );
        """);

        // Create stock_movement table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS stock_movement (
                id BIGSERIAL PRIMARY KEY,
                movement_date TIMESTAMP NOT NULL,
                product_id BIGINT,
                from_location_id BIGINT,
                to_location_id BIGINT,
                quantity DECIMAL(10,2) NOT NULL,
                movement_type stock_movement_type NOT NULL,
                reference_number VARCHAR(100),
                reference_type VARCHAR(100),
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (movement_date IS NOT NULL),
                CHECK (quantity IS NOT NULL),
                CHECK (movement_type IS NOT NULL)
            );
        """);

        // Create driver_metrics table with all production columns
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS driver_metrics (
                id BIGSERIAL PRIMARY KEY,
                driver_id BIGINT,
                metric_date DATE NOT NULL,
                total_trips INTEGER DEFAULT 0,
                total_distance_km DECIMAL(10,2) DEFAULT 0,
                total_hours DECIMAL(10,2) DEFAULT 0,
                fuel_consumption_liters DECIMAL(10,2) DEFAULT 0,
                average_speed_kmh DECIMAL(10,2) DEFAULT 0,
                hard_braking_count INTEGER DEFAULT 0,
                rapid_acceleration_count INTEGER DEFAULT 0,
                safety_score DECIMAL(5,2) DEFAULT 0,
                efficiency_score DECIMAL(5,2) DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                idle_time_hours DECIMAL(10,2),
                incident_count INTEGER DEFAULT 0,
                violations_count INTEGER DEFAULT 0,
                training_hours DECIMAL(10,2),
                overtime_hours DECIMAL(10,2),
                bonus_score DECIMAL(5,2),
                CHECK (id IS NOT NULL),
                CHECK (metric_date IS NOT NULL)
            );
        """);

        // Create vehicle_metrics table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS vehicle_metrics (
                id BIGSERIAL PRIMARY KEY,
                vehicle_id BIGINT,
                metric_date DATE NOT NULL,
                distance_traveled DECIMAL(10,2) DEFAULT 0,
                fuel_used DECIMAL(10,2) DEFAULT 0,
                fuel_efficiency DECIMAL(10,2) DEFAULT 0,
                maintenance_cost DECIMAL(15,2) DEFAULT 0,
                downtime_hours DECIMAL(10,2) DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CHECK (id IS NOT NULL),
                CHECK (metric_date IS NOT NULL)
            );
        """);

        // Create trip_metrics table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS trip_metrics (
                id BIGSERIAL PRIMARY KEY,
                trip_id BIGINT,
                total_distance DECIMAL(10,2) DEFAULT 0,
                total_duration DECIMAL(10,2) DEFAULT 0,
                fuel_used DECIMAL(10,2) DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CHECK (id IS NOT NULL)
            );
        """);

        // Create invoice table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS invoice (
                id BIGSERIAL PRIMARY KEY,
                invoice_number VARCHAR(100) NOT NULL,
                supplier_id BIGINT,
                account_id BIGINT,
                trip_id BIGINT,
                load_id BIGINT,
                invoice_date DATE NOT NULL,
                due_date DATE,
                total_amount DECIMAL(15,2) NOT NULL,
                tax_amount DECIMAL(15,2) DEFAULT 0,
                net_amount DECIMAL(15,2),
                status invoice_status DEFAULT 'PENDING',
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (invoice_number IS NOT NULL),
                CHECK (invoice_date IS NOT NULL),
                CHECK (total_amount IS NOT NULL)
            );
        """);

        // Create payment table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS payment (
                id BIGSERIAL PRIMARY KEY,
                payment_date TIMESTAMP NOT NULL,
                account_id BIGINT,
                amount DECIMAL(15,2) NOT NULL,
                currency VARCHAR(3) DEFAULT 'ZAR',
                payment_method VARCHAR(50),
                reference_number VARCHAR(100),
                status payment_status DEFAULT 'CAPTURED',
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (amount IS NOT NULL),
                CHECK (payment_date IS NOT NULL)
            );
        """);

        // Create payment_allocation table (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS payment_allocation (
                id BIGSERIAL PRIMARY KEY,
                payment_id BIGINT,
                invoice_id BIGINT,
                amount DECIMAL(15,2) NOT NULL,
                allocation_date DATE NOT NULL,
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (amount IS NOT NULL),
                CHECK (allocation_date IS NOT NULL)
            );
        """);

        // Create reconciliation table with generated column (no additional columns from production)
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS reconciliation (
                id BIGSERIAL PRIMARY KEY,
                reconciliation_date DATE NOT NULL,
                account_id BIGINT,
                statement_balance DECIMAL(15,2) NOT NULL,
                system_balance DECIMAL(15,2) NOT NULL,
                variance DECIMAL(15,2) GENERATED ALWAYS AS (statement_balance - system_balance) STORED,
                status reconciliation_status DEFAULT 'UNBALANCED',
                notes TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(255),
                updated_by VARCHAR(255),
                CHECK (id IS NOT NULL),
                CHECK (reconciliation_date IS NOT NULL),
                CHECK (statement_balance IS NOT NULL),
                CHECK (system_balance IS NOT NULL)
            );
        """);

        // Create finance reconciliation views
        createFinanceViews();
    }

    private void createFinanceViews() {
        logger.info("Creating finance reconciliation views...");

        // Create finance_reconciliation_view
        jdbcTemplate.execute("""
            CREATE OR REPLACE VIEW finance_reconciliation_view AS
            SELECT 
                a.id as account_id,
                a.name as account_name,
                a.type as account_type,
                ast.id as statement_id,
                ast.statement_date,
                ast.opening_balance,
                ast.closing_balance,
                ast.total_debits,
                ast.total_credits,
                at.transaction_date,
                CONCAT(at.source_type, ' - ', at.description) as transaction_type,
                at.source_id,
                at.amount,
                at.direction,
                at.description,
                ast.variance_amount
            FROM account a
            LEFT JOIN account_statement ast ON a.id = ast.account_id
            LEFT JOIN account_transaction at ON a.id = at.account_id 
                AND (ast.id = at.account_statement_id OR at.account_statement_id IS NULL)
            ORDER BY a.id, ast.statement_date DESC, at.transaction_date;
        """);

        // Create finance_reconciliation_running_balance view
        jdbcTemplate.execute("""
            CREATE OR REPLACE VIEW finance_reconciliation_running_balance AS
            SELECT 
                a.id as account_id,
                a.name as account_name,
                a.type as account_type,
                ast.id as statement_id,
                ast.statement_date,
                ast.opening_balance,
                ast.closing_balance,
                ast.total_debits,
                ast.total_credits,
                at.transaction_date,
                CONCAT(at.source_type, ' - ', at.description) as transaction_type,
                at.source_id,
                at.amount,
                at.direction,
                at.description,
                SUM(CASE 
                    WHEN at.direction = 'DEBIT' THEN -at.amount 
                    WHEN at.direction = 'CREDIT' THEN at.amount 
                    ELSE 0 
                END) OVER (PARTITION BY a.id ORDER BY at.transaction_date) as running_balance,
                ast.variance_amount
            FROM account a
            LEFT JOIN account_statement ast ON a.id = ast.account_id
            LEFT JOIN account_transaction at ON a.id = at.account_id 
                AND (ast.id = at.account_statement_id OR at.account_statement_id IS NULL)
            ORDER BY a.id, at.transaction_date;
        """);

        // Create finance_reconciliation_pending view
        jdbcTemplate.execute("""
            CREATE OR REPLACE VIEW finance_reconciliation_pending AS
            SELECT 
                a.id as account_id,
                a.name as account_name,
                a.type as account_type,
                at.transaction_date,
                CONCAT(at.source_type, ' - ', at.description) as transaction_type,
                at.source_id,
                at.amount,
                at.direction,
                at.description,
                rb.running_balance,
                ast.closing_balance as last_closing_balance,
                a.balance as system_balance,
                COALESCE(ast.closing_balance, 0) + rb.running_balance as projected_closing_balance,
                CASE 
                    WHEN at.account_statement_id IS NULL THEN TRUE 
                    ELSE FALSE 
                END as pending_reconciliation
            FROM account a
            LEFT JOIN account_statement ast ON a.id = ast.account_id 
                AND ast.status = 'CLOSED'
                AND ast.statement_date = (
                    SELECT MAX(statement_date) 
                    FROM account_statement 
                    WHERE account_id = a.id AND status = 'CLOSED'
                )
            LEFT JOIN account_transaction at ON a.id = at.account_id
            LEFT JOIN LATERAL (
                SELECT 
                    SUM(CASE 
                        WHEN at2.direction = 'DEBIT' THEN -at2.amount 
                        WHEN at2.direction = 'CREDIT' THEN at2.amount 
                        ELSE 0 
                    END) as running_balance
                FROM account_transaction at2
                WHERE at2.account_id = a.id 
                    AND at2.transaction_date > COALESCE(ast.statement_date, '1900-01-01')
                    AND at2.transaction_date <= at.transaction_date
            ) rb ON TRUE
            WHERE at.account_statement_id IS NULL
            ORDER BY a.id, at.transaction_date;
        """);
    }

    /* ===================== CONSTRAINT CREATION ===================== */
    private void createConstraints() {
        logger.info("Creating unique and foreign key constraints...");

        // Unique constraints
        jdbcTemplate.execute("""
            -- app_user
            ALTER TABLE app_user ADD CONSTRAINT IF NOT EXISTS app_user_email_key UNIQUE (email);
            
            -- role
            ALTER TABLE role ADD CONSTRAINT IF NOT EXISTS role_name_key UNIQUE (name);
            
            -- permission
            ALTER TABLE permission ADD CONSTRAINT IF NOT EXISTS permission_resource_action_key UNIQUE (resource, action);
            
            -- account
            ALTER TABLE account ADD CONSTRAINT IF NOT EXISTS account_name_key UNIQUE (name);
            
            -- suppliers
            ALTER TABLE suppliers ADD CONSTRAINT IF NOT EXISTS suppliers_name_key UNIQUE (name);
            
            -- driver
            ALTER TABLE driver ADD CONSTRAINT IF NOT EXISTS driver_license_number_key UNIQUE (license_number);
            
            -- vehicle
            ALTER TABLE vehicle ADD CONSTRAINT IF NOT EXISTS vehicle_registration_number_key UNIQUE (registration_number);
            ALTER TABLE vehicle ADD CONSTRAINT IF NOT EXISTS vehicle_vin_key UNIQUE (vin);
            
            -- load
            ALTER TABLE load ADD CONSTRAINT IF NOT EXISTS load_load_number_key UNIQUE (load_number);
            
            -- trip
            ALTER TABLE trip ADD CONSTRAINT IF NOT EXISTS trip_trip_number_key UNIQUE (trip_number);
            
            -- account_statement
            ALTER TABLE account_statement ADD CONSTRAINT IF NOT EXISTS account_statement_account_id_statement_date_key UNIQUE (account_id, statement_date);
            
            -- fuel_source
            ALTER TABLE fuel_source ADD CONSTRAINT IF NOT EXISTS fuel_source_name_key UNIQUE (name);
            
            -- fuel_slip
            ALTER TABLE fuel_slip ADD CONSTRAINT IF NOT EXISTS fuel_slip_slip_number_key UNIQUE (slip_number);
            
            -- inventory_location
            ALTER TABLE inventory_location ADD CONSTRAINT IF NOT EXISTS inventory_location_name_key UNIQUE (name);
            
            -- inventory_item
            ALTER TABLE inventory_item ADD CONSTRAINT IF NOT EXISTS inventory_item_sku_key UNIQUE (sku);
            
            -- driver_metrics
            ALTER TABLE driver_metrics ADD CONSTRAINT IF NOT EXISTS driver_metrics_driver_id_metric_date_key UNIQUE (driver_id, metric_date);
            
            -- vehicle_metrics
            ALTER TABLE vehicle_metrics ADD CONSTRAINT IF NOT EXISTS vehicle_metrics_vehicle_id_metric_date_key UNIQUE (vehicle_id, metric_date);
            
            -- invoice
            ALTER TABLE invoice ADD CONSTRAINT IF NOT EXISTS invoice_invoice_number_key UNIQUE (invoice_number);
        """);

        // Foreign key constraints with correct ON DELETE/UPDATE rules
        jdbcTemplate.execute("""
            -- user_role constraints
            ALTER TABLE user_role ADD CONSTRAINT IF NOT EXISTS user_role_user_id_fkey 
            FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;
            
            ALTER TABLE user_role ADD CONSTRAINT IF NOT EXISTS user_role_role_id_fkey 
            FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE;
            
            -- role_permission constraints
            ALTER TABLE role_permission ADD CONSTRAINT IF NOT EXISTS role_permission_role_id_fkey 
            FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE;
            
            ALTER TABLE role_permission ADD CONSTRAINT IF NOT EXISTS role_permission_permission_id_fkey 
            FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE;
            
            -- driver constraints
            ALTER TABLE driver ADD CONSTRAINT IF NOT EXISTS fk_driver_app_user 
            FOREIGN KEY (app_user_id) REFERENCES app_user(id) ON DELETE CASCADE;
            
            ALTER TABLE driver ADD CONSTRAINT IF NOT EXISTS fk_driver_vehicle 
            FOREIGN KEY (assigned_vehicle_id) REFERENCES vehicle(id) ON DELETE SET NULL;
            
            -- vehicle constraints
            ALTER TABLE vehicle ADD CONSTRAINT IF NOT EXISTS fk_vehicle_driver 
            FOREIGN KEY (assigned_driver_id) REFERENCES driver(id) ON DELETE SET NULL;
            
            -- account_statement constraints
            ALTER TABLE account_statement ADD CONSTRAINT IF NOT EXISTS account_statement_account_id_fkey 
            FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE NO ACTION;
            
            ALTER TABLE account_statement ADD CONSTRAINT IF NOT EXISTS fk_account_statement_account 
            FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE;
            
            ALTER TABLE account_statement ADD CONSTRAINT IF NOT EXISTS fk_account_statement_recon_by 
            FOREIGN KEY (recon_by) REFERENCES app_user(id) ON DELETE SET NULL;
            
            -- account_transaction constraints
            ALTER TABLE account_transaction ADD CONSTRAINT IF NOT EXISTS account_transaction_account_id_fkey 
            FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE NO ACTION;
            
            ALTER TABLE account_transaction ADD CONSTRAINT IF NOT EXISTS account_transaction_account_statement_id_fkey 
            FOREIGN KEY (account_statement_id) REFERENCES account_statement(id) ON DELETE NO ACTION;
            
            -- fuel_source constraints
            ALTER TABLE fuel_source ADD CONSTRAINT IF NOT EXISTS fuel_source_account_id_fkey 
            FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE NO ACTION;
            
            -- fuel_slip constraints
            ALTER TABLE fuel_slip ADD CONSTRAINT IF NOT EXISTS fuel_slip_fuel_source_id_fkey 
            FOREIGN KEY (fuel_source_id) REFERENCES fuel_source(id) ON DELETE NO ACTION;
            
            ALTER TABLE fuel_slip ADD CONSTRAINT IF NOT EXISTS fk_fuel_slip_trip 
            FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE SET NULL;
            
            ALTER TABLE fuel_slip ADD CONSTRAINT IF NOT EXISTS fk_fuel_slip_load 
            FOREIGN KEY (load_id) REFERENCES load(id) ON DELETE SET NULL;
            
            ALTER TABLE fuel_slip ADD CONSTRAINT IF NOT EXISTS fk_fuel_slip_account_statement 
            FOREIGN KEY (account_statement_id) REFERENCES account_statement(id) ON DELETE SET NULL;
            
            -- stock_count constraints
            ALTER TABLE stock_count ADD CONSTRAINT IF NOT EXISTS stock_count_location_id_fkey 
            FOREIGN KEY (location_id) REFERENCES inventory_location(id) ON DELETE NO ACTION;
            
            -- stock_count_line constraints
            ALTER TABLE stock_count_line ADD CONSTRAINT IF NOT EXISTS stock_count_line_stock_count_id_fkey 
            FOREIGN KEY (stock_count_id) REFERENCES stock_count(id) ON DELETE NO ACTION;
            
            ALTER TABLE stock_count_line ADD CONSTRAINT IF NOT EXISTS stock_count_line_product_id_fkey 
            FOREIGN KEY (product_id) REFERENCES inventory_item(id) ON DELETE NO ACTION;
            
            -- stock_movement constraints
            ALTER TABLE stock_movement ADD CONSTRAINT IF NOT EXISTS stock_movement_product_id_fkey 
            FOREIGN KEY (product_id) REFERENCES inventory_item(id) ON DELETE NO ACTION;
            
            ALTER TABLE stock_movement ADD CONSTRAINT IF NOT EXISTS stock_movement_from_location_id_fkey 
            FOREIGN KEY (from_location_id) REFERENCES inventory_location(id) ON DELETE NO ACTION;
            
            ALTER TABLE stock_movement ADD CONSTRAINT IF NOT EXISTS stock_movement_to_location_id_fkey 
            FOREIGN KEY (to_location_id) REFERENCES inventory_location(id) ON DELETE NO ACTION;
            
            -- driver_metrics constraints
            ALTER TABLE driver_metrics ADD CONSTRAINT IF NOT EXISTS driver_metrics_driver_id_fkey 
            FOREIGN KEY (driver_id) REFERENCES driver(id) ON DELETE NO ACTION;
            
            -- vehicle_metrics constraints
            ALTER TABLE vehicle_metrics ADD CONSTRAINT IF NOT EXISTS vehicle_metrics_vehicle_id_fkey 
            FOREIGN KEY (vehicle_id) REFERENCES vehicle(id) ON DELETE NO ACTION;
            
            -- trip constraints
            ALTER TABLE trip ADD CONSTRAINT IF NOT EXISTS trip_vehicle_id_fkey 
            FOREIGN KEY (vehicle_id) REFERENCES vehicle(id) ON DELETE NO ACTION;
            
            ALTER TABLE trip ADD CONSTRAINT IF NOT EXISTS trip_driver_id_fkey 
            FOREIGN KEY (driver_id) REFERENCES driver(id) ON DELETE NO ACTION;
            
            ALTER TABLE trip ADD CONSTRAINT IF NOT EXISTS trip_load_id_fkey 
            FOREIGN KEY (load_id) REFERENCES load(id) ON DELETE NO ACTION;
            
            ALTER TABLE trip ADD CONSTRAINT IF NOT EXISTS fk_trip_load 
            FOREIGN KEY (load_id) REFERENCES load(id) ON DELETE SET NULL;
            
            -- trip_metrics constraints
            ALTER TABLE trip_metrics ADD CONSTRAINT IF NOT EXISTS trip_metrics_trip_id_fkey 
            FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE NO ACTION;
            
            -- invoice constraints
            ALTER TABLE invoice ADD CONSTRAINT IF NOT EXISTS fk_invoice_account 
            FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE CASCADE;
            
            ALTER TABLE invoice ADD CONSTRAINT IF NOT EXISTS fk_invoice_trip 
            FOREIGN KEY (trip_id) REFERENCES trip(id) ON DELETE SET NULL;
            
            ALTER TABLE invoice ADD CONSTRAINT IF NOT EXISTS fk_invoice_load 
            FOREIGN KEY (load_id) REFERENCES load(id) ON DELETE SET NULL;
            
            -- payment constraints
            ALTER TABLE payment ADD CONSTRAINT IF NOT EXISTS payment_account_id_fkey 
            FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE NO ACTION;
            
            -- payment_allocation constraints
            ALTER TABLE payment_allocation ADD CONSTRAINT IF NOT EXISTS payment_allocation_payment_id_fkey 
            FOREIGN KEY (payment_id) REFERENCES payment(id) ON DELETE NO ACTION;
            
            ALTER TABLE payment_allocation ADD CONSTRAINT IF NOT EXISTS payment_allocation_invoice_id_fkey 
            FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE NO ACTION;
            
            -- reconciliation constraints
            ALTER TABLE reconciliation ADD CONSTRAINT IF NOT EXISTS reconciliation_account_id_fkey 
            FOREIGN KEY (account_id) REFERENCES account(id) ON DELETE NO ACTION;
        """);
    }

    /* ===================== INDEX CREATION ===================== */
    private void createIndexes() {
        logger.info("Creating indexes...");

        jdbcTemplate.execute("""
            -- User indexes
            CREATE INDEX IF NOT EXISTS idx_user_email ON app_user(email);
            CREATE INDEX IF NOT EXISTS idx_user_username ON app_user(username);
            CREATE INDEX IF NOT EXISTS idx_user_role_user ON user_role(user_id);
            CREATE INDEX IF NOT EXISTS idx_user_role_role ON user_role(role_id);
            CREATE INDEX IF NOT EXISTS idx_role_permission_role ON role_permission(role_id);
            
            -- Account indexes
            CREATE INDEX IF NOT EXISTS idx_account_type ON account(type);
            CREATE INDEX IF NOT EXISTS idx_account_status ON account(status);
            CREATE INDEX IF NOT EXISTS idx_account_number ON account(account_number);
            
            -- Driver indexes
            CREATE INDEX IF NOT EXISTS idx_driver_license ON driver(license_number);
            CREATE INDEX IF NOT EXISTS idx_driver_status ON driver(status);
            CREATE INDEX IF NOT EXISTS idx_driver_vehicle ON driver(assigned_vehicle_id);
            
            -- Vehicle indexes
            CREATE INDEX IF NOT EXISTS idx_vehicle_reg ON vehicle(registration_number);
            CREATE INDEX IF NOT EXISTS idx_vehicle_status ON vehicle(status);
            
            -- Fuel indexes
            CREATE INDEX IF NOT EXISTS idx_fuel_slip_vehicle ON fuel_slip(vehicle_id);
            CREATE INDEX IF NOT EXISTS idx_fuel_slip_driver ON fuel_slip(driver_id);
            CREATE INDEX IF NOT EXISTS idx_fuel_slip_date ON fuel_slip(transaction_date);
            CREATE INDEX IF NOT EXISTS idx_fuel_slip_finalized ON fuel_slip(finalized);
            
            -- Inventory indexes
            CREATE INDEX IF NOT EXISTS idx_inventory_item_sku ON inventory_item(sku);
            CREATE INDEX IF NOT EXISTS idx_inventory_item_category ON inventory_item(category);
            CREATE INDEX IF NOT EXISTS idx_stock_movement_product ON stock_movement(product_id);
            CREATE INDEX IF NOT EXISTS idx_stock_movement_date ON stock_movement(movement_date);
            CREATE INDEX IF NOT EXISTS idx_stock_movement_type ON stock_movement(movement_type);
            
            -- Trip indexes
            CREATE INDEX IF NOT EXISTS idx_trip_number ON trip(trip_number);
            CREATE INDEX IF NOT EXISTS idx_trip_vehicle ON trip(vehicle_id);
            CREATE INDEX IF NOT EXISTS idx_trip_driver ON trip(driver_id);
            CREATE INDEX IF NOT EXISTS idx_trip_dates ON trip(start_date, end_date);
            CREATE INDEX IF NOT EXISTS idx_trip_status ON trip(status);
            
            -- Financial indexes
            CREATE INDEX IF NOT EXISTS idx_account_transaction_date ON account_transaction(transaction_date);
            CREATE INDEX IF NOT EXISTS idx_account_transaction_source ON account_transaction(source_type, source_id);
            CREATE INDEX IF NOT EXISTS idx_account_transaction_account_date ON account_transaction(account_id, transaction_date);
            CREATE INDEX IF NOT EXISTS idx_account_statement_date ON account_statement(statement_date);
            CREATE INDEX IF NOT EXISTS idx_account_statement_account_date ON account_statement(account_id, statement_date);
            
            -- Invoice indexes
            CREATE INDEX IF NOT EXISTS idx_invoice_number ON invoice(invoice_number);
            CREATE INDEX IF NOT EXISTS idx_invoice_date ON invoice(invoice_date);
            CREATE INDEX IF NOT EXISTS idx_invoice_status ON invoice(status);
            CREATE INDEX IF NOT EXISTS idx_invoice_supplier ON invoice(supplier_id);
            
            -- Payment indexes
            CREATE INDEX IF NOT EXISTS idx_payment_date ON payment(payment_date);
            CREATE INDEX IF NOT EXISTS idx_payment_account ON payment(account_id);
            CREATE INDEX IF NOT EXISTS idx_payment_ref ON payment(reference_number);
            CREATE INDEX IF NOT EXISTS idx_payment_status ON payment(status);
            
            -- Reconciliation indexes
            CREATE INDEX IF NOT EXISTS idx_reconciliation_date ON reconciliation(reconciliation_date);
            CREATE INDEX IF NOT EXISTS idx_reconciliation_account ON reconciliation(account_id);
            CREATE INDEX IF NOT EXISTS idx_reconciliation_status ON reconciliation(status);
            
            -- Metrics indexes
            CREATE INDEX IF NOT EXISTS idx_driver_metrics_date ON driver_metrics(metric_date);
            CREATE INDEX IF NOT EXISTS idx_driver_metrics_driver_date ON driver_metrics(driver_id, metric_date);
            CREATE INDEX IF NOT EXISTS idx_vehicle_metrics_date ON vehicle_metrics(metric_date);
            CREATE INDEX IF NOT EXISTS idx_vehicle_metrics_vehicle_date ON vehicle_metrics(vehicle_id, metric_date);
        """);
    }

    /* ===================== SEED DATA ===================== */
    private void seedData() {
        logger.info("Seeding data...");

        jdbcTemplate.execute("""
            -- ==================== SEED ROLES ====================
            INSERT INTO role (name, description)
            SELECT 'SUPER_ADMIN', 'Full system access'
            WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='SUPER_ADMIN');
            
            INSERT INTO role (name, description)
            SELECT 'DISPATCHER', 'Manages trips and loads'
            WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='DISPATCHER');
            
            INSERT INTO role (name, description)
            SELECT 'FINANCE', 'Manages invoices and payments'
            WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='FINANCE');
            
            INSERT INTO role (name, description)
            SELECT 'DRIVER', 'Vehicle operator'
            WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='DRIVER');
            
            INSERT INTO role (name, description)
            SELECT 'MANAGER', 'Department manager'
            WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='MANAGER');
            
            INSERT INTO role (name, description)
            SELECT 'INVENTORY', 'Manages inventory'
            WHERE NOT EXISTS (SELECT 1 FROM role WHERE name='INVENTORY');
            
            -- ==================== SEED PERMISSIONS ====================
            INSERT INTO permission (resource, action)
            SELECT 'TRIP', 'CREATE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='TRIP' AND action='CREATE');
            
            INSERT INTO permission (resource, action)
            SELECT 'TRIP', 'VIEW'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='TRIP' AND action='VIEW');
            
            INSERT INTO permission (resource, action)
            SELECT 'TRIP', 'EDIT'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='TRIP' AND action='EDIT');
            
            INSERT INTO permission (resource, action)
            SELECT 'TRIP', 'DELETE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='TRIP' AND action='DELETE');
            
            INSERT INTO permission (resource, action)
            SELECT 'FUEL', 'CREATE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='FUEL' AND action='CREATE');
            
            INSERT INTO permission (resource, action)
            SELECT 'FUEL', 'VIEW'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='FUEL' AND action='VIEW');
            
            INSERT INTO permission (resource, action)
            SELECT 'FUEL', 'APPROVE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='FUEL' AND action='APPROVE');
            
            INSERT INTO permission (resource, action)
            SELECT 'INVOICE', 'CREATE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='INVOICE' AND action='CREATE');
            
            INSERT INTO permission (resource, action)
            SELECT 'INVOICE', 'VIEW'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='INVOICE' AND action='VIEW');
            
            INSERT INTO permission (resource, action)
            SELECT 'INVOICE', 'APPROVE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='INVOICE' AND action='APPROVE');
            
            INSERT INTO permission (resource, action)
            SELECT 'USER', 'MANAGE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='USER' AND action='MANAGE');
            
            INSERT INTO permission (resource, action)
            SELECT 'ACCOUNT', 'MANAGE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='ACCOUNT' AND action='MANAGE');
            
            INSERT INTO permission (resource, action)
            SELECT 'RECONCILIATION', 'PERFORM'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='RECONCILIATION' AND action='PERFORM');
            
            INSERT INTO permission (resource, action)
            SELECT 'INVENTORY', 'MANAGE'
            WHERE NOT EXISTS (SELECT 1 FROM permission WHERE resource='INVENTORY' AND action='MANAGE');
            
            -- ==================== SEED APP USERS ====================
            -- Admin user (password: Admin123)
            INSERT INTO app_user (email, password_hash, enabled, username, full_name, phone_number, designation)
            SELECT 'admin@logistics.local', '$2a$12$Q7Qz8x.tW8DOdki1Py3mau397uoGzAWXWVHuupcG.ByojTtuNRJ82', true, 'admin', 'System Administrator', '+27123456789', 'System Admin'
            WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email='admin@logistics.local');
            
            -- Dispatcher user (password: Dispatcher123)
            INSERT INTO app_user (email, password_hash, enabled, username, full_name, phone_number, designation)
            SELECT 'dispatcher@logistics.local', '$2a$12$Lq9VZ8KQ3vOeE6YtNpA9Be.5M2JkL8RzT1WqS3X4Y7Z6A8B0C1D2E3F4', true, 'dispatcher', 'Dispatch Manager', '+27123456788', 'Dispatcher'
            WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email='dispatcher@logistics.local');
            
            -- Finance user (password: Finance123)
            INSERT INTO app_user (email, password_hash, enabled, username, full_name, phone_number, designation)
            SELECT 'finance@logistics.local', '$2a$12$M5N6O7P8Q9R0S1T2U3V4W5X6Y7Z8A9B0C1D2E3F4G5H6I7J8K9L0M1N2', true, 'finance', 'Finance Manager', '+27123456787', 'Finance Officer'
            WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email='finance@logistics.local');
            
            -- Driver user (password: Driver123)
            INSERT INTO app_user (email, password_hash, enabled, username, full_name, phone_number, license_number, license_expiry, designation)
            SELECT 'driver@logistics.local', '$2a$12$N3O4P5Q6R7S8T9U0V1W2X3Y4Z5A6B7C8D9E0F1G2H3I4J5K6L7M8N9O0', true, 'driver1', 'John Doe', '+27123456786', 'DL123456789', '2026-12-31', 'Driver'
            WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE email='driver@logistics.local');
            
            -- ==================== ASSIGN USER ROLES ====================
            -- Admin gets SUPER_ADMIN
            INSERT INTO user_role (user_id, role_id)
            SELECT u.id, r.id
            FROM app_user u, role r
            WHERE u.email = 'admin@logistics.local'
            AND r.name = 'SUPER_ADMIN'
            AND NOT EXISTS (
                SELECT 1 FROM user_role ur
                WHERE ur.user_id = u.id AND ur.role_id = r.id
            );
            
            -- Dispatcher gets DISPATCHER
            INSERT INTO user_role (user_id, role_id)
            SELECT u.id, r.id
            FROM app_user u, role r
            WHERE u.email = 'dispatcher@logistics.local'
            AND r.name = 'DISPATCHER'
            AND NOT EXISTS (
                SELECT 1 FROM user_role ur
                WHERE ur.user_id = u.id AND ur.role_id = r.id
            );
            
            -- Finance gets FINANCE
            INSERT INTO user_role (user_id, role_id)
            SELECT u.id, r.id
            FROM app_user u, role r
            WHERE u.email = 'finance@logistics.local'
            AND r.name = 'FINANCE'
            AND NOT EXISTS (
                SELECT 1 FROM user_role ur
                WHERE ur.user_id = u.id AND ur.role_id = r.id
            );
            
            -- Driver gets DRIVER
            INSERT INTO user_role (user_id, role_id)
            SELECT u.id, r.id
            FROM app_user u, role r
            WHERE u.email = 'driver@logistics.local'
            AND r.name = 'DRIVER'
            AND NOT EXISTS (
                SELECT 1 FROM user_role ur
                WHERE ur.user_id = u.id AND ur.role_id = r.id
            );
            
            -- ==================== SEED ACCOUNTS ====================
            INSERT INTO account (name, type, provider, balance, currency, account_number, credit_limit, status, contact_person, contact_phone) 
            SELECT 'BP Fuel Account', 'FUEL', 'BP', 50000.00, 'ZAR', 'ACC-FUEL-BP-001', 100000.00, 'ACTIVE', 'Mike Johnson', '+27112233445'
            WHERE NOT EXISTS (SELECT 1 FROM account WHERE name='BP Fuel Account');
            
            INSERT INTO account (name, type, provider, balance, currency, account_number, credit_limit, status, contact_person, contact_phone) 
            SELECT 'FNB Fleet Card', 'FUEL', 'FNB', 25000.00, 'ZAR', 'ACC-FUEL-FNB-002', 50000.00, 'ACTIVE', 'Sarah Williams', '+27113344556'
            WHERE NOT EXISTS (SELECT 1 FROM account WHERE name='FNB Fleet Card');
            
            INSERT INTO account (name, type, provider, balance, currency, account_number, status, contact_person, contact_phone) 
            SELECT 'Yard Diesel Tank', 'FUEL', 'INTERNAL', 10000.00, 'ZAR', 'ACC-FUEL-YARD-003', 'ACTIVE', 'Yard Manager', '+27114455667'
            WHERE NOT EXISTS (SELECT 1 FROM account WHERE name='Yard Diesel Tank');
            
            INSERT INTO account (name, type, provider, balance, currency, account_number, credit_limit, status, contact_person, contact_phone) 
            SELECT 'Standard Bank', 'BANK', 'Standard Bank', 100000.00, 'ZAR', 'ACC-BANK-STD-004', 200000.00, 'ACTIVE', 'Bank Manager', '+27115566778'
            WHERE NOT EXISTS (SELECT 1 FROM account WHERE name='Standard Bank');
            
            INSERT INTO account (name, type, provider, balance, currency, account_number, status, contact_person, contact_phone) 
            SELECT 'Petty Cash', 'CASH', 'INTERNAL', 5000.00, 'ZAR', 'ACC-CASH-PETTY-005', 'ACTIVE', 'Finance Manager', '+27116677889'
            WHERE NOT EXISTS (SELECT 1 FROM account WHERE name='Petty Cash');
            
            -- ==================== SEED SUPPLIERS ====================
            INSERT INTO suppliers (name, contact_person, email, phone, address, tax_number, status)
            SELECT 'BP South Africa', 'Mike Johnson', 'supplier@bp.co.za', '+27112233445', 'BP House, Sandton, Johannesburg', '1234567890', 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name='BP South Africa');
            
            INSERT INTO suppliers (name, contact_person, email, phone, address, tax_number, status)
            SELECT 'Shell SA', 'Sarah Williams', 'accounts@shell.co.za', '+27113344556', 'Shell Building, Cape Town', '9876543210', 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name='Shell SA');
            
            INSERT INTO suppliers (name, contact_person, email, phone, address, tax_number, status)
            SELECT 'Midas Parts', 'David Brown', 'parts@midas.co.za', '+27114455667', '123 Parts St, Pretoria', '4567890123', 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM suppliers WHERE name='Midas Parts');
            
            -- ==================== SEED DRIVERS ====================
            INSERT INTO driver (first_name, last_name, license_number, license_expiry, phone_number, email, status, hire_date, license_type, app_user_id, employment_type, shift_pattern, training_completed)
            SELECT 'John', 'Doe', 'DL123456789', '2026-12-31', '+27123456789', 'john.doe@company.com', 'ACTIVE', '2023-01-15', 'CODE_14', 
                   (SELECT id FROM app_user WHERE email='driver@logistics.local'), 'FULL_TIME', 'DAY_SHIFT', true
            WHERE NOT EXISTS (SELECT 1 FROM driver WHERE license_number='DL123456789');
            
            INSERT INTO driver (first_name, last_name, license_number, license_expiry, phone_number, email, status, hire_date, license_type, employment_type, shift_pattern, training_completed)
            SELECT 'Jane', 'Smith', 'DL987654321', '2025-06-30', '+27876543210', 'jane.smith@company.com', 'ACTIVE', '2023-03-20', 'CODE_14', 'FULL_TIME', 'NIGHT_SHIFT', true
            WHERE NOT EXISTS (SELECT 1 FROM driver WHERE license_number='DL987654321');
            
            INSERT INTO driver (first_name, last_name, license_number, license_expiry, phone_number, email, status, hire_date, license_type, employment_type, shift_pattern, training_completed)
            SELECT 'Mike', 'Johnson', 'DL456789123', '2025-09-15', '+27112233445', 'mike.johnson@company.com', 'ACTIVE', '2023-06-10', 'CODE_10', 'FULL_TIME', 'DAY_SHIFT', true
            WHERE NOT EXISTS (SELECT 1 FROM driver WHERE license_number='DL456789123');
            
            -- ==================== SEED VEHICLES ====================
            INSERT INTO vehicle (registration_number, vin, make, model, year, fuel_type, current_mileage, status)
            SELECT 'ABC123GP', '1HGCM82633A123456', 'Volvo', 'FH16', 2022, 'DIESEL', 15000.50, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE registration_number='ABC123GP');
            
            INSERT INTO vehicle (registration_number, vin, make, model, year, fuel_type, current_mileage, status)
            SELECT 'DEF456GP', '1FDXF46F69EA98765', 'Mercedes', 'Actros', 2021, 'DIESEL', 45000.75, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE registration_number='DEF456GP');
            
            INSERT INTO vehicle (registration_number, vin, make, model, year, fuel_type, current_mileage, status)
            SELECT 'GHI789GP', '1N6BD06T45C456789', 'Scania', 'R500', 2023, 'DIESEL', 8000.25, 'ACTIVE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE registration_number='GHI789GP');
            
            INSERT INTO vehicle (registration_number, vin, make, model, year, fuel_type, current_mileage, status)
            SELECT 'JKL012GP', '1HTMMAAL07H123987', 'MAN', 'TGX', 2020, 'DIESEL', 75000.00, 'MAINTENANCE'
            WHERE NOT EXISTS (SELECT 1 FROM vehicle WHERE registration_number='JKL012GP');
            
            -- ==================== ASSIGN DRIVERS TO VEHICLES ====================
            UPDATE vehicle SET assigned_driver_id = (SELECT id FROM driver WHERE license_number = 'DL123456789') 
            WHERE registration_number = 'ABC123GP' AND assigned_driver_id IS NULL;
            
            UPDATE vehicle SET assigned_driver_id = (SELECT id FROM driver WHERE license_number = 'DL987654321') 
            WHERE registration_number = 'DEF456GP' AND assigned_driver_id IS NULL;
            
            UPDATE driver SET assigned_vehicle_id = (SELECT id FROM vehicle WHERE registration_number = 'ABC123GP') 
            WHERE license_number = 'DL123456789' AND assigned_vehicle_id IS NULL;
            
            UPDATE driver SET assigned_vehicle_id = (SELECT id FROM vehicle WHERE registration_number = 'DEF456GP') 
            WHERE license_number = 'DL987654321' AND assigned_vehicle_id IS NULL;
            
            -- ==================== SEED FUEL SOURCES ====================
            INSERT INTO fuel_source (name, source_type, account_id)
            SELECT 'Yard Fill', 'YARD', a.id 
            FROM account a 
            WHERE a.name='Yard Diesel Tank'
            AND NOT EXISTS (SELECT 1 FROM fuel_source WHERE name='Yard Fill');
            
            INSERT INTO fuel_source (name, source_type, account_id)
            SELECT 'BP Station 123', 'STATION', a.id 
            FROM account a 
            WHERE a.name='BP Fuel Account'
            AND NOT EXISTS (SELECT 1 FROM fuel_source WHERE name='BP Station 123');
            
            INSERT INTO fuel_source (name, source_type, account_id)
            SELECT 'Shell Umhlanga', 'STATION', a.id 
            FROM account a 
            WHERE a.name='FNB Fleet Card'
            AND NOT EXISTS (SELECT 1 FROM fuel_source WHERE name='Shell Umhlanga');
            
            -- ==================== SEED LOADS ====================
            INSERT INTO load (load_number, description, weight_kg, volume_cubic_m, status)
            SELECT 'LOAD2024001', 'Electronics and Appliances', 5000.00, 25.00, 'LOADED'
            WHERE NOT EXISTS (SELECT 1 FROM load WHERE load_number='LOAD2024001');
            
            INSERT INTO load (load_number, description, weight_kg, volume_cubic_m, status)
            SELECT 'LOAD2024002', 'General Merchandise', 8000.00, 40.00, 'PENDING'
            WHERE NOT EXISTS (SELECT 1 FROM load WHERE load_number='LOAD2024002');
            
            INSERT INTO load (load_number, description, weight_kg, volume_cubic_m, status)
            SELECT 'LOAD2024003', 'Building Materials', 12000.00, 60.00, 'UNLOADED'
            WHERE NOT EXISTS (SELECT 1 FROM load WHERE load_number='LOAD2024003');
            
            INSERT INTO load (load_number, description, weight_kg, volume_cubic_m, status)
            SELECT 'LOAD2024004', 'Agricultural Products', 7000.00, 35.00, 'LOADED'
            WHERE NOT EXISTS (SELECT 1 FROM load WHERE load_number='LOAD2024004');
            
            -- ==================== SEED TRIPS ====================
            INSERT INTO trip (trip_number, vehicle_id, driver_id, load_id, start_date, end_date, origin_location, destination_location, status)
            SELECT 'TRP2024001', v.id, d.id, l.id, '2024-01-15 08:00:00', '2024-01-16 18:00:00', 'Johannesburg', 'Durban', 'COMPLETED'
            FROM vehicle v, driver d, load l
            WHERE v.registration_number='ABC123GP' AND d.license_number='DL123456789' AND l.load_number='LOAD2024001'
            AND NOT EXISTS (SELECT 1 FROM trip WHERE trip_number='TRP2024001');
            
            INSERT INTO trip (trip_number, vehicle_id, driver_id, load_id, start_date, end_date, origin_location, destination_location, status)
            SELECT 'TRP2024002', v.id, d.id, l.id, '2024-01-17 07:30:00', '2024-01-18 17:45:00', 'Pretoria', 'Cape Town', 'IN_PROGRESS'
            FROM vehicle v, driver d, load l
            WHERE v.registration_number='DEF456GP' AND d.license_number='DL987654321' AND l.load_number='LOAD2024002'
            AND NOT EXISTS (SELECT 1 FROM trip WHERE trip_number='TRP2024002');
            
            INSERT INTO trip (trip_number, vehicle_id, driver_id, load_id, start_date, origin_location, destination_location, status)
            SELECT 'TRP2024003', v.id, d.id, l.id, '2024-01-20 06:00:00', 'Bloemfontein', 'Port Elizabeth', 'PENDING'
            FROM vehicle v, driver d, load l
            WHERE v.registration_number='GHI789GP' AND d.license_number='DL456789123' AND l.load_number='LOAD2024004'
            AND NOT EXISTS (SELECT 1 FROM trip WHERE trip_number='TRP2024003');
            
            -- ==================== SEED FUEL SLIPS ====================
            INSERT INTO fuel_slip (slip_number, transaction_date, vehicle_id, driver_id, fuel_source_id, quantity, unit_price, total_amount, odometer_reading, location, station_name, pump_number, notes, finalized)
            SELECT 'FS2024001', '2024-01-15 10:30:00', v.id, d.id, fs.id, 250.00, 22.50, 5625.00, 15250.75, 'Johannesburg', 'BP Station 123', 'Pump 5', 'Morning fill before trip', true
            FROM vehicle v, driver d, fuel_source fs
            WHERE v.registration_number='ABC123GP' AND d.license_number='DL123456789' 
            AND fs.name='BP Station 123'
            AND NOT EXISTS (SELECT 1 FROM fuel_slip WHERE slip_number='FS2024001');
            
            INSERT INTO fuel_slip (slip_number, transaction_date, vehicle_id, driver_id, fuel_source_id, quantity, unit_price, total_amount, odometer_reading, location, station_name, pump_number, notes, finalized)
            SELECT 'FS2024002', '2024-01-16 15:45:00', v.id, d.id, fs.id, 300.00, 23.10, 6930.00, 30500.25, 'Durban', 'Shell Umhlanga', 'Pump 8', 'Return trip refuel', true
            FROM vehicle v, driver d, fuel_source fs
            WHERE v.registration_number='ABC123GP' AND d.license_number='DL123456789' 
            AND fs.name='Shell Umhlanga'
            AND NOT EXISTS (SELECT 1 FROM fuel_slip WHERE slip_number='FS2024002');
            
            INSERT INTO fuel_slip (slip_number, transaction_date, vehicle_id, driver_id, fuel_source_id, quantity, unit_price, total_amount, odometer_reading, location, station_name, pump_number, notes, finalized)
            SELECT 'FS2024003', '2024-01-17 12:15:00', v.id, d.id, fs.id, 200.00, 22.80, 4560.00, 45250.50, 'Pretoria', 'BP Station 123', 'Pump 3', 'Start of Cape Town trip', true
            FROM vehicle v, driver d, fuel_source fs
            WHERE v.registration_number='DEF456GP' AND d.license_number='DL987654321' 
            AND fs.name='BP Station 123'
            AND NOT EXISTS (SELECT 1 FROM fuel_slip WHERE slip_number='FS2024003');
            
            -- ==================== SEED INVENTORY ====================
            INSERT INTO inventory_location (name, type, address) 
            SELECT 'Main Warehouse', 'WAREHOUSE', '123 Main St, Johannesburg'
            WHERE NOT EXISTS (SELECT 1 FROM inventory_location WHERE name='Main Warehouse');
            
            INSERT INTO inventory_location (name, type, address) 
            SELECT 'Yard Storage', 'YARD', '456 Yard Ave, Pretoria'
            WHERE NOT EXISTS (SELECT 1 FROM inventory_location WHERE name='Yard Storage');
            
            INSERT INTO inventory_location (name, type, address) 
            SELECT 'Service Bay', 'SITE', 'Service Center, Johannesburg'
            WHERE NOT EXISTS (SELECT 1 FROM inventory_location WHERE name='Service Bay');
            
            INSERT INTO inventory_item (sku, name, description, unit, unit_cost, category)
            SELECT 'ENG-OIL-5W30', 'Engine Oil 5W-30', 'Synthetic Engine Oil 5W-30 Grade', 'LITER', 150.00, 'LUBRICANTS'
            WHERE NOT EXISTS (SELECT 1 FROM inventory_item WHERE sku='ENG-OIL-5W30');
            
            INSERT INTO inventory_item (sku, name, description, unit, unit_cost, category)
            SELECT 'FIL-AIR-123', 'Air Filter', 'Heavy Duty Air Filter for Trucks', 'EA', 450.00, 'FILTERS'
            WHERE NOT EXISTS (SELECT 1 FROM inventory_item WHERE sku='FIL-AIR-123');
            
            INSERT INTO inventory_item (sku, name, description, unit, unit_cost, category)
            SELECT 'TYR-MIC-185R14', 'Michelin Tire 185R14', 'Commercial Vehicle Tire', 'EA', 2500.00, 'TYRES'
            WHERE NOT EXISTS (SELECT 1 FROM inventory_item WHERE sku='TYR-MIC-185R14');
            
            INSERT INTO inventory_item (sku, name, description, unit, unit_cost, category)
            SELECT 'BRAKE-PAD-FRONT', 'Brake Pads (Front)', 'Heavy Duty Brake Pads', 'SET', 1200.00, 'BRAKES'
            WHERE NOT EXISTS (SELECT 1 FROM inventory_item WHERE sku='BRAKE-PAD-FRONT');
            
            -- ==================== SEED STOCK MOVEMENTS ====================
            INSERT INTO stock_movement (movement_date, product_id, from_location_id, to_location_id, quantity, movement_type, reference_number, reference_type, notes)
            SELECT '2024-01-10 09:00:00', p.id, NULL, l.id, 100, 'IN', 'PO-001', 'PURCHASE_ORDER', 'Initial stock'
            FROM inventory_item p, inventory_location l
            WHERE p.sku = 'ENG-OIL-5W30' AND l.name = 'Main Warehouse'
            AND NOT EXISTS (SELECT 1 FROM stock_movement WHERE reference_number='PO-001');
            
            INSERT INTO stock_movement (movement_date, product_id, from_location_id, to_location_id, quantity, movement_type, reference_number, reference_type, notes)
            SELECT '2024-01-12 14:30:00', p.id, w.id, y.id, 20, 'OUT', 'TRF-001', 'TRANSFER', 'Transfer to yard'
            FROM inventory_item p, inventory_location w, inventory_location y
            WHERE p.sku = 'ENG-OIL-5W30' AND w.name = 'Main Warehouse' AND y.name = 'Yard Storage'
            AND NOT EXISTS (SELECT 1 FROM stock_movement WHERE reference_number='TRF-001');
            
            -- ==================== SEED INVOICES ====================
            INSERT INTO invoice (invoice_number, supplier_id, account_id, invoice_date, due_date, total_amount, tax_amount, net_amount, status, description)
            SELECT 'INV2024001', s.id, a.id, '2024-01-05', '2024-02-05', 15000.00, 2250.00, 17250.00, 'PENDING', 'Monthly fuel invoice - January'
            FROM suppliers s, account a
            WHERE s.name='BP South Africa' AND a.name='BP Fuel Account'
            AND NOT EXISTS (SELECT 1 FROM invoice WHERE invoice_number='INV2024001');
            
            INSERT INTO invoice (invoice_number, supplier_id, account_id, invoice_date, due_date, total_amount, tax_amount, net_amount, status, description)
            SELECT 'INV2024002', s.id, a.id, '2024-01-10', '2024-02-10', 8500.00, 1275.00, 9775.00, 'PAID', 'Vehicle maintenance parts'
            FROM suppliers s, account a
            WHERE s.name='Shell SA' AND a.name='Standard Bank'
            AND NOT EXISTS (SELECT 1 FROM invoice WHERE invoice_number='INV2024002');
            
            INSERT INTO invoice (invoice_number, supplier_id, account_id, trip_id, invoice_date, due_date, total_amount, tax_amount, net_amount, status, description)
            SELECT 'INV2024003', s.id, a.id, t.id, '2024-01-18', '2024-02-18', 12500.00, 1875.00, 14375.00, 'PENDING', 'Trip related expenses'
            FROM suppliers s, account a, trip t
            WHERE s.name='Midas Parts' AND a.name='Standard Bank' AND t.trip_number='TRP2024001'
            AND NOT EXISTS (SELECT 1 FROM invoice WHERE invoice_number='INV2024003');
            
            -- ==================== SEED PAYMENTS ====================
            INSERT INTO payment (payment_date, account_id, amount, currency, payment_method, reference_number, status, notes)
            SELECT '2024-01-20 10:00:00', a.id, 12550.00, 'ZAR', 'EFT', 'PAY2024001', 'POSTED', 'Payment for INV2024001 - partial'
            FROM account a
            WHERE a.name='BP Fuel Account'
            AND NOT EXISTS (SELECT 1 FROM payment WHERE reference_number='PAY2024001');
            
            INSERT INTO payment (payment_date, account_id, amount, currency, payment_method, reference_number, status, notes)
            SELECT '2024-01-12 14:30:00', a.id, 9775.00, 'ZAR', 'EFT', 'PAY2024002', 'POSTED', 'Full payment for INV2024002'
            FROM account a
            WHERE a.name='Standard Bank'
            AND NOT EXISTS (SELECT 1 FROM payment WHERE reference_number='PAY2024002');
            
            -- ==================== SEED PAYMENT ALLOCATIONS ====================
            INSERT INTO payment_allocation (payment_id, invoice_id, amount, allocation_date, notes)
            SELECT p.id, i.id, 12550.00, '2024-01-20', 'Partial payment for January fuel invoice'
            FROM payment p, invoice i
            WHERE p.reference_number='PAY2024001' AND i.invoice_number='INV2024001'
            AND NOT EXISTS (SELECT 1 FROM payment_allocation WHERE payment_id=p.id AND invoice_id=i.id);
            
            INSERT INTO payment_allocation (payment_id, invoice_id, amount, allocation_date, notes)
            SELECT p.id, i.id, 9775.00, '2024-01-12', 'Full payment for maintenance invoice'
            FROM payment p, invoice i
            WHERE p.reference_number='PAY2024002' AND i.invoice_number='INV2024002'
            AND NOT EXISTS (SELECT 1 FROM payment_allocation WHERE payment_id=p.id AND invoice_id=i.id);
            
            -- ==================== SEED ACCOUNT STATEMENTS ====================
            INSERT INTO account_statement (account_id, statement_date, period_start, period_end, opening_balance, closing_balance, total_debits, total_credits, status, currency)
            SELECT a.id, '2024-01-31', '2024-01-01', '2024-01-31', 50000.00, 37500.00, 12500.00, 0.00, 'CLOSED', 'ZAR'
            FROM account a
            WHERE a.name='BP Fuel Account'
            AND NOT EXISTS (SELECT 1 FROM account_statement WHERE account_id=a.id AND statement_date='2024-01-31');
            
            INSERT INTO account_statement (account_id, statement_date, period_start, period_end, opening_balance, closing_balance, total_debits, total_credits, status, currency)
            SELECT a.id, '2024-01-31', '2024-01-01', '2024-01-31', 100000.00, 90225.00, 9775.00, 0.00, 'CLOSED', 'ZAR'
            FROM account a
            WHERE a.name='Standard Bank'
            AND NOT EXISTS (SELECT 1 FROM account_statement WHERE account_id=a.id AND statement_date='2024-01-31');
            
            -- ==================== SEED ACCOUNT TRANSACTIONS ====================
            -- Fuel slip transactions
            INSERT INTO account_transaction (account_id, transaction_date, amount, direction, source_type, source_id, description, currency, account_statement_id)
            SELECT a.id, fs.transaction_date, fs.total_amount, 'DEBIT', 'FUEL_SLIP', fs.id, 'Fuel purchase - ' || fs.slip_number, 'ZAR', ast.id
            FROM account a, fuel_slip fs, account_statement ast
            WHERE a.name='BP Fuel Account' AND fs.slip_number='FS2024001' 
            AND ast.account_id = a.id AND ast.statement_date = '2024-01-31'
            AND NOT EXISTS (SELECT 1 FROM account_transaction WHERE source_type='FUEL_SLIP' AND source_id=fs.id);
            
            INSERT INTO account_transaction (account_id, transaction_date, amount, direction, source_type, source_id, description, currency, account_statement_id)
            SELECT a.id, fs.transaction_date, fs.total_amount, 'DEBIT', 'FUEL_SLIP', fs.id, 'Fuel purchase - ' || fs.slip_number, 'ZAR', ast.id
            FROM account a, fuel_slip fs, account_statement ast
            WHERE a.name='FNB Fleet Card' AND fs.slip_number='FS2024002' 
            AND ast.account_id = a.id AND ast.statement_date = '2024-01-31'
            AND NOT EXISTS (SELECT 1 FROM account_transaction WHERE source_type='FUEL_SLIP' AND source_id=fs.id);
            
            INSERT INTO account_transaction (account_id, transaction_date, amount, direction, source_type, source_id, description, currency, account_statement_id)
            SELECT a.id, fs.transaction_date, fs.total_amount, 'DEBIT', 'FUEL_SLIP', fs.id, 'Fuel purchase - ' || fs.slip_number, 'ZAR', ast.id
            FROM account a, fuel_slip fs, account_statement ast
            WHERE a.name='BP Fuel Account' AND fs.slip_number='FS2024003' 
            AND ast.account_id = a.id AND ast.statement_date = '2024-01-31'
            AND NOT EXISTS (SELECT 1 FROM account_transaction WHERE source_type='FUEL_SLIP' AND source_id=fs.id);
            
            -- Payment transactions
            INSERT INTO account_transaction (account_id, transaction_date, amount, direction, source_type, source_id, description, currency, account_statement_id)
            SELECT a.id, p.payment_date, p.amount, 'CREDIT', 'PAYMENT', p.id, 'Payment - ' || p.reference_number, 'ZAR', ast.id
            FROM account a, payment p, account_statement ast
            WHERE a.name='BP Fuel Account' AND p.reference_number='PAY2024001' 
            AND ast.account_id = a.id AND ast.statement_date = '2024-01-31'
            AND NOT EXISTS (SELECT 1 FROM account_transaction WHERE source_type='PAYMENT' AND source_id=p.id);
            
            INSERT INTO account_transaction (account_id, transaction_date, amount, direction, source_type, source_id, description, currency, account_statement_id)
            SELECT a.id, p.payment_date, p.amount, 'CREDIT', 'PAYMENT', p.id, 'Payment - ' || p.reference_number, 'ZAR', ast.id
            FROM account a, payment p, account_statement ast
            WHERE a.name='Standard Bank' AND p.reference_number='PAY2024002' 
            AND ast.account_id = a.id AND ast.statement_date = '2024-01-31'
            AND NOT EXISTS (SELECT 1 FROM account_transaction WHERE source_type='PAYMENT' AND source_id=p.id);
            
            -- ==================== SEED RECONCILIATION ====================
            INSERT INTO reconciliation (reconciliation_date, account_id, statement_balance, system_balance, status, notes)
            SELECT '2024-01-31', a.id, 37500.00, 37450.00, 'BALANCED', 'Monthly reconciliation - small variance adjusted'
            FROM account a
            WHERE a.name='BP Fuel Account'
            AND NOT EXISTS (SELECT 1 FROM reconciliation WHERE reconciliation_date='2024-01-31' AND account_id=a.id);
            
            INSERT INTO reconciliation (reconciliation_date, account_id, statement_balance, system_balance, status, notes)
            SELECT '2024-01-31', a.id, 90225.00, 90225.00, 'BALANCED', 'Monthly reconciliation - balanced'
            FROM account a
            WHERE a.name='Standard Bank'
            AND NOT EXISTS (SELECT 1 FROM reconciliation WHERE reconciliation_date='2024-01-31' AND account_id=a.id);
            
            -- ==================== SEED METRICS ====================
            -- Driver metrics
            INSERT INTO driver_metrics (driver_id, metric_date, total_trips, total_distance_km, total_hours, fuel_consumption_liters, average_speed_kmh, safety_score, efficiency_score, idle_time_hours, incident_count, violations_count)
            SELECT d.id, '2024-01-31', 12, 4500.50, 120.25, 1250.75, 75.5, 8.7, 7.9, 15.5, 0, 1
            FROM driver d WHERE d.license_number='DL123456789'
            AND NOT EXISTS (SELECT 1 FROM driver_metrics WHERE driver_id=d.id AND metric_date='2024-01-31');
            
            INSERT INTO driver_metrics (driver_id, metric_date, total_trips, total_distance_km, total_hours, fuel_consumption_liters, average_speed_kmh, safety_score, efficiency_score, idle_time_hours, incident_count, violations_count)
            SELECT d.id, '2024-01-31', 8, 3200.25, 95.75, 980.50, 72.8, 9.2, 8.1, 12.8, 0, 0
            FROM driver d WHERE d.license_number='DL987654321'
            AND NOT EXISTS (SELECT 1 FROM driver_metrics WHERE driver_id=d.id AND metric_date='2024-01-31');
            
            -- Vehicle metrics
            INSERT INTO vehicle_metrics (vehicle_id, metric_date, distance_traveled, fuel_used, fuel_efficiency, maintenance_cost, downtime_hours)
            SELECT v.id, '2024-01-31', 5000.75, 1550.25, 3.23, 12500.00, 8.5
            FROM vehicle v WHERE v.registration_number='ABC123GP'
            AND NOT EXISTS (SELECT 1 FROM vehicle_metrics WHERE vehicle_id=v.id AND metric_date='2024-01-31');
            
            INSERT INTO vehicle_metrics (vehicle_id, metric_date, distance_traveled, fuel_used, fuel_efficiency, maintenance_cost, downtime_hours)
            SELECT v.id, '2024-01-31', 3800.50, 1150.75, 3.30, 8500.00, 5.2
            FROM vehicle v WHERE v.registration_number='DEF456GP'
            AND NOT EXISTS (SELECT 1 FROM vehicle_metrics WHERE vehicle_id=v.id AND metric_date='2024-01-31');
            
            -- Trip metrics
            INSERT INTO trip_metrics (trip_id, total_distance, total_duration, fuel_used)
            SELECT t.id, 580.50, 10.25, 250.00
            FROM trip t WHERE t.trip_number='TRP2024001'
            AND NOT EXISTS (SELECT 1 FROM trip_metrics WHERE trip_id=t.id);
            
            INSERT INTO trip_metrics (trip_id, total_distance, total_duration, fuel_used)
            SELECT t.id, 325.75, 6.5, 180.00
            FROM trip t WHERE t.trip_number='TRP2024002'
            AND NOT EXISTS (SELECT 1 FROM trip_metrics WHERE trip_id=t.id);
            
            INSERT INTO trip_metrics (trip_id, total_distance, total_duration, fuel_used)
            SELECT t.id, 450.00, 8.0, 200.00
            FROM trip t WHERE t.trip_number='TRP2024003'
            AND NOT EXISTS (SELECT 1 FROM trip_metrics WHERE trip_id=t.id);
            
            -- ==================== UPDATE ACCOUNT BALANCES ====================
            -- Update account balances based on transactions
            UPDATE account a SET balance = (
                SELECT COALESCE(SUM(
                    CASE 
                        WHEN at.direction = 'DEBIT' THEN -at.amount 
                        WHEN at.direction = 'CREDIT' THEN at.amount 
                        ELSE 0 
                    END
                ), 0)
                FROM account_transaction at
                WHERE at.account_id = a.id
            ) + a.balance
            WHERE a.name IN ('BP Fuel Account', 'FNB Fleet Card', 'Standard Bank');
        """);

        logger.info("Data seeding completed");
    }
}