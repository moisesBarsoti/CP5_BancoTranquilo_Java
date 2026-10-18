# BancoTranquilo

API REST de simulação de pagamentos bancários desenvolvida com **Java 17**, **Spring Boot 4.1.1** e **PostgreSQL** para integração com o projeto [MercadoExpress](https://github.com/moisesBarsoti/CP5_Java_MercadoExpress).

O serviço recebe solicitações de pagamento, registra a transação no banco de dados e devolve um resultado de aprovação ou recusa. A principal característica é a **idempotência persistente por pedido**: repetir a mesma solicitação não cria outra transação bancária.

## Tecnologias

- Java 17 (versão de compilação do projeto)
- Spring Boot 4.1.1 e Maven Wrapper
- Spring Web MVC e Bean Validation
- Spring Data JPA / Hibernate
- PostgreSQL (validado localmente com PostgreSQL 17)
- JUnit 5 e Mockito

## Regras do pagamento

| Condição | Resultado |
| --- | --- |
| Valor maior que zero e até **R$ 5.000,00** | `APPROVED` |
| Valor acima de **R$ 5.000,00** | `DECLINED` |
| Mesmo `orderId` e mesmo valor, repetidos | Retorna a transação já registrada |
| Mesmo `orderId` com valor diferente | HTTP `409 Conflict` |
| `orderId` ou valor inválido | Requisição rejeitada pela validação (`400 Bad Request`) |

**Importante:** trata-se de uma simulação acadêmica; nenhuma instituição financeira real é consultada. O status de uma transação existente é reutilizado, em vez de recalculado.

## Configuração local

### Pré-requisitos

- JDK 17 ou superior compatível com o projeto
- PostgreSQL em execução
- Banco de dados `banco_tranquilo` criado e usuário com permissões apropriadas

Crie o banco no PostgreSQL, usando uma conta com permissões:

```sql
CREATE DATABASE banco_tranquilo;
```

Na raiz do projeto, gere seu arquivo local a partir do exemplo:

```bash
cp .env.example .env
```

Configure os valores reais no **`.env` local**:

```dotenv
BANK_DB_URL=jdbc:postgresql://localhost:5432/banco_tranquilo
BANK_DB_USERNAME=SEU_USUARIO
BANK_DB_PASSWORD=SUA_SENHA
```

O `.env` é ignorado pelo Git e não deve ser publicado. Não coloque credenciais nos commits nem em prints ou logs compartilhados.

### Executar a aplicação

O Spring Boot recebe as variáveis exportadas no terminal. Estando na pasta do BancoTranquilo:

```bash
set -a
source .env
set +a

./mvnw spring-boot:run
```

Por configuração, a API escuta em **`http://127.0.0.1:8081`**. Use outra janela de terminal para chamar os endpoints.

A aplicação utiliza `spring.jpa.hibernate.ddl-auto=update` para gerenciar o esquema no ambiente local. Em produção, são recomendadas migrações versionadas, como Flyway ou Liquibase.

## Endpoint de pagamento

### `POST /api/payments`

**Exemplo de requisição** — valor aprovado:

```bash
curl -i -X POST http://127.0.0.1:8081/api/payments \
  -H 'Content-Type: application/json' \
  -d '{"orderId":10001,"amount":3200.00}'
```

Resposta HTTP `200 OK` (o identificador é gerado dinamicamente):

```json
{
  "transactionId": "BT-<UUID_GERADO>",
  "orderId": 10001,
  "status": "APPROVED",
  "message": "Payment approved successfully"
}
```

**Exemplo de recusa:**

```bash
curl -i -X POST http://127.0.0.1:8081/api/payments \
  -H 'Content-Type: application/json' \
  -d '{"orderId":10002,"amount":6400.00}'
```

O serviço responde HTTP `200 OK` com `status` igual a `DECLINED`, pois a requisição foi processada normalmente e a decisão simulada foi uma recusa.

**Exemplo de conflito:** após registrar `orderId=10001` com valor `3200.00`, tente utilizá-lo com outro valor:

```bash
curl -i -X POST http://127.0.0.1:8081/api/payments \
  -H 'Content-Type: application/json' \
  -d '{"orderId":10001,"amount":4000.00}'
```

Resultado: HTTP `409 Conflict`, sem alterar a transação original.

> Os IDs 10001 e 10002 são apenas exemplos. Em um banco já utilizado, escolha IDs que ainda não tenham transações para evitar conflitos com registros de testes anteriores.

## Persistência e idempotência

A tabela `bank_payments` registra os campos `order_id`, `amount`, `transaction_id`, `status`, `message` e `created_at`, além de seu identificador primário.

A coluna `order_id` possui uma restrição de unicidade (`uk_bank_payments_order_id`). No repositório JPA, a operação de inserção utiliza PostgreSQL `INSERT ... ON CONFLICT (order_id) DO NOTHING`:

1. A primeira solicitação pode inserir uma transação.
2. Solicitações repetidas para o mesmo pedido não inserem novos registros.
3. O serviço lê a transação persistida e compara o valor solicitado com o valor armazenado.
4. Se os valores forem equivalentes, devolve a mesma resposta e o mesmo `transactionId`.
5. Se forem diferentes, lança `PaymentConflictException`, mapeada para HTTP `409`.

Esse mecanismo foi validado também com requisições simultâneas: cinco chamadas para o mesmo pedido retornaram a mesma transação e apenas um registro foi mantido no PostgreSQL.

Para inspecionar os registros localmente:

```bash
psql -h localhost -U "$BANK_DB_USERNAME" -d banco_tranquilo \
  -c 'SELECT order_id, amount, transaction_id, status FROM bank_payments ORDER BY id DESC LIMIT 10;'
```

## Integração com o MercadoExpress

O MercadoExpress é o responsável por criar os pedidos e gerenciar o fluxo de pagamentos via RabbitMQ. Seu consumidor de solicitações chama esta API HTTP e publica o resultado recebido para que o pedido seja atualizado para `PAID` ou outro estado correspondente.

No ambiente de desenvolvimento validado:

| Serviço | Endereço |
| --- | --- |
| BancoTranquilo executado no Mac | `http://127.0.0.1:8081` |
| MercadoExpress no Docker | `http://127.0.0.1:8080` |
| Endereço usado pelo container para alcançar o banco | `http://host.docker.internal:8081` |

A idempotência é importante porque uma solicitação publicada via mensageria pode ser entregue novamente. Uma nova entrega para o mesmo pedido e valor recupera a transação original e evita duplicação no banco.

Repositório do serviço de compras: [CP5_Java_MercadoExpress](https://github.com/moisesBarsoti/CP5_Java_MercadoExpress).

## Testes automatizados

Depois de carregar o `.env`, execute:

```bash
set -a
source .env
set +a
./mvnw clean test
```

O teste de inicialização de contexto (`BancoTranquiloApplicationTests`) requer um PostgreSQL disponível e credenciais corretas. Se `BANK_DB_URL` não for exportada, o Spring Boot pode falhar com a mensagem `url must start with "jdbc"`.

**Resultados obtidos em 18/10/2026:**

| Classe | Testes | Resultado |
| --- | ---: | --- |
| `PaymentControllerTest` | 4 | Aprovados |
| `PaymentServiceTest` | 6 | Aprovados |
| `BancoTranquiloApplicationTests` | 1 | Aprovado |
| **Total** | **11** | **0 falhas e 0 erros** |

Também foram realizados testes manuais com pagamentos aprovados, repetição de solicitações, conflitos de valor e concorrência.

## Observações

- O projeto não movimenta dinheiro real e não deve ser apresentado como integração com instituição financeira.
- O identificador `transactionId` é gerado na primeira inserção bem-sucedida e reutilizado nos próximos pedidos equivalentes.
- O campo `status` representa a decisão simulada, enquanto o código HTTP representa o processamento da solicitação.
- A API depende de um PostgreSQL configurado; não há serviço de banco incluído neste repositório.
- Nunca versione arquivos `.env`, senhas, tokens ou credenciais de infraestrutura.
