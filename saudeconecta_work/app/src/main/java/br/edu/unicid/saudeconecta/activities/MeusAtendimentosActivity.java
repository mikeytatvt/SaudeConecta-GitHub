package br.edu.unicid.saudeconecta.activities;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.adapters.AtendimentoAdapter;
import br.edu.unicid.saudeconecta.databinding.ActivityMeusAtendimentosBinding;
import br.edu.unicid.saudeconecta.databinding.BottomSheetAtendimentoBinding;
import br.edu.unicid.saudeconecta.databinding.ItemInfoLinhaBinding;
import br.edu.unicid.saudeconecta.models.ConsultaDetalhada;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.models.TipoAtendimento;
import br.edu.unicid.saudeconecta.repositories.ConsultaRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.LembreteScheduler;
import br.edu.unicid.saudeconecta.utils.NotificacaoHelper;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/** TELA 4 – Meus atendimentos: agendados, realizados e cancelados, com detalhes e cancelamento. */
public class MeusAtendimentosActivity extends AppCompatActivity implements AtendimentoAdapter.Listener {

    private static final int ABA_AGENDADAS = 0;
    private static final int ABA_REALIZADAS = 1;
    private static final int ABA_CANCELADAS = 2;

    private ActivityMeusAtendimentosBinding binding;
    private SessionManager sessao;
    private ConsultaRepository repositorio;
    private AtendimentoAdapter adapter;

    private final List<ConsultaDetalhada> agendadas = new ArrayList<>();
    private final List<ConsultaDetalhada> realizadas = new ArrayList<>();
    private final List<ConsultaDetalhada> canceladas = new ArrayList<>();
    private int abaAtual = ABA_AGENDADAS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        if (!sessao.isLogado() || sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityMeusAtendimentosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        repositorio = new ConsultaRepository(this);
        adapter = new AtendimentoAdapter(this);
        binding.rvAtendimentos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAtendimentos.setAdapter(adapter);

        binding.tabs.addTab(binding.tabs.newTab());
        binding.tabs.addTab(binding.tabs.newTab());
        binding.tabs.addTab(binding.tabs.newTab());
        atualizarTitulosAbas();
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                abaAtual = tab.getPosition();
                exibirAbaAtual();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        binding.btnAgendarVazio.setOnClickListener(v ->
                startActivity(new Intent(this, AgendamentoActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) carregar(); // atualiza ao voltar de um novo agendamento
    }

    // ---------------------------------------------------------------- Dados

    private void carregar() {
        repositorio.listarAtendimentos(sessao.getPacienteId(), new RepositoryCallback<List<ConsultaDetalhada>>() {
            @Override
            public void onSuccess(List<ConsultaDetalhada> lista) {
                if (isFinishing()) return;
                separarPorStatus(lista);
                atualizarTitulosAbas();
                exibirAbaAtual();
            }

            @Override
            public void onError(String mensagem) {
                if (!isFinishing()) Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void separarPorStatus(List<ConsultaDetalhada> lista) {
        agendadas.clear();
        realizadas.clear();
        canceladas.clear();
        for (ConsultaDetalhada c : lista) {
            if (StatusConsulta.AGENDADA.equals(c.consulta.status)) agendadas.add(c);
            else if (StatusConsulta.REALIZADA.equals(c.consulta.status)) realizadas.add(c);
            else canceladas.add(c);
        }
        // Agendadas: a mais próxima primeiro. Demais: a mais recente primeiro (ordem do banco).
        Collections.reverse(agendadas);
    }

    private void atualizarTitulosAbas() {
        TabLayout.Tab a = binding.tabs.getTabAt(ABA_AGENDADAS);
        TabLayout.Tab r = binding.tabs.getTabAt(ABA_REALIZADAS);
        TabLayout.Tab c = binding.tabs.getTabAt(ABA_CANCELADAS);
        if (a != null) a.setText(getString(R.string.aba_agendadas, agendadas.size()));
        if (r != null) r.setText(getString(R.string.aba_realizadas, realizadas.size()));
        if (c != null) c.setText(getString(R.string.aba_canceladas, canceladas.size()));
    }

    private void exibirAbaAtual() {
        List<ConsultaDetalhada> lista;
        int mensagemVazia;
        if (abaAtual == ABA_REALIZADAS) {
            lista = realizadas;
            mensagemVazia = R.string.vazio_realizadas;
        } else if (abaAtual == ABA_CANCELADAS) {
            lista = canceladas;
            mensagemVazia = R.string.vazio_canceladas;
        } else {
            lista = agendadas;
            mensagemVazia = R.string.vazio_agendadas;
        }
        adapter.atualizar(lista);
        boolean vazia = lista.isEmpty();
        binding.rvAtendimentos.setVisibility(vazia ? View.GONE : View.VISIBLE);
        binding.layoutVazio.setVisibility(vazia ? View.VISIBLE : View.GONE);
        binding.tvVazio.setText(mensagemVazia);
        binding.btnAgendarVazio.setVisibility(abaAtual == ABA_AGENDADAS ? View.VISIBLE : View.GONE);
    }

    // ---------------------------------------------------------------- Detalhes

    @Override
    public void aoVerDetalhes(ConsultaDetalhada c) {
        BottomSheetDialog painel = new BottomSheetDialog(this);
        BottomSheetAtendimentoBinding sheet = BottomSheetAtendimentoBinding.inflate(getLayoutInflater());
        painel.setContentView(sheet.getRoot());

        sheet.tvTitulo.setText(getString(R.string.consulta_de, c.especialidadeNome));
        AtendimentoAdapter.aplicarEstiloStatus(sheet.tvStatus, c.consulta.status);

        adicionarInfo(sheet, R.string.rotulo_profissional, c.profissionalNome);
        adicionarInfo(sheet, R.string.rotulo_data, DateUtils.isoParaBrComDiaSemana(c.consulta.data));
        adicionarInfo(sheet, R.string.rotulo_horario, c.consulta.hora);
        adicionarInfo(sheet, R.string.rotulo_tipo, getString(nomeTipo(c.consulta.tipoAtendimento)));
        adicionarInfo(sheet, R.string.rotulo_unidade, c.unidadeNome);
        String endereco = enderecoCompleto(c);
        adicionarInfo(sheet, R.string.rotulo_endereco, endereco);
        adicionarInfo(sheet, R.string.rotulo_telefone, c.unidadeTelefone);
        if (StatusConsulta.REALIZADA.equals(c.consulta.status) && c.consulta.tempoEsperaMin != null) {
            adicionarInfo(sheet, R.string.rotulo_tempo_espera, getString(R.string.minutos, c.consulta.tempoEsperaMin));
        }
        if (StatusConsulta.CANCELADA.equals(c.consulta.status)) {
            adicionarInfo(sheet, R.string.rotulo_motivo, c.consulta.motivoCancelamento);
        }
        adicionarInfo(sheet, R.string.rotulo_observacoes, c.consulta.observacoes);

        // Exames pedidos nesta consulta (aparecem nos atendimentos realizados)
        repositorio.listarExamesDaConsulta(c.consulta.id, new RepositoryCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> exames) {
                if (!exames.isEmpty() && painel.isShowing()) {
                    adicionarInfo(sheet, R.string.rotulo_exames, TextUtils.join("\n", exames));
                }
            }

            @Override
            public void onError(String mensagem) { }
        });

        sheet.btnComoChegar.setOnClickListener(v -> abrirMapa(endereco + ", São Paulo - SP"));
        if (AtendimentoAdapter.podeCancelar(c)) {
            sheet.btnCancelarConsulta.setVisibility(View.VISIBLE);
            sheet.btnCancelarConsulta.setOnClickListener(v -> {
                painel.dismiss();
                aoCancelar(c);
            });
        }
        painel.show();
    }

    private void adicionarInfo(BottomSheetAtendimentoBinding sheet, int rotulo, String valor) {
        if (valor == null || valor.trim().isEmpty()) return;
        ItemInfoLinhaBinding linha = ItemInfoLinhaBinding.inflate(getLayoutInflater(), sheet.containerInfo, false);
        linha.tvRotulo.setText(rotulo);
        linha.tvValor.setText(valor);
        sheet.containerInfo.addView(linha.getRoot());
    }

    private static String enderecoCompleto(ConsultaDetalhada c) {
        return c.unidadeEndereco + (c.unidadeBairro != null ? " – " + c.unidadeBairro : "");
    }

    private static int nomeTipo(String tipo) {
        if (TipoAtendimento.RETORNO.equals(tipo)) return R.string.tipo_retorno;
        if (TipoAtendimento.PROCEDIMENTO.equals(tipo)) return R.string.tipo_procedimento;
        if (TipoAtendimento.ODONTOLOGICO.equals(tipo)) return R.string.tipo_odontologico;
        return R.string.tipo_consulta_medica;
    }

    private void abrirMapa(String endereco) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(endereco))));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), R.string.erro_mapa, Snackbar.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- Cancelamento

