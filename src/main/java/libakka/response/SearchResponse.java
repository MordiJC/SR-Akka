package libakka.response;

import libakka.Book;

public class SearchResponse implements Response {
    private Book book;

    public SearchResponse(Book book) {
        this.book = book;
    }

    public Book getBook() {
        return book;
    }
}
