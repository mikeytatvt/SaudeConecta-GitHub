package br.edu.unicid.saudeconecta.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ActivityFormUnidadeBinding;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.repositories.AdminRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.HorarioUtils;
import br.edu.unicid.saudeconecta.utils.MaskTextWatcher;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.SimpleTextWatcher;
import br.edu.unicid.saudeconecta.utils.UiUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/** GESTOR – Formulário de inclusão/edição de unidade de saúde. */
public class FormUnidadeActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "extra_unidade_id";

    private ActivityFormUnidadeBinding binding;
    private AdminRepository repositorio;
    private UnidadeSaude unidade = new UnidadeSaude();
    private final List<Chip> chipsServicos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessao = new SessionManager(this);
        if (!sessao.isLogado() || !sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityFormUnidadeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        repositorio = new AdminRepository(this);

        binding.etTelefone.addTextChangedListener(new MaskTextWatcher(MaskTextWatcher.MASCARA_TELEFONE));
        limparErro(binding.tilNome, binding.etNome);
        limparErro(binding.tilEndereco, binding.etEndereco);
        limparErro(binding.tilBairro, binding.etBairro);
        limparErro(binding.tilTelefone, binding.etTelefone);
        limparErro(binding.tilHorario, binding.etHorario);
        criarChipsServicos();
        binding.btnSalvar.setOnClickListener(v -> salvar());

        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        if (id > 0) {
            binding.toolbar.setTitle(R.string.editar_unidade);
            carregar(id);
        } else {
            binding.etHorario.setText("Seg a Sex, 07h às 19h");
        }
    }

    private void limparErro(TextInputLayout til, EditText campo) {
        campo.addTextChangedListener(new SimpleTextWatcher(() -> til.setError(null)));
    }

    private void criarChipsServicos() {
        for (String servico : getResources().getStringArray(R.array.servicos_disponiveis)) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_chip_filtro, binding.cgServicos, false);
            chip.setId(View.generateViewId());
            chip.setText(servico);
            chip.setOnCheckedChangeListener((b, marcado) -> binding.tvErroServicos.setVisibility(View.GONE));
            binding.cgServicos.addView(chip);
            chipsServicos.add(chip);
        }
    }

    private void carregar(long id) {
        repositorio.buscarUnidade(id, new RepositoryCallback<UnidadeSaude>() {
            @Override
            public void onSuccess(UnidadeSaude u) {
                if (u == null || isFinishing()) return;
                unidade = u;
                binding.etNome.setText(u.nome);
                binding.etEndereco.setText(u.endereco);
                binding.etBairro.setText(u.bairro);
                binding.etTelefone.setText(u.telefone);
                binding.etHorario.setText(u.horarioFuncionamento);
                String servicos = HorarioUtils.normalizar(u.servicos);
                for (Chip c : chipsServicos) {
                    c.setChecked(servicos.contains(HorarioUtils.normalizar(c.getText().toString())));
                }
            }

            @Override
            public void onError(String m) {
                Snackbar.make(binding.getRoot(), m, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void salvar() {
        UiUtils.esconderTeclado(this);
        String nome = UiUtils.texto(binding.etNome).replaceAll("\\s+", " ");
        String endereco = UiUtils.texto(binding.etEndereco);
        String bairro = UiUtils.texto(binding.etBairro);
        String telefone = UiUtils.texto(binding.etTelefone);
        String horario = UiUtils.texto(binding.etHorario);
        List<String> servicos = new ArrayList<>();
        for (Chip c : chipsServicos) if (c.isChecked()) servicos.add(c.getText().toString());

        boolean ok = true;
        if (nome.length() < 3) { binding.tilNome.setError(getString(R.string.erro_nome_unidade)); ok = false; }
        if (endereco.length() < 5) { binding.tilEndereco.setError(getString(R.string.erro_endereco_unidade)); ok = false; }
        if (bairro.length() < 2) { binding.tilBairro.setError(getString(R.string.erro_bairro)); ok = false; }
        if (!Validators.isTelefoneValido(telefone)) {
            binding.tilTelefone.setError(getString(R.string.erro_telefone_invalido));
            ok = false;
        }
        // O horário precisa ser interpretável para a etiqueta "Aberta agora"
        if (HorarioUtils.estaAberta(horario, java.time.LocalDateTime.now()) == null) {
            binding.tilHorario.setError(getString(R.string.erro_horario));
            ok = false;
        }
        if (servicos.isEmpty()) {
            binding.tvErroServicos.setVisibility(View.VISIBLE);
            ok = false;
        }
        if (!ok) return;

        unidade.nome = nome;
        unidade.endereco = endereco;
        unidade.bairro = bairro;
        unidade.telefone = telefone;
        unidade.horarioFuncionamento = horario;
        unidade.servicos = TextUtils.join(", ", servicos);

        binding.btnSalvar.setEnabled(false);
        repositorio.salvarUnidade(unidade, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                if (isFinishing()) return;
                android.widget.Toast.makeText(FormUnidadeActivity.this,
                        R.string.salvo_sucesso, android.widget.Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String m) {
                if (isFinishing()) return;
                binding.btnSalvar.setEnabled(true);
                if (m.toLowerCase(Locale.ROOT).contains("nome")) binding.tilNome.setError(m);
                else Snackbar.make(binding.getRoot(), m, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
