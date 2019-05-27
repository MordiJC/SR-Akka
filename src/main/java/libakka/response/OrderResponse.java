package libakka.response;

public class OrderResponse implements Response {
    private Boolean status;

    public OrderResponse(Boolean status) {
        this.status = status;
    }

    public Boolean getStatus() {
        return status;
    }
}