    @Override
    public void aoCancelar(ConsultaDetalhada c) {
        String[] motivos = getResources().getStringArray(R.array.motivos_cancelamento);
        final int[] escolhido = {0};
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.cancelar_titulo))
                .setSingleChoiceItems(motivos, 0, (d, posicao) -> escolhido[0] = posicao)
                .setPositiveButton(R.string.cancelar_consulta, (d, w) -> cancelar(c, motivos[escolhido[0]]))
                .setNegativeButton(R.string.manter_consulta, null)
                .show();
    }

    private void cancelar(ConsultaDetalhada c, String motivo) {
        repositorio.cancelarConsulta(c.consulta.id, sessao.getPacienteId(), sessao.getUsuarioId(), motivo,
                new RepositoryCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean ok) {
                        LembreteScheduler.cancelar(MeusAtendimentosActivity.this, c.consulta.id);
                        NotificacaoHelper.remover(MeusAtendimentosActivity.this,
                                NotificacaoHelper.idConfirmacao(c.consulta.id));
                        if (isFinishing()) return;
                        Snackbar.make(binding.getRoot(), R.string.cancelamento_sucesso, Snackbar.LENGTH_LONG).show();
                        carregar();
                    }

                    @Override
                    public void onError(String mensagem) {
                        if (isFinishing()) return;
                        new MaterialAlertDialogBuilder(MeusAtendimentosActivity.this)
                                .setMessage(mensagem)
                                .setPositiveButton(R.string.fechar, null)
                                .show();
                    }
                });
    }
}
