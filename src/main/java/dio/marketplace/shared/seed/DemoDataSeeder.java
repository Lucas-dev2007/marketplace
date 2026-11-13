package dio.marketplace.shared.seed;

import dio.marketplace.catalog.domain.ProductCategory;
import dio.marketplace.catalog.infrastructure.persistence.entity.Product;
import dio.marketplace.catalog.infrastructure.persistence.repository.ProductEntityRepository;
import dio.marketplace.registration.infrastructure.persistence.entity.Address;
import dio.marketplace.registration.infrastructure.persistence.entity.Customer;
import dio.marketplace.registration.infrastructure.persistence.repository.CustomerEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sobe a aplicação já com 20 produtos e 20 clientes de demonstração.
 *
 * <p>É seguro para demonstração por três motivos: só roda quando a tabela está
 * vazia (nada é duplicado ao reiniciar), usa nomes e e-mails fictícios e pode ser
 * desligado com {@code MARKET_SEED_ENABLED=false}.
 *
 * <p>Depois de semear, garante que todo cliente tenha login: quem veio de versões
 * anteriores do projeto (sem as colunas de usuário e senha) ganha credenciais
 * aqui, sem precisar recriar o cadastro.
 *
 * <p>Roda depois do contexto estar pronto, então passa pelos mesmos repositórios
 * que a API usa — inclusive pelas validações de entidade.
 */
