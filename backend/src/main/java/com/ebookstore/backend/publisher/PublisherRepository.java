package com.ebookstore.backend.publisher;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * PublisherRepository — data access for the publishers table.
 *
 * WHY THESE METHODS:
 *   findByName — used by BookLoader.findOrCreate() to check if a
 *                publisher already exists before inserting a new one.
 *   findAll    — used by PublisherController to list all publishers
 *                for the UI brand browsing feature (REQ-CAT-003).
 */
public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    Optional<Publisher> findByName(String name);
}
