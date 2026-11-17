package br.edu.unicid.saudeconecta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ActivityCadastroBinding;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.repositories.UsuarioRepository;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.MaskTextWatcher;
import br.edu.unicid.saudeconecta.utils.SimpleTextWatcher;
import br.edu.unicid.saudeconecta.utils.UiUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/** TELA 1 – Cadastro de paciente com validações e consentimento LGPD. */
public class CadastroActivity extends AppCompatActivity {

    private ActivityCadastroBinding binding;
    private UsuarioRepository repositorio;
    private View primeiroInvalido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCadastroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repositorio = new UsuarioRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        configurarCampos();

        binding.tilNascimento.setEndIconOnClickListener(v -> abrirCalendario());
        binding.tvVerTermos.setOnClickListener(v -> mostrarTermos());
        binding.cbTermos.setOnCheckedChangeListener((b, marcado) -> {
            if (marcado) binding.tvErroTermos.setVisibility(View.GONE);
        });
        binding.btnCadastrar.setOnClickListener(v -> cadastrar());
        binding.tvJaTenhoConta.setOnClickListener(v -> finish());
    }

    private void configurarCampos() {
        binding.etCpf.addTextChangedListener(new MaskTextWatcher(MaskTextWatcher.MASCARA_CPF));
        binding.etNascimento.addTextChangedListener(new MaskTextWatcher(MaskTextWatcher.MASCARA_DATA));
        binding.etTelefone.addTextChangedListener(new MaskTextWatcher(MaskTextWatcher.MASCARA_TELEFONE));

        limparErroAoDigitar(binding.tilNome, binding.etNome);
        limparErroAoDigitar(binding.tilCpf, binding.etCpf);
        limparErroAoDigitar(binding.tilNascimento, binding.etNascimento);
        limparErroAoDigitar(binding.tilTelefone, binding.etTelefone);
        limparErroAoDigitar(binding.tilEmail, binding.etEmail);
        limparErroAoDigitar(binding.tilEndereco, binding.etEndereco);
        limparErroAoDigitar(binding.tilSenha, binding.etSenha);
        limparErroAoDigitar(binding.tilConfirmarSenha, binding.etConfirmarSenha);
    }

    private void limparErroAoDigitar(TextInputLayout til, EditText campo) {
        campo.addTextChangedListener(new SimpleTextWatcher(() -> til.setError(null)));
    }

    private void abrirCalendario() {
        long abrirEm = LocalDate.now().minusYears(30).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        CalendarConstraints restricoes = new CalendarConstraints.Builder()
                .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                .setValidator(DateValidatorPointBackward.now())
                .setOpenAt(abrirEm)
                .build();
        MaterialDatePicker<Long> seletor = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.titulo_calendario_nascimento)
                .setCalendarConstraints(restricoes)
                .build();
        seletor.addOnPositiveButtonClickListener(selecao -> {
            LocalDate data = Instant.ofEpochMilli(selecao).atZone(ZoneOffset.UTC).toLocalDate();
            binding.etNascimento.setText(DateUtils.formatarBr(data));
        });
        seletor.show(getSupportFragmentManager(), "data_nascimento");
    }

    private void mostrarTermos() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.titulo_termos)
                .setMessage(R.string.texto_termos_lgpd)
                .setPositiveButton(R.string.li_e_concordo, (d, w) -> binding.cbTermos.setChecked(true))
                .setNegativeButton(R.string.fechar, null)
                .show();
    }

    private void marcarErro(TextInputLayout til, int mensagem) {
        til.setError(getString(mensagem));
        if (primeiroInvalido == null) primeiroInvalido = til.getEditText();
    }

    private void cadastrar() {
        UiUtils.esconderTeclado(this);
        primeiroInvalido = null;

        String nome = UiUtils.texto(binding.etNome).replaceAll("\\s+", " ");
        String cpf = UiUtils.texto(binding.etCpf);
        String nascimento = UiUtils.texto(binding.etNascimento);
        String telefone = UiUtils.texto(binding.etTelefone);
        String email = UiUtils.texto(binding.etEmail);
        String endereco = UiUtils.texto(binding.etEndereco).replaceAll("\\s+", " ");
        String senha = UiUtils.textoSemTrim(binding.etSenha);
        String confirmar = UiUtils.textoSemTrim(binding.etConfirmarSenha);

        if (!Validators.isNomeCompletoValido(nome)) marcarErro(binding.tilNome, R.string.erro_nome_invalido);

        if (cpf.isEmpty()) marcarErro(binding.tilCpf, R.string.erro_cpf_obrigatorio);
        else if (!Validators.isCpfValido(cpf)) marcarErro(binding.tilCpf, R.string.erro_cpf_invalido);

        LocalDate dataNascimento = Validators.parseDataBr(nascimento);
        if (dataNascimento == null) {
            marcarErro(binding.tilNascimento, R.string.erro_data_invalida);
        } else if (dataNascimento.isAfter(LocalDate.now())) {
            marcarErro(binding.tilNascimento, R.string.erro_data_futura);
        } else {
            int idade = Period.between(dataNascimento, LocalDate.now()).getYears();
            if (idade < 18) marcarErro(binding.tilNascimento, R.string.erro_menor_idade);
            else if (idade > 120) marcarErro(binding.tilNascimento, R.string.erro_idade_maxima);
        }

        if (!Validators.isTelefoneValido(telefone)) marcarErro(binding.tilTelefone, R.string.erro_telefone_invalido);
        if (!Validators.isEmailValido(email)) marcarErro(binding.tilEmail, R.string.erro_email_invalido);
        if (endereco.length() < 10) marcarErro(binding.tilEndereco, R.string.erro_endereco_invalido);
        if (!Validators.isSenhaForte(senha)) marcarErro(binding.tilSenha, R.string.erro_senha_fraca);
        if (!senha.equals(confirmar)) marcarErro(binding.tilConfirmarSenha, R.string.erro_senhas_diferentes);

        boolean termosOk = binding.cbTermos.isChecked();
        binding.tvErroTermos.setVisibility(termosOk ? View.GONE : View.VISIBLE);

        if (primeiroInvalido != null) {
            primeiroInvalido.requestFocus();
            return;
        }
        if (!termosOk) return;

        setCarregando(true);
        repositorio.cadastrarPaciente(nome, cpf, dataNascimento.toString(), telefone, email, endereco, senha,
                new RepositoryCallback<String>() {
                    @Override
                    public void onSuccess(String cpfCadastrado) {
                        if (isFinishing()) return;
                        setCarregando(false);
                        mostrarSucesso(cpfCadastrado);
                    }

                    @Override
                    public void onError(String mensagem) {
                        if (isFinishing()) return;
                        setCarregando(false);
                        Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void mostrarSucesso(String cpf) {
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_check_circle)
                .setTitle(R.string.cadastro_sucesso_titulo)
                .setMessage(R.string.cadastro_sucesso_msg)
                .setCancelable(false)
                .setPositiveButton(R.string.ir_para_login, (d, w) -> {
                    Intent dados = new Intent();
                    dados.putExtra(LoginActivity.EXTRA_CPF, cpf);
                    setResult(RESULT_OK, dados);
                    finish();
                })
                .show();
    }

    private void setCarregando(boolean carregando) {
        binding.btnCadastrar.setEnabled(!carregando);
        binding.btnCadastrar.setText(carregando ? R.string.cadastrando : R.string.cadastrar);
    }
}
