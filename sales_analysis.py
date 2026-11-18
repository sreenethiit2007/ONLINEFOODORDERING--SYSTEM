import os
from pathlib import Path
import mysql.connector
import pandas as pd
import matplotlib.pyplot as plt

BASE = Path(__file__).resolve().parent
REPORTS = BASE / "Reports"
CHARTS = BASE / "Charts"
REPORTS.mkdir(exist_ok=True)
CHARTS.mkdir(exist_ok=True)

password = os.getenv("DB_PASSWORD")
if not password:
    raise RuntimeError("DB_PASSWORD is not set in this PowerShell session.")

connection = mysql.connector.connect(
    host="localhost", user="root", password=password, database="food_ordering_db"
)
try:
    orders = pd.read_sql(
        """SELECT o.order_id, o.order_date, o.total_amount, o.status,
                  c.customer_name
           FROM orders o JOIN customers c ON c.customer_id=o.customer_id
           ORDER BY o.order_date""", connection
    )
    popular = pd.read_sql(
        """SELECT m.item_name, SUM(oi.quantity) AS quantity_sold,
                  SUM(oi.subtotal) AS revenue
           FROM order_items oi
           JOIN menu_items m ON m.item_id=oi.item_id
           JOIN orders o ON o.order_id=oi.order_id
           WHERE o.status <> 'Cancelled'
           GROUP BY m.item_id, m.item_name
           ORDER BY quantity_sold DESC""", connection
    )
finally:
    connection.close()

orders["order_date"] = pd.to_datetime(orders["order_date"])
orders.to_csv(REPORTS / "sales_report.csv", index=False)
popular.to_csv(REPORTS / "popular_items.csv", index=False)

daily = orders.loc[orders["status"] != "Cancelled"].groupby(
    orders.loc[orders["status"] != "Cancelled", "order_date"].dt.date
)["total_amount"].sum()
fig, ax = plt.subplots(figsize=(9, 5))
daily.plot(kind="bar", ax=ax)
ax.set_title("Daily Restaurant Sales")
ax.set_xlabel("Order Date"); ax.set_ylabel("Sales (Rs.)")
fig.tight_layout(); fig.savefig(CHARTS / "sales_chart.png", dpi=180); plt.close(fig)

fig, ax = plt.subplots(figsize=(9, 5))
if not popular.empty:
    popular.head(10).sort_values("quantity_sold").plot(
        kind="barh", x="item_name", y="quantity_sold", legend=False, ax=ax
    )
ax.set_title("Popular Menu Items"); ax.set_xlabel("Quantity Sold"); ax.set_ylabel("Menu Item")
fig.tight_layout(); fig.savefig(CHARTS / "popular_items.png", dpi=180); plt.close(fig)

print("Sales analysis completed.")
print(f"Orders analysed: {len(orders)}")
print(f"Reports saved to: {REPORTS}")
print(f"Charts saved to: {CHARTS}")
