package libakka.server.manager.inator;

import akka.Done;
import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import akka.stream.ThrottleMode;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import libakka.request.SearchRequest;
import libakka.request.StreamRequest;
import libakka.response.SearchResponse;
import libakka.response.StreamResponse;
import libakka.server.manager.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class Streaminator extends AbstractActor {

    private ActorSystem system;
    private ActorMaterializer materializer;
    private String title;

    public Streaminator(String title) {
        system = getContext().system();
        materializer = ActorMaterializer.create(system);
        this.title = title;
    }


    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(StreamRequest.class,
                        streamRequest -> system.actorSelection("akka.tcp://server@127.0.0.1:1337/user/server/databaseManagerActor")
                                .resolveOne(Duration.ofSeconds(1))
                                .whenComplete((databaseManager, throwable) -> {
                                            databaseManager.tell(new SearchRequest(title), getSelf());
                                            getContext().become(receive(getSender()));
                                        }
                                ).exceptionally(throwable -> {
                                    getSender().tell(new StreamResponse(ByteString.fromString("Database access failure.")), getSelf());
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
                                sender.tell(new StreamResponse(ByteString.fromString("Book not found!")), getSelf());
                                getContext().stop(getSelf());
                            } else {
                                if (searchResponse.getBook().getFile().trim().isEmpty()) {
                                    sender.tell(new StreamResponse(ByteString.fromString("Book file not found!")), getSelf());
                                    getContext().stop(getSelf());
                                    return;
                                }
                                File bookFile;
                                try {
                                    bookFile = Paths.get(getClass().getClassLoader().getResource("db/book/" + searchResponse.getBook().getFile()).toURI()).toFile();
                                } catch (URISyntaxException e) {
                                    sender.tell(new StreamResponse(ByteString.fromString("Book file not found!")), getSelf());
                                    getContext().stop(getSelf());
                                    return;
                                }

                                if (!bookFile.exists()) {
                                    sender.tell(new StreamResponse(ByteString.fromString("Book file not found!")), getSelf());
                                    getContext().stop(getSelf());
                                    return;
                                }

                                Flow<ByteString, ByteString, NotUsed> flow =
                                        Framing.delimiter(
                                                ByteString.fromString(System.lineSeparator()),
                                                512,
                                                FramingTruncation.ALLOW)
                                                .throttle(1, Duration.ofSeconds(1), 1, ThrottleMode.shaping());

                                Sink<ByteString, CompletionStage<Done>> sink = Sink.foreach(param -> sender.tell(new StreamResponse(param), getSelf()));

                                List<ByteString> lines = new ArrayList();

                                try {
                                    lines = Files.readAllLines(bookFile.toPath()).stream().map(ByteString::fromString).collect(Collectors.toList());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                Source.from(lines).via(flow).runWith(sink, materializer).thenRun(() -> getContext().stop(getSelf()));
                            }
                        }
                )
                .matchAny(
                        o -> System.out.println("Received unknown request: " + o.toString())
                )
                .build();
    }
}
