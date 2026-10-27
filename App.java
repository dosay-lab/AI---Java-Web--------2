import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を使います。
import java.net.InetSocketAddress; // 待ち受ける番号を指定する機能を使います。
import java.net.URLDecoder; // 追加：フォームから届いた日本語を元に戻すために使います。
import java.nio.charset.StandardCharsets; // 追加：UTF-8の文字コードを指定するために使います。
import java.util.ArrayList; // 追加：Todoを入れるリストを作るために使います。
import java.util.List; // 追加：Todoを入れるリストの型を使います。

public class App { // プログラム本体の名前を App にします。
    public static void main(String[] args) throws Exception { // ここからプログラムを始めます。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080 番で待ち受けるサーバーを用意します。
        List<String> todos = new ArrayList<>(); // 追加：Todoを保存する空のリストを一度だけ作ります。
        server.createContext("/", exchange -> { // 「/」にアクセスされたときの処理を始めます。
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // 追加：送信方法がGETかPOSTかを取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 移動：通常の応答は文字として返します。
            if (path.equals("/add") && method.equals("POST")) { // 追加：Todoの追加先へPOSTで送られたか調べます。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 追加：送られた内容をUTF-8で読みます。
                String value = body.substring(5); // 追加：「todo=」の後ろを取り出します。
                String todo = URLDecoder.decode(value, StandardCharsets.UTF_8); // 追加：日本語を元に戻します。
                if (!todo.isEmpty()) { // 追加：空の入力は保存しません。
                    todos.add(todo); // 追加：入力されたTodoをリストに入れます。
                } // 追加：空かどうかの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // 追加：戻る先を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // 追加：入力後に「/」を開くよう伝えます。
                exchange.close(); // 追加：この応答を閉じます。
                return; // 追加：ここで追加の処理を終えます。
            } else if (path.equals("/")) { // 追加：「/」でフォームとTodo一覧を表示します。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // 追加：入力フォームと箇条書きを始めます。
                for (String todo : todos) { // 移動：Todoを1件ずつ取り出します。
                    html += "<li>" + todo + "</li>"; // 移動：1件を箇条書きにします。
                } // 移動：Todoを取り出す処理を終えます。
                html += "</ul>"; // 移動：箇条書きのHTMLを閉じます。
                message = html; // 移動：組み立てたHTMLを応答にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // 移動：HTMLをUTF-8で返すと伝えます。
            } else if (path.equals("/hello")) { // パスが「/hello」かどうか比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLの「?」より後ろを取り出します。
                String name = query.substring(5); // 「name=」の後ろを取り出します。
                message = "こんにちは、" + name + "さん！"; // 名前を応答の文に入れます。
            } else if (path.equals("/bye")) { // パスが「/bye」かどうか比べます。
                message = "さようなら！";
            } else {
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
