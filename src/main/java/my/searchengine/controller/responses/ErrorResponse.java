package my.searchengine.controller.responses;

public class ErrorResponse extends Response{
    private final String error;

    public ErrorResponse(String error) {
        super(false);
        this.error = error;
    }

    public String getError() {
        return error;
    }
}
