package dio.marketplace.registration.application;

import dio.marketplace.registration.infrastructure.persistence.repository.CustomerEntityRepository;
import dio.marketplace.registration.interfaces.dto.ClienteDtos;
import dio.marketplace.shared.web.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Operações de cliente que não passam pelo CRUD genérico.
 *
 * <p>Hoje só a anonimização: atender o direito de eliminação da LGPD sem quebrar
 * o histórico de pedidos. Apagar a linha do cliente levaria junto a referência
 * dos pedidos antigos; trocar os dados pessoais por valores neutros resolve o
 * pedido do titular e mantém a contabilidade da loja.
 */
@Service
public class ClienteService {

    private final CustomerEntityRepository clientes;

    public ClienteService(CustomerEntityRepository clientes) {
        this.clientes = clientes;
    }

    /**
     * Dados do cliente ligado ao usuário autenticado.
     *
     * <p>É o que permite ao cliente comprar no próprio nome: a tela descobre o
     * {@code clienteId} com uma chamada autenticada, em vez de pedir para a pessoa
     * digitar um id que ela não conhece.
     */
    @Transactional(readOnly = true)
    public ClienteDtos.ClienteLogado meuCadastro(String username) {
        var cliente = clientes.findByUsername(username)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Não há cadastro de cliente ligado a este usuário."));

        return new ClienteDtos.ClienteLogado(cliente.getId(), nomeCompleto(cliente), cliente.getEmail());
    }

    private static String nomeCompleto(dio.marketplace.registration.infrastructure.persistence.entity.Customer cliente) {
        var sobrenome = cliente.getLastName();
        return sobrenome == null || sobrenome.isBlank()
                ? cliente.getFirstName()
                : cliente.getFirstName() + " " + sobrenome;
    }

    /**
     * Apaga nome, e-mail e endereço, mantendo o id.
     *
     * <p>O e-mail precisa continuar único no banco, então vira um endereço
     * descartável derivado do próprio id. O endereço é removido pela
     * {@code orphanRemoval} da relação.
     */
    @Transactional
    public void anonimizar(UUID id) {
        var cliente = clientes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));

        cliente.setFirstName("Cliente");
        cliente.setLastName("removido");
        cliente.setEmail("anonimo-" + id + "@removido.local");
        cliente.setAddress(null);

        clientes.save(cliente);
    }
}
