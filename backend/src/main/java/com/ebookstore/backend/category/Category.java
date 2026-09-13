package com.ebookstore.backend.category;

import jakarta.persistence.*;

/**
 * Category — JPA entity mapping to the `categories` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-001, REQ-CAT-002)
 * =============================================================
 * Books are grouped into categories (Fiction, Science, etc.).
 * A Category has only a unique name — no hierarchy.
 *
 * Categories are created at startup by BookLoader via
 * CategoryService.findOrCreate(name). There is no admin UI.
 *
 * RULE: Entities must NEVER be returned directly from a controller.
 * Use a DTO (or inline record) instead.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable category name, e.g. "Fiction", "Science". */
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    // Required by JPA
    protected Category() {}

    public Category(String name) {
        this.name = name;
    }

    public Long getId()        { return id; }
    public String getName()    { return name; }
    public void setName(String name) { this.name = name; }
}
