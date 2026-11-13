# Mercado Tech

Marketplace de demonstração em Spring Boot: catálogo de produtos, cadastro de clientes, carrinho e pedidos com baixa de estoque,
perfis de acesso (administrador e cliente) e dados de exemplo prontos para testar.
O objetivo é servir de portfólio **e** de leitura sobre como as peças do backend se encaixam.

---

## Tecnologias

| Camada | O que é usado |
| --- | --- |
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Data REST, Security, Validation, Actuator) |
| Banco | MySQL 9.6 (dois bancos separados) |
| Migrações | Flyway |
| Documentação da API | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, Mockito, AssertJ |
| Infra local | Docker Compose |
| CI | GitHub Actions |

Front-end: uma página estática (HTML + CSS + JavaScript puro) servida pelo próprio Spring Boot, sem build separado.

---

## Como rodar

### Pré-requisitos

- JDK 25 (`java -version`)
- Docker com Docker Compose

### 1. Suba os bancos

```bash
docker compose up -d
```

Isso cria dois MySQL: `registration` na porta **3307** (clientes, endereços e pedidos) e `catalog` na porta **3308** (produtos).
Usuário `app`, senha `app`, ambos definidos no `docker-compose.yml`.

### 2. Suba a aplicação

```bash
./gradlew bootRun
```

Na primeira subida, o Flyway cria o esquema e o seed grava 20 produtos e 20 clientes de demonstração.
O suporte a Docker Compose do Spring Boot também sobe os bancos se eles ainda não estiverem no ar.

### 3. Acesse

| O quê | Endereço |
| --- | --- |
| Loja (interface) | http://localhost:8080/ |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI em JSON | http://localhost:8080/v3/api-docs |
| Health check | http://localhost:8080/actuator/health |

### 4. Login

A API usa HTTP Basic, então o login é o próprio par usuário/senha em cada requisição.

| Usuário | Senha (desenvolvimento) | Perfil |
| --- | --- | --- |
| `admin` | `admin` | ADMIN — gerencia produtos e clientes |
| `cliente` | `cliente` | CLIENTE — consulta o catálogo e os próprios pedidos |

Essas senhas só valem quando as variáveis de ambiente não estão definidas: a aplicação **avisa no log** que está usando a senha de
desenvolvimento. Fora da sua máquina, defina `MARKET_API_PASSWORD` e `MARKET_CLIENTE_PASSWORD`.

#### Login de cada cliente

Cada cliente do seed também tem login próprio, com o perfil CLIENTE. A regra das credenciais de demonstração é simples:
**usuário = parte do e-mail antes do `@`** e **senha = primeiro nome sem acento + `123`**.

| Usuário | E-mail | Senha |
| --- | --- | --- |
| `ana.souza` | ana.souza@exemplo.com | `ana123` |
| `bruno.lima` | bruno.lima@exemplo.com | `bruno123` |
| `carla.mendes` | carla.mendes@exemplo.com | `carla123` |
| `diego.ferreira` | diego.ferreira@exemplo.com | `diego123` |
| `elisa.ramos` | elisa.ramos@exemplo.com | `elisa123` |
| `felipe.barbosa` | felipe.barbosa@exemplo.com | `felipe123` |
| `gabriela.nunes` | gabriela.nunes@exemplo.com | `gabriela123` |
| `henrique.alves` | henrique.alves@exemplo.com | `henrique123` |
| `isabela.rocha` | isabela.rocha@exemplo.com | `isabela123` |
| `joao.pereira` | joao.pereira@exemplo.com | `joao123` |
| `karina.dias` | karina.dias@exemplo.com | `karina123` |
| `lucas.martins` | lucas.martins@exemplo.com | `lucas123` |
| `mariana.castro` | mariana.castro@exemplo.com | `mariana123` |
| `nelson.ribeiro` | nelson.ribeiro@exemplo.com | `nelson123` |
| `olivia.prado` | olivia.prado@exemplo.com | `olivia123` |
| `paulo.costa` | paulo.costa@exemplo.com | `paulo123` |
| `queila.andrade` | queila.andrade@exemplo.com | `queila123` |
| `rafael.teixeira` | rafael.teixeira@exemplo.com | `rafael123` |
| `sofia.cardoso` | sofia.cardoso@exemplo.com | `sofia123` |
| `tiago.monteiro` | tiago.monteiro@exemplo.com | `tiago123` |

