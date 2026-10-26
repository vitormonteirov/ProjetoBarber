/**
 * Camada de integração com a API Spring Boot (ProjetoBarber)
 * Backend URL: http://localhost:8080/api
 */
const API_BASE_URL = 'http://localhost:8080/api';

const api = {
    // --- CLIENTES ---
    async listarClientes(apenasAtivos = true, pagina = 0, tamanho = 10) {
        const response = await fetch(`${API_BASE_URL}/clientes?apenasAtivos=${apenasAtivos}&page=${pagina}&size=${tamanho}`);
        if (!response.ok) throw new Error(`Erro ao listar clientes: ${response.statusText}`);
        return await response.json();
    },

    async criarCliente(cliente) {
        const response = await fetch(`${API_BASE_URL}/clientes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(cliente)
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            throw new Error(errorData?.message || `Erro ao cadastrar cliente (${response.status})`);
        }
        return await response.json();
    },

    // --- BARBEIROS ---
    async listarBarbeiros(apenasAtivos = true) {
        const response = await fetch(`${API_BASE_URL}/barbeiros?apenasAtivos=${apenasAtivos}`);
        if (!response.ok) throw new Error(`Erro ao listar barbeiros: ${response.statusText}`);
        return await response.json();
    },

    // --- SERVIÇOS ---
    async listarServicos(apenasAtivos = true) {
        const response = await fetch(`${API_BASE_URL}/servicos?apenasAtivos=${apenasAtivos}`);
        if (!response.ok) throw new Error(`Erro ao listar serviços: ${response.statusText}`);
        return await response.json();
    },

    // --- AGENDAMENTOS ---
    async listarAgendamentos() {
        const response = await fetch(`${API_BASE_URL}/agendamentos`);
        if (!response.ok) throw new Error(`Erro ao listar agendamentos: ${response.statusText}`);
        return await response.json();
    },

    async criarAgendamento(agendamento) {
        const response = await fetch(`${API_BASE_URL}/agendamentos`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(agendamento)
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            throw new Error(errorData?.message || `Erro ao criar agendamento (${response.status})`);
        }
        return await response.json();
    }
};
