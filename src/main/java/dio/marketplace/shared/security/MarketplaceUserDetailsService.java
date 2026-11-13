package dio.marketplace.shared.security;

import dio.marketplace.registration.infrastructure.persistence.repository.CustomerEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Quem pode entrar na API.
 *
 * <p>Existem três origens de login, verificadas nesta ordem:
 *
 * <ol>
 *   <li>o administrador definido nas variáveis de ambiente (ROLE_ADMIN);</li>
 *   <li>o usuário genérico de demonstração (ROLE_CLIENTE), que serve para testar
 *       a API rapidamente;</li>
 *   <li>os clientes cadastrados no banco, cada um com o próprio login
 *       (ROLE_CLIENTE) — é o que liga a pessoa autenticada ao cadastro dela.</li>
 * </ol>
 *
 * <p>A senha do cliente fica gravada como hash BCrypt; quem compara é o Spring
 * Security, usando o {@link PasswordEncoder} configurado. Senha em texto puro não
 * existe em lugar nenhum.
 *
 * <p>A consulta ao banco usa a transação do banco de registro, que é a principal.
 */
@Component
public class MarketplaceUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(MarketplaceUserDetailsService.class);

    /** Usada só quando a variável de ambiente não é definida. */
    private static final String SENHA_DEV_ADMIN = "admin";
    private static final String SENHA_DEV_CLIENTE = "cliente";

    private final CustomerEntityRepository clientes;
    private final Conta administrador;
    private final Conta clientePadrao;

    /**
     * Usuário e hash da senha, sem mais nada. Deliberadamente não guardamos um
     * {@link UserDetails} pronto: o Spring apaga a senha do objeto devolvido depois
     * de autenticar ({@code eraseCredentialsAfterAuthentication}), então reutilizar
     * a mesma instância faria o segundo login falhar com 401.
     */
    private record Conta(String usuario, String senhaComHash, String perfil) {
    }

    public MarketplaceUserDetailsService(CustomerEntityRepository clientes,
                                         PasswordEncoder passwordEncoder,
                                         @Value("${market.security.admin.username:admin}") String usuarioAdmin,
                                         @Value("${market.security.admin.password:}") String senhaAdmin,
                                         @Value("${market.security.cliente.username:cliente}") String usuarioCliente,
                                         @Value("${market.security.cliente.password:}") String senhaCliente) {
        this.clientes = clientes;

        // O hash BCrypt é gerado uma vez, na subida: ele é caro e a senha não muda
        // em tempo de execução.
        this.administrador = conta(usuarioAdmin, senhaAdmin, SENHA_DEV_ADMIN, "ADMIN", passwordEncoder);
        this.clientePadrao = conta(usuarioCliente, senhaCliente, SENHA_DEV_CLIENTE, "CLIENTE", passwordEncoder);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        if (administrador.usuario().equals(username)) {
            return paraUserDetails(administrador);
        }

        if (clientePadrao.usuario().equals(username)) {
            return paraUserDetails(clientePadrao);
        }

        // Demais logins vêm do cadastro de clientes. A mensagem de erro é genérica
        // de propósito: dizer "usuário não existe" entregaria quais logins existem.
        return clientes.findByUsername(username)
                .filter(cliente -> StringUtils.hasText(cliente.getPassword()))
                .map(cliente -> User.withUsername(cliente.getUsername())
                        .password(cliente.getPassword())
                        .roles("CLIENTE")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha inválidos."));
    }

    private static Conta conta(String nome, String senha, String senhaDeDesenvolvimento,
                               String perfil, PasswordEncoder passwordEncoder) {
        if (!StringUtils.hasText(senha)) {
            logger.warn("Senha de '{}' não configurada: usando a senha de desenvolvimento. "
                    + "Defina MARKET_API_PASSWORD e MARKET_CLIENTE_PASSWORD fora da sua máquina.", nome);
        }

        var hash = passwordEncoder.encode(StringUtils.hasText(senha) ? senha : senhaDeDesenvolvimento);
        return new Conta(nome, hash, perfil);
    }

    /** Instância nova a cada autenticação, pelo motivo explicado no record {@link Conta}. */
    private static UserDetails paraUserDetails(Conta conta) {
        return User.withUsername(conta.usuario())
                .password(conta.senhaComHash())
                .roles(conta.perfil())
                .build();
    }
}
