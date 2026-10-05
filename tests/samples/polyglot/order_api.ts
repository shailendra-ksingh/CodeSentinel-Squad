export async function createOrder(payload: any) {
  const response = await fetch('/api/orders');
  const result = await response.json();
  document.getElementById('status')!.innerHTML = result.message;
  return response;
}
