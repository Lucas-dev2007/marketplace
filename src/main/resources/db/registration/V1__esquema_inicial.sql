-- Banco de registro (porta 3307): clientes e endereços.
--
-- Banco novo recebe este script inteiro. Banco que já existe é marcado como
-- "baseline" na versão 1 e segue direto para a V2 — as instruções estão no README.

CREATE TABLE IF NOT EXISTS `Address` (
    `id` binary(16) NOT NULL,
    `city` varchar(255) DEFAULT NULL,
    `createdOn` datetime(6) NOT NULL,
    `postalCode` varchar(255) DEFAULT NULL,
    `state` varchar(255) DEFAULT NULL,
    `street` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- O endereço é um registro separado com chave única: um cliente tem no máximo um
-- endereço, e o endereço não se repete entre clientes.
CREATE TABLE IF NOT EXISTS `Customer` (
    `id` binary(16) NOT NULL,
    `createdOn` datetime(6) NOT NULL,
    `email` varchar(255) NOT NULL,
    `firstName` varchar(255) NOT NULL,
    `lastName` varchar(255) DEFAULT NULL,
    `address_id` binary(16) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UK3qgg01qojcmbdp47dkaom9x45` (`email`),
    UNIQUE KEY `UKi7f12t1rqxjnufl1k69g1887v` (`address_id`),
    CONSTRAINT `FKfok4ytcqy7lovuiilldbebpd9` FOREIGN KEY (`address_id`) REFERENCES `Address` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
