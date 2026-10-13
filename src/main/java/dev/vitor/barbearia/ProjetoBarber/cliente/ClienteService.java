package dev.vitor.barbearia.ProjetoBarber.cliente;

import dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public Page<ClienteModel> listarAtivos(Pageable pageable) {
        return clienteRepository.findByAtivoTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ClienteModel> listarTodos(Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<ClienteModel> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    @Transactional
    public ClienteModel criar(ClienteDTO dto) {
        ClienteModel cliente = ClienteModel.builder()
                .nome(dto.nome())
                .telefone(dto.telefone())
                .email(dto.email())
                .ativo(true)
                .build();
        return clienteRepository.save(cliente);
    }

    @Transactional
    public ClienteModel atualizar(Long id, ClienteDTO dto) {
        return clienteRepository.findById(id).map(cliente -> {
            cliente.setNome(dto.nome());
            cliente.setTelefone(dto.telefone());
            cliente.setEmail(dto.email());
            return clienteRepository.save(cliente);
        }).orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
    }

    @Transactional
    public void excluir(Long id) {
        ClienteModel cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado com ID: " + id));
        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }
}
