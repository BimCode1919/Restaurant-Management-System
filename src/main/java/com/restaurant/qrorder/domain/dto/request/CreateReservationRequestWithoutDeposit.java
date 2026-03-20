package com.restaurant.qrorder.domain.dto.request;

/**
 * @deprecated Merged into {@link CreateReservationRequest}.
 * Deposit is now auto-determined by the service based on partySize and preOrderItems.
 * This class is kept only to avoid breaking any external references during migration.
 * Remove after confirming no other consumers exist.
 */
@Deprecated(since = "refactor", forRemoval = true)
public class CreateReservationRequestWithoutDeposit {
}
