import java.sql.*;
import java.util.*;
import java.time.LocalDate;

public class FoodOrdering {
    static final String URL = "jdbc:mysql://localhost:3306/food_ordering_db";
    static final String USER = "root";
    static final Scanner sc = new Scanner(System.in);

    static Connection db() throws SQLException {
        String pw = System.getenv("DB_PASSWORD");
        if (pw == null || pw.isEmpty())
            throw new SQLException("Set DB_PASSWORD in PowerShell first.");
        return DriverManager.getConnection(URL, USER, pw);
    }
    static String text(String p) { System.out.print(p); return sc.nextLine().trim(); }
    static int number(String p) {
        while (true) try { return Integer.parseInt(text(p)); }
        catch (NumberFormatException e) { System.out.println("Enter a valid number."); }
    }
    static double amount(String p) {
        while (true) try {
            double n = Double.parseDouble(text(p));
            if (n > 0) return n;
            System.out.println("Enter an amount greater than zero.");
        } catch (NumberFormatException e) { System.out.println("Enter a valid amount."); }
    }
static String val(String s) {
    return s == null ? "" : s;
}
    public static void main(String[] args) {
        try (Connection c = db()) { System.out.println("MySQL connected successfully."); }
        catch (SQLException e) { System.out.println("Connection error: " + e.getMessage()); return; }

        while (true) {
            System.out.println("\\n========================================");
            System.out.println("       ONLINE FOOD ORDERING SYSTEM");
            System.out.println("========================================");
            System.out.println("1. Add Customer");
            System.out.println("2. List Customers");
            System.out.println("3. Add Menu Item");
            System.out.println("4. View Menu");
            System.out.println("5. Change Item Availability");
            System.out.println("6. Place Order");
            System.out.println("7. Update Order Status");
            System.out.println("8. View Customer Orders");
            System.out.println("9. Daily Sales Report");
            System.out.println("0. Exit");
            int ch = number("Enter choice: ");
            try {
                switch (ch) {
                    case 1: addCustomer(); break;
                    case 2: listCustomers(); break;
                    case 3: addMenuItem(); break;
                    case 4: viewMenu(); break;
                    case 5: availability(); break;
                    case 6: placeOrder(); break;
                    case 7: updateStatus(); break;
                    case 8: customerOrders(); break;
                    case 9: dailySales(); break;
                    case 0: System.out.println("Exiting..."); return;
                    default: System.out.println("Choose 0 to 9.");
                }
            } catch (Exception e) { System.out.println("Error: " + e.getMessage()); }
        }
    }

