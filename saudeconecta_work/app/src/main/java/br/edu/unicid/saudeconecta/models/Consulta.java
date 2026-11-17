package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "consultas": agendamentos e atendimentos (alimenta os indicadores do gestor). */
@Entity(tableName = "consultas",
        foreignKeys = {
                @ForeignKey(entity = Paciente.class, parentColumns = "id",
                        childColumns = "paciente_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Profissional.class, parentColumns = "id", childColumns = "profissional_id"),
                @ForeignKey(entity = UnidadeSaude.class, parentColumns = "id", childColumns = "unidade_id"),
                @ForeignKey(entity = Especialidade.class, parentColumns = "id", childColumns = "especialidade_id")},
        indices = {
                @Index("paciente_id"),
                @Index(value = {"profissional_id", "data", "hora"}),
                @Index("unidade_id"),
                @Index("especialidade_id"),
                @Index("data"),
                @Index("status")})
public class Consulta {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "paciente_id")
    public long pacienteId;

    @ColumnInfo(name = "profissional_id")
    public long profissionalId;

    @ColumnInfo(name = "unidade_id")
    public long unidadeId;

    @ColumnInfo(name = "especialidade_id")
    public long especialidadeId;

    /** yyyy-MM-dd */
    @NonNull
    public String data = "";

    /** HH:mm */
    @NonNull
    public String hora = "";

    @NonNull
    public String status = StatusConsulta.AGENDADA;

    @NonNull
    @ColumnInfo(name = "tipo_atendimento")
    public String tipoAtendimento = TipoAtendimento.CONSULTA_MEDICA;

    public String observacoes;

    @ColumnInfo(name = "motivo_cancelamento")
    public String motivoCancelamento;

    /** Minutos entre a chegada e o atendimento (apenas consultas realizadas). */
    @ColumnInfo(name = "tempo_espera_min")
    public Integer tempoEsperaMin;

    @ColumnInfo(name = "criado_em")
    public long criadoEm;

    @ColumnInfo(name = "atualizado_em")
    public long atualizadoEm;

    public static Consulta criar(long pacienteId, Profissional profissional, String data, String hora,
                                 String status, String tipoAtendimento) {
        Consulta c = new Consulta();
        c.pacienteId = pacienteId;
        c.profissionalId = profissional.id;
        c.unidadeId = profissional.unidadeId;
        c.especialidadeId = profissional.especialidadeId;
        c.data = data;
        c.hora = hora;
        c.status = status;
        c.tipoAtendimento = tipoAtendimento;
        c.criadoEm = System.currentTimeMillis();
        c.atualizadoEm = c.criadoEm;
        return c;
    }
}
