package br.edu.unicid.saudeconecta.database;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import br.edu.unicid.saudeconecta.models.Consulta;
import br.edu.unicid.saudeconecta.models.Especialidade;
import br.edu.unicid.saudeconecta.models.Exame;
import br.edu.unicid.saudeconecta.models.Medicamento;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.Paciente;
import br.edu.unicid.saudeconecta.models.Perfil;
import br.edu.unicid.saudeconecta.models.Profissional;
import br.edu.unicid.saudeconecta.models.ResultadoExame;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.models.StatusExame;
import br.edu.unicid.saudeconecta.models.TipoAtendimento;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.models.Usuario;
import br.edu.unicid.saudeconecta.utils.AgendaUtils;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.PasswordUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/**
 * Popula o banco com DADOS SIMULADOS (nenhuma informação real de pacientes).
 * Os totais reproduzem a Figura 3 do banner: 2.854 atendimentos, 1.287 consultas médicas,
 * 842 exames e 725 pacientes, com 892/678/543/420/321 atendimentos por UBS.
 */
public final class DatabaseSeeder {

    public static final String SENHA_DEMO = "Saude@2026";
    public static final String CPF_PACIENTE_DEMO = "11144477735"; // CPF de teste (fictício)
    public static final String CPF_GESTOR_DEMO = "52998224725";   // CPF de teste (fictício)

    private static final int[] ATENDIMENTOS_POR_UNIDADE = {892, 678, 543, 420, 321};
    private static final int TOTAL_CONSULTAS_MEDICAS = 1287;
    private static final int TOTAL_EXAMES_REALIZADOS = 842;
    private static final int TOTAL_PACIENTES_ATENDIDOS = 725;
    private static final int DIAS_HISTORICO = 21;

    private static final String[] PRIMEIROS_NOMES = {"Ana", "Bruno", "Carla", "Daniel", "Eduarda", "Fábio",
            "Gabriela", "Henrique", "Isadora", "João", "Karina", "Leonardo", "Mariana", "Nicolas", "Olívia",
            "Paulo", "Rafaela", "Samuel", "Tatiane", "Vinícius", "Yasmin", "Wesley", "Letícia", "Diego",
            "Priscila", "Mateus", "Luana", "Caio", "Sofia", "Igor"};
    private static final String[] SOBRENOMES = {"Silva", "Santos", "Oliveira", "Souza", "Rodrigues",
            "Ferreira", "Alves", "Pereira", "Lima", "Gomes", "Costa", "Ribeiro", "Martins", "Carvalho",
            "Araújo", "Melo", "Barbosa", "Rocha", "Dias", "Teixeira", "Moreira", "Cavalcanti", "Correia"};
    private static final String[] EXAMES = {"Hemograma completo", "Glicemia em jejum",
            "Colesterol total e frações", "Triglicerídeos", "Urina tipo 1", "Creatinina", "TSH",
            "Hemoglobina glicada", "Raio-X de tórax", "Eletrocardiograma", "Ultrassonografia abdominal"};

    private DatabaseSeeder() { }

    public static void popularSeNecessario(AppDatabase db) {
        if (db.usuarioDao().contar() > 0) return;
        db.runInTransaction(() -> popular(db));
    }

