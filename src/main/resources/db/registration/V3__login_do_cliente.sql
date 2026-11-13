-- Cada cliente passa a ter login próprio na API.
--
-- As colunas nascem anuláveis porque os clientes que já existem não têm senha;
-- o seed de demonstração preenche usuário e senha (com hash BCrypt) na subida
-- seguinte, derivando o usuário da parte local do e-mail.

ALTER TABLE `Customer`
    ADD COLUMN `username` varchar(60) DEFAULT NULL AFTER `email`,
    ADD COLUMN `password` varchar(100) DEFAULT NULL AFTER `username`;

-- Unique, mas tolerando NULL: cliente sem login ainda pode existir.
CREATE UNIQUE INDEX `uk_customer_username` ON `Customer` (`username`);
