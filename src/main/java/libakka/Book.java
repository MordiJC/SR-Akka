package libakka;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Book implements Serializable {
    public String getTitle() {
        return title;
    }

    public Float getPrice() {
        return price;
    }

    public String getFile() {
        return file;
    }

    private String title;
    private Float price;
    private String file;

    private static final long serialVersionUID = 1L;

    public Book(String title, Float price, String file) {
        this.title = title;
        this.price = price;
        this.file = file;
    }

    @Override
    public String toString() {
        return "'" + title + "\' " + price + " \'" + file + '\'';
    }

    public static Book fromString(String s) {
        Pattern pattern = Pattern.compile("^\\s*\\'(?<title>[^\\']+)\\'\\s+(?<price>\\d+\\.?\\d*)\\s+\\'(?<file>[^\\']*)\\'.*$");
        Matcher matcher = pattern.matcher(s);
        if (!matcher.matches()) {
            throw new IllegalArgumentException();
        }

        return new Book(matcher.group("title"), Float.parseFloat(matcher.group("price")), matcher.group("file"));
    }
}
