package libakka.server.manager.inator;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import libakka.Book;
import libakka.request.SearchRequest;
import libakka.response.SearchResponse;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchDatabasinator extends AbstractActor {
    private String database;

    public SearchDatabasinator(String database) {
        this.database = database;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(SearchRequest.class,
                        searchRequest -> {
                            try (Stream<String> stream = Files.lines(Paths.get(database))) {
                                List<Book> books = stream.map(Book::fromString).filter(book -> book.getTitle().equals(searchRequest.title)).collect(Collectors.toList());
                                if (books.size() > 0) {
                                    getSender().tell(new SearchResponse(books.get(0)), getSelf());
                                } else {
                                    getSender().tell(new SearchResponse(null), getSelf());
                                }
                            }
                        }
                )
                .matchAny(
                        o -> System.out.println("Received unknown message: " + o.toString())
                )
                .build();
    }

    @Override
    public void postStop() {
        getContext().parent().tell(new SearchResponse(null), getSelf());
    }
}
