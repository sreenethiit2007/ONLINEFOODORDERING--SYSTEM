# Online Food Ordering and Restaurant Sales Analysis System

## Project overview
A local academic full-stack system that accepts food orders through a browser, stores records in MySQL through a Java HTTP server, and analyses sales using Python.

## Exact folder organization
- `Documentation/` — detailed SRS (Word and PDF).
- `Software/Diagrams/` — Use Case, UML Class, UML Sequence, System Architecture, DFD Level 0 and DFD Level 1.
- `Database/` — schema, sample data and the ER diagram in `Database/Diagrams/`.
- `Java/` — only the two Java source programs and dependency folder.
- `Frontend/` — browser ordering page.
- `Python/` — analysis script, requirements, sample chart images, and generated-output folders.
- `Config/` — setup notes.

## Database setup
Execute `Database/database_schema.sql` in MySQL Workbench. Execute `Database/sample_data.sql` only once on a fresh database. Do not run sample inserts again on an existing populated database.

## Password safety
Set the local MySQL password in the same PowerShell session before running Java or Python:
```powershell
$env:DB_PASSWORD="your-local-password"
```
Never put the actual password in code, SQL, screenshots, or GitHub.

## Java dependencies
Copy the real MySQL Connector/J and JSON-java JAR files into `Java/lib/`. The package does not include fake placeholder JAR files. Use the filenames present in your folder when compiling.

## Run Java server
From the `Java` folder:
```powershell
javac -cp ".;lib\mysql-connector-j-26.10.0.jar;lib\json-20250517 (2).jar" FoodOrderServer.java
java -cp ".;lib\mysql-connector-j-26.10.0.jar;lib\json-20250517 (2).jar" FoodOrderServer
```
Keep the server running. Open `Frontend/index.html` in a browser. The implemented API is `POST http://localhost:8080/order`; the root URL is not a website route.

## Run Python analysis
```powershell
cd Python
python -m pip install -r requirements.txt
$env:DB_PASSWORD="your-local-password"
python sales_analysis.py
```
The `*_sample.png` charts are illustrative examples and open directly. Running the script creates real CSV and PNG outputs from the current database.

## Notes
This is an academic local demonstration, not a production-ready commercial platform. Production use requires authentication, role-based access, strict CORS, HTTPS, payment integration, logging, backup and security hardening.
