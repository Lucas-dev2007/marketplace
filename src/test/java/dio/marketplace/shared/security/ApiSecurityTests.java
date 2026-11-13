package dio.marketplace.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante as regras de acesso: sem login ninguém entra, cliente não escreve no
 * catálogo e não vê dados de cliente, administrador pode.
 *
 * <p>Sobe a aplicação de verdade em uma porta aleatória e faz as chamadas por HTTP,
 * então passa pela cadeia completa de filtros de segurança — é o que interessa
 * aqui. Precisa dos dois bancos no ar, os mesmos do {@code docker compose} e dos
 * serviços do GitHub Actions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiSecurityTests {

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int porta;

    @Test
    void semCredencialDeveResponder401() throws Exception {
        assertThat(statusHttp("GET", "/products", null)).isEqualTo(401);
    }

    @Test
    void clientePodeLerOCatalogo() throws Exception {
        assertThat(statusHttp("GET", "/products", credencial("cliente", "cliente"))).isEqualTo(200);
    }

    @Test
    void clienteNaoPodeCadastrarProduto() throws Exception {
        var corpo = """
                {"name":"Produto do cliente","category":"FONE","price":10.00,"stock":1}
                """;

        assertThat(statusHttp("POST", "/products", credencial("cliente", "cliente"), corpo)).isEqualTo(403);
    }

    @Test
    void clienteNaoPodeVerClientes() throws Exception {
        assertThat(statusHttp("GET", "/customers", credencial("cliente", "cliente"))).isEqualTo(403);
    }

    @Test
    void administradorPodeVerClientes() throws Exception {
        assertThat(statusHttp("GET", "/customers", credencial("admin", "admin"))).isEqualTo(200);
    }

    /** Cliente cadastrado entra com o próprio login (usuário do seed + senha padrão). */
    @Test
    void clienteCadastradoEntraComOProprioLogin() throws Exception {
        assertThat(statusHttp("GET", "/products", credencial("ana.souza", "ana123"))).isEqualTo(200);
    }

    @Test
    void clienteVeOProprioCadastro() throws Exception {
        var resposta = enviar("GET", "/clientes/eu", credencial("ana.souza", "ana123"), null);

        assertThat(resposta.corpo()).contains("ana.souza@exemplo.com");
        assertThat(resposta.status()).isEqualTo(200);
    }

    /** O cliente só compra no próprio nome; mandar o id de outro cadastro é 403. */
    @Test
    void clienteNaoPodeFecharPedidoParaOutroCadastro() throws Exception {
        var corpo = """
                {"clienteId":"%s","itens":[{"produtoId":"%s","quantidade":1}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        assertThat(statusHttp("POST", "/pedidos", credencial("ana.souza", "ana123"), corpo)).isEqualTo(403);
    }

    private int statusHttp(String metodo, String caminho, String autorizacao) throws IOException, InterruptedException {
        return statusHttp(metodo, caminho, autorizacao, null);
    }

    private int statusHttp(String metodo, String caminho, String autorizacao, String corpo)
            throws IOException, InterruptedException {
        return enviar(metodo, caminho, autorizacao, corpo).status();
    }

    private Resposta enviar(String metodo, String caminho, String autorizacao, String corpo)
            throws IOException, InterruptedException {
        var construtor = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + porta + caminho))
                .header("Content-Type", "application/json");

        if (autorizacao != null) {
            construtor.header("Authorization", autorizacao);
        }

        var requisicao = construtor
                .method(metodo, corpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corpo))
                .build();

        var resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
        return new Resposta(resposta.statusCode(), resposta.body());
    }

    private record Resposta(int status, String corpo) {
    }

    private String credencial(String usuario, String senha) {
        var texto = usuario + ":" + senha;
        return "Basic " + Base64.getEncoder().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }
}
