/**
 * Aplicação Principal Tenorio's Barber
 * Especialista Front-end & UX/UI: Fluxo Guiado, Alta Usabilidade e Integração REST
 */

// Estado Global da Aplicação
const appState = {
    step: 1,
    servicos: [],
    barbeiros: [],
    slotsDisponiveis: [],
    carregandoSlots: false,
    modoAuth: 'novo', // 'novo' | 'login'
    backendOnline: false,
    selecionado: {
        servico: null,
        barbeiro: null,
        dataIso: '',
        horario: ''
    }
};

// Formatação Monetária BRL
const formatadorMoeda = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL'
});

// Dados de Demonstração / Fallback gracioso quando o backend estiver inicializando
const MOCK_SERVICOS = [
    { id: 1, nome: "Corte Tradicional & Lavagem", preco: 50.00, duracaoMinutos: 35, descricao: "Corte na tesoura ou máquina com alinhamento, acabamento impecável na navalha e finalização com pomada matte." },
    { id: 2, nome: "Barba Terapia Completa", preco: 45.00, duracaoMinutos: 30, descricao: "Toalha quente emoliente, óleo para barba premium, alinhamento preciso na navalha e pós-barba refrescante." },
    { id: 3, nome: "Combo Cabelo & Barba VIP", preco: 85.00, duracaoMinutos: 60, descricao: "O clássico completo: corte de cabelo personalizado + barba terapia relaxante com direito a massagem facial." },
    { id: 4, nome: "Acabamento & Pezinho", preco: 25.00, duracaoMinutos: 20, descricao: "Manutenção do contorno do cabelo e costeletas para manter o visual sempre alinhado para a semana." }
];

const MOCK_BARBEIROS = [
    { id: 1, nome: "Vitor Tenorio", email: "vitor@tenoriosbarber.com", telefone: "(11) 98888-1111", ativo: true },
    { id: 2, nome: "Carlos 'Navalha' Silva", email: "carlos@tenoriosbarber.com", telefone: "(11) 98888-2222", ativo: true },
    { id: 3, nome: "Diego Ramos", email: "diego@tenoriosbarber.com", telefone: "(11) 98888-3333", ativo: true }
];

