package br.edu.unicid.saudeconecta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.adapters.ExameAdapter;
import br.edu.unicid.saudeconecta.databinding.ActivityExamesBinding;
import br.edu.unicid.saudeconecta.databinding.BottomSheetResultadoBinding;
import br.edu.unicid.saudeconecta.databinding.ItemInfoLinhaBinding;
import br.edu.unicid.saudeconecta.models.ExameDetalhado;
import br.edu.unicid.saudeconecta.models.ResultadoExame;
import br.edu.unicid.saudeconecta.models.StatusExame;
import br.edu.unicid.saudeconecta.repositories.ExameRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/** TELA 5 – Exames e resultados: lista, situação, resultado e PDF. */
public class ExamesActivity extends AppCompatActivity implements ExameAdapter.Listener {

    private ActivityExamesBinding binding;
    private SessionManager sessao;
    private ExameRepository repositorio;
    private ExameAdapter adapter;
    private final List<ExameDetalhado> todos = new ArrayList<>();
    private boolean gerandoPdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        if (!sessao.isLogado() || sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityExamesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        repositorio = new ExameRepository(this);
        adapter = new ExameAdapter(this);
        binding.rvExames.setLayoutManager(new LinearLayoutManager(this));
        binding.rvExames.setAdapter(adapter);

        binding.chipTodos.setChecked(true);
        atualizarContadores();
        binding.grupoFiltros.setOnCheckedStateChangeListener((grupo, ids) -> aplicarFiltro());

        carregar();
    }

    private void carregar() {
        repositorio.listarExames(sessao.getPacienteId(), new RepositoryCallback<List<ExameDetalhado>>() {
            @Override
            public void onSuccess(List<ExameDetalhado> lista) {
                if (isFinishing()) return;
                todos.clear();
                todos.addAll(lista);
                atualizarContadores();
                aplicarFiltro();
            }

            @Override
            public void onError(String mensagem) {
                if (!isFinishing()) Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private List<ExameDetalhado> filtrar(boolean comResultado) {
        List<ExameDetalhado> saida = new ArrayList<>();
        for (ExameDetalhado e : todos) {
            boolean disponivel = StatusExame.RESULTADO_DISPONIVEL.equals(e.exame.status);
            if (disponivel == comResultado) saida.add(e);
        }
        return saida;
    }

    private void atualizarContadores() {
        binding.chipTodos.setText(getString(R.string.filtro_todos, todos.size()));
        binding.chipResultados.setText(getString(R.string.filtro_resultados, filtrar(true).size()));
        binding.chipAndamento.setText(getString(R.string.filtro_andamento, filtrar(false).size()));
    }

    private void aplicarFiltro() {
        List<ExameDetalhado> lista;
        if (binding.chipResultados.isChecked()) lista = filtrar(true);
        else if (binding.chipAndamento.isChecked()) lista = filtrar(false);
        else lista = todos;

        adapter.atualizar(lista);
        boolean vazia = lista.isEmpty();
        binding.rvExames.setVisibility(vazia ? View.GONE : View.VISIBLE);
        binding.layoutVazio.setVisibility(vazia ? View.VISIBLE : View.GONE);
        binding.tvVazio.setText(R.string.vazio_exames);
    }

    // ---------------------------------------------------------------- Resultado

    @Override
    public void aoVerResultado(ExameDetalhado exame) {
        repositorio.buscarResultado(exame.exame.id, new RepositoryCallback<ResultadoExame>() {
            @Override
            public void onSuccess(ResultadoExame resultado) {
                if (!isFinishing()) mostrarResultado(exame, resultado);
            }

            @Override
            public void onError(String mensagem) {
                if (!isFinishing()) Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void mostrarResultado(ExameDetalhado exame, ResultadoExame resultado) {
        BottomSheetDialog painel = new BottomSheetDialog(this);
        BottomSheetResultadoBinding sheet = BottomSheetResultadoBinding.inflate(getLayoutInflater());
        painel.setContentView(sheet.getRoot());

        sheet.tvTitulo.setText(exame.exame.nome);
        sheet.tvResumo.setText(resultado.resumo);
        sheet.tvValores.setText(resultado.valores);
        adicionarInfo(sheet, R.string.rotulo_observacao, resultado.observacao);
        adicionarInfo(sheet, R.string.rotulo_realizacao, DateUtils.isoParaBr(exame.exame.dataRealizacao));
        adicionarInfo(sheet, R.string.rotulo_liberacao, DateUtils.isoParaBr(resultado.dataLiberacao));
        adicionarInfo(sheet, R.string.rotulo_profissional, exame.profissionalNome);
        adicionarInfo(sheet, R.string.rotulo_unidade, exame.unidadeNome);

        sheet.btnAbrirPdf.setOnClickListener(v -> {
            painel.dismiss();
            aoAbrirPdf(exame);
        });
        painel.show();
    }

    private void adicionarInfo(BottomSheetResultadoBinding sheet, int rotulo, String valor) {
        if (valor == null || valor.trim().isEmpty()) return;
        ItemInfoLinhaBinding linha = ItemInfoLinhaBinding.inflate(getLayoutInflater(), sheet.containerInfo, false);
        linha.tvRotulo.setText(rotulo);
        linha.tvValor.setText(valor);
        sheet.containerInfo.addView(linha.getRoot());
    }

    // ---------------------------------------------------------------- PDF

    @Override
    public void aoAbrirPdf(ExameDetalhado exame) {
        if (gerandoPdf) return;
        gerandoPdf = true;
        Snackbar aviso = Snackbar.make(binding.getRoot(), R.string.gerando_pdf, Snackbar.LENGTH_INDEFINITE);
        aviso.show();
        repositorio.gerarPdf(sessao.getPacienteId(), exame, new RepositoryCallback<File>() {
            @Override
            public void onSuccess(File arquivo) {
                gerandoPdf = false;
                aviso.dismiss();
                if (isFinishing()) return;
                Intent intent = new Intent(ExamesActivity.this, VisualizarPdfActivity.class);
                intent.putExtra(VisualizarPdfActivity.EXTRA_CAMINHO, arquivo.getAbsolutePath());
                intent.putExtra(VisualizarPdfActivity.EXTRA_TITULO, exame.exame.nome);
                startActivity(intent);
            }

            @Override
            public void onError(String mensagem) {
                gerandoPdf = false;
                aviso.dismiss();
                if (!isFinishing()) Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
