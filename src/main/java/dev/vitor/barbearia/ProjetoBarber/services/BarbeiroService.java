package dev.vitor.barbearia.ProjetoBarber.services;

import dev.vitor.barbearia.ProjetoBarber.dtos.BarbeiroDTO;
import dev.vitor.barbearia.ProjetoBarber.models.Barbeiro;
import dev.vitor.barbearia.ProjetoBarber.repositories.BarbeiroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BarbeiroService {

    @Autowired
    private BarbeiroRepository barbeiroRepository;

    public Page<Barbeiro> listarTodos(Pageable pageable) {
        return barbeiroRepository.findAll(pageable);
    }

    public Optional<Barbeiro> buscarPorId(Long id) {
        return barbeiroRepository.findById(id);
    }

    public Barbeiro criar(BarbeiroDTO dto) {
        Barbeiro barbeiro = new Barbeiro(null, dto.nome(), dto.email(), dto.telefone());
        return barbeiroRepository.save(barbeiro);
    }

    public Barbeiro atualizar(Long id, BarbeiroDTO dto) {
        return barbeiroRepository.findById(id).map(barbeiro -> {
            barbeiro.setNome(dto.nome());
            barbeiro.setEmail(dto.email());
            barbeiro.setTelefone(dto.telefone());
            return barbeiroRepository.save(barbeiro);
        }).orElseThrow(() -> new RuntimeException("Barbeiro não encontrado com ID: " + id));
    }

    public void excluir(Long id) {
        if(barbeiroRepository.existsById(id)){
            barbeiroRepository.deleteById(id);
        } else {
            throw new RuntimeException("Barbeiro não encontrado com ID: " + id);
        }
    }
}
