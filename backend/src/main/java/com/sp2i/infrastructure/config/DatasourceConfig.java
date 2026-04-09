package com.sp2i.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Configuration explicite de la DataSource.
 *
 * Pourquoi cette classe existe :
 * - en local, on utilise souvent une URL JDBC classique
 *   ex: jdbc:postgresql://localhost:5432/sp2i_build
 * - sur Render, la variable injectee par le Blueprint pour Postgres
 *   est au format :
 *   postgresql://user:password@host:5432/database
 *
 * Hikari / Spring attendent une URL JDBC.
 * On normalise donc l'URL ici de maniere pedagogique et centralisee.
 */
@Configuration
public class DatasourceConfig {

    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String driverClassName;

    public DatasourceConfig(
            @Value("${spring.datasource.url}") String datasourceUrl,
            @Value("${spring.datasource.username}") String datasourceUsername,
            @Value("${spring.datasource.password}") String datasourcePassword,
            @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}") String driverClassName
    ) {
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.driverClassName = driverClassName;
    }

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(normalizeJdbcUrl(datasourceUrl));
        dataSource.setUsername(datasourceUsername);
        dataSource.setPassword(datasourcePassword);
        return dataSource;
    }

    private String normalizeJdbcUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }

        if (rawUrl.startsWith("jdbc:postgresql://")) {
            return stripCredentialsFromJdbcUrl(rawUrl);
        }

        if (rawUrl.startsWith("postgresql://")) {
            return convertRenderPostgresUrl(rawUrl);
        }

        if (rawUrl.startsWith("postgres://")) {
            return convertRenderPostgresUrl(rawUrl.replaceFirst("^postgres://", "postgresql://"));
        }

        return rawUrl;
    }

    private String convertRenderPostgresUrl(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();

            StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(host);
            if (port > 0) {
                jdbcUrl.append(":").append(port);
            }
            jdbcUrl.append(path);
            return jdbcUrl.toString();
        } catch (IllegalArgumentException exception) {
            return "jdbc:" + rawUrl;
        }
    }

    private String stripCredentialsFromJdbcUrl(String rawUrl) {
        try {
            String normalizedUrl = rawUrl.replaceFirst("^jdbc:", "");
            return convertRenderPostgresUrl(normalizedUrl);
        } catch (IllegalArgumentException exception) {
            return rawUrl;
        }
    }
}
