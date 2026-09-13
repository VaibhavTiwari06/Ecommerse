package com.ebookstore.backend.publisher;

import jakarta.persistence.*;

/**
 * Publisher — JPA entity mapping to the `publishers` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-001, REQ-CAT-003)
 * =============================================================
 * Books belong to a publisher (Penguin, HarperCollins, etc.).
 * "Brand" browsing (BR-003) lets users filter books by publisher.
 *
 * Publishers are created at startup by BookLoader via
 * PublisherService.findOrCreate(name). There is no admin UI.
 */
@Entity
@Table(name = "publishers")
public class Publisher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Publisher name, e.g. "Penguin Books", "HarperCollins". */
    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    // Required by JPA
    protected Publisher() {}

    public Publisher(String name) {
        this.name = name;
    }

    public Long getId()        { return id; }
    public String getName()    { return name; }
    public void setName(String name) { this.name = name; }
}
