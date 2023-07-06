package searchengine.repositoies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.models.Page;
import searchengine.models.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    void deleteAllBySite(Site site);

    List<Page> findAllBySite(Site site);

    @Query(value = "from Page p where p.path = ?1 and p.site.id = ?2")
    Optional<Page> findByPathAndSiteId(String path, int siteId);

    @Query(value = "select count(p.id) from Page p where p.site.id = :siteId")
    Integer getCountId(int siteId);
}
