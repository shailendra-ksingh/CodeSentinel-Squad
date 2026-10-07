package samples.polyglot;

import java.math.BigDecimal;

/**
 * Small Java API example used by the CodeSentinel polyglot demo.
 *
 * The response field names intentionally differ from the TypeScript
 * client contract so that project-level integration can identify it.
 */
public class OrderController {

    public OrderResponse createOrder(String customerId) {

        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException(
                    "customerId is required"
            );
        }

        return new OrderResponse(
                "ORD-1001",
                new BigDecimal("1250.00")
        );
    }

    public record OrderResponse(
            String orderId,
            BigDecimal amount
    ) {
    }
}
