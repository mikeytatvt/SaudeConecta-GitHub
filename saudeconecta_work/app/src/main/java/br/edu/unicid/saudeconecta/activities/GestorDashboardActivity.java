package br.edu.unicid.saudeconecta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ActivityGestorDashboardBinding;
import br.edu.unicid.saudeconecta.databinding.NavHeaderPacienteBinding;
import br.edu.unicid.saudeconecta.repositories.DashboardRepository;
import br.edu.unicid.saudeconecta.repositories.NotificacaoRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/** DASHBOARD DO GESTOR (Figura 3 do banner) – acesso exclusivo ao perfil GESTOR. */
public class GestorDashboardActivity extends AppCompatActivity {

    private ActivityGestorDashboardBinding binding;
    private SessionManager sessao;
    private DashboardRepository repositorio;
    private NotificacaoRepository notificacaoRepositorio;
    private final NumberFormat inteiro = NumberFormat.getIntegerInstance(DateUtils.PT_BR);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        // Controle de acesso por perfil: paciente não entra na área administrativa
        if (!sessao.isLogado() || !sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityGestorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.conteudo, binding.toolbar);

        repositorio = new DashboardRepository(this);
        notificacaoRepositorio = new NotificacaoRepository(this);

        binding.tvSaudacao.setText(getString(R.string.ola_gestor, UiUtils.primeiroNome(sessao.getNome())));
        binding.chip30dias.setChecked(true);
        binding.grupoPeriodo.setOnCheckedStateChangeListener((grupo, ids) -> carregarIndicadores());
        binding.btnNotificacoes.setOnClickListener(v -> startActivity(new Intent(this, NotificacoesActivity.class)));
        configurarMenuLateral();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) return;
        binding.navView.setCheckedItem(R.id.nav_dashboard);
        carregarIndicadores(); // sempre atualizado com os agendamentos mais recentes
        atualizarContador();
    }

    // ---------------------------------------------------------------- Indicadores

    private int diasSelecionados() {
        if (binding.chip7dias.isChecked()) return 7;
        if (binding.chip90dias.isChecked()) return 90;
        return 30;
    }

    private void carregarIndicadores() {
        repositorio.carregar(diasSelecionados(), new RepositoryCallback<DashboardRepository.Indicadores>() {
            @Override
            public void onSuccess(DashboardRepository.Indicadores r) {
                if (!isFinishing()) exibir(r);
            }

            @Override
            public void onError(String mensagem) {
                if (!isFinishing()) Snackbar.make(binding.conteudo, mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void exibir(DashboardRepository.Indicadores r) {
        binding.tvPeriodo.setText(getString(R.string.periodo_de_a,
                DateUtils.formatarBr(r.inicio), DateUtils.formatarBr(r.fim)));

        binding.tvKpiAtendimentos.setText(inteiro.format(r.atendimentos));
        binding.tvKpiConsultas.setText(inteiro.format(r.consultasMedicas));
        binding.tvKpiExames.setText(inteiro.format(r.exames));
        binding.tvKpiPacientes.setText(inteiro.format(r.pacientesAtendidos));

        binding.graficoUnidades.setItens(r.porUnidade);
        binding.graficoEspera.setPontos(r.esperaPorDia);
        binding.graficoEspecialidades.setItens(r.porEspecialidade);

        binding.tvTempoMedio.setText(r.tempoMedioEspera > 0
                ? String.format(DateUtils.PT_BR, "%.0f", r.tempoMedioEspera) : "—");
        binding.tvReducao.setText(r.reducaoEspera == null ? "—" : percentual(r.reducaoEspera));
        binding.tvCancelamento.setText(percentual(r.taxaCancelamento));
        binding.tvOcupacao.setText(percentual(r.ocupacaoAgenda));
        binding.tvAgendadasProximas.setText(inteiro.format(r.agendadasProximas));
        binding.tvPacientesCadastrados.setText(inteiro.format(r.pacientesCadastrados));
        binding.tvUnidadesAtivas.setText(inteiro.format(r.unidadesAtivas));
        binding.tvProfissionaisAtivos.setText(inteiro.format(r.profissionaisAtivos));

        binding.tvAtualizado.setText(getString(R.string.atualizado_em,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))));
    }

    private static String percentual(double valor) {
        return String.format(DateUtils.PT_BR, "%.0f%%", valor);
    }

    private void atualizarContador() {
        notificacaoRepositorio.contarNaoLidas(sessao.getUsuarioId(), new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer total) {
                if (isFinishing()) return;
                binding.tvBadge.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
                binding.tvBadge.setText(total > 9 ? "9+" : String.format(Locale.ROOT, "%d", total));
            }

            @Override
            public void onError(String mensagem) { }
        });
    }

    // ---------------------------------------------------------------- Menu lateral

    private void configurarMenuLateral() {
        binding.toolbar.setNavigationOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));

        NavHeaderPacienteBinding cabecalho = NavHeaderPacienteBinding.bind(binding.navView.getHeaderView(0));
        cabecalho.tvNavNome.setText(sessao.getNome());
        cabecalho.tvNavPerfil.setText(R.string.perfil_gestor);

        binding.navView.setNavigationItemSelectedListener(item -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            tratarItemMenu(item.getItemId());
            return item.getItemId() == R.id.nav_dashboard;
        });

        OnBackPressedCallback fecharMenu = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                binding.drawerLayout.closeDrawer(GravityCompat.START);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, fecharMenu);
        binding.drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                fecharMenu.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                fecharMenu.setEnabled(false);
            }
        });
    }

    private void tratarItemMenu(int id) {
        if (id == R.id.nav_agendamentos) startActivity(new Intent(this, AdminAgendamentosActivity.class));
        else if (id == R.id.nav_pacientes) startActivity(new Intent(this, AdminPacientesActivity.class));
        else if (id == R.id.nav_unidades) startActivity(new Intent(this, AdminUnidadesActivity.class));
        else if (id == R.id.nav_profissionais) startActivity(new Intent(this, AdminProfissionaisActivity.class));
        else if (id == R.id.nav_relatorios) startActivity(new Intent(this, RelatoriosActivity.class));
        else if (id == R.id.nav_notificacoes_gestor) startActivity(new Intent(this, NotificacoesActivity.class));
        else if (id == R.id.nav_privacidade_gestor) mostrarPrivacidade();
        else if (id == R.id.nav_sair_gestor) confirmarSaida();
    }

    private void mostrarPrivacidade() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_termos)
                .setMessage(R.string.texto_termos_lgpd)
                .setPositiveButton(R.string.fechar, null)
                .show();
    }

    private void confirmarSaida() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.sair_confirmacao)
                .setPositiveButton(R.string.sair, (d, w) -> UiUtils.sairParaLogin(this))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
