package dev.authlib.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Configuration management for AuthLib
 */
public class Config {
    private final Dotenv dotenv;

    public final String JWT_SECRET_KEY;
    public final String JWT_ALGORITHM;
    public final int JWT_ACCESS_TOKEN_EXPIRY_MINUTES;
    public final int JWT_REFRESH_TOKEN_EXPIRY_DAYS;

    public final String DATABASE_URL;
    public final String DATABASE_USER;
    public final String DATABASE_PASSWORD;

    public final String SMTP_SERVER;
    public final String SMTP_USERNAME;
    public final String SMTP_PASSWORD;
    public final String SMTP_FROM;

    public final String LOG_LEVEL;

    public Config() {
        this.dotenv = Dotenv.configure().ignoreIfMissing().load();

        JWT_SECRET_KEY = getEnv("JWT_SECRET_KEY", "change-me-in-production");
        JWT_ALGORITHM = getEnv("JWT_ALGORITHM", "HS256");
        JWT_ACCESS_TOKEN_EXPIRY_MINUTES = Integer.parseInt(
            getEnv("JWT_ACCESS_TOKEN_EXPIRY_MINUTES", "15")
        );
        JWT_REFRESH_TOKEN_EXPIRY_DAYS = Integer.parseInt(
            getEnv("JWT_REFRESH_TOKEN_EXPIRY_DAYS", "7")
        );

        DATABASE_URL = getEnv("DATABASE_URL", "jdbc:h2:mem:authlib");
        DATABASE_USER = getEnv("DATABASE_USER", "sa");
        DATABASE_PASSWORD = getEnv("DATABASE_PASSWORD", "");

        SMTP_SERVER = getEnv("SMTP_SERVER", "smtp.gmail.com");
        SMTP_USERNAME = getEnv("SMTP_USERNAME", "");
        SMTP_PASSWORD = getEnv("SMTP_PASSWORD", "");
        SMTP_FROM = getEnv("SMTP_FROM", "noreply@authlib.dev");

        LOG_LEVEL = getEnv("LOG_LEVEL", "INFO");
    }

    /**
     * Validate configuration
     */
    public void validate() {
        if (JWT_SECRET_KEY.equals("change-me-in-production")) {
            throw new IllegalStateException("JWT_SECRET_KEY must be set in production");
        }
        if (DATABASE_URL == null || DATABASE_URL.isEmpty()) {
            throw new IllegalStateException("DATABASE_URL must be set");
        }
    }

    /**
     * Check if running in production
     */
    public boolean isProduction() {
        String mode = getEnv("MODE", "development");
        return "production".equals(mode);
    }

    private String getEnv(String key, String defaultValue) {
        String value = dotenv.get(key);
        if (value != null) {
            return value;
        }
        
        value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
