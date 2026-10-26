/**
 * Camada de Integração HTTP com a API Spring Boot (ProjetoBarber)
 * Backend URL: http://localhost:8080/api
 */
const API_BASE_URL = 'http://localhost:8080/api';

const api = {
    // Utilitário para lidar com respostas HTTP e erros do GlobalExceptionHandler
    async handleResponse(response) {
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

    // --- BARBEIROS ---
    async listarBarbeiros(apenasAtivos = true, size = 50) {
        const res = await fetch(`${API_BASE_URL}/barbeiros?apenasAtivos=${apenasAtivos}&size=${size}`);
        const data = await this.handleResponse(res);
        return data?.content || [];
    },

    async buscarBarbeiroPorId(id) {
        const res = await fetch(`${API_BASE_URL}/barbeiros/${id}`);
        return await this.handleResponse(res);
    },

    // --- SERVIÇOS ---
    async listarServicos(apenasAtivos = true, size = 50) {
        const res = await fetch(`${API_BASE_URL}/servicos?apenasAtivos=${apenasAtivos}&size=${size}`);
        const data = await this.handleResponse(res);
        return data?.content || [];
    },

    // --- CLIENTES ---
    async listarClientes(apenasAtivos = true, size = 100) {
        const res = await fetch(`${API_BASE_URL}/clientes?apenasAtivos=${apenasAtivos}&size=${size}`);
        const data = await this.handleResponse(res);
        return data?.content || [];
    },

    async criarCliente(cliente) {
        const res = await fetch(`${API_BASE_URL}/clientes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(cliente)
        });
        return await this.handleResponse(res);
    },

    // --- AGENDAMENTOS ---
    async listarAgendamentos(page = 0, size = 20) {
        const res = await fetch(`${API_BASE_URL}/agendamentos?page=${page}&size=${size}&sort=dataHora,desc`);
        const data = await this.handleResponse(res);
        return data?.content || [];
    },

    async criarAgendamento(agendamento) {
        const res = await fetch(`${API_BASE_URL}/agendamentos`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(agendamento)
        });
        return await this.handleResponse(res);
    },

    async cancelarAgendamento(id) {
        const res = await fetch(`${API_BASE_URL}/agendamentos/${id}/cancelar`, {
            method: 'PATCH'
        });
        return await this.handleResponse(res);
    },

    // Verificação de saúde da API
    async verificarConexao() {
        try {
            const res = await fetch(`${API_BASE_URL}/servicos?size=1`);
            return res.ok;
        } catch {
            return false;
        }
    }
};
