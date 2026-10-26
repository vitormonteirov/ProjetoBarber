# 💈 Tenorio's Barber - Sistema de Barbearia

Sistema fullstack de alta precisão para a **Tenorio's Barber**, gerenciando agendamentos, clientes, serviços e barbeiros.

## 📁 Estrutura do Repositório (Monorepo)

O projeto segue o padrão de **arquitetura desacoplada** recomendada pelo mercado:

```text
ProjetoBarber/
│
├── backend/                   # API REST desenvolvida em Java 21 com Spring Boot 4
│   ├── src/                   # Controllers, Services, Repositories, DTOs e Migrations Flyway
│   ├── pom.xml                # Gerenciador de dependências Maven
│   └── mvnw / mvnw.cmd        # Maven Wrapper
│
├── frontend/                  # Aplicação cliente (HTML5, CSS3, JavaScript Vanilla com Fetch API)
│   ├── index.html             # Interface do usuário (Dashboard de integração)
│   ├── css/
│   │   └── style.css          # Estilos com tema Dark/Barber
│   └── js/
│       ├── api.js             # Módulo cliente HTTP centralizado (consumo dos endpoints REST)
│       └── app.js             # Lógica de interface e manipulação do DOM
│
└── .gitignore                 # Regras de ignore para Java e Front-end
```

---

## 🚀 Como Executar o Projeto

### 1. Back-end (API Spring Boot)
1. Certifique-se de ter o **Java 21** e o **MySQL** em execução.
2. Navegue até a pasta do backend:
   ```bash
   cd backend
   ```
3. Execute o Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
   *(Ou no Windows via PowerShell: `.\mvnw.cmd spring-boot:run` ou pela sua IDE como IntelliJ).*
4. A API estará disponível em: `http://localhost:8080/api`

### 2. Front-end
1. Você não precisa compilar nada: basta abrir o arquivo `frontend/index.html` em qualquer navegador.
2. **Recomendado:** Abra com a extensão **Live Server** do VS Code (ou qualquer servidor estático local).
3. A página detectará automaticamente se o backend está ligado na porta `8080` e exibirá o indicador verde de conexão!
