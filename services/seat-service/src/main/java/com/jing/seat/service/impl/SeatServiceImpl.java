package com.jing.seat.service.impl;

import com.jing.common.enumeration.SeatType;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.DuplicateResourceException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.dto.seat.*;
import com.jing.seat.filter.*;
import com.jing.common.pagination.PageResponse;
import com.jing.common.pagination.PageableFactory;
import com.jing.common.dto.seat.SeatResponse;
import com.jing.seat.mapper.SeatMapper;
import com.jing.seat.model.Seat;
import com.jing.seat.model.SeatMap;
import com.jing.seat.repository.SeatMapRepository;
import com.jing.seat.repository.SeatRepository;
import com.jing.seat.service.SeatService;
import com.jing.seat.specification.SeatSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SeatServiceImpl implements SeatService {
    private static final int MAX_CLASH_PREVIEW = 10;
    private static final String COLUMN_LETTERS = "ABCDEFGHJK";
    private static final int MAX_ROW = 100;
    private static final int MAX_BULK = 300;
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "seatRow", "columnLetter", "seatNumber", "seatType", "createdAt");


    private final SeatRepository seatRepository;
    private final SeatMapRepository seatMapRepository;


    @Override
    public SeatResponse createSeat(SeatRequest request) {
        SeatMap seatMap = getSeatMap(request.seatMapId());
        char letter = resolveLetter(request.columnLetter(), seatMap);
        validateAttributes(request.isEmergencyExit(), request.hasBassinet(), request.isWheelchairAccessible());

        if (seatRepository.countBySeatMapId(seatMap.getId()) >= capacity(seatMap)){
            throw new BadRequestException("Seat map " + seatMap.getId() + " is already full ("
                    + capacity(seatMap) + " seats)");
        }
        String seatNumber = request.seatRow() + String.valueOf(letter);
        Long aircraftId = seatMap.getCabinClass().getAircraftId();

        if (seatRepository.existsByAircraftIdAndSeatNumber(aircraftId, seatNumber)){
            throw new DuplicateResourceException("Seat", "Seat Number", seatNumber + " already exists on aircraft " + aircraftId);
        }

        Seat saved = save(SeatMapper.toEntity(request, seatMap, letter, seatNumber));

        return SeatMapper.toResponse(saved);
    }

    @Override
    public List<SeatResponse> createSeats(SeatBulkRequest request) {
        List<SeatRequest> items = request.seats();
        if(items.size() > MAX_BULK){
            throw new BadRequestException("At most " + MAX_BULK + " seats per request");
        }
        Long mapId = items.get(0).seatMapId();
        if(items.stream().anyMatch(i -> !mapId.equals(i.seatMapId()))) {
            throw new BadRequestException("All seats in one request must belong to the same seat map");
        }

        SeatMap seatMap = getSeatMap(mapId);
        Long aircraftId = seatMap.getCabinClass().getAircraftId();

        Set<String> numbers = new LinkedHashSet<>();
        List<Seat> seats = new ArrayList<>();

        for(SeatRequest item : items){
            char letter = resolveLetter(item.columnLetter(), seatMap);
            validateAttributes(item.isEmergencyExit(), item.hasBassinet(), item.isWheelchairAccessible());
            String number = item.seatRow() + String.valueOf(letter);
            if (!numbers.add(number)) {
                throw new BadRequestException("Duplicate seat in request: " + number);
            }

            seats.add(SeatMapper.toEntity(item, seatMap, letter, number));
        }
        if (seatRepository.countBySeatMapId(mapId) + seats.size() > capacity(seatMap)) {
            throw new BadRequestException("Request exceeds the capacity of seat map " + mapId
                    + " (" + capacity(seatMap) + " seats)");
        }

        assertNoneTaken(aircraftId, numbers);
        return saveAll(seats);
    }

    @Override
    public List<SeatResponse> generateSeats(SeatGenerateRequest request) {
        SeatMap seatMap = getSeatMap(request.seatMapId());
        int left = seatMap.getLeftSeatsPerRow();
        int right = seatMap.getRightSeatsPerRow();
        int perRow = left + right;
        if (perRow > COLUMN_LETTERS.length()){
            throw new BadRequestException("Seat map has more than " + COLUMN_LETTERS.length() + " seats per row");
        }

        int startRow = request.startRow();
        int endRow = startRow + seatMap.getTotalRows() - 1;
        if (endRow > MAX_ROW) {
            throw new BadRequestException("Rows " + startRow + "-" + endRow + " exceed the maximum row " + MAX_ROW);
        }

        // Seats this map already has
        List<Seat> existing = seatRepository.findBySeatMapId(seatMap.getId());
        if(!existing.isEmpty()) {
            int firstRow = existing.stream()
                    .mapToInt(Seat::getSeatRow).min().getAsInt();
            if(firstRow != startRow){
                throw new BadRequestException(
                        "Seat map already starts at row " + firstRow + "; startRow must stay the same");
            }

        }
        Set<String> existingNumbers = existing.stream()
                .map(Seat::getSeatNumber)
                .collect(Collectors.toSet());
        Long aircraftId = seatMap.getCabinClass().getAircraftId();

        Set<String> newNumbers = new LinkedHashSet<>();
        List<Seat> seats = new ArrayList<>();

        for (int row = startRow; row <= endRow; row++) {
            for (int i = 0; i < perRow; i++) {
                char letter = COLUMN_LETTERS.charAt(i);
                String number = row + String.valueOf(letter);
                if (existingNumbers.contains(number)) {
                    continue; // already generated
                }
                newNumbers.add(number);
                seats.add(SeatMapper.generated(seatMap, row, letter, number, typeFor(i, left, right)));
            }
        }

        if (seats.isEmpty()) {
            throw new DuplicateResourceException("Seats", "seatMapId", seatMap.getId());
        }

        // Clash with seats of OTHER seat maps on the same aircraft
        List<String> taken = seatRepository.findByAircraftIdAndSeatNumberIn(aircraftId, newNumbers).stream()
                .map(Seat::getSeatNumber)
                .sorted()
                .toList();
        if(!taken.isEmpty()) {
            throw new BadRequestException("Rows " + startRow + "-" + endRow + " overlap seats already on aircraft " + aircraftId
            + ": " + taken.stream().limit(MAX_CLASH_PREVIEW).toList()
                    + (taken.size() > MAX_CLASH_PREVIEW ? " and " + (taken.size() - MAX_CLASH_PREVIEW) + " more" : ""));
        }


        return  saveAll(seats);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getById(Long id) {
        return SeatMapper.toResponse(getSeat(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SeatResponse> search(SeatFilter filter, int page, int size, String sortBy, String sortDirection) {
        return PageResponse.from(
                seatRepository.findAll(SeatSpecification.toSpec(filter), buildPageable(page, size, sortBy, sortDirection))
                        .map(SeatMapper::toResponse)
        );
    }

    @Override
    public SeatResponse updateSeat(Long id, SeatUpdateRequest request) {
        Seat seat = getSeat(id);
        SeatMapper.updateEntity(seat, request);
        validateAttributes(seat.getIsEmergencyExit(), seat.getHasBassinet(), seat.getIsWheelchairAccessible());
        return SeatMapper.toResponse(seatRepository.save(seat));
    }

    @Override
    public SeatResponse blockSeat(Long id) {
        Seat seat = getSeat(id);
        seat.setIsBlocked(true);
        seat.setIsAvailable(false);
        return SeatMapper.toResponse(seatRepository.save(seat));
    }

    @Override
    public SeatResponse unblockSeat(Long id) {
        Seat seat = getSeat(id);
        seat.setIsBlocked(false);
        seat.setIsAvailable(Boolean.TRUE.equals(seat.getIsActive()));
        return SeatMapper.toResponse(seatRepository.save(seat));
    }

    @Override
    public SeatResponse setActive(Long id, boolean active) {
        Seat seat = getSeat(id);
        seat.setIsActive(active);
        seat.setIsAvailable(active && !Boolean.TRUE.equals(seat.getIsBlocked()));
        return SeatMapper.toResponse(seatRepository.save(seat));
    }

    @Override
    public void deleteSeat(Long id) {
        seatRepository.delete(getSeat(id));
    }

    public static SeatType typeFor(int index, int left, int right){
        if (index < left){
            if (index == 0) return SeatType.WINDOW;
            if (index == left - 1) return SeatType.AISLE;   // last seat of the left block
            return SeatType.MIDDLE;
        }

        int positionRightBack = index - left;
        if (positionRightBack == right - 1) return SeatType.WINDOW;
        if (positionRightBack == 0) return SeatType.AISLE;

        return SeatType.MIDDLE;
    }

    private static char resolveLetter(Character raw, SeatMap map){
        char letter = Character.toUpperCase(raw);
        int perRow = map.getLeftSeatsPerRow() + map.getRightSeatsPerRow();
        int index = COLUMN_LETTERS.indexOf(letter);
        if (index < 0  || index >= perRow){
            throw new BadRequestException("Column '" + letter + "' is not valid for this seat map; allowed: "
                    + COLUMN_LETTERS.substring(0, Math.min(perRow, COLUMN_LETTERS.length())));
        }
        return letter;
    }


    private void validateAttributes(Boolean emergencyExit, Boolean bassinet, Boolean wheelchair){
        if (Boolean.TRUE.equals(emergencyExit)
                && (Boolean.TRUE.equals(bassinet) || Boolean.TRUE.equals(wheelchair))) {
            throw new BadRequestException(
                    "An emergency exit seat cannot be a bassinet or wheelchair-accessible seat");
        }
    }

    private void assertNoneTaken(Long aircraftId, Collection<String> numbers){
        List<String> taken = seatRepository.findByAircraftIdAndSeatNumberIn(aircraftId, numbers)
                .stream()
                .map(Seat::getSeatNumber)
                .sorted()
                .toList();
        if(!taken.isEmpty()){
            throw new BadRequestException("Seats already exist on aircraft " + aircraftId + ": " + taken);
        }
    }

    private Seat getSeat(Long id){
        return seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seat", "id", id));
    }

    private SeatMap getSeatMap(Long id){
        return seatMapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SeatMap", "id", id));
    }

    private static long capacity(SeatMap seatMap){
        return (long) seatMap.getTotalRows() * (seatMap.getLeftSeatsPerRow() + seatMap.getRightSeatsPerRow());
    }

    private Seat save(Seat seat){
        try{
            return seatRepository.saveAndFlush(seat);
        }catch (DataIntegrityViolationException e){
            throw translate(e, "Seat " + seat.getSeatNumber() + " already exists on this aircraft");
        }
    }

    private List<SeatResponse> saveAll(List<Seat> seats){
        try{
            // flush now so a duplicate is caught here, not later at commit time
            return seatRepository.saveAllAndFlush(seats).stream().map(SeatMapper::toResponse).toList();
        }catch (DataIntegrityViolationException e){
            throw translate(e, "One or more seats already exist on this aircraft");
        }
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDirection) {
        return PageableFactory.of(page, size, sortBy, sortDirection,
                SORTABLE_FIELDS, DEFAULT_SORT_FIELD, Sort.Direction.ASC);
    }

    private RuntimeException translate(DataIntegrityViolationException e, String duplicateMessage) {
        String cause = String.valueOf(e.getMostSpecificCause().getMessage());
        log.error("Seat insert failed: {}", cause);
        if (cause.contains("uk_seat_aircraft_seat_number")) {
            return new BadRequestException(duplicateMessage);
        }
        return e;   // anything else is a real bug, don't hide it
    }




}
