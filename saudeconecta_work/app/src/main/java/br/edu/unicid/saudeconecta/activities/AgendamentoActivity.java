package br.edu.unicid.saudeconecta.activities;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.adapters.OpcoesAdapter;
import br.edu.unicid.saudeconecta.databinding.ActivityAgendamentoBinding;
import br.edu.unicid.saudeconecta.databinding.ItemDiaBinding;
import br.edu.unicid.saudeconecta.databinding.ItemHorarioBinding;
import br.edu.unicid.saudeconecta.models.Especialidade;
import br.edu.unicid.saudeconecta.models.Profissional;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.repositories.AgendamentoRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.AgendaUtils;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.LembreteScheduler;
import br.edu.unicid.saudeconecta.utils.NotificacaoHelper;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/**
 * TELA 3 – Agendamento em 4 etapas (Figura 2 do banner):
 * 1 Serviço (especialidade) → 2 Profissional (unidade + profissional) → 3 Data e hora → 4 Confirmação.
 */
public class AgendamentoActivity extends AppCompatActivity {

    private static final int DIAS_EXIBIDOS = 10;

    private ActivityAgendamentoBinding binding;
    private SessionManager sessao;
    private AgendamentoRepository repositorio;

    private List<Especialidade> especialidades = new ArrayList<>();
    private List<UnidadeSaude> unidades = new ArrayList<>();
    private List<Profissional> profissionais = new ArrayList<>();
    private final List<LocalDate> dias = new ArrayList<>();
    private Map<String, Set<String>> agendaOcupada = new HashMap<>();

    private Especialidade especialidadeEscolhida;
    private UnidadeSaude unidadeEscolhida;
    private Profissional profissionalEscolhido;
    private LocalDate diaEscolhido;
    private String horaEscolhida;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        if (!sessao.isLogado() || sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityAgendamentoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        repositorio = new AgendamentoRepository(this);
        // Sistema verifica disponibilidade nos próximos dias úteis (a partir de amanhã)
        dias.addAll(DateUtils.proximosDiasUteis(LocalDate.now().plusDays(1), DIAS_EXIBIDOS));

        binding.actvEspecialidade.setOnItemClickListener((p, v, posicao, id) -> escolherEspecialidade(posicao));
        binding.actvUnidade.setOnItemClickListener((p, v, posicao, id) -> escolherUnidade(posicao));
        binding.actvProfissional.setOnItemClickListener((p, v, posicao, id) -> escolherProfissional(posicao));
        binding.btnConfirmar.setOnClickListener(v -> mostrarConfirmacao());

        binding.tilUnidade.setEnabled(false);       // liberados conforme as escolhas anteriores
        binding.tilProfissional.setEnabled(false);
        atualizarEtapa(1);
        carregarEspecialidades();
    }

    // ============================================================ ETAPA 1 – Serviço

    private void carregarEspecialidades() {
        repositorio.listarEspecialidades(new RepositoryCallback<List<Especialidade>>() {
            @Override
            public void onSuccess(List<Especialidade> lista) {
                especialidades = lista;
                List<String> nomes = new ArrayList<>();
                for (Especialidade e : lista) nomes.add(e.nome);
                binding.actvEspecialidade.setAdapter(new OpcoesAdapter(AgendamentoActivity.this, nomes));
            }

            @Override
            public void onError(String mensagem) {
                mostrarErro(mensagem);
            }
        });
    }

    private void escolherEspecialidade(int posicao) {
        especialidadeEscolhida = especialidades.get(posicao);
        limparSelecao(binding.tilUnidade, binding.actvUnidade);
        limparSelecao(binding.tilProfissional, binding.actvProfissional);
        unidadeEscolhida = null;
        profissionalEscolhido = null;
        limparAgenda();
        atualizarEtapa(2);

        repositorio.listarUnidades(especialidadeEscolhida.id, new RepositoryCallback<List<UnidadeSaude>>() {
            @Override
            public void onSuccess(List<UnidadeSaude> lista) {
                unidades = lista;
                List<String> nomes = new ArrayList<>();
                for (UnidadeSaude u : lista) nomes.add(u.nome);
                binding.actvUnidade.setAdapter(new OpcoesAdapter(AgendamentoActivity.this, nomes));
                binding.tilUnidade.setEnabled(!lista.isEmpty());
                if (lista.size() == 1) {
                    binding.actvUnidade.setText(nomes.get(0), false);
                    escolherUnidade(0);
                }
            }

            @Override
            public void onError(String mensagem) {
                mostrarErro(mensagem);
            }
        });
    }

