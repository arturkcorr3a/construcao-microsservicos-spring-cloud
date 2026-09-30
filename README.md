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

## Testes (Postman)

Importe `postman/pecas-clientes-representantes.postman_collection.json` no Postman
e rode a coleção (Run collection). Todas as requisições usam `{{baseUrl}} = http://localhost:8080`,
ou seja, somente o gateway. Ids e CPFs são gerados aleatoriamente a cada execução,
então dá para rodar a coleção várias vezes sem reiniciar os serviços.

Pela linha de comando:

```bash
npx newman run postman/pecas-clientes-representantes.postman_collection.json
```
