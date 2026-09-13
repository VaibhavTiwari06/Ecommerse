package com.ebookstore.backend.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * CategoryRepository — data access for the categories table.
 *
 * WHY THESE METHODS:
 *   findByName   — used by BookLoader.findOrCreate() to look up an existing
 *                  category before deciding whether to insert a new one.
 *   findAll      — used by CategoryController to list all categories
 *                  for the UI browse sidebar (REQ-CAT-002).
 *
 * Spring Data JPA generates the SQL for both from the method names.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);
}
