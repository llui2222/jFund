package com.xm.jfund.db;

import com.xm.jfund.utils.Parameters;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JFundDBConnectionProvider {

    private static final JFundDBConnectionProvider INSTANCE = new JFundDBConnectionProvider();

    private volatile JFundDB jFundDB;

    public static JFundDBConnectionProvider getInstance() {
        return INSTANCE;
    }

    public static void init(final JFundDB jFundDB) {
        Parameters.requireNonNull(jFundDB);
        INSTANCE.setJFundDB(jFundDB);
    }

    public Connection getConnection() throws SQLException {
        if (jFundDB == null) {
            throw new IllegalStateException("JFundDBConnectionProvider was not initialized");
        }
        return DriverManager.getConnection(jFundDB.getUrl(), jFundDB.getUsername(), jFundDB.getPassword());
    }

    private void setJFundDB(final JFundDB jFundDB) {
        this.jFundDB = jFundDB;
    }
}
