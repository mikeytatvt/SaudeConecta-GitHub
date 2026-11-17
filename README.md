# Saúde Conecta

Aplicativo Android acadêmico para gestão e acompanhamento de atendimentos na Atenção Básica. O repositório reúne, em uma única aplicação, a evolução das etapas 1 a 10 do projeto.

> Projeto desenvolvido para fins educacionais. Os dados apresentados pelo aplicativo são demonstrativos e não devem ser usados para decisões clínicas ou para armazenar dados reais de pacientes.

## Funcionalidades reunidas

### Área do paciente
- Login, cadastro e recuperação de senha.
- Tela inicial e navegação pelas funcionalidades.
- Agendamento de consultas.
- Consulta dos atendimentos agendados e respectivos detalhes.
- Consulta de exames e visualização/compartilhamento de resultados em PDF.
- Consulta de medicamentos.
- Busca e consulta de unidades de saúde.
- Notificações e lembretes de atendimento.

### Área do gestor/administrador
- Dashboard com indicadores e gráficos.
- Gerenciamento de unidades de saúde.
- Gerenciamento de profissionais.
- Consulta e gerenciamento de pacientes.
- Gerenciamento de agendamentos.
- Relatórios em PDF.

## Tecnologias

- Java 17
- Android SDK (compileSdk 34; minSdk 26)
- AndroidX, AppCompat, Material Components e ConstraintLayout
- Room/SQLite para persistência local
- Retrofit, Gson e OkHttp para integração REST
- MPAndroidChart para gráficos
- MySQL 8 (script de estrutura em `docs/mysql/saude_conecta_mysql.sql`)
- Gradle 8.7 / Android Gradle Plugin 8.5.2

## Como abrir

1. Instale o Android Studio e um JDK 17.
2. Abra a pasta raiz deste repositório no Android Studio.
3. Configure o Gradle 8.7 e o Android Gradle Plugin 8.5.2; instale os componentes do Android SDK solicitados.
4. Execute o app em um emulador ou dispositivo Android 8.0 (API 26) ou superior.

**Observação sobre o Wrapper:** o material original forneceu `gradle-wrapper.properties`, mas não incluiu o JAR nem os scripts `gradlew`/`gradlew.bat`. Por isso, o Wrapper não está completo neste pacote. É possível configurar o Gradle pelo ambiente Android Studio ou gerar o Wrapper localmente com Gradle 8.7 (`gradle wrapper --gradle-version 8.7`).

O projeto está configurado por padrão para usar o banco local Room/SQLite e dados demonstrativos (`USAR_API_REMOTA = false` em `AppConfig.java`). A integração Retrofit está preparada para uma API REST; para utilizá-la, é necessário disponibilizar o backend correspondente e configurar o endereço em `AppConfig.java`. O endereço `10.0.2.2` é destinado ao emulador Android e não representa um servidor público.

## Banco MySQL

O script `docs/mysql/saude_conecta_mysql.sql` contém a estrutura do banco MySQL 8 associada ao projeto. Ele pode ser usado como referência para a camada de backend. O aplicativo Android, por padrão, não depende desse banco remoto para iniciar, pois utiliza Room/SQLite localmente.

## Organização

```text
app/                  Código-fonte e recursos Android
docs/mysql/           Script SQL do banco MySQL
gradle/               Configurações do Gradle Wrapper
README.md             Documentação do projeto
```

Dentro de `app/src/main/java/br/edu/unicid/saudeconecta/`, o código está separado em `activities`, `adapters`, `api`, `database`, `models`, `repositories`, `utils` e `views`.

## Escopo e observações

- Este repositório consolida os arquivos incrementais das etapas 1 a 10 em uma única estrutura Android.
- A presença de telas e classes no projeto não significa, por si só, que exista um backend remoto implantado.
- Utilize somente dados fictícios em testes e demonstrações. Não publique informações pessoais, credenciais reais, tokens ou chaves de API.

## Licença

Ainda não foi definida uma licença para este projeto.
