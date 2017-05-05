package com.infoclinika.mssharing.model.internal.repository;

import com.infoclinika.mssharing.model.internal.entity.MSFunctionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * @author Oleksii Tymchenko
 */
public interface MSFunctionItemRepository extends JpaRepository<MSFunctionItem,Long> {

    @Query("SELECT m FROM MSFunctionItem m WHERE m.translatedPath = :path")
    Set<MSFunctionItem> findByPath(@Param("path") String path);

    @Query("select count(m.id) from MSFunctionItem m where m.translatedPath = :path")
    Long countByTranslatedPath(@Param("path") String translatedPath);

}
