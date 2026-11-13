package dio.marketplace.shared.web;

import dio.marketplace.shared.stock.EstoqueInsuficienteException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Traduz erros técnicos em respostas que quem chama a API entende.
 *
 * <p>Sem isso, campo obrigatório vazio virava {@code 500} com corpo vazio — parecia
 * defeito do servidor, quando o problema era o dado enviado — e e-mail repetido
 * virava {@code 409} com "Could not commit changes!", em inglês.
 *
 * <p>O {@code @Order} é necessário porque o Spring Data REST tem tratamento de erro
 * próprio e, sem a prioridade, responderia antes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Corpo de erro: uma frase e, quando houver, o detalhe por campo. */
    public record ErroResponse(String mensagem, List<String> detalhes) {
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> dadosInvalidos(ConstraintViolationException exception) {
        var detalhes = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .distinct()
                .sorted()
                .toList();

        return ResponseEntity.badRequest()
                .body(new ErroResponse("Alguns dados não estão válidos. Confira e envie de novo.", detalhes));
    }

    /**
     * Falha de validação de um corpo de requisição ({@code @Valid} no controller),
     * como quantidade zero em um item de pedido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(MethodArgumentNotValidException exception) {
        var detalhes = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(mensagem -> mensagem != null && !mensagem.isBlank())
                .distinct()
                .sorted()
                .toList();

        return ResponseEntity.badRequest()
                .body(new ErroResponse("Alguns dados não estão válidos. Confira e envie de novo.", detalhes));
    }

    /**
     * Violação de regra do banco, quase sempre e-mail que já existe. A mensagem é
     * neutra porque a mesma exceção cobre campo obrigatório que chegou vazio.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> dadoRepetido(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(
                        "Não foi possível salvar. Algum dado repete um cadastro que já existe ou uma informação obrigatória ficou de fora.",
                        List.of()));
    }

    /**
     * JSON malformado ou valor fora do permitido (uma categoria que não existe, por
     * exemplo). A mensagem do Jackson é técnica e em inglês; aqui vira uma frase
     * simples.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(
                        "Não foi possível ler os dados enviados. Confira o formato do JSON e se os valores são aceitos pela API.",
                        List.of()));
    }

    /**
     * Identificador na URL que não é um UUID válido, como {@code /products/abc}.
     * Sem isso o Spring devolvia 500, como se o servidor tivesse falhado.
     */
    @ExceptionHandler(ConversionFailedException.class)
    public ResponseEntity<ErroResponse> identificadorInvalido(ConversionFailedException exception) {
        return ResponseEntity.badRequest()
                .body(new ErroResponse(
                        "O identificador informado não é válido. Copie o link do registro e tente de novo.",
                        List.of()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(exception.getMessage(), List.of()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> semPermissao(AcessoNegadoException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErroResponse(exception.getMessage(), List.of()));
    }

    /** Regras de negócio e falta de estoque: 409, porque o pedido conflita com o estado atual. */
    @ExceptionHandler({RegraDeNegocioException.class, EstoqueInsuficienteException.class})
    public ResponseEntity<ErroResponse> conflito(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResponse(exception.getMessage(), List.of()));
    }

    /**
     * Banco fora do ar ou conexão recusada. Responde 503 para quem chamou saber que
     * pode tentar de novo, e o detalhe técnico fica só no log do servidor.
     */
    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ErroResponse> bancoIndisponivel(RuntimeException exception) {
        logger.error("Falha ao acessar o banco de dados", exception);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErroResponse(
                        "Não foi possível falar com o banco de dados agora. Tente de novo em instantes.",
                        List.of()));
    }
}
