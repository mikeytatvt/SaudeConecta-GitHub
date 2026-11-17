package br.edu.unicid.saudeconecta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.adapters.NotificacaoAdapter;
import br.edu.unicid.saudeconecta.databinding.ActivityNotificacoesBinding;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;
import br.edu.unicid.saudeconecta.repositories.NotificacaoRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/**
 * TELA 8 – Notificações: confirmações, lembretes, alterações, resultados e avisos.
 * Tocar abre a tela relacionada; deslizar exclui (com opção de desfazer).
 */
public class NotificacoesActivity extends AppCompatActivity implements NotificacaoAdapter.Listener {

    private ActivityNotificacoesBinding binding;
    private SessionManager sessao;
    private NotificacaoRepository repositorio;
    private NotificacaoAdapter adapter;
    private final List<Notificacao> todas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        if (!sessao.isLogado()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityNotificacoesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.inflateMenu(R.menu.menu_notificacoes);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.acao_marcar_todas) {
                marcarTodas();
                return true;
            }
            return false;
        });

        repositorio = new NotificacaoRepository(this);
        adapter = new NotificacaoAdapter(this);
        binding.rvNotificacoes.setLayoutManager(new LinearLayoutManager(this));
        binding.rvNotificacoes.setAdapter(adapter);
        configurarDeslizarParaExcluir();

        binding.chipTodas.setChecked(true);
        binding.grupoFiltros.setOnCheckedStateChangeListener((grupo, ids) -> exibir());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) carregar();
    }

    private void carregar() {
        repositorio.listar(sessao.getUsuarioId(), new RepositoryCallback<List<Notificacao>>() {
            @Override
            public void onSuccess(List<Notificacao> lista) {
                if (isFinishing()) return;
                todas.clear();
                todas.addAll(lista);
                exibir();
            }

            @Override
            public void onError(String mensagem) {
                if (!isFinishing()) Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void exibir() {
        List<Notificacao> naoLidas = new ArrayList<>();
        for (Notificacao n : todas) if (!n.lida) naoLidas.add(n);
        binding.chipTodas.setText(getString(R.string.filtro_todas_qtd, todas.size()));
        binding.chipNaoLidas.setText(getString(R.string.filtro_nao_lidas, naoLidas.size()));

        boolean soNaoLidas = binding.chipNaoLidas.isChecked();
        List<Notificacao> lista = soNaoLidas ? naoLidas : todas;
        adapter.atualizar(lista);

        boolean vazia = lista.isEmpty();
        binding.rvNotificacoes.setVisibility(vazia ? View.GONE : View.VISIBLE);
        binding.layoutVazio.setVisibility(vazia ? View.VISIBLE : View.GONE);
        binding.tvVazio.setText(soNaoLidas ? R.string.vazio_nao_lidas : R.string.vazio_notificacoes);
    }

    // ---------------------------------------------------------------- Ações

    @Override
    public void aoAbrir(Notificacao n) {
        if (!n.lida) {
            n.lida = true;
            repositorio.marcarComoLida(n.id, sessao.getUsuarioId());
            exibir();
        }
        // Leva o paciente para a tela relacionada à notificação
        if (sessao.isGestor()) return;
        Class<?> destino = null;
        if (TipoNotificacao.CONFIRMACAO.equals(n.tipo) || TipoNotificacao.LEMBRETE.equals(n.tipo)
                || TipoNotificacao.ALTERACAO.equals(n.tipo)) {
            destino = MeusAtendimentosActivity.class;
        } else if (TipoNotificacao.RESULTADO.equals(n.tipo)) {
            destino = ExamesActivity.class;
        }
        if (destino != null) startActivity(new Intent(this, destino));
    }

    private void marcarTodas() {
        repositorio.marcarTodasComoLidas(sessao.getUsuarioId(), new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                if (isFinishing()) return;
                for (Notificacao n : todas) n.lida = true;
                exibir();
                Snackbar.make(binding.getRoot(), R.string.todas_marcadas, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String mensagem) { }
        });
    }

    private void configurarDeslizarParaExcluir() {
        ItemTouchHelper.SimpleCallback gesto = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder origem,
                                  @NonNull RecyclerView.ViewHolder destino) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direcao) {
                int posicao = holder.getBindingAdapterPosition();
                if (posicao == RecyclerView.NO_POSITION) return;
                excluir(adapter.itemNa(posicao));
            }
        };
        new ItemTouchHelper(gesto).attachToRecyclerView(binding.rvNotificacoes);
    }

    private void excluir(Notificacao n) {
        todas.remove(n);
        exibir();
        repositorio.excluir(n, sessao.getUsuarioId(), new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                if (isFinishing()) return;
                Snackbar.make(binding.getRoot(), R.string.notificacao_excluida, Snackbar.LENGTH_LONG)
                        .setAction(R.string.desfazer, v -> desfazerExclusao(n))
                        .setActionTextColor(getColor(R.color.verde_claro))
                        .show();
            }

            @Override
            public void onError(String mensagem) { }
        });
    }

    private void desfazerExclusao(Notificacao n) {
        repositorio.restaurar(n, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                if (!isFinishing()) carregar();
            }

            @Override
            public void onError(String mensagem) { }
        });
    }
}
