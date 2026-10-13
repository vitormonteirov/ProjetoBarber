package dev.vitor.barbearia.ProjetoBarber.servico;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServicoRepository extends JpaRepository<ServicoModel, Long> {
    Page<ServicoModel> findByAtivoTrue(Pageable pageable);
    Optional<ServicoModel> findByIdAndAtivoTrue(Long id);
}
