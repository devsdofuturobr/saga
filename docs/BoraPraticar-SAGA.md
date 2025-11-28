# SAGA Pattern na Prática com Spring Boot 🎭🚀

![Banner SAGA](./assets/saga.png)

### SAGA em dois sabores 🍦
- Orquestrada (command/HTTP): um orquestrador central coordena cada passo — no nosso caso, o `order-service` chama `payment` e `inventory` e aplica compensações quando necessário.
  - Prós: fluxo explícito, debugging simples, ótimo para demos e times iniciando.
  - Contras: acoplamento ao orquestrador, risco de ponto único de falha, precisa de cuidado para escalar.
- Coreografada (eventos): não há orquestrador; cada serviço publica eventos e reage aos de outros (ex.: `OrderCreated` → `PaymentProcessed` → `InventoryReserved`), geralmente com Outbox + Kafka/RabbitMQ.
  - Prós: baixo acoplamento, mais escalável e resiliente.
  - Contras: rastreabilidade e observabilidade exigem mais esforço, consistência é eventual, requer mensageria e padrões como Outbox.

## Bora Praticar hoje SAGA Orquestrado? 🧠
- SAGA coordena transações locais entre microserviços com passos e compensações.
- Use quando precisa de resiliência e consistência eventual; evite quando precisa de consistência forte imediata.
- Aqui tem código pronto, endpoints, curls e compensações para testar rápido.

## Endpoints e Swagger UI 🔗
- Endpoints
  - Order: `POST /api/orders`, `GET /api/orders/{id}`, `GET /api/orders`, `GET /api/orders/customer/{customerId}`
  - Payment: `POST /api/payments/process`, `POST /api/payments/refund`
  - Inventory: `POST /api/inventory/update`, `POST /api/inventory/compensate`, `GET /api/inventory/products{,/available,/{productId}}`
- Swagger UI
  - `http://localhost:8080/swagger-ui/index.html`
  - `http://localhost:8081/swagger-ui/index.html`
  - `http://localhost:8082/swagger-ui/index.html`

## Diagrama do Fluxo (Mermaid) 🗺️
```mermaid
sequenceDiagram
    participant C as Cliente
    participant O as Order Service
    participant P as Payment Service
    participant I as Inventory Service

    C->>O: POST /api/orders
    O->>P: processPayment(orderId, customerId, amount)
    alt pagamento OK
        O->>I: updateInventory(productId, quantity)
        alt inventário OK
            O->>O: status = COMPLETED
        else inventário falhou
            O->>P: refundPayment(orderId)
            O->>O: status = INVENTORY_FAILED / CANCELLED
        end
    else pagamento falhou
        O->>O: status = PAYMENT_FAILED
    end
```

E aí, devs! BoraPraticar SAGA de um jeito leve, direto e prático? 😎 Neste BoraPraticar vamos montar e entender um fluxo SAGA completo com três microserviços: `order-service`, `payment-service` e `inventory-service`. Além do passo a passo, tem benefícios, quando usar (e quando não!), códigos essenciais e curls para testar cenários felizes e compensatórios. Repo: https://github.com/devsdofuturobr/saga.git

---

## O que vamos construir 🧩
- `Order Service` (8080): orquestrador da SAGA, cria pedidos e coordena os passos
- `Payment Service` (8081): processa pagamentos e reembolsos
- `Inventory Service` (8082): atualiza e compensa estoque

Fluxo resumido:
1) Cria pedido (`PENDING`)
2) Processa pagamento (`PAYMENT_*`)
3) Atualiza estoque (`INVENTORY_*`)
4) Sucesso → `COMPLETED`; falha → compensação e `CANCELLED`

Estados de pedido: `PENDING`, `PAYMENT_PROCESSING`, `PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `INVENTORY_PROCESSING`, `INVENTORY_COMPLETED`, `INVENTORY_FAILED`, `COMPLETED`, `CANCELLED`.

---

## Por que usar SAGA? ✨
- Consistência eventual com autonomia por serviço
- Resiliência: cada etapa tem compensação definida
- Escalabilidade: transações locais, comunicação leve
- Observabilidade e auditoria de cada etapa

## Checklist SAGA ✅
- [ ] Cada passo tem uma compensação definida
- [ ] Estados do pedido cobrem sucesso e falhas
- [ ] Comunicação remota simples e com tratamento de erro
- [ ] Scripts/collections para reproduzir cenários
- [ ] Logs claros para entender o fluxo

## Quando evitar SAGA? 🛑
- Você precisa de consistência forte e imediata em uma única operação
- O domínio é simples e cabe em uma transação local
- Latência ultrabaixa e complexidade operacional não são aceitáveis
- O time ainda não tem maturidade para lidar com falhas e compensações

---

## Códigos que importam 🧠

Orquestração da SAGA (Order Service):

```java
// order-service/src/main/java/com/saga/orderservice/service/SagaOrchestrator.java
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaOrchestrator {
    private final PaymentServiceClient paymentServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final OrderRepository orderRepository;

    public void startOrderSaga(Order order) {
        log.info("Starting SAGA for order: {}", order.getId());
        try {
            updateOrderStatus(order.getId(), OrderStatus.PAYMENT_PROCESSING);
            boolean paymentProcessed = paymentServiceClient.processPayment(
                    order.getId(), order.getCustomerId(), order.getTotalAmount());
            if (!paymentProcessed) {
                updateOrderStatus(order.getId(), OrderStatus.PAYMENT_FAILED);
                return;
            }
            updateOrderStatus(order.getId(), OrderStatus.PAYMENT_COMPLETED);

            updateOrderStatus(order.getId(), OrderStatus.INVENTORY_PROCESSING);
            boolean inventoryUpdated = inventoryServiceClient.updateInventory(
                    order.getProductId(), order.getQuantity());
            if (!inventoryUpdated) {
                // compensação
                paymentServiceClient.refundPayment(order.getId());
                updateOrderStatus(order.getId(), OrderStatus.INVENTORY_FAILED);
                return;
            }
            updateOrderStatus(order.getId(), OrderStatus.INVENTORY_COMPLETED);
            updateOrderStatus(order.getId(), OrderStatus.COMPLETED);
        } catch (Exception e) {
            log.error("Error in SAGA for order {}: {}", order.getId(), e.getMessage());
            handleSagaFailure(order);
        }
    }

    private void handleSagaFailure(Order order) {
        try {
            if (order.getStatus() == OrderStatus.PAYMENT_COMPLETED ||
                order.getStatus() == OrderStatus.INVENTORY_PROCESSING ||
                order.getStatus() == OrderStatus.INVENTORY_FAILED) {
                paymentServiceClient.refundPayment(order.getId());
            }
            updateOrderStatus(order.getId(), OrderStatus.CANCELLED);
        } catch (Exception ignored) {}
    }

    private void updateOrderStatus(Long orderId, OrderStatus status) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus(status);
            orderRepository.save(o);
        });
    }
}
```

Controller do Order (criar pedido):

```java
// order-service/src/main/java/com/saga/orderservice/controller/OrderController.java
@PostMapping
public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
    OrderResponse response = orderService.createOrder(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
}
```

Clientes Feign (comunicação remota):

```java
// order-service/src/main/java/com/saga/orderservice/client/PaymentServiceClient.java
@FeignClient(name = "payment-service", url = "http://localhost:8081")
public interface PaymentServiceClient {
    @PostMapping("/api/payments/process")
    boolean processPayment(@RequestParam("orderId") Long orderId,
                           @RequestParam("customerId") String customerId,
                           @RequestParam("amount") BigDecimal amount);

