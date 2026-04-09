package com.sp2i.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

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

        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }

        if (rawUrl.startsWith("postgresql://")) {
            return "jdbc:" + rawUrl;
        }

        if (rawUrl.startsWith("postgres://")) {
            return "jdbc:postgresql://" + rawUrl.substring("postgres://".length());
        }

        return rawUrl;
    }
}
