import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class FoodOrderServer {
    private static final String URL="jdbc:mysql://localhost:3306/food_ordering_db";
    private static final String USER="root";
    private static final String PASSWORD=System.getenv("DB_PASSWORD");

    public static void main(String[] args) throws Exception {
        if(PASSWORD==null||PASSWORD.isBlank()) throw new IllegalStateException("DB_PASSWORD is not configured.");
        HttpServer server=HttpServer.create(new InetSocketAddress(8080),0);
        server.createContext("/order",FoodOrderServer::handleOrder);
        server.start();
        System.out.println("Food Order Server running on port 8080");
    }
    private static void handleOrder(HttpExchange ex)throws IOException{
        ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods","POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers","Content-Type");
        if("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())){ex.sendResponseHeaders(204,-1);return;}
        if(!"POST".equalsIgnoreCase(ex.getRequestMethod())){send(ex,405,new JSONObject().put("success",false).put("message","Use POST."));return;}
        try{
            JSONObject req=new JSONObject(new String(ex.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            String name=req.optString("customerName","").trim();
            String phone=req.optString("phone","").trim();
            JSONArray items=req.optJSONArray("items");
            if(name.isEmpty()||items==null||items.isEmpty())throw new IllegalArgumentException("Enter customer name and add at least one item.");
            int orderId;double total=0;
            try(Connection con=DriverManager.getConnection(URL,USER,PASSWORD)){
                con.setAutoCommit(false);
                try{
                    int customerId;
                    try(PreparedStatement ps=con.prepareStatement("INSERT INTO customers(customer_name,phone) VALUES(?,?)",Statement.RETURN_GENERATED_KEYS)){
                        ps.setString(1,name);ps.setString(2,phone.isEmpty()?null:phone);ps.executeUpdate();
                        try(ResultSet k=ps.getGeneratedKeys()){k.next();customerId=k.getInt(1);}
                    }
                    try(PreparedStatement ps=con.prepareStatement("INSERT INTO orders(customer_id,total_amount,status) VALUES(?,0,'Placed')",Statement.RETURN_GENERATED_KEYS)){
                        ps.setInt(1,customerId);ps.executeUpdate();try(ResultSet k=ps.getGeneratedKeys()){k.next();orderId=k.getInt(1);}
                    }
                    for(int i=0;i<items.length();i++){
                        JSONObject item=items.getJSONObject(i);int id=item.getInt("itemId");int qty=item.getInt("quantity");
                        if(qty<=0)throw new IllegalArgumentException("Quantity must be greater than zero.");
                        double price;String itemName;
                        try(PreparedStatement ps=con.prepareStatement("SELECT item_name,price FROM menu_items WHERE item_id=? AND availability=TRUE")){
                            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Menu item unavailable: "+id);itemName=rs.getString(1);price=rs.getDouble(2);}
                        }
                        double subtotal=price*qty;total+=subtotal;
                        try(PreparedStatement ps=con.prepareStatement("INSERT INTO order_items(order_id,item_id,quantity,subtotal) VALUES(?,?,?,?)")){
                            ps.setInt(1,orderId);ps.setInt(2,id);ps.setInt(3,qty);ps.setDouble(4,subtotal);ps.executeUpdate();
                        }
                        System.out.println(itemName+" x "+qty+" = Rs. "+subtotal);
                    }
                    try(PreparedStatement ps=con.prepareStatement("UPDATE orders SET total_amount=? WHERE order_id=?")){
                        ps.setDouble(1,total);ps.setInt(2,orderId);ps.executeUpdate();
                    }
                    con.commit();
                }catch(Exception e){con.rollback();throw e;}
            }
            System.out.println("Order #"+orderId+" | "+name+" | Rs. "+total);
            send(ex,200,new JSONObject().put("success",true).put("orderId",orderId).put("totalAmount",total).put("status","Placed").put("message","Order placed successfully."));
        }catch(Exception e){e.printStackTrace();send(ex,400,new JSONObject().put("success",false).put("message",e.getMessage()));}
    }
    private static void send(HttpExchange ex,int status,JSONObject data)throws IOException{
        byte[] b=data.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");
        ex.sendResponseHeaders(status,b.length);try(OutputStream out=ex.getResponseBody()){out.write(b);}
    }
}
