package com.ebookstore.backend.book;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * DeliveryDateCalculator — calculates tentative delivery dates.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (TASK-CHK-002, REQ-CHK-003)
 * =============================================================
 * The tentative delivery date is displayed on the Book Detail page
 * (REQ-CAT-004 AC4) and on the Purchase Confirmation page.
 *
 * Rule: order date + 5 BUSINESS DAYS (skip weekends).
 *
 * This is a pure utility class — no Spring beans, no DB access.
 * Static method keeps it stateless and easily testable.
 *
 * EXAMPLE:
 *   Order placed Friday 2026-08-21
 *   +1 → Mon 24 (business day 1)
 *   +2 → Tue 25 (business day 2)
 *   +3 → Wed 26 (business day 3)
 *   +4 → Thu 27 (business day 4)
 *   +5 → Fri 28 (business day 5)
 *   → tentative delivery: Fri 2026-08-28
 */
public final class DeliveryDateCalculator {

    private DeliveryDateCalculator() {
        // Utility class — not instantiable
    }

    /**
     * Returns the date that is {@code businessDays} working days after {@code from}.
     * Saturdays and Sundays are skipped. No public holiday calendar is applied.
     *
     * @param from         the starting date (typically today / order date)
     * @param businessDays number of business days to add (must be > 0)
     * @return the resulting date
     */
    public static LocalDate addBusinessDays(LocalDate from, int businessDays) {
        LocalDate result = from;
        int added = 0;
        while (added < businessDays) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY
                    && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }

    /**
     * Convenience method: today + 5 business days.
     * This is the standard delivery estimate used throughout the application.
     *
     * @return tentative delivery date
     */
    public static LocalDate tentativeDeliveryDate() {
        return addBusinessDays(LocalDate.now(), 5);
    }
}
