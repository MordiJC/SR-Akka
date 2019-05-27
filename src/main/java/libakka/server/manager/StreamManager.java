package libakka.server.manager;

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import libakka.request.StreamRequest;
import libakka.server.manager.inator.Streaminator;

import java.time.Duration;

import static akka.actor.SupervisorStrategy.stop;

public class StreamManager extends AbstractActor {
    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(StreamRequest.class,
                        streamRequest -> getContext().actorOf(Props.create(Streaminator.class, streamRequest.title)).tell(streamRequest, getSender())
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
