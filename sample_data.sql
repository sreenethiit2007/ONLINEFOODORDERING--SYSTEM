USE food_ordering_db;

-- Run this sample data script only once on a fresh database.
INSERT INTO menu_items (item_name, category, price, availability) VALUES
('Burger','Fast Food',120.00,TRUE),
('Pizza','Fast Food',250.00,TRUE),
('Sandwich','Snacks',100.00,TRUE),
('Fried Rice','Main Course',150.00,TRUE),
('Juice','Beverage',60.00,TRUE),
('Tomato Toast','Snacks',80.00,TRUE);

INSERT INTO customers (customer_name, phone, email) VALUES
('Arun','9876500001','arun@example.com'),
('Bala','9876500002','bala@example.com'),
('Kumar','9876500003','kumar@example.com');

INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
(1,'2026-11-01 12:15:00',370.00,'Completed'),
(2,'2026-11-02 19:30:00',310.00,'Completed'),
(3,'2026-11-03 13:05:00',250.00,'Preparing');

INSERT INTO order_items (order_id,item_id,quantity,subtotal) VALUES
(1,1,1,120.00),(1,2,1,250.00),
(2,4,1,150.00),(2,5,1,60.00),(2,3,1,100.00),
(3,2,1,250.00);
