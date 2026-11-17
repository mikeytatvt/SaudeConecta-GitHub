-- =====================================================================
-- SAÚDE CONECTA – Banco de dados MySQL 8 (para a API Spring Boot)
-- Mesma estrutura das tabelas Room do aplicativo Android.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS saude_conecta
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE saude_conecta;

CREATE TABLE usuarios (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome           VARCHAR(120) NOT NULL,
  cpf            CHAR(11)     NOT NULL UNIQUE,
  email          VARCHAR(120) NOT NULL UNIQUE,
  telefone       VARCHAR(20),
  senha_hash     VARCHAR(255) NOT NULL,
  senha_salt     VARCHAR(64)  NOT NULL,
  perfil         VARCHAR(10)  NOT NULL DEFAULT 'PACIENTE',
  ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
  criado_em      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ultimo_acesso  DATETIME     NULL,
  CONSTRAINT ck_usuarios_perfil CHECK (perfil IN ('PACIENTE','GESTOR'))
) ENGINE=InnoDB;

CREATE TABLE unidades_saude (
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome                   VARCHAR(100) NOT NULL UNIQUE,
  endereco               VARCHAR(200) NOT NULL,
  bairro                 VARCHAR(80),
  telefone               VARCHAR(20),
  horario_funcionamento  VARCHAR(80),
  servicos               VARCHAR(500),
  ativo                  BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE especialidades (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome       VARCHAR(60) NOT NULL UNIQUE,
  descricao  VARCHAR(200)
) ENGINE=InnoDB;

CREATE TABLE pacientes (
  id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id             BIGINT NULL UNIQUE,
  nome                   VARCHAR(120) NOT NULL,
  cpf                    CHAR(11)     NOT NULL UNIQUE,
  data_nascimento        DATE,
  telefone               VARCHAR(20),
  email                  VARCHAR(120),
  endereco               VARCHAR(200),
  cartao_sus             VARCHAR(20),
  unidade_referencia_id  BIGINT NULL,
  criado_em              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_pacientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE SET NULL,
  CONSTRAINT fk_pacientes_unidade FOREIGN KEY (unidade_referencia_id) REFERENCES unidades_saude(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE profissionais (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome              VARCHAR(120) NOT NULL,
  registro          VARCHAR(30)  NOT NULL UNIQUE,
  especialidade_id  BIGINT NOT NULL,
  unidade_id        BIGINT NOT NULL,
  ativo             BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_prof_especialidade FOREIGN KEY (especialidade_id) REFERENCES especialidades(id),
  CONSTRAINT fk_prof_unidade FOREIGN KEY (unidade_id) REFERENCES unidades_saude(id)
) ENGINE=InnoDB;

CREATE TABLE consultas (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  paciente_id          BIGINT NOT NULL,
  profissional_id      BIGINT NOT NULL,
  unidade_id           BIGINT NOT NULL,
  especialidade_id     BIGINT NOT NULL,
  data                 DATE   NOT NULL,
  hora                 TIME   NOT NULL,
  status               VARCHAR(12) NOT NULL DEFAULT 'AGENDADA',
  tipo_atendimento     VARCHAR(20) NOT NULL DEFAULT 'CONSULTA_MEDICA',
  observacoes          VARCHAR(500),
  motivo_cancelamento  VARCHAR(200),
  tempo_espera_min     INT NULL,
  criado_em            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  atualizado_em        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_consultas_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE,
  CONSTRAINT fk_consultas_prof FOREIGN KEY (profissional_id) REFERENCES profissionais(id),
  CONSTRAINT fk_consultas_unidade FOREIGN KEY (unidade_id) REFERENCES unidades_saude(id),
  CONSTRAINT fk_consultas_esp FOREIGN KEY (especialidade_id) REFERENCES especialidades(id),
  CONSTRAINT ck_consultas_status CHECK (status IN ('AGENDADA','REALIZADA','CANCELADA')),
  CONSTRAINT ck_consultas_tipo CHECK (tipo_atendimento IN ('CONSULTA_MEDICA','RETORNO','PROCEDIMENTO','ODONTOLOGICO')),
  INDEX idx_consultas_agenda (profissional_id, data, hora),
  INDEX idx_consultas_data (data),
  INDEX idx_consultas_status (status)
) ENGINE=InnoDB;

CREATE TABLE exames (
  id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
  paciente_id                  BIGINT NOT NULL,
  consulta_id                  BIGINT NULL,
  unidade_id                   BIGINT NULL,
  profissional_solicitante_id  BIGINT NULL,
  nome                         VARCHAR(120) NOT NULL,
  tipo                         VARCHAR(40),
  data_solicitacao             DATE,
  data_realizacao              DATE NULL,
  status                       VARCHAR(25) NOT NULL DEFAULT 'SOLICITADO',
  preparo                      VARCHAR(300),
  criado_em                    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_exames_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE,
  CONSTRAINT fk_exames_consulta FOREIGN KEY (consulta_id) REFERENCES consultas(id) ON DELETE SET NULL,
  CONSTRAINT fk_exames_unidade FOREIGN KEY (unidade_id) REFERENCES unidades_saude(id) ON DELETE SET NULL,
  CONSTRAINT fk_exames_prof FOREIGN KEY (profissional_solicitante_id) REFERENCES profissionais(id) ON DELETE SET NULL,
  CONSTRAINT ck_exames_status CHECK (status IN ('SOLICITADO','AGENDADO','REALIZADO','RESULTADO_DISPONIVEL'))
) ENGINE=InnoDB;

CREATE TABLE resultados_exames (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  exame_id        BIGINT NOT NULL UNIQUE,
  resumo          VARCHAR(300),
  valores         TEXT,
  observacao      VARCHAR(500),
  arquivo_pdf     VARCHAR(255),
  data_liberacao  DATE,
  CONSTRAINT fk_resultado_exame FOREIGN KEY (exame_id) REFERENCES exames(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE medicamentos (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  paciente_id      BIGINT NOT NULL,
  profissional_id  BIGINT NULL,
  nome             VARCHAR(120) NOT NULL,
  dosagem          VARCHAR(40),
  orientacao       VARCHAR(300),
  quantidade       VARCHAR(40),
  data_prescricao  DATE,
  ativo            BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_med_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id) ON DELETE CASCADE,
  CONSTRAINT fk_med_prof FOREIGN KEY (profissional_id) REFERENCES profissionais(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE notificacoes (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id     BIGINT NOT NULL,
  titulo         VARCHAR(120) NOT NULL,
  mensagem       VARCHAR(500) NOT NULL,
  tipo           VARCHAR(15)  NOT NULL DEFAULT 'AVISO',
  data_hora      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lida           BOOLEAN      NOT NULL DEFAULT FALSE,
  referencia_id  BIGINT NULL,
  CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
  CONSTRAINT ck_notif_tipo CHECK (tipo IN ('CONFIRMACAO','LEMBRETE','ALTERACAO','RESULTADO','AVISO'))
) ENGINE=InnoDB;

-- Dados básicos
INSERT INTO especialidades (nome, descricao) VALUES
 ('Clínica Geral','Atendimento clínico geral'), ('Pediatria','Saúde de crianças e adolescentes'),
 ('Cardiologia','Coração e sistema circulatório'), ('Dermatologia','Pele, cabelos e unhas'),
 ('Ginecologia','Saúde da mulher'), ('Ortopedia','Ossos, músculos e articulações'),
 ('Odontologia','Saúde bucal');

INSERT INTO unidades_saude (nome, endereco, bairro, telefone, horario_funcionamento, servicos) VALUES
 ('UBS Centro','Rua das Palmeiras, 120','Centro','(11) 3000-1001','Seg a Sex, 07h às 19h','Clínica Geral, Pediatria, Cardiologia, Dermatologia, Ginecologia, Odontologia'),
 ('UBS Norte','Av. dos Ipês, 845','Jardim Norte','(11) 3000-1002','Seg a Sex, 07h às 17h','Clínica Geral, Cardiologia, Ortopedia'),
 ('UBS Sul','Rua do Lago, 300','Vila Sul','(11) 3000-1003','Seg a Sex, 07h às 17h','Clínica Geral, Pediatria, Ortopedia'),
 ('UBS Leste','Av. Sol Nascente, 1500','Parque Leste','(11) 3000-1004','Seg a Sex, 07h às 19h','Clínica Geral, Dermatologia, Odontologia'),
 ('UBS Oeste','Rua das Acácias, 77','Vila Oeste','(11) 3000-1005','Seg a Sex, 07h às 17h','Clínica Geral, Ginecologia, Odontologia');
