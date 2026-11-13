/*
 * Loja de demonstração do marketplace.
 *
 * Fala com a mesma API que o Swagger documenta, usando HTTP Basic. As credenciais
 * ficam apenas na memória da página: recarregar o navegador pede login de novo.
 * O objetivo é demonstrar os fluxos reais (catálogo, carrinho, pedido, cancelamento,
 * cadastro e anonimização de cliente), não ser uma vitrine decorativa.
 */

const estado = {
    usuario: "",
    senha: "",
    admin: false,
    carrinho: new Map(),
    clientes: [],
    // Preenchido para o perfil CLIENTE com o cadastro ligado ao login.
    proprioClienteId: null,
};

const $ = (id) => document.getElementById(id);

function credencial() {
    return "Basic " + btoa(estado.usuario + ":" + estado.senha);
}

/** Chamada para a API com o usuário logado. Lança Error com a mensagem do servidor. */
async function api(caminho, opcoes = {}) {
    const resposta = await fetch(caminho, {
        ...opcoes,
        headers: {
            Authorization: credencial(),
            ...(opcoes.body ? { "Content-Type": "application/json" } : {}),
            ...(opcoes.headers || {}),
        },
    });

    if (resposta.status === 204) {
        return null;
    }

    const texto = await resposta.text();
    let corpo = null;
    try {
        corpo = texto ? JSON.parse(texto) : null;
    } catch (erro) {
        corpo = null;
    }

    if (!resposta.ok) {
        const mensagem = (corpo && (corpo.mensagem || corpo.error)) || "Falha na chamada (" + resposta.status + ")";
        throw new Error(mensagem);
    }

    return corpo;
}

function listaDe(dados, chave) {
    return (dados && dados._embedded && dados._embedded[chave]) || [];
}

/*
 * O Spring Data REST não repete o id no corpo: ele aparece no link "self".
 * Este helper tira o id de lá para o carrinho e o pedido poderem usá-lo.
 */
function idDaEntidade(item) {
    const href = item && item._links && item._links.self && item._links.self.href;
    return href ? href.split("/").pop() : item.id;
}

function dinheiro(valor) {
    return Number(valor).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
}

/* ------------------------------------------------------------------ login */

$("form-login").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const status = $("status-login");
    status.textContent = "Entrando...";
    status.classList.remove("erro");

    estado.usuario = $("usuario").value.trim();
    estado.senha = $("senha").value;

    try {
        await api("/products?size=1");
        // Quem enxerga clientes é administrador.
        try {
            await api("/customers?size=1");
            estado.admin = true;
        } catch (erro) {
            estado.admin = false;
        }

        status.textContent = estado.admin ? "Conectado como administrador" : "Conectado como cliente";
        $("loja").hidden = false;
        $("carrinho").hidden = false;
        $("aba-clientes").hidden = !estado.admin;

        if (estado.admin) {
            await carregarClientes();
        } else {
            await carregarProprioCadastro();
        }
        await carregarCatalogo();
    } catch (erro) {
        $("loja").hidden = true;
        $("carrinho").hidden = true;
        estado.proprioClienteId = null;
        status.textContent = "Não foi possível entrar: " + erro.message;
        status.classList.add("erro");
    }
});

/* ------------------------------------------------------------------ abas */

document.querySelectorAll(".aba").forEach((aba) => {
    aba.addEventListener("click", () => {
        document.querySelectorAll(".aba").forEach((outra) => outra.classList.remove("ativa"));
        aba.classList.add("ativa");

        $("secao-catalogo").hidden = aba.dataset.secao !== "catalogo";
        $("secao-pedidos").hidden = aba.dataset.secao !== "pedidos";
        $("secao-clientes").hidden = aba.dataset.secao !== "clientes";

        if (aba.dataset.secao === "pedidos") {
            carregarPedidos();
        }
        if (aba.dataset.secao === "clientes") {
            carregarClientes();
        }
    });
});

/* --------------------------------------------------------------- catálogo */

$("form-filtro").addEventListener("submit", (evento) => {
    evento.preventDefault();
    carregarCatalogo();
});

async function carregarCatalogo() {
    const aviso = $("aviso-catalogo");
    aviso.textContent = "Carregando catálogo...";
    aviso.classList.remove("erro");

    const categoria = $("categoria").value;
    const termo = $("busca").value.trim();

    let caminho;
    if (termo) {
        caminho = "/products/search/busca?size=100&sort=price,asc&termo=" + encodeURIComponent(termo);
        if (categoria) {
            // A busca livre não filtra por categoria; filtra no navegador.
            caminho += "";
        }
    } else if (categoria) {
        caminho = "/products/search/findByCategory?size=100&category=" + encodeURIComponent(categoria);
    } else {
        caminho = "/products?size=100&sort=name";
    }

    try {
        const dados = await api(caminho);
        let produtos = listaDe(dados, "products");

        if (termo && categoria) {
            produtos = produtos.filter((produto) => produto.category === categoria);
        }

        produtos = produtos.map((produto) => ({ ...produto, id: idDaEntidade(produto) }));
        desenharProdutos(produtos);
        aviso.textContent = produtos.length === 0 ? "Nenhum produto encontrado com esse filtro." : "";
    } catch (erro) {
        aviso.textContent = "Não foi possível carregar o catálogo: " + erro.message;
        aviso.classList.add("erro");
    }
}

