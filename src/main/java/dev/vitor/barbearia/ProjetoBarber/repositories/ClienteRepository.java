package dev.vitor.barbearia.ProjetoBarber.repositories;

import dev.vitor.barbearia.ProjetoBarber.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
