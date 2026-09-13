package com.ebookstore.backend.user;

import com.ebookstore.backend.user.dto.AddressDTO;
import com.ebookstore.backend.user.dto.AddressRequestDTO;
import com.ebookstore.backend.user.dto.UserProfileDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * UserService — handles user profile and delivery address business logic.
 *
 * =============================================================
 * WHY THIS CLASS EXISTS (REQ-USR-006, REQ-CHK-001)
 * =============================================================
 * Provides:
 *  - getProfile()         : return safe user data (no password hash)
 *  - getGiftPointBalance(): return current gift point total
 *  - addAddress()         : save a new delivery address for the user
 *  - getAddresses()       : list all saved addresses for the user
 *
 * These are called by UserController and AddressController.
 * They may also be called by CheckoutService (to list addresses
 * during checkout summary).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public UserService(UserRepository userRepository,
                       AddressRepository addressRepository) {
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
    }

    // ------------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------------

    /**
     * Returns the user's profile data as a safe DTO (no password hash).
     *
     * @param userId the authenticated user's ID (from JWT)
     * @return UserProfileDTO with name, email, phone, gift point balance
     * @throws EntityNotFoundException if the user ID doesn't exist (shouldn't
     *         happen for a valid JWT, but guarded just in case)
     */
    public UserProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                    new EntityNotFoundException("User not found: " + userId));

        return new UserProfileDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getGiftPointBalance()
        );
    }

    /**
     * Returns the user's current gift point balance.
     * Exposed as a separate lightweight endpoint for the checkout page.
     *
     * @param userId the authenticated user's ID
     * @return current gift point balance (always >= 0)
     */
    public int getGiftPointBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                    new EntityNotFoundException("User not found: " + userId));
        return user.getGiftPointBalance();
    }

    // ------------------------------------------------------------------
    // Addresses
    // ------------------------------------------------------------------

    /**
     * Saves a new delivery address for the user.
     *
     * If isDefault=true in the request, we set this address as default.
     * We don't unset other defaults here — the frontend can highlight
     * the most recently added default. Future enhancement could unset
     * previous defaults.
     *
     * @param userId  the authenticated user's ID
     * @param request address data from the request body
     * @return the saved address as an AddressDTO
     */
    @Transactional
    public AddressDTO addAddress(Long userId, AddressRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                    new EntityNotFoundException("User not found: " + userId));

        Address address = new Address();
        address.setUser(user);
        address.setLabel(request.label());
        address.setStreet(request.street());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPincode(request.pincode());
        address.setDefault(Boolean.TRUE.equals(request.isDefault()));

        Address saved = addressRepository.save(address);
        return toDTO(saved);
    }

    /**
     * Returns all saved delivery addresses for the user.
     * Ordered by creation time (oldest first) for a stable UI list.
     *
     * @param userId the authenticated user's ID
     * @return list of AddressDTO (empty list if user has no addresses)
     */
    public List<AddressDTO> getAddresses(Long userId) {
        return addressRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ------------------------------------------------------------------
    // Mapping helper
    // ------------------------------------------------------------------

    /**
     * Converts an Address entity to its DTO representation.
     * Keeps mapping logic in one place — avoids duplication.
     */
    private AddressDTO toDTO(Address address) {
        return new AddressDTO(
                address.getId(),
                address.getLabel(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.isDefault()
        );
    }
}
