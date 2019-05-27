package libakka.response;

import akka.util.ByteString;

public class StreamResponse implements Response {
    private ByteString streamData;

    public StreamResponse(ByteString stream) {
        this.streamData = stream;
    }

    public ByteString getStreamData() {
        return streamData;
    }
}
