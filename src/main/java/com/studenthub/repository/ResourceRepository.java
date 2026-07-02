package com.studenthub.repository;

import com.studenthub.model.Resource;
import com.studenthub.model.Resource.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Page<Resource> findByCourseId(Long courseId, Pageable pageable);

    Page<Resource> findByUploaderId(Long uploaderId, Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE r.course.id = :courseId AND r.resourceType = :type")
    Page<Resource> findByCourseIdAndType(@Param("courseId") Long courseId,
                                         @Param("type") ResourceType type,
                                         Pageable pageable);

    @Query("SELECT r FROM Resource r WHERE " +
           "(:q IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (:courseId IS NULL OR r.course.id = :courseId) " +
           "AND (:type IS NULL OR r.resourceType = :type)")
    Page<Resource> search(@Param("q") String query,
                          @Param("courseId") Long courseId,
                          @Param("type") ResourceType type,
                          Pageable pageable);

    @Modifying
    @Query("UPDATE Resource r SET r.downloadCount = r.downloadCount + 1 WHERE r.id = :id")
    void incrementDownloadCount(@Param("id") Long id);
}
