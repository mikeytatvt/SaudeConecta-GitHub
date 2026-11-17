package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import br.edu.unicid.saudeconecta.models.ItemIndicador;

/** Consultas agregadas que alimentam o Dashboard do Gestor. */
@Dao
public interface IndicadorDao {

    @Query("SELECT COUNT(*) FROM consultas WHERE status = 'REALIZADA' AND data BETWEEN :inicio AND :fim")
    int contarAtendimentosRealizados(String inicio, String fim);

    @Query("SELECT COUNT(*) FROM consultas WHERE status = 'REALIZADA' AND tipo_atendimento = 'CONSULTA_MEDICA' "
            + "AND data BETWEEN :inicio AND :fim")
    int contarConsultasMedicas(String inicio, String fim);

    @Query("SELECT COUNT(*) FROM exames WHERE status IN ('REALIZADO', 'RESULTADO_DISPONIVEL') "
            + "AND data_realizacao BETWEEN :inicio AND :fim")
    int contarExamesRealizados(String inicio, String fim);

    @Query("SELECT COUNT(DISTINCT paciente_id) FROM consultas WHERE status = 'REALIZADA' "
            + "AND data BETWEEN :inicio AND :fim")
    int contarPacientesAtendidos(String inicio, String fim);

    @Query("SELECT u.nome AS rotulo, COUNT(c.id) AS valor FROM unidades_saude u "
            + "LEFT JOIN consultas c ON c.unidade_id = u.id AND c.status = 'REALIZADA' "
            + "AND c.data BETWEEN :inicio AND :fim "
            + "WHERE u.ativo = 1 GROUP BY u.id ORDER BY valor DESC, u.nome")
    List<ItemIndicador> atendimentosPorUnidade(String inicio, String fim);

    @Query("SELECT e.nome AS rotulo, COUNT(c.id) AS valor FROM especialidades e "
            + "LEFT JOIN consultas c ON c.especialidade_id = e.id AND c.status = 'REALIZADA' "
            + "AND c.data BETWEEN :inicio AND :fim "
            + "GROUP BY e.id ORDER BY valor DESC, e.nome")
    List<ItemIndicador> atendimentosPorEspecialidade(String inicio, String fim);

    @Query("SELECT data AS rotulo, AVG(tempo_espera_min) AS valor FROM consultas "
            + "WHERE status = 'REALIZADA' AND tempo_espera_min IS NOT NULL AND data BETWEEN :inicio AND :fim "
            + "GROUP BY data ORDER BY data")
    List<ItemIndicador> tempoEsperaPorDia(String inicio, String fim);

    @Query("SELECT COALESCE(AVG(tempo_espera_min), 0) FROM consultas WHERE status = 'REALIZADA' "
            + "AND tempo_espera_min IS NOT NULL AND data BETWEEN :inicio AND :fim")
    double tempoMedioEspera(String inicio, String fim);

    @Query("SELECT COUNT(*) FROM consultas WHERE status = :status AND data BETWEEN :inicio AND :fim")
    int contarPorStatusNoPeriodo(String status, String inicio, String fim);

    @Query("SELECT COUNT(*) FROM consultas WHERE data BETWEEN :inicio AND :fim")
    int contarTodasNoPeriodo(String inicio, String fim);

    @Query("SELECT COUNT(*) FROM pacientes")
    int contarPacientes();

    @Query("SELECT COUNT(*) FROM unidades_saude WHERE ativo = 1")
    int contarUnidadesAtivas();

    @Query("SELECT COUNT(*) FROM profissionais WHERE ativo = 1")
    int contarProfissionaisAtivos();
}