function desenharProdutos(produtos) {
    const container = $("produtos");
    container.replaceChildren();

    produtos.forEach((produto) => {
        const cartao = document.createElement("article");
        cartao.className = "produto";

        const nome = document.createElement("h3");
        nome.textContent = produto.name;

        const marca = document.createElement("p");
        marca.className = "marca";
        marca.textContent = [produto.brand, produto.category].filter(Boolean).join(" · ");

        const preco = document.createElement("p");
        preco.className = "preco";
        preco.textContent = dinheiro(produto.price);

        const estoque = document.createElement("p");
        estoque.className = "estoque";
        estoque.textContent = produto.stock > 0 ? produto.stock + " em estoque" : "Sem estoque";

        const botao = document.createElement("button");
        botao.type = "button";
        botao.textContent = "Adicionar ao carrinho";
        botao.disabled = produto.stock <= 0;
        botao.addEventListener("click", () => adicionarAoCarrinho(produto));

        cartao.append(nome, marca, preco, estoque, botao);
        container.append(cartao);
    });
}

/* --------------------------------------------------------------- carrinho */

function adicionarAoCarrinho(produto) {
    const item = estado.carrinho.get(produto.id) || { produto, quantidade: 0 };
    item.quantidade += 1;
    estado.carrinho.set(produto.id, item);
    desenharCarrinho();
}

function desenharCarrinho() {
    const lista = $("itens-carrinho");
    lista.replaceChildren();

    let total = 0;
    estado.carrinho.forEach((item, id) => {
        const subtotal = Number(item.produto.price) * item.quantidade;
        total += subtotal;

        const linha = document.createElement("li");
        const texto = document.createElement("span");
        texto.textContent = item.quantidade + "× " + item.produto.name;

        const valor = document.createElement("span");
        valor.textContent = dinheiro(subtotal);

        const remover = document.createElement("button");
        remover.type = "button";
        remover.className = "secundario";
        remover.textContent = "−";
        remover.setAttribute("aria-label", "Remover uma unidade de " + item.produto.name);
        remover.addEventListener("click", () => {
            item.quantidade -= 1;
            if (item.quantidade <= 0) {
                estado.carrinho.delete(id);
            } else {
                estado.carrinho.set(id, item);
            }
            desenharCarrinho();
        });

        linha.append(texto, valor, remover);
        lista.append(linha);
    });

    $("total-carrinho").textContent = dinheiro(total);
}

$("limpar-carrinho").addEventListener("click", () => {
    estado.carrinho.clear();
    $("aviso-carrinho").textContent = "";
    desenharCarrinho();
});

$("fechar-pedido").addEventListener("click", async () => {
    const aviso = $("aviso-carrinho");
    aviso.classList.remove("erro");

    if (estado.carrinho.size === 0) {
        aviso.textContent = "O carrinho está vazio.";
        return;
    }

    // Administrador escolhe o cliente; o cliente logado compra no próprio nome.
    const clienteId = estado.admin ? $("cliente-pedido").value : estado.proprioClienteId;
    if (!clienteId) {
        aviso.textContent = estado.admin
            ? "Escolha o cliente do pedido."
            : "Este login não está ligado a um cadastro de cliente.";
        aviso.classList.add("erro");
        return;
    }

    const itens = [...estado.carrinho.values()].map((item) => ({
        produtoId: item.produto.id,
        quantidade: item.quantidade,
    }));

    aviso.textContent = "Fechando pedido...";

    try {
        const pedido = await api("/pedidos", {
            method: "POST",
            body: JSON.stringify({ clienteId, itens }),
        });

        estado.carrinho.clear();
        desenharCarrinho();
        aviso.textContent = "Pedido " + pedido.id.slice(0, 8) + " confirmado: " + dinheiro(pedido.total);
        await carregarCatalogo();
        await carregarPedidos();
    } catch (erro) {
        aviso.textContent = erro.message;
        aviso.classList.add("erro");
    }
});

/* ---------------------------------------------------------------- pedidos */

async function carregarPedidos() {
    const aviso = $("aviso-pedidos");
    aviso.textContent = "Carregando pedidos...";
    aviso.classList.remove("erro");

    try {
        const pedidos = await api("/pedidos");
        desenharPedidos(pedidos);
        aviso.textContent = pedidos.length === 0 ? "Você ainda não tem pedidos." : "";
    } catch (erro) {
        aviso.textContent = "Não foi possível carregar os pedidos: " + erro.message;
        aviso.classList.add("erro");
    }
}

