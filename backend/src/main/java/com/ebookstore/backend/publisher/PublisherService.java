package com.ebookstore.backend.publisher;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PublisherService — business logic for the publisher module.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-001, REQ-CAT-003)
 * =============================================================
 * Two responsibilities:
 *
 *  1. listAll()       — return all publishers for the browse sidebar.
 *  2. findOrCreate()  — used by BookLoader to get-or-insert a publisher
 *                       by name during seed loading. Prevents duplicates.
 */
@Service
public class PublisherService {

    private final PublisherRepository publisherRepository;

    public PublisherService(PublisherRepository publisherRepository) {
        this.publisherRepository = publisherRepository;
    }

    /**
     * Returns all publishers ordered alphabetically.
     * Used by GET /api/publishers (REQ-CAT-003).
     */
    public List<Publisher> listAll() {
        return publisherRepository.findAll()
                .stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    /**
     * Finds an existing publisher by name, or creates and saves a new one.
     * Called by BookLoader during catalogue seed loading.
     *
     * @param name the publisher name (e.g. "Penguin Books")
     * @return the existing or newly created Publisher entity
     */
    @Transactional
    public Publisher findOrCreate(String name) {
        return publisherRepository.findByName(name)
                .orElseGet(() -> publisherRepository.save(new Publisher(name)));
    }
}
