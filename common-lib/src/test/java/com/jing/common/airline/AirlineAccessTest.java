package com.jing.common.airline;

import com.jing.common.dto.airline.AircraftResponse;
import com.jing.common.dto.airline.AirlineResponse;
import com.jing.common.enumeration.AircraftStatus;
import com.jing.common.enumeration.AirlineStatus;
import com.jing.common.enumeration.UserRole;
import com.jing.common.exception.BadRequestException;
import com.jing.common.exception.ForbiddenException;
import com.jing.common.exception.ResourceNotFoundException;
import com.jing.common.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AirlineAccessTest {

    private static final CurrentUser ADMIN = new CurrentUser(14L, UserRole.ROLE_SYSTEM_ADMIN);
    private static final CurrentUser OWNER = new CurrentUser(16L, UserRole.ROLE_AIRLINE_OWNER);   // owns airline 3
    private static final CurrentUser NEW_OWNER = new CurrentUser(20L, UserRole.ROLE_AIRLINE_OWNER); // owns none yet
    private static final CurrentUser CUSTOMER = new CurrentUser(15L, UserRole.ROLE_CUSTOMER);

    private final AirlineCoreClient client = mock(AirlineCoreClient.class);
    private final AirlineAccess access = new AirlineAccess(client);

    @BeforeEach
    void setUp() {
        when(client.airlineOfOwner(16L)).thenReturn(AirlineResponse.builder().id(3L).build());
        when(client.airlineOfOwner(20L)).thenThrow(new ResourceNotFoundException("Airline not found"));
        when(client.airline(3L)).thenReturn(AirlineResponse.builder().id(3L).airlineName("Own Air").status(AirlineStatus.ACTIVE).build());
        when(client.airline(7L)).thenReturn(AirlineResponse.builder().id(7L).airlineName("Banned Air").status(AirlineStatus.BANNED).build());
        when(client.aircraft(1L)).thenReturn(aircraft(1L, 3L, AircraftStatus.ACTIVE));
        when(client.aircraft(2L)).thenReturn(aircraft(2L, 9L, AircraftStatus.ACTIVE));   // another airline's
        when(client.aircraft(5L)).thenReturn(aircraft(5L, 3L, AircraftStatus.INACTIVE));
        when(client.aircraft(404L)).thenThrow(new ResourceNotFoundException("Aircraft not found"));
    }

    private static AircraftResponse aircraft(Long id, Long airlineId, AircraftStatus status) {
        AircraftResponse a = new AircraftResponse();
        a.setId(id);
        a.setCode("AC-" + id);
        a.setAirlineId(airlineId);
        a.setStatus(status);
        return a;
    }

    @Test
    void ownersManageOnlyTheirOwnAirline() {
        assertThatCode(() -> access.requireCanManage(OWNER, 3L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireCanManage(OWNER, 9L)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> access.requireCanManage(CUSTOMER, 3L)).isInstanceOf(ForbiddenException.class);
        assertThatCode(() -> access.requireCanManage(ADMIN, 9L)).doesNotThrowAnyException();
    }

    @Test
    void newRecordsOfAnOwnerAlwaysGoToTheirAirline() {
        assertThat(access.airlineForNew(OWNER, null)).isEqualTo(3L);
        assertThat(access.airlineForNew(OWNER, 3L)).isEqualTo(3L);
        assertThatThrownBy(() -> access.airlineForNew(OWNER, 9L)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> access.airlineForNew(NEW_OWNER, null))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("Create your airline first");
    }

    @Test
    void adminsMustNameAnActiveAirline() {
        assertThat(access.airlineForNew(ADMIN, 3L)).isEqualTo(3L);
        assertThatThrownBy(() -> access.airlineForNew(ADMIN, null)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> access.airlineForNew(ADMIN, 7L)).hasMessageContaining("BANNED");
    }

    @Test
    void anAircraftCanOnlyBeUsedByItsOwnAirlineWhenActive() {
        assertThatCode(() -> access.requireAircraftOf(1L, 3L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> access.requireAircraftOf(2L, 3L)).hasMessageContaining("belongs to another airline");
        assertThatThrownBy(() -> access.requireAircraftOf(5L, 3L)).hasMessageContaining("INACTIVE");
        assertThatThrownBy(() -> access.requireAircraftOf(404L, 3L)).hasMessageContaining("does not exist");
    }

    @Test
    void theOwnersAirlineIsLookedUpOnceAndCached() {
        access.requireCanManage(OWNER, 3L);
        access.requireCanManage(OWNER, 3L);
        access.ownedAirlineId(OWNER);

        verify(client, times(1)).airlineOfOwner(16L);
    }
}
