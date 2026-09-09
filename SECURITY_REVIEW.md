# Revisão de Segurança

Data da revisão: 9 de setembro de 2026.

## Escopo e metodologia

A revisão cobriu código Java, configuração Spring, endpoints HTTP, autenticação,
autorização, persistência, tratamento de erros, dependências Maven e automações do GitHub.

Foram usados revisão manual orientada ao OWASP API Security Top 10, testes com MockMvc,
SBOM CycloneDX, consulta à API OSV e OWASP Dependency-Check 12.2.2. CodeQL com consultas
`security-extended` e Dependabot foram configurados para análise contínua.

Esta é uma análise pontual, não uma garantia de ausência de vulnerabilidades.

## Modelo de ameaça

Os ativos principais são a integridade e a disponibilidade das tarefas, as credenciais
administrativas e os detalhes internos da aplicação.

```text
Cliente HTTP → Spring Security → Controller → Service → Repository JPA → Banco
```

O atacante considerado pode enviar requisições e JSON arbitrários, repetir chamadas e tentar
acessar funções sem autenticação ou sem o papel necessário.

## Achados e tratamento

| Risco | Achado | Tratamento | Estado |
| ----- | ------ | ---------- | ------ |
| Alto | CRUD sem autenticação | autenticação stateless; escrita somente para `ADMIN` | Mitigado |
| Alto | Oito advisories transitivos | versões corrigidas, Dependabot e SBOM | Mitigado |
| Alto | H2 Console exposto por padrão | restrito ao profile `dev` e autenticado | Mitigado |
| Médio | Listagem sem limite | paginação padrão 20 e máximo 100 | Mitigado |
| Médio | Swagger exposto sempre | desabilitado por padrão | Mitigado |
| Médio | Campos JSON desconhecidos aceitos | rejeição explícita e DTOs | Mitigado |
| Baixo | `PUT` parcial | status obrigatório no DTO de atualização | Mitigado |
| Baixo | IDs não positivos | validação `@Positive` | Mitigado |
| Informativo | Artefatos com dados locais | excluídos via `.gitignore` | Mitigado |

## Análise de dependências

O primeiro SBOM continha 81 componentes. A consulta OSV encontrou três advisories para
Jackson Databind, três para Tomcat Embed Core, um para Commons Lang e um para Log4j API.

- Jackson: `GHSA-5gvw-p9qm-jgwh`, `GHSA-5jmj-h7xm-6q6v`,
  `GHSA-mhm7-754m-9p8w`
- Tomcat: `GHSA-9xv2-5v5q-p794`, `GHSA-gcx9-497g-6cp6`,
  `GHSA-h3x4-894j-xpx5`
- Commons Lang: `GHSA-j288-q9x7-2f5v`
- Log4j API: `GHSA-qv9r-c865-cp47`

As versões foram ajustadas para Jackson 2.21.6, Tomcat 10.1.59, Commons Lang 3.18.0 e
Log4j 2.25.5, que contêm as correções indicadas. Os overrides devem ser removidos quando
o BOM de uma futura versão do Spring Boot passar a gerenciar versões iguais ou superiores.

Após a correção, uma nova consulta do SBOM final à API OSV analisou 81 componentes e não
encontrou associações com vulnerabilidades conhecidas. Esse resultado representa a base do OSV
na data da revisão e não substitui monitoramento contínuo.

A execução do OWASP Dependency-Check não terminou: sem API key, a NVD respondeu
`429 Too Many Requests` ao atualizar aproximadamente 389 mil registros. O profile Maven
`security` permanece configurado e deve ser executado com `NVD_API_KEY`.

## Controles positivos observados

- DTOs evitam exposição e alteração direta da entidade;
- Spring Data gera queries parametrizadas, sem concatenação de SQL;
- validação ocorre antes da persistência;
- respostas de erro não expõem stack traces;
- exceções inesperadas são registradas no servidor;
- limites de tamanho existem no DTO e no schema;
- CORS permanece fechado por padrão;
- segredos locais reais são ignorados pelo Git.

## Riscos residuais e decisões

| Risco residual | Recomendação para produção |
| -------------- | -------------------------- |
| HTTP Basic não oferece ciclo de vida de identidade | usar OIDC/OAuth 2.1 e tokens curtos |
| Basic depende de transporte seguro | terminar TLS no gateway e redirecionar HTTP |
| ausência de rate limiting | aplicar limite por identidade/IP no gateway |
| H2 é efêmero | usar PostgreSQL e migrations Flyway/Liquibase |
| não há propriedade por usuário | adicionar autorização por objeto se houver multiusuário |
| não há trilha de auditoria de negócio | registrar ator, ação, resultado e correlation ID |
| dependências mudam após esta fotografia | manter Dependabot, CodeQL e SCA recorrentes |

O CSRF está desabilitado porque a API é stateless e aceita corpos JSON. Como navegadores podem
reutilizar credenciais Basic, qualquer uso fora de clientes de API deve ser reavaliado junto
com CORS, TLS e o modelo de autenticação.

## Referências

- [OWASP API Security Top 10](https://owasp.org/API-Security/)
- [Spring Boot: H2 Web Console](https://docs.spring.io/spring-boot/reference/data/sql.html)
- [OSV: banco distribuído de vulnerabilidades](https://osv.dev/)
- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)
- [GitHub CodeQL](https://docs.github.com/en/code-security/code-scanning/introduction-to-code-scanning/about-code-scanning-with-codeql)

## Evidências de verificação

- testes automatizados de 401, 403, CRUD, paginação e validação;
- build limpo em Java 25 com 8 testes e nenhuma falha;
- cobertura JaCoCo de 88,76% das linhas, com piso de 80% aplicado pelo Maven;
- smoke test do JAR: `401` sem credenciais, paginação limitada, criação `201` e
  documentação/H2 autenticados;
- Maven Enforcer exige Java 25 e Maven 3.9 ou superior;
- JaCoCo produz relatório de cobertura durante `verify`;
- CycloneDX produz e incorpora o SBOM JSON ao artefato;
- CodeQL e Dependabot estão preparados em `.github/`;
- política de reporte responsável disponível em `SECURITY.md`.
