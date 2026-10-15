import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** Subsystem A on its own. */
class OrderBatcherTest {
    @TempDir Path dir;
    OrderBatcher batcher;

    @BeforeEach void setUp() { batcher = new OrderBatcher(new BatchRepository(dir)); }

    @Test void reserveTakesGoodsFromTheShelf() {
        Batch b = batcher.reserve("X-1", "BOX", 30);
        assertEquals(Batch.Status.RESERVED, b.getStatus());
        assertEquals(70, batcher.available("BOX"));
    }

    @Test void releaseGivesGoodsBack() {
        Batch b = batcher.reserve("X-1", "BOX", 30);
        batcher.release(b.getId());
        assertEquals(100, batcher.available("BOX"));
        assertEquals(Batch.Status.RELEASED, batcher.find(b.getId()).orElseThrow().getStatus());
    }

    @Test void illegalStatusChangeIsRejected() {
        Batch b = batcher.reserve("X-1", "BOX", 10);
        batcher.release(b.getId());
        assertThrows(IllegalStateException.class, () -> batcher.release(b.getId()));   // RELEASED -> RELEASED
        assertThrows(IllegalStateException.class, () -> new Batch("B", "R", "BOX", 1).release());   // NEW -> RELEASED
    }

    @Test void badInputAndNotEnoughStockAreRejected() {
        assertThrows(WarehouseException.class, () -> batcher.reserve("X", "BOX", 0));
        assertThrows(WarehouseException.class, () -> batcher.reserve("X", "bad sku!", 5));
        assertThrows(WarehouseException.class, () -> batcher.reserve("X", null, 5));
        batcher.reserve("X-1", "BOX", 80);
        assertThrows(WarehouseException.class, () -> batcher.reserve("X-2", "BOX", 30));
        assertThrows(WarehouseException.class, () -> batcher.release("BATCH-99"));
    }

    @Test void stateSurvivesRestart() {
        batcher.reserve("X-1", "BOX", 25);
        OrderBatcher afterRestart = new OrderBatcher(new BatchRepository(dir));
        assertEquals(75, afterRestart.available("BOX"));
        assertEquals("X-1", afterRestart.find("BATCH-1").orElseThrow().getRef());
    }
}
