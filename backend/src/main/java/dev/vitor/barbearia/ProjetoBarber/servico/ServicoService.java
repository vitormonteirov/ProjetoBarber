package dev.vitor.barbearia.ProjetoBarber.servico;

import dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServicoService {

    private final ServicoRepository servicoRepository;

    @Transactional(readOnly = true)
    public Page<ServicoModel> listarAtivos(Pageable pageable) {
        return servicoRepository.findByAtivoTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<ServicoModel> listarTodos(Pageable pageable) {
        return servicoRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<ServicoModel> buscarPorId(Long id) {
        return servicoRepository.findById(id);
    }

    @Transactional
    public ServicoModel criar(ServicoDTO dto) {
        ServicoModel servico = ServicoModel.builder()
                .nome(dto.nome())
                .descricao(dto.descricao())
                .preco(dto.preco())
                .duracaoMinutos(dto.duracaoMinutos())
                .ativo(true)
                .build();
        return servicoRepository.save(servico);
    }

    @Transactional
    public ServicoModel atualizar(Long id, ServicoDTO dto) {
        return servicoRepository.findById(id).map(servico -> {
            servico.setNome(dto.nome());
            servico.setDescricao(dto.descricao());
            servico.setPreco(dto.preco());
            servico.setDuracaoMinutos(dto.duracaoMinutos());
            return servicoRepository.save(servico);
        }).orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
    }

    @Transactional
    public void excluir(Long id) {
        ServicoModel servico = servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado com ID: " + id));
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }
}
