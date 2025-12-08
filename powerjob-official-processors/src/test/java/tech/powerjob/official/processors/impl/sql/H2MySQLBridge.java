package tech.powerjob.official.processors.impl.sql;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * H2 MySQL Bridge Driver
 *
 * This driver acts as a bridge that converts MySQL JDBC URLs to H2 URLs
 * while maintaining MySQL compatibility mode in H2.
 *
 * Usage: Register this driver and use MySQL JDBC URLs, which will be automatically
 * converted to H2 URLs with MySQL compatibility mode.
 */
public class H2MySQLBridge implements Driver {

    private static final Pattern MYSQL_URL_PATTERN =
        Pattern.compile("jdbc:mysql://([^/]+)/([^?]+)(?:\\?(.*))?");

    private final Driver h2Driver;

    static {
        try {
            // Register this bridge driver
            DriverManager.registerDriver(new H2MySQLBridge());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register H2MySQLBridge", e);
        }
    }

    public H2MySQLBridge() throws SQLException {
        // Load the actual H2 driver
        this.h2Driver = DriverManager.getDriver("jdbc:h2:mem:");
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }

        // Convert MySQL URL to H2 URL
        String h2Url = convertMySQLUrlToH2(url);

        // Create H2 connection with modified properties
        Properties h2Props = new Properties(info);

        // Convert MySQL credentials to H2 credentials
        if (!h2Props.containsKey("user") || h2Props.getProperty("user").equals("root")) {
            h2Props.setProperty("user", "sa");
        }
        if (!h2Props.containsKey("password")) {
            h2Props.setProperty("password", "");
        }

        return h2Driver.connect(h2Url, h2Props);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith("jdbc:mysql:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        String h2Url = convertMySQLUrlToH2(url);
        return h2Driver.getPropertyInfo(h2Url, info);
    }

    @Override
    public int getMajorVersion() {
        return h2Driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return h2Driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return h2Driver.jdbcCompliant();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException {
        return h2Driver.getParentLogger();
    }

    /**
     * Convert MySQL JDBC URL to H2 URL with MySQL compatibility
     */
    private String convertMySQLUrlToH2(String mysqlUrl) {
        Matcher matcher = MYSQL_URL_PATTERN.matcher(mysqlUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid MySQL URL format: " + mysqlUrl);
        }

        String host = matcher.group(1);
        String database = matcher.group(2);
        String params = matcher.group(3);

        // For testing, we'll use in-memory H2 database with initialization
        // In production, you might want to map host to different H2 databases
        String h2Url = String.format("jdbc:h2:mem:%s;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
                                   database.replace("-", "_"));

        // Add MySQL compatibility parameters
        h2Url += ";DB_CLOSE_DELAY=-1"; // Keep database in memory
        h2Url += ";CASE_INSENSITIVE_IDENTIFIERS=TRUE";

        // Add database initialization from classpath script
        h2Url += ";INIT=RUNSCRIPT FROM 'classpath:db_init.sql'";

        // Convert some MySQL-specific parameters to H2 equivalents
        if (params != null) {
            if (params.contains("useUnicode=true")) {
                // H2 uses Unicode by default
            }
            if (params.contains("characterEncoding=UTF-8")) {
                // H2 uses UTF-8 by default
            }
            if (params.contains("serverTimezone=")) {
                // H2 doesn't need timezone specification
            }
        }

        return h2Url;
    }

    /**
     * Utility method to create a test context with MySQL URL that will be bridged to H2
     */
    public static String createMySQLCompatibleH2Url(String database) {
        return String.format("jdbc:mysql://localhost:3306/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai",
                           database);
    }
}