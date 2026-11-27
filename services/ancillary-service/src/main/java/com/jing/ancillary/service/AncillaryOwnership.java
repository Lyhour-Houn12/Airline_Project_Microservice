package com.jing.ancillary.service;

import com.jing.ancillary.client.FlightInstanceCabinClient;
import com.jing.ancillary.client.FlightInstanceClient;
import com.jing.ancillary.model.Ancillary;
import com.jing.ancillary.model.Meal;
import com.jing.ancillary.repository.*;
import com.jing.common.airline.AirlineAccess;
import com.jing.common.dto.ancillary.FlightCabinAncillaryBulkRequest;
import com.jing.common.dto.ancillary.FlightCabinAncillaryItemRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealBulkRequest;
import com.jing.common.dto.ancillary.FlightInstanceMealItemRequest;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Who may change what in ancillary-service. Controllers call this before the service:
 * <ul>
 *   <li>meals, ancillaries and insurance belong to an airline: only its owner (or an admin) manages them;</li>
 *   <li>assigning them to a flight: the flight instance must exist, be the owner's, and every meal or
 *       ancillary must belong to that same airline.</li>
 * </ul>
 * A missing record is left to the service (which answers 404).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AncillaryOwnership {

    private final AirlineAccess airlineAccess;
    private final AncillaryRepository ancillaries;
    private final MealRepository meals;
    private final InsuranceCoverageRepository coverages;
    private final FlightCabinAncillaryRepository flightCabinAncillaries;
    private final FlightInstanceMealRepository flightInstanceMeals;
    private final FlightInstanceClient flightInstanceClient;
    private final FlightInstanceCabinClient flightInstanceCabinClient;

    /** A new meal / ancillary / insurance: an owner may only create them for their own airline. */
    public void newRecord(CurrentUser user, Long airlineId) {
        airlineAccess.airlineForNew(user, airlineId);
    }

    public void ancillary(CurrentUser user, Long id) {
        ancillaries.findById(id).ifPresent(a -> airlineAccess.requireCanManage(user, a.getAirlineId()));
    }

    public void meal(CurrentUser user, Long id) {
        meals.findById(id).ifPresent(m -> airlineAccess.requireCanManage(user, m.getAirlineId()));
    }

    public void coverage(CurrentUser user, Long id) {
        coverages.findById(id).ifPresent(c -> airlineAccess.requireCanManage(user, c.getAirlineId()));
    }

    public void flightCabinAncillary(CurrentUser user, Long id) {
        flightCabinAncillaries.findById(id)
                .ifPresent(f -> airlineAccess.requireCanManage(user, f.getAncillary().getAirlineId()));
    }

    public void flightInstanceMeal(CurrentUser user, Long id) {
        flightInstanceMeals.findById(id)
                .ifPresent(f -> airlineAccess.requireCanManage(user, f.getMeal().getAirlineId()));
    }

    /** Ancillaries for a flight-instance cabin: the cabin's flight decides the airline. */
    public void newFlightCabinAncillaries(CurrentUser user, FlightCabinAncillaryBulkRequest request) {
        Long flightInstanceId;
        try {
            flightInstanceId = flightInstanceCabinClient.flightInstanceCabin(request.flightInstanceCabinId()).flightInstanceId();
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Flight instance cabin " + request.flightInstanceCabinId() + " does not exist");
        }
        Long airlineId = airlineOfFlightInstance(flightInstanceId);
        airlineAccess.requireCanManage(user, airlineId);
        for (FlightCabinAncillaryItemRequest item : request.ancillaries()) {
            ancillaries.findById(item.ancillaryId()).map(Ancillary::getAirlineId)
                    .filter(owner -> !Objects.equals(owner, airlineId))
                    .ifPresent(other -> {
                        throw new BadRequestException("Ancillary " + item.ancillaryId() + " belongs to another airline");
                    });
        }
    }

    /** Meals for a flight instance: they must be the flight's airline's meals. */
    public void newFlightInstanceMeals(CurrentUser user, FlightInstanceMealBulkRequest request) {
        Long airlineId = airlineOfFlightInstance(request.flightInstanceId());
        airlineAccess.requireCanManage(user, airlineId);
        for (FlightInstanceMealItemRequest item : request.meals()) {
            meals.findById(item.mealId()).map(Meal::getAirlineId)
                    .filter(owner -> !Objects.equals(owner, airlineId))
                    .ifPresent(other -> {
                        throw new BadRequestException("Meal " + item.mealId() + " belongs to another airline");
                    });
        }
    }

    private Long airlineOfFlightInstance(Long flightInstanceId) {
        try {
            return flightInstanceClient.flightInstance(flightInstanceId).getAirlineId();
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Flight instance " + flightInstanceId + " does not exist");
        }
    }
}
