package searchengine.dto.response;


public class ErrorResponse implements Response {
    private boolean result = false;
    private final String error;

    public ErrorResponse(String error) {
        this.error = error;
    }

    @Override
    public boolean getResult() {
        return result;
    }

    public String getError() {
        return error;
    }
}
