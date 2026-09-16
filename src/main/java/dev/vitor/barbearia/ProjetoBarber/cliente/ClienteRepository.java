package dev.vitor.barbearia.ProjetoBarber.cliente;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ClienteRepository extends JpaRepository<ClienteModel, Long> {}
