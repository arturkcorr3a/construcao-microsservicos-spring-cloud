# Peças, Clientes e Representantes — Microsserviços com Spring Cloud

Java 17+ · Maven · Spring Boot 4.0.8 · Spring Cloud 2025.1.3

## Arquitetura

| Módulo                   | Porta | Papel                                                       |
|--------------------------|-------|-------------------------------------------------------------|
| `config-server`          | 8888  | Configuração centralizada (lê os arquivos de `config-repo/`) |
| `eureka-server`          | 8761  | Service discovery                                           |
| `gateway`                | 8080  | API Gateway — **único ponto de acesso dos clientes**        |
| `pecas-service`          | 8081  | Cadastro/consulta de peças                                  |
| `clientes-service`       | 8082  | Cadastro/consulta de clientes                               |
| `representantes-service` | 8083  | Cadastro/consulta de representantes comerciais              |

Cada serviço só guarda localmente o próprio nome e o endereço do config server
(`spring.config.import`). Portas, URL do Eureka, banco de dados e rotas do gateway
vêm de `config-repo/`. O gateway roteia com `lb://<servico>`, resolvendo as
instâncias pelo Eureka.

Os dados ficam em um H2 em memória (um banco por serviço) e são perdidos quando o serviço reinicia.

## Endpoints (via gateway, `http://localhost:8080`)

| Método | Caminho                          | Descrição                       |
|--------|----------------------------------|---------------------------------|
| POST   | `/pecas`                         | Cadastra `{id, nome, descricao}` |
| GET    | `/pecas`                         | Lista todas                     |
| GET    | `/pecas/{id}`                    | Consulta por id                 |
| GET    | `/pecas/busca?nome=...`          | Consulta por nome (parcial, sem diferenciar maiúsculas) |
| POST   | `/clientes`                      | Cadastra `{cpf, nome}`          |
| GET    | `/clientes`                      | Lista todos                     |
| GET    | `/clientes/{cpf}`                | Consulta por CPF                |
| GET    | `/clientes/busca?nome=...`       | Consulta por nome               |
| POST   | `/representantes`                | Cadastra `{cpf, nome}`          |
| GET    | `/representantes`                | Lista todos                     |
| GET    | `/representantes/{cpf}`          | Consulta por CPF                |
| GET    | `/representantes/busca?nome=...` | Consulta por nome               |

Respostas: `201` no cadastro, `409` para id/CPF já cadastrado, `400` para dados inválidos, `404` quando não encontrado.

## Como executar

```bash
mvn package -DskipTests
```

Suba **a partir da raiz do projeto** (o config server procura `./config-repo`), nesta ordem:

```bash
java -jar config-server/target/config-server-0.0.1-SNAPSHOT.jar
java -jar eureka-server/target/eureka-server-0.0.1-SNAPSHOT.jar
java -jar pecas-service/target/pecas-service-0.0.1-SNAPSHOT.jar
java -jar clientes-service/target/clientes-service-0.0.1-SNAPSHOT.jar
java -jar representantes-service/target/representantes-service-0.0.1-SNAPSHOT.jar
java -jar gateway/target/gateway-0.0.1-SNAPSHOT.jar
```

Painel do Eureka: http://localhost:8761

> Depois de subir um serviço, espere alguns segundos até ele aparecer no gateway
> (registro no Eureka + atualização do cache). Até lá, o gateway responde `503`.

## Como executar com Docker

```bash
docker compose up --build -d     # constroi as 6 imagens e sobe tudo
docker compose ps                # config-server e eureka-server devem ficar "healthy"
docker compose logs -f gateway   # acompanhar os logs de um servico
docker compose down              # derrubar tudo
```

- Um único `Dockerfile` genérico gera a imagem de cada módulo (`--build-arg MODULE=<modulo>`).
  O `docker-compose.yml` o reaproveita para os 6 serviços.
- Ordem de subida: os serviços de domínio e o gateway só iniciam depois que o config server
  e o Eureka passam no healthcheck.
- Dentro da rede do compose, os serviços se acham pelo nome (`config-server`, `eureka-server`).
  Isso é passado pelas variáveis `CONFIG_SERVER_URL` e `EUREKA_URL`; sem elas, o padrão é `localhost`.
- Portas publicadas no host: **8080** (gateway), 8761 (painel do Eureka) e 8888 (config server).
  Os microsserviços de domínio **não** publicam portas, então só dá para acessá-los pelo gateway.
- `config-repo/` é montado no config server, então dá para editar a configuração sem reconstruir a imagem.

A coleção do Postman funciona igual (`baseUrl = http://localhost:8080`).

## Observabilidade (Prometheus + Grafana)

Sobe junto com o `docker compose up --build -d`:

| Ferramenta | URL                   | Acesso      |
|------------|-----------------------|-------------|
| Prometheus | http://localhost:9090 | —           |
| Grafana    | http://localhost:3000 | admin/admin |

**Como as métricas chegam ao Prometheus**
- Todos os serviços usam `spring-boot-starter-actuator` + `micrometer-registry-prometheus`
  e expõem `/actuator/prometheus`. Esse endpoint **não** é roteado pelo gateway; só o Prometheus
  o acessa, dentro da rede do compose.
- O Prometheus descobre o gateway e os microsserviços **pelo Eureka** (`eureka_sd_configs`), então
  réplicas novas entram no scrape sozinhas. Config server e Eureka têm endereço fixo.
  Ver os alvos em http://localhost:9090/targets.
- Toda série recebe o label `servico` (e a tag `application`) com o nome do serviço.

**Métricas customizadas** (contadores nos controllers):

| Métrica                                          | Significado |
|--------------------------------------------------|-------------|
| `pecas_cadastro_total{resultado="sucesso"}`      | peças cadastradas |
| `pecas_cadastro_total{resultado="duplicado"}`    | tentativas com id já existente (409) |
| `clientes_cadastro_total{...}`                   | idem, para clientes |
| `representantes_cadastro_total{...}`             | idem, para representantes |

Métricas automáticas úteis: `http_server_requests_seconds_*` (por uri/status, com histograma para
percentis), `spring_cloud_gateway_requests_seconds_*` (por rota do gateway), `jvm_memory_used_bytes`,
`process_cpu_usage`.

**Dashboard**: o Grafana já sobe com o datasource do Prometheus e o dashboard
*Microsserviços: Peças, Clientes e Representantes* (pasta *Atividade*, também definido como página inicial),
com filtro por serviço e painéis de: serviços no ar, cadastros, requisições por rota do gateway,
status HTTP, requisições/s, taxa de erros, latência p95, heap e CPU.
Rode a coleção do Postman para gerar tráfego e ver os gráficos se mexerem.

Arquivos em `observability/`: `prometheus/prometheus.yml` e `grafana/` (provisioning + dashboard JSON).

## Testes (Postman)

Importe `postman/pecas-clientes-representantes.postman_collection.json` no Postman
e rode a coleção (Run collection). Todas as requisições usam `{{baseUrl}} = http://localhost:8080`,
ou seja, somente o gateway. Ids e CPFs são gerados aleatoriamente a cada execução,
então dá para rodar a coleção várias vezes sem reiniciar os serviços.

Pela linha de comando:

```bash
npx newman run postman/pecas-clientes-representantes.postman_collection.json
```