// --- NOTIFICAÇÕES TOAST ---
function showToast(mensagem, tipo = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${tipo}`;
    
    let icone = '✅';
    if (tipo === 'error') icone = '⚠️';
    if (tipo === 'info') icone = 'ℹ️';

    toast.innerHTML = `
        <span style="font-size: 1.2rem;">${icone}</span>
        <div style="font-size: 0.9rem; font-weight: 500;">${mensagem}</div>
    `;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'toastSlideIn 0.3s ease reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

// --- FORMATAÇÕES DE DATA ---
function getHojeIso() {
    const hoje = new Date();
    const ano = hoje.getFullYear();
    const mes = String(hoje.getMonth() + 1).padStart(2, '0');
    const dia = String(hoje.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
}

function getAmanhaIso() {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    const ano = d.getFullYear();
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
}

function formatarDataExtenso(isoDate) {
    if (!isoDate) return 'Não definida';
    const partes = isoDate.split('-');
    if (partes.length === 3) {
        return `${partes[2]}/${partes[1]}/${partes[0]}`;
    }
    return isoDate;
}

// --- INICIALIZAÇÃO DA APLICAÇÃO ---
document.addEventListener('DOMContentLoaded', async () => {
    // Configura data padrão inicial para "Hoje"
    appState.selecionado.dataIso = getHojeIso();

    const datePicker = document.getElementById('booking-date-picker');
    if (datePicker) {
        datePicker.min = getHojeIso();
        datePicker.value = getHojeIso();
    }

    configurarEventosUI();
    atualizarEstadoAuth();
    await carregarCatalogo();
});

// Verifica status do backend e carrega dados
async function carregarCatalogo() {
    const dot = document.getElementById('api-dot');
    const text = document.getElementById('api-text');

    const online = await api.verificarConexao();
    appState.backendOnline = online;

    if (online) {
        dot.className = 'dot online';
        text.textContent = 'API Conectada (8080)';
        try {
            const [servicos, barbeiros] = await Promise.all([
                api.listarServicos().catch(() => []),
                api.listarBarbeiros().catch(() => [])
            ]);

            appState.servicos = servicos.length ? servicos : MOCK_SERVICOS;
            appState.barbeiros = barbeiros.length ? barbeiros : MOCK_BARBEIROS;
        } catch (err) {
            console.warn('Erro ao carregar do backend, usando dados demonstrativos:', err);
            appState.servicos = MOCK_SERVICOS;
            appState.barbeiros = MOCK_BARBEIROS;
        }
    } else {
        dot.className = 'dot offline';
        text.textContent = 'Modo Demonstração';
        appState.servicos = MOCK_SERVICOS;
        appState.barbeiros = MOCK_BARBEIROS;
    }

    renderizarCatalogoGeral();
    renderizarPasso1Servicos();
    renderizarPasso2Barbeiros();
}

// Atualiza Navbar e telas de acordo com a sessão do usuário
function atualizarEstadoAuth() {
    const user = api.getUsuarioLogado();
    const btnLoginNav = document.getElementById('btn-abrir-login');
    const userBadgeNav = document.getElementById('user-badge');
    const navMeusAgendamentos = document.getElementById('nav-meus-agendamentos-item');

    const cardLogged = document.getElementById('cliente-autenticado-box');
    const boxDeslogado = document.getElementById('cliente-deslogado-box');

    if (user && user.nome) {
        if (btnLoginNav) btnLoginNav.style.display = 'none';
        if (userBadgeNav) userBadgeNav.style.display = 'flex';
        if (navMeusAgendamentos) navMeusAgendamentos.style.display = 'block';

        const avatarLetter = user.nome.charAt(0).toUpperCase();
        const navAvatar = document.getElementById('nav-user-avatar');
        const navName = document.getElementById('nav-user-name');
        if (navAvatar) navAvatar.textContent = avatarLetter;
        if (navName) navName.textContent = user.nome.split(' ')[0];

        // Passo 4 do Agendamento
        if (cardLogged) {
            cardLogged.style.display = 'flex';
            document.getElementById('logged-user-avatar').textContent = avatarLetter;
            document.getElementById('logged-user-name').textContent = user.nome;
            document.getElementById('logged-user-email').textContent = user.email;
        }
        if (boxDeslogado) boxDeslogado.style.display = 'none';

    } else {
        if (btnLoginNav) btnLoginNav.style.display = 'inline-flex';
        if (userBadgeNav) userBadgeNav.style.display = 'none';
        if (navMeusAgendamentos) navMeusAgendamentos.style.display = 'none';

        if (cardLogged) cardLogged.style.display = 'none';
        if (boxDeslogado) boxDeslogado.style.display = 'block';
    }
}

// --- RENDERIZAÇÃO DOS CATÁLOGOS NA PÁGINA INICIAL ---
function renderizarCatalogoGeral() {
    // Serviços na vitrine
    const gridServicos = document.getElementById('grid-servicos');
    if (gridServicos) {
        gridServicos.innerHTML = appState.servicos.map(s => `
            <div class="service-card">
                <div>
                    <div class="service-card-top">
                        <span class="service-badge">⏱ ${s.duracaoMinutos || 30} min</span>
                        <span class="service-price">${formatadorMoeda.format(s.preco || 0)}</span>
                    </div>
                    <h3 class="service-title">${s.nome}</h3>
                    <p class="service-desc">${s.descricao || 'Atendimento com produtos selecionados e alto rigor estético.'}</p>
                </div>
                <div class="service-footer">
                    <button type="button" class="btn btn-primary w-full" onclick="iniciarAgendamentoComServico(${s.id})">
                        Agendar Este Serviço →
                    </button>
                </div>
            </div>
        `).join('');
    }

    // Barbeiros na vitrine
    const gridBarbeiros = document.getElementById('grid-barbeiros');
    if (gridBarbeiros) {
        gridBarbeiros.innerHTML = appState.barbeiros.map(b => {
            const iniciais = b.nome.split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase();
            return `
                <div class="barber-card">
                    <div>
                        <div class="barber-avatar-box">${iniciais}</div>
                        <h3 class="service-title">${b.nome}</h3>
                        <p class="service-desc" style="margin-bottom: 0.8rem;">
                            Especialista em visuais modernos, degradê navalhado e barba alinhada.
                        </p>
                        <span class="barber-status-badge">● Disponível para agendamento</span>
                    </div>
                    <div class="service-footer" style="margin-top: 1.5rem;">
                        <button type="button" class="btn btn-ghost w-full" onclick="iniciarAgendamentoComBarbeiro(${b.id})">
                            Agendar com ${b.nome.split(' ')[0]} →
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }
}

