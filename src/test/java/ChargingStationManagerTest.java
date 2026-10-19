import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import charging.ChargingStationManager;
import charging.Dispatch;
import charging.DispatchRepository;
import exception.WarehouseException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

// Subsystem C on its own
class ChargingStationManagerTest {
    @TempDir Path dir;
    ChargingStationManager charging;

    @BeforeEach void setUp() {
        charging = new ChargingStationManager(new DispatchRepository(dir));
    }

    @Test void confirmDocksTheRobotAndLogsIt() {
        Dispatch d = charging.confirm("X-1", "R1");
        assertEquals(Dispatch.Status.CONFIRMED, d.getStatus());
        assertEquals("S1", d.getStation());
        assertEquals("CONFIRMED DSP-1 robot=R1 station=S1", charging.log().get(0));
    }

    @Test void failsWhenAllStationsAreTakenAndWorksAgainAfterCancel() {
        Dispatch first = charging.confirm("X-1", "R1");
        charging.confirm("X-2", "R2");
        assertThrows(WarehouseException.class, () -> charging.confirm("X-3", "R3"));
        charging.cancel(first.getId());
        assertEquals("S1", charging.confirm("X-3", "R3").getStation());
        assertTrue(charging.log().contains("CANCELLED DSP-1"));
    }

    @Test void illegalStatusChangeIsRejected() {
        Dispatch d = charging.confirm("X-1", "R1");
        charging.cancel(d.getId());
        assertThrows(IllegalStateException.class, () -> charging.cancel(d.getId()));
        assertThrows(WarehouseException.class, () -> charging.cancel("DSP-99"));
    }

    @Test void stateSurvivesRestart() {
        charging.confirm("X-1", "R1");
        ChargingStationManager afterRestart = new ChargingStationManager(new DispatchRepository(dir));
        assertTrue(afterRestart.busyStations().contains("S1"));
        assertEquals("DSP-1", afterRestart.findByRef("X-1").orElseThrow().getId());
    }
}
