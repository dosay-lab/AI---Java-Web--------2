import com.sun.net.httpserver.HttpServer; // Webサーバーの機能を使います。
import java.net.InetSocketAddress; // 待ち受ける番号を指定する機能を使います。
import java.net.URLDecoder; // 追加：フォームから届いた日本語を元に戻すために使います。
import java.nio.charset.StandardCharsets; // 追加：UTF-8の文字コードを指定するために使います。
import java.nio.file.Files; // ファイルを読み書きします。
import java.nio.file.Path; // 保存先を表します。
import java.util.ArrayList; // 追加：Todoを入れるリストを作るために使います。
import java.util.List; // 追加：Todoを入れるリストの型を使います。

public class App { // プログラム本体の名前を App にします。
    static List<Todo> todos = new ArrayList<>(); // ★変更
    static int nextId = 1; // 次に追加するTodoの番号です。

    static void save() throws java.io.IOException { // 一覧をUTF-8で保存します。
        List<String> lines = new ArrayList<>(); // 書き出す各行を用意します。
        for (Todo todo : todos) { // 全件を順番に処理します。
            String title = todo.getTitle().replace("\r", " ").replace("\n", " "); // 1件を1行に収めます。
            title = "\"" + title.replace("\"", "\"\"") + "\""; // カンマや引用符をCSV形式で保護します。
            lines.add(todo.getId() + "," + (todo.isDone() ? "1" : "0") + "," + title); // id,done,titleの順に並べます。
        }
        Files.write(Path.of("todos.csv"), lines, StandardCharsets.UTF_8); // 全件をファイルへ書き出します。
    }

    static void load() throws java.io.IOException { // 保存済みの一覧を読み込みます。
        Path file = Path.of("todos.csv"); // 保存先を指定します。
        if (!Files.exists(file))
            return; // ファイルがなければ空のまま起動します。
        List<Todo> loaded = new ArrayList<>(); // 読み込み結果を一時的に保持します。
        int maxId = 0; // 最大のidを記録します。
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) { // UTF-8で各行を読みます。
            String[] fields = line.split(",", 3); // id、完了状態、タイトルに分けます。
            if (fields.length != 3)
                throw new java.io.IOException("不正なCSV行: " + line); // 欠けた行を検出します。
            try { // 数値とCSVの引用符を検査します。
                int id = Integer.parseInt(fields[0]); // idを復元します。
                if (id < 1 || id == Integer.MAX_VALUE)
                    throw new IllegalArgumentException(); // 次のidを作れない値を防ぎます。
                if (!fields[1].equals("0") && !fields[1].equals("1"))
                    throw new IllegalArgumentException(); // 完了状態を検査します。
                String value = fields[2]; // タイトル部分を取り出します。
                StringBuilder title = new StringBuilder(); // 引用符を外したタイトルを組み立てます。
                if (value.startsWith("\"")) { // 引用されたタイトルを処理します。
                    if (!value.endsWith("\"") || value.length() < 2)
                        throw new IllegalArgumentException(); // 閉じ引用符を確認します。
                    for (int i = 1; i < value.length() - 1; i++) { // 内容を1文字ずつ読みます。
                        char c = value.charAt(i); // 現在の文字を取得します。
                        if (c == '"') { // 引用符ならエスケープを確認します。
                            if (i + 1 >= value.length() - 1 || value.charAt(++i) != '"')
                                throw new IllegalArgumentException(); // 二重引用符だけを許します。
                        }
                        title.append(c); // 復元した文字を追加します。
                    }
                } else { // 引用されていないタイトルを処理します。
                    title.append(value); // 既存の単純なCSV行にも対応します。
                }
                Todo todo = new Todo(id, title.toString()); // Todoを再作成します。
                todo.setDone(fields[1].equals("1")); // 完了状態を戻します。
                loaded.add(todo); // 復元したTodoを一覧へ追加します。
                maxId = Math.max(maxId, id); // 最大idを更新します。
            } catch (IllegalArgumentException e) { // 不正な値をまとめて扱います。
                throw new java.io.IOException("不正なCSV行: " + line, e); // 読み込みエラーを通知します。
            }
        }
        todos.clear(); // 復元前の一覧を空にします。
        todos.addAll(loaded); // 読み込んだ全件を反映します。
        nextId = maxId + 1; // 最大idの次を採番します。
    }

    public static void main(String[] args) throws Exception { // ここからプログラムを始めます。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080 番で待ち受けるサーバーを用意します。
        load(); // 起動時に保存済みのTodoを復元します。
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
                    save(); // 追加後の一覧を保存します。
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
                                save(); // 完了状態を保存します。
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
                        if (todos.removeIf(todo -> todo.getId() == id))
                            save(); // 削除したとき一覧を保存します。
                    } catch (NumberFormatException e) { // ★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // ★追加
                exchange.sendResponseHeaders(303, -1); // ★追加
                exchange.close(); // ★追加
                return; // ★追加
            } else if (path.equals("/")) { // 追加：「/」でフォームとTodo一覧を表示します。
                String html = "<html><head><meta charset='UTF-8'><title>わたしのTodo</title>"
                        + "<style>body { max-width: 600px; margin: 24px auto; padding: 0 16px; font-size: 16px; } input, button { font-size: 1em; }</style>"
                        + "</head><body><h1>わたしのTodo</h1>"
                        + "<form method='post' action='/add'><input name='todo'><button>追加</button></form>"; // 見出しと最小限のスタイルを追加します。
                if (todos.isEmpty()) {
                    html += "<p>やることは、いまゼロです</p>";
                }
                html += "<ul>";
                for (Todo todo : todos) { // ★変更
                    String title = todo.isDone() ? "<s>" + todo.getTitle() + "</s>" : todo.getTitle();
                    html += "<li>" + title + " <a href='/done?id=" + todo.getId()
                            + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                } // 移動：Todoを取り出す処理を終えます。
                html += "</ul></body></html>"; // 箇条書きとページのHTMLを閉じます。
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
