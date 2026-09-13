package com.ebookstore.backend.user;

import com.ebookstore.backend.user.dto.AddressDTO;
import com.ebookstore.backend.user.dto.AddressRequestDTO;
import com.ebookstore.backend.user.dto.UserProfileDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UserController — handles user profile and address endpoints.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-USR-006)
 * =============================================================
 * Provides endpoints for:
 *   GET  /api/user/profile    — view own profile
 *   GET  /api/user/giftpoints — view gift point balance
 *   POST /api/user/addresses  — add a delivery address
 *   GET  /api/user/addresses  — list all saved addresses
 *
 * ALL endpoints require JWT authentication (SecurityConfig: anyRequest().authenticated()).
 *
 * HOW WE GET THE CURRENT USER:
 *   @AuthenticationPrincipal User user
 *   Spring injects the User object that was set in the SecurityContext
 *   by JwtAuthenticationFilter. This is the cleanest way to get the
 *   currently authenticated user without calling the DB again.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /api/user/profile
     *
     * Returns the authenticated user's profile data.
     * Safe fields only — no password hash (REQ-USR-006).
     *
     * @param currentUser injected from SecurityContext by Spring
     * @return 200 OK with UserProfileDTO
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getProfile(
            @AuthenticationPrincipal User currentUser) {
        UserProfileDTO profile = userService.getProfile(currentUser.getId());
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /api/user/giftpoints
     *
     * Returns just the gift point balance — lightweight call used
     * by the checkout page before displaying the redemption option.
     *
     * @param currentUser injected from SecurityContext
     * @return 200 OK with {"balance": N}
     */
    @GetMapping("/giftpoints")
    public ResponseEntity<Map<String, Integer>> getGiftPoints(
            @AuthenticationPrincipal User currentUser) {
        int balance = userService.getGiftPointBalance(currentUser.getId());
        return ResponseEntity.ok(Map.of("balance", balance));
    }

    /**
     * POST /api/user/addresses
     *
     * Adds a new delivery address for the authenticated user.
     * A user can have multiple addresses (Decision OQ-002).
     *
     * @param currentUser injected from SecurityContext
     * @param request     address data (validated by @Valid)
     * @return 201 Created with the saved AddressDTO
     */
    @PostMapping("/addresses")
    public ResponseEntity<AddressDTO> addAddress(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddressRequestDTO request) {
        AddressDTO address = userService.addAddress(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    /**
     * GET /api/user/addresses
     *
     * Returns all saved delivery addresses for the authenticated user.
     * Used by the profile page and checkout address selector.
     *
     * @param currentUser injected from SecurityContext
     * @return 200 OK with list of AddressDTOs (empty list if none saved)
     */
    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDTO>> getAddresses(
            @AuthenticationPrincipal User currentUser) {
        List<AddressDTO> addresses = userService.getAddresses(currentUser.getId());
        return ResponseEntity.ok(addresses);
    }
}
