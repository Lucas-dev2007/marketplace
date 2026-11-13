package dio.marketplace.shared.web;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa as respostas de erro do {@link ApiExceptionHandler}.
 *
 * <p>Um controller de mentira só lança as exceções; o MockMvc monta o Spring MVC
 * completo em volta, com o handler registrado. Assim roda sem banco e sem subir a
 * aplicação inteira.
 */
class ApiExceptionHandlerTests {

    private static final Validator VALIDADOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ControllerDeTeste())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void dadoInvalidoDeveVirar400ComAMensagemDoCampo() throws Exception {
        mockMvc.perform(post("/teste/dado-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("Confira e envie de novo")))
                .andExpect(jsonPath("$.detalhes", hasItem("Informe o nome do cliente.")));
    }

    @Test
    void dadoRepetidoDeveVirar409SemVazarMensagemTecnica() throws Exception {
        mockMvc.perform(post("/teste/dado-repetido"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("Algum dado repete")))
                .andExpect(jsonPath("$.mensagem").value(not(containsString("Duplicate entry"))))
                .andExpect(jsonPath("$.detalhes").isEmpty());
    }

    @Test
    void jsonMalformadoDeveVirar400ComMensagemEmPortugues() throws Exception {
        mockMvc.perform(post("/teste/com-corpo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{isso não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("Confira o formato do JSON")))
                .andExpect(jsonPath("$.mensagem").value(not(containsString("JSON parse error"))));
    }

    @Test
    void identificadorInvalidoDeveVirar400ENao500() throws Exception {
        mockMvc.perform(post("/teste/id-invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("identificador informado")));
    }

    @Test
    void bancoIndisponivelDeveVirar503SemVazarDetalheTecnico() throws Exception {
        mockMvc.perform(post("/teste/banco-fora"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensagem").value(containsString("banco de dados agora")))
                .andExpect(jsonPath("$.mensagem").value(not(containsString("connection refused"))));
    }

    @RestController
    static class ControllerDeTeste {

        @PostMapping("/teste/dado-invalido")
        void dadoInvalido() {
            throw new ConstraintViolationException(VALIDADOR.validate(new ClienteDeTeste(" ")));
        }

        @PostMapping("/teste/dado-repetido")
        void dadoRepetido() {
            throw new DataIntegrityViolationException("Duplicate entry 'ana@example.com' for key 'email'");
        }

        @PostMapping("/teste/com-corpo")
        void comCorpo(@RequestBody ClienteDeTeste cliente) {
        }

        @PostMapping("/teste/banco-fora")
        void bancoFora() {
            throw new DataAccessResourceFailureException("connection refused");
        }

        @PostMapping("/teste/id-invalido")
        void idInvalido() {
            throw new ConversionFailedException(TypeDescriptor.valueOf(String.class),
                    TypeDescriptor.valueOf(java.util.UUID.class), "abc", new IllegalArgumentException("Invalid UUID"));
        }
    }

    record ClienteDeTeste(@NotBlank(message = "Informe o nome do cliente.") String firstName) {
    }
}
