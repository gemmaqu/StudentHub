package com.studenthub.repository;

import com.studenthub.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    @Override
    @EntityGraph(attributePaths = {"course", "uploader"})
    Optional<Resource> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"course", "uploader"})
    Page<Resource> findAll(Specification<Resource> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"course", "uploader"})
    Page<Resource> findByCourseId(Long courseId, Pageable pageable);

    @EntityGraph(attributePaths = {"course", "uploader"})
    Page<Resource> findByUploaderId(Long uploaderId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Resource r SET r.downloadCount = r.downloadCount + 1 WHERE r.id = :id")
    int incrementDownloadCount(@Param("id") Long id);
}
