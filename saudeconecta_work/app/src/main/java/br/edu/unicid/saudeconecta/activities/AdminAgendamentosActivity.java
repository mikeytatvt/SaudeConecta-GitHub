package br.edu.unicid.saudeconecta.activities;

import android.text.TextUtils;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.DialogTempoEsperaBinding;
import br.edu.unicid.saudeconecta.models.AgendamentoAdmin;
import br.edu.unicid.saudeconecta.models.ItemAdmin;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/**
 * GESTOR – Consultar agendamentos da rede, registrar atendimento (com tempo de espera)
 * e cancelar pela unidade. Cada registro atualiza os indicadores do dashboard.
 */
public class AdminAgendamentosActivity extends AdminListaBaseActivity {

    private Chip chipAgendadas;
    private Chip chipRealizadas;
    private Chip chipCanceladas;
    private Chip chipHoje;
    private Chip chip7;
    private Chip chip30;

    @Override
    protected int titulo() { return R.string.titulo_admin_agendamentos; }

    @Override
    protected int dicaBusca() { return R.string.busca_agendamentos; }

    @Override
    protected void configurar() {
        chipAgendadas = adicionarChip(binding.grupoFiltro1, R.string.aba_agendadas_simples);
        chipRealizadas = adicionarChip(binding.grupoFiltro1, R.string.aba_realizadas_simples);
        chipCanceladas = adicionarChip(binding.grupoFiltro1, R.string.aba_canceladas_simples);
        chipHoje = adicionarChip(binding.grupoFiltro2, R.string.filtro_hoje);
        chip7 = adicionarChip(binding.grupoFiltro2, R.string.filtro_7_dias);
        chip30 = adicionarChip(binding.grupoFiltro2, R.string.filtro_30_dias);
        chipAgendadas.setChecked(true);
        chip7.setChecked(true);
        binding.grupoFiltro1.setOnCheckedStateChangeListener((g, ids) -> carregar());
        binding.grupoFiltro2.setOnCheckedStateChangeListener((g, ids) -> carregar());
    }

    private String statusSelecionado() {
        if (chipRealizadas.isChecked()) return StatusConsulta.REALIZADA;
        if (chipCanceladas.isChecked()) return StatusConsulta.CANCELADA;
        return StatusConsulta.AGENDADA;
    }

    @Override
    protected void carregar() {
        String status = statusSelecionado();
        int dias = chipHoje.isChecked() ? 0 : chip30.isChecked() ? 30 : 7;
        LocalDate hoje = LocalDate.now();
        // Agendadas: de hoje para frente. Realizadas e canceladas: de hoje para trás.
        boolean futuro = StatusConsulta.AGENDADA.equals(status);
        LocalDate inicio = futuro ? hoje : hoje.minusDays(dias);
        LocalDate fim = futuro ? hoje.plusDays(dias) : hoje;
        if (StatusConsulta.CANCELADA.equals(status)) fim = hoje.plusDays(60); // inclui cancelamentos de datas futuras

        repositorio.listarAgendamentos(status, inicio, fim, new RepositoryCallback<List<AgendamentoAdmin>>() {
            @Override
            public void onSuccess(List<AgendamentoAdmin> lista) {
                if (!futuro) Collections.reverse(lista); // mais recentes primeiro
                List<ItemAdmin> itens = new ArrayList<>();
                for (AgendamentoAdmin a : lista) itens.add(paraItem(a));
                exibir(itens);
            }

            @Override
            public void onError(String m) { mensagem(m); }
        });
    }

