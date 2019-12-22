package com.xm.jfund.db;

import com.xm.jfund.utils.Parameters;

public class JFundDB {
    private final String url;
    private final String username;
    private final String password;

    private JFundDB(final String url, final String username, final String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static JFundDB create(final String url, final String username, final String password) {
        Parameters.requireNonEmpty(url);
        Parameters.requireNonEmpty(username);
        Parameters.requireNonNull(password);
        return new JFundDB(url, username, password);
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