    private static void popular(AppDatabase db) {
        Random rnd = new Random(2026);
        long agora = System.currentTimeMillis();
        LocalDate hoje = LocalDate.now();

        // 1) Unidades de saúde (endereços fictícios)
        List<UnidadeSaude> unidades = Arrays.asList(
                UnidadeSaude.criar("UBS Centro", "Rua das Palmeiras, 120", "Centro", "(11) 3000-1001",
                        "Seg a Sex, 07h às 19h",
                        "Clínica Geral, Pediatria, Cardiologia, Dermatologia, Ginecologia, Odontologia, Vacinação, Coleta de exames"),
                UnidadeSaude.criar("UBS Norte", "Av. dos Ipês, 845", "Jardim Norte", "(11) 3000-1002",
                        "Seg a Sex, 07h às 17h",
                        "Clínica Geral, Cardiologia, Ortopedia, Vacinação, Curativos"),
                UnidadeSaude.criar("UBS Sul", "Rua do Lago, 300", "Vila Sul", "(11) 3000-1003",
                        "Seg a Sex, 07h às 17h",
                        "Clínica Geral, Pediatria, Ortopedia, Coleta de exames, Farmácia básica"),
                UnidadeSaude.criar("UBS Leste", "Av. Sol Nascente, 1500", "Parque Leste", "(11) 3000-1004",
                        "Seg a Sex, 07h às 19h",
                        "Clínica Geral, Dermatologia, Odontologia, Vacinação, Farmácia básica"),
                UnidadeSaude.criar("UBS Oeste", "Rua das Acácias, 77", "Vila Oeste", "(11) 3000-1005",
                        "Seg a Sex, 07h às 17h",
                        "Clínica Geral, Ginecologia, Odontologia, Coleta de exames, Saúde da Mulher"));
        List<Long> unidadeIds = db.unidadeSaudeDao().inserirTodos(unidades);

        // 2) Especialidades
        String[][] esp = {
                {"Clínica Geral", "Atendimento clínico geral e acompanhamento de rotina"},
                {"Pediatria", "Saúde de crianças e adolescentes"},
                {"Cardiologia", "Coração e sistema circulatório"},
                {"Dermatologia", "Pele, cabelos e unhas"},
                {"Ginecologia", "Saúde da mulher"},
                {"Ortopedia", "Ossos, músculos e articulações"},
                {"Odontologia", "Saúde bucal"}};
        List<Especialidade> especialidades = new ArrayList<>();
        for (String[] e : esp) especialidades.add(Especialidade.criar(e[0], e[1]));
        List<Long> espIds = db.especialidadeDao().inserirTodos(especialidades);
        Map<String, Long> espPorNome = new HashMap<>();
        for (int i = 0; i < esp.length; i++) espPorNome.put(esp[i][0], espIds.get(i));
        long idOdonto = espPorNome.get("Odontologia");

        // 3) Profissionais (nomes fictícios) – {nome, registro, especialidade, índice da UBS}
        Object[][] defs = {
                {"Dra. Juliana Costa", "CRM-SP 100101", "Clínica Geral", 0},
                {"Dr. Marcos Vieira", "CRM-SP 100102", "Clínica Geral", 1},
                {"Dra. Patrícia Nogueira", "CRM-SP 100103", "Clínica Geral", 2},
                {"Dr. Renato Alves", "CRM-SP 100104", "Clínica Geral", 3},
                {"Dra. Camila Duarte", "CRM-SP 100105", "Clínica Geral", 4},
                {"Dra. Beatriz Ramos", "CRM-SP 100106", "Pediatria", 0},
                {"Dr. Felipe Moura", "CRM-SP 100107", "Pediatria", 2},
                {"Dr. Eduardo Prado", "CRM-SP 100108", "Cardiologia", 0},
                {"Dra. Helena Martins", "CRM-SP 100109", "Cardiologia", 1},
                {"Dra. Larissa Campos", "CRM-SP 100110", "Dermatologia", 0},
                {"Dr. Thiago Barros", "CRM-SP 100111", "Dermatologia", 3},
                {"Dra. Renata Freitas", "CRM-SP 100112", "Ginecologia", 0},
                {"Dra. Sabrina Lopes", "CRM-SP 100113", "Ginecologia", 4},
                {"Dr. Gustavo Teixeira", "CRM-SP 100114", "Ortopedia", 1},
                {"Dr. André Cardoso", "CRM-SP 100115", "Ortopedia", 2},
                {"Dra. Vanessa Rocha", "CRO-SP 200101", "Odontologia", 0},
                {"Dr. Rodrigo Pires", "CRO-SP 200102", "Odontologia", 3},
                {"Dra. Aline Mendes", "CRO-SP 200103", "Odontologia", 4}};
        List<Profissional> profs = new ArrayList<>();
        for (Object[] d : defs) {
            profs.add(Profissional.criar((String) d[0], (String) d[1],
                    espPorNome.get((String) d[2]), unidadeIds.get((Integer) d[3])));
        }
        List<Long> profIds = db.profissionalDao().inserirTodos(profs);
        for (int i = 0; i < profs.size(); i++) profs.get(i).id = profIds.get(i);

        Profissional juliana = profs.get(0);
        Profissional cardioCentro = profs.get(7);
        Profissional dermatoCentro = profs.get(9);
        Profissional dentistaCentro = profs.get(15);

        // Profissionais por unidade (médicos e dentistas)
        List<List<Profissional>> medicosPorUnidade = new ArrayList<>();
        List<List<Profissional>> dentistasPorUnidade = new ArrayList<>();
        for (int u = 0; u < unidadeIds.size(); u++) {
            medicosPorUnidade.add(new ArrayList<>());
            dentistasPorUnidade.add(new ArrayList<>());
        }
        for (Profissional p : profs) {
            int u = unidadeIds.indexOf(p.unidadeId);
            if (p.especialidadeId == idOdonto) dentistasPorUnidade.get(u).add(p);
            else medicosPorUnidade.get(u).add(p);
        }

        // 4) Usuários de demonstração
        long mariaUid = db.usuarioDao().inserir(novoUsuario("Maria Aparecida Silva", CPF_PACIENTE_DEMO,
                "maria.silva@exemplo.com", "(11) 98888-0001", Perfil.PACIENTE, agora));
        long gestorUid = db.usuarioDao().inserir(novoUsuario("Fernanda Ribeiro", CPF_GESTOR_DEMO,
                "gestor@saudeconecta.exemplo", "(11) 98888-0002", Perfil.GESTOR, agora));

        Paciente maria = new Paciente();
        maria.usuarioId = mariaUid;
        maria.nome = "Maria Aparecida Silva";
        maria.cpf = CPF_PACIENTE_DEMO;
        maria.dataNascimento = "1985-03-14";
        maria.telefone = "(11) 98888-0001";
        maria.email = "maria.silva@exemplo.com";
        maria.endereco = "Rua Exemplo, 45 – Centro, São Paulo/SP";
        maria.cartaoSus = "898 0012 3456 7890";
        maria.unidadeReferenciaId = unidadeIds.get(0);
        maria.criadoEm = agora;
        long mariaPid = db.pacienteDao().inserir(maria);

        // 5) Pacientes simulados (cadastrados na UBS, sem conta no app)
        Set<String> cpfsUsados = new HashSet<>(Arrays.asList(CPF_PACIENTE_DEMO, CPF_GESTOR_DEMO));
        List<Paciente> simulados = new ArrayList<>();
        for (int i = 0; i < TOTAL_PACIENTES_ATENDIDOS - 1; i++) {
            Paciente p = new Paciente();
            p.nome = sortear(rnd, PRIMEIROS_NOMES) + " " + sortear(rnd, SOBRENOMES) + " " + sortear(rnd, SOBRENOMES);
            p.cpf = gerarCpfUnico(rnd, cpfsUsados);
            p.dataNascimento = LocalDate.of(1940 + rnd.nextInt(82), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28)).toString();
            p.telefone = String.format(Locale.ROOT, "(11) 9%04d-%04d", rnd.nextInt(10000), rnd.nextInt(10000));
            p.unidadeReferenciaId = unidadeIds.get(rnd.nextInt(unidadeIds.size()));
            p.criadoEm = agora;
            simulados.add(p);
        }
        List<Long> simIds = db.pacienteDao().inserirTodos(simulados);

