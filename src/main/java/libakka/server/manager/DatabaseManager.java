package libakka.server.manager;

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import libakka.request.SearchRequest;
import libakka.server.manager.inator.Databasinator;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static akka.actor.SupervisorStrategy.stop;

public class DatabaseManager extends AbstractActor {
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(SearchRequest.class,
                        streamRequest -> {
                            List<String> dbs = List.of(
                                    Objects.requireNonNull(getClass().getClassLoader().getResource("db/db1.txt")).getPath(),
                                    Objects.requireNonNull(getClass().getClassLoader().getResource("db/db2.txt")).getPath()
                            );
                            getContext().actorOf(Props.create(Databasinator.class, dbs)).tell(streamRequest, getSender());
                        }
                )
                .matchAny(
                        o -> System.out.println("Received unknown message: " + o.toString())
                )
                .build();
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
