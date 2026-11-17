package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;

import java.time.LocalDate;
import java.util.List;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.database.dao.IndicadorDao;
import br.edu.unicid.saudeconecta.models.ItemIndicador;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.utils.AgendaUtils;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/**
 * Calcula os indicadores do gestor a partir dos atendimentos registrados.
 * Cada consulta agendada/realizada pelos pacientes alimenta automaticamente estes números.
 */
public class DashboardRepository {

    public static class Indicadores {
        public LocalDate inicio;
        public LocalDate fim;
        public int atendimentos;
        public int consultasMedicas;
        public int exames;
        public int pacientesAtendidos;
        public List<ItemIndicador> porUnidade;
        public List<ItemIndicador> porEspecialidade;
        public List<ItemIndicador> esperaPorDia;
        public double tempoMedioEspera;
        /** Redução (%) do tempo de espera entre o início e o fim do período; null se não houver dados. */
        public Double reducaoEspera;
        public double taxaCancelamento;
        public double ocupacaoAgenda;
        public int agendadasProximas;
        public int pacientesCadastrados;
        public int unidadesAtivas;
        public int profissionaisAtivos;
    }

    private static final int DIAS_UTEIS_OCUPACAO = 10;

    private final AppDatabase db;

    public DashboardRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void carregar(int dias, RepositoryCallback<Indicadores> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                Indicadores r = calcular(dias);
                AppExecutors.principal(() -> callback.onSuccess(r));
            } catch (Exception e) {
                AppExecutors.principal(() -> callback.onError("Não foi possível calcular os indicadores."));
            }
        });
    }

    private Indicadores calcular(int dias) {
        IndicadorDao dao = db.indicadorDao();
        Indicadores r = new Indicadores();
        r.fim = LocalDate.now();
        r.inicio = r.fim.minusDays(dias);
        String ini = r.inicio.toString();
        String fim = r.fim.toString();

        r.atendimentos = dao.contarAtendimentosRealizados(ini, fim);
        r.consultasMedicas = dao.contarConsultasMedicas(ini, fim);
        r.exames = dao.contarExamesRealizados(ini, fim);
        r.pacientesAtendidos = dao.contarPacientesAtendidos(ini, fim);
        r.porUnidade = dao.atendimentosPorUnidade(ini, fim);
        r.porEspecialidade = dao.atendimentosPorEspecialidade(ini, fim);
        r.esperaPorDia = dao.tempoEsperaPorDia(ini, fim);
        r.tempoMedioEspera = dao.tempoMedioEspera(ini, fim);
        r.reducaoEspera = calcularReducao(r.esperaPorDia);

        int todas = dao.contarTodasNoPeriodo(ini, fim);
        int canceladas = dao.contarPorStatusNoPeriodo(StatusConsulta.CANCELADA, ini, fim);
        r.taxaCancelamento = todas == 0 ? 0 : canceladas * 100.0 / todas;

        // Ocupação da agenda nos próximos 10 dias úteis
        List<LocalDate> proximos = DateUtils.proximosDiasUteis(LocalDate.now().plusDays(1), DIAS_UTEIS_OCUPACAO);
        r.agendadasProximas = dao.contarPorStatusNoPeriodo(StatusConsulta.AGENDADA,
                proximos.get(0).toString(), proximos.get(proximos.size() - 1).toString());
        r.profissionaisAtivos = dao.contarProfissionaisAtivos();
        int capacidade = r.profissionaisAtivos * DIAS_UTEIS_OCUPACAO * AgendaUtils.HORARIOS_ATENDIMENTO.length;
        r.ocupacaoAgenda = capacidade == 0 ? 0 : r.agendadasProximas * 100.0 / capacidade;

        r.pacientesCadastrados = dao.contarPacientes();
        r.unidadesAtivas = dao.contarUnidadesAtivas();
        return r;
    }

    /** Compara a média do primeiro terço do período com a do último terço. */
    private static Double calcularReducao(List<ItemIndicador> serie) {
        if (serie == null || serie.size() < 3) return null;
        int k = Math.max(1, serie.size() / 3);
        double inicio = 0;
        double fim = 0;
        for (int i = 0; i < k; i++) {
            inicio += serie.get(i).valor;
            fim += serie.get(serie.size() - 1 - i).valor;
        }
        inicio /= k;
        fim /= k;
        if (inicio <= 0) return null;
        return (inicio - fim) * 100.0 / inicio;
    }
}
