package dio.marketplace.catalog;

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
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Banco do módulo de catálogo (produtos), na porta 3308.
 *
 * <p>Mesma estrutura da configuração de registro, mas sem {@code @Primary}: este é
 * o banco secundário, então cada bean se identifica pelo qualificador
 * {@code "Catalog"} e quem injeta precisa repetir o nome.
 *
 * <p>O Flyway é configurado à mão porque a autoconfiguração do Spring Boot só
 * cuida do banco principal quando existem dois.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(
        basePackages = "dio.marketplace.catalog",
        entityManagerFactoryRef = "catalogEntityManagerFactory",
        transactionManagerRef = "catalogTransactionManager")
public class CatalogConfiguration {

    @Qualifier("Catalog")
    @Bean
    @ConfigurationProperties("catalog.datasource")
    public DataSourceProperties catalogDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Qualifier("Catalog")
    @Bean
    @ConfigurationProperties("catalog.datasource.configuration")
    public HikariDataSource catalogDataSource(@Qualifier("Catalog") DataSourceProperties properties) {
        return JpaPersistenceUnits.dataSource(properties);
    }

    @Qualifier("Catalog")
    @Bean
    @ConfigurationProperties("catalog.jpa")
    public JpaProperties catalogJpaProperties() {
        return new JpaProperties();
    }

    /**
     * Migrações do banco de catálogo. Os scripts ficam em {@code db/catalog} e
     * rodam sozinhos quando a aplicação sobe.
     *
     * <p>{@code baselineOnMigrate} existe para bancos que já existiam antes do
     * Flyway: eles são marcados na versão 1 e recebem só as migrações seguintes,
     * sem reexecutar a criação das tabelas.
     */
    @Qualifier("Catalog")
    @Bean(initMethod = "migrate")
    public Flyway catalogFlyway(@Qualifier("Catalog") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/catalog")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();
    }

    /** Depende do Flyway para o Hibernate poder validar o esquema já migrado. */
    @Bean
    @Qualifier("Catalog")
    @DependsOn("catalogFlyway")
    public LocalContainerEntityManagerFactoryBean catalogEntityManagerFactory(
            @Qualifier("Catalog") DataSource dataSource,
            @Qualifier("Catalog") JpaProperties jpaProperties) {
        return JpaPersistenceUnits.entityManagerFactory(
                dataSource, jpaProperties, "dio.marketplace.catalog", "catalog");
    }

    @Qualifier("Catalog")
    @Bean
    public PlatformTransactionManager catalogTransactionManager(
            @Qualifier("Catalog") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return JpaPersistenceUnits.transactionManager(entityManagerFactory);
    }
}
