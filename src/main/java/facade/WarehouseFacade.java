package facade;
// Facade 1: the client's one call. fulfil() runs A (reserve) -> B (robot) -> C (charging + dispatch)
// Saves progress after each step, and rolls back in reverse order if a step fails.

import charging.ChargingStationManager;
import charging.Dispatch;
import reserve.Batch;
import reserve.OrderBatcher;
import robot.RobotNavigator;
import robot.RobotTask;
import exception.WarehouseException;

public class WarehouseFacade {
    private final OrderBatcher batcher;
    private final RobotNavigator navigator;
    private final ChargingStationManager charging;
    private final ChainRepository chains;

    public WarehouseFacade(OrderBatcher batcher, RobotNavigator navigator, ChargingStationManager charging, ChainRepository chains) {
        this.batcher = batcher;
        this.navigator = navigator;
        this.charging = charging;
        this.chains = chains;
    }

    public Chain fulfil(String sku, int quantity) {
        String id = chains.nextId();
        Batch batch = batcher.reserve(id, sku, quantity);   // step A: if it fails, nothing is saved yet
        Chain chain = new Chain(id, sku, quantity, batch.getId(), Chain.Status.PENDING_B);
        chains.save(chain);
        return advance(chain);
    }

    // After a restart: finish (or roll back) every chain that was interrupted. Returns how many were handled
    public int recover() {
        int handled = 0;
        for (Chain chain : chains.all()) {
            Chain.Status s = chain.getStatus();
            if (s == Chain.Status.COMPENSATING) {
                undo(chain);
                handled++;
            } else if (s == Chain.Status.PENDING_B || s == Chain.Status.PENDING_C) {
                try {
                    advance(chain);
                } catch (WarehouseException e) {
                    // already by advance()
                }
                handled++;
            }
        }
        return handled;
    }

    // Continues a chain from the step saved on disk.
    private Chain advance(Chain chain) {
        try {
            if (chain.getStatus() == Chain.Status.PENDING_B) {
                navigator.assign(chain.getId(), chain.getQuantity());           // step B
                chain.setStatus(Chain.Status.PENDING_C);
                chains.save(chain);
            }
            if (chain.getStatus() == Chain.Status.PENDING_C) {
                RobotTask task = navigator.findByRef(chain.getId()).orElseThrow();
                charging.confirm(chain.getId(), task.getRobotId());             // step C
                chain.setStatus(Chain.Status.COMPLETED);
                chains.save(chain);
            }
            return chain;
        } catch (WarehouseException e) {
            undo(chain);
            throw new WarehouseException("facade.Chain " + chain.getId() + " rolled back: " + e.getMessage());
        }
    }

    // Compensation: C, then B, then A. Each step is undone only if it is really committed on disk
    public void undo(Chain chain) {
        chain.setStatus(Chain.Status.COMPENSATING);
        chains.save(chain);

        charging.findByRef(chain.getId()).ifPresent(d -> {
            if (d.getStatus() == Dispatch.Status.CONFIRMED) {
                charging.cancel(d.getId());
            }
        });
        navigator.findByRef(chain.getId()).ifPresent(t -> {
            if (t.getStatus() == RobotTask.Status.ASSIGNED) {
                navigator.release(t.getId());
            }
        });
        batcher.find(chain.getBatchId()).ifPresent(b -> {
            if (b.getStatus() == Batch.Status.RESERVED) {
                batcher.release(b.getId());
            }
        });
        chain.setStatus(Chain.Status.COMPENSATED);
        chains.save(chain);
    }
}
