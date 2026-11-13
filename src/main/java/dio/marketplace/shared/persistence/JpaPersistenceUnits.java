package dio.marketplace.shared.persistence;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.LinkedHashMap;

/**
 * Peças comuns para montar as duas unidades de persistência.
 *
 * <p>O Spring Boot configura sozinho apenas um banco. Como aqui são dois, cada
 * módulo monta a sua conexão, a sua fábrica de entidades e o seu gerenciador de
 * transação na mão. Esse código era igual nos dois módulos e já tinha divergido
 * (um lado recriava as tabelas a cada subida e o outro não), então ficou aqui.
 */
public final class JpaPersistenceUnits {

    private JpaPersistenceUnits() {
    }

    public static HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * @param basePackage     pacote das entidades do módulo. É o que separa os
     *                        dois bancos: cada fábrica enxerga só o seu.
     * @param persistenceUnit nome interno da unidade, que aparece nos logs de
     *                        inicialização.
     */
    public static LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                             JpaProperties jpaProperties,
                                                                             String basePackage,
                                                                             String persistenceUnit) {
        // O mapa é copiado porque o Spring altera esse objeto durante a
        // inicialização, e a cópia evita que um módulo interfira no outro.
        var builder = new EntityManagerFactoryBuilder(
                new HibernateJpaVendorAdapter(),
                x -> new LinkedHashMap<>(jpaProperties.getProperties()),
                null
        );
        return builder
                .dataSource(dataSource)
                .packages(basePackage)
                .persistenceUnit(persistenceUnit)
                .properties(jpaProperties.getProperties())
                .build();
    }

    /**
     * Usa {@code Assert.state} em vez de {@code assert} porque verificações com
     * {@code assert} são removidas ao rodar a aplicação, e aí a falha apareceria
     * só como um NullPointerException confuso.
     */
    public static PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        var factory = entityManagerFactory.getObject();
        Assert.state(factory != null,
                "A unidade de persistência '" + entityManagerFactory.getPersistenceUnitName() + "' não foi inicializada.");
        return new JpaTransactionManager(factory);
    }
}
