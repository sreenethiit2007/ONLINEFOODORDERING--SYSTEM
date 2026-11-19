# Online Food Ordering and Restaurant Sales Analysis System

## Project Overview
The Online Food Ordering and Restaurant Sales Analysis System is designed to manage restaurant menu items, customer details, food orders, order status, and sales analysis.

## Technologies Used
- **Frontend:** HTML, CSS, JavaScript
- **Backend:** Java
- **Database:** MySQL
- **Data Analytics:** Python, Pandas, Matplotlib

## Main Features
1. Manage customer details.
2. Add and view menu items.
3. Check menu item availability.
4. Place food orders with multiple items.
5. Calculate order totals automatically.
6. Update order status.
7. View customer order history.
8. Generate daily sales reports.
9. Analyze popular food items and sales trends.

## Project Structure
```text
OnlineFoodOrderingProject/
├── FoodOrderServer.java
├── FoodOrdering.java
├── index.html
├── sales_analysis.py
├── database_schema.sql
├── sample_data.sql
├── requirements.txt
├── SRS_Document.docx
└── Python/
    ├── Reports/
    │   ├── sales_report.csv
    │   └── daily_sales_report.csv
    └── Charts/
        ├── popular_items.png
        └── sales_chart.png
```

## Software Requirements
- Java JDK
- MySQL Server
- MySQL Workbench
- Python 3
- Visual Studio Code

## Python Libraries
Install the required libraries:

```bash
pip install mysql-connector-python pandas matplotlib
```

## Running the Java Console Application

```powershell
$env:DB_PASSWORD="YOUR_MYSQL_PASSWORD"
javac -cp ".;mysql-connector-j-26.10.0.jar" FoodOrdering.java
java -cp ".;mysql-connector-j-26.10.0.jar" FoodOrdering
```

## Running Python Sales Analysis

```bash
python sales_analysis.py
```

The Python program generates sales reports in CSV format and charts in PNG format.

## Project Documentation
The Software Requirements Specification (SRS), UML diagrams, DFD diagrams, ER diagram, and system architecture diagram are included in this repository.

## Conclusion
This project simplifies restaurant order management and provides useful sales insights through Python-based data analysis.
