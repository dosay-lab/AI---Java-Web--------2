import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を使います。
import java.net.InetSocketAddress; // 待ち受ける番号を指定する機能を使います。

public class App { // プログラム本体の名前を App にします。
    public static void main(String[] args) throws Exception { // ここからプログラムを始めます。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080 番で待ち受けるサーバーを用意します。
        server.createContext("/", exchange -> { // 「/」にアクセスされたときの処理を始めます。
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String message;
            if (path.equals("/hello")) { // パスが「/hello」かどうか比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLの「?」より後ろを取り出します。
                String name = query.substring(5); // 「name=」の後ろを取り出します。
                message = "こんにちは、" + name + "さん！"; // 名前を応答の文に入れます。
            } else if (path.equals("/bye")) { // パスが「/bye」かどうか比べます。
                message = "さようなら！";
            } else {
                message = "ページが見つかりません";
            }
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 文字を UTF-8 で返すと伝えます。
            byte[] body = message.getBytes("UTF-8"); // 文字を送信用のデータに変えます。
            exchange.sendResponseHeaders(200, body.length); // 成功したこととデータの長さを伝えます。
            exchange.getResponseBody().write(body); // データをブラウザに送ります。
            exchange.getResponseBody().close(); // 送り終わったことを伝えます。
        }); // 「/」の処理を終えます。
        server.start(); // サーバーを起動します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことを表示します。
    } // プログラムの開始処理を終えます。
} // App の定義を終えます。
