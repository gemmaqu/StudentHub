package com.studenthub.repository;

import com.studenthub.model.Resource;
import com.studenthub.model.Resource.ResourceType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ResourceSpecifications {

    private ResourceSpecifications() {
    }

    /** Adds a predicate only for filters that were supplied, so the planner can use the matching index. */
    public static Specification<Resource> search(String q, Long courseId, ResourceType type) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(q)) {
                String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, '\\'),
                        cb.like(cb.lower(root.get("description")), pattern, '\\')));
            }
            if (courseId != null) predicates.add(cb.equal(root.get("course").get("id"), courseId));
            if (type != null) predicates.add(cb.equal(root.get("resourceType"), type));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
