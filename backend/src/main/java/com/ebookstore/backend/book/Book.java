package com.ebookstore.backend.book;

import com.ebookstore.backend.category.Category;
import com.ebookstore.backend.publisher.Publisher;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Book — JPA entity mapping to the `books` table.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CAT-002, REQ-CAT-001 through REQ-CAT-006)
 * =============================================================
 * Central entity of the catalogue. Holds all product information:
 * metadata (ISBN, title, authors, description), pricing, stock level,
 * and foreign keys to Category and Publisher.
 *
 * DESIGN NOTES:
 *
 *   search_vector — the PostgreSQL-generated TSVECTOR column.
 *     It is mapped with insertable=false, updatable=false so that:
 *       a) Hibernate never tries to write to it (PostgreSQL manages it).
 *       b) It is readable from JPQL queries if needed.
 *       c) In H2 test mode (ddl-auto=create-drop) Hibernate creates
 *          the column as VARCHAR(255) — irrelevant since tests don't
 *          exercise native search queries.
 *
 *   price — BigDecimal for exact monetary arithmetic (₹), never double/float.
 *
 *   authors — comma-separated string matching the seed data format.
 *             Example: "J.K. Rowling" or "Frank Herbert, Brian Herbert"
 *
 * RULE: This entity must NEVER be returned directly from a controller.
 * Always map to BookSummaryDTO or BookDetailDTO first.
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ISBN-13 preferred; unique across all books. */
    @Column(name = "isbn", nullable = false, unique = true, length = 13)
    private String isbn;

    /** Full book title, including subtitles. Up to 500 chars. */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Comma-separated author list.
     * Examples: "Toni Morrison"  or  "Frank Herbert, Brian Herbert"
     * TEXT in DB — no length limit.
     */
    @Column(name = "authors", nullable = false, columnDefinition = "TEXT")
    private String authors;

    /** Full book synopsis. TEXT — no length limit. */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Open Library cover image URL. Up to 1000 chars. */
    @Column(name = "cover_image_url", nullable = false, length = 1000)
    private String coverImageUrl;

    /**
     * The publisher this book belongs to.
     * Many books → one publisher (REQ-CAT-003).
     * LAZY loading — publisher data loaded only when accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private Publisher publisher;

    /**
     * Publication year as a string, e.g. "2008".
     * Nullable — not all Open Library entries have a date.
     */
    @Column(name = "published_date", length = 10)
    private String publishedDate;

    /** Total page count. Nullable — not all entries have this. */
    @Column(name = "page_count")
    private Integer pageCount;

    /** ISO 639-1 language code. Default "en". */
    @Column(name = "language", nullable = false, length = 10)
    private String language = "en";

    /**
     * The category this book belongs to.
     * Many books → one category (REQ-CAT-002).
     * LAZY loading.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * Price in Indian Rupees (₹).
     * NUMERIC(10,2) in DB — BigDecimal for exact arithmetic (REQ-NFR-002).
     * DB CHECK constraint: price > 0.
     */
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Number of units in stock.
     * DB CHECK constraint: stock_quantity >= 0.
     */
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity = 0;

    /**
     * PostgreSQL-managed TSVECTOR column for full-text search (CR-002).
     *
     * insertable=false, updatable=false: Hibernate never writes to this column.
     * PostgreSQL's GENERATED ALWAYS AS ... STORED keeps it in sync automatically.
     *
     * In H2 test mode this column is created as VARCHAR(255) by Hibernate
     * and is never queried — tests use a fallback LIKE search.
     */
    @Column(name = "search_vector", insertable = false, updatable = false)
    private String searchVector;

    /** Record creation timestamp — set once on INSERT. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ------------------------------------------------------------------
    // Lifecycle callbacks
    // ------------------------------------------------------------------

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // ------------------------------------------------------------------
    // Getters and setters
    // ------------------------------------------------------------------

    public Long getId()                          { return id; }

    public String getIsbn()                      { return isbn; }
    public void setIsbn(String isbn)             { this.isbn = isbn; }

    public String getTitle()                     { return title; }
    public void setTitle(String title)           { this.title = title; }

    public String getAuthors()                   { return authors; }
    public void setAuthors(String authors)       { this.authors = authors; }

    public String getDescription()               { return description; }
    public void setDescription(String d)         { this.description = d; }

    public String getCoverImageUrl()             { return coverImageUrl; }
    public void setCoverImageUrl(String url)     { this.coverImageUrl = url; }

    public Publisher getPublisher()              { return publisher; }
    public void setPublisher(Publisher p)        { this.publisher = p; }

    public String getPublishedDate()             { return publishedDate; }
    public void setPublishedDate(String d)       { this.publishedDate = d; }

    public Integer getPageCount()                { return pageCount; }
    public void setPageCount(Integer c)          { this.pageCount = c; }

    public String getLanguage()                  { return language; }
    public void setLanguage(String lang)         { this.language = lang; }

    public Category getCategory()                { return category; }
    public void setCategory(Category c)          { this.category = c; }

    public BigDecimal getPrice()                 { return price; }
    public void setPrice(BigDecimal price)       { this.price = price; }

    public int getStockQuantity()                { return stockQuantity; }
    public void setStockQuantity(int qty)        { this.stockQuantity = qty; }

    public String getSearchVector()              { return searchVector; }

    public Instant getCreatedAt()                { return createdAt; }
}