        // 6) Histórico de atendimentos REALIZADOS (base dos indicadores do gestor)
        List<LocalDate> diasHist = DateUtils.diasUteisAnteriores(hoje, DIAS_HISTORICO);
        List<Consulta> historico = new ArrayList<>();
        // 3 consultas realizadas da Maria (contam no total da UBS Centro)
        historico.add(realizada(mariaPid, juliana, diasHist.get(1), "08:30", TipoAtendimento.CONSULTA_MEDICA, 42,
                "Consulta de rotina. Solicitados exames laboratoriais."));
        historico.add(realizada(mariaPid, cardioCentro, diasHist.get(6), "10:00", TipoAtendimento.CONSULTA_MEDICA, 35,
                "Avaliação cardiológica preventiva. Pressão arterial controlada."));
        historico.add(realizada(mariaPid, juliana, diasHist.get(diasHist.size() - 3), "14:00",
                TipoAtendimento.CONSULTA_MEDICA, 24, "Retorno com resultados de exames."));
        int consultasMaria = historico.size();

        List<Integer> slotsUnidade = new ArrayList<>();
        for (int u = 0; u < ATENDIMENTOS_POR_UNIDADE.length; u++) {
            int qtd = ATENDIMENTOS_POR_UNIDADE[u] - (u == 0 ? consultasMaria : 0);
            for (int k = 0; k < qtd; k++) slotsUnidade.add(u);
        }
        Collections.shuffle(slotsUnidade, rnd);
        int n = slotsUnidade.size();

