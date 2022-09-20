package my.searchengine.controller.responses;

public class Response {
    private final Boolean result;

    public Response(Boolean result) {
        this.result = result;
    }

    public Boolean getResult() {
        return result;
    }
}
