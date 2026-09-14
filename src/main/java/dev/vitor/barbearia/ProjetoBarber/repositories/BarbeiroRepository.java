package dev.vitor.barbearia.ProjetoBarber.repositories;

import dev.vitor.barbearia.ProjetoBarber.models.Barbeiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarbeiroRepository extends JpaRepository<Barbeiro, Long> {
}
