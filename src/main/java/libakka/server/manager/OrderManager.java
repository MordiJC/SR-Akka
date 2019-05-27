package libakka.server.manager;

import akka.actor.AbstractActor;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import libakka.request.OrderRequest;
import libakka.server.manager.inator.Orderinator;

import java.time.Duration;

import static akka.actor.SupervisorStrategy.stop;

public class OrderManager extends AbstractActor {

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(OrderRequest.class,
                        streamRequest -> getContext().actorOf(Props.create(Orderinator.class)).tell(streamRequest, getSender())
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
