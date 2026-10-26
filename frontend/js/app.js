/**
 * Lógica do Frontpage BarberShop - Integração com API Spring Boot
 */

// Estado da Aplicação
const state = {
    servicos: [],
    barbeiros: [],
    clientes: [],
    agendamentos: [],
    modoNovoCliente: false,
    selecionado: {
        servico: null,
        barbeiro: null,
        clienteId: null,
        dataHora: ''
    }
};

// Formatação de Moeda Brasileira
const formatadorMoeda = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
});

// Formatação de Data e Hora
function formatarDataHora(isoString) {
    if (!isoString) return '--';
    const data = new Date(isoString);
    return data.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Sistema de Notificações Toast
function showToast(mensagem, tipo = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${tipo}`;
    toast.innerHTML = `
        <span>${tipo === 'success' ? '✅' : '❌'}</span>
        <div>${mensagem}</div>
    `;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

// Inicialização da Página
document.addEventListener('DOMContentLoaded', async () => {
    configurarDataMinima();
    configurarEventos();
    await verificarStatusBackend();
    await carregarDadosIniciais();
});

// Configura para que o input de data/hora aceite apenas horários futuros (@Future no Java)
function configurarDataMinima() {
    const inputData = document.getElementById('input-data-hora');
    const agora = new Date();
    // Adiciona 10 minutos de margem mínima
    agora.setMinutes(agora.getMinutes() + 10);
    const ano = agora.getFullYear();
    const mes = String(agora.getMonth() + 1).padStart(2, '0');
    const dia = String(agora.getDate()).padStart(2, '0');
    const horas = String(agora.getHours()).padStart(2, '0');
    const minutos = String(agora.getMinutes()).padStart(2, '0');
    inputData.min = `${ano}-${mes}-${dia}T${horas}:${minutos}`;
}

// Verifica conexão com o backend Spring Boot
async function verificarStatusBackend() {
    const dot = document.getElementById('api-dot');
    const text = document.getElementById('api-text');

    const online = await api.verificarConexao();
    if (online) {
        dot.className = 'dot online';
        text.textContent = 'Backend Online (Porta 8080)';
    } else {
        dot.className = 'dot offline';
        text.textContent = 'Backend Offline';
    }
}

// Carrega todos os catálogos da API
async function carregarDadosIniciais() {
    try {
        const [servicos, barbeiros, clientes] = await Promise.all([
            api.listarServicos().catch(() => []),
            api.listarBarbeiros().catch(() => []),
            api.listarClientes().catch(() => [])
        ]);

        state.servicos = servicos;
        state.barbeiros = barbeiros;
        state.clientes = clientes;

        renderizarServicos();
        renderizarBarbeiros();
        renderizarSelects();
    } catch (err) {
        console.error('Falha ao inicializar dados:', err);
        showToast('Não foi possível obter dados da API.', 'error');
    }
}

// --- RENDERIZAR SERVIÇOS ---
function renderizarServicos() {
    const grid = document.getElementById('grid-servicos');
    if (!state.servicos.length) {
        grid.innerHTML = '<p class="empty-state" style="grid-column: 1/-1;">Nenhum serviço cadastrado no sistema.</p>';
        return;
    }

    grid.innerHTML = state.servicos.map(servico => `
        <div class="interactive-card ${state.selecionado.servico?.id === servico.id ? 'selected' : ''}" 
             data-id="${servico.id}" onclick="selecionarServico(${servico.id})">
            <div class="card-top">
                <span class="card-badge">⏱ ${servico.duracaoMinutos || 30} min</span>
            </div>
            <h3 class="card-title">${servico.nome}</h3>
            <p class="card-desc">${servico.descricao || 'Serviço de alta precisão e acabamento impecável.'}</p>
            <div class="card-footer">
                <span class="price-tag">${formatadorMoeda.format(servico.preco || 0)}</span>
                <span style="color: var(--blue-accent); font-weight: 600; font-size: 0.85rem;">Escolher →</span>
            </div>
        </div>
    `).join('');
}

// --- RENDERIZAR BARBEIROS ---
function renderizarBarbeiros() {
    const grid = document.getElementById('grid-barbeiros');
    if (!state.barbeiros.length) {
        grid.innerHTML = '<p class="empty-state" style="grid-column: 1/-1;">Nenhum barbeiro ativo cadastrado.</p>';
        return;
    }

    grid.innerHTML = state.barbeiros.map(barbeiro => {
        const iniciais = barbeiro.nome.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase();
        return `
            <div class="interactive-card ${state.selecionado.barbeiro?.id === barbeiro.id ? 'selected' : ''}" 
                 data-id="${barbeiro.id}" onclick="selecionarBarbeiro(${barbeiro.id})">
                <div class="avatar-circle">${iniciais}</div>
                <h3 class="card-title">${barbeiro.nome}</h3>
                <p class="card-desc">📞 ${barbeiro.telefone || 'Sem telefone'}<br>✉️ ${barbeiro.email || ''}</p>
                <div class="card-footer">
                    <span class="card-badge" style="background: rgba(16, 185, 129, 0.15); color: var(--success);">Disponível</span>
                    <span style="color: var(--blue-accent); font-weight: 600; font-size: 0.85rem;">Selecionar →</span>
                </div>
            </div>
        `;
    }).join('');
}

// Popula os seletores de Serviços, Barbeiros e Clientes
function renderizarSelects() {
    // Select Serviços
    const selectServico = document.getElementById('select-servico');
    selectServico.innerHTML = '<option value="">-- Escolha um serviço --</option>' +
        state.servicos.map(s => `<option value="${s.id}">${s.nome} - ${formatadorMoeda.format(s.preco)}</option>`).join('');

    // Select Barbeiros
    const selectBarbeiro = document.getElementById('select-barbeiro');
    selectBarbeiro.innerHTML = '<option value="">-- Escolha um barbeiro --</option>' +
        state.barbeiros.map(b => `<option value="${b.id}">${b.nome}</option>`).join('');

    // Select Clientes
    const selectCliente = document.getElementById('select-cliente');
    selectCliente.innerHTML = '<option value="">-- Selecione seu nome --</option>' +
        state.clientes.map(c => `<option value="${c.id}">${c.nome} (${c.telefone})</option>`).join('');
}

// Seleção de Serviço (via Card ou Select)
window.selecionarServico = function(id) {
    const servico = state.servicos.find(s => s.id === Number(id));
    state.selecionado.servico = servico || null;
    document.getElementById('select-servico').value = servico ? servico.id : '';
    renderizarServicos();
    atualizarResumo();

    // Rola até o formulário se for pelo card
    const formElement = document.getElementById('agendamento');
    if (formElement && window.scrollY < formElement.offsetTop - 300) {
        formElement.scrollIntoView({ behavior: 'smooth' });
    }
};

// Seleção de Barbeiro (via Card ou Select)
window.selecionarBarbeiro = function(id) {
    const barbeiro = state.barbeiros.find(b => b.id === Number(id));
    state.selecionado.barbeiro = barbeiro || null;
    document.getElementById('select-barbeiro').value = barbeiro ? barbeiro.id : '';
    renderizarBarbeiros();
    atualizarResumo();
};

// Atualiza o painel lateral de resumo
function atualizarResumo() {
    const servico = state.selecionado.servico;
    const barbeiro = state.selecionado.barbeiro;
    const dataHoraVal = document.getElementById('input-data-hora').value;

    document.getElementById('resumo-servico').textContent = servico ? servico.nome : 'Nenhum selecionado';
    document.getElementById('resumo-duracao').textContent = servico ? `${servico.duracaoMinutos || 30} minutos` : '--';
    document.getElementById('resumo-barbeiro').textContent = barbeiro ? barbeiro.nome : 'Nenhum selecionado';
    document.getElementById('resumo-data').textContent = dataHoraVal ? formatarDataHora(dataHoraVal) : 'Não definido';
    document.getElementById('resumo-preco').textContent = servico ? formatadorMoeda.format(servico.preco) : 'R$ 0,00';
}

// --- RENDERIZAR TABELA DE AGENDAMENTOS ---
// --- MODAL DE CONFIRMAÇÃO ---
function abrirModalConfirmacao(agendamento) {
    document.getElementById('modal-protocolo').textContent = `#${agendamento.id}`;
    document.getElementById('modal-cliente').textContent = agendamento.clienteNome || 'Cliente #' + agendamento.clienteId;
    document.getElementById('modal-servico').textContent = agendamento.servicoNome || state.selecionado.servico?.nome || 'Serviço Selecionado';
    document.getElementById('modal-barbeiro').textContent = agendamento.barbeiroNome || state.selecionado.barbeiro?.nome || 'Barbeiro Selecionado';
    document.getElementById('modal-data').textContent = formatarDataHora(agendamento.dataHora);
    document.getElementById('modal-preco').textContent = agendamento.preco ? formatadorMoeda.format(agendamento.preco) : (state.selecionado.servico?.preco ? formatadorMoeda.format(state.selecionado.servico.preco) : 'R$ 0,00');

    document.getElementById('modal-confirmacao').style.display = 'flex';
}

function fecharModalConfirmacao() {
    document.getElementById('modal-confirmacao').style.display = 'none';
}

// Eventos da Interface
function configurarEventos() {
    // Modal de Confirmação
    document.getElementById('btn-fechar-modal').addEventListener('click', fecharModalConfirmacao);

    // Selects
    document.getElementById('select-servico').addEventListener('change', (e) => {
        selecionarServico(e.target.value);
    });

    document.getElementById('select-barbeiro').addEventListener('change', (e) => {
        selecionarBarbeiro(e.target.value);
    });

    document.getElementById('input-data-hora').addEventListener('change', atualizarResumo);

    // Alternar tipo de cliente (Existente vs Novo)
    const tabExistente = document.getElementById('tab-cliente-existente');
    const tabNovo = document.getElementById('tab-cliente-novo');
    const boxExistente = document.getElementById('box-cliente-existente');
    const boxNovo = document.getElementById('box-cliente-novo');

    tabExistente.addEventListener('click', () => {
        state.modoNovoCliente = false;
        tabExistente.classList.add('active');
        tabNovo.classList.remove('active');
        boxExistente.style.display = 'block';
        boxNovo.style.display = 'none';
    });

    tabNovo.addEventListener('click', () => {
        state.modoNovoCliente = true;
        tabNovo.classList.add('active');
        tabExistente.classList.remove('active');
        boxExistente.style.display = 'none';
        boxNovo.style.display = 'block';
    });

    // Submissão do Formulário de Agendamento
    const form = document.getElementById('form-booking');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const btnSubmit = document.getElementById('btn-submit-booking');
        btnSubmit.disabled = true;
        btnSubmit.textContent = 'Processando agendamento...';

        try {
            // 1. Validar e obter Cliente
            let clienteId;
            if (state.modoNovoCliente) {
                const nome = document.getElementById('novo-nome').value.trim();
                const telefone = document.getElementById('novo-telefone').value.trim();
                const email = document.getElementById('novo-email').value.trim();

                if (!nome || !telefone || !email) {
                    throw new Error('Por favor, preencha todos os campos do novo cliente.');
                }

                // Cria o cliente no backend
                const novoCliente = await api.criarCliente({ nome, telefone, email });
                clienteId = novoCliente.id;
                showToast(`Cadastro de ${novoCliente.nome} realizado!`);
                
                // Atualiza lista de clientes
                state.clientes = await api.listarClientes();
                renderizarSelects();
            } else {
                clienteId = document.getElementById('select-cliente').value;
                if (!clienteId) throw new Error('Por favor, selecione um cliente existente.');
            }

            // 2. Validar Barbeiro e Serviço
            const barbeiroId = document.getElementById('select-barbeiro').value;
            const servicoId = document.getElementById('select-servico').value;
            const rawDataHora = document.getElementById('input-data-hora').value;

            if (!barbeiroId) throw new Error('Selecione um barbeiro.');
            if (!servicoId) throw new Error('Selecione um serviço.');
            if (!rawDataHora) throw new Error('Informe a data e o horário.');

            // O Spring Boot espera formato ISO: "YYYY-MM-DDTHH:mm:ss"
            const dataHoraIso = rawDataHora.length === 16 ? `${rawDataHora}:00` : rawDataHora;

            // 3. Montar payload do AgendamentoDTO
            const agendamentoPayload = {
                clienteId: Number(clienteId),
                barbeiroId: Number(barbeiroId),
                servicoId: Number(servicoId),
                dataHora: dataHoraIso
            };

            // 4. Enviar para a API
            const agendamentoCriado = await api.criarAgendamento(agendamentoPayload);
            showToast(`🎉 Agendamento #${agendamentoCriado.id} confirmado com sucesso!`, 'success');

            // 5. Reset do formulário e feedback
            form.reset();
            state.selecionado.servico = null;
            state.selecionado.barbeiro = null;
            renderizarServicos();
            renderizarBarbeiros();
            atualizarResumo();
            configurarDataMinima();

            // Exibe modal com detalhes da reserva
            abrirModalConfirmacao(agendamentoCriado);

        } catch (err) {
            console.error('Falha no agendamento:', err);
            showToast(err.message, 'error');
        } finally {
            btnSubmit.disabled = false;
            btnSubmit.textContent = 'Confirmar Agendamento';
        }
    });
}
