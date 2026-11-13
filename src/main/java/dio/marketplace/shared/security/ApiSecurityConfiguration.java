package dio.marketplace.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protege a API, que guarda dados pessoais de clientes.
 *
 * <p>Quem autentica é o {@link MarketplaceUserDetailsService}: o administrador
 * configurado no ambiente, o usuário genérico de demonstração e cada cliente
 * cadastrado (com login próprio no banco). Os perfis são:
 *
 * <ul>
 *   <li><b>ADMIN</b> — gerencia produtos e clientes;</li>
 *   <li><b>CLIENTE</b> — consulta o catálogo, vê os próprios dados e cuida dos
 *       próprios pedidos.</li>
 * </ul>
 *
 * <p>A autenticação é HTTP Basic e o servidor não guarda sessão: cada requisição
 * manda usuário e senha, por isso a API precisa ser servida por HTTPS. CSRF fica
 * desligado porque protege navegador com cookie, que aqui não existe.
 */
@Configuration(proxyBeanMethods = false)
public class ApiSecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(requisicoes -> requisicoes
                        // Health e erros precisam ficar abertos: um é do monitoramento,
                        // o outro é para o Spring conseguir montar a resposta de erro.
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
                        // Documentação da API e a loja estática são públicas; os dados
                        // que elas consomem continuam exigindo login.
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/", "/index.html", "/app.js", "/style.css").permitAll()
                        // O cliente precisa consultar o próprio cadastro para comprar.
                        // Esta regra vem antes da regra de ADMIN sobre /clientes/**.
                        .requestMatchers(HttpMethod.GET, "/clientes/eu").authenticated()
                        // Dados pessoais de cliente: só o administrador vê e mexe.
                        .requestMatchers("/customers/**", "/clientes/**").hasRole("ADMIN")
                        // Catálogo é leitura para todos; escrita só para o administrador.
                        .requestMatchers(HttpMethod.POST, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .formLogin(formLogin -> formLogin.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