@Component
@ConditionalOnProperty(name = "market.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final ProductEntityRepository produtos;
    private final CustomerEntityRepository clientes;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(ProductEntityRepository produtos, CustomerEntityRepository clientes,
                          PasswordEncoder passwordEncoder) {
        this.produtos = produtos;
        this.clientes = clientes;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        semearProdutos();
        semearClientes();
        garantirCredenciaisDosClientes();
    }

    /**
     * Dá login a quem ainda não tem, usando a regra de demonstração descrita em
     * {@link CredenciaisDemo}. A senha é gravada como hash BCrypt.
     */
    private void garantirCredenciaisDosClientes() {
        var semLogin = new ArrayList<dio.marketplace.registration.infrastructure.persistence.entity.Customer>();

        for (var cliente : clientes.findAll()) {
            if (cliente.getUsername() != null && !cliente.getUsername().isBlank()
                    && cliente.getPassword() != null && !cliente.getPassword().isBlank()) {
                continue;
            }

            cliente.setUsername(usuarioUnico(CredenciaisDemo.usuarioDoEmail(cliente.getEmail()), cliente));
            cliente.setPassword(passwordEncoder.encode(CredenciaisDemo.senhaDoNome(cliente.getFirstName())));
            semLogin.add(cliente);
        }

        if (semLogin.isEmpty()) {
            return;
        }

        clientes.saveAll(semLogin);
        logger.info("Seed: {} clientes receberam login de demonstração "
                + "(usuário = parte do e-mail antes do @, senha = primeiro nome + 123).", semLogin.size());
    }

    /**
     * Evita dois clientes com o mesmo usuário: em caso de colisão, o id entra como
     * sufixo, mantendo o login previsível na maioria dos casos.
     */
    private String usuarioUnico(String candidato,
                                dio.marketplace.registration.infrastructure.persistence.entity.Customer cliente) {
        var usuario = candidato == null ? "cliente" : candidato;
        var existente = clientes.findByUsername(usuario);

        if (existente.isEmpty() || existente.get().getId().equals(cliente.getId())) {
            return usuario;
        }

        return usuario + "-" + cliente.getId().toString().substring(0, 4);
    }

    private void semearProdutos() {
        if (produtos.count() > 0) {
            logger.info("Catálogo já tem produtos: seed ignorado.");
            return;
        }

        produtos.saveAll(List.of(
                produto("Notebook Dell Inspiron 14", "Dell", ProductCategory.NOTEBOOK, "4299.00", 12,
                        "14 polegadas, Core i5, 16 GB de RAM e SSD de 512 GB."),
                produto("Notebook Acer Nitro V", "Acer", ProductCategory.NOTEBOOK, "5899.90", 7,
                        "Tela de 144 Hz, RTX 3050 e 16 GB de RAM. Feito para jogos."),
                produto("Notebook Lenovo IdeaPad 1", "Lenovo", ProductCategory.NOTEBOOK, "2799.00", 20,
                        "Leve, com 8 GB de RAM e SSD de 256 GB. Bom para estudos."),
                produto("MacBook Air M2", "Apple", ProductCategory.NOTEBOOK, "8299.00", 5,
                        "Chip M2, tela de 13,6 polegadas e até 18 horas de bateria."),
                produto("Notebook Vaio FE15", "Vaio", ProductCategory.NOTEBOOK, "3699.00", 9,
                        "Tela de 15,6 polegadas, Core i7 e 16 GB de RAM."),
                produto("Galaxy S24", "Samsung", ProductCategory.CELULAR, "3499.00", 15,
                        "Tela de 6,2 polegadas, 256 GB e câmera de 50 MP."),
                produto("iPhone 15", "Apple", ProductCategory.CELULAR, "5999.00", 8,
                        "128 GB, câmera dupla de 48 MP e entrada USB-C."),
                produto("Moto G84 5G", "Motorola", ProductCategory.CELULAR, "1699.90", 25,
                        "256 GB, tela de 120 Hz e bateria de 5.000 mAh."),
                produto("Redmi Note 13", "Xiaomi", ProductCategory.CELULAR, "1499.00", 30,
                        "128 GB, tela AMOLED de 120 Hz e carregamento rápido."),
                produto("Asus Zenfone 10", "Asus", ProductCategory.CELULAR, "3899.00", 6,
                        "Compacto, com Snapdragon 8 Gen 2 e 256 GB."),
                produto("iPad 10", "Apple", ProductCategory.TABLET, "3299.00", 10,
                        "Tela de 10,9 polegadas, 64 GB e suporte a Apple Pencil."),
                produto("Galaxy Tab S9", "Samsung", ProductCategory.TABLET, "4199.00", 6,
                        "Tela AMOLED de 11 polegadas e caneta S Pen inclusa."),
                produto("Fone Sony WH-1000XM5", "Sony", ProductCategory.FONE, "2499.00", 14,
                        "Cancelamento de ruído e até 30 horas de bateria."),
                produto("Galaxy Buds FE", "Samsung", ProductCategory.FONE, "499.00", 22,
                        "Fone sem fio com cancelamento ativo de ruído."),
                produto("AirPods Pro 2", "Apple", ProductCategory.FONE, "1899.00", 11,
                        "Cancelamento de ruído e estojo com carregamento MagSafe."),
                produto("Amazfit GTR 4", "Amazfit", ProductCategory.SMARTWATCH, "999.00", 18,
                        "GPS integrado e até 14 dias de bateria."),
                produto("Apple Watch SE", "Apple", ProductCategory.SMARTWATCH, "2799.00", 7,
                        "Monitoramento cardíaco e detecção de queda."),
                produto("Mouse Logitech MX Master 3S", "Logitech", ProductCategory.ACESSORIO, "649.90", 16,
                        "Silencioso, com rolagem rápida e conexão em três aparelhos."),
                produto("Teclado Redragon K68", "Redragon", ProductCategory.ACESSORIO, "289.90", 24,
                        "Mecânico, ABNT2, com iluminação RGB."),
                produto("Monitor LG 27 Full HD", "LG", ProductCategory.ACESSORIO, "1199.00", 13,
                        "27 polegadas, 75 Hz e painel IPS.")));

        logger.info("Seed: 20 produtos de demonstração criados.");
    }

    private void semearClientes() {
        if (clientes.count() > 0) {
            logger.info("Cadastro já tem clientes: seed ignorado.");
            return;
        }

        clientes.saveAll(List.of(
                cliente("Ana", "Souza", "ana.souza@exemplo.com", "Rua das Flores, 120", "01000-000", "São Paulo", "SP"),
                cliente("Bruno", "Lima", "bruno.lima@exemplo.com", "Avenida Brasil, 45", "20000-000", "Rio de Janeiro", "RJ"),
                cliente("Carla", "Mendes", "carla.mendes@exemplo.com", "Rua da Praia, 300", "40000-000", "Salvador", "BA"),
                cliente("Diego", "Ferreira", "diego.ferreira@exemplo.com", "Rua das Acácias, 12", "80000-000", "Curitiba", "PR"),
                cliente("Elisa", "Ramos", "elisa.ramos@exemplo.com", "Avenida Beira-Mar, 980", "60000-000", "Fortaleza", "CE"),
                cliente("Felipe", "Barbosa", "felipe.barbosa@exemplo.com", "Rua do Comércio, 77", "90000-000", "Porto Alegre", "RS"),
                cliente("Gabriela", "Nunes", "gabriela.nunes@exemplo.com", "Rua Sete de Setembro, 210", "50000-000", "Recife", "PE"),
                cliente("Henrique", "Alves", "henrique.alves@exemplo.com", "Avenida Amazonas, 1500", "69000-000", "Manaus", "AM"),
                cliente("Isabela", "Rocha", "isabela.rocha@exemplo.com", "Rua das Palmeiras, 88", "74000-000", "Goiânia", "GO"),
                cliente("João", "Pereira", "joao.pereira@exemplo.com", "Rua XV de Novembro, 33", "88000-000", "Florianópolis", "SC"),
                cliente("Karina", "Dias", "karina.dias@exemplo.com", "Avenida Afonso Pena, 640", "30000-000", "Belo Horizonte", "MG"),
                cliente("Lucas", "Martins", "lucas.martins@exemplo.com", "Rua da Aurora, 19", "53000-000", "Olinda", "PE"),
                cliente("Mariana", "Castro", "mariana.castro@exemplo.com", "Avenida Central, 202", "65000-000", "São Luís", "MA"),
                cliente("Nelson", "Ribeiro", "nelson.ribeiro@exemplo.com", "Rua do Sol, 55", "57000-000", "Maceió", "AL"),
                cliente("Olívia", "Prado", "olivia.prado@exemplo.com", "Avenida das Nações, 910", "78000-000", "Cuiabá", "MT"),
                cliente("Paulo", "Costa", "paulo.costa@exemplo.com", "Rua das Gaivotas, 7", "29000-000", "Vitória", "ES"),
                cliente("Queila", "Andrade", "queila.andrade@exemplo.com", "Travessa São José, 41", "66000-000", "Belém", "PA"),
                cliente("Rafael", "Teixeira", "rafael.teixeira@exemplo.com", "Rua Marechal, 123", "87000-000", "Maringá", "PR"),
                cliente("Sofia", "Cardoso", "sofia.cardoso@exemplo.com", "Avenida Goiás, 350", "75000-000", "Anápolis", "GO"),
                cliente("Tiago", "Monteiro", "tiago.monteiro@exemplo.com", "Rua do Riacho, 62", "58000-000", "João Pessoa", "PB")));

        logger.info("Seed: 20 clientes de demonstração criados.");
    }

    private static Product produto(String nome, String marca, ProductCategory categoria,
                                   String preco, int estoque, String descricao) {
        var produto = new Product();
        produto.setName(nome);
        produto.setBrand(marca);
        produto.setCategory(categoria);
        produto.setPrice(new BigDecimal(preco));
        produto.setStock(estoque);
        produto.setDescription(descricao);
        return produto;
    }

    private static Customer cliente(String nome, String sobrenome, String email,
                                    String rua, String cep, String cidade, String estado) {
        var endereco = new Address();
        endereco.setStreet(rua);
        endereco.setPostalCode(cep);
        endereco.setCity(cidade);
        endereco.setState(estado);

        var cliente = new Customer();
        cliente.setFirstName(nome);
        cliente.setLastName(sobrenome);
        cliente.setEmail(email);
        cliente.setAddress(endereco);
        return cliente;
    }
}
