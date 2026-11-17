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
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.adapters.UnidadeAdapter;
import br.edu.unicid.saudeconecta.databinding.ActivityUnidadesBinding;
import br.edu.unicid.saudeconecta.databinding.BottomSheetUnidadeBinding;
import br.edu.unicid.saudeconecta.databinding.ItemInfoLinhaBinding;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.repositories.UnidadeRepository;
import br.edu.unicid.saudeconecta.utils.HorarioUtils;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.SimpleTextWatcher;
import br.edu.unicid.saudeconecta.utils.UiUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/** TELA 7 – Unidades de saúde: busca, filtro por serviço, ligar e como chegar. */
public class UnidadesActivity extends AppCompatActivity implements UnidadeAdapter.Listener {

    private ActivityUnidadesBinding binding;
    private SessionManager sessao;
    private UnidadeRepository repositorio;
    private UnidadeAdapter adapter;
    private final List<UnidadeSaude> todas = new ArrayList<>();
    private long unidadeReferenciaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        if (!sessao.isLogado()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityUnidadesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        repositorio = new UnidadeRepository(this);
        adapter = new UnidadeAdapter(this);
        binding.rvUnidades.setLayoutManager(new LinearLayoutManager(this));
        binding.rvUnidades.setAdapter(adapter);

        binding.chipTodas.setChecked(true);
        binding.grupoServicos.setOnCheckedStateChangeListener((grupo, ids) -> aplicarFiltro());
        binding.etBusca.addTextChangedListener(new SimpleTextWatcher(this::aplicarFiltro));

        carregar();
    }

    private void carregar() {
        repositorio.carregar(sessao.isGestor() ? 0 : sessao.getPacienteId(),
                new RepositoryCallback<UnidadeRepository.Dados>() {
                    @Override
                    public void onSuccess(UnidadeRepository.Dados dados) {
                        if (isFinishing()) return;
                        todas.clear();
                        todas.addAll(dados.unidades);
                        unidadeReferenciaId = dados.unidadeReferenciaId;
                        // A unidade de referência do paciente aparece primeiro
                        Collections.sort(todas, (a, b) -> {
                            if (a.id == unidadeReferenciaId) return -1;
                            if (b.id == unidadeReferenciaId) return 1;
                            return a.nome.compareTo(b.nome);
                        });
                        aplicarFiltro();
                    }

                    @Override
                    public void onError(String mensagem) {
                        if (!isFinishing()) Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    /** Combina o texto digitado e o serviço escolhido (busca sem acentos). */
    private void aplicarFiltro() {
        String busca = HorarioUtils.normalizar(UiUtils.texto(binding.etBusca));
        String servico = "";
        int idChip = binding.grupoServicos.getCheckedChipId();
        if (idChip != View.NO_ID && idChip != R.id.chipTodas) {
            Chip chip = findViewById(idChip);
            servico = HorarioUtils.normalizar(chip.getText().toString());
        }

        List<UnidadeSaude> filtradas = new ArrayList<>();
        for (UnidadeSaude u : todas) {
            String alvo = HorarioUtils.normalizar(u.nome + " " + u.bairro + " " + u.endereco + " " + u.servicos);
            boolean bateBusca = busca.isEmpty() || alvo.contains(busca);
            boolean bateServico = servico.isEmpty() || HorarioUtils.normalizar(u.servicos).contains(servico);
            if (bateBusca && bateServico) filtradas.add(u);
        }

        adapter.atualizar(filtradas, unidadeReferenciaId);
        boolean vazia = filtradas.isEmpty();
        binding.rvUnidades.setVisibility(vazia ? View.GONE : View.VISIBLE);
        binding.layoutVazio.setVisibility(vazia ? View.VISIBLE : View.GONE);
    }

    // ---------------------------------------------------------------- Ações

    @Override
    public void aoSelecionar(UnidadeSaude u) {
        BottomSheetDialog painel = new BottomSheetDialog(this);
        BottomSheetUnidadeBinding sheet = BottomSheetUnidadeBinding.inflate(getLayoutInflater());
        painel.setContentView(sheet.getRoot());

        sheet.tvTitulo.setText(u.nome);
        adicionarInfo(sheet, R.string.rotulo_endereco, u.endereco + (u.bairro != null ? " – " + u.bairro : ""));
        adicionarInfo(sheet, R.string.rotulo_telefone, u.telefone);
        adicionarInfo(sheet, R.string.rotulo_horario_funcionamento, u.horarioFuncionamento);
        adicionarInfo(sheet, R.string.rotulo_servicos, TextUtils.join("\n", UnidadeAdapter.servicos(u)));

        repositorio.listarProfissionais(u.id, new RepositoryCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> profissionais) {
                if (!profissionais.isEmpty() && painel.isShowing()) {
                    adicionarInfo(sheet, R.string.rotulo_profissionais, TextUtils.join("\n", profissionais));
                }
            }

            @Override
            public void onError(String mensagem) { }
        });

        sheet.btnLigar.setOnClickListener(v -> aoLigar(u));
        sheet.btnComoChegar.setOnClickListener(v -> aoComoChegar(u));
        painel.show();
    }

    private void adicionarInfo(BottomSheetUnidadeBinding sheet, int rotulo, String valor) {
        if (valor == null || valor.trim().isEmpty()) return;
        ItemInfoLinhaBinding linha = ItemInfoLinhaBinding.inflate(getLayoutInflater(), sheet.containerInfo, false);
        linha.tvRotulo.setText(rotulo);
        linha.tvValor.setText(valor);
        sheet.containerInfo.addView(linha.getRoot());
    }

    /** Abre o discador com o número preenchido (não liga sozinho e não exige permissão). */
    @Override
    public void aoLigar(UnidadeSaude u) {
        String numero = Validators.somenteDigitos(u.telefone);
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + numero)));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), u.telefone, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void aoComoChegar(UnidadeSaude u) {
        String endereco = u.endereco + (u.bairro != null ? ", " + u.bairro : "") + ", São Paulo - SP";
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(endereco))));
        } catch (ActivityNotFoundException e) {
            Snackbar.make(binding.getRoot(), R.string.erro_mapa, Snackbar.LENGTH_LONG).show();
        }
    }
}
