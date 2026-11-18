import java.sql.*;
import java.util.Scanner;

public class FoodOrdering {
    private static final String URL = "jdbc:mysql://localhost:3306/food_ordering_db";
    private static final String USER = "root";

    public static void main(String[] args) {
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            System.out.println("Set DB_PASSWORD in this terminal before running.");
            return;
        }
        try (Connection con = DriverManager.getConnection(URL, USER, password);
             Scanner sc = new Scanner(System.in)) {
            System.out.println("AVAILABLE MENU");
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT item_id,item_name,price FROM menu_items WHERE availability=TRUE")) {
                while (rs.next()) System.out.printf("%d. %s - Rs. %.2f%n",rs.getInt(1),rs.getString(2),rs.getDouble(3));
            }
            System.out.print("Customer name: ");
            String name=sc.nextLine().trim();
            if(name.isEmpty()) throw new IllegalArgumentException("Customer name is required.");
            con.setAutoCommit(false);
            try {
                int customerId, orderId;
                try(PreparedStatement ps=con.prepareStatement("INSERT INTO customers(customer_name) VALUES(?)",Statement.RETURN_GENERATED_KEYS)){
                    ps.setString(1,name); ps.executeUpdate();
                    try(ResultSet k=ps.getGeneratedKeys()){k.next();customerId=k.getInt(1);}
                }
                try(PreparedStatement ps=con.prepareStatement("INSERT INTO orders(customer_id,total_amount,status) VALUES(?,0,'Placed')",Statement.RETURN_GENERATED_KEYS)){
                    ps.setInt(1,customerId);ps.executeUpdate();
                    try(ResultSet k=ps.getGeneratedKeys()){k.next();orderId=k.getInt(1);}
                }
                double total=0;
                while(true){
                    System.out.print("Menu item ID (0 to finish): ");
                    int id=Integer.parseInt(sc.nextLine());
                    if(id==0) break;
                    System.out.print("Quantity: ");
                    int qty=Integer.parseInt(sc.nextLine());
                    if(qty<=0) throw new IllegalArgumentException("Quantity must be positive.");
                    double price;
                    try(PreparedStatement ps=con.prepareStatement("SELECT price FROM menu_items WHERE item_id=? AND availability=TRUE")){
                        ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Item unavailable.");price=rs.getDouble(1);}
                    }
                    double subtotal=price*qty;total+=subtotal;
                    try(PreparedStatement ps=con.prepareStatement("INSERT INTO order_items(order_id,item_id,quantity,subtotal) VALUES(?,?,?,?)")){
                        ps.setInt(1,orderId);ps.setInt(2,id);ps.setInt(3,qty);ps.setDouble(4,subtotal);ps.executeUpdate();
                    }
                }
                if(total<=0) throw new IllegalArgumentException("Order must contain at least one item.");
                try(PreparedStatement ps=con.prepareStatement("UPDATE orders SET total_amount=? WHERE order_id=?")){
                    ps.setDouble(1,total);ps.setInt(2,orderId);ps.executeUpdate();
                }
                con.commit();System.out.printf("Order #%d saved. Total: Rs. %.2f%n",orderId,total);
            } catch(Exception e){con.rollback();throw e;}
            finally{con.setAutoCommit(true);}
        } catch(Exception e){System.err.println("Order failed: "+e.getMessage());}
    }
}
