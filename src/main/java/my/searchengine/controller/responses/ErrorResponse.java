package my.searchengine.controller.responses;

import lombok.Getter;

@Getter
public class ErrorResponse extends Response{
    private final String error;

    public ErrorResponse(String error) {
        super(false);
        this.error = error;
    }
}