Quem entrou com um desses logins compra **no próprio nome**: o `GET /clientes/eu` informa o cadastro ligado ao usuário e o pedido só
pode ser fechado para ele. A senha fica no banco como hash BCrypt e o hash não sai em nenhuma resposta (o campo é `@JsonIgnore`).

Cliente cadastrado pela API (`POST /customers`) recebe credenciais na mesma regra, geradas no evento de criação.

> Essas credenciais existem para a demonstração poder ser testada de ponta a ponta. Num sistema real, a senha é escolhida pelo
> cliente por um fluxo de convite e recuperação — nunca derivada do nome.

### Variáveis de ambiente

O `.env.example` lista todas as variáveis com valores fictícios. Ele **não é lido automaticamente** — sirva-se dele para exportar
no terminal ou configurar na IDE:

```bash
export MARKET_API_PASSWORD='uma-senha-forte'
export MARKET_CLIENTE_PASSWORD='outra-senha-forte'
./gradlew bootRun
```

| Variável | Padrão | Para que serve |
| --- | --- | --- |
| `MARKET_API_USER` / `MARKET_API_PASSWORD` | `admin` / (senha de dev) | Usuário administrador |
| `MARKET_CLIENTE_USER` / `MARKET_CLIENTE_PASSWORD` | `cliente` / (senha de dev) | Usuário cliente |
| `REGISTRATION_DB_URL` | `jdbc:mysql://localhost:3307/registration` | Banco de registro |
| `REGISTRATION_DB_USERNAME` / `REGISTRATION_DB_PASSWORD` | `app` / `app` | Credenciais do banco de registro |
| `CATALOG_DB_URL` | `jdbc:mysql://localhost:3308/catalog` | Banco de catálogo |
| `CATALOG_DB_USERNAME` / `CATALOG_DB_PASSWORD` | `app` / `app` | Credenciais do banco de catálogo |
| `MARKET_SEED_ENABLED` | `true` | Liga/desliga os dados de demonstração |
| `REGISTRATION_DB_DDL` / `CATALOG_DB_DDL` | `validate` | Validação do esquema pelo Hibernate |

---

## Endpoints principais

### Catálogo de produtos (Spring Data REST)

| Método | Caminho | Quem pode | O que faz |
| --- | --- | --- | --- |
| GET | `/products?page=0&size=20&sort=price,asc` | qualquer usuário logado | Lista paginada |
| GET | `/products/{id}` | qualquer usuário logado | Busca um produto |
| GET | `/products/search/findByCategory?category=NOTEBOOK` | qualquer usuário logado | Filtra por categoria |
| GET | `/products/search/busca?termo=note&minimo=2000&maximo=5000` | qualquer usuário logado | Busca por nome e faixa de preço |
| POST | `/products` | ADMIN | Cadastra produto |
| PUT/PATCH | `/products/{id}` | ADMIN | Atualiza produto |
| DELETE | `/products/{id}` | ADMIN | Remove produto |

Categorias disponíveis: `NOTEBOOK`, `CELULAR`, `TABLET`, `FONE`, `SMARTWATCH`, `ACESSORIO`.

### Clientes

| Método | Caminho | Quem pode | O que faz |
| --- | --- | --- | --- |
| GET | `/customers?size=20` | ADMIN | Lista clientes (com endereço resumido) |
| GET | `/customers/{id}` | ADMIN | Detalha um cliente |
| GET | `/customers/search/findByFirstNameStartingWithIgnoreCase?firstName=a` | ADMIN | Busca por início do nome |
| POST | `/customers` | ADMIN | Cadastra cliente |
| PUT/PATCH | `/customers/{id}` | ADMIN | Atualiza cliente |
| GET | `/clientes/eu` | qualquer usuário logado | Dados do próprio cadastro (id, nome, e-mail) |
| POST | `/clientes/{id}/anonimizar` | ADMIN | Apaga dados pessoais (LGPD), mantendo o histórico |

