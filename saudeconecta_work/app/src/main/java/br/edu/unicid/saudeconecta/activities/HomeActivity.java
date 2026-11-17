package br.edu.unicid.saudeconecta.activities;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ActivityHomeBinding;
import br.edu.unicid.saudeconecta.databinding.NavHeaderPacienteBinding;
import br.edu.unicid.saudeconecta.models.ConsultaDetalhada;
import br.edu.unicid.saudeconecta.repositories.ConsultaRepository;
import br.edu.unicid.saudeconecta.repositories.NotificacaoRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/** TELA 2 – Início do paciente (Figura 1 do banner). */
public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private SessionManager sessao;
    private ConsultaRepository consultaRepositorio;
    private NotificacaoRepository notificacaoRepositorio;
    private ConsultaDetalhada proximaConsulta;

    /** Android 13+: pede autorização para mostrar notificações na barra do celular. */
    private final ActivityResultLauncher<String> pedirPermissaoNotificacao = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedida -> {
                if (concedida && binding != null) carregarDados(); // gera os lembretes já com permissão
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        // Controle de acesso: somente paciente logado
        if (!sessao.isLogado() || sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.conteudo, binding.toolbar);

        consultaRepositorio = new ConsultaRepository(this);
        notificacaoRepositorio = new NotificacaoRepository(this);

        binding.tvSaudacao.setText(getString(R.string.ola_nome, UiUtils.primeiroNome(sessao.getNome())));
        configurarMenuLateral();
        configurarAcoes();
        solicitarPermissaoNotificacoes();
    }

    private void solicitarPermissaoNotificacoes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pedirPermissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding == null) return;
        // Recarrega sempre que volta para a tela (ex.: depois de agendar ou cancelar)
        binding.navView.setCheckedItem(R.id.nav_inicio);
        carregarDados();
    }

    // ---------------------------------------------------------------- Menu lateral

    private void configurarMenuLateral() {
        binding.toolbar.setNavigationOnClickListener(v -> binding.drawerLayout.openDrawer(GravityCompat.START));

        NavHeaderPacienteBinding cabecalho = NavHeaderPacienteBinding.bind(binding.navView.getHeaderView(0));
        cabecalho.tvNavNome.setText(sessao.getNome());

        binding.navView.setNavigationItemSelectedListener(item -> {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            tratarItemMenu(item.getItemId());
            return item.getItemId() == R.id.nav_inicio;
        });

        // Botão "voltar" fecha o menu quando ele está aberto
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
        if (id == R.id.nav_agendar) abrirAgendamento();
        else if (id == R.id.nav_atendimentos) abrirMeusAtendimentos();
        else if (id == R.id.nav_exames) abrirExames();
        else if (id == R.id.nav_medicamentos) abrirMedicamentos();
        else if (id == R.id.nav_unidades) abrirUnidades();
        else if (id == R.id.nav_notificacoes) abrirNotificacoes();
        else if (id == R.id.nav_privacidade) mostrarPrivacidade();
        else if (id == R.id.nav_sair) confirmarSaida();
    }

    // ---------------------------------------------------------------- Botões e atalhos

    private void configurarAcoes() {
        binding.btnNotificacoes.setOnClickListener(v -> abrirNotificacoes());
        binding.btnVerDetalhes.setOnClickListener(v -> mostrarDetalhesConsulta());
        binding.btnAgendarAgora.setOnClickListener(v -> abrirAgendamento());

        binding.cardAgendar.setOnClickListener(v -> abrirAgendamento());
        binding.cardAtendimentos.setOnClickListener(v -> abrirMeusAtendimentos());
        binding.cardExames.setOnClickListener(v -> abrirExames());
        binding.cardMedicamentos.setOnClickListener(v -> abrirMedicamentos());
        binding.cardUnidades.setOnClickListener(v -> abrirUnidades());
        binding.cardNotificacoes.setOnClickListener(v -> abrirNotificacoes());
    }

    // Cada etapa seguinte troca o aviso pela abertura da tela correspondente.
    private void abrirAgendamento() {
        startActivity(new Intent(this, AgendamentoActivity.class));
    }
    private void abrirMeusAtendimentos() {
        startActivity(new Intent(this, MeusAtendimentosActivity.class));
    }
    private void abrirExames() {
        startActivity(new Intent(this, ExamesActivity.class));
    }
    private void abrirMedicamentos() {
        startActivity(new Intent(this, MedicamentosActivity.class));
    }
    private void abrirUnidades() {
        startActivity(new Intent(this, UnidadesActivity.class));
    }
    private void abrirNotificacoes() {
        startActivity(new Intent(this, NotificacoesActivity.class));
    }

    private void avisarEtapa(@StringRes int modulo, int etapa) {
        Snackbar.make(binding.conteudo, getString(R.string.modulo_em_breve, getString(modulo), etapa),
                Snackbar.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- Dados

    private void carregarDados() {
        consultaRepositorio.buscarProximaConsulta(sessao.getPacienteId(), new RepositoryCallback<ConsultaDetalhada>() {
            @Override
            public void onSuccess(ConsultaDetalhada consulta) {
                if (!isFinishing()) exibirProximaConsulta(consulta);
            }

            @Override
            public void onError(String mensagem) {
                if (isFinishing()) return;
                exibirProximaConsulta(null);
                Snackbar.make(binding.conteudo, mensagem, Snackbar.LENGTH_LONG).show();
            }
        });

        // Sistema verifica consultas próximas e gera lembretes; depois atualiza o contador do sino
        notificacaoRepositorio.gerarLembretesProximos(sessao.getUsuarioId(), sessao.getPacienteId(),
                new RepositoryCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer criados) {
                        if (!isFinishing()) atualizarContador();
                    }

                    @Override
                    public void onError(String mensagem) {
                        if (!isFinishing()) atualizarContador();
                    }
                });
    }

    private void atualizarContador() {
        notificacaoRepositorio.contarNaoLidas(sessao.getUsuarioId(), new RepositoryCallback<Integer>() {
            @Override
            public void onSuccess(Integer total) {
                if (!isFinishing()) atualizarContadorNotificacoes(total);
            }

            @Override
            public void onError(String mensagem) {
                // contador é opcional: em caso de erro apenas não é exibido
            }
        });
    }

    private void exibirProximaConsulta(ConsultaDetalhada c) {
        proximaConsulta = c;
        binding.tvCarregando.setVisibility(View.GONE);
        if (c == null) {
            binding.layoutConsulta.setVisibility(View.GONE);
            binding.layoutSemConsulta.setVisibility(View.VISIBLE);
            return;
        }
        binding.layoutSemConsulta.setVisibility(View.GONE);
        binding.layoutConsulta.setVisibility(View.VISIBLE);
        binding.tvConsultaTitulo.setText(getString(R.string.consulta_de, c.especialidadeNome));
        binding.tvConsultaDataHora.setText(getString(R.string.data_hora,
                DateUtils.isoParaBr(c.consulta.data), c.consulta.hora));
        binding.tvConsultaUnidade.setText(c.unidadeNome);
        binding.tvConsultaProfissional.setText(c.profissionalNome);
    }

    private void atualizarContadorNotificacoes(int naoLidas) {
        if (naoLidas <= 0) {
            binding.tvBadge.setVisibility(View.GONE);
            binding.btnNotificacoes.setContentDescription(getString(R.string.notificacoes));
        } else {
            binding.tvBadge.setVisibility(View.VISIBLE);
            binding.tvBadge.setText(naoLidas > 9 ? "9+" : String.valueOf(naoLidas));
            binding.btnNotificacoes.setContentDescription(getString(R.string.notificacoes_nao_lidas, naoLidas));
        }
    }

    // ---------------------------------------------------------------- Diálogos

    private void mostrarDetalhesConsulta() {
        ConsultaDetalhada c = proximaConsulta;
        if (c == null) return;
        String endereco = c.unidadeEndereco + (c.unidadeBairro != null ? " – " + c.unidadeBairro : "");
        String mensagem = getString(R.string.detalhes_consulta_msg,
                c.profissionalNome,
                DateUtils.isoParaBrComDiaSemana(c.consulta.data),
                c.consulta.hora,
                c.unidadeNome,
                endereco,
                c.unidadeTelefone);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.consulta_de, c.especialidadeNome))
                .setMessage(mensagem)
                .setPositiveButton(R.string.fechar, null)
                .setNeutralButton(R.string.como_chegar, (d, w) -> abrirMapa(endereco + ", São Paulo - SP"))
                .show();
    }

    private void abrirMapa(String endereco) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(endereco)));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.conteudo, R.string.erro_mapa, Snackbar.LENGTH_LONG).show();
        }
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
