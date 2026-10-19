import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import charging.ChargingStationManager;
import charging.Dispatch;
import charging.DispatchRepository;
import exception.WarehouseException;
import facade.Chain;
import facade.ChainRepository;
import facade.WarehouseFacade;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import reserve.Batch;
import reserve.BatchRepository;
import reserve.OrderBatcher;
import robot.RobotNavigator;
import robot.RobotTask;
import robot.TaskRepository;

class WarehouseFacadeTest {
    @TempDir Path dir;
    Scenario warehouse;

    @BeforeEach void setUp() {
        warehouse = new Scenario(dir);
    }

    @Test void happyPath_allThreeStepsDone() {
        warehouse.order("BOX", 10)
                .thenChain("CH-1", Chain.Status.COMPLETED)
                .thenBatch("CH-1", Batch.Status.RESERVED)
                .thenTask("CH-1", RobotTask.Status.ASSIGNED)
                .thenDispatch("CH-1", Dispatch.Status.CONFIRMED)
                .thenAvailable("BOX", 90);
    }

    @Test void stepAFails_nothingIsSaved() {
        warehouse.orderFails("BOX", 500);
        assertTrue(warehouse.chains.all().isEmpty());
        warehouse.thenAvailable("BOX", 100);
    }

    @Test void stepBFails_onlyAIsUndone() {
        warehouse.orderFails("BOX", 60)       // 60 is more than one robot can carry
                .thenChain("CH-1", Chain.Status.COMPENSATED)
                .thenBatch("CH-1", Batch.Status.RELEASED)
                .thenNoTask("CH-1")
                .thenNoDispatch("CH-1")
                .thenAvailable("BOX", 100);
    }

    @Test void stepCFails_BAndAAreUndone() {
        warehouse.order("BOX", 10).order("BOX", 10)    // both charging stations are now taken
                .orderFails("BOX", 10)
                .thenChain("CH-3", Chain.Status.COMPENSATED)
                .thenTask("CH-3", RobotTask.Status.RELEASED)
                .thenBatch("CH-3", Batch.Status.RELEASED)
                .thenNoDispatch("CH-3")
                .thenChain("CH-1", Chain.Status.COMPLETED)       // earlier orders are untouched
                .thenAvailable("BOX", 80);
    }

    @Test void compensationGoesInReverseOrder() {
        List<String> calls = new ArrayList<>();
        OrderBatcher batcher = new OrderBatcher(new BatchRepository(dir)) {
            @Override public void release(String id) {
                calls.add("A");
                super.release(id);
            }
        };
        RobotNavigator navigator = new RobotNavigator(new TaskRepository(dir)) {
            @Override public void release(String id) {
                calls.add("B");
                super.release(id);
            }
        };
        ChargingStationManager charging = new ChargingStationManager(new DispatchRepository(dir)) {
            @Override public void cancel(String id) {
                calls.add("C");
                super.cancel(id);
            }
        };
        WarehouseFacade facade = new WarehouseFacade(batcher, navigator, charging, new ChainRepository(dir));

        facade.fulfil("BOX", 10);
        facade.undo(new ChainRepository(dir).find("CH-1").orElseThrow());
        assertEquals(List.of("C", "B", "A"), calls);
    }

    // restart in the middle of a chain: only the files on disk remain

    @Test void restartAfterAAndB_recoverFinishesStepC() {
        Batch batch = warehouse.batcher.reserve("CH-1", "BOX", 10);
        warehouse.navigator.assign("CH-1", 10);
        warehouse.chains.save(new Chain("CH-1", "BOX", 10, batch.getId(), Chain.Status.PENDING_C));

        assertEquals(1, warehouse.restart().facade.recover());

        warehouse.thenChain("CH-1", Chain.Status.COMPLETED).thenDispatch("CH-1", Dispatch.Status.CONFIRMED);
    }