// --- RENDERIZAÇÃO DO WIZARD PASSO 1 (SERVIÇOS) ---
function renderizarPasso1Servicos() {
    const container = document.getElementById('wizard-servicos-list');
    if (!container) return;

    container.innerHTML = appState.servicos.map(s => {
        const isSelected = appState.selecionado.servico?.id === s.id;
        return `
            <div class="wizard-card-item ${isSelected ? 'selected' : ''}" onclick="selecionarServicoWizard(${s.id})">
                <div class="item-name">${s.nome}</div>
                <div class="item-info">⏱ ${s.duracaoMinutos || 30} minutos de atendimento</div>
                <div class="item-price">${formatadorMoeda.format(s.preco || 0)}</div>
            </div>
        `;
    }).join('');

    const btnNext = document.getElementById('btn-next-step-1');
    if (btnNext) {
        btnNext.disabled = !appState.selecionado.servico;
    }
}

// --- RENDERIZAÇÃO DO WIZARD PASSO 2 (BARBEIROS) ---
function renderizarPasso2Barbeiros() {
    const container = document.getElementById('wizard-barbeiros-list');
    if (!container) return;

    // Adiciona opção "Qualquer Barbeiro" para máxima facilidade + barbeiros cadastrados
    let html = '';

    appState.barbeiros.forEach(b => {
        const isSelected = appState.selecionado.barbeiro?.id === b.id;
        html += `
            <div class="wizard-card-item ${isSelected ? 'selected' : ''}" onclick="selecionarBarbeiroWizard(${b.id})">
                <div class="item-name">${b.nome}</div>
                <div class="item-info">Barbeiro Profissional</div>
                <span class="barber-status-badge">● Atendendo hoje</span>
            </div>
        `;
    });

    container.innerHTML = html;

    const btnNext = document.getElementById('btn-next-step-2');
    if (btnNext) {
        btnNext.disabled = !appState.selecionado.barbeiro;
    }
}