Exclusão de cliente é bloqueada de propósito: apagar a linha quebraria os pedidos. O caminho para atender um pedido de exclusão é a
anonimização.

### Pedidos

| Método | Caminho | Quem pode | O que faz |
| --- | --- | --- | --- |
| POST | `/pedidos` | ADMIN ou CLIENTE | Fecha um pedido e baixa o estoque |
| GET | `/pedidos` | ADMIN vê todos; CLIENTE vê os seus | Lista pedidos |
| GET | `/pedidos/{id}` | dono do pedido ou ADMIN | Detalha um pedido |
| POST | `/pedidos/{id}/cancelar` | dono do pedido ou ADMIN | Cancela e devolve o estoque |

Exemplo de fechamento de pedido:

```bash
curl -u cliente:cliente -X POST http://localhost:8080/pedidos \
  -H "Content-Type: application/json" \
  -d '{
        "clienteId": "ID-DO-CLIENTE",
        "itens": [
          {"produtoId": "ID-DO-PRODUTO", "quantidade": 2}
        ]
      }'
```

```json
{
  "id": "8b5a9000-21f9-4896-b09e-9ecb074991ba",
  "cliente": "Ana Souza",
  "status": "CONFIRMADO",
  "total": 998.00,
  "criadoEm": "2026-11-13T17:19:00.260347Z",
  "itens": [
    {"produto": "Galaxy Buds FE", "quantidade": 2, "precoUnitario": 499.00, "subtotal": 998.00}
  ]
}
```

Respostas de erro: `400` dados inválidos, `401` sem credencial, `403` perfil sem permissão ou pedido de outra pessoa, `404` não
encontrado, `409` regra de negócio (estoque insuficiente, pedido já cancelado). O corpo vem sempre como
`{"mensagem": "...", "detalhes": [...]}`.

Os endpoints do Spring Data REST falam HAL: a lista vem dentro de `_embedded` e o id de cada item aparece no link `self`, não no corpo.

---

## Como uma requisição atravessa o código

Tomando `POST /pedidos` como exemplo:

1. **Filtros do Spring Security** (`shared/security/ApiSecurityConfiguration`) leem o cabeçalho `Authorization`, validam usuário e
   senha no `UserDetailsService` em memória e montam a autenticação com o perfil (`ROLE_ADMIN` ou `ROLE_CLIENTE`). Como a sessão é
   `STATELESS`, nada fica guardado entre requisições.
2. **Controller** (`registration/interfaces/PedidoController`) recebe o JSON já convertido em `PedidoDtos.NovoPedido`. As anotações
   de validação (`@NotNull`, `@Min`, `@NotEmpty`) são checadas antes de qualquer regra rodar; se algo falhar, a resposta é `400`.
3. **Serviço de orquestração** (`registration/application/CheckoutService`) resolve nome e preço atuais de cada produto no banco de
   catálogo — isso acontece antes de mexer em estoque, para poder recusar um produto inexistente sem ter alterado nada.
4. **Porta de estoque** (`shared/stock/EstoquePort`, implementada por `catalog/infrastructure/stock/StockAdapter`) baixa o estoque em
   uma transação do banco de catálogo, com um `UPDATE` condicional por item.
5. **Serviço de pedido** (`registration/application/PedidoService`) grava o pedido e os itens na transação do banco de registro,
   calculando o total a partir dos preços lidos no passo 3.
6. **DTO de resposta** (`PedidoDtos.Pedido`) é montado a partir da entidade e devolvido como JSON — a entidade não vaza para a API.
7. **Erros** de qualquer etapa passam pelo `shared/web/ApiExceptionHandler`, que converte exceção em status + mensagem em português.

As leituras (`GET /products`, `GET /customers`) encurtam o caminho: o Spring Data REST monta o controller e o JSON direto a partir do
repositório.

---

## Decisões de arquitetura

### Dois bancos de dados

O projeto tem dois esquemas MySQL servidos pela mesma aplicação:

- **registration** (3307): `Customer`, `Address`, `Orders`, `OrderItem`
- **catalog** (3308): `Product`

