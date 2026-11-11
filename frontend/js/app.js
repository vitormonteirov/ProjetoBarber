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
        servicos: [],
        barbeiro: null,
        dataIso: '',
        horario: ''
    }
};

function obterServicosSelecionados() {
    if (Array.isArray(appState.selecionado.servicos) && appState.selecionado.servicos.length > 0) {
        return appState.selecionado.servicos;
    }
    if (appState.selecionado.servico) {
        return [appState.selecionado.servico];
    }
    return [];
}

function obterResumoServicosSelecionados() {
    const lista = obterServicosSelecionados();
    if (!lista.length) {
        return { nomes: '', duracaoTotal: 0, precoTotal: 0, ids: [] };
    }
    return {
        nomes: lista.map(s => s.nome).join(' + '),
        duracaoTotal: lista.reduce((acc, s) => acc + (Number(s.duracaoMinutos) || 30), 0),
        precoTotal: lista.reduce((acc, s) => acc + (Number(s.preco) || 0), 0),
        ids: lista.map(s => Number(s.id))
    };
}

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
    { id: 1, nome: "Igor Tenorio", email: "igor@tenoriosbarber.com", telefone: "(11) 98888-1111", ativo: true },
    { id: 2, nome: "Indiano", email: "indiano@tenoriosbarber.com", telefone: "(11) 98888-2222", ativo: true },
    { id: 3, nome: "Ezequiel", email: "ezequiel@tenoriosbarber.com", telefone: "(11) 98888-3333", ativo: true }
];

// --- SANITIZAÇÃO ANTI-XSS (PROTEÇÃO CONTRA INJEÇÃO DE HTML/SCRIPTS) ---
function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

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
        <div style="font-size: 0.9rem; font-weight: 500;">${escapeHtml(mensagem)}</div>
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
    const navGestorDashboard = document.getElementById('nav-gestor-dashboard-item');
    const navBarbeiroAgenda = document.getElementById('nav-barbeiro-agenda-item');
    const secGestor = document.getElementById('gestor-dashboard');
    const gestorOnlyBox = document.getElementById('gestor-only-container');

    const cardLogged = document.getElementById('cliente-autenticado-box');
    const boxDeslogado = document.getElementById('cliente-deslogado-box');

    if (user && user.nome) {
        if (btnLoginNav) btnLoginNav.style.display = 'none';
        if (userBadgeNav) userBadgeNav.style.display = 'flex';

        const isGestor = user.role === 'ROLE_GESTOR';
        const isBarbeiro = user.role === 'ROLE_BARBEIRO';

        if (navMeusAgendamentos) navMeusAgendamentos.style.display = (!isBarbeiro) ? 'block' : 'none';
        if (navGestorDashboard) navGestorDashboard.style.display = isGestor ? 'block' : 'none';
        if (navBarbeiroAgenda) navBarbeiroAgenda.style.display = isBarbeiro ? 'block' : 'none';

        if (secGestor) {
            if (isGestor || isBarbeiro) {
                secGestor.style.display = 'block';
                configurarVisibilidadeAbasPorRole(isGestor);
                carregarPainelGestorOuBarbeiro();
            } else {
                secGestor.style.display = 'none';
            }
        }

        const avatarLetter = user.nome.charAt(0).toUpperCase();
        const navAvatar = document.getElementById('nav-user-avatar');
        const navName = document.getElementById('nav-user-name');
        const navRole = userBadgeNav ? userBadgeNav.querySelector('.user-role') : null;
        if (navAvatar) navAvatar.textContent = avatarLetter;
        if (navName) navName.textContent = user.nome.split(' ')[0];
        if (navRole) {
            navRole.textContent = isGestor ? 'Gestor' : (isBarbeiro ? 'Barbeiro' : 'Cliente VIP');
        }

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
        if (navGestorDashboard) navGestorDashboard.style.display = 'none';
        if (navBarbeiroAgenda) navBarbeiroAgenda.style.display = 'none';
        if (secGestor) secGestor.style.display = 'none';

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
                        <span class="service-badge">⏱ ${Number(s.duracaoMinutos) || 30} min</span>
                        <span class="service-price">${formatadorMoeda.format(s.preco || 0)}</span>
                    </div>
                    <h3 class="service-title">${escapeHtml(s.nome)}</h3>
                    <p class="service-desc">${escapeHtml(s.descricao || 'Atendimento com produtos selecionados e alto rigor estético.')}</p>
                </div>
                <div class="service-footer">
                    <button type="button" class="btn btn-primary w-full" onclick="iniciarAgendamentoComServico(${Number(s.id)})">
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
            const iniciais = escapeHtml((b.nome || '').split(' ').map(n => n[0]).slice(0, 2).join('').toUpperCase());
            const primeiroNome = escapeHtml((b.nome || '').split(' ')[0]);
            return `
                <div class="barber-card">
                    <div>
                        <div class="barber-avatar-box">${iniciais}</div>
                        <h3 class="service-title">${escapeHtml(b.nome)}</h3>
                        <p class="service-desc" style="margin-bottom: 0.8rem;">
                            Especialista em visuais modernos, degradê navalhado e barba alinhada.
                        </p>
                        <span class="barber-status-badge">● Disponível para agendamento</span>
                    </div>
                    <div class="service-footer" style="margin-top: 1.5rem;">
                        <button type="button" class="btn btn-ghost w-full" onclick="iniciarAgendamentoComBarbeiro(${Number(b.id)})">
                            Agendar com ${primeiroNome} →
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    }
}

// --- RENDERIZAÇÃO DO WIZARD PASSO 1 (SERVIÇOS — CARRINHO MULTI-SERVIÇO) ---
function renderizarPasso1Servicos() {
    const container = document.getElementById('wizard-servicos-list');
    if (!container) return;

    const selecionados = obterServicosSelecionados();
    const idsSelecionados = new Set(selecionados.map(s => Number(s.id)));

    container.innerHTML = appState.servicos.map(s => {
        const isSelected = idsSelecionados.has(Number(s.id));
        const checkTag = isSelected ? '<span class="servico-pill" style="float:right; background: var(--blue-accent); color: #050a14; font-weight:700;">✓ Selecionado</span>' : '';
        return `
            <div class="wizard-card-item ${isSelected ? 'selected' : ''}"
                 role="checkbox"
                 aria-checked="${isSelected}"
                 tabindex="0"
                 onclick="selecionarServicoWizard(${Number(s.id)})"
                 onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();selecionarServicoWizard(${Number(s.id)});}">
                <div class="item-name">${escapeHtml(s.nome)} ${checkTag}</div>
                <div class="item-info">⏱ ${Number(s.duracaoMinutos) || 30} minutos de atendimento</div>
                <div class="item-price">${formatadorMoeda.format(s.preco || 0)}</div>
            </div>
        `;
    }).join('');

    const btnNext = document.getElementById('btn-next-step-1');
    if (btnNext) {
        btnNext.disabled = selecionados.length === 0;
        const resumo = obterResumoServicosSelecionados();
        btnNext.textContent = selecionados.length > 0
            ? `Avançar com ${selecionados.length} serviço(s) (${formatadorMoeda.format(resumo.precoTotal)}) →`
            : 'Avançar para o Barbeiro →';
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
            <div class="wizard-card-item ${isSelected ? 'selected' : ''}"
                 role="radio"
                 aria-checked="${isSelected}"
                 tabindex="0"
                 onclick="selecionarBarbeiroWizard(${Number(b.id)})"
                 onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();selecionarBarbeiroWizard(${Number(b.id)});}">
                <div class="item-name">${escapeHtml(b.nome)}</div>
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
    const resumoServicos = obterResumoServicosSelecionados();
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
            slots = await api.consultarDisponibilidade(barbeiroId, resumoServicos.ids, dataIso);
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
        // Formata "08:00:00" ou "08:00" para "08:00" e valida formato HH:mm
        const horaCurta = String(slotTime).substring(0, 5).replace(/[^0-9:]/g, '');
        const isSelected = appState.selecionado.horario === horaCurta;

        return `
            <button type="button" class="slot-btn ${isSelected ? 'selected' : ''}" 
                    onclick="selecionarHorarioSlot('${horaCurta}')">
                ${escapeHtml(horaCurta)}
            </button>
        `;
    }).join('');

    if (btnNext) {
        btnNext.disabled = !appState.selecionado.horario;
    }
}

// Gera slots de demonstração se a API estiver offline (respeitando pausa padrão das 12:00 às 13:00)
function gerarSlotsMock(dataIso) {
    const lista = [];
    const agora = new Date();
    const dataEscolhida = new Date(dataIso + 'T00:00:00');
    const isHoje = dataEscolhida.toDateString() === agora.toDateString();

    for (let h = 8; h <= 18; h++) {
        if (h === 12) continue; // Pausa de almoço padrão: 12:00 às 13:00
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

    if (!Array.isArray(appState.selecionado.servicos)) {
        appState.selecionado.servicos = [];
    }

    const idx = appState.selecionado.servicos.findIndex(s => Number(s.id) === Number(id));
    if (idx >= 0) {
        appState.selecionado.servicos.splice(idx, 1);
    } else {
        appState.selecionado.servicos.push(servico);
    }

    appState.selecionado.servico = appState.selecionado.servicos[0] || null;
    appState.selecionado.horario = '';
    renderizarPasso1Servicos();
    atualizarResumoLateral();
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
        appState.selecionado.servicos = [servico];
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
        if (!obterServicosSelecionados().length) {
            irParaPasso(1);
            showToast(`Barbeiro ${barbeiro.nome.split(' ')[0]} selecionado! Agora escolha o(s) serviço(s).`, 'info');
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
    const resumoServicos = obterResumoServicosSelecionados();
    const b = appState.selecionado.barbeiro;
    const d = appState.selecionado.dataIso;
    const h = appState.selecionado.horario;

    document.getElementById('resumo-servico').textContent = resumoServicos.nomes || 'Selecione no passo 1';
    document.getElementById('resumo-duracao').textContent = resumoServicos.duracaoTotal ? `${resumoServicos.duracaoTotal} min` : '-- min';
    document.getElementById('resumo-barbeiro').textContent = b ? b.nome : 'Selecione no passo 2';
    document.getElementById('resumo-data').textContent = d ? formatarDataExtenso(d) : 'Hoje';
    document.getElementById('resumo-horario').textContent = h ? `${h}h` : 'A escolher';
    document.getElementById('resumo-total').textContent = formatadorMoeda.format(resumoServicos.precoTotal || 0);
}

// --- CONFIGURAÇÃO DE TODOS OS EVENTOS DE INTERFACE ---
function configurarEventosUI() {
    // Navegação nos botões do Stepper
    document.getElementById('step-btn-1')?.addEventListener('click', () => irParaPasso(1));
    document.getElementById('step-btn-2')?.addEventListener('click', () => {
        if (obterServicosSelecionados().length) irParaPasso(2);
        else showToast('Escolha pelo menos um serviço primeiro.', 'info');
    });
    document.getElementById('step-btn-3')?.addEventListener('click', () => {
        if (obterServicosSelecionados().length && appState.selecionado.barbeiro) {
            irParaPasso(3);
            carregarSlotsDisponiveis();
        } else {
            showToast('Escolha o serviço e o profissional primeiro.', 'info');
        }
    });
    document.getElementById('step-btn-4')?.addEventListener('click', () => {
        if (obterServicosSelecionados().length && appState.selecionado.barbeiro && appState.selecionado.horario) {
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

    // Acessibilidade (WCAG 2.2): Fechar modais abertos ao pressionar a tecla Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            ['modal-auth', 'modal-sucesso', 'modal-meus-agendamentos', 'modal-concluir-atendimento', 'modal-reagendar-atendimento'].forEach(id => {
                const modal = document.getElementById(id);
                if (modal && modal.style.display && modal.style.display !== 'none') {
                    modal.style.display = 'none';
                }
            });
        }
    });
}

// Processa o Agendamento com o Backend Spring Boot
async function submeterAgendamentoFinal() {
    const servicosSelecionados = obterServicosSelecionados();
    const resumoServicos = obterResumoServicosSelecionados();
    const b = appState.selecionado.barbeiro;
    const dataIso = appState.selecionado.dataIso;
    const horario = appState.selecionado.horario;

    if (!servicosSelecionados.length) { showToast('Por favor, selecione pelo menos um serviço no Passo 1.', 'error'); return; }
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

        // Payload do AgendamentoDTO com suporte a múltiplos serviços (servicoIds)
        const payload = {
            clienteId: user.id,
            barbeiroId: b.id,
            servicoId: resumoServicos.ids[0],
            servicoIds: resumoServicos.ids,
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
                servicoNome: resumoServicos.nomes,
                barbeiroNome: b.nome,
                dataHora: dataHoraIso,
                preco: resumoServicos.precoTotal
            };
        }

        // Exibe Ticket de Sucesso
        abrirModalTicket(agendamentoCriado);
        showToast('🎉 Agendamento reservado com sucesso!', 'success');

        // Reset amigável para próximo uso
        appState.selecionado.horario = '';
        appState.selecionado.servicos = [];
        appState.selecionado.servico = null;
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
    const resumoServicos = obterResumoServicosSelecionados();
    document.getElementById('ticket-protocolo').textContent = `#${agendamento.id}`;
    document.getElementById('ticket-cliente').textContent = agendamento.clienteNome || api.getUsuarioLogado()?.nome || 'Cliente';
    document.getElementById('ticket-servico').textContent = agendamento.servicoNome || resumoServicos.nomes || 'Serviço';
    document.getElementById('ticket-barbeiro').textContent = agendamento.barbeiroNome || appState.selecionado.barbeiro?.nome || 'Barbeiro';
    
    const dataHoraObj = new Date(agendamento.dataHora);
    const dataFormatada = dataHoraObj.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    const horaFormatada = dataHoraObj.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    
    document.getElementById('ticket-data-hora').textContent = `${dataFormatada} às ${horaFormatada}h`;
    document.getElementById('ticket-valor').textContent = formatadorMoeda.format(agendamento.preco || resumoServicos.precoTotal || 0);

    document.getElementById('modal-sucesso').style.display = 'flex';
}

