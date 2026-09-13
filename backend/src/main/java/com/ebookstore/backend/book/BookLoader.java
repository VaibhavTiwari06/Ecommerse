package com.ebookstore.backend.book;

import com.ebookstore.backend.category.Category;
import com.ebookstore.backend.category.CategoryService;
import com.ebookstore.backend.publisher.Publisher;
import com.ebookstore.backend.publisher.PublisherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * BookLoader — seeds the book catalogue from books.json on application startup.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-003, REQ-SEED-002)
 * =============================================================
 * Implements ApplicationRunner so it runs once after the Spring context
 * is fully initialised (after Flyway migrations have been applied).
 *
 * LOADING STRATEGY:
 *   - Reads books.json from the classpath (src/main/resources/data/books.json)
 *   - For each book, checks existsByIsbn() FIRST — skips if already loaded
 *   - Creates Category and Publisher entries via findOrCreate() — idempotent
 *   - Saves the Book entity
 *
 * IDEMPOTENCY:
 *   Running the application multiple times does NOT cause duplicate data.
 *   If even one book with matching ISBN exists, that entry is skipped.
 *   This means a partial load (e.g. first startup crashed) is safely resumed.
 *
 * BOOKS.JSON LOCATION:
 *   The file lives at data/seed/books.json in the project root.
 *   It is copied to src/main/resources/data/books.json (classpath)
 *   so Spring can load it at runtime.
 *
 *   The classpath resource is registered in application.properties:
 *     (no extra config needed — any file under src/main/resources is
 *     automatically on the classpath)
 *
 * JSON STRUCTURE (per book):
 *   {
 *     "isbn": "9781...",
 *     "title": "...",
 *     "authors": ["Author One", "Author Two"],   ← array
 *     "description": "...",
 *     "coverImageUrl": "https://...",
 *     "publisher": "Penguin Books",              ← string
 *     "publishedDate": "2008",
 *     "pageCount": 320,
 *     "language": "en",
 *     "category": "Fiction",                     ← string
 *     "price": 499.00,
 *     "stockQuantity": 10
 *   }
 *
 * NOTE ON TRANSACTIONS:
 *   The entire run() method is @Transactional. If parsing fails midway,
 *   nothing is committed. Individual per-book failures are caught and
 *   logged — one bad entry does not abort the whole load.
 *
 * NOTE ON ObjectMapper:
 *   ObjectMapper is instantiated directly rather than injected via Spring DI.
 *   In Spring Boot 4.x with spring-boot-starter-webmvc the Jackson
 *   ObjectMapper bean is registered by JacksonAutoConfiguration only after
 *   full MVC context startup — it is not reliably available for injection
 *   during ApplicationRunner execution. Creating it directly is the
 *   correct and simplest approach for a seed loader.
 */
@Component
public class BookLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BookLoader.class);

    /**
     * Classpath location of the seed file.
     * The file must be placed at:
     *   src/main/resources/data/books.json
     */
    private static final String SEED_FILE = "data/books.json";

    /**
     * ObjectMapper created directly — not injected.
     * See class-level Javadoc for the rationale.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BookRepository    bookRepository;
    private final CategoryService   categoryService;
    private final PublisherService  publisherService;

    public BookLoader(BookRepository bookRepository,
                      CategoryService categoryService,
                      PublisherService publisherService) {
        this.bookRepository   = bookRepository;
        this.categoryService  = categoryService;
        this.publisherService = publisherService;
    }

    /**
     * Runs after the Spring context starts.
     * Reads books.json and inserts any books not already in the database.
     *
     * REQ-SEED-002
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        ClassPathResource resource = new ClassPathResource(SEED_FILE);

        if (!resource.exists()) {
            log.warn("BookLoader: seed file not found at classpath:{} — skipping catalogue load",
                    SEED_FILE);
            return;
        }

        List<BookRecord> records;
        try (InputStream is = resource.getInputStream()) {
            records = OBJECT_MAPPER.readValue(is,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, BookRecord.class));
        } catch (Exception e) {
            log.error("BookLoader: failed to parse {}: {}", SEED_FILE, e.getMessage(), e);
            return;
        }

        log.info("BookLoader: found {} books in seed file", records.size());
        int loaded = 0;
        int skipped = 0;

        for (BookRecord rec : records) {
            try {
                if (bookRepository.existsByIsbn(rec.isbn())) {
                    skipped++;
                    continue;
                }

                Category  category  = categoryService.findOrCreate(rec.category());
                Publisher publisher = publisherService.findOrCreate(rec.publisher());

                Book book = new Book();
                book.setIsbn(rec.isbn());
                book.setTitle(rec.title());
                // authors is a JSON array — join to comma-separated string
                book.setAuthors(String.join(", ", rec.authors()));
                book.setDescription(rec.description());
                book.setCoverImageUrl(rec.coverImageUrl());
                book.setPublisher(publisher);
                book.setPublishedDate(rec.publishedDate());
                book.setPageCount(rec.pageCount());
                book.setLanguage(rec.language() != null ? rec.language() : "en");
                book.setCategory(category);
                // price may arrive as integer (e.g. 899) or decimal (734.5)
                book.setPrice(BigDecimal.valueOf(rec.price()));
                book.setStockQuantity(rec.stockQuantity());

                bookRepository.save(book);
                loaded++;

            } catch (Exception e) {
                log.error("BookLoader: failed to load book isbn={}: {}",
                        rec.isbn(), e.getMessage(), e);
            }
        }

        log.info("BookLoader: loaded={} skipped(already existed)={}", loaded, skipped);
    }

    // ------------------------------------------------------------------
    // Inner record — models one JSON entry in books.json
    // ------------------------------------------------------------------

    /**
     * BookRecord — deserialization target for a single books.json entry.
     *
     * Jackson maps JSON field names to record component names.
     * The "authors" field is a JSON array of strings in the seed file.
     * All other fields are scalars.
     */
    private record BookRecord(
            String isbn,
            String title,
            List<String> authors,
            String description,
            String coverImageUrl,
            String publisher,
            String publishedDate,
            Integer pageCount,
            String language,
            String category,
            double price,
            int stockQuantity
    ) {}
}
