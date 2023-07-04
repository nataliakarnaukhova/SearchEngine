package searchengine.repositoies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.models.SearchIndex;

import java.util.List;
import java.util.Optional;

@Repository
public interface SearchIndexRepository extends JpaRepository<SearchIndex, Integer> {
    @Transactional
    @Modifying
    @Query(value = "delete from SearchIndex s where s.pageId = :pageId")
    void deleteSelectedContains(int pageId);

    @Query(value = "select s.pageId from SearchIndex s where s.lemmaId = :id")
    List<Integer> findPageIdByLemma(int id);

    @Query(value = "from SearchIndex s where s.pageId = ?1 and s.lemmaId = ?2")
    Optional<SearchIndex> findByLemmaId(int pageId, int lemmaId);
}