        boolean[] medica = new boolean[n];
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);
        Collections.shuffle(indices, rnd);
        for (int i = 0; i < TOTAL_CONSULTAS_MEDICAS - consultasMaria; i++) medica[indices.get(i)] = true;

        // Cada paciente simulado recebe ao menos 1 atendimento (total de 725 pacientes atendidos)
        List<Long> pacientesAtrib = new ArrayList<>(simIds);
        while (pacientesAtrib.size() < n) pacientesAtrib.add(simIds.get(rnd.nextInt(simIds.size())));
        Collections.shuffle(pacientesAtrib, rnd);

        for (int i = 0; i < n; i++) {
            int u = slotsUnidade.get(i);
            List<Profissional> medicos = medicosPorUnidade.get(u);
            List<Profissional> dentistas = dentistasPorUnidade.get(u);
            String tipo;
            Profissional prof;
            if (medica[i]) {
                tipo = TipoAtendimento.CONSULTA_MEDICA;
                prof = medicos.get(rnd.nextInt(medicos.size()));
            } else if (!dentistas.isEmpty() && rnd.nextDouble() < 0.4) {
                tipo = TipoAtendimento.ODONTOLOGICO;
                prof = dentistas.get(rnd.nextInt(dentistas.size()));
            } else {
                tipo = rnd.nextBoolean() ? TipoAtendimento.RETORNO : TipoAtendimento.PROCEDIMENTO;
                prof = medicos.get(rnd.nextInt(medicos.size()));
            }
            int dia = rnd.nextInt(diasHist.size());
            String hora = String.format(Locale.ROOT, "%02d:%02d", 7 + rnd.nextInt(11), rnd.nextBoolean() ? 0 : 30);
            historico.add(realizada(pacientesAtrib.get(i), prof, diasHist.get(dia), hora, tipo,
                    tempoEspera(rnd, dia, diasHist.size()), null));
        }
        List<Long> histIds = db.consultaDao().inserirTodos(historico);

        // 7) Exames
        List<Exame> exames = new ArrayList<>();
        Long centroId = unidadeIds.get(0);
        exames.add(Exame.criar(mariaPid, histIds.get(0), centroId, juliana.id, "Hemograma completo", "Laboratorial",
                diasHist.get(1).toString(), diasHist.get(3).toString(), StatusExame.RESULTADO_DISPONIVEL));
        exames.add(Exame.criar(mariaPid, histIds.get(0), centroId, juliana.id, "Glicemia em jejum", "Laboratorial",
                diasHist.get(1).toString(), diasHist.get(3).toString(), StatusExame.RESULTADO_DISPONIVEL));
        exames.add(Exame.criar(mariaPid, histIds.get(1), centroId, cardioCentro.id, "Colesterol total e frações",
                "Laboratorial", diasHist.get(6).toString(), diasHist.get(diasHist.size() - 2).toString(),
                StatusExame.REALIZADO));
        exames.add(Exame.criar(mariaPid, histIds.get(2), centroId, juliana.id, "Raio-X de tórax", "Imagem",
                diasHist.get(diasHist.size() - 3).toString(), null, StatusExame.SOLICITADO));
        Exame ecg = Exame.criar(mariaPid, histIds.get(1), centroId, cardioCentro.id, "Eletrocardiograma",
                "Cardiológico", diasHist.get(6).toString(),
                DateUtils.proximoDiaUtil(hoje.plusDays(7)).toString(), StatusExame.AGENDADO);
        ecg.preparo = "Evitar cafeína e atividade física intensa 2 horas antes.";
        exames.add(ecg);
        int examesRealizadosMaria = 3;

        List<Integer> idxExames = new ArrayList<>();
        for (int i = consultasMaria; i < historico.size(); i++) idxExames.add(i);
        Collections.shuffle(idxExames, rnd);
        for (int k = 0; k < TOTAL_EXAMES_REALIZADOS - examesRealizadosMaria; k++) {
            int idx = idxExames.get(k);
            Consulta c = historico.get(idx);
            String nome = sortear(rnd, EXAMES);
            exames.add(Exame.criar(c.pacienteId, histIds.get(idx), c.unidadeId, c.profissionalId, nome,
                    tipoExame(nome), c.data, c.data, StatusExame.REALIZADO));
        }
        List<Long> exameIds = db.exameDao().inserirTodos(exames);

        String liberacao = diasHist.get(5).toString();
        db.resultadoExameDao().inserir(ResultadoExame.criar(exameIds.get(0),
                "Hemograma dentro dos valores de referência.",
                "Hemácias: 4,6 milhões/µL (ref. 4,0 a 5,4)\nHemoglobina: 13,8 g/dL (ref. 12,0 a 16,0)\n"
                        + "Hematócrito: 41% (ref. 36 a 46)\nLeucócitos: 6.800/µL (ref. 4.000 a 11.000)\n"
                        + "Plaquetas: 245.000/µL (ref. 150.000 a 450.000)",
                "Sem alterações significativas.", "resultado_" + exameIds.get(0) + ".pdf", liberacao));
        db.resultadoExameDao().inserir(ResultadoExame.criar(exameIds.get(1),
                "Glicemia levemente acima do valor de referência.",
                "Glicemia de jejum: 104 mg/dL (ref. 70 a 99)",
                "Recomenda-se acompanhamento com a equipe de saúde e orientação alimentar.",
                "resultado_" + exameIds.get(1) + ".pdf", liberacao));

        // 8) Agendamentos futuros
        Set<String> ocupados = new HashSet<>();
        List<Consulta> futuras = new ArrayList<>();
        LocalDate dataProxima = DateUtils.proximoDiaUtil(hoje.plusDays(3));
        Consulta proxima = Consulta.criar(mariaPid, juliana, dataProxima.toString(), "09:30",
                StatusConsulta.AGENDADA, TipoAtendimento.CONSULTA_MEDICA);
        proxima.observacoes = "Consulta Clínica Geral";
        futuras.add(proxima);
        ocupados.add(chave(juliana.id, proxima.data, proxima.hora));

        LocalDate dataOdonto = DateUtils.proximoDiaUtil(hoje.plusDays(12));
        Consulta odonto = Consulta.criar(mariaPid, dentistaCentro, dataOdonto.toString(), "14:00",
                StatusConsulta.AGENDADA, TipoAtendimento.ODONTOLOGICO);
        odonto.observacoes = "Avaliação odontológica";
        futuras.add(odonto);
        ocupados.add(chave(dentistaCentro.id, odonto.data, odonto.hora));

        Consulta cancelada = Consulta.criar(mariaPid, dermatoCentro,
                DateUtils.proximoDiaUtil(hoje.plusDays(5)).toString(), "11:00",
                StatusConsulta.CANCELADA, TipoAtendimento.CONSULTA_MEDICA);
        cancelada.motivoCancelamento = "Cancelada pela paciente pelo aplicativo.";
        futuras.add(cancelada);

        for (Profissional p : profs) {
            String tipo = p.especialidadeId == idOdonto ? TipoAtendimento.ODONTOLOGICO : TipoAtendimento.CONSULTA_MEDICA;
            for (LocalDate dia : DateUtils.proximosDiasUteis(hoje.plusDays(1), 15)) {
                for (String hora : AgendaUtils.HORARIOS_ATENDIMENTO) {
                    String k = chave(p.id, dia.toString(), hora);
                    if (rnd.nextDouble() < 0.25 && !ocupados.contains(k)) {
                        ocupados.add(k);
                        futuras.add(Consulta.criar(simIds.get(rnd.nextInt(simIds.size())), p,
                                dia.toString(), hora, StatusConsulta.AGENDADA, tipo));
                    }
                }
            }
        }
        List<Long> futurasIds = db.consultaDao().inserirTodos(futuras);

        // 9) Medicamentos da Maria
        String dataPresc = diasHist.get(1).toString();
        db.medicamentoDao().inserirTodos(Arrays.asList(
                Medicamento.criar(mariaPid, cardioCentro.id, "Losartana potássica", "50 mg",
                        "Tomar 1 comprimido por via oral, uma vez ao dia, pela manhã.", "30 comprimidos",
                        diasHist.get(6).toString()),
                Medicamento.criar(mariaPid, juliana.id, "Metformina", "850 mg",
                        "Tomar 1 comprimido após o almoço e 1 após o jantar.", "60 comprimidos", dataPresc),
                Medicamento.criar(mariaPid, juliana.id, "Dipirona sódica", "500 mg",
                        "Tomar 1 comprimido a cada 6 horas em caso de dor ou febre. Não exceder 4 ao dia.",
                        "10 comprimidos", dataPresc),
                Medicamento.criar(mariaPid, juliana.id, "Omeprazol", "20 mg",
                        "Tomar 1 cápsula em jejum, 30 minutos antes do café da manhã.", "28 cápsulas",
                        dataPresc)));

        // 10) Notificações
        long hora = 60L * 60L * 1000L;
        String dataBr = DateUtils.formatarBr(dataProxima);
        db.notificacaoDao().inserirTodos(Arrays.asList(
                Notificacao.criar(mariaUid, "Consulta confirmada",
                        "Sua consulta de Clínica Geral com Dra. Juliana Costa na UBS Centro foi confirmada para "
                                + dataBr + " às 09:30.", TipoNotificacao.CONFIRMACAO, agora - 2 * hora, false,
                        futurasIds.get(0)),
                Notificacao.criar(mariaUid, "Lembrete de atendimento",
                        "Sua consulta é em " + dataBr + ". Chegue 15 minutos antes e leve documento com foto e o Cartão SUS.",
                        TipoNotificacao.LEMBRETE, agora - hora, false, futurasIds.get(0)),
                Notificacao.criar(mariaUid, "Agendamento alterado",
                        "Sua consulta de Dermatologia foi cancelada. Você pode reagendar pelo aplicativo.",
                        TipoNotificacao.ALTERACAO, agora - 24 * hora, true, futurasIds.get(2)),
                Notificacao.criar(mariaUid, "Resultado de exame disponível",
                        "O resultado do exame Hemograma completo já está disponível.",
                        TipoNotificacao.RESULTADO, agora - 72 * hora, false, exameIds.get(0)),
                Notificacao.criar(mariaUid, "Campanha de vacinação",
                        "A UBS Centro está realizando a campanha de vacinação contra a gripe. Procure a unidade.",
                        TipoNotificacao.AVISO, agora - 120 * hora, true, null),
                Notificacao.criar(gestorUid, "Indicadores atualizados",
                        "Os indicadores da rede foram atualizados com os atendimentos do período.",
                        TipoNotificacao.AVISO, agora - 3 * hora, false, null)));
    }

    // ------------------------------------------------------------------

    private static Usuario novoUsuario(String nome, String cpf, String email, String telefone,
                                       String perfil, long agora) {
        Usuario u = new Usuario();
        u.nome = nome;
        u.cpf = cpf;
        u.email = email;
        u.telefone = telefone;
        u.perfil = perfil;
        u.senhaSalt = PasswordUtils.gerarSalt();
        u.senhaHash = PasswordUtils.gerarHash(SENHA_DEMO, u.senhaSalt);
        u.criadoEm = agora;
        return u;
    }

    private static Consulta realizada(long pacienteId, Profissional prof, LocalDate data, String hora,
                                      String tipo, int espera, String obs) {
        Consulta c = Consulta.criar(pacienteId, prof, data.toString(), hora, StatusConsulta.REALIZADA, tipo);
        c.tempoEsperaMin = espera;
        c.observacoes = obs;
        return c;
    }

    /** Tempo de espera decrescente ao longo do período (~37% de redução, como no banner). */
    private static int tempoEspera(Random rnd, int dia, int totalDias) {
        double p = totalDias > 1 ? dia / (double) (totalDias - 1) : 1;
        double base = 55 - 26.4 * p;
        return Math.max(5, (int) Math.round(base + rnd.nextGaussian() * 6));
    }

    private static String tipoExame(String nome) {
        if (nome.startsWith("Raio-X") || nome.startsWith("Ultrassonografia")) return "Imagem";
        if (nome.equals("Eletrocardiograma")) return "Cardiológico";
        return "Laboratorial";
    }

    private static String chave(long profissionalId, String data, String hora) {
        return profissionalId + "|" + data + "|" + hora;
    }

    private static String sortear(Random rnd, String[] opcoes) {
        return opcoes[rnd.nextInt(opcoes.length)];
    }

    private static String gerarCpfUnico(Random rnd, Set<String> usados) {
        while (true) {
            int[] d = new int[11];
            for (int i = 0; i < 9; i++) d[i] = rnd.nextInt(10);
            d[9] = Validators.calcularDigitoCpf(d, 9);
            d[10] = Validators.calcularDigitoCpf(d, 10);
            StringBuilder sb = new StringBuilder();
            for (int x : d) sb.append(x);
            String cpf = sb.toString();
            if (Validators.isCpfValido(cpf) && usados.add(cpf)) return cpf;
        }
    }
}