A separação representa a fronteira de dois contextos: catálogo cuida do que é vendido; registro cuida de quem compra e do que foi
comprado. Como consequência, **não existe chave estrangeira entre produto e item de pedido** — o item guarda uma cópia do nome e do
preço do momento da compra. Isso é intencional: se o preço do produto mudar amanhã, o pedido antigo continua com o valor que o cliente
pagou.

Cada banco tem sua configuração completa (`CatalogConfiguration` e `RegistrationConfiguration`): `DataSource`, `EntityManagerFactory`,
`PlatformTransactionManager`, `Flyway` e o pacote de repositórios que pertence a ele. O de registro é `@Primary`; o de catálogo é
identificado pelo qualificador `Catalog` em toda injeção.

### Ports and adapters

O módulo de registro não conhece o repositório de produtos. Ele conhece a interface `EstoquePort`, com dois métodos (`reservar`,
`devolver`), e troca com o catálogo um objeto simples, `ReservaEstoque(produtoId, quantidade)`. Quem implementa a porta é o
`StockAdapter`, que fica no módulo de catálogo e usa a transação daquele banco.

O ganho é direto: fecha-se a dependência entre módulos e o fechamento de pedido pode ser testado sem banco, com um dublê da porta
(é o que `CheckoutServiceTests` faz).

### Spring Data REST onde faz sentido

Produtos e clientes são CRUD simples: listar, paginar, filtrar, criar, atualizar. Para esse formato, o Spring Data REST gera os
endpoints a partir da interface do repositório, com paginação, ordenação, HAL e busca derivada do nome do método
(`findByFirstNameStartingWithIgnoreCase`). Escrever controllers para isso seria repetição.

Pedidos, por outro lado, têm regra de verdade — reservar estoque, checar o dono, impedir edição direta. Por isso são um
`@RestController` escrito à mão, e o repositório de pedidos é marcado com `exported = false` para não virar endpoint público por
acidente.

### Duas transações, com compensação

Fechar um pedido escreve em dois bancos, e o Spring não tem transação distribuída aqui (seria preciso JTA). A garantia vem de ordem
mais compensação:

1. reserva o estoque — transação do catálogo, tudo ou nada;
2. grava o pedido — transação do registro;
3. se o passo 2 falhar, devolve o estoque reservado e propaga o erro.

O cancelamento faz o inverso: devolve o estoque e depois marca o pedido como cancelado, revertendo a devolução se a segunda etapa
falhar.

A baixa de estoque é um `UPDATE ... WHERE stock >= quantidade`. É o banco quem decide e trava a linha, então dois pedidos simultâneos
do último item não conseguem baixar o mesmo saldo: um recebe `1` linha afetada, o outro recebe `0` e vira `409`.

O que essa compensação **não** cobre: se o processo morrer entre as duas etapas, sobra estoque reservado sem pedido. A solução seria
persistir um evento de reserva e reprocessá-lo; está nas pendências no fim deste README.

### Segurança

- HTTP Basic. Quem autentica é o `MarketplaceUserDetailsService`, com três origens: o administrador do ambiente, o usuário
  genérico `cliente` e cada cliente cadastrado (login e hash BCrypt no banco).
- `SessionCreationPolicy.STATELESS`: cada requisição manda credencial; nada de sessão no servidor.
- CSRF desligado porque não há cookie de sessão para proteger.
- Regras por perfil: `/customers/**` e `/clientes/**` só ADMIN; escrita em `/products/**` só ADMIN; o resto exige login.
- Dono do pedido: o `Principal` vira o campo `usuario` do pedido, e a checagem de dono fica em um único método (`buscarPermitido`),
  para não ser esquecida em endpoint novo. Além disso, o cliente só fecha pedido para o próprio cadastro (`garantirQuePodeComprarPara`).
- O `UserDetails` é montado de novo a cada autenticação de propósito: o Spring apaga a senha do objeto devolvido depois de
  autenticar, então guardar uma instância única faria o segundo login falhar com `401`.
- Swagger e a loja estática são públicos; os dados que elas consomem continuam exigindo login.

