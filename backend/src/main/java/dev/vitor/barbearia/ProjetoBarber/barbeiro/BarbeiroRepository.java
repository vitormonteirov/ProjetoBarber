package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BarbeiroRepository extends JpaRepository<BarbeiroModel, Long> {
    Page<BarbeiroModel> findByAtivoTrue(Pageable pageable);
    Optional<BarbeiroModel> findByIdAndAtivoTrue(Long id);
    Optional<BarbeiroModel> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BarbeiroModel b WHERE b.id = :id")
    Optional<BarbeiroModel> findByIdWithLock(@Param("id") Long id);
}
