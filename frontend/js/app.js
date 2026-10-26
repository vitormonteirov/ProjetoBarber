// Elementos do DOM
const formCliente = document.getElementById('form-cliente');
const listaClientes = document.getElementById('lista-clientes');
const apiStatusDot = document.getElementById('api-status-dot');
const apiStatusText = document.getElementById('api-status-text');
const btnRefresh = document.getElementById('btn-refresh');
const msgFeedback = document.getElementById('msg-feedback');

function exibirMensagem(texto, tipo = 'success') {
    msgFeedback.textContent = texto;
    msgFeedback.className = `alert ${tipo}`;
    setTimeout(() => {
        msgFeedback.className = 'alert';
    }, 4000);
}

// Carregar clientes do backend
async function carregarClientes() {
    try {
        const data = await api.listarClientes();
        
        // Atualiza status visual de conexão com a API
        apiStatusDot.className = 'dot online';
        apiStatusText.textContent = 'Backend Online (porta 8080)';

        listaClientes.innerHTML = '';

        // No Spring Boot Pageable, a lista de itens fica na propriedade 'content'
        const clientes = data.content || [];

        if (clientes.length === 0) {
            listaClientes.innerHTML = '<p class="empty-state">Nenhum cliente cadastrado ainda.</p>';
            return;
        }

        clientes.forEach(cliente => {
            const card = document.createElement('div');
            card.className = 'item-card';
            card.innerHTML = `
                <h3>#${cliente.id} - ${cliente.nome}</h3>
                <p>📞 <strong>Tel:</strong> ${cliente.telefone}</p>
                <p>✉️ <strong>E-mail:</strong> ${cliente.email}</p>
            `;
            listaClientes.appendChild(card);
        });
    } catch (erro) {
        console.error('Erro na integração:', erro);
        apiStatusDot.className = 'dot offline';
        apiStatusText.textContent = 'Backend Offline ou não alcançável';
        listaClientes.innerHTML = `
            <p class="empty-state" style="color: var(--danger);">
                ⚠️ Não foi possível conectar ao back-end.<br>
                Certifique-se de que o Spring Boot está rodando em <code>http://localhost:8080</code>.
            </p>
        `;
    }
}

// Submissão do formulário de cadastro de cliente
formCliente.addEventListener('submit', async (e) => {
    e.preventDefault();

    const novoCliente = {
        nome: document.getElementById('nome').value.trim(),
        telefone: document.getElementById('telefone').value.trim(),
        email: document.getElementById('email').value.trim()
    };

    try {
        await api.criarCliente(novoCliente);
        exibirMensagem(`Cliente "${novoCliente.nome}" cadastrado com sucesso!`, 'success');
        formCliente.reset();
        await carregarClientes();
    } catch (erro) {
        exibirMensagem(erro.message, 'error');
    }
});

btnRefresh.addEventListener('click', carregarClientes);

// Carregamento inicial ao abrir a página
document.addEventListener('DOMContentLoaded', () => {
    carregarClientes();
});
