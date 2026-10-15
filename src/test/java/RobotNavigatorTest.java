import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** Subsystem B on its own. */
class RobotNavigatorTest {
    @TempDir Path dir;
    RobotNavigator navigator;

    @BeforeEach void setUp() { navigator = new RobotNavigator(new TaskRepository(dir)); }

    @Test void assignsTheFirstFreeRobot() {
        assertEquals("R1", navigator.assign("X-1", 10).getRobotId());
        assertEquals("R2", navigator.assign("X-2", 10).getRobotId());
    }

    @Test void tooBigLoadIsRejected() {
        assertThrows(WarehouseException.class, () -> navigator.assign("X-1", RoutePlanner.ROBOT_CAPACITY + 1));
    }

    @Test void failsWhenAllRobotsAreBusyAndWorksAgainAfterRelease() {
        RobotTask first = navigator.assign("X-1", 5);
        navigator.assign("X-2", 5);
        navigator.assign("X-3", 5);
        assertThrows(WarehouseException.class, () -> navigator.assign("X-4", 5));
        navigator.release(first.getId());
        assertEquals("R1", navigator.assign("X-4", 5).getRobotId());
    }

    @Test void illegalStatusChangeIsRejected() {
        RobotTask t = navigator.assign("X-1", 10);
        navigator.release(t.getId());
        assertThrows(IllegalStateException.class, () -> navigator.release(t.getId()));
        assertThrows(WarehouseException.class, () -> navigator.release("TASK-99"));
    }

    @Test void stateSurvivesRestart() {
        navigator.assign("X-1", 10);
        RobotNavigator afterRestart = new RobotNavigator(new TaskRepository(dir));
        assertTrue(afterRestart.busyRobots().contains("R1"));
        assertEquals("TASK-1", afterRestart.findByRef("X-1").orElseThrow().getId());
    }
}
