import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection; // ★
import java.sql.DriverManager; // ★
import java.sql.PreparedStatement; // ★
import java.sql.ResultSet; // ★
import java.sql.SQLException; // ★
import java.sql.Statement; // ★

public class App {
    private static final String DB_URL = "jdbc:sqlite:todos.db"; // ★

    private static Connection connect() throws SQLException { // ★
        return DriverManager.getConnection(DB_URL); // ★
    } // ★

    private static void initDb() throws SQLException { // ★
        try (Connection db = connect(); Statement stmt = db.createStatement()) { // ★
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS todos (id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"); // ★
        } // ★
    } // ★

    private static void add(String title) throws SQLException { // ★
        try (Connection db = connect();
                PreparedStatement stmt = db.prepareStatement("INSERT INTO todos (title, done) VALUES (?, 0)")) { // ★
            stmt.setString(1, title); // ★
            stmt.executeUpdate(); // ★
        } // ★
    } // ★

    private static void done(int id) throws SQLException { // ★
        try (Connection db = connect();
                PreparedStatement stmt = db.prepareStatement("UPDATE todos SET done = 1 WHERE id = ?")) { // ★
            stmt.setInt(1, id); // ★
            stmt.executeUpdate(); // ★
        } // ★
    } // ★

    private static void delete(int id) throws SQLException { // ★
        try (Connection db = connect();
                PreparedStatement stmt = db.prepareStatement("DELETE FROM todos WHERE id = ?")) { // ★
            stmt.setInt(1, id); // ★
            stmt.executeUpdate(); // ★
        } // ★
    } // ★

    private static String escape(String s) { // ★
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;"); // ★
    } // ★

    private static String page() throws SQLException { // ★
        StringBuilder html = new StringBuilder("<html><head><meta charset='UTF-8'><title>わたしのTodo</title>"
                + "<style>body { max-width: 600px; margin: 24px auto; padding: 0 16px; font-size: 16px; } "
                + "input, button { font-size: 1em; }</style></head><body><h1>わたしのTodo</h1>"
                + "<form method='post' action='/add'><input name='todo'><button>追加</button></form>");
        try (Connection db = connect();
                PreparedStatement stmt = db.prepareStatement("SELECT id, title, done FROM todos ORDER BY id");
                ResultSet rows = stmt.executeQuery()) { // ★
            boolean empty = true; // ★
            html.append("<ul>");
            while (rows.next()) { // ★
                empty = false; // ★
                int id = rows.getInt("id"); // ★
                String title = escape(rows.getString("title")); // ★
                if (rows.getInt("done") != 0)
                    title = "<s>" + title + "</s>"; // ★
                html.append("<li>").append(title).append(" <a href='/done?id=").append(id)
                        .append("'>完了</a> <a href='/delete?id=").append(id).append("'>削除</a></li>"); // ★
            } // ★
            html.append("</ul>");
            if (empty)
                html.append("<p>やることは、いまゼロです</p>"); // ★
        } // ★
        return html.append("</body></html>").toString();
    } // ★

    private static void redirect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static void respond(HttpExchange exchange, int status, String type, String message)
            throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    public static void main(String[] args) throws Exception {
        initDb(); // ★
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            try { // ★
                if (path.equals("/add") && method.equals("POST")) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    if (body.startsWith("todo=")) {
                        String title = URLDecoder.decode(body.substring(5), StandardCharsets.UTF_8);
                        if (!title.isEmpty())
                            add(title); // ★
                    }
                    redirect(exchange);
                } else if ((path.equals("/done") || path.equals("/delete")) && method.equals("GET")) {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("id=")) {
                        try {
                            int id = Integer.parseInt(query.substring(3));
                            if (path.equals("/done"))
                                done(id);
                            else
                                delete(id); // ★
                        } catch (NumberFormatException ignored) {
                            // 不正なIDは処理しない。
                        }
                    }
                    redirect(exchange);
                } else if (path.equals("/") && method.equals("GET")) {
                    respond(exchange, 200, "text/html", page()); // ★
                } else {
                    respond(exchange, 404, "text/plain", "ページが見つかりません");
                }
            } catch (SQLException e) { // ★
                e.printStackTrace(); // ★
                respond(exchange, 500, "text/plain", "データベースエラー"); // ★
            }
        });
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （停止: Ctrl+C）");
    }
}
