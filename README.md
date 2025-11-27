# SAGA Pattern - Microservices Implementation

Este projeto demonstra a implementação do padrão SAGA para gerenciamento de transações distribuídas em uma arquitetura de microserviços.

## Arquitetura

O sistema consiste em três microserviços principais:

1. **Order Service** (Porta 8080) - Gerencia pedidos e orquestra o padrão SAGA
2. **Payment Service** (Porta 8081) - Processa pagamentos e reembolsos
3. **Inventory Service** (Porta 8082) - Gerencia estoque de produtos

## Fluxo SAGA

O fluxo segue o padrão SAGA com compensação:

1. **Criação do Pedido**: O Order Service cria um pedido com status PENDING
2. **Processamento de Pagamento**: Solicita ao Payment Service para processar o pagamento
3. **Atualização de Inventário**: Solicita ao Inventory Service para atualizar o estoque
4. **Compensação**: Se algum passo falhar, as ações anteriores são compensadas

### Estados do Pedido

- `PENDING` - Pedido criado
- `PAYMENT_PROCESSING` - Pagamento em processamento
- `PAYMENT_COMPLETED` - Pagamento concluído
- `PAYMENT_FAILED` - Pagamento falhou
- `INVENTORY_PROCESSING` - Inventário em processamento
- `INVENTORY_COMPLETED` - Inventário atualizado
- `INVENTORY_FAILED` - Falha na atualização do inventário
- `COMPLETED` - Pedido concluído com sucesso
- `CANCELLED` - Pedido cancelado

## Tecnologias Utilizadas

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Cloud OpenFeign (comunicação entre serviços)
- H2 Database (banco em memória)
- Maven

## Pré-requisitos

- Java 17 ou superior
- Maven 3.6 ou superior
- Bash (para scripts de execução)

## Como Executar

### Opção 1: Scripts de Execução (Recomendado)

1. **Iniciar todos os serviços**:
   ```bash
   chmod +x run-services.sh
   ./run-services.sh
   ```

2. **Verificar status dos serviços**:
   ```bash
   chmod +x status.sh
   ./status.sh
   ```

3. **Parar todos os serviços**:
   ```bash
   chmod +x stop-services.sh
   ./stop-services.sh
   ```

4. **Testar o padrão SAGA**:
   ```bash
   chmod +x test-saga.sh
   ./test-saga.sh
   ```

### Opção 2: Manual

1. **Order Service**:
   ```bash
   cd order-service
   mvn spring-boot:run
   ```

2. **Payment Service** (em outro terminal):
   ```bash
   cd payment-service
   mvn spring-boot:run
   ```

3. **Inventory Service** (em outro terminal):
   ```bash
   cd inventory-service
   mvn spring-boot:run
   ```

## Endpoints da API

### Order Service
- `POST /api/orders` - Criar novo pedido
- `GET /api/orders/{id}` - Buscar pedido por ID
- `GET /api/orders` - Listar todos os pedidos
- `GET /api/orders/customer/{customerId}` - Buscar pedidos por cliente

### Payment Service
- `POST /api/payments/process` - Processar pagamento
- `POST /api/payments/refund` - Processar reembolso

### Inventory Service
- `POST /api/inventory/update` - Atualizar inventário
- `POST /api/inventory/compensate` - Compensar inventário
- `GET /api/inventory/products` - Listar todos os produtos
- `GET /api/inventory/products/available` - Listar produtos disponíveis
- `GET /api/inventory/products/{productId}` - Buscar produto por ID
- `POST /api/inventory/products` - Criar novo produto
- `PUT /api/inventory/products/{productId}/stock` - Atualizar estoque

## Exemplos de Uso

### Criar um Pedido

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "productId": "PROD-001",
    "quantity": 2,
    "totalAmount": 1399.98
  }'