    static void addCustomer() throws SQLException {
        String name = text("Customer name: ");
        String phone = text("Phone (optional): ");
        if (name.isEmpty()) { System.out.println("Name is required."); return; }
        try (Connection c = db(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO customers(customer_name,phone) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, name);
            if (phone.isEmpty()) p.setNull(2, Types.VARCHAR); else p.setString(2, phone);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) { if (r.next()) System.out.println("Customer ID: " + r.getInt(1)); }
        }
    }

    static void listCustomers() throws SQLException {
        String q = "SELECT customer_id,customer_name,phone,email FROM customers ORDER BY customer_id";
        try (Connection c = db(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(q)) {
            System.out.printf("%-10s %-24s %-18s %-25s%n", "ID", "CUSTOMER", "PHONE", "EMAIL");
            while (r.next()) System.out.printf("%-10d %-24s %-18s %-25s%n",
                r.getInt(1), r.getString(2), val(r.getString(3)), val(r.getString(4)));
        }
    }

    static void addMenuItem() throws SQLException {
        String name = text("Item name: "), category = text("Category: ");
        double price = amount("Price: ");
        try (Connection c = db(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO menu_items(item_name,category,price,availability) VALUES(?,?,?,TRUE)",
                Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, name); p.setString(2, category); p.setDouble(3, price); p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) { if (r.next()) System.out.println("New Item ID: " + r.getInt(1)); }
        }
    }

    static void viewMenu() throws SQLException {
        String q = "SELECT item_id,item_name,category,price,availability FROM menu_items ORDER BY item_id";
        try (Connection c = db(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(q)) {
            System.out.printf("%-8s %-22s %-18s %-12s %-15s%n", "ITEM ID", "ITEM NAME", "CATEGORY", "PRICE", "AVAILABILITY");
            System.out.println("----------------------------------------------------------------------------");
            while (r.next()) System.out.printf("%-8d %-22s %-18s Rs.%-8.2f %-15s%n",
                r.getInt(1), r.getString(2), r.getString(3), r.getDouble(4),
                r.getBoolean(5) ? "Available" : "Unavailable");
        }
    }

    static void availability() throws SQLException {
        viewMenu();
        int id = number("Item ID: ");
        String a = text("Available? (Y/N): ");
        if (!a.equalsIgnoreCase("Y") && !a.equalsIgnoreCase("N")) { System.out.println("Enter Y or N."); return; }
        try (Connection c = db(); PreparedStatement p = c.prepareStatement("UPDATE menu_items SET availability=? WHERE item_id=?")) {
            p.setBoolean(1, a.equalsIgnoreCase("Y")); p.setInt(2, id);
            System.out.println(p.executeUpdate() > 0 ? "Availability updated." : "Item ID not found.");
        }
    }

    static class Line { int id, qty; double sub; Line(int i,int q,double s){id=i;qty=q;sub=s;} }

    static void placeOrder() throws SQLException {
        viewMenu();
        int cid = number("Existing Customer ID (add customer with option 1 first): ");
        try (Connection c = db()) {
            String customer;
            try (PreparedStatement p = c.prepareStatement("SELECT customer_name FROM customers WHERE customer_id=?")) {
                p.setInt(1,cid); try (ResultSet r=p.executeQuery()) {
                    if (!r.next()) { System.out.println("Customer ID not found."); return; }
                    customer = r.getString(1);
                }
            }
            List<Line> lines = new ArrayList<>(); double total = 0;
            while (true) {
                int id = number("Item ID (0 to finish): "); if (id == 0) break;
                int qty = number("Quantity: "); if (qty <= 0) { System.out.println("Quantity must be positive."); continue; }
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT item_name,price FROM menu_items WHERE item_id=? AND availability=TRUE")) {
                    p.setInt(1,id); try (ResultSet r=p.executeQuery()) {
                        if (!r.next()) { System.out.println("Item unavailable or ID not found."); continue; }
                        double sub = r.getDouble("price") * qty; total += sub; lines.add(new Line(id,qty,sub));
                        System.out.printf("Added %s x %d = Rs.%.2f%n", r.getString("item_name"), qty, sub);
                    }
                }
            }
            if (lines.isEmpty()) { System.out.println("No items selected."); return; }
            c.setAutoCommit(false);
            try {
                int orderId;
                try (PreparedStatement p=c.prepareStatement(
                    "INSERT INTO orders(customer_id,total_amount,status) VALUES(?,?,'Placed')", Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1,cid); p.setDouble(2,total); p.executeUpdate();
                    try(ResultSet r=p.getGeneratedKeys()){r.next();orderId=r.getInt(1);}
                }
                try (PreparedStatement p=c.prepareStatement(
                    "INSERT INTO order_items(order_id,item_id,quantity,subtotal) VALUES(?,?,?,?)")) {
                    for(Line x:lines){p.setInt(1,orderId);p.setInt(2,x.id);p.setInt(3,x.qty);p.setDouble(4,x.sub);p.addBatch();}
                    p.executeBatch();
                }
                c.commit();
                System.out.println("\\n========== ORDER CONFIRMATION ==========");
                System.out.println("Customer Name : " + customer);
                System.out.println("Order ID      : " + orderId);
                for(Line x:lines) {
                    try(PreparedStatement p=c.prepareStatement("SELECT item_name FROM menu_items WHERE item_id=?")){
                        p.setInt(1,x.id);try(ResultSet r=p.executeQuery()){if(r.next())System.out.println(r.getString(1)+" x "+x.qty+" = Rs."+x.sub);}
                    }
                }
                System.out.printf("Total Amount  : Rs.%.2f%nOrder Status  : Placed%n",total);
                System.out.println("Order saved successfully in MySQL!");
                System.out.println("=========================================");
            } catch(Exception e){c.rollback();throw e;}
            finally {c.setAutoCommit(true);}
        }
    }

    static void updateStatus() throws SQLException {
        int id=number("Order ID: ");
        System.out.println("1. Placed  2. Preparing  3. Completed  4. Cancelled");
        int n=number("New status: ");
        String[] statuses={"","Placed","Preparing","Completed","Cancelled"};
        if(n<1||n>4){System.out.println("Invalid status.");return;}
        try(Connection c=db();PreparedStatement p=c.prepareStatement("UPDATE orders SET status=? WHERE order_id=?")){
            p.setString(1,statuses[n]);p.setInt(2,id);
            System.out.println(p.executeUpdate()>0?"Status updated to "+statuses[n]:"Order ID not found.");
        }
    }

    static void customerOrders() throws SQLException {
        int id=number("Customer ID: ");
        String q="SELECT o.order_id,c.customer_name,o.order_date,o.total_amount,o.status FROM orders o JOIN customers c ON o.customer_id=c.customer_id WHERE c.customer_id=? ORDER BY o.order_id DESC";
        try(Connection c=db();PreparedStatement p=c.prepareStatement(q)){
            p.setInt(1,id);try(ResultSet r=p.executeQuery()){
                System.out.printf("%-9s %-22s %-22s %-12s %-12s%n","ORDER ID","CUSTOMER","DATE","TOTAL","STATUS");
                while(r.next())System.out.printf("%-9d %-22s %-22s Rs.%-8.2f %-12s%n",
                    r.getInt(1),r.getString(2),r.getTimestamp(3),r.getDouble(4),r.getString(5));
            }
        }
    }

    static void dailySales() throws SQLException {
    String q = "SELECT DATE(order_date), COUNT(*), SUM(total_amount) FROM orders WHERE DATE(order_date)=? AND status='Completed' GROUP BY DATE(order_date)";

    try (Connection c = db();
         PreparedStatement p = c.prepareStatement(q)) {

        p.setDate(1, java.sql.Date.valueOf(LocalDate.now()));

        try (ResultSet r = p.executeQuery()) {
            System.out.println("TODAY'S COMPLETED SALES");

            if (r.next()) {
                System.out.printf(
                    "Date: %s | Orders: %d | Revenue: Rs.%.2f%n",
                    r.getDate(1),
                    r.getInt(2),
                    r.getDouble(3)
                );
            } else {
                System.out.println("No completed orders today.");
            }
        }
    }
}
}
