package com.ebookstore.backend.category;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CategoryController — handles the category listing endpoint.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-001, REQ-CAT-002)
 * =============================================================
 * Single public endpoint:
 *   GET /api/categories — returns all category names for the browse sidebar.
 *
 * PUBLIC — no JWT required (SecurityConfig permits /api/categories).
 * Guest users can browse by category (REQ-USR-001, REQ-CAT-002).
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * GET /api/categories
     *
     * Returns all categories sorted alphabetically.
     * Used by the frontend browse sidebar (REQ-CAT-002).
     *
     * @return 200 OK with list of CategoryDTO (id, name)
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> listCategories() {
        List<CategoryDTO> categories = categoryService.listAll()
                .stream()
                .map(c -> new CategoryDTO(c.getId(), c.getName()))
                .toList();
        return ResponseEntity.ok(categories);
    }

    /**
     * DTO for a single category — safe to serialise directly.
     * We use a record here: concise, immutable, no boilerplate.
     */
    public record CategoryDTO(Long id, String name) {}
}
