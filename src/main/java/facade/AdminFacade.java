package facade;

import charging.ChargingStationManager;
import exception.WarehouseException;
import reserve.OrderBatcher;
import robot.RobotNavigator;

import java.util.List;

// Facade 2: for the operator. Shows what is saved in all three subsystems and compensates a chain by hand
public class AdminFacade {
    private final WarehouseFacade facade;
    private final OrderBatcher batcher;
    private final RobotNavigator navigator;
    private final ChargingStationManager charging;
    private final ChainRepository chains;

    public AdminFacade(WarehouseFacade facade, OrderBatcher batcher, RobotNavigator navigator, ChargingStationManager charging, ChainRepository chains) {
        this.facade = facade;
        this.batcher = batcher;
        this.navigator = navigator;
        this.charging = charging;
        this.chains = chains;
    }

    public void compensate(String chainId) {
        Chain chain = chains.find(chainId).orElseThrow(() -> new WarehouseException("Unknown chain " + chainId));
        if (chain.getStatus() == Chain.Status.COMPENSATED) {
            throw new WarehouseException(chainId + " is already compensated");
        }
        facade.undo(chain);
    }

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("Busy robots: ").append(navigator.busyRobots())
                .append(", busy charging stations: ")
                .append(charging.busyStations()).append("\n");
        List<Chain> all = chains.all();
        if (all.isEmpty()) {
            sb.append("(no orders yet)\n");
        }
        for (Chain c : all) {
            sb.append(c.getId()).append(" ").append(c.getSku()).append(" x").append(c.getQuantity())
              .append("  ").append(c.getStatus()).append("\n");
            sb.append("   A: ").append(batcher.find(c.getBatchId()).map(b -> b.getStatus().toString()).orElse("-")).append("\n");
            sb.append("   B: ").append(navigator.findByRef(c.getId()).map(t -> t.getStatus() + " " + t.getRobotId()).orElse("-")).append("\n");
            sb.append("   C: ").append(charging.findByRef(c.getId()).map(d -> d.getStatus() + " " + d.getStation()).orElse("-")).append("\n");
        }
        return sb.toString();
    }
}
