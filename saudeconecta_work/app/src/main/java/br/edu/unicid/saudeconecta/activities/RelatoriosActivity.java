package br.edu.unicid.saudeconecta.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.text.NumberFormat;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ActivityRelatoriosBinding;
import br.edu.unicid.saudeconecta.databinding.ItemInfoLinhaBinding;
import br.edu.unicid.saudeconecta.repositories.DashboardRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.RelatorioPdfGenerator;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/** GESTOR – Relatórios: resumo do período e geração do relatório em PDF. */
public class RelatoriosActivity extends AppCompatActivity {

    private ActivityRelatoriosBinding binding;
    private SessionManager sessao;
    private DashboardRepository repositorio;
    private DashboardRepository.Indicadores indicadores;
    private final NumberFormat inteiro = NumberFormat.getIntegerInstance(DateUtils.PT_BR);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        if (!sessao.isLogado() || !sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityRelatoriosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        repositorio = new DashboardRepository(this);
        binding.chip30dias.setChecked(true);
        binding.grupoPeriodo.setOnCheckedStateChangeListener((g, ids) -> carregar());
        binding.btnGerarPdf.setOnClickListener(v -> gerarPdf());
        carregar();
    }

    private int dias() {
        if (binding.chip7dias.isChecked()) return 7;
        if (binding.chip90dias.isChecked()) return 90;
        return 30;
    }

    private void carregar() {
        binding.btnGerarPdf.setEnabled(false);
        repositorio.carregar(dias(), new RepositoryCallback<DashboardRepository.Indicadores>() {
            @Override
            public void onSuccess(DashboardRepository.Indicadores r) {
                if (isFinishing()) return;
                indicadores = r;
                binding.tvPeriodo.setText(getString(R.string.periodo_de_a,
                        DateUtils.formatarBr(r.inicio), DateUtils.formatarBr(r.fim)));
                binding.containerResumo.removeAllViews();
                linha(R.string.kpi_atendimentos, inteiro.format(r.atendimentos));
                linha(R.string.kpi_consultas, inteiro.format(r.consultasMedicas));
                linha(R.string.kpi_exames, inteiro.format(r.exames));
                linha(R.string.kpi_pacientes, inteiro.format(r.pacientesAtendidos));
                linha(R.string.ind_tempo_medio, String.format(DateUtils.PT_BR, "%.0f", r.tempoMedioEspera));
                linha(R.string.ind_reducao, r.reducaoEspera == null ? "—"
                        : String.format(DateUtils.PT_BR, "%.0f%%", r.reducaoEspera));
                binding.btnGerarPdf.setEnabled(true);
            }

            @Override
            public void onError(String m) {
                if (!isFinishing()) Snackbar.make(binding.getRoot(), m, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void linha(@StringRes int rotulo, String valor) {
        ItemInfoLinhaBinding l = ItemInfoLinhaBinding.inflate(getLayoutInflater(), binding.containerResumo, false);
        l.tvRotulo.setText(rotulo);
        l.tvValor.setText(valor);
        binding.containerResumo.addView(l.getRoot());
    }

    private void gerarPdf() {
        if (indicadores == null) return;
        binding.btnGerarPdf.setEnabled(false);
        binding.btnGerarPdf.setText(R.string.gerando_pdf);
        DashboardRepository.Indicadores dados = indicadores;
        String gestor = sessao.getNome();
        AppExecutors.disco().execute(() -> {
            File arquivo = null;
            try {
                arquivo = RelatorioPdfGenerator.gerar(getApplicationContext(), dados, gestor);
            } catch (Exception ignorado) {
                // tratado abaixo (arquivo == null)
            }
            File resultado = arquivo;
            AppExecutors.principal(() -> {
                if (isFinishing()) return;
                binding.btnGerarPdf.setEnabled(true);
                binding.btnGerarPdf.setText(R.string.gerar_relatorio_pdf);
                if (resultado == null) {
                    Snackbar.make(binding.getRoot(), R.string.erro_pdf, Snackbar.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(this, VisualizarPdfActivity.class);
                intent.putExtra(VisualizarPdfActivity.EXTRA_CAMINHO, resultado.getAbsolutePath());
                intent.putExtra(VisualizarPdfActivity.EXTRA_TITULO, getString(R.string.titulo_relatorio_pdf));
                startActivity(intent);
            });
        });
    }
}
