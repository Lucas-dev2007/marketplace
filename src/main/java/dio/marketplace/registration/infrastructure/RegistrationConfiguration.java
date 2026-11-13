package dio.marketplace.registration.infrastructure;


import com.zaxxer.hikari.HikariDataSource;
import dio.marketplace.shared.persistence.JpaPersistenceUnits;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Banco do módulo de registro (clientes e endereços), na porta 3307.
 *
 * <p>O {@code @Primary} faz deste o banco padrão: é o que o Spring entrega quando
 * alguém pede um {@code DataSource} ou {@code PlatformTransactionManager} sem
 * dizer qual. O catálogo é o secundário e se identifica por qualificador.
 *
 * <p>O {@code @EnableJpaRepositories} limita os repositórios deste banco ao pacote
 * {@code dio.marketplace.registration} — sem isso, um repositório do catálogo
 * poderia cair no banco errado.
 *
 * <p>O Flyway é configurado à mão porque a autoconfiguração do Spring Boot assume
 * um único banco.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = "dio.marketplace.registration",
        entityManagerFactoryRef = "registrationEntityManagerFactory",
        transactionManagerRef = "registrationTransactionManager")
public class RegistrationConfiguration {

    @Primary
    @Bean
    @ConfigurationProperties("registration.datasource")
    public DataSourceProperties registrationDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean
    @ConfigurationProperties("registration.datasource.configuration")
    public HikariDataSource registrationDataSource(DataSourceProperties properties) {
        return JpaPersistenceUnits.dataSource(properties);
    }

    @Primary
    @Bean
    @ConfigurationProperties("registration.jpa")
    public JpaProperties registrationJpaProperties() {
        return new JpaProperties();
    }

    /**
     * Migrações do banco de registro. A V1 cria o esquema inicial e a V2, os
     * pedidos.
     *
     * <p>{@code baselineOnMigrate} atende bancos que já existiam antes do Flyway:
     * eles são marcados na versão 1 e recebem só as migrações seguintes, sem
     * reexecutar a criação das tabelas.
     */
    @Primary
    @Bean(initMethod = "migrate")
    public Flyway registrationFlyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/registration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }

    /** Depende do Flyway para o Hibernate poder validar o esquema já migrado. */
    @Primary
    @Bean
    @DependsOn("registrationFlyway")
    public LocalContainerEntityManagerFactoryBean registrationEntityManagerFactory(DataSource dataSource,
                                                                                   JpaProperties jpaProperties) {
        return JpaPersistenceUnits.entityManagerFactory(
                dataSource, jpaProperties, "dio.marketplace.registration", "registration");
    }

    @Primary
    @Bean
    public PlatformTransactionManager registrationTransactionManager(
            @Qualifier("registrationEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return JpaPersistenceUnits.transactionManager(entityManagerFactory);
    }
}
