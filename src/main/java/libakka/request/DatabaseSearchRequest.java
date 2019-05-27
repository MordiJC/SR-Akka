package libakka.request;

public class DatabaseSearchRequest extends Request {
    public final String db;

    public DatabaseSearchRequest(String title, String db) {
        super(title);
        this.db = db;
    }
}
