-- Database schema used by the CodeSentinel polyglot demo.
--
-- customer_id is the identifier shared by the API and risk service.

CREATE TABLE customer (
                          customer_id VARCHAR(50) PRIMARY KEY,
                          name VARCHAR(100) NOT NULL
);

CREATE TABLE orders (
                        order_id VARCHAR(50) PRIMARY KEY,
                        customer_id VARCHAR(50) NOT NULL,
                        amount DECIMAL(12, 2) NOT NULL,
                        CONSTRAINT fk_orders_customer
                            FOREIGN KEY (customer_id)
                                REFERENCES customer(customer_id)
);