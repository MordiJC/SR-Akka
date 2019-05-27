package libakka.server.manager.inator;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import libakka.request.OrderRequest;
import libakka.request.SearchRequest;
import libakka.response.OrderResponse;
import libakka.response.SearchResponse;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

public class Orderinator extends AbstractActor {
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(OrderRequest.class,
                        orderRequest -> getContext().system()
                                .actorSelection("akka.tcp://server@127.0.0.1:1337/user/server/databaseManagerActor")
                                .resolveOne(Duration.ofSeconds(1))
                                .whenComplete((databaseManager, throwable) -> {
                                    databaseManager.tell(new SearchRequest(orderRequest.title), getSelf());
                                    getContext().become(receive(getSender()));
                                })
                                .exceptionally(throwable -> {
                                    getSender().tell(new OrderResponse(false), getSelf());
                                    getContext().stop(getSelf());
                                    return null;
                                })
                )
                .matchAny(
                        o -> System.out.println("Received unknown request: " + o.toString())
                )
                .build();
    }

    private Receive receive(ActorRef sender) {
        return ReceiveBuilder.create()
                .match(SearchResponse.class,
                        searchResponse -> {
                            if (searchResponse.getBook() == null) {
                                sender.tell(new OrderResponse(false), getSelf());
                            } else {
                                String bookEntry = "\'" + searchResponse.getBook().getTitle() + "\'\n";

                                try {
                                    Path p = Paths.get(getClass().getClassLoader().getResource("db/").getPath(), "orders.txt");
                                    Files.write(
                                            p,
                                            bookEntry.getBytes(),
                                            StandardOpenOption.CREATE,
                                            StandardOpenOption.APPEND,
                                            StandardOpenOption.SYNC
                                    );
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                sender.tell(new OrderResponse(true), getSelf());
                                getContext().stop(getSelf());
                            }
                        }
                )
                .matchAny(
                        o -> System.out.println("Received unknown request: " + o.toString())
                )
                .build();
    }
}