// --- RENDERIZAÇÃO DO WIZARD PASSO 3 (HORÁRIOS / SLOTS) ---
async function carregarSlotsDisponiveis() {
    const container = document.getElementById('slots-container');
    const badge = document.getElementById('slots-count-badge');
    const btnNext = document.getElementById('btn-next-step-3');

    if (!container) return;

    const barbeiroId = appState.selecionado.barbeiro?.id;
    const servicoId = appState.selecionado.servico?.id;
    const dataIso = appState.selecionado.dataIso;

    if (!barbeiroId || !dataIso) {
        container.innerHTML = '<p class="slots-empty-state">Por favor, complete a escolha do barbeiro e da data.</p>';
        return;
    }

    container.innerHTML = '<p class="slots-empty-state"><span class="spinner"></span> Consultando horários livres na agenda...</p>';
    if (badge) badge.textContent = 'Buscando...';

    let slots = [];

    if (appState.backendOnline) {
        try {
            slots = await api.consultarDisponibilidade(barbeiroId, servicoId, dataIso);
        } catch (err) {
            console.warn('Falha ao consultar disponibilidade no backend, gerando slots locais:', err);
            slots = gerarSlotsMock(dataIso);
        }
    } else {
        slots = gerarSlotsMock(dataIso);
    }

    appState.slotsDisponiveis = slots || [];

    if (!slots || slots.length === 0) {
        container.innerHTML = `
            <div class="slots-empty-state">
                <p>Nenhum horário disponível para esta data com este barbeiro.</p>
                <small style="color: var(--blue-accent); display: block; margin-top: 0.5rem;">Tente selecionar outro dia ou outro profissional.</small>
            </div>
        `;
        if (badge) badge.textContent = '0 livres';
        if (btnNext) btnNext.disabled = true;
        return;
    }

    if (badge) badge.textContent = `${slots.length} disponíveis`;

    // Renderiza cada horário em chips clicáveis
    container.innerHTML = slots.map(slotTime => {
        // Formata "08:00:00" ou "08:00" para "08:00"
        const horaCurta = slotTime.substring(0, 5);
        const isSelected = appState.selecionado.horario === horaCurta;

        return `
            <button type="button" class="slot-btn ${isSelected ? 'selected' : ''}" 
                    onclick="selecionarHorarioSlot('${horaCurta}')">
                ${horaCurta}
            </button>
        `;
    }).join('');

    if (btnNext) {
        btnNext.disabled = !appState.selecionado.horario;
    }
}

// Gera slots de demonstração se a API estiver offline
function gerarSlotsMock(dataIso) {
    const lista = [];
    const agora = new Date();
    const dataEscolhida = new Date(dataIso + 'T00:00:00');
    const isHoje = dataEscolhida.toDateString() === agora.toDateString();

    for (let h = 8; h <= 18; h++) {
        for (let m of [0, 30]) {
            const horaStr = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
            if (isHoje) {
                const horaSlot = new Date();
                horaSlot.setHours(h, m, 0, 0);
                if (horaSlot > agora) {
                    lista.push(horaStr);
                }
            } else {
                lista.push(horaStr);
            }
        }
    }
    return lista;
}

// --- SELEÇÕES DO USUÁRIO ---
window.selecionarServicoWizard = function(id) {
    const servico = appState.servicos.find(s => s.id === Number(id));
    if (!servico) return;
    appState.selecionado.servico = servico;
    renderizarPasso1Servicos();
    atualizarResumoLateral();
    
    // Avança suavemente para o passo 2
    setTimeout(() => irParaPasso(2), 200);
};

window.selecionarBarbeiroWizard = function(id) {
    const barbeiro = appState.barbeiros.find(b => b.id === Number(id));
    if (!barbeiro) return;
    appState.selecionado.barbeiro = barbeiro;
    renderizarPasso2Barbeiros();
    atualizarResumoLateral();

    // Avança suavemente para o passo 3 e carrega slots
    setTimeout(() => {
        irParaPasso(3);
        carregarSlotsDisponiveis();
    }, 200);
};

window.selecionarHorarioSlot = function(horaCurta) {
    appState.selecionado.horario = horaCurta;
    
    // Atualiza chips selecionados
    document.querySelectorAll('.slot-btn').forEach(btn => {
        btn.classList.toggle('selected', btn.textContent.trim() === horaCurta);
    });

    const btnNext = document.getElementById('btn-next-step-3');
    if (btnNext) btnNext.disabled = false;

    atualizarResumoLateral();

    // Feedback sonoro/visual e avanço para Passo 4
    setTimeout(() => irParaPasso(4), 250);
};

// Ações a partir da vitrine na Home
window.iniciarAgendamentoComServico = function(id) {
    const servico = appState.servicos.find(s => s.id === Number(id));
    if (servico) {
        appState.selecionado.servico = servico;
        renderizarPasso1Servicos();
        atualizarResumoLateral();
        irParaPasso(2);
        document.getElementById('agendamento').scrollIntoView({ behavior: 'smooth' });
    }
};

