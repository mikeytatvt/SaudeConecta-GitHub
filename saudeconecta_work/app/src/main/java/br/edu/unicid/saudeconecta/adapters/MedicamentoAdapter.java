package br.edu.unicid.saudeconecta.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ItemMedicamentoBinding;
import br.edu.unicid.saudeconecta.databinding.ItemMedicamentoResumoBinding;
import br.edu.unicid.saudeconecta.models.MedicamentoDetalhado;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/** Lista de medicamentos com um card de resumo no topo. */
public class MedicamentoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void aoSelecionar(MedicamentoDetalhado medicamento);
    }

    private static final int TIPO_RESUMO = 0;
    private static final int TIPO_ITEM = 1;

    private final List<MedicamentoDetalhado> itens = new ArrayList<>();
    private final List<String> farmacias = new ArrayList<>();
    private final Listener listener;

    public MedicamentoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void atualizar(List<MedicamentoDetalhado> medicamentos, List<String> locais) {
        itens.clear();
        itens.addAll(medicamentos);
        farmacias.clear();
        farmacias.addAll(locais);
        notifyDataSetChanged();
    }

    /** Texto "Médico – Especialidade" usado no card e no painel de detalhes. */
    public static String nomeMedico(Context ctx, MedicamentoDetalhado m) {
        if (m.profissionalNome == null) return "—";
        if (m.especialidadeNome == null) return m.profissionalNome;
        return ctx.getString(R.string.medico_especialidade, m.profissionalNome, m.especialidadeNome);
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TIPO_RESUMO : TIPO_ITEM;
    }

    @Override
    public int getItemCount() {
        return itens.isEmpty() ? 0 : itens.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_RESUMO) {
            return new ResumoHolder(ItemMedicamentoResumoBinding.inflate(inflater, parent, false));
        }
        return new ItemHolder(ItemMedicamentoBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ResumoHolder) {
            ItemMedicamentoResumoBinding b = ((ResumoHolder) holder).binding;
            Context ctx = b.getRoot().getContext();
            b.tvTotal.setText(ctx.getResources().getQuantityString(
                    R.plurals.total_medicamentos, itens.size(), itens.size()));
            b.tvFarmacias.setVisibility(farmacias.isEmpty() ? View.GONE : View.VISIBLE);
            b.tvFarmacias.setText(ctx.getString(R.string.retirada_farmacia, TextUtils.join(", ", farmacias)));
            return;
        }

        MedicamentoDetalhado item = itens.get(position - 1);
        ItemMedicamentoBinding b = ((ItemHolder) holder).binding;
        Context ctx = b.getRoot().getContext();
        b.tvNome.setText(item.medicamento.nome);
        b.tvDosagem.setText(item.medicamento.dosagem);
        b.tvOrientacao.setText(item.medicamento.orientacao);
        b.tvQuantidade.setText(ctx.getString(R.string.quantidade, item.medicamento.quantidade));
        b.tvPrescricao.setText(ctx.getString(R.string.prescrito_em, DateUtils.isoParaBr(item.medicamento.dataPrescricao)));
        b.tvMedico.setText(nomeMedico(ctx, item));
        b.getRoot().setOnClickListener(v -> listener.aoSelecionar(item));
    }

    static class ResumoHolder extends RecyclerView.ViewHolder {
        final ItemMedicamentoResumoBinding binding;

        ResumoHolder(ItemMedicamentoResumoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        final ItemMedicamentoBinding binding;

        ItemHolder(ItemMedicamentoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
