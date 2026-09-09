# Secure Task API

API REST para gestão de tarefas construída como projeto de portfólio, com foco em engenharia
backend, testes automatizados e segurança por padrão.

## Destaques

- Java 25 LTS e Spring Boot 3.5
- arquitetura em camadas: Controller → Service → Repository
- DTOs separados da entidade JPA
- autenticação HTTP Basic stateless e autorização por papel
- proteção CSRF por cookie e header nas operações de escrita
- validação de entrada e respostas de erro consistentes
- paginação, ordenação e filtro por status
- OpenAPI/Swagger no ambiente de desenvolvimento
- testes de integração, cobertura JaCoCo e SBOM CycloneDX
- cobertura de linhas de 89,08%, com limite mínimo de 80% aplicado no build
- CI, CodeQL `security-extended` e Dependabot
- revisão de segurança em [SECURITY_REVIEW.md](SECURITY_REVIEW.md)

## Stack

- Java 25 (LTS)
- Spring Boot 3.5.16
- Spring Web, Spring Security e Bean Validation
- Spring Data JPA e H2
- springdoc-openapi
- Maven Wrapper
- JUnit 5, MockMvc, Spring Security Test e JaCoCo

## Executando localmente

Pré-requisito: JDK 25. Não é necessário instalar Maven.

O profile `dev` habilita Swagger e H2 Console. Ambos continuam protegidos pela autenticação.

```powershell
# Windows
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

```bash
# Linux/macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Credenciais locais de demonstração:

- usuário: `portfolio-admin`
- senha: `dev-only-password`

Serviços disponíveis:

- API: `http://localhost:8080/tasks`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- H2 Console: `http://localhost:8080/h2-console`

Sem o profile `dev`, a aplicação exige as variáveis `API_USERNAME` e `API_PASSWORD`, além de
manter Swagger e H2 Console desabilitados. Fora de localhost, HTTP Basic deve ser usado somente
atrás de TLS.

## Endpoints

| Método | Rota          | Acesso      | Descrição                                   |
| ------ | ------------- | ----------- | ------------------------------------------- |
| GET    | `/tasks`      | autenticado | Lista tarefas com filtro e paginação        |
| GET    | `/tasks/{id}` | autenticado | Busca uma tarefa por ID positivo            |
| GET    | `/csrf`       | autenticado | Emite o token CSRF para operações de escrita |
| POST   | `/tasks`      | `ADMIN`     | Cria uma tarefa                             |
| PUT    | `/tasks/{id}` | `ADMIN`     | Substitui os campos editáveis de uma tarefa |
| DELETE | `/tasks/{id}` | `ADMIN`     | Remove uma tarefa                           |

Parâmetros de listagem:

- `status`: `PENDING`, `IN_PROGRESS` ou `DONE`
- `page`: página baseada em zero
- `size`: padrão 20, máximo 100
- `sort`: campo e direção, por exemplo `sort=createdAt,desc`

```bash
curl -u portfolio-admin:dev-only-password \
  "http://localhost:8080/tasks?status=PENDING&page=0&size=20&sort=createdAt,desc"
```

### Criar uma tarefa

```bash
COOKIE_JAR=$(mktemp)
CSRF_TOKEN=$(curl --silent --cookie-jar "$COOKIE_JAR" \
  -u portfolio-admin:dev-only-password \
  http://localhost:8080/csrf | jq --raw-output .token)

curl -u portfolio-admin:dev-only-password \
  --cookie "$COOKIE_JAR" \
  -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -d '{"title":"Revisar Spring Security","description":"Validar controles","status":"PENDING"}'

rm "$COOKIE_JAR"
```

O exemplo usa `jq` para extrair o token. Requisições `POST`, `PUT` e `DELETE` exigem o cookie
e o header CSRF, além da autenticação.

O `status` é opcional na criação e assume `PENDING`. No `PUT`, ele é obrigatório para manter
a semântica de substituição completa.

## Segurança

- deny-by-default para rotas não mapeadas;
- autenticação stateless e autorização distinta para leitura e escrita;
- proteção CSRF com cookie `SameSite=Strict` e header `X-XSRF-TOKEN`;
- segredos externalizados por variáveis de ambiente;
- H2 Console, Swagger e SQL detalhado restritos ao profile `dev`;
- campos JSON desconhecidos são rejeitados;
- DTOs impedem alteração direta da entidade;
- queries JPA parametrizadas reduzem risco de SQL injection;
- limites de payload e paginação reduzem abuso de recursos;
- detalhes internos e stack traces não são devolvidos ao cliente;
- CodeQL, Dependabot, OWASP Dependency-Check e SBOM apoiam o processo de segurança.

Consulte [SECURITY.md](SECURITY.md) para reporte responsável e
[SECURITY_REVIEW.md](SECURITY_REVIEW.md) para achados e riscos residuais.

## Testes e análise

```powershell
# Testes
.\mvnw.cmd test

# Build, cobertura e SBOM
.\mvnw.cmd verify

# SCA completo; uma chave gratuita da NVD é fortemente recomendada
.\mvnw.cmd verify -Psecurity
```

Artefatos gerados em `target/`:

- cobertura: `site/jacoco/index.html`
- SBOM incorporado: `classes/META-INF/sbom/application.cdx.json`
- Dependency-Check: `dependency-check-report.html`

## Estrutura

```text
src/main/java/com/joaoandrade/todoapi/
├── config/       # segurança e OpenAPI
├── controller/   # contrato HTTP
├── dto/          # entrada, saída e paginação
├── exception/    # tratamento centralizado
├── model/        # entidade e enum
├── repository/   # persistência JPA
└── service/      # regras de negócio e transações
```

## Limitações conhecidas

O projeto usa H2 em memória e um usuário configurado pelo Spring Boot para demonstração. Um
cenário de produção deveria adotar banco persistente com migrations, provedor de identidade,
tokens de curta duração, TLS no gateway e rate limiting distribuído.

## Licença

Distribuído sob a licença MIT. Consulte [LICENSE](LICENSE).

## Autor

João Andrade
