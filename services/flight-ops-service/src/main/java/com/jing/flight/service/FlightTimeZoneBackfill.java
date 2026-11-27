package com.jing.flight.service;

import com.jing.common.exception.BaseException;
import com.jing.flight.client.AirportZones;
import com.jing.flight.model.FlightInstance;
import com.jing.flight.repository.FlightInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

/**
 * Flight instances created before time zones existed have no UTC moments. This job works them out
 * from their airports' zones: shortly after start-up, then every 10 minutes (location-service may not
 * be up yet). Instances whose airport is missing or has no zone are reported, and left as they are:
 * they can't be booked until the airport data is fixed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlightTimeZoneBackfill {

    private final FlightInstanceRepository repository;
    private final AirportZones airportZones;

    @Scheduled(initialDelayString = "${flight.time-zone-backfill.initial-delay-millis:30000}",
               fixedDelayString = "${flight.time-zone-backfill.every-millis:600000}")
    @Transactional
    public void backfill() {
        var missing = repository.findTop500ByDepartureAtIsNullOrderByIdAsc();
        if (missing.isEmpty()) return;

        int fixed = 0;
        Map<String, Integer> problems = new TreeMap<>();
        for (FlightInstance fi : missing) {
            try {
                fi.applyTimeZones(airportZones.zoneOf(fi.getDepartureAirportId()),
                        airportZones.zoneOf(fi.getArrivalAirportId()));
                fixed++;
            } catch (BaseException e) {                 // airport missing / no time zone
                problems.merge(e.getMessage(), 1, Integer::sum);
            } catch (RuntimeException e) {              // location-service down: try again next run
                log.info("Time-zone backfill postponed: {}", e.getMessage());
                return;
            }
        }
        if (fixed > 0) {
            log.info("Time-zone backfill: {} flight instance(s) now have UTC departure/arrival", fixed);
        }
        problems.forEach((reason, count) ->
                log.warn("Time-zone backfill: {} flight instance(s) can't be converted and can't be booked: {}",
                        count, reason));
    }
}
