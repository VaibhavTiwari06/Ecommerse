package com.ebookstore.backend.book;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeliveryDateCalculatorTest — unit tests for DeliveryDateCalculator.
 *
 * =============================================================
 * WHAT THIS TEST VERIFIES (TEST-CAT-001 partial)
 * =============================================================
 * Pure unit test — no Spring context needed.
 *
 * TEST-DELIVERY-001  Monday → +5 business days → Monday (skip weekends)
 * TEST-DELIVERY-002  Friday → +5 business days → Friday next week
 * TEST-DELIVERY-003  Result is never on a Saturday or Sunday
 * TEST-DELIVERY-004  addBusinessDays(date, 0) returns the same date
 */
class DeliveryDateCalculatorTest {

    /**
     * TEST-DELIVERY-001
     * Start on a Monday — 5 business days later should be next Monday.
     * Mon→Tue(1)→Wed(2)→Thu(3)→Fri(4)→Mon(5) — skips Sat+Sun.
     */
    @Test
    @DisplayName("TEST-DELIVERY-001: Monday + 5 business days = following Monday")
    void mondayPlusFiveBusinessDaysIsFollowingMonday() {
        // Find next Monday from today as a stable test date
        LocalDate monday = LocalDate.of(2026, 8, 24); // a Monday
        LocalDate result = DeliveryDateCalculator.addBusinessDays(monday, 5);
        assertThat(result).isEqualTo(LocalDate.of(2026, 8, 31)); // next Monday
    }

    /**
     * TEST-DELIVERY-002
     * Start on a Friday — 5 business days later should be the Friday of the week after next.
     * Fri→Mon(1)→Tue(2)→Wed(3)→Thu(4)→Fri(5) — skips Sat+Sun.
     */
    @Test
    @DisplayName("TEST-DELIVERY-002: Friday + 5 business days = following Friday")
    void fridayPlusFiveBusinessDaysIsFollowingFriday() {
        LocalDate friday = LocalDate.of(2026, 8, 21); // a Friday
        LocalDate result = DeliveryDateCalculator.addBusinessDays(friday, 5);
        assertThat(result).isEqualTo(LocalDate.of(2026, 8, 28)); // next Friday
    }

    /**
     * TEST-DELIVERY-003
     * The result of addBusinessDays() is never a weekend day,
     * regardless of how many days are added.
     */
    @Test
    @DisplayName("TEST-DELIVERY-003: Result never falls on a weekend")
    void resultNeverFallsOnWeekend() {
        LocalDate start = LocalDate.of(2026, 8, 17); // a Monday
        for (int days = 1; days <= 20; days++) {
            LocalDate result = DeliveryDateCalculator.addBusinessDays(start, days);
            assertThat(result.getDayOfWeek())
                    .as("Adding %d business days should not land on weekend", days)
                    .isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
        }
    }

    /**
     * TEST-DELIVERY-004
     * Adding 0 business days should return the original date.
     */
    @Test
    @DisplayName("TEST-DELIVERY-004: Adding 0 business days returns same date")
    void addingZeroDaysReturnsSameDate() {
        LocalDate date = LocalDate.of(2026, 8, 24);
        LocalDate result = DeliveryDateCalculator.addBusinessDays(date, 0);
        assertThat(result).isEqualTo(date);
    }

    /**
     * TEST-DELIVERY-005
     * tentativeDeliveryDate() returns a future date (not today, not past).
     */
    @Test
    @DisplayName("TEST-DELIVERY-005: tentativeDeliveryDate() is in the future")
    void tentativeDeliveryDateIsInFuture() {
        LocalDate delivery = DeliveryDateCalculator.tentativeDeliveryDate();
        assertThat(delivery).isAfter(LocalDate.now());
    }
}
