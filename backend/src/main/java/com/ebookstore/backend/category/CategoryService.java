package com.ebookstore.backend.category;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CategoryService — business logic for the category module.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-001, REQ-CAT-002)
 * =============================================================
 * Two responsibilities:
 *
 *  1. listAll()       — return all categories for the browse sidebar.
 *  2. findOrCreate()  — used by BookLoader to get-or-insert a category
 *                       by name during seed loading. Prevents duplicates.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Returns all categories ordered by name.
     * Used by GET /api/categories (REQ-CAT-002).
     */
    public List<Category> listAll() {
        return categoryRepository.findAll()
                .stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    /**
     * Finds an existing category by name, or creates and saves a new one.
     * Called by BookLoader during catalogue seed loading.
     *
     * @param name the category name (e.g. "Fiction")
     * @return the existing or newly created Category entity
     */
    @Transactional
    public Category findOrCreate(String name) {
        return categoryRepository.findByName(name)
                .orElseGet(() -> categoryRepository.save(new Category(name)));
    }
}
