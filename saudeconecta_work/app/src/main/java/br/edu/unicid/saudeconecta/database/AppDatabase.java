package br.edu.unicid.saudeconecta.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import br.edu.unicid.saudeconecta.database.dao.AdminDao;
import br.edu.unicid.saudeconecta.database.dao.ConsultaDao;
import br.edu.unicid.saudeconecta.database.dao.EspecialidadeDao;
import br.edu.unicid.saudeconecta.database.dao.ExameDao;
import br.edu.unicid.saudeconecta.database.dao.IndicadorDao;
import br.edu.unicid.saudeconecta.database.dao.MedicamentoDao;
import br.edu.unicid.saudeconecta.database.dao.NotificacaoDao;
import br.edu.unicid.saudeconecta.database.dao.PacienteDao;
import br.edu.unicid.saudeconecta.database.dao.ProfissionalDao;
import br.edu.unicid.saudeconecta.database.dao.ResultadoExameDao;
import br.edu.unicid.saudeconecta.database.dao.UnidadeSaudeDao;
import br.edu.unicid.saudeconecta.database.dao.UsuarioDao;
import br.edu.unicid.saudeconecta.models.Consulta;
import br.edu.unicid.saudeconecta.models.Especialidade;
import br.edu.unicid.saudeconecta.models.Exame;
import br.edu.unicid.saudeconecta.models.Medicamento;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.Paciente;
import br.edu.unicid.saudeconecta.models.Profissional;
import br.edu.unicid.saudeconecta.models.ResultadoExame;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.models.Usuario;

/**
 * Banco local (SQLite via Room) com as 10 tabelas do projeto.
 * A mesma estrutura está em docs/mysql/saude_conecta_mysql.sql para a API Spring Boot.
 */
@Database(entities = {
        Usuario.class, Paciente.class, Profissional.class, UnidadeSaude.class, Especialidade.class,
        Consulta.class, Exame.class, ResultadoExame.class, Medicamento.class, Notificacao.class},
        version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String NOME_BANCO = "saude_conecta.db";
    private static volatile AppDatabase instancia;

    public abstract UsuarioDao usuarioDao();
    public abstract PacienteDao pacienteDao();
    public abstract ProfissionalDao profissionalDao();
    public abstract UnidadeSaudeDao unidadeSaudeDao();
    public abstract EspecialidadeDao especialidadeDao();
    public abstract ConsultaDao consultaDao();
    public abstract ExameDao exameDao();
    public abstract ResultadoExameDao resultadoExameDao();
    public abstract MedicamentoDao medicamentoDao();
    public abstract NotificacaoDao notificacaoDao();
    public abstract IndicadorDao indicadorDao();
    public abstract AdminDao adminDao();

    public static AppDatabase getInstance(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, NOME_BANCO)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instancia;
    }
}
