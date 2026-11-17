package br.edu.unicid.saudeconecta.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.adapters.MedicamentoAdapter;
import br.edu.unicid.saudeconecta.databinding.ActivityMedicamentosBinding;
import br.edu.unicid.saudeconecta.databinding.BottomSheetMedicamentoBinding;
import br.edu.unicid.saudeconecta.databinding.ItemInfoLinhaBinding;
import br.edu.unicid.saudeconecta.models.MedicamentoDetalhado;
import br.edu.unicid.saudeconecta.repositories.MedicamentoRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/** TELA 6 – Medicamentos prescritos ao paciente. */
public class MedicamentosActivity extends AppCompatActivity implements MedicamentoAdapter.Listener {

    private ActivityMedicamentosBinding binding;
    private SessionManager sessao;
    private MedicamentoRepository repositorio;
    private MedicamentoAdapter adapter;
    private final List<String> farmacias = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);
        if (!sessao.isLogado() || sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityMedicamentosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        repositorio = new MedicamentoRepository(this);
        adapter = new MedicamentoAdapter(this);
        binding.rvMedicamentos.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMedicamentos.setAdapter(adapter);
        carregar();
    }

    private void carregar() {
        repositorio.carregar(sessao.getPacienteId(), new RepositoryCallback<MedicamentoRepository.Dados>() {
            @Override
            public void onSuccess(MedicamentoRepository.Dados dados) {
                if (isFinishing()) return;
                farmacias.clear();
                farmacias.addAll(dados.farmacias);
                adapter.atualizar(dados.medicamentos, dados.farmacias);
                boolean vazio = dados.medicamentos.isEmpty();
                binding.rvMedicamentos.setVisibility(vazio ? View.GONE : View.VISIBLE);
                binding.layoutVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String mensagem) {
                if (!isFinishing()) Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void aoSelecionar(MedicamentoDetalhado m) {
        BottomSheetDialog painel = new BottomSheetDialog(this);
        BottomSheetMedicamentoBinding sheet = BottomSheetMedicamentoBinding.inflate(getLayoutInflater());
        painel.setContentView(sheet.getRoot());

        sheet.tvTitulo.setText(m.medicamento.nome);
        adicionarInfo(sheet, R.string.rotulo_dosagem, m.medicamento.dosagem);
        adicionarInfo(sheet, R.string.rotulo_orientacao, m.medicamento.orientacao);
        adicionarInfo(sheet, R.string.rotulo_quantidade, m.medicamento.quantidade);
        adicionarInfo(sheet, R.string.rotulo_prescricao, DateUtils.isoParaBr(m.medicamento.dataPrescricao));
        adicionarInfo(sheet, R.string.rotulo_medico, MedicamentoAdapter.nomeMedico(this, m));
        if (!farmacias.isEmpty()) {
            adicionarInfo(sheet, R.string.rotulo_retirada, TextUtils.join("\n", farmacias));
        }
        painel.show();
    }

    private void adicionarInfo(BottomSheetMedicamentoBinding sheet, int rotulo, String valor) {
        if (valor == null || valor.trim().isEmpty()) return;
        ItemInfoLinhaBinding linha = ItemInfoLinhaBinding.inflate(getLayoutInflater(), sheet.containerInfo, false);
        linha.tvRotulo.setText(rotulo);
        linha.tvValor.setText(valor);
        sheet.containerInfo.addView(linha.getRoot());
    }
}
