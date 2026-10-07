/**
 * Small client-side example used by the CodeSentinel polyglot demo.
 *
 * The response contract intentionally differs from the Java API.
 * This gives the project-level review a real cross-language issue to find.
 */

export interface OrderResponse {
  id: string;
  total: number;
}

export interface CreateOrderRequest {
  customerId: string;
}

export async function createOrder(
    customerId: string
): Promise<OrderResponse> {

  const request: CreateOrderRequest = {
    customerId
  };

  const response = await fetch("/api/orders", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(
        `Order creation failed: ${response.status}`
    );
  }

  return response.json();
}