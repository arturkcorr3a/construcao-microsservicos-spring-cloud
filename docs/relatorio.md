# Relatório do projeto: Microsserviços de Peças, Clientes e Representantes

**Disciplina:** Construção de Software (PUCRS)
**Data:** 30/09/2026
**Stack:** Java 17 (build em JDK 21/25) · Maven · Spring Boot 4.0.8 · Spring Cloud 2025.1.3

Este relatório descreve tudo o que foi construído no projeto, na ordem em que foi feito: as decisões de
arquitetura, os problemas encontrados e como cada entrega foi verificada.

---

## Sumário

1. [Visão geral](#1-visão-geral)
2. [Etapa 1: microsserviços com Spring Cloud](#2-etapa-1-microsserviços-com-spring-cloud)
3. [Etapa 2: orquestração com Docker](#3-etapa-2-orquestração-com-docker)
4. [Etapa 3: observabilidade (Prometheus + Grafana)](#4-etapa-3-observabilidade-prometheus--grafana)
5. [Etapa 4: testes unitários e de integração](#5-etapa-4-testes-unitários-e-de-integração)
6. [Etapa 5: testes de mutação (PIT)](#6-etapa-5-testes-de-mutação-pit)
7. [Problemas encontrados e soluções](#7-problemas-encontrados-e-soluções)
8. [Resumo das verificações](#8-resumo-das-verificações)
9. [Estrutura final do projeto](#9-estrutura-final-do-projeto)
10. [Como executar](#10-como-executar)
11. [Fora do escopo / possíveis evoluções](#11-fora-do-escopo--possíveis-evoluções)

---

## 1. Visão geral

O sistema tem 3 microsserviços de domínio e 3 componentes de infraestrutura Spring Cloud, que
implementam os padrões **API Gateway**, **Centralized Configuration** e **Service Discovery (Eureka)**.
Depois vieram orquestração com Docker, métricas com Prometheus e Grafana, testes automatizados e
testes de mutação.

```
                         ┌──────────────────┐
   Cliente / Postman ──▶ │  gateway :8080   │ ── lb://<servico> (resolve via Eureka)
                         └────────┬─────────┘
             ┌────────────────────┼────────────────────┐
             ▼                    ▼                    ▼
     pecas-service:8081   clientes-service:8082   representantes-service:8083
             │                    │                    │
             └──── registram-se no ──▶ eureka-server :8761
             └──── buscam config em ─▶ config-server :8888 ──▶ config-repo/*.yml

   prometheus :9090 ── descobre os serviços pelo Eureka e coleta /actuator/prometheus
   grafana    :3000 ── dashboard provisionado a partir do Prometheus
```

| Componente               | Porta | Papel |
|--------------------------|-------|-------|
| `config-server`          | 8888  | Configuração centralizada (backend *native*, pasta `config-repo/`) |
| `eureka-server`          | 8761  | Service discovery |
| `gateway`                | 8080  | Único ponto de entrada da API |
| `pecas-service`          | 8081  | Cadastro e consulta de peças |
| `clientes-service`       | 8082  | Cadastro e consulta de clientes |
| `representantes-service` | 8083  | Cadastro e consulta de representantes comerciais |
| `prometheus`             | 9090  | Coleta de métricas |
| `grafana`                | 3000  | Visualização (admin/admin) |

---

## 2. Etapa 1: microsserviços com Spring Cloud

### Requisitos atendidos

| # | Funcionalidade | Endpoint (via gateway) |
|---|----------------|------------------------|
| 1 | Cadastrar peças (id, nome, descrição) | `POST /pecas` |
| 2 | Consultar peças por nome e por id | `GET /pecas/busca?nome=` · `GET /pecas/{id}` |
| 3 | Listar todas as peças | `GET /pecas` |
| 4 | Cadastrar clientes (CPF, nome) | `POST /clientes` |
| 5 | Consultar clientes por nome e por CPF | `GET /clientes/busca?nome=` · `GET /clientes/{cpf}` |
| 6 | Listar todos os clientes | `GET /clientes` |
| 7 | Cadastrar representantes (CPF, nome) | `POST /representantes` |
| 8 | Consultar representantes por nome e por CPF | `GET /representantes/busca?nome=` · `GET /representantes/{cpf}` |
| 9 | Listar todos os representantes | `GET /representantes` |

Códigos de resposta: `201` no cadastro (com header `Location`), `400` para dados inválidos, `404` para
registro inexistente e `409` para id/CPF já cadastrado. A busca por nome aceita parte do nome e não
diferencia maiúsculas de minúsculas.

### Decisões

- **"Restaurantes" → Representantes.** O enunciado pede os microsserviços "Peças, Clientes e
  Restaurantes", mas as funcionalidades 7 a 9 descrevem representantes comerciais e nada fala de
  restaurantes. Por isso o terceiro serviço foi interpretado como **Representantes**.
- **Base nos templates** de `templates/`: `eureka/`, `gs-gateway/` e `gs-centralized-configuration/`.
  Foi usado o par Boot 4.0.8 + Cloud 2025.1.3 do template do gateway.
- **Config server com backend *native*.** O template lia um repositório git em `~/Desktop/config`.
  Aqui a configuração fica em `config-repo/`, dentro do projeto, para que ele funcione sozinho.
  Cada serviço mantém localmente só o nome e o `spring.config.import`; portas, Eureka, banco e rotas
  vêm do config server.
- **Gateway com `lb://`.** As rotas (`/pecas/**`, `/clientes/**`, `/representantes/**`) resolvem as
  instâncias pelo Eureka.
- **Persistência:** H2 em memória, um banco por serviço. Os dados somem quando o serviço reinicia.
- **POM agregador** na raiz (`packaging pom`). Cada módulo tem o `spring-boot-starter-parent` como
  parent, como nos templates.

### Coleção do Postman

`postman/pecas-clientes-representantes.postman_collection.json` tem 24 requisições e 36 asserções,
em 3 pastas. Todas usam `{{baseUrl}} = http://localhost:8080`, ou seja, **só o gateway**. Cada pasta
cobre: cadastrar (2x), duplicado (409), sem nome (400), listar, consultar por id/CPF, id/CPF
inexistente (404) e busca por nome. Ids, CPFs e sufixos de nome são gerados aleatoriamente em
scripts de pré-requisição, então a coleção pode ser rodada várias vezes sem reiniciar os serviços.

### Verificação

- `mvn package` compilou os 6 módulos. Os serviços subiram na ordem config → eureka → serviços →
  gateway, e os 4 clientes se registraram no Eureka nas portas vindas do config server.
- `newman run` na coleção: **24/24 requisições e 36/36 asserções**, duas execuções seguidas.
  Também houve conferência manual com `curl` pelo gateway.

---

## 3. Etapa 2: orquestração com Docker

### Entregáveis

- **`Dockerfile` genérico** (multi-stage), que gera a imagem de qualquer módulo via `--build-arg MODULE=`:
  - build com `maven:3.9-eclipse-temurin-21`, usando cache do `~/.m2` compartilhado entre os módulos
    (`--mount=type=cache,sharing=locked`);
  - runtime com `eclipse-temurin:21-jre` + `curl` (usado nos healthchecks).
- **`docker-compose.yml`** com os 6 serviços (8 depois da etapa 3):
  - `config-server` e `eureka-server` têm healthcheck, e os demais só iniciam depois que os dois ficam
    `service_healthy`, com `restart: on-failure` como garantia extra;
  - só são publicadas no host as portas 8080, 8761 e 8888. **Os microsserviços de domínio não publicam
    portas**, então só são acessíveis pelo gateway;
  - `config-repo/` é montado no config server com `:ro,z`. O `z` é necessário por causa do SELinux do
    Fedora.
- **`.dockerignore`** exclui `target/`, `templates/` e `postman/`.

### Ajuste no código

Dentro da rede do compose, os serviços se encontram pelo nome, não por `localhost`. Os endereços
passaram a ser configuráveis, com `localhost` como padrão (a execução local continua igual):

- `spring.config.import: configserver:${CONFIG_SERVER_URL:http://localhost:8888}`
- `eureka.client.service-url.defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}`

### Verificação

- As 6 imagens foram construídas. O `docker compose up --wait` subiu a infraestrutura antes dos
  serviços, e os 4 clientes se registraram no Eureka com o IP de cada container.
- Newman pelo gateway em Docker: **36/36 asserções**. O acesso direto à porta 8081 pelo host foi
  **recusado**, como esperado.

---

## 4. Etapa 3: observabilidade (Prometheus + Grafana)

### Métricas nos serviços

- Os 6 módulos ganharam `spring-boot-starter-actuator` e `micrometer-registry-prometheus`, e cada um
  expõe `/actuator/prometheus`. Esse endpoint **não** é roteado pelo gateway.
- Configuração (em `config-repo/application.yml` e local no config-server e no Eureka):
  - tag comum `application=${spring.application.name}`;
  - histograma de `http.server.requests`, que permite calcular percentis (p95).
- **Métricas de negócio customizadas**, seguindo o padrão `Counter` do material de apoio:
  - `pecas_cadastro_total{resultado="sucesso"|"duplicado"}`
  - `clientes_cadastro_total{...}` e `representantes_cadastro_total{...}`

### Prometheus (`observability/prometheus/prometheus.yml`)

- Job `servicos` com **`eureka_sd_configs`**: o Prometheus descobre o gateway e os microsserviços pelo
  próprio Eureka, então réplicas novas entram na coleta sem mudar configuração. O label `servico` é
  derivado do nome da aplicação no Eureka.
- Job `infraestrutura` com endereços fixos para o config-server e o Eureka, que não se registram no
  Eureka.
- Intervalo de coleta: 5s.

### Grafana (`observability/grafana/`)

- Datasource do Prometheus e dashboard **provisionados**: são criados sozinhos quando o container sobe.
- Dashboard *Microsserviços: Peças, Clientes e Representantes* (pasta *Atividade*, também definido como
  página inicial), com filtro por serviço e 10 painéis:
  - serviços no ar e cadastros com sucesso;
  - requisições por rota e por status HTTP no gateway;
  - requisições/s e taxa de erros 4xx/5xx por serviço;
  - latência p95;
  - cadastros por minuto e resultado;
  - heap da JVM e CPU.

### Achado durante a implementação

No gateway (WebFlux), a métrica padrão `http_server_requests` registra todas as chamadas com
`uri="UNKNOWN"`. Os painéis do gateway usam então `spring_cloud_gateway_requests_seconds_*`, que tem
`routeId` e `httpStatusCode`. A escolha foi feita depois de inspecionar as métricas reais no
Prometheus.

### Verificação

- Prometheus: **6/6 alvos `up`**, 4 descobertos via Eureka e 2 fixos. Os contadores customizados
  bateram exatamente com a coleção: 2 sucessos e 1 duplicado por serviço a cada execução.
- Grafana, conferido pela API:
  - o datasource responde ("Successfully queried the Prometheus API");
  - o dashboard foi provisionado e é a página inicial;
  - **os 10 painéis retornam séries**.
- Os gráficos não foram inspecionados visualmente; a validação foi feita pela API do Grafana.

---

## 5. Etapa 4: testes unitários e de integração

### Refatoração em camadas

Antes, cada controller chamava o `JpaRepository` direto: não havia camada de serviço, nem código de
persistência com lógica própria para testar isolado. Para ser possível testar **serviços**,
**controllers isolando o framework web** e **persistência isolando o framework e o BD**, cada serviço
foi reorganizado:

```
controller/PecaController        HTTP: rotas, 201 + Location, 404, 409 via @ExceptionHandler
service/PecaService              regra de duplicado + contadores Micrometer
repository/PecaRepository        interface de persistência usada pelo serviço (sem JPA)
persistence/PecaRepositoryJpa    implementação: converte Peca <-> PecaEntity e delega ao Spring Data
persistence/PecaJpaRepository    Spring Data JPA
persistence/PecaEntity           @Entity
model/Peca                       modelo de domínio (sem anotações JPA)
```

Clientes e Representantes seguem o mesmo padrão, com `cpf` (String) como chave. Os dois foram
gerados a partir de um mesmo modelo parametrizado.

A API não mudou (rotas, status, JSON), com uma exceção: o corpo da resposta **409** passou a seguir o
formato padrão `ProblemDetail` (RFC 9457) em vez do JSON de erro padrão do Spring Boot. O status
continua 409, e a coleção do Postman continua passando.

### Testes unitários: JUnit 5 + Mockito, sem contexto Spring, padrão AAA, `@DisplayName` em português

| Classe (por serviço)  | O que fica isolado | Testes |
|-----------------------|--------------------|-------:|
| `*ServiceTest`        | Repositório mockado (`@Mock`); métricas num `SimpleMeterRegistry` em memória | 6 |
| `*ControllerTest`     | **Framework web**: métodos chamados direto (`@InjectMocks`), sem servlet/MockMvc | 7 |
| `*RepositoryJpaTest`  | **Framework de persistência e BD**: `JpaRepository` mockado; testa conversão e delegação | 7 |

### Testes de integração: slices do Spring Boot, H2 em memória

| Classe (por serviço)   | Anotação | O que integra | Testes |
|------------------------|----------|---------------|-------:|
| `*RepositoryJpaIT`     | `@DataJpaTest` + `@Import` | JPA/Hibernate real + H2, rollback por teste | 5 |
| `*ControllerWebMvcIT`  | `@WebMvcTest` + `@MockitoBean` | Spring MVC real: rotas, JSON, validação 400, 404, 409 | 8 |
| `*ApiIT`               | `@SpringBootTest` + `@AutoConfigureMockMvc` | Todas as camadas reais, incluindo as métricas | 2 |

### Infraestrutura de testes

- `src/test/resources/application.yml` desliga o config server e o Eureka, então os testes rodam sem
  nenhuma infraestrutura no ar.
- Os `*Test` rodam no Surefire (`mvn test`). Os `*IT` rodam no **Failsafe** (`mvn verify`).
- Dependências no Boot 4 (modular): `spring-boot-starter-webmvc-test` e
  `spring-boot-starter-data-jpa-test`. `@MockBean` foi substituído por `@MockitoBean`.

### Verificação

- `mvn verify` na raiz: **102 testes** nesta etapa (57 unitários + 45 de integração), todos passando.
  Os unitários levam menos de 1s por serviço.
- **Mutação manual**: invertendo o `if` do duplicado no `PecaService`, 2 testes falharam. Depois o
  arquivo foi restaurado e conferido pelo checksum.
- Regressão em Docker: newman **36/36**, e as métricas continuaram chegando ao Prometheus com os
  mesmos nomes.

---

## 6. Etapa 5: testes de mutação (PIT)

### Configuração

Profile `mutacao` nos 3 POMs de serviço, que não pesa no build normal:

```bash
mvn test -Pmutacao
```

| Item            | Valor |
|-----------------|-------|
| Ferramenta      | `pitest-maven` 1.30.0 + `pitest-junit5-plugin` 1.2.3 (funcionou com o Maven rodando em Java 25) |
| Classes mutadas | `controller.*`, `service.*` e `persistence.*`; `model` e `*Application` ficam de fora |
| Testes usados   | só os unitários (`*Test`); os `*IT` subiriam o Spring a cada mutante |
| Mutadores       | `STRONGER` |
| Relatórios      | HTML + XML em `target/pit-reports/`, sem timestamp |
| Limite          | `mutationThreshold = 100`: o build falha se algum mutante sobreviver |

### Resultado

| Serviço         | Mutantes | Mortos | Força dos testes | Cobertura de linhas |
|-----------------|---------:|-------:|-----------------:|--------------------:|
| pecas           | 25 | 25 | 100% | 96% (49/51) |
| clientes        | 24 | 24 | 100% | 96% (47/49) |
| representantes  | 24 | 24 | 100% | 96% (47/49) |

As linhas sem cobertura são o construtor vazio `protected` das entidades JPA, usado só pelo Hibernate.
Isso foi conferido no relatório HTML: o construtor não tem lógica nem gera mutantes.

### Defeito real encontrado

A primeira execução deu **96%**, com 1 sobrevivente por serviço, o mesmo nos três:
`existePorId`/`existePorCpf` → *"replaced boolean return with true"*. Os testes só verificavam o caso
verdadeiro. Se o método passasse a retornar sempre `true`, **todo cadastro seria rejeitado como
duplicado** sem que nenhum teste unitário percebesse. O problema foi corrigido com um teste para o
caso falso (`*RepositoryJpaTest.existePor*_inexistente_retornaFalso`). Com isso, os testes unitários
passaram a **60 (20 por serviço)** e o total a **105**.

### Verificação

- `mvn test -Pmutacao`: BUILD SUCCESS, 100% nos 3 serviços.
- **O limite funciona:** com a asserção nova desativada temporariamente, o build falhou com
  *"Mutation score of 96 is below threshold of 100"*. O arquivo foi restaurado e conferido pelo
  checksum.
- `mvn verify` sem o profile: 105 testes passando, sem rodar o PIT.

---

## 7. Problemas encontrados e soluções

| Problema | Causa | Solução |
|----------|-------|---------|
| Build offline (`mvn -o`) falhou | Dependências do config server não estavam no `~/.m2`, porque os templates tinham sido compilados com Gradle | Build online |
| Gateway respondia **503** logo após subir | O gateway busca o registro do Eureka a cada 30s e guarda as instâncias em cache por ~35s | `registry-fetch-interval-seconds: 5` e `loadbalancer.cache.ttl: 5s` no `config-repo/gateway.yml`. Ainda existe uma janela curta na subida, documentada no README |
| `pkill -f <jar>` encerrou o próprio shell | O padrão aparecia na linha de comando do próprio shell | Processos passaram a ser encerrados pelo PID |
| `permission denied` no socket do Docker | Usuário fora do grupo `docker` | Usuário adicionado ao grupo. Sessões antigas precisam de `newgrp docker`, `sg docker -c` ou novo login |
| Timeout ao baixar a imagem do Grafana | Instabilidade no Docker Hub | Novo `docker compose pull` (funcionou na 2ª tentativa) |
| Métricas HTTP do gateway com `uri="UNKNOWN"` | Comportamento do gateway WebFlux | Painéis do gateway usam `spring_cloud_gateway_requests_seconds_*` |
| Variável com vários argumentos não funcionou no zsh | O zsh não separa variáveis em argumentos como o bash | Verificação do Grafana reescrita em Python |
| `cd` para `src/test` falhou | O diretório ainda não existia | `mkdir -p` antes. Nenhum arquivo foi criado fora do lugar, o que foi conferido |

---

## 8. Resumo das verificações

| O que foi verificado | Como | Resultado |
|----------------------|------|-----------|
| API pelo gateway (local) | newman, 2 execuções | 24 requisições, 36/36 asserções |
| API pelo gateway (Docker) | newman | 36/36; acesso direto à 8081 recusado |
| Service discovery | API do Eureka | 4 clientes registrados |
| Coleta de métricas | API do Prometheus | 6/6 alvos `up`; contadores batem com a coleção |
| Dashboard | API do Grafana | Datasource ok; 10/10 painéis com dados |
| Testes unitários + integração | `mvn verify` | 105 testes (60 + 45), 0 falhas |
| Testes de mutação | `mvn test -Pmutacao` | 73/73 mutantes mortos (100%) |
| Limite de mutação | Asserção desativada temporariamente | Build falhou como esperado |
| Regressão após a refatoração | newman em Docker + Prometheus | 36/36; métricas inalteradas |

---

## 9. Estrutura final do projeto

```
atividade-completa/
├── pom.xml                       # agregador (6 módulos)
├── Dockerfile                    # imagem genérica (ARG MODULE)
├── docker-compose.yml            # 6 serviços + Prometheus + Grafana
├── .dockerignore
├── README.md
├── docs/relatorio.md             # este relatório
├── config-repo/                  # configuração centralizada
│   ├── application.yml           # comum: Eureka, H2, actuator/métricas
│   ├── gateway.yml               # porta + rotas lb://
│   └── {pecas,clientes,representantes}-service.yml
├── config-server/  eureka-server/  gateway/
├── pecas-service/                # e clientes-service/, representantes-service/
│   ├── pom.xml                   # testes, failsafe, profile "mutacao"
│   └── src/
│       ├── main/java/.../{controller,service,repository,persistence,model}
│       └── test/
│           ├── java/.../{controller,service,persistence}/*Test, *IT, *ApiIT
│           └── resources/application.yml
├── observability/
│   ├── prometheus/prometheus.yml
│   └── grafana/{provisioning/{datasources,dashboards},dashboards/microsservicos.json}
├── postman/pecas-clientes-representantes.postman_collection.json
└── templates/                    # material de referência (não modificado)
```

---

## 10. Como executar

```bash
# Tudo em Docker (serviços + Prometheus + Grafana)
docker compose up --build -d
#   API:        http://localhost:8080   (esperar ~30s até os serviços se registrarem)
#   Eureka:     http://localhost:8761
#   Prometheus: http://localhost:9090
#   Grafana:    http://localhost:3000   (admin/admin)
docker compose down

# Local, sem Docker (a partir da raiz; nesta ordem)
mvn package -DskipTests
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar
java -jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar
java -jar pecas-service/target/pecas-service-0.0.1-SNAPSHOT.jar          # e os outros 2 serviços
java -jar gateway/target/gateway-0.0.1-SNAPSHOT.jar

# Testes
mvn test                  # unitários
mvn verify                # unitários + integração
mvn test -Pmutacao        # unitários + mutação (PIT)
npx newman run postman/pecas-clientes-representantes.postman_collection.json   # API via gateway
```

---

## 11. Fora do escopo / possíveis evoluções

- **Logs estruturados (JSON) e tracing distribuído (OTLP/Jaeger):** aparecem no material de apoio da
  etapa de observabilidade, mas não foram pedidos; só as métricas foram implementadas.
- **Persistência durável:** o H2 em memória perde os dados a cada reinício. Um banco real (ex.:
  PostgreSQL em container) seria o próximo passo.
- **Testes na imagem Docker:** o `Dockerfile` usa `-DskipTests`, então os testes rodam no `mvn verify`
  e não no build da imagem.
- **Cobertura com JaCoCo/SonarQube:** citados no material de testes, mas não pedidos.
- **Healthcheck dos serviços de domínio e do gateway no compose:** hoje só a infraestrutura tem
  healthcheck, por isso existe a janela de ~30s até o gateway enxergar os serviços.