    // ============================================================ ETAPA 2 – Unidade e profissional

    private void escolherUnidade(int posicao) {
        unidadeEscolhida = unidades.get(posicao);
        limparSelecao(binding.tilProfissional, binding.actvProfissional);
        profissionalEscolhido = null;
        limparAgenda();
        atualizarEtapa(2);

        repositorio.listarProfissionais(especialidadeEscolhida.id, unidadeEscolhida.id,
                new RepositoryCallback<List<Profissional>>() {
                    @Override
                    public void onSuccess(List<Profissional> lista) {
                        profissionais = lista;
                        List<String> nomes = new ArrayList<>();
                        for (Profissional p : lista) nomes.add(p.nome);
                        binding.actvProfissional.setAdapter(new OpcoesAdapter(AgendamentoActivity.this, nomes));
                        binding.tilProfissional.setEnabled(!lista.isEmpty());
                        if (lista.size() == 1) {
                            binding.actvProfissional.setText(nomes.get(0), false);
                            escolherProfissional(0);
                        }
                    }

                    @Override
                    public void onError(String mensagem) {
                        mostrarErro(mensagem);
                    }
                });
    }

    private void escolherProfissional(int posicao) {
        profissionalEscolhido = profissionais.get(posicao);
        atualizarEtapa(3);
        carregarAgenda();
    }

    // ============================================================ ETAPA 3 – Data e hora

    /** Sistema verifica disponibilidade: busca os horários já ocupados do profissional. */
    private void carregarAgenda() {
        limparAgenda();
        binding.tvDatasInfo.setText(R.string.info_carregando_agenda);
        long profissionalId = profissionalEscolhido.id;
        repositorio.carregarAgenda(profissionalId, dias.get(0), dias.get(dias.size() - 1),
                new RepositoryCallback<Map<String, Set<String>>>() {
                    @Override
                    public void onSuccess(Map<String, Set<String>> agenda) {
                        // Ignora respostas antigas se o usuário trocou de profissional nesse meio-tempo
                        if (profissionalEscolhido == null || profissionalEscolhido.id != profissionalId) return;
                        agendaOcupada = agenda;
                        montarDias();
                    }

                    @Override
                    public void onError(String mensagem) {
                        mostrarErro(mensagem);
                    }
                });
    }

    private int horariosLivres(LocalDate dia) {
        Set<String> ocupados = agendaOcupada.get(dia.toString());
        int livres = 0;
        for (String h : AgendaUtils.HORARIOS_ATENDIMENTO) {
            if (ocupados == null || !ocupados.contains(h)) livres++;
        }
        return livres;
    }

    private void montarDias() {
        binding.containerDatas.removeAllViews();
        LocalDate primeiroDisponivel = null;

        for (LocalDate dia : dias) {
            ItemDiaBinding item = ItemDiaBinding.inflate(getLayoutInflater(), binding.containerDatas, false);
            String semana = DateUtils.diaSemanaCurto(dia);
            String data = DateUtils.diaMes(dia);
            item.tvDiaSemana.setText(semana);
            item.tvDiaData.setText(data);
            item.getRoot().setContentDescription(getString(R.string.cd_dia, semana, data));
            item.getRoot().setTag(dia);

            boolean disponivel = horariosLivres(dia) > 0;
            if (disponivel && primeiroDisponivel == null) primeiroDisponivel = dia;
            item.getRoot().setOnClickListener(v -> escolherDia(dia));
            estilizarOpcao(item.getRoot(), false, disponivel, item.tvDiaData, item.tvDiaSemana);
            binding.containerDatas.addView(item.getRoot());
        }

        if (primeiroDisponivel == null) {
            binding.tvDatasInfo.setText(R.string.info_sem_datas);
            binding.tvHorariosInfo.setText(R.string.info_escolha_data);
            return;
        }
        binding.tvDatasInfo.setText(R.string.info_datas_legenda);
        escolherDia(primeiroDisponivel); // já mostra os horários do primeiro dia livre
    }

