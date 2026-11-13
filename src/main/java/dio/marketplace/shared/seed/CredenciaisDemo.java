package dio.marketplace.shared.seed;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Regras de geração das credenciais de demonstração dos clientes.
 *
 * <p>Serve para o projeto poder ser testado sem um fluxo de convite: cada cliente
 * de exemplo ganha um usuário derivado do e-mail e uma senha previsível, e os dois
 * ficam documentados no README.
 *
 * <p><b>Só para demonstração.</b> Num sistema real a senha é escolhida pelo
 * próprio cliente e trocada por um fluxo de convite/recuperação — nunca gerada a
 * partir do nome.
 */
public final class CredenciaisDemo {

    /** Sufixo fixo das senhas de demonstração. */
    private static final String SUFIXO_SENHA = "123";

    private CredenciaisDemo() {
    }

    /**
     * Parte do e-mail antes do "@": {@code ana.souza@exemplo.com} vira
     * {@code ana.souza}. Só letras, números, ponto, hífen e sublinhado sobrevivem.
     */
    public static String usuarioDoEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        var parteLocal = email.substring(0, email.indexOf('@')).toLowerCase(Locale.ROOT);
        var limpo = parteLocal.replaceAll("[^a-z0-9._-]", "");

        return limpo.isBlank() ? null : limpo;
    }

    /**
     * Primeiro nome sem acento e em minúsculas, mais o sufixo:
     * {@code João} vira {@code joao123}.
     */
    public static String senhaDoNome(String nome) {
        var semAcento = Normalizer.normalize(nome == null ? "" : nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "") + SUFIXO_SENHA;
    }
}
