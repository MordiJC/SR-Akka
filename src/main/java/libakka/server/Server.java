package libakka.server;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import libakka.request.OrderRequest;
import libakka.request.SearchRequest;
import libakka.request.StreamRequest;
import libakka.response.SearchResponse;
import libakka.server.manager.DatabaseManager;
import libakka.server.manager.OrderManager;
import libakka.server.manager.StreamManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;

import static akka.actor.SupervisorStrategy.restart;

public class Server extends AbstractActor {

    private ActorRef databaseManagerActor;
    private ActorRef orderManagerActor;
    private ActorRef streamManagerActor;

    public Server() {
        databaseManagerActor = context().actorOf(Props.create(DatabaseManager.class), "databaseManagerActor");
        orderManagerActor = context().actorOf(Props.create(OrderManager.class), "orderManagerActor");
        streamManagerActor = context().actorOf(Props.create(StreamManager.class), "streamManagerActor");
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(SearchRequest.class,
                        searchRequest -> databaseManagerActor.tell(searchRequest, getSender())
                )
                .match(OrderRequest.class,
                        orderRequest -> orderManagerActor.tell(orderRequest, getSender())
                )
                .match(StreamRequest.class,
                        streamRequest -> streamManagerActor.tell(streamRequest, getSender())
                )
                .matchAny(
                        o -> System.out.println("[Server.class] Received unknown request: " + o.toString())
                )
                .build();
    }

    private static SupervisorStrategy strategy =
            new OneForOneStrategy(5, Duration.ofSeconds(10), DeciderBuilder.
                    matchAny(o -> restart()).
                    build()
            );

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    private static final String quitCmd = "^\\s*q(uit)?\\s*";

    public static void main(String[] args) {
        Config config = ConfigFactory.load("server");
        ActorSystem system = ActorSystem.create("server", config);
        system.actorOf(Props.create(Server.class), "server");

        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
        String line;

        System.out.println("Welcome to library server.\nTo quit enter command: `q` or `quit`");

        while (true) {
            try {
                line = buffer.readLine();
            } catch (IOException e) {
                break;
            }

            if (line.matches(quitCmd)) {
                System.out.println("Bye bye!");
            }
        }
        system.terminate();
    }
}