window.iniciarAgendamentoComBarbeiro = function(id) {
    const barbeiro = appState.barbeiros.find(b => b.id === Number(id));
    if (barbeiro) {
        appState.selecionado.barbeiro = barbeiro;
        renderizarPasso2Barbeiros();
        atualizarResumoLateral();
        // Se ainda não escolheu serviço, pede para escolher primeiro
        if (!appState.selecionado.servico) {
            irParaPasso(1);
            showToast(`Barbeiro ${barbeiro.nome.split(' ')[0]} selecionado! Agora escolha o serviço.`, 'info');
        } else {
            irParaPasso(3);
            carregarSlotsDisponiveis();
        }
        document.getElementById('agendamento').scrollIntoView({ behavior: 'smooth' });
    }
};

// --- NAVEGAÇÃO ENTRE OS PASSOS DO WIZARD ---
function irParaPasso(passoNum) {
    appState.step = passoNum;

    // Atualiza stepper visual
    for (let i = 1; i <= 4; i++) {
        const btn = document.getElementById(`step-btn-${i}`);
        const pane = document.getElementById(`wizard-pane-${i}`);
        
        if (btn) {
            btn.classList.remove('active', 'completed');
            if (i === passoNum) {
                btn.classList.add('active');
            } else if (i < passoNum) {
                btn.classList.add('completed');
            }
        }

        if (pane) {
            pane.classList.toggle('active', i === passoNum);
        }
    }
}

// --- ATUALIZAÇÃO DO RESUMO LATERAL FLUTUANTE ---
function atualizarResumoLateral() {
    const s = appState.selecionado.servico;
    const b = appState.selecionado.barbeiro;
    const d = appState.selecionado.dataIso;
    const h = appState.selecionado.horario;

    document.getElementById('resumo-servico').textContent = s ? s.nome : 'Selecione no passo 1';
    document.getElementById('resumo-duracao').textContent = s ? `${s.duracaoMinutos || 30} min` : '-- min';
    document.getElementById('resumo-barbeiro').textContent = b ? b.nome : 'Selecione no passo 2';
    document.getElementById('resumo-data').textContent = d ? formatarDataExtenso(d) : 'Hoje';
    document.getElementById('resumo-horario').textContent = h ? `${h}h` : 'A escolher';
    document.getElementById('resumo-total').textContent = s ? formatadorMoeda.format(s.preco || 0) : 'R$ 0,00';
}

