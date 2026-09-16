package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface BarbeiroRepository extends JpaRepository<BarbeiroModel, Long> {}