```

### Verificar Status do Pedido

```bash
curl http://localhost:8080/api/orders/{orderId}
```

### Verificar Inventário

```bash
curl http://localhost:8082/api/inventory/products/PROD-001
```

## Consoles H2

Cada serviço possui seu próprio banco H2 com console web disponível:

- Order Service: http://localhost:8080/h2-console
- Payment Service: http://localhost:8081/h2-console
- Inventory Service: http://localhost:8082/h2-console

**Configurações do H2 Console:**
- JDBC URL: `jdbc:h2:mem:orderdb` (ou `paymentdb`, `inventorydb`)
- Username: `sa`
- Password: (deixe em branco)

## Testando Falhas

O sistema está configurado para simular falhas aleatórias:

- **Payment Service**: 10% de chance de falha no processamento
- **Inventory Service**: Falha quando estoque insuficiente
- **Compensação**: Reembolso automático em caso de falha

Para testar falhas, você pode:

1. Criar um pedido com quantidade maior que o estoque disponível
2. Aguardar até que uma falha de pagamento ocorra (10% de chance)
3. Verificar os logs para ver o processo de compensação

## Logs e Monitoramento

Cada serviço gera logs detalhados do processo SAGA:

```bash
# Ver logs do Order Service
tail -f order-service/logs/spring.log

# Ver logs do Payment Service
tail -f payment-service/logs/spring.log

# Ver logs do Inventory Service
tail -f inventory-service/logs/spring.log
```

## Estrutura do Projeto

```
saga/
├── order-service/          # Order Service com SAGA Orchestrator
│   ├── src/main/java/com/saga/orderservice/
│   │   ├── entity/       # Entidades JPA
│   │   ├── repository/   # Repositórios Spring Data
│   │   ├── service/      # Lógica de negócio e SAGA
│   │   ├── controller/   # APIs REST
│   │   └── client/       # Clientes Feign
│   └── src/main/resources/
├── payment-service/        # Payment Service
│   ├── src/main/java/com/saga/paymentservice/
│   └── src/main/resources/
├── inventory-service/      # Inventory Service
│   ├── src/main/java/com/saga/inventoryservice/
│   └── src/main/resources/
├── run-services.sh         # Script para iniciar todos os serviços
├── stop-services.sh        # Script para parar todos os serviços
├── status.sh              # Script para verificar status
├── test-saga.sh           # Script para testar o padrão SAGA
└── README.md              # Este arquivo
```

## Notas Importantes

1. **Bancos em Memória**: Os serviços usam H2 em memória, então os dados são perdidos ao reiniciar
2. **Portas**: Certifique-se de que as portas 8080, 8081 e 8082 estejam disponíveis
3. **Comunicação**: Os serviços se comunicam via REST usando OpenFeign
4. **Compensação**: O sistema implementa compensação automática para garantir consistência eventual

## Solução de Problemas

### Serviços não iniciam
- Verifique se as portas estão disponíveis: `netstat -an | grep 808`
- Verifique se o Java 17 está instalado: `java -version`
- Verifique se o Maven está instalado: `mvn -version`

### Falha na comunicação entre serviços
- Verifique se todos os serviços estão rodando: `./status.sh`
- Verifique os logs de cada serviço
- Certifique-se de que iniciou os serviços na ordem correta

### Pedidos ficam travados em PENDING
- Isso pode indicar falha de comunicação com outros serviços
- Verifique os logs do Order Service para detalhes
- Verifique se Payment Service e Inventory Service estão respondendo

## Próximos Passos

Para uma implementação de produção, considere:

1. **Mensageria**: Substituir comunicação REST por mensageria (RabbitMQ, Kafka)
2. **Banco de Dados Persistente**: Usar PostgreSQL, MySQL ou MongoDB
3. **Service Discovery**: Implementar Eureka ou Consul
4. **API Gateway**: Adicionar um gateway para gerenciar as APIs
5. **Monitoramento**: Implementar Prometheus, Grafana ou similar
6. **Testes**: Adicionar testes unitários e de integração
7. **Containerização**: Criar Docker containers para cada serviço
8. **Orquestração**: Usar Kubernetes ou Docker Swarm