    @Test void restartAfterA_recoverRunsBAndC() {
        Batch batch = warehouse.batcher.reserve("CH-1", "BOX", 10);
        warehouse.chains.save(new Chain("CH-1", "BOX", 10, batch.getId(), Chain.Status.PENDING_B));

        warehouse.restart().facade.recover();

        warehouse.thenChain("CH-1", Chain.Status.COMPLETED)
                .thenTask("CH-1", RobotTask.Status.ASSIGNED)
                .thenDispatch("CH-1", Dispatch.Status.CONFIRMED);
    }

    @Test void restartDuringCompensation_recoverFinishesTheUndo() {
        Batch batch = warehouse.batcher.reserve("CH-1", "BOX", 10);
        RobotTask task = warehouse.navigator.assign("CH-1", 10);
        warehouse.navigator.release(task.getId());          // B is already undone, A is not yet
        warehouse.chains.save(new Chain("CH-1", "BOX", 10, batch.getId(), Chain.Status.COMPENSATING));

        warehouse.restart().facade.recover();

        warehouse.thenChain("CH-1", Chain.Status.COMPENSATED)
                .thenBatch("CH-1", Batch.Status.RELEASED)
                .thenAvailable("BOX", 100);
    }

    @Test void recoverRollsBackWhenTheResumedStepFailsAgain() {
        // chains CH-1 and CH-2 take both stations; CH-3 was interrupted before step C
        warehouse.order("BOX", 10).order("BOX", 10);
        Batch batch = warehouse.batcher.reserve("CH-3", "BOX", 10);
        warehouse.navigator.assign("CH-3", 10);
        warehouse.chains.save(new Chain("CH-3", "BOX", 10, batch.getId(), Chain.Status.PENDING_C));

        warehouse.restart().facade.recover();

        warehouse.thenChain("CH-3", Chain.Status.COMPENSATED)
                .thenTask("CH-3", RobotTask.Status.RELEASED)
                .thenBatch("CH-3", Batch.Status.RELEASED);
    }

    @Test void finishedWorkSurvivesRestartAndRecoverLeavesItAlone() {
        warehouse.order("BOX", 10).orderFails("BOX", 60);
        assertEquals(0, warehouse.restart().facade.recover());
        warehouse.thenChain("CH-1", Chain.Status.COMPLETED).thenChain("CH-2", Chain.Status.COMPENSATED);
    }

    // Admin facade

    @Test void adminCompensatesACompletedOrderAndFreesEverything() {
        warehouse.order("BOX", 10);
        warehouse.admin.compensate("CH-1");
        warehouse.thenChain("CH-1", Chain.Status.COMPENSATED)
                .thenDispatch("CH-1", Dispatch.Status.CANCELLED)
                .thenTask("CH-1", RobotTask.Status.RELEASED)
                .thenBatch("CH-1", Batch.Status.RELEASED)
                .thenAvailable("BOX", 100);
        assertTrue(warehouse.navigator.busyRobots().isEmpty());
        assertTrue(warehouse.charging.busyStations().isEmpty());
    }

    @Test void adminRejectsUnknownOrAlreadyCompensatedOrders() {
        warehouse.order("BOX", 10);
        warehouse.admin.compensate("CH-1");
        assertThrows(WarehouseException.class, () -> warehouse.admin.compensate("CH-1"));
        assertThrows(WarehouseException.class, () -> warehouse.admin.compensate("CH-42"));
    }

    @Test void reportShowsAllThreeSubsystems() {
        warehouse.order("BOX", 10).orderFails("BOX", 60);
        String report = warehouse.admin.report();
        assertTrue(report.contains("CH-1 BOX x10  COMPLETED"));
        assertTrue(report.contains("C: CONFIRMED S1"));
        assertTrue(report.contains("CH-2 BOX x60  COMPENSATED"));
    }

    @Test void reportOnEmptyWarehouse() {
        assertTrue(warehouse.admin.report().contains("no orders"));
    }
}
