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
            let errorMsg = `Erro na requisição (${response.status})`;
            try {
                const errJson = await response.json();
                if (errJson.mensagem) {
                    errorMsg = errJson.mensagem;
                } else if (errJson.message) {
                    errorMsg = errJson.message;
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
        const data = await this.request(`/barbeiros?apenasAtivos=${apenasAtivos}&size=${size}`);
        return data?.content || [];
    },

    async buscarBarbeiroPorId(id) {
        return await this.request(`/barbeiros/${id}`);
    },

    // --- SERVIÇOS (Público) ---
    async listarServicos(apenasAtivos = true, size = 50) {
        const data = await this.request(`/servicos?apenasAtivos=${apenasAtivos}&size=${size}`);
        return data?.content || [];
    },

    // --- DISPONIBILIDADE DE HORÁRIOS (Público) ---
    // Retorna array de LocalTime string: ["08:00:00", "08:30:00", ...]
    async consultarDisponibilidade(barbeiroId, servicoId, dataIso) {
        let endpoint = `/agendamentos/disponibilidade?barbeiroId=${barbeiroId}&data=${dataIso}`;
        if (servicoId) {
            endpoint += `&servicoId=${servicoId}`;
        }
        return await this.request(endpoint);
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