Como a credencial viaja em cada requisição, **produção exige HTTPS**. Em memória serve para demonstração; um sistema real usaria um
provedor de identidade (OAuth2/OIDC) e uma tabela ligando usuário a cliente.

### Validação e tratamento de erros

As validações de formato ficam nos DTOs (`PedidoDtos`), que são records próprios — a API não expõe entidade. Regras de negócio
lançam exceções específicas (`EstoqueInsuficienteException`, `RegraDeNegocioException`, `RecursoNaoEncontradoException`,
`AcessoNegadoException`), e o `ApiExceptionHandler` traduz cada uma para o status certo, sempre com mensagem em português.

### Migrações com Flyway

O esquema não é mais criado pelo Hibernate. Cada banco tem sua pasta de scripts:

- `src/main/resources/db/registration/` — `V1__esquema_inicial.sql` (clientes e endereços), `V2__pedidos.sql` (pedidos e itens),
  `V3__login_do_cliente.sql` (usuário e senha do cliente)
- `src/main/resources/db/catalog/` — `V1__esquema_inicial.sql` (produtos)

O `Flyway` é configurado à mão nos dois módulos, porque a autoconfiguração do Spring Boot assume um banco só. O Hibernate ficou em
`validate`: se alguém mudar uma entidade sem criar a migração, a aplicação não sobe — melhor falhar cedo do que descobrir em
produção.

**Etapa manual para bancos que já existiam antes do Flyway:** eles são marcados como *baseline* na versão 1
(`baselineOnMigrate`), ou seja, a V1 não roda de novo e só as migrações seguintes são aplicadas. Foi o que aconteceu neste projeto:
os dois bancos já tinham tabelas, receberam a marca de baseline e a V2 foi aplicada. Por isso a V2 usa `CREATE TABLE IF NOT EXISTS`:
em banco antigo as tabelas de pedido já existiam e a migração apenas registra a versão. Em um banco novo, a V1 e a V2 criam tudo.

### Seed de demonstração

`shared/seed/DemoDataSeeder` é um `CommandLineRunner` que grava 20 produtos e 20 clientes fictícios (e-mails `@exemplo.com`). Ele só
insere quando a tabela está vazia, então reiniciar a aplicação não duplica nada. Para desligar:

```bash
MARKET_SEED_ENABLED=false ./gradlew bootRun
```

---

## Estrutura de pastas

```
src/main/java/dio/marketplace
├── MarketplaceApplication.java      ponto de entrada
├── catalog/                         contexto de catálogo (banco 3308)
│   ├── CatalogConfiguration.java     DataSource, JPA, transação e Flyway deste banco
│   ├── domain/ProductCategory.java   categorias do catálogo
│   └── infrastructure/
│       ├── persistence/              entidade Product e repositório com busca e baixa de estoque
│       └── stock/StockAdapter.java   implementação da porta de estoque
├── registration/                    contexto de registro (banco 3307)
│   ├── infrastructure/
│   │   ├── RegistrationConfiguration.java   configuração do banco de registro
│   │   └── persistence/                     entidades Customer, Address, Order, OrderItem e repositórios
│   ├── domain/OrderStatus.java       CONFIRMADO / CANCELADO
│   ├── application/
│   │   ├── CheckoutService.java      orquestra estoque + pedido (as duas transações)
│   │   ├── PedidoService.java        grava, lista e cancela pedidos
│   │   ├── ClienteService.java       anonimização (LGPD)
│   │   └── ItemCalculado.java        item com preço e nome já resolvidos
│   └── interfaces/
│       ├── PedidoController.java     /pedidos
│       ├── ClienteController.java    /clientes/{id}/anonimizar
│       └── dto/PedidoDtos.java       formatos de entrada e saída
└── shared/                          o que é comum aos dois contextos
    ├── security/ApiSecurityConfiguration.java   usuários, perfis e regras de acesso
    ├── web/ApiExceptionHandler.java             exceções → status HTTP
    ├── web/ (demais exceções de negócio)
    ├── stock/EstoquePort.java, ReservaEstoque.java   contrato entre os dois contextos
    ├── persistence/JpaPersistenceUnits.java     fábrica de EntityManagerFactory e transação
    └── seed/DemoDataSeeder.java                 dados de demonstração

src/main/resources
├── application.properties           portas, credenciais e chaves de configuração
├── db/registration, db/catalog      migrações do Flyway
└── static/                          index.html, app.js e style.css da loja

src/test/java/dio/marketplace        testes unitários e de integração
.github/workflows/ci.yml             pipeline de testes
```

