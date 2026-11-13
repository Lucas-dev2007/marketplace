-- Banco de catálogo (porta 3308): produtos.
--
-- Esta migração serve para bancos novos. Bancos que já existem são marcados como
-- "baseline" na versão 1 e pulam este script — as instruções estão no README.

CREATE TABLE IF NOT EXISTS `Product` (
    `id` binary(16) NOT NULL,
    `brand` varchar(255) DEFAULT NULL,
    `category` enum('ACESSORIO','CELULAR','FONE','NOTEBOOK','SMARTWATCH','TABLET') NOT NULL,
    `createdAt` datetime(6) NOT NULL,
    `description` varchar(1000) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `price` decimal(12,2) NOT NULL,
    `stock` int NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_product_category` (`category`),
    CONSTRAINT `Product_chk_1` CHECK ((`stock` >= 0))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
