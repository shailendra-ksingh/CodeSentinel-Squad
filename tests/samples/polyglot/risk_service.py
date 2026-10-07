"""
Small risk service example used by the CodeSentinel polyglot demo.

The service uses the same customer identifier passed through the
order flow. The code is intentionally simple so that the project
review can focus on cross-language relationships.
"""


def evaluate_risk(customer_id):
    if not customer_id:
        return {
            "risk": "HIGH",
            "reason": "customer_id is missing"
        }

    return {
        "customer_id": customer_id,
        "risk": "LOW"
    }