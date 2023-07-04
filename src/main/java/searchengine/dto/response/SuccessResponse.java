package searchengine.dto.response;

import lombok.Getter;

@Getter
public class SuccessResponse implements Response {
    private final boolean result = true;

    @Override
    public boolean getResult() {
        return result;
    }
}
