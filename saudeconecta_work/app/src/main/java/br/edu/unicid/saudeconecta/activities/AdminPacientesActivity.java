package br.edu.unicid.saudeconecta.activities;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.models.ItemAdmin;
import br.edu.unicid.saudeconecta.models.PacienteResumo;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/**
 * GESTOR – Gerenciar pacientes: busca, dados de contato e alteração da unidade de referência.
 * LGPD: o CPF aparece sempre mascarado; o gestor vê só o necessário para a gestão.
 */
public class AdminPacientesActivity extends AdminListaBaseActivity {

    @Override
    protected int titulo() { return R.string.titulo_admin_pacientes; }

    @Override
    protected int dicaBusca() { return R.string.busca_pacientes; }

    @Override
    protected void carregar() {
        repositorio.listarPacientes(new RepositoryCallback<List<PacienteResumo>>() {
            @Override
            public void onSuccess(List<PacienteResumo> lista) {
                List<ItemAdmin> itens = new ArrayList<>();
                for (PacienteResumo r : lista) {
                    ItemAdmin i = new ItemAdmin();
                    i.id = r.paciente.id;
                    i.origem = r;
                    i.titulo = r.paciente.nome;
                    i.subtitulo = "CPF " + Validators.mascararCpf(r.paciente.cpf)
                            + "  •  Nasc. " + DateUtils.isoParaBr(r.paciente.dataNascimento);
                    i.detalhe = "UBS de referência: " + (r.unidadeNome != null ? r.unidadeNome : "não definida")
                            + "\nAtendimentos realizados: " + r.atendimentos + "  •  Agendadas: " + r.agendadas;
                    boolean temConta = r.paciente.usuarioId != null;
                    i.etiqueta = getString(temConta ? R.string.com_conta : R.string.sem_conta);
                    i.estiloEtiqueta = temConta ? ItemAdmin.ETIQUETA_VERDE : ItemAdmin.ETIQUETA_AMARELA;
                    i.acao1 = getString(R.string.alterar_ubs);
                    itens.add(i);
                }
                exibir(itens);
            }

            @Override
            public void onError(String m) { mensagem(m); }
        });
    }

    @Override
    public void aoTocar(ItemAdmin item) {
        PacienteResumo r = (PacienteResumo) item.origem;
        String dados = "CPF: " + Validators.mascararCpf(r.paciente.cpf)
                + "\nNascimento: " + DateUtils.isoParaBr(r.paciente.dataNascimento)
                + "\nTelefone: " + valor(r.paciente.telefone)
                + "\nE-mail: " + valor(r.paciente.email)
                + "\nEndereço: " + valor(r.paciente.endereco)
                + "\nCartão SUS: " + valor(r.paciente.cartaoSus)
                + "\nUBS de referência: " + valor(r.unidadeNome)
                + "\n\nAtendimentos realizados: " + r.atendimentos
                + "\nConsultas agendadas: " + r.agendadas;
        new MaterialAlertDialogBuilder(this)
                .setTitle(r.paciente.nome)
                .setMessage(dados)
                .setPositiveButton(R.string.fechar, null)
                .setNeutralButton(R.string.alterar_ubs, (d, w) -> aoAcao1(item))
                .show();
    }

    private static String valor(String v) {
        return v == null || v.trim().isEmpty() ? "—" : v;
    }

    @Override
    public void aoAcao1(ItemAdmin item) {
        PacienteResumo r = (PacienteResumo) item.origem;
        repositorio.listarUnidadesAtivas(new RepositoryCallback<List<UnidadeSaude>>() {
            @Override
            public void onSuccess(List<UnidadeSaude> unidades) {
                if (isFinishing() || unidades.isEmpty()) return;
                String[] nomes = new String[unidades.size()];
                int atual = -1;
                for (int i = 0; i < unidades.size(); i++) {
                    nomes[i] = unidades.get(i).nome;
                    if (r.paciente.unidadeReferenciaId != null && unidades.get(i).id == r.paciente.unidadeReferenciaId) {
                        atual = i;
                    }
                }
                final int[] escolhida = {Math.max(atual, 0)};
                new MaterialAlertDialogBuilder(AdminPacientesActivity.this)
                        .setTitle(R.string.alterar_ubs_titulo)
                        .setSingleChoiceItems(nomes, escolhida[0], (d, posicao) -> escolhida[0] = posicao)
                        .setPositiveButton(R.string.salvar, (d, w) ->
                                salvarUnidade(r.paciente.id, unidades.get(escolhida[0]).id))
                        .setNegativeButton(R.string.cancelar, null)
                        .show();
            }

            @Override
            public void onError(String m) { mensagem(m); }
        });
    }

    private void salvarUnidade(long pacienteId, long unidadeId) {
        repositorio.alterarUnidadeReferencia(pacienteId, unidadeId, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                mensagem(R.string.ubs_alterada);
                carregar();
            }

            @Override
            public void onError(String m) { alerta(m); }
        });
    }
}
