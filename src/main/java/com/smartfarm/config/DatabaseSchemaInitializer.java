package com.smartfarm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running automatic database schema validation and update...");

        // Ensure users table columns exist
        addColumnSafely("users", "manager_id", "VARCHAR(255)");
        addColumnSafely("users", "max_project_capacity", "INT DEFAULT 4");
        addColumnSafely("users", "created_by_id", "VARCHAR(255)");
        addColumnSafely("users", "email", "VARCHAR(255)");
        addColumnSafely("users", "role", "VARCHAR(255) DEFAULT 'MANAGER'");
        addColumnSafely("users", "status", "VARCHAR(255) DEFAULT 'ACTIVE'");
        addColumnSafely("users", "privileges", "VARCHAR(500)");

        // Ensure projects table columns exist
        addColumnSafely("projects", "manager_id", "VARCHAR(255)");
        addColumnSafely("projects", "supervisor_id", "VARCHAR(255)");

        // Ensure sales table columns exist
        addColumnSafely("sales", "customer_id", "VARCHAR(255)");
        addColumnSafely("sales", "amount_paid", "DECIMAL(19,2)");
        addColumnSafely("sales", "balance_due", "DECIMAL(19,2)");
        addColumnSafely("sales", "payment_mode", "VARCHAR(255) DEFAULT 'CASH'");
        addColumnSafely("sales", "payment_status", "VARCHAR(255) DEFAULT 'PAID_IN_FULL'");

        // Ensure customers table and columns exist
        executeSafely("CREATE TABLE IF NOT EXISTS customers ("
                + "id VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "name VARCHAR(255), "
                + "contact VARCHAR(255) UNIQUE, "
                + "id_number VARCHAR(255) UNIQUE, "
                + "address VARCHAR(255), "
                + "is_active BOOLEAN DEFAULT TRUE, "
                + "credit_limit DECIMAL(19,2) DEFAULT 0, "
                + "total_purchases DECIMAL(19,2) DEFAULT 0, "
                + "total_paid DECIMAL(19,2) DEFAULT 0, "
                + "outstanding_debt DECIMAL(19,2) DEFAULT 0, "
                + "credit_status VARCHAR(255) DEFAULT 'CLEAR', "
                + "category VARCHAR(255) DEFAULT 'General Buyer')");

        addColumnSafely("customers", "credit_limit", "DECIMAL(19,2) DEFAULT 0");
        addColumnSafely("customers", "total_purchases", "DECIMAL(19,2) DEFAULT 0");
        addColumnSafely("customers", "total_paid", "DECIMAL(19,2) DEFAULT 0");
        addColumnSafely("customers", "outstanding_debt", "DECIMAL(19,2) DEFAULT 0");
        addColumnSafely("customers", "credit_status", "VARCHAR(255) DEFAULT 'CLEAR'");
        addColumnSafely("customers", "category", "VARCHAR(255) DEFAULT 'General Buyer'");

        // Migrate any legacy rows from singular 'customer' table if present
        executeSafely("INSERT IGNORE INTO customers (id, name, contact, id_number, address, is_active) "
                + "SELECT id, name, contact, id_number, address, is_active FROM customer");

        // Ensure suppliers and supplier_purchases exist
        executeSafely("CREATE TABLE IF NOT EXISTS suppliers ("
                + "id VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "name VARCHAR(255) NOT NULL, "
                + "contact VARCHAR(255) NOT NULL, "
                + "category VARCHAR(255), "
                + "address VARCHAR(255), "
                + "total_billed DECIMAL(19,2) DEFAULT 0, "
                + "total_paid DECIMAL(19,2) DEFAULT 0, "
                + "outstanding_debt DECIMAL(19,2) DEFAULT 0, "
                + "status VARCHAR(255) DEFAULT 'ACTIVE')");

        executeSafely("CREATE TABLE IF NOT EXISTS supplier_purchases ("
                + "id VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "supplier_id VARCHAR(255) NOT NULL, "
                + "project_id VARCHAR(255), "
                + "item_description VARCHAR(255) NOT NULL, "
                + "invoice_amount DECIMAL(19,2) NOT NULL, "
                + "amount_paid DECIMAL(19,2) DEFAULT 0, "
                + "balance_due DECIMAL(19,2) DEFAULT 0, "
                + "payment_status VARCHAR(255) DEFAULT 'UNPAID', "
                + "purchase_date DATE, "
                + "notes VARCHAR(500))");

        // Ensure employee and labor assignment tables exist
        executeSafely("CREATE TABLE IF NOT EXISTS employee ("
                + "id VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "full_name VARCHAR(255) NOT NULL, "
                + "id_number VARCHAR(255) UNIQUE NOT NULL, "
                + "phone_number VARCHAR(255), "
                + "employment_type VARCHAR(255), "
                + "daily_rate DECIMAL(19,2) DEFAULT 0, "
                + "status VARCHAR(255) DEFAULT 'ACTIVE')");

        executeSafely("CREATE TABLE IF NOT EXISTS activity_labor_assignments ("
                + "id VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "activity_id VARCHAR(255) NOT NULL, "
                + "employee_id VARCHAR(255) NOT NULL, "
                + "days_worked INT DEFAULT 1, "
                + "calculated_wage DECIMAL(19,2) DEFAULT 0, "
                + "assignment_date DATE, "
                + "status VARCHAR(255) DEFAULT 'ASSIGNED')");

        // Ensure join/helper tables exist
        executeSafely("CREATE TABLE IF NOT EXISTS user_assigned_categories ("
                + "user_id VARCHAR(255) NOT NULL, "
                + "category_id VARCHAR(255) NOT NULL, "
                + "PRIMARY KEY (user_id, category_id))");

        executeSafely("CREATE TABLE IF NOT EXISTS password_reset_tokens ("
                + "token VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "user_id VARCHAR(255) NOT NULL, "
                + "expiry_date DATETIME NOT NULL)");

        executeSafely("CREATE TABLE IF NOT EXISTS inventory_usages ("
                + "id VARCHAR(255) NOT NULL PRIMARY KEY, "
                + "inventory_item_id VARCHAR(255), "
                + "project_id VARCHAR(255), "
                + "quantity_used DECIMAL(19,2) NOT NULL, "
                + "usage_date DATE, "
                + "notes VARCHAR(500))");

        log.info("Database schema validation completed.");
    }

    private void addColumnSafely(String tableName, String columnName, String columnType) {
        try {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            log.info("Schema update: added column {}.{}", tableName, columnName);
        } catch (Exception e) {
            // Already exists or table not yet created by Hibernate, safe to proceed
        }
    }

    private void executeSafely(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            // Safe to proceed
        }
    }
}