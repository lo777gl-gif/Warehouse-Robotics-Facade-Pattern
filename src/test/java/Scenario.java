import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;


// Mini test language, so a test reads like a sentence:
// warehouse.order("BOX", 10).orderFails("BOX", 60).thenChain("CH-2", COMPENSATED).thenBatch("CH-2", RELEASED);
// restart() builds all objects again from the files on disk, like restarting the program.
public class Scenario {
    private final Path dir;
    OrderBatcher batcher;
    RobotNavigator navigator;
    ChargingStationManager charging;
    ChainRepository chains;
    WarehouseFacade facade;
    AdminFacade admin;

    public Scenario(Path dir) {
        this.dir = dir; restart();
    }

    public Scenario restart() {
        batcher = new OrderBatcher(new BatchRepository(dir));
        navigator = new RobotNavigator(new TaskRepository(dir));
        charging = new ChargingStationManager(new DispatchRepository(dir));
        chains = new ChainRepository(dir);
        facade = new WarehouseFacade(batcher, navigator, charging, chains);
        admin = new AdminFacade(facade, batcher, navigator, charging, chains);
        return this;
    }

    // ----- actions -----
    public Scenario order(String sku, int quantity) {
        assertEquals(Chain.Status.COMPLETED, facade.fulfil(sku, quantity).getStatus());
        return this;
    }

    public Scenario orderFails(String sku, int quantity) {
        assertThrows(WarehouseException.class, () -> facade.fulfil(sku, quantity));
        return this;
    }

    // ----- checks -----
    public Scenario thenChain(String chainId, Chain.Status expected) {
        assertEquals(expected, chains.find(chainId).orElseThrow().getStatus());
        return this;
    }

    public Scenario thenBatch(String chainId, Batch.Status expected) {
        String batchId = chains.find(chainId).orElseThrow().getBatchId();
        assertEquals(expected, batcher.find(batchId).orElseThrow().getStatus());
        return this;
    }

    public Scenario thenTask(String chainId, RobotTask.Status expected) {
        assertEquals(expected, navigator.findByRef(chainId).orElseThrow().getStatus());
        return this;
    }

    public Scenario thenNoTask(String chainId) { assertTrue(navigator.findByRef(chainId).isEmpty()); return this; }

    public Scenario thenDispatch(String chainId, Dispatch.Status expected) {
        assertEquals(expected, charging.findByRef(chainId).orElseThrow().getStatus());
        return this;
    }

    public Scenario thenNoDispatch(String chainId) { assertTrue(charging.findByRef(chainId).isEmpty()); return this; }

    public Scenario thenAvailable(String sku, int expected) { assertEquals(expected, batcher.available(sku)); return this; }
}
