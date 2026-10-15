import java.util.List;
import java.util.Optional;

// A: reserves goods on the shelf for an order and gives them back
public class OrderBatcher {
    private final BatchRepository repo;
    private final ShelfRules rules = new ShelfRules();

    public OrderBatcher(BatchRepository repo) { this.repo = repo; }

    public Batch reserve(String ref, String sku, int quantity) {
        List<Batch> all = repo.loadAll();
        rules.check(sku, quantity, reserved(all, sku));
        Batch batch = new Batch("BATCH-" + (all.size() + 1), ref, sku, quantity);
        batch.reserve();
        all.add(batch);
        repo.saveAll(all);
        return batch;
    }

    private int reserved(List<Batch> all, String sku) {
        int sum = 0;
        for (Batch b : all)
            if (b.getSku().equals(sku) && b.getStatus() == Batch.Status.RESERVED) {
                sum += b.getQuantity();
            }
        return sum;
    }

    public void release(String batchId) {
        List<Batch> all = repo.loadAll();
        for (Batch b : all) {
            if (b.getId().equals(batchId)) {
                b.release();
                repo.saveAll(all);
                return;
            }
        }
        throw new WarehouseException("Unknown batch " + batchId);
    }

    public Optional<Batch> find(String batchId) {
        return repo.loadAll().stream().filter(b -> b.getId().equals(batchId)).findFirst();
    }

    public int available(String sku) {
        return ShelfRules.STOCK_PER_SKU - reserved(repo.loadAll(), sku);
    }
}