function renderizarCartaoFidelidadeCliente(fid) {
    const box = document.getElementById('fidelidade-cliente-card');
    if (!box) return;
    if (!fid) {
        box.innerHTML = '';
        return;
    }

    const meta = Number(fid.metaSelos) || 10;
    const selosAtuais = Number(fid.selosAtuais) || 0;
    const totalConcluidos = Number(fid.totalAtendimentosConcluidos) || 0;
    const faltam = Number(fid.faltamParaRecompensa) || (meta - selosAtuais);
    const elegivel = Boolean(fid.elegivelRecompensa);

    let selosHtml = '';
    for (let i = 1; i <= meta; i++) {
        const preenchido = (elegivel && selosAtuais === 0) || i <= selosAtuais;
        selosHtml += `<span style="display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:50%;margin:2px;font-size:0.85rem;font-weight:700;border:1px solid var(--blue-accent);background:${preenchido ? 'var(--blue-accent)' : 'rgba(56,189,248,0.08)'};color:${preenchido ? '#050a14' : 'var(--blue-accent)'};">${preenchido ? '★' : i}</span>`;
    }

    const msgStatus = elegivel
        ? `<strong style="color: var(--success);">🎁 Parabéns! Você completou ${totalConcluidos} atendimentos e conquistou um corte/prêmio fidelidade!</strong>`
        : `Faltam <strong>${faltam}</strong> atendimento(s) concluído(s) para o seu <strong>${meta}º corte premiado</strong>.`;

    box.innerHTML = `
        <div class="appointment-item-card" style="border-color: var(--blue-accent); flex-direction: column; align-items: flex-start; gap: 0.6rem;">
            <div style="display:flex; justify-content:space-between; width:100%; align-items:center; flex-wrap:wrap; gap:0.5rem;">
                <div>
                    <h4 style="margin:0;">⭐ Cartão Fidelidade Digital VIP</h4>
                    <small style="color: var(--text-muted);">Total de visitas concluídas: <strong>${totalConcluidos}</strong></small>
                </div>
                <span class="barber-status-badge">${elegivel ? '🎁 Prêmio Disponível!' : `${selosAtuais}/${meta} selos`}</span>
            </div>
            <div style="display:flex; flex-wrap:wrap; gap:2px; margin: 0.2rem 0;">${selosHtml}</div>
            <div style="font-size: 0.85rem; color: var(--text-secondary);">${msgStatus}</div>
        </div>
    `;
}

