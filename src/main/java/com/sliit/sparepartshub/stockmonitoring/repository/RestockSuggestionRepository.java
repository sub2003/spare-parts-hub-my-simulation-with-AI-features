package com.sliit.sparepartshub.stockmonitoring.repository;

import com.sliit.sparepartshub.entity.RestockSuggestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestockSuggestionRepository extends JpaRepository<RestockSuggestion, Integer> {

    @Override
    @EntityGraph(attributePaths = {"product", "reviewedBy"})
    List<RestockSuggestion> findAll();

    @EntityGraph(attributePaths = {"product", "reviewedBy"})
    List<RestockSuggestion> findAllByOrderByCreatedAtDesc();

    @Override
    @EntityGraph(attributePaths = {"product", "reviewedBy"})
    Optional<RestockSuggestion> findById(Integer id);

    @EntityGraph(attributePaths = {"product", "reviewedBy"})
    List<RestockSuggestion> findByStatus(RestockSuggestion.Status status);

    @EntityGraph(attributePaths = {"product", "reviewedBy"})
    List<RestockSuggestion> findByProduct_ProductIdOrderByCreatedAtDesc(Integer productId);
}