    private ItemAdmin paraItem(AgendamentoAdmin a) {
        ItemAdmin i = new ItemAdmin();
        i.id = a.consulta.id;
        i.origem = a;
        i.titulo = DateUtils.isoParaBr(a.consulta.data) + " às " + a.consulta.hora + " – " + a.especialidadeNome;
        i.subtitulo = "Paciente: " + a.pacienteNome;
        StringBuilder detalhe = new StringBuilder(a.profissionalNome).append(" – ").append(a.unidadeNome);
        if (a.consulta.tempoEsperaMin != null) {
            detalhe.append("\nTempo de espera: ").append(a.consulta.tempoEsperaMin).append(" min");
        }
        if (!TextUtils.isEmpty(a.consulta.motivoCancelamento)) {
            detalhe.append("\nMotivo: ").append(a.consulta.motivoCancelamento);
        }
        i.detalhe = detalhe.toString();

        String status = a.consulta.status;
        if (StatusConsulta.REALIZADA.equals(status)) {
            i.etiqueta = getString(R.string.status_realizada);
            i.estiloEtiqueta = ItemAdmin.ETIQUETA_AZUL;
        } else if (StatusConsulta.CANCELADA.equals(status)) {
            i.etiqueta = getString(R.string.status_cancelada);
            i.estiloEtiqueta = ItemAdmin.ETIQUETA_VERMELHA;
        } else {
            i.etiqueta = getString(R.string.status_agendada);
            i.estiloEtiqueta = ItemAdmin.ETIQUETA_VERDE;
            i.acao1 = getString(R.string.registrar_atendimento);
            i.acao2 = getString(R.string.cancelar);
        }
        return i;
    }

    // ---------------------------------------------------------------- Registrar atendimento

    @Override
    public void aoAcao1(ItemAdmin item) {
        AgendamentoAdmin a = (AgendamentoAdmin) item.origem;
        DialogTempoEsperaBinding campo = DialogTempoEsperaBinding.inflate(getLayoutInflater());
        String mensagem = getString(R.string.registrar_msg, a.pacienteNome,
                DateUtils.isoParaBr(a.consulta.data) + " " + a.consulta.hora + " – " + a.especialidadeNome);
        if (LocalDate.parse(a.consulta.data).isAfter(LocalDate.now())) {
            mensagem += "\n\n" + getString(R.string.registrar_antecipado, DateUtils.isoParaBr(a.consulta.data));
        }

        androidx.appcompat.app.AlertDialog dialogo = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.registrar_titulo)
                .setMessage(mensagem)
                .setView(campo.getRoot())
                .setPositiveButton(R.string.salvar, null) // tratado abaixo para validar sem fechar
                .setNegativeButton(R.string.cancelar, null)
                .show();

        dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String texto = UiUtils.texto(campo.etTempo);
            int minutos;
            try {
                minutos = Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                minutos = -1;
            }
            if (minutos < 0 || minutos > 300) {
                campo.tilTempo.setError(getString(R.string.erro_tempo_espera));
                return;
            }
            dialogo.dismiss();
            registrar(a.consulta.id, minutos);
        });
    }

    private void registrar(long consultaId, int minutos) {
        repositorio.registrarAtendimento(consultaId, minutos, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                mensagem(R.string.registrado_sucesso);
                carregar();
            }

            @Override
            public void onError(String m) { alerta(m); }
        });
    }

    // ---------------------------------------------------------------- Cancelar pela unidade

    @Override
    public void aoAcao2(ItemAdmin item) {
        AgendamentoAdmin a = (AgendamentoAdmin) item.origem;
        String[] motivos = getResources().getStringArray(R.array.motivos_cancelamento_unidade);
        final int[] escolhido = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.cancelar_titulo)
                .setSingleChoiceItems(motivos, 0, (d, posicao) -> escolhido[0] = posicao)
                .setPositiveButton(R.string.cancelar_consulta, (d, w) -> cancelar(a, motivos[escolhido[0]]))
                .setNegativeButton(R.string.manter_consulta, null)
                .show();
    }

    private void cancelar(AgendamentoAdmin a, String motivo) {
        repositorio.cancelarPelaUnidade(a, motivo, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                mensagem(R.string.cancelado_pela_unidade);
                carregar();
            }

            @Override
            public void onError(String m) { alerta(m); }
        });
    }

    @Override
    public void aoTocar(ItemAdmin item) {
        AgendamentoAdmin a = (AgendamentoAdmin) item.origem;
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.consulta_de, a.especialidadeNome))
                .setMessage(item.subtitulo + "\n" + DateUtils.isoParaBrComDiaSemana(a.consulta.data)
                        + " às " + a.consulta.hora + "\n" + item.detalhe)
                .setPositiveButton(R.string.fechar, null)
                .show();
    }
}
