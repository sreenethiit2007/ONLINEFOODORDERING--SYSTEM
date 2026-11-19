import os
from pathlib import Path
import mysql.connector
import pandas as pd
import matplotlib.pyplot as plt

password = os.getenv("DB_PASSWORD")
if not password:
    raise RuntimeError("DB_PASSWORD is not set in this PowerShell session.")

con = mysql.connector.connect(
    host="localhost", user="root", password=password,
    database="food_ordering_db"
)
reports = Path("Python") / "Reports"
charts = Path("Python") / "Charts"
reports.mkdir(parents=True, exist_ok=True)
charts.mkdir(parents=True, exist_ok=True)

try:
    query = """
    SELECT m.item_name, SUM(oi.quantity) AS total_quantity,
           SUM(oi.subtotal) AS total_sales
    FROM order_items oi
    JOIN menu_items m ON oi.item_id=m.item_id
    JOIN orders o ON oi.order_id=o.order_id
    WHERE o.status='Completed'
    GROUP BY m.item_id,m.item_name
    ORDER BY total_quantity DESC
    """
    df = pd.read_sql(query, con)
    print("\\n===== RESTAURANT SALES ANALYSIS =====")
    if df.empty:
        print("No completed orders found. Use Java menu option 7 to mark an order Completed.")
    else:
        print(df.to_string(index=False))
        df.to_csv(reports / "sales_report.csv", index=False)

        plt.figure(figsize=(8, 5))
        plt.bar(df["item_name"], df["total_quantity"])
        plt.title("Popular Food Items")
        plt.xlabel("Food Items")
        plt.ylabel("Quantity Sold")
        plt.xticks(rotation=30, ha="right")
        plt.tight_layout()
        plt.savefig(charts / "popular_items.png")
        plt.close()

        plt.figure(figsize=(8, 5))
        plt.bar(df["item_name"], df["total_sales"])
        plt.title("Restaurant Sales Analysis")
        plt.xlabel("Food Items")
        plt.ylabel("Total Sales (Rs.)")
        plt.xticks(rotation=30, ha="right")
        plt.tight_layout()
        plt.savefig(charts / "sales_chart.png")
        plt.close()

    daily = pd.read_sql("""
        SELECT DATE(order_date) AS sales_date, COUNT(*) AS total_orders,
               SUM(total_amount) AS total_revenue
        FROM orders WHERE status='Completed'
        GROUP BY DATE(order_date) ORDER BY sales_date
    """, con)
    daily.to_csv(reports / "daily_sales_report.csv", index=False)
    print("\\nDaily sales report:")
    print(daily.to_string(index=False) if not daily.empty else "No completed sales.")
    print("\\nReports saved in Python/Reports and charts in Python/Charts.")
finally:
    con.close()
