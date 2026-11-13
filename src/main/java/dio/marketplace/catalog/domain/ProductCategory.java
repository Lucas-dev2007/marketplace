package dio.marketplace.catalog.domain;

/**
 * Categorias aceitas pelo catálogo.
 *
 * <p>É um enum, e não texto livre, para não entrar "notbook" ou "celular " com
 * espaço no fim — o filtro por categoria fica previsível e a API recusa valor
 * fora da lista com uma resposta 400.
 */
public enum ProductCategory {
    NOTEBOOK,
    CELULAR,
    TABLET,
    FONE,
    SMARTWATCH,
    ACESSORIO
}
