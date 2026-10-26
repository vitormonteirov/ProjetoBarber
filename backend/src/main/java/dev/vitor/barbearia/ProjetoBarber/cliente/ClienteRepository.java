package dev.vitor.barbearia.ProjetoBarber.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<ClienteModel, Long> {
    Page<ClienteModel> findByAtivoTrue(Pageable pageable);
    Optional<ClienteModel> findByIdAndAtivoTrue(Long id);
    Optional<ClienteModel> findByEmail(String email);
}
