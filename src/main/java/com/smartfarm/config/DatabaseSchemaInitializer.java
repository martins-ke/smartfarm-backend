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