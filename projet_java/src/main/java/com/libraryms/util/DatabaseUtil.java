package com.libraryms.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:sqlite:libraryms.db";

    static {
        try {
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite driver not found", e);
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}