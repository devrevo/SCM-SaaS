package com.scm_saas.SCM_SaaS.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Bean
    public DataSource liquibaseDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(dbUrl);
        dataSource.setUser(dbUsername);
        dataSource.setPassword(dbPassword);

        System.out.println("Liquibase DataSource configured with URL: " + dbUrl);
        return dataSource;
    }

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(liquibaseDataSource());
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        liquibase.setShouldRun(true);
        liquibase.setClearCheckSums(true);

        System.out.println("Liquibase configuration loaded successfully.");
        return liquibase;
    }
}
