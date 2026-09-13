package com.ebookstore.backend.publisher;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PublisherController — handles the publisher listing endpoint.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-001, REQ-CAT-003)
 * =============================================================
 * Single public endpoint:
 *   GET /api/publishers — returns all publisher names for brand browsing.
 *
 * PUBLIC — no JWT required (SecurityConfig permits /api/publishers).
 * Guest users can browse by publisher (REQ-USR-001, REQ-CAT-003).
 */
@RestController
@RequestMapping("/api/publishers")
public class PublisherController {

    private final PublisherService publisherService;

    public PublisherController(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    /**
     * GET /api/publishers
     *
     * Returns all publishers sorted alphabetically.
     * Used by the frontend brand browse sidebar (REQ-CAT-003).
     *
     * @return 200 OK with list of PublisherDTO (id, name)
     */
    @GetMapping
    public ResponseEntity<List<PublisherDTO>> listPublishers() {
        List<PublisherDTO> publishers = publisherService.listAll()
                .stream()
                .map(p -> new PublisherDTO(p.getId(), p.getName()))
                .toList();
        return ResponseEntity.ok(publishers);
    }

    /**
     * DTO for a single publisher.
     */
    public record PublisherDTO(Long id, String name) {}
}