// Modal Meus Agendamentos
async function abrirModalMeusAgendamentos() {
    const modal = document.getElementById('modal-meus-agendamentos');
    const container = document.getElementById('lista-meus-agendamentos');
    modal.style.display = 'flex';

    if (!appState.backendOnline) {
        renderizarCartaoFidelidadeCliente({
            totalAtendimentosConcluidos: 4,
            metaSelos: 10,
            selosAtuais: 4,
            faltamParaRecompensa: 6,
            elegivelRecompensa: false
        });
        container.innerHTML = `
            <div class="appointment-item-card">
                <div class="app-item-info">
                    <h4>Corte Clássico & Barba VIP</h4>
                    <p>Barbeiro: Igor Tenorio • Data: Amanhã às 15:30h</p>
                </div>
                <span class="barber-status-badge" style="background: rgba(56, 189, 248, 0.15); color: var(--blue-accent);">Agendado</span>
            </div>
        `;
        return;
    }

    try {
        container.innerHTML = '<p class="loading-state"><span class="spinner"></span> Carregando agendamentos...</p>';
        const [agendamentos, fidelidade] = await Promise.all([
            api.meusAgendamentos(),
            api.obterMinhaFidelidade().catch(() => null)
        ]);
        renderizarCartaoFidelidadeCliente(fidelidade);

        if (!agendamentos || agendamentos.length === 0) {
            container.innerHTML = '<p class="slots-empty-state">Você ainda não possui agendamentos cadastrados.</p>';
            return;
        }

        container.innerHTML = agendamentos.map(a => {
            const dataHoraObj = new Date(a.dataHora);
            const dataFormatada = dataHoraObj.toLocaleDateString('pt-BR');
            const horaFormatada = dataHoraObj.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
            const isCancelado = a.status === 'CANCELADO';
            const isNoShow = a.status === 'NAO_COMPARECEU';
            const isConcluido = a.status === 'CONCLUIDO';

            let acaoOuBadge = '';
            if (isCancelado) {
                acaoOuBadge = '<span class="barber-status-badge" style="color: var(--danger); background: var(--danger-bg);">Cancelado</span>';
            } else if (isNoShow) {
                acaoOuBadge = '<span class="barber-status-badge" style="color: var(--warning);">Não Compareceu (No-Show)</span>';
            } else if (isConcluido) {
                acaoOuBadge = '<span class="barber-status-badge">Concluído</span>';
            } else {
                acaoOuBadge = `<button type="button" class="btn btn-outline-sm" onclick="cancelarAgendamentoModal(${Number(a.id)})" style="border-color: var(--danger); color: var(--danger);">Cancelar</button>`;
            }

            return `
                <div class="appointment-item-card ${(isCancelado || isNoShow) ? 'cancelado' : ''}">
                    <div class="app-item-info">
                        <h4>${escapeHtml(a.servicoNome || 'Serviço Agendado')}</h4>
                        <p>Barbeiro: ${escapeHtml(a.barbeiroNome)} • 📅 ${escapeHtml(dataFormatada)} às ${escapeHtml(horaFormatada)}h</p>
                    </div>
                    <div>${acaoOuBadge}</div>
                </div>
            `;
        }).join('');
    } catch (err) {
        container.innerHTML = `<p class="slots-empty-state" style="color: var(--danger);">${escapeHtml(err.message)}</p>`;
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

// =========================================================================
// PAINEL DINÂMICO DO GESTOR & ABAS DE GESTÃO OPERACIONAL
// =========================================================================
let gestorEventosConfigurados = false;
let filaConclusaoCache = [];
let historicoItensCache = [];
let barbeirosCrudCache = [];
let servicosCrudCache = [];
let clientesCrudCache = [];

function configurarVisibilidadeAbasPorRole(isGestor) {
    const abasExclusivasGestor = ['financeiro', 'balcao', 'barbeiros', 'servicos'];
    abasExclusivasGestor.forEach(tab => {
        const btn = document.querySelector(`.gestor-tab-btn[data-tab="${tab}"]`);
        if (btn) btn.style.display = isGestor ? 'inline-block' : 'none';
    });

    const groupBloqueioBarbeiro = document.getElementById('group-bloqueio-barbeiro');
    if (groupBloqueioBarbeiro) {
        groupBloqueioBarbeiro.style.display = isGestor ? 'block' : 'none';
    }

    const titulo = document.getElementById('painel-titulo');
    const subtitulo = document.getElementById('painel-subtitulo');
    if (isGestor) {
        if (titulo) titulo.innerHTML = 'Central Operacional do <span>Gestor</span>';
        if (subtitulo) subtitulo.textContent = 'Caixa do período, histórico permanente, comissões, gestão de barbeiros, serviços e reservas de balcão.';
        const algumaAtiva = document.querySelector('.gestor-tab-btn.active');
        if (!algumaAtiva || algumaAtiva.style.display === 'none') {
            selecionarAbaOperacional('financeiro');
        }
    } else {
        if (titulo) titulo.innerHTML = 'Minha Agenda do <span>Barbeiro</span>';
        if (subtitulo) subtitulo.textContent = 'Gerencie seus atendimentos agendados: conclua informando o pagamento, envie lembrete WhatsApp, bloqueie folgas ou reagende.';
        selecionarAbaOperacional('agenda');
    }
}

function selecionarAbaOperacional(tabName) {
    document.querySelectorAll('.gestor-tab-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tabName);
    });
    document.querySelectorAll('.gestor-tab-pane').forEach(pane => {
        pane.classList.toggle('active', pane.dataset.pane === tabName);
    });
}

function formatarIsoLocal(dateObj) {
    const y = dateObj.getFullYear();
    const m = String(dateObj.getMonth() + 1).padStart(2, '0');
    const d = String(dateObj.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
}

function aplicarPresetPeriodo(preset) {
    const hoje = new Date();
    const inputIni = document.getElementById('gestor-data-inicio');
    const inputFim = document.getElementById('gestor-data-fim');
    if (!inputIni || !inputFim) return;

    if (preset === 'hoje') {
        const iso = formatarIsoLocal(hoje);
        inputIni.value = iso;
        inputFim.value = iso;
    } else if (preset === '7dias') {
        const d7 = new Date(hoje);
        d7.setDate(hoje.getDate() - 6);
        inputIni.value = formatarIsoLocal(d7);
        inputFim.value = formatarIsoLocal(hoje);
    } else if (preset === 'tudo') {
        inputIni.value = '2020-01-01';
        inputFim.value = formatarIsoLocal(hoje);
    } else {
        const primeiroDiaMes = new Date(hoje.getFullYear(), hoje.getMonth(), 1);
        inputIni.value = formatarIsoLocal(primeiroDiaMes);
        inputFim.value = formatarIsoLocal(hoje);
    }
}

async function carregarPainelGestorOuBarbeiro() {
    configurarEventosGestor();
    const user = api.getUsuarioLogado();
    if (!user || !appState.backendOnline) return;

    const inputIni = document.getElementById('gestor-data-inicio');
    if (inputIni && !inputIni.value) {
        aplicarPresetPeriodo('mes');
    }

    const balcaoData = document.getElementById('balcao-data');
    if (balcaoData && !balcaoData.value) {
        balcaoData.min = getHojeIso();
        balcaoData.value = getHojeIso();
    }

    if (user.role === 'ROLE_GESTOR') {
        await Promise.all([
            atualizarRelatorioEHistoricoGestor(),
            carregarFilaConclusao(),
            carregarBloqueiosAgenda(),
            carregarDadosOperacionaisGestor()
        ]);
    } else if (user.role === 'ROLE_BARBEIRO') {
        await Promise.all([
            carregarFilaConclusao(),
            carregarBloqueiosAgenda()
        ]);
    }
}

async function atualizarRelatorioEHistoricoGestor() {
    const dataInicio = document.getElementById('gestor-data-inicio')?.value;
    const dataFim = document.getElementById('gestor-data-fim')?.value;

    try {
        const [relatorio, todosBarbeiros] = await Promise.all([
            api.obterRelatorioPeriodo(dataInicio, dataFim),
            api.listarBarbeiros(false, 100).catch(() => [])
        ]);

        renderizarKpisRelatorio(relatorio);
        renderizarResumoBarbeirosPeriodo(relatorio.porBarbeiro || []);
        popularSelectsGestor(todosBarbeiros, relatorio);
        await carregarTabelaHistorico();
    } catch (err) {
        showToast(err.message || 'Erro ao carregar relatório do período.', 'error');
    }
}

function renderizarKpisRelatorio(rel) {
    document.getElementById('kpi-caixa-total').textContent = formatadorMoeda.format(rel.caixaTotal || 0);
    document.getElementById('kpi-ticket-medio').textContent = `Ticket médio: ${formatadorMoeda.format(rel.ticketMedio || 0)}`;
    document.getElementById('kpi-atendimentos-realizados').textContent = String(rel.totalAtendimentosRealizados || 0);
    document.getElementById('kpi-pendentes-cancelados').textContent =
        `${rel.agendamentosPendentesNoPeriodo || 0} agendados • ${rel.agendamentosCanceladosNoPeriodo || 0} cancelados • ${rel.agendamentosNaoCompareceuNoPeriodo || 0} faltas`;
    document.getElementById('kpi-comissao-total').textContent = formatadorMoeda.format(rel.comissaoTotal || 0);
    document.getElementById('kpi-lucro-liquido').textContent = formatadorMoeda.format(rel.lucroLiquido || 0);
}

function renderizarResumoBarbeirosPeriodo(lista) {
    const container = document.getElementById('gestor-barbeiros-resumo');
    if (!container) return;

    if (!lista.length) {
        container.innerHTML = '<p class="slots-empty-state">Nenhum atendimento concluído neste período.</p>';
        return;
    }

    container.innerHTML = lista.map(b => {
        const servicosTags = (b.servicos || []).map(s =>
            `<span class="servico-pill">${escapeHtml(s.servicoNome)}: <strong>${Number(s.quantidade)}x</strong> (${formatadorMoeda.format(s.totalPago || 0)})</span>`
        ).join(' ');

        const badgeStatus = b.barbeiroAtivo === false
            ? '<span class="barber-status-badge" style="color: var(--warning);">Histórico (Inativo/Removido)</span>'
            : `<span class="barber-status-badge">Comissão padrão: ${escapeHtml(b.percentualComissaoAtual ?? 50)}%</span>`;

        return `
            <div class="barbeiro-periodo-item">
                <div class="bp-top">
                    <div>
                        <h4>${escapeHtml(b.barbeiroNome)}</h4>
                        ${badgeStatus}
                    </div>
                    <div class="bp-metrics">
                        <span><strong>${Number(b.totalAtendimentos)}</strong> atend.</span>
                        <span>Caixa: <strong>${formatadorMoeda.format(b.totalPago || 0)}</strong></span>
                        <span>Comissão: <strong class="text-warning">${formatadorMoeda.format(b.totalComissao || 0)}</strong></span>
                    </div>
                </div>
                <div class="bp-servicos">${servicosTags || '<small>Sem detalhamento de serviço</small>'}</div>
            </div>
        `;
    }).join('');
}

function popularSelectsGestor(todosBarbeiros, relatorio) {
    const selCalc = document.getElementById('calc-select-barbeiro');
    const selHistBarbeiro = document.getElementById('filtro-historico-barbeiro');
    const selHistServico = document.getElementById('filtro-historico-servico');

    // Combina barbeiros cadastrados + barbeiros presentes apenas no histórico (excluídos)
    const mapaBarbeiros = new Map();
    (todosBarbeiros || []).forEach(b => mapaBarbeiros.set(b.id, {
        id: b.id,
        nome: b.nome,
        percentualComissao: b.percentualComissao ?? 50,
        ativo: b.ativo
    }));
    (relatorio?.porBarbeiro || []).forEach(b => {
        if (!mapaBarbeiros.has(b.barbeiroId)) {
            mapaBarbeiros.set(b.barbeiroId, {
                id: b.barbeiroId,
                nome: `${b.barbeiroNome} (Histórico)`,
                percentualComissao: b.percentualComissaoAtual ?? 50,
                ativo: false
            });
        }
    });

    const listaBarbeiros = Array.from(mapaBarbeiros.values());
    if (selCalc) {
        const valorAtual = selCalc.value;
        selCalc.innerHTML = '<option value="">Selecione um barbeiro...</option>' +
            listaBarbeiros.map(b => `<option value="${Number(b.id)}" data-pct="${escapeHtml(b.percentualComissao)}">${escapeHtml(b.nome)} (${escapeHtml(b.percentualComissao)}%)</option>`).join('');
        if (valorAtual) selCalc.value = valorAtual;
    }

    if (selHistBarbeiro) {
        const valorAtual = selHistBarbeiro.value;
        selHistBarbeiro.innerHTML = '<option value="">Todos os barbeiros</option>' +
            listaBarbeiros.map(b => `<option value="${Number(b.id)}">${escapeHtml(b.nome)}</option>`).join('');
        if (valorAtual) selHistBarbeiro.value = valorAtual;
    }

    if (selHistServico) {
        const valorAtual = selHistServico.value;
        const mapaServicos = new Map();
        (appState.servicos || []).forEach(s => mapaServicos.set(s.id, s.nome));
        (relatorio?.porServico || []).forEach(s => {
            if (s.servicoId && !mapaServicos.has(s.servicoId)) {
                mapaServicos.set(s.servicoId, `${s.servicoNome} (Histórico)`);
            }
        });
        selHistServico.innerHTML = '<option value="">Todos os serviços</option>' +
            Array.from(mapaServicos.entries()).map(([id, nome]) => `<option value="${Number(id)}">${escapeHtml(nome)}</option>`).join('');
        if (valorAtual) selHistServico.value = valorAtual;
    }
}

async function carregarTabelaHistorico() {
    const tbody = document.getElementById('tabela-historico-body');
    if (!tbody) return;

    const dataInicio = document.getElementById('gestor-data-inicio')?.value;
    const dataFim = document.getElementById('gestor-data-fim')?.value;
    const barbeiroId = document.getElementById('filtro-historico-barbeiro')?.value || null;
    const servicoId = document.getElementById('filtro-historico-servico')?.value || null;

    try {
        const page = await api.listarHistoricoAtendimentos({ dataInicio, dataFim, barbeiroId, servicoId, size: 100 });
        const itens = page?.content || [];
        historicoItensCache = itens;

        if (!itens.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="slots-empty-state">Nenhum atendimento registrado neste filtro.</td></tr>';
            return;
        }

        tbody.innerHTML = itens.map(h => {
            const dt = new Date(h.dataHoraAtendimento);
            const dataStr = dt.toLocaleDateString('pt-BR') + ' ' + dt.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
            const clienteTag = h.clienteNaoUsuario
                ? `${escapeHtml(h.clienteNome || 'Cliente Avulso')} <small>(Avulso)</small>`
                : escapeHtml(h.clienteNome || '--');

            return `
                <tr>
                    <td>${escapeHtml(dataStr)}</td>
                    <td>${clienteTag}</td>
                    <td><strong>${escapeHtml(h.barbeiroNome)}</strong></td>
                    <td>${escapeHtml(h.servicoNome)}</td>
                    <td><span class="servico-pill">${escapeHtml(h.formaPagamento)}</span></td>
                    <td><strong>${formatadorMoeda.format(h.valorPago || 0)}</strong></td>
                    <td>${escapeHtml(h.percentualComissao)}%</td>
                    <td class="text-warning"><strong>${formatadorMoeda.format(h.valorComissao || 0)}</strong></td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="slots-empty-state" style="color: var(--danger);">${escapeHtml(err.message)}</td></tr>`;
    }
}

function exportarHistoricoCsv() {
    if (!historicoItensCache || !historicoItensCache.length) {
        showToast('Não há registros no histórico atual para exportar.', 'info');
        return;
    }

    const cabecalho = [
        'ID',
        'Data/Hora Atendimento',
        'Cliente',
        'Tipo Cliente',
        'Barbeiro',
        'Servico',
        'Forma Pagamento',
        'Valor Pago (R$)',
        'Percentual Comissao (%)',
        'Valor Comissao (R$)'
    ];

    const escaparCsv = (val) => `"${String(val ?? '').replace(/"/g, '""')}"`;

    const linhas = historicoItensCache.map(h => [
        h.id,
        h.dataHoraAtendimento,
        h.clienteNome || 'Cliente Avulso',
        h.clienteNaoUsuario ? 'Nao-Usuario' : 'Cadastrado',
        h.barbeiroNome,
        h.servicoNome,
        h.formaPagamento,
        Number(h.valorPago || 0).toFixed(2),
        Number(h.percentualComissao || 0).toFixed(2),
        Number(h.valorComissao || 0).toFixed(2)
    ].map(escaparCsv).join(';'));

    const conteudoCsv = '\uFEFF' + [cabecalho.map(escaparCsv).join(';'), ...linhas].join('\r\n');
    const blob = new Blob([conteudoCsv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const ini = document.getElementById('gestor-data-inicio')?.value || 'inicio';
    const fim = document.getElementById('gestor-data-fim')?.value || 'fim';
    link.href = url;
    link.download = `relatorio-atendimentos-${ini}-a-${fim}.csv`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    showToast('Relatório CSV exportado com sucesso!');
}

async function carregarFilaConclusao() {
    const container = document.getElementById('lista-fila-conclusao');
    const user = api.getUsuarioLogado();
    if (!container || !user || !appState.backendOnline) return;

    try {
        const lista = user.role === 'ROLE_GESTOR'
            ? await api.listarAgendamentosGestor('AGENDADO', 0, 50)
            : await api.minhaAgendaBarbeiro(0, 50);

        const pendentes = (lista || []).filter(a => a.status === 'AGENDADO');
        filaConclusaoCache = pendentes;

        if (!pendentes.length) {
            container.innerHTML = '<p class="slots-empty-state">Não há agendamentos com status AGENDADO no momento.</p>';
            return;
        }

        container.innerHTML = pendentes.map(a => {
            const dt = new Date(a.dataHora);
            const dataStr = dt.toLocaleDateString('pt-BR');
            const horaStr = dt.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
            const precoSugerido = a.preco ?? 0;
            const duracao = a.duracaoTotalMinutos || 30;
            const badgeAvulso = (!a.clienteId && a.nomeNaoUsuario)
                ? '<span class="servico-pill" style="margin-left: 0.4rem;">Não-Usuário</span>'
                : '';

            const faltasCount = Number(a.clienteFaltasCount || 0);
            const badgeNoShow = faltasCount > 0
                ? `<span class="servico-pill" style="margin-left:0.4rem; border-color: var(--warning); color: var(--warning);" title="Este cliente possui ${faltasCount} falta(s) (No-Show) no histórico">⚠️ ${faltasCount} falta(s)</span>`
                : '';

            const totalConcluidos = Number(a.clienteTotalConcluidos || 0);
            const badgeFidelidade = a.clienteElegivelDescontoFidelidade
                ? `<span class="servico-pill" style="margin-left:0.4rem; border-color: var(--success); color: var(--success);">🎁 Prêmio Fidelidade (${totalConcluidos} cortes)</span>`
                : (totalConcluidos > 0
                    ? `<span class="servico-pill" style="margin-left:0.4rem;">⭐ Fidelidade: ${totalConcluidos} corte(s)</span>`
                    : '');

            const btnWhatsApp = a.clienteTelefone
                ? `<button type="button" class="btn btn-outline-sm" style="border-color: var(--success); color: var(--success);" onclick="abrirLembreteWhatsAppPorId(${Number(a.id)})" title="Enviar lembrete de confirmação via WhatsApp">📲 WhatsApp</button>`
                : '';

            return `
                <div class="appointment-item-card">
                    <div class="app-item-info">
                        <h4>#${Number(a.id)} • ${escapeHtml(a.servicoNome || 'Atendimento')} — ${escapeHtml(a.clienteNome || a.nomeNaoUsuario || 'Cliente')} ${badgeAvulso} ${badgeNoShow} ${badgeFidelidade}</h4>
                        <p>Barbeiro: <strong>${escapeHtml(a.barbeiroNome)}</strong> • 📅 ${escapeHtml(dataStr)} às ${escapeHtml(horaStr)}h (${Number(duracao)} min) • Valor ref.: ${formatadorMoeda.format(precoSugerido)}</p>
                    </div>
                    <div class="crud-item-actions">
                        <button type="button" class="btn btn-primary" onclick="abrirModalConcluirPorId(${Number(a.id)})">
                            ✓ Concluir
                        </button>
                        ${btnWhatsApp}
                        <button type="button" class="btn btn-outline-sm" onclick="abrirModalReagendarPorId(${Number(a.id)})">
                            🕒 Alterar Horário
                        </button>
                        <button type="button" class="btn btn-outline-sm" style="border-color: var(--warning); color: var(--warning);" onclick="marcarNaoCompareceuPorId(${Number(a.id)})" title="Registrar que o cliente faltou (No-Show) sem gerar cobrança">
                            🚫 Não Compareceu
                        </button>
                        <button type="button" class="btn btn-outline-sm" style="border-color: var(--danger); color: var(--danger);" onclick="cancelarAgendamentoOperacionalPorId(${Number(a.id)})">
                            ✕ Cancelar
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        container.innerHTML = `<p class="slots-empty-state" style="color: var(--danger);">${escapeHtml(err.message)}</p>`;
    }
}

window.abrirLembreteWhatsAppPorId = function(id) {
    const item = filaConclusaoCache.find(a => Number(a.id) === Number(id));
    if (!item || !item.clienteTelefone) {
        showToast('Cliente não possui telefone cadastrado para WhatsApp.', 'info');
        return;
    }
    const digitos = String(item.clienteTelefone).replace(/\D/g, '');
    if (!digitos) {
        showToast('Telefone inválido para WhatsApp.', 'error');
        return;
    }
    const numeroWa = digitos.startsWith('55') ? digitos : `55${digitos}`;
    const dt = new Date(item.dataHora);
    const dataStr = dt.toLocaleDateString('pt-BR');
    const horaStr = dt.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    const nomeCliente = item.clienteNome || item.nomeNaoUsuario || 'Cliente';
    const msg = `Olá, ${nomeCliente}! Passando para confirmar seu agendamento na Tenório's Barbearia:\n` +
        `✂️ Serviço: ${item.servicoNome || 'Atendimento'}\n` +
        `💈 Barbeiro: ${item.barbeiroNome}\n` +
        `📅 Data: ${dataStr} às ${horaStr}h\n` +
        `Podemos confirmar sua presença?`;

    const url = `https://wa.me/${numeroWa}?text=${encodeURIComponent(msg)}`;
    window.open(url, '_blank', 'noopener,noreferrer');
};

async function carregarBloqueiosAgenda() {
    const container = document.getElementById('lista-bloqueios-agenda');
    if (!container || !appState.backendOnline) return;

    try {
        const bloqueios = await api.listarBloqueiosAgenda();
        if (!bloqueios || !bloqueios.length) {
            container.innerHTML = '<p class="slots-empty-state">Nenhum bloqueio pontual registrado.</p>';
            return;
        }

        container.innerHTML = bloqueios.map(b => {
            const dtIni = new Date(b.dataHoraInicio);
            const dtFim = new Date(b.dataHoraFim);
            const iniStr = dtIni.toLocaleDateString('pt-BR') + ' ' + dtIni.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
            const fimStr = dtFim.toLocaleDateString('pt-BR') + ' ' + dtFim.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

            return `
                <div class="crud-item-card">
                    <div class="crud-item-info">
                        <h4>🔒 ${escapeHtml(b.barbeiroNome)} — <small>${escapeHtml(b.motivo)}</small></h4>
                        <p>📅 De <strong>${escapeHtml(iniStr)}h</strong> até <strong>${escapeHtml(fimStr)}h</strong></p>
                    </div>
                    <div class="crud-item-actions">
                        <button type="button" class="btn btn-outline-sm" style="border-color: var(--danger); color: var(--danger);" onclick="removerBloqueioAgendaPorId(${Number(b.id)})">
                            Remover
                        </button>
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        container.innerHTML = `<p class="slots-empty-state" style="color: var(--danger);">${escapeHtml(err.message)}</p>`;
    }
}

window.removerBloqueioAgendaPorId = async function(id) {
    if (!confirm('Deseja remover este bloqueio de horário e liberar a agenda novamente?')) return;
    try {
        await api.removerBloqueioAgenda(id);
        showToast('Bloqueio removido e horário liberado com sucesso!');
        await carregarBloqueiosAgenda();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.abrirModalConcluirPorId = function(id) {
    const item = filaConclusaoCache.find(a => Number(a.id) === Number(id));
    if (!item) return;
    document.getElementById('concluir-agendamento-id').value = item.id;
    document.getElementById('concluir-valor-pago').value = Number(item.preco || 0).toFixed(2);
    document.getElementById('modal-concluir-resumo').textContent =
        `Agendamento #${item.id} • ${item.servicoNome || 'Atendimento'} (${item.barbeiroNome || ''})`;
    document.getElementById('modal-concluir-atendimento').style.display = 'flex';
};

window.abrirModalReagendarPorId = function(id) {
    const item = filaConclusaoCache.find(a => Number(a.id) === Number(id));
    if (!item) return;
    document.getElementById('reagendar-agendamento-id').value = item.id;
    const dt = new Date(item.dataHora);
    document.getElementById('reagendar-nova-data').value = formatarIsoLocal(dt);
    document.getElementById('reagendar-nova-hora').value = dt.toTimeString().substring(0, 5);
    document.getElementById('reagendar-nota').value = '';
    document.getElementById('modal-reagendar-resumo').textContent =
        `Agendamento #${item.id} • ${item.clienteNome || item.nomeNaoUsuario || 'Cliente'} com ${item.barbeiroNome}`;
    document.getElementById('modal-reagendar-atendimento').style.display = 'flex';
};

window.cancelarAgendamentoOperacionalPorId = async function(id) {
    const motivo = prompt('Informe o motivo do cancelamento (para registro de auditoria):', 'Solicitado pelo cliente / imprevisto na agenda');
    if (!motivo || !motivo.trim()) return;
    try {
        await api.cancelarAgendamentoOperacional(id, motivo.trim());
        showToast('Agendamento cancelado com sucesso!');
        await carregarPainelGestorOuBarbeiro();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.marcarNaoCompareceuPorId = async function(id) {
    const obs = prompt('Registrar ausência (No-Show) do cliente. Observação opcional:', 'Cliente não compareceu no horário agendado');
    if (obs === null) return;
    try {
        await api.marcarNaoCompareceu(id, obs.trim());
        showToast('Ausência (No-Show) registrada com sucesso!', 'info');
        await carregarPainelGestorOuBarbeiro();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

// =========================================================================
// DADOS E CRUD DAS ABAS OPERACIONAIS DO GESTOR (BARBEIROS, SERVIÇOS E BALCÃO)
// =========================================================================
async function carregarDadosOperacionaisGestor() {
    try {
        const [barbeiros, servicos, clientes] = await Promise.all([
            api.listarBarbeiros(false, 100).catch(() => []),
            api.listarServicos(false, 100).catch(() => []),
            api.listarClientesGestor(true, 100).catch(() => [])
        ]);

        barbeirosCrudCache = barbeiros || [];
        servicosCrudCache = servicos || [];
        clientesCrudCache = clientes || [];

        renderizarListaBarbeirosCrud();
        renderizarListaServicosCrud();
        popularSelectsReservaBalcao();
        await carregarHorariosLivresBalcao();
    } catch (err) {
        console.warn('Falha ao carregar dados operacionais do gestor:', err);
    }
}

function formatarHoraCurta(horaStr, fallback = '') {
    if (!horaStr) return fallback;
    return String(horaStr).substring(0, 5);
}

function renderizarListaBarbeirosCrud() {
    const container = document.getElementById('lista-gestor-barbeiros-crud');
    if (!container) return;

    if (!barbeirosCrudCache.length) {
        container.innerHTML = '<p class="slots-empty-state">Nenhum barbeiro cadastrado.</p>';
        return;
    }

    container.innerHTML = barbeirosCrudCache.map(b => {
        const ativo = b.ativo !== false;
        const pct = b.percentualComissao ?? 50;
        const expIni = formatarHoraCurta(b.horarioInicioExpediente, '08:00');
        const expFim = formatarHoraCurta(b.horarioFimExpediente, '19:00');
        const pausaIni = formatarHoraCurta(b.horarioInicioPausa, '12:00');
        const pausaFim = formatarHoraCurta(b.horarioFimPausa, '13:00');
        const textoPausa = (b.horarioInicioPausa === null && b.horarioFimPausa === null)
            ? ' • Sem pausa fixa'
            : ` • ☕ Pausa: ${escapeHtml(pausaIni)} às ${escapeHtml(pausaFim)}`;

        return `
            <div class="crud-item-card ${ativo ? '' : 'inativo'}">
                <div class="crud-item-info">
                    <h4>${escapeHtml(b.nome)} ${ativo ? '<span class="barber-status-badge">Ativo</span>' : '<span class="barber-status-badge" style="color: var(--warning);">Inativo</span>'}</h4>
                    <p>📧 ${escapeHtml(b.email)} • 📱 ${escapeHtml(b.telefone)} • Comissão: <strong>${escapeHtml(pct)}%</strong></p>
                    <p><small>🕒 Expediente: <strong>${escapeHtml(expIni)} às ${escapeHtml(expFim)}</strong>${textoPausa}</small></p>
                </div>
                <div class="crud-item-actions">
                    <button type="button" class="btn btn-outline-sm" onclick="prepararEdicaoBarbeiro(${Number(b.id)})">Editar</button>
                    ${ativo
                        ? `<button type="button" class="btn btn-outline-sm" style="border-color: var(--danger); color: var(--danger);" onclick="desativarBarbeiroCrud(${Number(b.id)})">Desativar</button>`
                        : ''
                    }
                </div>
            </div>
        `;
    }).join('');
}

window.prepararEdicaoBarbeiro = function(id) {
    const b = barbeirosCrudCache.find(item => Number(item.id) === Number(id));
    if (!b) return;
    document.getElementById('gestor-barbeiro-id').value = b.id;
    document.getElementById('gestor-barbeiro-nome').value = b.nome || '';
    document.getElementById('gestor-barbeiro-email').value = b.email || '';
    document.getElementById('gestor-barbeiro-telefone').value = b.telefone || '';
    document.getElementById('gestor-barbeiro-comissao').value = b.percentualComissao ?? 50;
    document.getElementById('gestor-barbeiro-exp-inicio').value = formatarHoraCurta(b.horarioInicioExpediente, '08:00');
    document.getElementById('gestor-barbeiro-exp-fim').value = formatarHoraCurta(b.horarioFimExpediente, '19:00');
    document.getElementById('gestor-barbeiro-pausa-inicio').value = formatarHoraCurta(b.horarioInicioPausa, '12:00');
    document.getElementById('gestor-barbeiro-pausa-fim').value = formatarHoraCurta(b.horarioFimPausa, '13:00');
    document.getElementById('gestor-barbeiro-senha').value = '';
    document.getElementById('group-barbeiro-senha').style.display = 'none';
    document.getElementById('titulo-form-barbeiro').textContent = `Editar Barbeiro #${b.id}`;
    document.getElementById('btn-cancelar-edicao-barbeiro').style.display = 'inline-flex';
};

function resetarFormBarbeiro() {
    document.getElementById('form-gestor-barbeiro')?.reset();
    document.getElementById('gestor-barbeiro-id').value = '';
    document.getElementById('gestor-barbeiro-comissao').value = '50';
    document.getElementById('gestor-barbeiro-exp-inicio').value = '08:00';
    document.getElementById('gestor-barbeiro-exp-fim').value = '19:00';
    document.getElementById('gestor-barbeiro-pausa-inicio').value = '12:00';
    document.getElementById('gestor-barbeiro-pausa-fim').value = '13:00';
    document.getElementById('group-barbeiro-senha').style.display = 'block';
    document.getElementById('titulo-form-barbeiro').textContent = 'Cadastrar Novo Barbeiro';
    document.getElementById('btn-cancelar-edicao-barbeiro').style.display = 'none';
}

window.desativarBarbeiroCrud = async function(id) {
    if (!confirm('Deseja desativar este barbeiro? O histórico de atendimentos dele permanecerá intacto.')) return;
    try {
        await api.desativarBarbeiro(id);
        showToast('Barbeiro desativado com sucesso.');
        await carregarCatalogo();
        await carregarPainelGestorOuBarbeiro();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

function renderizarListaServicosCrud() {
    const container = document.getElementById('lista-gestor-servicos-crud');
    if (!container) return;

    if (!servicosCrudCache.length) {
        container.innerHTML = '<p class="slots-empty-state">Nenhum serviço cadastrado.</p>';
        return;
    }

    container.innerHTML = servicosCrudCache.map(s => {
        const ativo = s.ativo !== false;
        return `
            <div class="crud-item-card ${ativo ? '' : 'inativo'}">
                <div class="crud-item-info">
                    <h4>${escapeHtml(s.nome)} — <strong>${formatadorMoeda.format(s.preco || 0)}</strong> (${Number(s.duracaoMinutos) || 30} min)</h4>
                    <p>${escapeHtml(s.descricao || 'Sem descrição detalhada')}</p>
                </div>
                <div class="crud-item-actions">
                    <button type="button" class="btn btn-outline-sm" onclick="prepararEdicaoServico(${Number(s.id)})">Editar</button>
                    ${ativo
                        ? `<button type="button" class="btn btn-outline-sm" style="border-color: var(--danger); color: var(--danger);" onclick="desativarServicoCrud(${Number(s.id)})">Desativar</button>`
                        : ''
                    }
                </div>
            </div>
        `;
    }).join('');
}

window.prepararEdicaoServico = function(id) {
    const s = servicosCrudCache.find(item => Number(item.id) === Number(id));
    if (!s) return;
    document.getElementById('gestor-servico-id').value = s.id;
    document.getElementById('gestor-servico-nome').value = s.nome || '';
    document.getElementById('gestor-servico-preco').value = s.preco ?? '';
    document.getElementById('gestor-servico-duracao').value = s.duracaoMinutos ?? 30;
    document.getElementById('gestor-servico-descricao').value = s.descricao || '';
    document.getElementById('titulo-form-servico').textContent = `Editar Serviço #${s.id}`;
    document.getElementById('btn-cancelar-edicao-servico').style.display = 'inline-flex';
};

function resetarFormServico() {
    document.getElementById('form-gestor-servico')?.reset();
    document.getElementById('gestor-servico-id').value = '';
    document.getElementById('gestor-servico-duracao').value = '30';
    document.getElementById('titulo-form-servico').textContent = 'Incluir Novo Serviço';
    document.getElementById('btn-cancelar-edicao-servico').style.display = 'none';
}

window.desativarServicoCrud = async function(id) {
    if (!confirm('Deseja desativar este serviço? O histórico financeiro permanecerá intacto.')) return;
    try {
        await api.desativarServico(id);
        showToast('Serviço desativado com sucesso.');
        await carregarCatalogo();
        await carregarPainelGestorOuBarbeiro();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

function popularSelectsReservaBalcao() {
    const selCliente = document.getElementById('balcao-select-cliente');
    const selBarbeiro = document.getElementById('balcao-select-barbeiro');
    const selServico = document.getElementById('balcao-select-servico');
    const selBloqueioBarbeiro = document.getElementById('bloqueio-select-barbeiro');

    if (selCliente) {
        const val = selCliente.value;
        selCliente.innerHTML = '<option value="">Selecione um cliente cadastrado...</option>' +
            clientesCrudCache.map(c => `<option value="${Number(c.id)}">${escapeHtml(c.nome)} (${escapeHtml(c.email)})</option>`).join('');
        if (val) selCliente.value = val;
    }

    if (selBarbeiro) {
        const val = selBarbeiro.value;
        const ativos = barbeirosCrudCache.filter(b => b.ativo !== false);
        selBarbeiro.innerHTML = '<option value="">Selecione o barbeiro...</option>' +
            ativos.map(b => `<option value="${Number(b.id)}">${escapeHtml(b.nome)}</option>`).join('');
        if (val) selBarbeiro.value = val;
    }

    if (selBloqueioBarbeiro) {
        const val = selBloqueioBarbeiro.value;
        const ativos = barbeirosCrudCache.filter(b => b.ativo !== false);
        selBloqueioBarbeiro.innerHTML = '<option value="">Selecione o barbeiro...</option>' +
            ativos.map(b => `<option value="${Number(b.id)}">${escapeHtml(b.nome)}</option>`).join('');
        if (val) selBloqueioBarbeiro.value = val;
    }

    if (selServico) {
        const val = selServico.value;
        const ativos = servicosCrudCache.filter(s => s.ativo !== false);
        selServico.innerHTML = '<option value="">Selecione o serviço...</option>' +
            ativos.map(s => `<option value="${Number(s.id)}">${escapeHtml(s.nome)} (${formatadorMoeda.format(s.preco || 0)} • ${Number(s.duracaoMinutos) || 30} min)</option>`).join('');
        if (val) selServico.value = val;
    }
}

async function carregarHorariosLivresBalcao() {
    const selHora = document.getElementById('balcao-hora');
    const barbeiroId = document.getElementById('balcao-select-barbeiro')?.value;
    const servicoId = document.getElementById('balcao-select-servico')?.value;
    const dataIso = document.getElementById('balcao-data')?.value;

    if (!selHora) return;
    if (!barbeiroId || !dataIso) {
        selHora.innerHTML = '<option value="">Selecione o barbeiro e a data...</option>';
        return;
    }

    try {
        const slots = await api.consultarDisponibilidade(barbeiroId, servicoId || null, dataIso);
        if (!slots || !slots.length) {
            selHora.innerHTML = '<option value="">Sem horários livres nesta data</option>';
            return;
        }
        selHora.innerHTML = slots.map(st => {
            const horaCurta = String(st).substring(0, 5).replace(/[^0-9:]/g, '');
            return `<option value="${escapeHtml(horaCurta)}">${escapeHtml(horaCurta)}h</option>`;
        }).join('');
    } catch {
        selHora.innerHTML = '<option value="">Erro ao carregar horários</option>';
    }
}

function configurarEventosGestor() {
    if (gestorEventosConfigurados) return;
    gestorEventosConfigurados = true;

    // Alternância das abas operacionais
    document.querySelectorAll('.gestor-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => selecionarAbaOperacional(btn.dataset.tab));
    });

    document.getElementById('btn-aplicar-periodo')?.addEventListener('click', () => {
        document.querySelectorAll('.period-Quick-presets .chip-date').forEach(b => b.classList.remove('active'));
        atualizarRelatorioEHistoricoGestor();
    });

    document.getElementById('btn-exportar-csv')?.addEventListener('click', exportarHistoricoCsv);

    document.querySelectorAll('.period-Quick-presets .chip-date').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.period-Quick-presets .chip-date').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            aplicarPresetPeriodo(btn.dataset.preset);
            atualizarRelatorioEHistoricoGestor();
        });
    });

    document.getElementById('filtro-historico-barbeiro')?.addEventListener('change', carregarTabelaHistorico);
    document.getElementById('filtro-historico-servico')?.addEventListener('change', carregarTabelaHistorico);
    document.getElementById('btn-recarregar-fila')?.addEventListener('click', () => {
        carregarFilaConclusao();
        carregarBloqueiosAgenda();
    });

    // Formulário de Bloqueio Pontual de Agenda / Folgas (Barbeiro e Gestor)
    document.getElementById('form-bloqueio-agenda')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const user = api.getUsuarioLogado();
        const isGestor = user?.role === 'ROLE_GESTOR';
        const barbeiroId = isGestor ? document.getElementById('bloqueio-select-barbeiro')?.value : null;
        const iniRaw = document.getElementById('bloqueio-data-inicio')?.value;
        const fimRaw = document.getElementById('bloqueio-data-fim')?.value;
        const motivo = document.getElementById('bloqueio-motivo')?.value?.trim();

        if (isGestor && !barbeiroId) {
            showToast('Selecione o barbeiro para registrar o bloqueio na agenda.', 'error');
            return;
        }
        if (!iniRaw || !fimRaw || !motivo) {
            showToast('Informe início, fim e o motivo do bloqueio.', 'error');
            return;
        }

        const dataHoraInicio = iniRaw.length === 16 ? `${iniRaw}:00` : iniRaw;
        const dataHoraFim = fimRaw.length === 16 ? `${fimRaw}:00` : fimRaw;

        try {
            await api.criarBloqueioAgenda({ barbeiroId, dataHoraInicio, dataHoraFim, motivo });
            showToast('Bloqueio de agenda registrado com sucesso!', 'success');
            document.getElementById('form-bloqueio-agenda')?.reset();
            await carregarBloqueiosAgenda();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    document.getElementById('calc-select-barbeiro')?.addEventListener('change', (e) => {
        const opt = e.target.selectedOptions[0];
        const pct = opt?.dataset?.pct;
        if (pct !== undefined) {
            document.getElementById('calc-input-percentual').value = pct;
        }
    });

    document.getElementById('btn-simular-comissao')?.addEventListener('click', async () => {
        const barbeiroId = document.getElementById('calc-select-barbeiro')?.value;
        const percentual = document.getElementById('calc-input-percentual')?.value;
        const dataInicio = document.getElementById('gestor-data-inicio')?.value;
        const dataFim = document.getElementById('gestor-data-fim')?.value;

        if (!barbeiroId) {
            showToast('Selecione um barbeiro para calcular a comissão.', 'error');
            return;
        }

        try {
            const res = await api.calcularComissaoBarbeiro(barbeiroId, { dataInicio, dataFim, percentual });
            const box = document.getElementById('calc-resultado-box');
            box.style.display = 'block';
            box.innerHTML = `
                <div class="calc-res-grid">
                    <div><span>Profissional:</span> <strong>${escapeHtml(res.barbeiroNome)}</strong></div>
                    <div><span>Atendimentos no período:</span> <strong>${Number(res.totalAtendimentos)}</strong></div>
                    <div><span>Total faturado pelo barbeiro:</span> <strong>${formatadorMoeda.format(res.totalFaturado || 0)}</strong></div>
                    <div><span>Porcentagem aplicada:</span> <strong>${escapeHtml(res.percentualAplicado)}%</strong> (cadastro atual: ${escapeHtml(res.percentualCadastradoAtual)}%)</div>
                    <div><span>Comissão calculada (${escapeHtml(res.percentualAplicado)}%):</span> <strong class="text-warning">${formatadorMoeda.format(res.comissaoCalculada || 0)}</strong></div>
                    <div><span>Comissão gravada no histórico:</span> <strong>${formatadorMoeda.format(res.comissaoRegistradaNoHistorico || 0)}</strong></div>
                    <div><span>Saldo líquido da barbearia:</span> <strong class="text-success">${formatadorMoeda.format(res.liquidoBarbearia || 0)}</strong></div>
                </div>
            `;
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    document.getElementById('btn-salvar-comissao-fixa')?.addEventListener('click', async () => {
        const barbeiroId = document.getElementById('calc-select-barbeiro')?.value;
        const percentual = document.getElementById('calc-input-percentual')?.value;
        if (!barbeiroId || percentual === '') {
            showToast('Selecione o barbeiro e informe a porcentagem.', 'error');
            return;
        }
        try {
            await api.atualizarPercentualComissaoBarbeiro(barbeiroId, percentual);
            showToast(`Percentual fixo do barbeiro atualizado para ${percentual}%!`, 'success');
            await atualizarRelatorioEHistoricoGestor();
            await carregarDadosOperacionaisGestor();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Modal Concluir Atendimento
    document.getElementById('btn-fechar-modal-concluir')?.addEventListener('click', () => {
        document.getElementById('modal-concluir-atendimento').style.display = 'none';
    });

    document.getElementById('form-concluir-atendimento')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('concluir-agendamento-id').value;
        const valorPago = document.getElementById('concluir-valor-pago').value;
        const formaPagamento = document.getElementById('concluir-forma-pagamento').value;

        try {
            await api.concluirAtendimento(id, valorPago, formaPagamento);
            document.getElementById('modal-concluir-atendimento').style.display = 'none';
            showToast('Atendimento concluído e registrado no caixa e no histórico!', 'success');
            await carregarPainelGestorOuBarbeiro();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Modal Reagendar Atendimento
    document.getElementById('btn-fechar-modal-reagendar')?.addEventListener('click', () => {
        document.getElementById('modal-reagendar-atendimento').style.display = 'none';
    });

    document.getElementById('form-reagendar-atendimento')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = document.getElementById('reagendar-agendamento-id').value;
        const novaData = document.getElementById('reagendar-nova-data').value;
        const novaHora = document.getElementById('reagendar-nova-hora').value;
        const nota = document.getElementById('reagendar-nota').value.trim();

        if (!novaData || !novaHora || !nota) {
            showToast('Preencha a nova data, horário e a nota de alteração.', 'error');
            return;
        }

        const horaFormatada = novaHora.length === 5 ? `${novaHora}:00` : novaHora;
        const novaDataHoraIso = `${novaData}T${horaFormatada}`;

        try {
            await api.alterarHorarioAgendamento(id, novaDataHoraIso, nota);
            document.getElementById('modal-reagendar-atendimento').style.display = 'none';
            showToast('Horário do agendamento atualizado com sucesso!', 'success');
            await carregarPainelGestorOuBarbeiro();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Aba Reserva Rápida de Balcão (Não-Usuário vs Cliente Cadastrado)
    document.querySelectorAll('input[name="balcao-tipo-cliente"]').forEach(radio => {
        radio.addEventListener('change', (e) => {
            const isNaoUsuario = e.target.value === 'nao-usuario';
            document.getElementById('box-balcao-nao-usuario').style.display = isNaoUsuario ? 'grid' : 'none';
            document.getElementById('box-balcao-cadastrado').style.display = isNaoUsuario ? 'none' : 'grid';
        });
    });

    document.getElementById('balcao-select-barbeiro')?.addEventListener('change', carregarHorariosLivresBalcao);
    document.getElementById('balcao-select-servico')?.addEventListener('change', carregarHorariosLivresBalcao);
    document.getElementById('balcao-data')?.addEventListener('change', carregarHorariosLivresBalcao);

    document.getElementById('form-gestor-agendamento-balcao')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const tipo = document.querySelector('input[name="balcao-tipo-cliente"]:checked')?.value || 'nao-usuario';
        const barbeiroId = Number(document.getElementById('balcao-select-barbeiro')?.value);
        const servicoId = Number(document.getElementById('balcao-select-servico')?.value);
        const dataIso = document.getElementById('balcao-data')?.value;
        const hora = document.getElementById('balcao-hora')?.value;

        if (!barbeiroId || !servicoId || !dataIso || !hora) {
            showToast('Selecione barbeiro, serviço, data e um horário disponível.', 'error');
            return;
        }

        const horaFormatada = hora.length === 5 ? `${hora}:00` : hora;
        const payload = {
            barbeiroId,
            servicoId,
            dataHora: `${dataIso}T${horaFormatada}`
        };

        if (tipo === 'cadastrado') {
            const clienteId = Number(document.getElementById('balcao-select-cliente')?.value);
            if (!clienteId) {
                showToast('Selecione o cliente cadastrado.', 'error');
                return;
            }
            payload.clienteId = clienteId;
        } else {
            const nomeAvulso = document.getElementById('balcao-nome-avulso')?.value.trim();
            const telAvulso = document.getElementById('balcao-telefone-avulso')?.value.trim();
            if (!nomeAvulso) {
                showToast('Informe o nome do cliente (não-usuário) para reservar o horário.', 'error');
                return;
            }
            payload.nomeNaoUsuario = nomeAvulso;
            payload.telefoneNaoUsuario = telAvulso || null;
        }

        try {
            const criado = await api.criarAgendamento(payload);
            showToast(`Reserva #${criado.id} registrada com sucesso!`, 'success');
            document.getElementById('balcao-nome-avulso').value = '';
            document.getElementById('balcao-telefone-avulso').value = '';
            await carregarPainelGestorOuBarbeiro();
            selecionarAbaOperacional('agenda');
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Aba Equipe de Barbeiros (Criar / Atualizar)
    document.getElementById('btn-cancelar-edicao-barbeiro')?.addEventListener('click', resetarFormBarbeiro);

    document.getElementById('form-gestor-barbeiro')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const idEdicao = document.getElementById('gestor-barbeiro-id').value;
        const nome = document.getElementById('gestor-barbeiro-nome').value.trim();
        const email = document.getElementById('gestor-barbeiro-email').value.trim();
        const telefone = document.getElementById('gestor-barbeiro-telefone').value.replace(/\D/g, '');
        const senha = document.getElementById('gestor-barbeiro-senha').value.trim();
        const percentualComissao = document.getElementById('gestor-barbeiro-comissao').value;
        const horarioInicioExpediente = document.getElementById('gestor-barbeiro-exp-inicio')?.value || '08:00';
        const horarioFimExpediente = document.getElementById('gestor-barbeiro-exp-fim')?.value || '19:00';
        const horarioInicioPausa = document.getElementById('gestor-barbeiro-pausa-inicio')?.value || null;
        const horarioFimPausa = document.getElementById('gestor-barbeiro-pausa-fim')?.value || null;

        try {
            if (idEdicao) {
                await api.atualizarBarbeiro(idEdicao, {
                    nome,
                    email,
                    telefone,
                    horarioInicioExpediente,
                    horarioFimExpediente,
                    horarioInicioPausa,
                    horarioFimPausa
                });
                if (percentualComissao !== '') {
                    await api.atualizarPercentualComissaoBarbeiro(idEdicao, percentualComissao);
                }
                showToast('Dados e expediente do barbeiro atualizados com sucesso!');
            } else {
                if (!senha || senha.length < 6) {
                    showToast('Informe uma senha inicial com pelo menos 6 caracteres para o barbeiro.', 'error');
                    return;
                }
                await api.cadastrarBarbeiroComAcesso({
                    nome,
                    email,
                    senha,
                    telefone,
                    percentualComissao,
                    horarioInicioExpediente,
                    horarioFimExpediente,
                    horarioInicioPausa,
                    horarioFimPausa
                });
                showToast('Novo barbeiro cadastrado com conta de acesso ROLE_BARBEIRO!');
            }
            resetarFormBarbeiro();
            await carregarCatalogo();
            await carregarPainelGestorOuBarbeiro();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Aba Catálogo de Serviços (Criar / Atualizar)
    document.getElementById('btn-cancelar-edicao-servico')?.addEventListener('click', resetarFormServico);

    document.getElementById('form-gestor-servico')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const idEdicao = document.getElementById('gestor-servico-id').value;
        const nome = document.getElementById('gestor-servico-nome').value.trim();
        const preco = document.getElementById('gestor-servico-preco').value;
        const duracaoMinutos = document.getElementById('gestor-servico-duracao').value;
        const descricao = document.getElementById('gestor-servico-descricao').value.trim();

        try {
            if (idEdicao) {
                await api.atualizarServico(idEdicao, { nome, descricao, preco, duracaoMinutos });
                showToast('Serviço atualizado com sucesso!');
            } else {
                await api.criarServico({ nome, descricao, preco, duracaoMinutos });
                showToast('Novo serviço incluído no catálogo!');
            }
            resetarFormServico();
            await carregarCatalogo();
            await carregarPainelGestorOuBarbeiro();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });
}


