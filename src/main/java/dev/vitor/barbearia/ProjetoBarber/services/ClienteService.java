package dev.vitor.barbearia.ProjetoBarber.services;

import dev.vitor.barbearia.ProjetoBarber.dtos.ClienteDTO;
import dev.vitor.barbearia.ProjetoBarber.models.Cliente;
import dev.vitor.barbearia.ProjetoBarber.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    public Page<Cliente> listarTodos(Pageable pageable) {
        return clienteRepository.findAll(pageable);
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return clienteRepository.findById(id);
    }

    public Cliente criar(ClienteDTO dto) {
        Cliente cliente = new Cliente(null, dto.nome(), dto.telefone(), dto.email());
        return clienteRepository.save(cliente);
    }

    public Cliente atualizar(Long id, ClienteDTO dto) {
        return clienteRepository.findById(id).map(cliente -> {
            cliente.setNome(dto.nome());
            cliente.setTelefone(dto.telefone());
            cliente.setEmail(dto.email());
            return clienteRepository.save(cliente);
        }).orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID: " + id));
    }

    public void excluir(Long id) {
        if(clienteRepository.existsById(id)){
            clienteRepository.deleteById(id);
        } else {
            throw new RuntimeException("Cliente não encontrado com ID: " + id);
        }
    }
}
