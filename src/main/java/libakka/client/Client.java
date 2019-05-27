package libakka.client;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import libakka.request.OrderRequest;
import libakka.request.SearchRequest;
import libakka.request.StreamRequest;
import libakka.response.OrderResponse;
import libakka.response.SearchResponse;
import libakka.response.StreamResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client extends AbstractActor {

    private static final String searchCmd = "search\\s+(.+)$";
    private static final String orderCmd = "order\\s+(.+)$";
    private static final String streamCmd = "stream\\s+(.+)$";

    private String serverPath;

    public Client(String serverPath) {
        this.serverPath = serverPath;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().match(String.class,
                s -> {
                    if (s.matches(searchCmd)) {
                        Pattern pattern = Pattern.compile(searchCmd);
                        Matcher matcher = pattern.matcher(s);
                        matcher.matches();
                        getContext().actorSelection(serverPath).tell(new SearchRequest(matcher.group(1)), getSelf());
                    } else if (s.matches(orderCmd)) {
                        Pattern pattern = Pattern.compile(orderCmd);
                        Matcher matcher = pattern.matcher(s);
                        matcher.matches();
                        getContext().actorSelection(serverPath).tell(new OrderRequest(matcher.group(1)), getSelf());
                    } else if (s.matches(streamCmd)) {
                        Pattern pattern = Pattern.compile(streamCmd);
                        Matcher matcher = pattern.matcher(s);
                        matcher.matches();
                        getContext().actorSelection(serverPath).tell(new StreamRequest(matcher.group(1)), getSelf());
                    } else {
                        System.out.println("Incorrect command: `" + s + "`");
                    }
                }
        ).match(SearchResponse.class,
                searchResponse -> {
                    if(searchResponse.getBook() == null) {
                        System.out.println("Search result: None");
                    } else {
                        System.out.println("Search result: " + searchResponse.getBook().toString());
                    }
                }
        ).match(OrderResponse.class,
                orderResponse -> System.out.println("Order status: " + (orderResponse.getStatus() ? "Successful" : "Failed"))
        ).match(StreamResponse.class,
                streamResponse -> System.out.println(streamResponse.getStreamData().utf8String())
        ).matchAny(
                o -> System.out.println("Received unknown response: " + o.toString())
        ).build();
    }

    private static final String SERVER_LOCATION = "akka.tcp://server@127.0.0.1:1337/user/server";
    private static final String helpCmd = "^\\s*h(elp)?\\s*";
    private static final String quitCmd = "^\\s*q(uit)?\\s*";
    private static final String helpMsg = "Help:\n" +
            "search [title] - search for book\n" +
            "order [title]  - order title\n" +
            "stream [title] - stream book\n" +
            "h(elp)         - display this help\n" +
            "q(uit)         - quit an application";

    public static void main(String[] args) {
        Config config = ConfigFactory.load("client");
        ActorSystem system = ActorSystem.create("client", config);
        ActorRef client = system.actorOf(Props.create(Client.class, SERVER_LOCATION), "client");

        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
        String line;

        System.out.println("Welcome to library.\n" + helpMsg);

        while (true) {
            try {
                line = buffer.readLine();
            } catch (IOException e) {
                break;
            }

            if (line.matches(helpCmd)) {
                System.out.println(helpMsg);
            } else if (line.matches(quitCmd)) {
                System.out.println("Bye bye!");
            } else {
                client.tell(line, null);
            }
        }
        system.terminate();
    }
}
