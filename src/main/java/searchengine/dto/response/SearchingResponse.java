package searchengine.dto.response;

import lombok.Getter;
import searchengine.dto.searching.SearchingDto;

import java.util.List;

@Getter
public class SearchingResponse extends SuccessResponse {
    private final int count;
    private final List<SearchingDto> data;

    public SearchingResponse(int count, List<SearchingDto> data) {
        this.count = count;
        this.data = data;
    }
}
