package searchengine.repositoies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.models.Lemma;
import searchengine.models.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    @Query(value = "from Lemma l where l.lemma = :value and l.site.id = :siteId")
    Optional<Lemma> findByLemmaAndSiteId(String value, int siteId);

    void deleteAllBySite(Site site);

    @Query(value = "select count(l.id) from Lemma l where l.site.id = :siteId")
    Integer getCountBySiteId(int siteId);

    @Query(value = "select max(l.frequency) from Lemma l")
    Integer getMaxFrequency();

    List<Lemma> findByLemma(String lemma);

    @Query(value = "select l from Lemma l where l.lemma = :lemma and l.site.id = :siteId")
    List<Lemma> findAllByLemmaAndSiteId(String lemma, int siteId);

    @Query(value = "select count(l.id) from Lemma l")
    Integer getCount();
}