// --- CONFIGURAÇÃO DE TODOS OS EVENTOS DE INTERFACE ---
function configurarEventosUI() {
    // Navegação nos botões do Stepper
    document.getElementById('step-btn-1')?.addEventListener('click', () => irParaPasso(1));
    document.getElementById('step-btn-2')?.addEventListener('click', () => {
        if (appState.selecionado.servico) irParaPasso(2);
        else showToast('Escolha o serviço primeiro.', 'info');
    });
    document.getElementById('step-btn-3')?.addEventListener('click', () => {
        if (appState.selecionado.servico && appState.selecionado.barbeiro) {
            irParaPasso(3);
            carregarSlotsDisponiveis();
        } else {
            showToast('Escolha o serviço e o profissional primeiro.', 'info');
        }
    });
    document.getElementById('step-btn-4')?.addEventListener('click', () => {
        if (appState.selecionado.servico && appState.selecionado.barbeiro && appState.selecionado.horario) {
            irParaPasso(4);
        } else {
            showToast('Escolha o horário para prosseguir.', 'info');
        }
    });

    // Botões Próximo / Anterior
    document.getElementById('btn-next-step-1')?.addEventListener('click', () => irParaPasso(2));
    document.getElementById('btn-prev-step-2')?.addEventListener('click', () => irParaPasso(1));
    document.getElementById('btn-next-step-2')?.addEventListener('click', () => {
        irParaPasso(3);
        carregarSlotsDisponiveis();
    });
    document.getElementById('btn-prev-step-3')?.addEventListener('click', () => irParaPasso(2));
    document.getElementById('btn-next-step-3')?.addEventListener('click', () => irParaPasso(4));
    document.getElementById('btn-prev-step-4')?.addEventListener('click', () => irParaPasso(3));

    // Seletores Rápidos de Data (Hoje / Amanhã / Datepicker)
    const chipHoje = document.getElementById('chip-date-hoje');
    const chipAmanha = document.getElementById('chip-date-amanha');
    const datePicker = document.getElementById('booking-date-picker');

    chipHoje?.addEventListener('click', () => {
        chipHoje.classList.add('active');
        chipAmanha?.classList.remove('active');
        appState.selecionado.dataIso = getHojeIso();
        if (datePicker) datePicker.value = getHojeIso();
        appState.selecionado.horario = '';
        atualizarResumoLateral();
        carregarSlotsDisponiveis();
    });

    chipAmanha?.addEventListener('click', () => {
        chipAmanha.classList.add('active');
        chipHoje?.classList.remove('active');
        appState.selecionado.dataIso = getAmanhaIso();
        if (datePicker) datePicker.value = getAmanhaIso();
        appState.selecionado.horario = '';
        atualizarResumoLateral();
        carregarSlotsDisponiveis();
    });

    datePicker?.addEventListener('change', (e) => {
        if (!e.target.value) return;
        chipHoje?.classList.remove('active');
        chipAmanha?.classList.remove('active');
        appState.selecionado.dataIso = e.target.value;
        appState.selecionado.horario = '';
        atualizarResumoLateral();
        carregarSlotsDisponiveis();
    });

    // Alternar abas de autenticação rápida no Passo 4
    const tabNovo = document.getElementById('tab-auth-novo');
    const tabLogin = document.getElementById('tab-auth-login');
    const formNovo = document.getElementById('form-cadastro-rapido');
    const formLogin = document.getElementById('form-login-rapido');

    tabNovo?.addEventListener('click', () => {
        appState.modoAuth = 'novo';
        tabNovo.classList.add('active');
        tabLogin?.classList.remove('active');
        if (formNovo) formNovo.style.display = 'block';
        if (formLogin) formLogin.style.display = 'none';
    });

    tabLogin?.addEventListener('click', () => {
        appState.modoAuth = 'login';
        tabLogin.classList.add('active');
        tabNovo?.classList.remove('active');
        if (formNovo) formNovo.style.display = 'none';
        if (formLogin) formLogin.style.display = 'block';
    });

    // Botão de troca de usuário logado
    document.getElementById('btn-trocar-usuario')?.addEventListener('click', () => {
        api.logout();
        atualizarEstadoAuth();
        showToast('Você desconectou da conta.', 'info');
    });

    document.getElementById('btn-logout')?.addEventListener('click', () => {
        api.logout();
        atualizarEstadoAuth();
        showToast('Sessão encerrada com sucesso.', 'info');
    });

    // Modais
    document.getElementById('btn-abrir-login')?.addEventListener('click', () => {
        document.getElementById('modal-auth').style.display = 'flex';
    });

    document.getElementById('btn-fechar-modal-auth')?.addEventListener('click', () => {
        document.getElementById('modal-auth').style.display = 'none';
    });

    document.getElementById('btn-concluir-reserva')?.addEventListener('click', () => {
        document.getElementById('modal-sucesso').style.display = 'none';
    });

    document.getElementById('btn-abrir-meus-agendamentos')?.addEventListener('click', (e) => {
        e.preventDefault();
        abrirModalMeusAgendamentos();
    });

    document.getElementById('btn-fechar-modal-meus-agendamentos')?.addEventListener('click', () => {
        document.getElementById('modal-meus-agendamentos').style.display = 'none';
    });

    // Submissão do login isolado na Navbar
    document.getElementById('form-modal-login')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('modal-login-email').value.trim();
        const senha = document.getElementById('modal-login-senha').value.trim();

        try {
            await api.login(email, senha);
            showToast('Login realizado com sucesso!');
            document.getElementById('modal-auth').style.display = 'none';
            atualizarEstadoAuth();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // --- CONFIRMAÇÃO FINAL DO AGENDAMENTO (PASSO 4) ---
    document.getElementById('btn-confirmar-agendamento')?.addEventListener('click', submeterAgendamentoFinal);
}

// Processa o Agendamento com o Backend Spring Boot
async function submeterAgendamentoFinal() {
    const s = appState.selecionado.servico;
    const b = appState.selecionado.barbeiro;
    const dataIso = appState.selecionado.dataIso;
    const horario = appState.selecionado.horario;

    if (!s) { showToast('Por favor, selecione um serviço no Passo 1.', 'error'); return; }
    if (!b) { showToast('Por favor, selecione um barbeiro no Passo 2.', 'error'); return; }
    if (!horario) { showToast('Por favor, escolha um horário no Passo 3.', 'error'); return; }

    const btnSubmit = document.getElementById('btn-confirmar-agendamento');
    const txtBtn = document.getElementById('btn-confirmar-texto');
    const spinner = document.getElementById('btn-confirmar-spinner');

    btnSubmit.disabled = true;
    txtBtn.textContent = 'Processando Agendamento...';
    spinner.style.display = 'inline-block';

    try {
        let user = api.getUsuarioLogado();

        // Se o cliente não estiver logado, fazemos o cadastro ou login transparente
        if (!user) {
            if (appState.modoAuth === 'novo') {
                const nome = document.getElementById('cad-nome')?.value.trim();
                const telefone = document.getElementById('cad-telefone')?.value.trim();
                const email = document.getElementById('cad-email')?.value.trim();
                const senha = document.getElementById('cad-senha')?.value.trim();

                if (!nome || !telefone || !email || !senha) {
                    throw new Error('Preencha todos os campos obrigatórios para o agendamento.');
                }
                if (senha.length < 6) {
                    throw new Error('A senha deve ter pelo menos 6 caracteres.');
                }

                if (appState.backendOnline) {
                    user = await api.registrar(nome, email, senha, telefone);
                } else {
                    user = { id: 999, nome, email, role: 'ROLE_CLIENTE', token: 'mock-token' };
                    api.setSession(user);
                }
                showToast(`Conta criada com sucesso! Bem-vindo, ${user.nome.split(' ')[0]}.`);
            } else {
                const email = document.getElementById('login-quick-email')?.value.trim();
                const senha = document.getElementById('login-quick-senha')?.value.trim();

                if (!email || !senha) {
                    throw new Error('Informe seu e-mail e senha cadastrados.');
                }

                if (appState.backendOnline) {
                    user = await api.login(email, senha);
                } else {
                    user = { id: 999, nome: 'Cliente VIP', email, role: 'ROLE_CLIENTE', token: 'mock-token' };
                    api.setSession(user);
                }
                showToast(`Bem-vindo de volta, ${user.nome.split(' ')[0]}!`);
            }
            atualizarEstadoAuth();
        }

        // Formata dataHora para padrão aceito pelo Spring Boot (ex: "2026-10-29T14:30:00")
        const horaFormatada = horario.length === 5 ? `${horario}:00` : horario;
        const dataHoraIso = `${dataIso}T${horaFormatada}`;

        // Payload do AgendamentoDTO
        const payload = {
            clienteId: user.id,
            barbeiroId: b.id,
            servicoId: s.id,
            dataHora: dataHoraIso
        };

        let agendamentoCriado = null;

        if (appState.backendOnline) {
            agendamentoCriado = await api.criarAgendamento(payload);
        } else {
            // Mock de sucesso quando offline
            agendamentoCriado = {
                id: Math.floor(Math.random() * 9000) + 1000,
                clienteNome: user.nome,
                servicoNome: s.nome,
                barbeiroNome: b.nome,
                dataHora: dataHoraIso,
                preco: s.preco
            };
        }

        // Exibe Ticket de Sucesso
        abrirModalTicket(agendamentoCriado);
        showToast('🎉 Agendamento reservado com sucesso!', 'success');

        // Reset amigável para próximo uso
        appState.selecionado.horario = '';
        renderizarPasso1Servicos();
        renderizarPasso2Barbeiros();
        atualizarResumoLateral();
        irParaPasso(1);

    } catch (err) {
        console.error('Falha no agendamento:', err);
        showToast(err.message, 'error');
    } finally {
        btnSubmit.disabled = false;
        txtBtn.textContent = 'Confirmar Agendamento';
        spinner.style.display = 'none';
    }
}

// Exibe o Ticket de Agendamento Confirmado
function abrirModalTicket(agendamento) {
    document.getElementById('ticket-protocolo').textContent = `#${agendamento.id}`;
    document.getElementById('ticket-cliente').textContent = agendamento.clienteNome || api.getUsuarioLogado()?.nome || 'Cliente';
    document.getElementById('ticket-servico').textContent = agendamento.servicoNome || appState.selecionado.servico?.nome || 'Serviço';
    document.getElementById('ticket-barbeiro').textContent = agendamento.barbeiroNome || appState.selecionado.barbeiro?.nome || 'Barbeiro';
    
    const dataHoraObj = new Date(agendamento.dataHora);
    const dataFormatada = dataHoraObj.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const horaFormatada = dataHoraObj.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    
    document.getElementById('ticket-data-hora').textContent = `${dataFormatada} às ${horaFormatada}h`;
    document.getElementById('ticket-valor').textContent = formatadorMoeda.format(agendamento.preco || appState.selecionado.servico?.preco || 0);

    document.getElementById('modal-sucesso').style.display = 'flex';
}

// Modal Meus Agendamentos
async function abrirModalMeusAgendamentos() {
    const modal = document.getElementById('modal-meus-agendamentos');
    const container = document.getElementById('lista-meus-agendamentos');
    modal.style.display = 'flex';

    if (!appState.backendOnline) {
        container.innerHTML = `
            <div class="appointment-item-card">
                <div class="app-item-info">
                    <h4>Corte Clássico & Barba VIP</h4>
                    <p>Barbeiro: Vitor Tenorio • Data: Amanhã às 15:30h</p>
                </div>
                <span class="barber-status-badge" style="background: rgba(56, 189, 248, 0.15); color: var(--blue-accent);">Agendado</span>
            </div>
        `;
        return;
    }

    try {
        container.innerHTML = '<p class="loading-state"><span class="spinner"></span> Carregando agendamentos...</p>';
        const agendamentos = await api.meusAgendamentos();

        if (!agendamentos || agendamentos.length === 0) {
            container.innerHTML = '<p class="slots-empty-state">Você ainda não possui agendamentos cadastrados.</p>';
            return;
        }

        container.innerHTML = agendamentos.map(a => {
            const dataHoraObj = new Date(a.dataHora);
            const dataFormatada = dataHoraObj.toLocaleDateString('pt-BR');
            const horaFormatada = dataHoraObj.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
            const isCancelado = a.status === 'CANCELADO';

            return `
                <div class="appointment-item-card ${isCancelado ? 'cancelado' : ''}">
                    <div class="app-item-info">
                        <h4>${a.servicoNome || 'Serviço Agendado'}</h4>
                        <p>Barbeiro: ${a.barbeiroNome} • 📅 ${dataFormatada} às ${horaFormatada}h</p>
                    </div>
                    <div>
                        ${isCancelado 
                            ? '<span class="barber-status-badge" style="color: var(--danger); background: var(--danger-bg);">Cancelado</span>' 
                            : `<button type="button" class="btn btn-outline-sm" onclick="cancelarAgendamentoModal(${a.id})" style="border-color: var(--danger); color: var(--danger);">Cancelar</button>`
                        }
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        container.innerHTML = `<p class="slots-empty-state" style="color: var(--danger);">${err.message}</p>`;
    }
}

window.cancelarAgendamentoModal = async function(id) {
    if (!confirm('Deseja realmente cancelar este agendamento?')) return;
    try {
        await api.cancelarAgendamento(id);
        showToast('Agendamento cancelado com sucesso.');
        abrirModalMeusAgendamentos();
    } catch (err) {
        showToast(err.message, 'error');
    }
};