function desenharPedidos(pedidos) {
    const container = $("pedidos");
    container.replaceChildren();

    pedidos.forEach((pedido) => {
        const cartao = document.createElement("article");
        cartao.className = "pedido";

        const cabecalho = document.createElement("header");

        const identificacao = document.createElement("div");
        const cliente = document.createElement("strong");
        cliente.textContent = pedido.cliente;

        const rotulo = document.createElement("div");
        rotulo.className = "rotulo";
        rotulo.textContent = new Date(pedido.criadoEm).toLocaleString("pt-BR")
            + " · " + pedido.status.toLowerCase();

        identificacao.append(cliente, rotulo);

        const total = document.createElement("strong");
        total.textContent = dinheiro(pedido.total);

        cabecalho.append(identificacao, total);

        const itens = document.createElement("ul");
        pedido.itens.forEach((item) => {
            const linha = document.createElement("li");
            linha.textContent = item.quantidade + "× " + item.produto + " — " + dinheiro(item.subtotal);
            itens.append(linha);
        });

        cartao.append(cabecalho, itens);

        if (pedido.status === "CONFIRMADO") {
            const cancelar = document.createElement("button");
            cancelar.type = "button";
            cancelar.className = "secundario";
            cancelar.textContent = "Cancelar pedido";
            cancelar.addEventListener("click", async () => {
                try {
                    await api("/pedidos/" + pedido.id + "/cancelar", { method: "POST" });
                    await carregarPedidos();
                    await carregarCatalogo();
                } catch (erro) {
                    $("aviso-pedidos").textContent = erro.message;
                    $("aviso-pedidos").classList.add("erro");
                }
            });
            cartao.append(cancelar);
        }

        container.append(cartao);
    });
}

/* --------------------------------------------------------------- clientes */

/**
 * Descobre de quem é o login atual. Com o id em mãos, o carrinho fecha o pedido
 * no nome da própria pessoa e o seletor de cliente some da tela.
 */
async function carregarProprioCadastro() {
    try {
        const eu = await api("/clientes/eu");
        estado.proprioClienteId = eu.id;
        $("comprando-como").textContent = "Comprando como " + eu.nome + " (" + eu.email + ")";
        $("cliente-pedido").hidden = true;
        $("rotulo-cliente").hidden = true;
    } catch (erro) {
        estado.proprioClienteId = null;
        $("comprando-como").textContent = "Sem cadastro de cliente para este login: " + erro.message;
    }
}

async function carregarClientes() {
    if (!estado.admin) {
        return;
    }

    try {
        const dados = await api("/customers?size=100");
        estado.clientes = listaDe(dados, "customers").map((cliente) => ({
            id: idDaEntidade(cliente),
            nome: [cliente.firstName, cliente.lastName].filter(Boolean).join(" "),
            endereco: cliente.address,
        }));

        desenharClientes();
        preencherSeletorDeClientes();
    } catch (erro) {
        $("aviso-cliente").textContent = "Não foi possível carregar os clientes: " + erro.message;
        $("aviso-cliente").classList.add("erro");
    }
}

function desenharClientes() {
    const container = $("clientes");
    container.replaceChildren();

    estado.clientes.forEach((cliente) => {
        const cartao = document.createElement("article");
        cartao.className = "pedido cliente";

        const nome = document.createElement("strong");
        nome.textContent = cliente.nome;

        const endereco = document.createElement("div");
        endereco.className = "rotulo";
        endereco.textContent = cliente.endereco || "Sem endereço";

        const anonimizar = document.createElement("button");
        anonimizar.type = "button";
        anonimizar.className = "secundario";
        anonimizar.textContent = "Anonimizar (LGPD)";
        anonimizar.addEventListener("click", async () => {
            if (!confirm("Apagar nome, e-mail e endereço de " + cliente.nome + "?")) {
                return;
            }
            try {
                await api("/clientes/" + cliente.id + "/anonimizar", { method: "POST" });
                await carregarClientes();
            } catch (erro) {
                $("aviso-cliente").textContent = erro.message;
                $("aviso-cliente").classList.add("erro");
            }
        });

        cartao.append(nome, endereco, anonimizar);
        container.append(cartao);
    });
}

function preencherSeletorDeClientes() {
    const seletor = $("cliente-pedido");
    seletor.replaceChildren();

    const vazio = document.createElement("option");
    vazio.value = "";
    vazio.textContent = estado.clientes.length ? "Escolha um cliente" : "Nenhum cliente cadastrado";
    seletor.append(vazio);

    estado.clientes.forEach((cliente) => {
        const opcao = document.createElement("option");
        opcao.value = cliente.id;
        opcao.textContent = cliente.nome;
        seletor.append(opcao);
    });
}

$("form-cliente").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const aviso = $("aviso-cliente");
    aviso.classList.remove("erro");
    aviso.textContent = "Salvando...";

    const corpo = {
        firstName: $("nome").value.trim(),
        lastName: $("sobrenome").value.trim() || null,
        email: $("email").value.trim(),
        address: {
            street: $("rua").value.trim() || null,
            postalCode: $("cep").value.trim() || null,
            city: $("cidade").value.trim() || null,
            state: $("estado").value.trim() || null,
        },
    };

    try {
        await api("/customers", { method: "POST", body: JSON.stringify(corpo) });
        aviso.textContent = "Cliente cadastrado.";
        $("form-cliente").reset();
        await carregarClientes();
    } catch (erro) {
        aviso.textContent = erro.message;
        aviso.classList.add("erro");
    }
});