---

## Interface web

A página em `http://localhost:8080/` consome a mesma API do Swagger, com login Basic feito no topo da tela:

- catálogo em cartões, com filtro por categoria e busca por nome (`/products/search/...`);
- carrinho com quantidade e total, que chama `POST /pedidos`;
- aba de pedidos, com status, itens e botão de cancelar;
- aba de clientes (só para ADMIN), com cadastro e botão de anonimização.

O código fica em `src/main/resources/static` e não tem build: o Spring Boot serve os arquivos estáticos direto. As credenciais ficam
apenas na memória da página — recarregar pede login de novo.

Com o login de **administrador**, o carrinho mostra um seletor de cliente e permite fechar pedido em nome de qualquer um. Com o login
de **cliente**, a tela chama `GET /clientes/eu`, mostra "Comprando como …" e fecha o pedido no próprio cadastro — o seletor some.
A aba de clientes fica indisponível para esse perfil.

---

## Testes e CI

```bash
./gradlew test
```

Os testes cobrem:

- `PedidoServiceTests` — cálculo do subtotal de cada item (preço × quantidade) e do total como soma dos subtotais, com vários itens,
  quantidades diferentes, centavos e pedido vazio;
- `CheckoutServiceTests` — ordem reserva/gravação, devolução do estoque quando a gravação falha e recusa por falta de estoque;
- `ApiSecurityTests` — `401` sem credencial, cliente lendo catálogo, `403` ao escrever produto e ao ver clientes, login do cliente
  cadastrado, `GET /clientes/eu` e `403` ao tentar fechar pedido para outro cadastro (HTTP real, na porta aleatória);
- `ApiExceptionHandlerTests` — formato das respostas de erro;
- `JpaCustomerRepositoryTests` — persistência e busca de cliente;
- `MarketplaceApplicationTests` — a aplicação sobe com as duas conexões.

Os testes de integração precisam dos bancos no ar (`docker compose up -d`); o Gradle desliga o suporte a Docker Compose durante os
testes para não subir containers por conta própria.

O workflow `.github/workflows/ci.yml` roda `./gradlew test` em todo push e pull request, subindo os mesmos dois MySQL como serviços
do runner nas portas 3307 e 3308.

---

## Limitações conhecidas e próximos passos

1. **Deploy público não foi feito.** Não há ambiente, domínio nem credenciais de publicação neste projeto. Para publicar faltam:
   servidor com HTTPS, dois bancos MySQL acessíveis, as variáveis do `.env.example` como segredos do ambiente e,
   preferencialmente, `--springdoc.api-docs.enabled=false` para não expor o Swagger. Até lá, a demonstração é local.
2. **Compensação não persistida.** Se o processo cair entre reservar estoque e gravar o pedido, sobra estoque reservado sem pedido.
   Ação concreta: gravar um evento `ReservaEstoque` em tabela própria na mesma transação do catálogo e reprocessá-lo na subida, com
   prazo de expiração.
3. **Usuário não é o mesmo que cliente.** O dono do pedido é o usuário da API (`admin`/`cliente`), não a linha de `Customer`. Com um
   provedor de identidade real, o certo é vincular os dois e derivar o `clienteId` da sessão, em vez de recebê-lo no corpo.
4. **Fechamento de pedido sem chave de idempotência.** Um `POST /pedidos` repetido por timeout de rede cria dois pedidos. Ação
   concreta: aceitar um cabeçalho `Idempotency-Key` e guardar o resultado por chave.
5. **Credenciais de cliente geradas para a demonstração.** O login sai do e-mail e a senha do primeiro nome, com hash BCrypt no
   banco. Ação concreta: trocar por convite com token de uso único e tela de definição de senha; até lá, o `TODO` está no
   `CustomerEventHandler`.
