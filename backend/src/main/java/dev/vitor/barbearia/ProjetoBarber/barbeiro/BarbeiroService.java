package dev.vitor.barbearia.ProjetoBarber.barbeiro;

import dev.vitor.barbearia.ProjetoBarber.exceptions.BusinessRuleException;
import dev.vitor.barbearia.ProjetoBarber.exceptions.ResourceNotFoundException;
import dev.vitor.barbearia.ProjetoBarber.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BarbeiroService {

    private final BarbeiroRepository barbeiroRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Page<BarbeiroModel> listarAtivos(Pageable pageable) {
        return barbeiroRepository.findByAtivoTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<BarbeiroModel> listarTodos(Pageable pageable) {
        return barbeiroRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<BarbeiroModel> buscarPorId(Long id) {
        return barbeiroRepository.findById(id);
    }

    @Transactional
    public Optional<BarbeiroModel> buscarPorIdComLock(Long id) {
        return barbeiroRepository.findByIdWithLock(id);
    }

    @Transactional
    public BarbeiroModel criar(BarbeiroDTO dto) {
        LocalTime inicioExp = dto.horarioInicioExpediente() != null
                ? dto.horarioInicioExpediente()
                : BarbeiroModel.EXPEDIENTE_INICIO_PADRAO;
        LocalTime fimExp = dto.horarioFimExpediente() != null
                ? dto.horarioFimExpediente()
                : BarbeiroModel.EXPEDIENTE_FIM_PADRAO;
        validarExpedienteEPausa(inicioExp, fimExp, dto.horarioInicioPausa(), dto.horarioFimPausa());

        BarbeiroModel barbeiro = BarbeiroModel.builder()
                .nome(dto.nome())
                .email(dto.email())
                .telefone(dto.telefone())
                .ativo(true)
                .horarioInicioExpediente(inicioExp)
                .horarioFimExpediente(fimExp)
                .horarioInicioPausa(dto.horarioInicioPausa())
                .horarioFimPausa(dto.horarioFimPausa())
                .build();
        return barbeiroRepository.save(barbeiro);
    }

    @Transactional
    public BarbeiroModel atualizar(Long id, BarbeiroDTO dto) {
        return barbeiroRepository.findById(id).map(barbeiro -> {
            LocalTime inicioExp = dto.horarioInicioExpediente() != null
                    ? dto.horarioInicioExpediente()
                    : (barbeiro.getHorarioInicioExpediente() != null ? barbeiro.getHorarioInicioExpediente() : BarbeiroModel.EXPEDIENTE_INICIO_PADRAO);
            LocalTime fimExp = dto.horarioFimExpediente() != null
                    ? dto.horarioFimExpediente()
                    : (barbeiro.getHorarioFimExpediente() != null ? barbeiro.getHorarioFimExpediente() : BarbeiroModel.EXPEDIENTE_FIM_PADRAO);

            validarExpedienteEPausa(inicioExp, fimExp, dto.horarioInicioPausa(), dto.horarioFimPausa());

            barbeiro.setNome(dto.nome());
            barbeiro.setEmail(dto.email());
            barbeiro.setTelefone(dto.telefone());
            barbeiro.setHorarioInicioExpediente(inicioExp);
            barbeiro.setHorarioFimExpediente(fimExp);
            barbeiro.setHorarioInicioPausa(dto.horarioInicioPausa());
            barbeiro.setHorarioFimPausa(dto.horarioFimPausa());
            return barbeiroRepository.save(barbeiro);
        }).orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + id));
    }

    @Transactional
    public BarbeiroModel atualizarExpediente(
            Long id,
            LocalTime horarioInicioExpediente,
            LocalTime horarioFimExpediente,
            LocalTime horarioInicioPausa,
            LocalTime horarioFimPausa) {
        BarbeiroModel barbeiro = barbeiroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + id));

        LocalTime inicioExp = horarioInicioExpediente != null
                ? horarioInicioExpediente
                : (barbeiro.getHorarioInicioExpediente() != null ? barbeiro.getHorarioInicioExpediente() : BarbeiroModel.EXPEDIENTE_INICIO_PADRAO);
        LocalTime fimExp = horarioFimExpediente != null
                ? horarioFimExpediente
                : (barbeiro.getHorarioFimExpediente() != null ? barbeiro.getHorarioFimExpediente() : BarbeiroModel.EXPEDIENTE_FIM_PADRAO);

        validarExpedienteEPausa(inicioExp, fimExp, horarioInicioPausa, horarioFimPausa);

        barbeiro.setHorarioInicioExpediente(inicioExp);
        barbeiro.setHorarioFimExpediente(fimExp);
        barbeiro.setHorarioInicioPausa(horarioInicioPausa);
        barbeiro.setHorarioFimPausa(horarioFimPausa);
        return barbeiroRepository.save(barbeiro);
    }

    public void validarExpedienteEPausa(
            LocalTime inicioExpediente,
            LocalTime fimExpediente,
            LocalTime inicioPausa,
            LocalTime fimPausa) {
        if (inicioExpediente == null || fimExpediente == null || !inicioExpediente.isBefore(fimExpediente)) {
            throw new BusinessRuleException("O horário de início do expediente deve ser anterior ao horário de término.");
        }
        if ((inicioPausa == null) != (fimPausa == null)) {
            throw new BusinessRuleException("Para configurar intervalo de pausa/almoço, informe tanto o início quanto o fim da pausa.");
        }
        if (inicioPausa != null) {
            if (!inicioPausa.isBefore(fimPausa)) {
                throw new BusinessRuleException("O início da pausa deve ser anterior ao fim da pausa.");
            }
            if (inicioPausa.isBefore(inicioExpediente) || fimPausa.isAfter(fimExpediente)) {
                throw new BusinessRuleException("O intervalo de pausa deve estar contido dentro do horário de expediente do barbeiro.");
            }
        }
    }

    @Transactional
    public void excluir(Long id) {
        BarbeiroModel barbeiro = barbeiroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + id));
        barbeiro.setAtivo(false);
        barbeiroRepository.save(barbeiro);

        if (barbeiro.getEmail() != null) {
            usuarioRepository.findByEmail(barbeiro.getEmail()).ifPresent(usuario -> {
                usuario.setAtivo(false);
                usuarioRepository.save(usuario);
            });
        }
    }

    @Transactional
    public BarbeiroModel atualizarPercentualComissao(Long id, java.math.BigDecimal percentualComissao) {
        BarbeiroModel barbeiro = barbeiroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barbeiro não encontrado com ID: " + id));
        barbeiro.setPercentualComissao(percentualComissao.setScale(2, java.math.RoundingMode.HALF_UP));
        return barbeiroRepository.save(barbeiro);
    }
}
