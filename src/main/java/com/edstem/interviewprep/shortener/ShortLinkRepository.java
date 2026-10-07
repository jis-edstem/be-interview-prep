package com.edstem.interviewprep.shortener;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ShortLinkRepository extends JpaRepository<ShortLink, Long> {

    Optional<ShortLink> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying
    @Query("update ShortLink link set link.visitCount = link.visitCount + 1 where link.id = :id")
    void incrementVisitCount(Long id);
}
