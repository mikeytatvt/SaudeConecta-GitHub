package br.edu.unicid.saudeconecta.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.adapters.OpcoesAdapter;
import br.edu.unicid.saudeconecta.databinding.ActivityFormProfissionalBinding;
import br.edu.unicid.saudeconecta.models.Especialidade;
import br.edu.unicid.saudeconecta.models.Profissional;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.repositories.AdminRepository;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.SimpleTextWatcher;
import br.edu.unicid.saudeconecta.utils.UiUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/** GESTOR – Formulário de inclusão/edição de profissional. */
public class FormProfissionalActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "extra_profissional_id";

    private ActivityFormProfissionalBinding binding;
    private AdminRepository repositorio;
    private Profissional profissional = new Profissional();
    private List<Especialidade> especialidades = new ArrayList<>();
    private List<UnidadeSaude> unidades = new ArrayList<>();
    private Especialidade especialidadeEscolhida;
    private UnidadeSaude unidadeEscolhida;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager sessao = new SessionManager(this);
        if (!sessao.isLogado() || !sessao.isGestor()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityFormProfissionalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        repositorio = new AdminRepository(this);

        limparErro(binding.tilNome, binding.etNome);
        limparErro(binding.tilRegistro, binding.etRegistro);
        binding.actvEspecialidade.setOnItemClickListener((p, v, pos, id) -> {
            especialidadeEscolhida = especialidades.get(pos);
            binding.tilEspecialidade.setError(null);
        });
        binding.actvUnidade.setOnItemClickListener((p, v, pos, id) -> {
            unidadeEscolhida = unidades.get(pos);
            binding.tilUnidade.setError(null);
        });
        binding.btnSalvar.setOnClickListener(v -> salvar());

        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        if (id > 0) binding.toolbar.setTitle(R.string.editar_profissional);
        carregarOpcoes(id);
    }

    private void limparErro(TextInputLayout til, EditText campo) {
        campo.addTextChangedListener(new SimpleTextWatcher(() -> til.setError(null)));
    }

    private void carregarOpcoes(long id) {
        repositorio.carregarOpcoesProfissional(new RepositoryCallback<AdminRepository.OpcoesProfissional>() {
            @Override
            public void onSuccess(AdminRepository.OpcoesProfissional o) {
                if (isFinishing()) return;
                especialidades = o.especialidades;
                unidades = o.unidades;
                List<String> nomesEsp = new ArrayList<>();
                for (Especialidade e : especialidades) nomesEsp.add(e.nome);
                List<String> nomesUni = new ArrayList<>();
                for (UnidadeSaude u : unidades) nomesUni.add(u.nome);
                binding.actvEspecialidade.setAdapter(new OpcoesAdapter(FormProfissionalActivity.this, nomesEsp));
                binding.actvUnidade.setAdapter(new OpcoesAdapter(FormProfissionalActivity.this, nomesUni));
                if (id > 0) carregarProfissional(id);
            }

            @Override
            public void onError(String m) {
                Snackbar.make(binding.getRoot(), m, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void carregarProfissional(long id) {
        repositorio.buscarProfissional(id, new RepositoryCallback<Profissional>() {
            @Override
            public void onSuccess(Profissional p) {
                if (p == null || isFinishing()) return;
                profissional = p;
                binding.etNome.setText(p.nome);
                binding.etRegistro.setText(p.registro);
                for (Especialidade e : especialidades) {
                    if (e.id == p.especialidadeId) {
                        especialidadeEscolhida = e;
                        binding.actvEspecialidade.setText(e.nome, false);
                    }
                }
                for (UnidadeSaude u : unidades) {
                    if (u.id == p.unidadeId) {
                        unidadeEscolhida = u;
                        binding.actvUnidade.setText(u.nome, false);
                    }
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
        String registro = UiUtils.texto(binding.etRegistro).toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");

        boolean ok = true;
        if (!Validators.isNomeCompletoValido(nome)) {
            binding.tilNome.setError(getString(R.string.erro_nome_invalido));
            ok = false;
        }
        if (!registro.matches("^(CRM|CRO|COREN)-[A-Z]{2} \\d{4,7}$")) {
            binding.tilRegistro.setError(getString(R.string.erro_registro));
            ok = false;
        }
        if (especialidadeEscolhida == null) {
            binding.tilEspecialidade.setError(getString(R.string.erro_selecione));
            ok = false;
        }
        if (unidadeEscolhida == null) {
            binding.tilUnidade.setError(getString(R.string.erro_selecione));
            ok = false;
        }
        if (!ok) return;

        profissional.nome = nome;
        profissional.registro = registro;
        profissional.especialidadeId = especialidadeEscolhida.id;
        profissional.unidadeId = unidadeEscolhida.id;

        binding.btnSalvar.setEnabled(false);
        repositorio.salvarProfissional(profissional, especialidadeEscolhida.nome, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                if (isFinishing()) return;
                Toast.makeText(FormProfissionalActivity.this, R.string.salvo_sucesso, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String m) {
                if (isFinishing()) return;
                binding.btnSalvar.setEnabled(true);
                if (m.toLowerCase(Locale.ROOT).contains("registro")) binding.tilRegistro.setError(m);
                else Snackbar.make(binding.getRoot(), m, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}