    private void escolherDia(LocalDate dia) {
        diaEscolhido = dia;
        horaEscolhida = null;
        binding.btnConfirmar.setEnabled(false);
        atualizarEtapa(3);

        for (int i = 0; i < binding.containerDatas.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) binding.containerDatas.getChildAt(i);
            LocalDate d = (LocalDate) card.getTag();
            ItemDiaBinding item = ItemDiaBinding.bind(card);
            estilizarOpcao(card, d.equals(dia), horariosLivres(d) > 0, item.tvDiaData, item.tvDiaSemana);
        }
        montarHorarios();
    }

    private void montarHorarios() {
        binding.containerHorarios.removeAllViews();
        Set<String> ocupados = agendaOcupada.get(diaEscolhido.toString());
        if (ocupados == null) ocupados = Collections.emptySet();
        int espaco = Math.round(6 * getResources().getDisplayMetrics().density);

        String[] horarios = AgendaUtils.HORARIOS_ATENDIMENTO;
        for (int i = 0; i < horarios.length; i++) {
            String hora = horarios[i];
            ItemHorarioBinding item = ItemHorarioBinding.inflate(getLayoutInflater(), binding.containerHorarios, false);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) item.getRoot().getLayoutParams();
            if (i > 0) lp.setMarginStart(espaco);
            item.getRoot().setLayoutParams(lp);

            boolean livre = !ocupados.contains(hora);
            item.tvHora.setText(hora);
            item.getRoot().setTag(hora);
            if (livre) {
                item.getRoot().setContentDescription(hora);
                item.tvHora.setPaintFlags(item.tvHora.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                item.getRoot().setContentDescription(getString(R.string.cd_horario_ocupado, hora));
                item.tvHora.setPaintFlags(item.tvHora.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            item.getRoot().setOnClickListener(v -> escolherHora(hora));
            estilizarOpcao(item.getRoot(), false, livre, item.tvHora, null);
            binding.containerHorarios.addView(item.getRoot());
        }
        binding.tvHorariosInfo.setText(ocupados.isEmpty() ? "" : getString(R.string.info_horarios_legenda));
        binding.tvHorariosInfo.setVisibility(ocupados.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void escolherHora(String hora) {
        horaEscolhida = hora;
        Set<String> ocupados = agendaOcupada.get(diaEscolhido.toString());
        for (int i = 0; i < binding.containerHorarios.getChildCount(); i++) {
            MaterialCardView card = (MaterialCardView) binding.containerHorarios.getChildAt(i);
            String h = (String) card.getTag();
            boolean livre = ocupados == null || !ocupados.contains(h);
            estilizarOpcao(card, h.equals(hora), livre, ItemHorarioBinding.bind(card).tvHora, null);
        }
        binding.btnConfirmar.setEnabled(true);
        atualizarEtapa(4);
        binding.scroll.post(() -> binding.scroll.smoothScrollTo(0, binding.btnConfirmar.getBottom()));
    }

    // ============================================================ ETAPA 4 – Confirmação

    private void mostrarConfirmacao() {
        if (profissionalEscolhido == null || diaEscolhido == null || horaEscolhida == null) return;
        String mensagem = getString(R.string.confirmacao_msg,
                especialidadeEscolhida.nome,
                unidadeEscolhida.nome,
                profissionalEscolhido.nome,
                DateUtils.isoParaBrComDiaSemana(diaEscolhido.toString()),
                horaEscolhida);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirmacao_titulo)
                .setMessage(mensagem)
                .setPositiveButton(R.string.confirmar, (d, w) -> salvarAgendamento())
                .setNegativeButton(R.string.voltar, null)
                .show();
    }

    /** Sistema registra o atendimento. */
    private void salvarAgendamento() {
        setCarregando(true);
        String data = diaEscolhido.toString();
        String hora = horaEscolhida;
        repositorio.agendar(sessao.getPacienteId(), sessao.getUsuarioId(), profissionalEscolhido,
                especialidadeEscolhida.nome, unidadeEscolhida.nome, data, hora,
                new RepositoryCallback<Long>() {
                    @Override
                    public void onSuccess(Long consultaId) {
                        if (isFinishing()) return;
                        setCarregando(false);
                        avisarNoSistema(consultaId, data, hora);
                        mostrarSucesso(data, hora);
                    }

                    @Override
                    public void onError(String mensagem) {
                        if (isFinishing()) return;
                        setCarregando(false);
                        new MaterialAlertDialogBuilder(AgendamentoActivity.this)
                                .setMessage(mensagem)
                                .setPositiveButton(R.string.fechar, null)
                                .show();
                        carregarAgenda(); // atualiza a disponibilidade
                    }
                });
    }

    /** Notificação na barra do Android e lembrete automático 24 h antes da consulta. */
    private void avisarNoSistema(long consultaId, String data, String hora) {
        String mensagem = "Sua consulta de " + especialidadeEscolhida.nome + " com " + profissionalEscolhido.nome
                + " na " + unidadeEscolhida.nome + " foi confirmada para " + DateUtils.isoParaBr(data)
                + " às " + hora + ".";
        NotificacaoHelper.mostrar(this, NotificacaoHelper.idConfirmacao(consultaId), "Consulta confirmada",
                mensagem, TipoNotificacao.CONFIRMACAO);
        LembreteScheduler.agendar(this, consultaId, sessao.getUsuarioId(), data, hora);
    }

    private void mostrarSucesso(String data, String hora) {
        String mensagem = getString(R.string.agendamento_sucesso_msg,
                especialidadeEscolhida.nome, DateUtils.isoParaBr(data), hora, unidadeEscolhida.nome);
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_check_circle)
                .setTitle(R.string.agendamento_sucesso_titulo)
                .setMessage(mensagem)
                .setCancelable(false)
                .setPositiveButton(R.string.voltar_inicio, (d, w) -> finish())
                .show();
    }

    // ============================================================ Apoio visual

    /** Atualiza o indicador 1-2-3-4 do topo. */
    private void atualizarEtapa(int etapa) {
        TextView[] circulos = {binding.etapa1, binding.etapa2, binding.etapa3, binding.etapa4};
        TextView[] rotulos = {binding.rotulo1, binding.rotulo2, binding.rotulo3, binding.rotulo4};
        View[] linhasEsquerda = {binding.linha1Esq, binding.linha2Esq, binding.linha3Esq, binding.linha4Esq};
        View[] linhasDireita = {binding.linha1Dir, binding.linha2Dir, binding.linha3Dir, binding.linha4Dir};

        int verde = getColor(R.color.verde_escuro);
        int cinza = getColor(R.color.borda);
        for (int i = 0; i < 4; i++) {
            int numero = i + 1;
            boolean alcancada = numero <= etapa;
            circulos[i].setBackgroundResource(alcancada ? R.drawable.bg_etapa_ativa : R.drawable.bg_etapa_inativa);
            circulos[i].setTextColor(getColor(alcancada ? R.color.branco : R.color.texto_secundario));
            rotulos[i].setTextColor(getColor(numero == etapa ? R.color.verde_escuro : R.color.texto_secundario));
            rotulos[i].setTypeface(null, numero == etapa ? Typeface.BOLD : Typeface.NORMAL);
            linhasEsquerda[i].setBackgroundColor(alcancada ? verde : cinza);
            linhasDireita[i].setBackgroundColor(numero < etapa ? verde : cinza);
        }
    }

    /** Selecionado = verde com texto branco; indisponível = esmaecido e sem clique. */
    private void estilizarOpcao(MaterialCardView card, boolean selecionado, boolean disponivel,
                                TextView textoPrincipal, TextView textoSecundario) {
        card.setEnabled(disponivel);
        card.setAlpha(disponivel ? 1f : 0.4f);
        card.setCardBackgroundColor(getColor(selecionado ? R.color.verde_escuro : R.color.branco));
        card.setStrokeColor(getColor(selecionado ? R.color.verde_escuro : R.color.borda));
        textoPrincipal.setTextColor(getColor(selecionado ? R.color.branco : R.color.texto_primario));
        if (textoSecundario != null) {
            textoSecundario.setTextColor(getColor(selecionado ? R.color.branco_translucido : R.color.texto_secundario));
        }
    }

    private void limparSelecao(TextInputLayout campo, MaterialAutoCompleteTextView lista) {
        lista.setText("", false);
        campo.setEnabled(false);
    }

    private void limparAgenda() {
        diaEscolhido = null;
        horaEscolhida = null;
        agendaOcupada = new HashMap<>();
        binding.containerDatas.removeAllViews();
        binding.containerHorarios.removeAllViews();
        binding.tvDatasInfo.setText(R.string.info_escolha_profissional);
        binding.tvHorariosInfo.setText(R.string.info_escolha_data);
        binding.tvHorariosInfo.setVisibility(View.VISIBLE);
        binding.btnConfirmar.setEnabled(false);
    }

    private void setCarregando(boolean carregando) {
        binding.btnConfirmar.setEnabled(!carregando);
        binding.btnConfirmar.setText(carregando ? R.string.agendando : R.string.confirmar_agendamento);
    }

    private void mostrarErro(String mensagem) {
        if (!isFinishing()) Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
    }
}
