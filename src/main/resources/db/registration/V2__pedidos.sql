-- Pedidos ficam no banco de registro porque pertencem ao cliente e ao histórico
-- de compra, não ao catálogo.
--
-- IF NOT EXISTS porque a versão anterior do projeto criava estas tabelas pelo
-- hbm2ddl do Hibernate: em bancos que já rodaram aquele esquema, esta migração
-- precisa apenas marcar a versão como aplicada, sem recriar nada.

CREATE TABLE IF NOT EXISTS `Orders` (
    `id` binary(16) NOT NULL,
    `createdAt` datetime(6) NOT NULL,
    `status` enum('CANCELADO','CONFIRMADO') NOT NULL,
    `total` decimal(12,2) NOT NULL,
    -- Nome do usuário da API que fechou o pedido. É o que permite ao perfil
    -- CLIENTE ver apenas os próprios pedidos, sem depender do cadastro.
    `usuario` varchar(60) NOT NULL,
    `customer_id` binary(16) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKhwn935tudm12n4ihi91mnm0w5` (`customer_id`),
    CONSTRAINT `FKhwn935tudm12n4ihi91mnm0w5` FOREIGN KEY (`customer_id`) REFERENCES `Customer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- O item guarda nome e preço do momento da compra. Produto e pedido estão em
-- bancos diferentes, então não existe chave estrangeira: o snapshot é o que
-- mantém o histórico correto quando o preço muda depois.
CREATE TABLE IF NOT EXISTS `OrderItem` (
    `id` binary(16) NOT NULL,
    `productId` binary(16) NOT NULL,
    `productName` varchar(255) NOT NULL,
    `quantity` int NOT NULL,
    `unitPrice` decimal(12,2) NOT NULL,
    `order_id` binary(16) NOT NULL,
    PRIMARY KEY (`id`),
    KEY `FKea17ekwokumlak9h7eevmkfoc` (`order_id`),
    CONSTRAINT `FKea17ekwokumlak9h7eevmkfoc` FOREIGN KEY (`order_id`) REFERENCES `Orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
