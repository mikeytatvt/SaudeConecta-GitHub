package br.edu.unicid.saudeconecta.activities;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.adapters.ItemAdminAdapter;
import br.edu.unicid.saudeconecta.databinding.ActivityAdminListaBinding;
import br.edu.unicid.saudeconecta.models.ItemAdmin;
import br.edu.unicid.saudeconecta.repositories.AdminRepository;
import br.edu.unicid.saudeconecta.utils.HorarioUtils;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.SimpleTextWatcher;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/**
 * Base das listas administrativas: controle de acesso (somente GESTOR), barra verde,
 * busca, filtros, contador, lista vazia e botão "novo".
 */
public abstract class AdminListaBaseActivity extends AppCompatActivity implements ItemAdminAdapter.Listener {

    protected ActivityAdminListaBinding binding;
    protected SessionManager sessao;
    protected AdminRepository repositorio;
    private ItemAdminAdapter adapter;
    private final List<ItemAdmin> todos = new ArrayList<>();

    @StringRes
    protected abstract int titulo();

    @StringRes
    protected abstract int dicaBusca();

    /** Carrega os dados e chama exibir(...). */
    protected abstract void carregar();

    /** Texto do botão "novo"; 0 = sem botão. */
    @StringRes
    protected int textoBotaoNovo() {
        return 0;
    }

    protected void aoNovo() { }

    /** Configurações extras da tela (ex.: filtros). */
    protected void configurar() { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        if (!sessao.isLogado() || !sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityAdminListaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setTitle(titulo());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        repositorio = new AdminRepository(this);
        adapter = new ItemAdminAdapter(this);
        binding.rvItens.setLayoutManager(new LinearLayoutManager(this));
        binding.rvItens.setAdapter(adapter);

        binding.tilBusca.setHint(dicaBusca());
        binding.etBusca.addTextChangedListener(new SimpleTextWatcher(this::aplicarBusca));

        if (textoBotaoNovo() != 0) {
            binding.fabNovo.setVisibility(View.VISIBLE);
            binding.fabNovo.setText(textoBotaoNovo());
            binding.fabNovo.setOnClickListener(v -> aoNovo());
        }
        configurar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) carregar(); // atualiza ao voltar de um formulário
    }

    protected void exibir(List<ItemAdmin> itens) {
        todos.clear();
        todos.addAll(itens);
        aplicarBusca();
    }

    private void aplicarBusca() {
        String busca = HorarioUtils.normalizar(UiUtils.texto(binding.etBusca));
        List<ItemAdmin> filtrados = new ArrayList<>();
        for (ItemAdmin i : todos) {
            String alvo = HorarioUtils.normalizar(i.titulo + " " + i.subtitulo + " " + i.detalhe);
            if (busca.isEmpty() || alvo.contains(busca)) filtrados.add(i);
        }
        adapter.atualizar(filtrados);
        binding.tvContador.setText(getString(R.string.contador_registros, filtrados.size()));
        boolean vazia = filtrados.isEmpty();
        binding.rvItens.setVisibility(vazia ? View.GONE : View.VISIBLE);
        binding.layoutVazio.setVisibility(vazia ? View.VISIBLE : View.GONE);
    }

    /** Cria um chip de filtro dentro do grupo indicado. */
    protected Chip adicionarChip(ChipGroup grupo, @StringRes int texto) {
        grupo.setVisibility(View.VISIBLE);
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filtro, grupo, false);
        chip.setId(View.generateViewId());
        chip.setText(texto);
        grupo.addView(chip);
        return chip;
    }

    protected void mensagem(String texto) {
        if (!isFinishing()) Snackbar.make(binding.getRoot(), texto, Snackbar.LENGTH_LONG).show();
    }

    protected void mensagem(@StringRes int texto) {
        mensagem(getString(texto));
    }

    protected void alerta(String texto) {
        if (isFinishing()) return;
        new MaterialAlertDialogBuilder(this)
                .setMessage(texto)
                .setPositiveButton(R.string.fechar, null)
                .show();
    }

    @Override
    public void aoTocar(ItemAdmin item) { }

    @Override
    public void aoAcao1(ItemAdmin item) { }

    @Override
    public void aoAcao2(ItemAdmin item) { }
}
