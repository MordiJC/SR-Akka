package libakka.server.manager.inator;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import libakka.request.SearchRequest;
import libakka.response.SearchResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static akka.actor.SupervisorStrategy.stop;

public class Databasinator extends AbstractActor {

    private List<String> databases;
    private AtomicInteger counter;
    private AtomicBoolean found;

    public Databasinator(List<String> databases) {
        this.databases = new ArrayList<>(databases);
        this.counter = new AtomicInteger(databases.size());
        this.found = new AtomicBoolean(false);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(SearchRequest.class,
                        searchRequest -> {
                            getContext().become(receive(getSender()));
                            databases.forEach(
                                    db -> getContext().actorOf(Props.create(SearchDatabasinator.class, db))
                                            .tell(searchRequest, getSelf())
                            );
                        })
                .matchAny(
                        o -> System.out.println("Received unknown message: " + o.toString())
                )
                .build();
    }

    private Receive receive(ActorRef sender) {
        return ReceiveBuilder.create()
                .match(SearchResponse.class,
                        searchResponse -> {
                            if (searchResponse.getBook() == null) {
                                if (counter.get() > 1)
                                    counter.decrementAndGet();
                                else if (!found.get()) {
                                    sender.tell(searchResponse, getSelf());
                                    getContext().stop(getSelf());
                                }
                            } else {
                                sender.tell(searchResponse, getSelf());
                                getContext().stop(getSelf());
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
        for (ActorRef each : getContext().getChildren()) {
            getContext().unwatch(each);
            getContext().stop(each);
        }
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(5, Duration.ofSeconds(10), DeciderBuilder.
                    matchAny(o -> stop()).
                    build()
            );

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
