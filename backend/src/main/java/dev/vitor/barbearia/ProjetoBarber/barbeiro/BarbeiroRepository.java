package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BarbeiroRepository extends JpaRepository<BarbeiroModel, Long> {
    Page<BarbeiroModel> findByAtivoTrue(Pageable pageable);
    Optional<BarbeiroModel> findByIdAndAtivoTrue(Long id);
    Optional<BarbeiroModel> findByEmail(String email);
}
