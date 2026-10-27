import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を使います。
import java.net.InetSocketAddress; // 待ち受ける番号を指定する機能を使います。
import java.net.URLDecoder; // 追加：フォームから届いた日本語を元に戻すために使います。
import java.nio.charset.StandardCharsets; // 追加：UTF-8の文字コードを指定するために使います。
import java.util.ArrayList; // 追加：Todoを入れるリストを作るために使います。
import java.util.List; // 追加：Todoを入れるリストの型を使います。

public class App { // プログラム本体の名前を App にします。
    static List<Todo> todos = new ArrayList<>(); // ★変更
    static int nextId = 1; // ★変更

    public static void main(String[] args) throws Exception { // ここからプログラムを始めます。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080 番で待ち受けるサーバーを用意します。
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更
        egg.setDone(true); // ★変更
        todos.add(egg); // ★変更
        server.createContext("/", exchange -> { // 「/」にアクセスされたときの処理を始めます。
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // 追加：送信方法がGETかPOSTかを取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 移動：通常の応答は文字として返します。
            if (path.equals("/add") && method.equals("POST")) { // 追加：Todoの追加先へPOSTで送られたか調べます。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 追加：送られた内容をUTF-8で読みます。
                String value = body.substring(5); // 追加：「todo=」の後ろを取り出します。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更
                if (!title.isEmpty()) { // ★変更
                    todos.add(new Todo(nextId, title)); // ★変更
                    nextId++; // ★変更
                } // 追加：空かどうかの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // 追加：戻る先を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // 追加：入力後に「/」を開くよう伝えます。
                exchange.close(); // 追加：この応答を閉じます。
                return; // 追加：ここで追加の処理を終えます。
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加
                    try { // ★追加
                        int id = Integer.parseInt(query.substring(3)); // ★追加
                        for (Todo todo : todos) { // ★追加
                            if (todo.getId() == id) { // ★追加
                                todo.setDone(true); // ★追加
                                break; // ★追加
                            } // ★追加
                        } // ★追加
                    } catch (NumberFormatException e) { // ★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加
                String query = exchange.getRequestURI().getQuery(); // ★追加
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加
                    try { // ★追加
                        int id = Integer.parseInt(query.substring(3)); // ★追加
                        todos.removeIf(todo -> todo.getId() == id); // ★追加
                    } catch (NumberFormatException e) { // ★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/")) { // 追加：「/」でフォームとTodo一覧を表示します。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // 追加：入力フォームと箇条書きを始めます。
                for (Todo todo : todos) { // ★変更
                    String mark = ""; // ★変更
                    if (todo.isDone()) { // ★変更
                        mark = " ✔"; // ★変更
                    } // ★変更
                    html += "<li>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId()
                            + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                } // 移動：Todoを取り出す処理を終えます。
                html += "</ul>"; // 移動：箇条書きのHTMLを閉じます。
                message = html; // 移動：組み立てたHTMLを応答にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // 移動：HTMLをUTF-8で返すと伝えます。
            } else { // ★変更
                message = "ページが見つかりません";
            }
            byte[] body = message.getBytes("UTF-8"); // 文字を送信用のデータに変えます。
            exchange.sendResponseHeaders(200, body.length); // 成功したこととデータの長さを伝えます。
            exchange.getResponseBody().write(body); // データをブラウザに送ります。
            exchange.getResponseBody().close(); // 送り終わったことを伝えます。
        }); // 「/」の処理を終えます。
        server.start(); // サーバーを起動します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことを表示します。
    } // プログラムの開始処理を終えます。
} // App の定義を終えます。

class Todo { // ★変更
    private final int id; // ★変更
    private final String title; // ★変更
    private boolean done; // ★変更

    Todo(int id, String title) { // ★変更
        this.id = id; // ★変更
        this.title = title; // ★変更
        this.done = false; // ★変更
    } // ★変更

    int getId() { // ★変更
        return id; // ★変更
    } // ★変更

    String getTitle() { // ★変更
        return title; // ★変更
    } // ★変更

    boolean isDone() { // ★変更
        return done; // ★変更
    } // ★変更

    void setDone(boolean done) { // ★変更
        this.done = done; // ★変更
    } // ★変更
} // ★変更
