/**
 * Camada de Integração HTTP com a API Spring Boot (ProjetoBarber)
 * Backend URL: http://localhost:8080/api
 */
const API_BASE_URL = 'http://localhost:8080/api';

const api = {
    // --- GERENCIAMENTO DE SESSÃO / JWT ---
    getToken() {
        return localStorage.getItem('barber_token');
    },

    setSession(authData) {
        if (authData?.token) {
            localStorage.setItem('barber_token', authData.token);
            localStorage.setItem('barber_user', JSON.stringify({
                id: authData.id,
                nome: authData.nome,
                email: authData.email,
                role: authData.role
            }));
        }
    },

    getUsuarioLogado() {
        try {
            const userStr = localStorage.getItem('barber_user');
            return userStr ? JSON.parse(userStr) : null;
        } catch {
            return null;
        }
    },

    logout() {
        localStorage.removeItem('barber_token');
        localStorage.removeItem('barber_user');
    },

    // --- REQUISIÇÃO CENTRALIZADA COM CABEÇALHOS AUTOMÁTICOS ---
    async request(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        const url = `${API_BASE_URL}${endpoint}`;
        const response = await fetch(url, { ...options, headers });

        if (!response.ok) {
            if (response.status === 401 && !endpoint.startsWith('/auth/')) {
                this.logout();
            }
            let errorMsg = `Erro na requisição (${response.status})`;
            try {
                const errJson = await response.json();
                if (errJson.detail) {
                    errorMsg = errJson.detail;
                } else if (errJson.mensagem) {
                    errorMsg = errJson.mensagem;
                } else if (errJson.message) {
                    errorMsg = errJson.message;
                } else if (errJson.errors && typeof errJson.errors === 'object') {
                    errorMsg = Object.values(errJson.errors).join(', ');
                } else if (errJson.erros && Array.isArray(errJson.erros)) {
                    errorMsg = errJson.erros.join(', ');
                }
            } catch (_) {}
            throw new Error(errorMsg);
        }

        if (response.status === 204) return null;
        return await response.json();
    },

    // --- AUTENTICAÇÃO ---
    async login(email, senha) {
        const data = await this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, senha })
        });
        this.setSession(data);
        return data;
    },

    async registrar(nome, email, senha, telefone) {
        const data = await this.request('/auth/registrar', {
            method: 'POST',
            body: JSON.stringify({ nome, email, senha, telefone })
        });
        this.setSession(data);
        return data;
    },

    // --- BARBEIROS (Público) ---
    async listarBarbeiros(apenasAtivos = true, size = 50) {
        const params = new URLSearchParams({
            apenasAtivos: String(Boolean(apenasAtivos)),
            size: String(Math.min(100, Number(size) || 50))
        });
        const data = await this.request(`/barbeiros?${params.toString()}`);
        return data?.content || [];
    },

    async buscarBarbeiroPorId(id) {
        return await this.request(`/barbeiros/${encodeURIComponent(id)}`);
    },

    // --- SERVIÇOS (Público) ---
    async listarServicos(apenasAtivos = true, size = 50) {
        const params = new URLSearchParams({
            apenasAtivos: String(Boolean(apenasAtivos)),
            size: String(Math.min(100, Number(size) || 50))
        });
        const data = await this.request(`/servicos?${params.toString()}`);
        return data?.content || [];
    },

    // --- DISPONIBILIDADE DE HORÁRIOS (Público) ---
    // Retorna array de LocalTime string: ["08:00:00", "08:30:00", ...]
    async consultarDisponibilidade(barbeiroId, servicoId, dataIso) {
        const params = new URLSearchParams({
            barbeiroId: String(barbeiroId),
            data: String(dataIso)
        });
        if (servicoId) {
            params.set('servicoId', String(servicoId));
        }
        return await this.request(`/agendamentos/disponibilidade?${params.toString()}`);
    },

    // --- AGENDAMENTOS (Requer autenticação CLIENTE ou GESTOR) ---
    async criarAgendamento(payload) {
        return await this.request('/agendamentos', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
    },

    async meusAgendamentos(page = 0, size = 10) {
        const data = await this.request(`/agendamentos/meus-agendamentos?page=${page}&size=${size}`);
        return data?.content || [];
    },

    async cancelarAgendamento(id) {
        return await this.request(`/agendamentos/${id}/cancelar`, {
            method: 'PATCH'
        });
    },

    // --- BARBEIRO / GESTOR: CONCLUSÃO E AGENDA ---
    async minhaAgendaBarbeiro(page = 0, size = 30) {
        const data = await this.request(`/barbeiro/minha-agenda?page=${page}&size=${size}`);
        return data?.content || [];
    },

    async listarAgendamentosGestor(status = 'AGENDADO', page = 0, size = 30) {
        const q = status ? `status=${status}&` : '';
        const data = await this.request(`/agendamentos?${q}page=${page}&size=${size}`);
        return data?.content || [];
    },

    async concluirAtendimento(agendamentoId, valorPago, formaPagamento) {
        const payload = { formaPagamento };
        if (valorPago !== null && valorPago !== undefined && valorPago !== '') {
            payload.valorPago = Number(valorPago);
        }
        return await this.request(`/barbeiro/agendamentos/${agendamentoId}/concluir`, {
            method: 'PATCH',
            body: JSON.stringify(payload)
        });
    },

    // --- GESTOR: RELATÓRIO DINÂMICO, CAIXA, HISTÓRICO E COMISSÕES ---
    async obterRelatorioPeriodo(dataInicio, dataFim) {
        const params = new URLSearchParams();
        if (dataInicio) params.set('dataInicio', dataInicio);
        if (dataFim) params.set('dataFim', dataFim);
        return await this.request(`/gestor/relatorio?${params.toString()}`);
    },

    async listarHistoricoAtendimentos({ dataInicio, dataFim, barbeiroId, servicoId, page = 0, size = 50 } = {}) {
        const params = new URLSearchParams({ page: String(page), size: String(size) });
        if (dataInicio) params.set('dataInicio', dataInicio);
        if (dataFim) params.set('dataFim', dataFim);
        if (barbeiroId) params.set('barbeiroId', String(barbeiroId));
        if (servicoId) params.set('servicoId', String(servicoId));
        return await this.request(`/gestor/historico?${params.toString()}`);
    },

    async calcularComissaoBarbeiro(barbeiroId, { dataInicio, dataFim, percentual } = {}) {
        const params = new URLSearchParams();
        if (dataInicio) params.set('dataInicio', dataInicio);
        if (dataFim) params.set('dataFim', dataFim);
        if (percentual !== null && percentual !== undefined && percentual !== '') {
            params.set('percentual', String(percentual));
        }
        return await this.request(`/gestor/comissoes/${barbeiroId}?${params.toString()}`);
    },

    async atualizarPercentualComissaoBarbeiro(barbeiroId, percentualComissao) {
        return await this.request(`/gestor/barbeiros/${encodeURIComponent(barbeiroId)}/comissao`, {
            method: 'PATCH',
            body: JSON.stringify({ percentualComissao: Number(percentualComissao) })
        });
    },

    // --- GESTÃO OPERACIONAL: BARBEIROS, SERVIÇOS, CLIENTES E REAGENDAMENTO ---
    async cadastrarBarbeiroComAcesso({ nome, email, senha, telefone, percentualComissao }) {
        const payload = { nome, email, senha, telefone };
        if (percentualComissao !== null && percentualComissao !== undefined && percentualComissao !== '') {
            payload.percentualComissao = Number(percentualComissao);
        }
        return await this.request('/gestor/barbeiros', {
            method: 'POST',
            body: JSON.stringify(payload)
        });
    },

    async atualizarBarbeiro(id, { nome, email, telefone }) {
        return await this.request(`/barbeiros/${encodeURIComponent(id)}`, {
            method: 'PUT',
            body: JSON.stringify({ nome, email, telefone })
        });
    },

    async desativarBarbeiro(id) {
        return await this.request(`/barbeiros/${encodeURIComponent(id)}`, {
            method: 'DELETE'
        });
    },

    async criarServico({ nome, descricao, preco, duracaoMinutos }) {
        return await this.request('/servicos', {
            method: 'POST',
            body: JSON.stringify({
                nome,
                descricao: descricao || '',
                preco: Number(preco),
                duracaoMinutos: Number(duracaoMinutos)
            })
        });
    },

    async atualizarServico(id, { nome, descricao, preco, duracaoMinutos }) {
        return await this.request(`/servicos/${encodeURIComponent(id)}`, {
            method: 'PUT',
            body: JSON.stringify({
                nome,
                descricao: descricao || '',
                preco: Number(preco),
                duracaoMinutos: Number(duracaoMinutos)
            })
        });
    },

    async desativarServico(id) {
        return await this.request(`/servicos/${encodeURIComponent(id)}`, {
            method: 'DELETE'
        });
    },

    async listarClientesGestor(apenasAtivos = true, size = 100) {
        const params = new URLSearchParams({
            apenasAtivos: String(Boolean(apenasAtivos)),
            size: String(Math.min(100, Number(size) || 100))
        });
        const data = await this.request(`/clientes?${params.toString()}`);
        return data?.content || [];
    },

    async alterarHorarioAgendamento(agendamentoId, novaDataHora, notaAlteracao) {
        return await this.request(`/barbeiro/agendamentos/${encodeURIComponent(agendamentoId)}/alterar-horario`, {
            method: 'PATCH',
            body: JSON.stringify({ novaDataHora, notaAlteracao })
        });
    },

    async cancelarAgendamentoOperacional(agendamentoId, motivo) {
        return await this.request(`/barbeiro/agendamentos/${encodeURIComponent(agendamentoId)}/cancelar`, {
            method: 'PATCH',
            body: JSON.stringify({ motivo })
        });
    },

    // Verificação de saúde da API
    async verificarConexao() {
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 2000);
            const res = await fetch(`${API_BASE_URL}/servicos?size=1`, { signal: controller.signal });
            clearTimeout(timeoutId);
            return res.ok;
        } catch {
            return false;
        }
    }
};