    @PostMapping("/api/payments/refund")
    boolean refundPayment(@RequestParam("orderId") Long orderId);
}
```

---

## Endpoints principais 🛣️

Order Service (8080)
- POST `/api/orders`
- GET `/api/orders/{id}`
- GET `/api/orders`
- GET `/api/orders/customer/{customerId}`

Payment Service (8081)
- POST `/api/payments/process?orderId=...&customerId=...&amount=...`
- POST `/api/payments/refund?orderId=...`

Inventory Service (8082)
- POST `/api/inventory/update?productId=...&quantity=...`
- POST `/api/inventory/compensate?productId=...&quantity=...`
- GET `/api/inventory/products`
- GET `/api/inventory/products/available`
- GET `/api/inventory/products/{productId}`

Swagger UI (dev-friendly):
- http://localhost:8080/swagger-ui/index.html
- http://localhost:8081/swagger-ui/index.html
- http://localhost:8082/swagger-ui/index.html

H2 Console (para ver o banco):
- `order`: http://localhost:8080/h2-console (jdbc:h2:mem:orderdb)
- `payment`: http://localhost:8081/h2-console (jdbc:h2:mem:paymentdb)
- `inventory`: http://localhost:8082/h2-console (jdbc:h2:mem:inventorydb)
Username: `sa` • Password: vazio

---

## Bora testar com curl 🧪

Happy path (pedido confirmado):

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "productId": "PROD-001",
    "quantity": 2,
    "totalAmount": 1399.98
  }'

# supondo que o ID retornado seja 1
curl http://localhost:8080/api/orders/1
curl http://localhost:8082/api/inventory/products/PROD-001
```

Cenário compensatório (falha no inventário → reembolso):

```bash
# quantidade maior que o estoque para forçar falha de inventário
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "productId": "PROD-001",
    "quantity": 999,
    "totalAmount": 999999.99
  }'

# ver status do pedido (tende a CANCELLED após compensação)
curl http://localhost:8080/api/orders/2

# conferir inventário e (se pagamento tiver completado) reembolso aplicado
curl http://localhost:8082/api/inventory/products/PROD-001
```

Falha de pagamento (aleatória, ~10%):

```bash
# recrie pedidos e observe logs/status; quando o pagamento falha, o pedido fica como PAYMENT_FAILED
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-456",
    "productId": "PROD-002",
    "quantity": 1,
    "totalAmount": 1299.99
  }'
```

Manual (se quiser chamar direto):

```bash
# pagamento
curl -X POST "http://localhost:8081/api/payments/process?orderId=1&customerId=CUST-123&amount=1399.98"

# reembolso
curl -X POST "http://localhost:8081/api/payments/refund?orderId=1"
```

---

## Execução rápida ▶️
- Terminais: `mvn spring-boot:run` dentro de cada serviço

---

## Erros comuns e dicas 🪛
- Ciclo de beans: evite injetar `OrderService` dentro do orquestrador; use `OrderRepository` para atualizar status.
- Idempotência: compensações devem tolerar reexecuções sem efeitos colaterais indesejados.
- Timeouts e retries: configure limites e políticas de reexecução para chamadas remotas.
- Observabilidade: registre transições de estado e correlações por `orderId`.

---

## BoraPraticar: takeaways 🎯
- SAGA é sobre coordenar transações locais com compensações pensadas
- Troque “transação distribuída gigante” por “etapas menores + rollback inteligente”
- Observabilidade e logs são parte do jogo
- Nem todo problema pede SAGA — seja intencional 😉

Repo completo para você clonar e brincar: https://github.com/devsdofuturobr/saga.git

---

## Valeu por chegar até aqui! 🙌
- Se curtiu este BoraPraticar, deixa um comentário com suas dúvidas ou ideias.
- Compartilhe com a galera e ajuda a levar SAGA para mais devs! 🔄✨
