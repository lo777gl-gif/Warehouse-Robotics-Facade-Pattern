import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** C: docks the robot on a free charging station, logs the dispatch, and can cancel it. */
public class ChargingStationManager {
    private final DispatchRepository repo;
    private final ChargingRules rules = new ChargingRules();

    public ChargingStationManager(DispatchRepository repo) { this.repo = repo; }

    public Dispatch confirm(String ref, String robotId) {
        List<Dispatch> all = repo.loadAll();
        String station = rules.chooseStation(busy(all));
        Dispatch dispatch = new Dispatch("DSP-" + (all.size() + 1), ref, robotId);
        dispatch.confirm(station);
        all.add(dispatch);
        repo.saveAll(all);
        repo.log("CONFIRMED " + dispatch.getId() + " robot=" + robotId + " station=" + station);
        return dispatch;
    }

    public void cancel(String dispatchId) {
        List<Dispatch> all = repo.loadAll();
        for (Dispatch d : all) {
            if (d.getId().equals(dispatchId)) {
                d.cancel();
                repo.saveAll(all);
                repo.log("CANCELLED " + d.getId());
                return;
            }
        }
        throw new WarehouseException("Unknown dispatch " + dispatchId);
    }

    public Optional<Dispatch> findByRef(String ref) {
        return repo.loadAll().stream().filter(d -> d.getRef().equals(ref)).findFirst();
    }

    public Set<String> busyStations() { return busy(repo.loadAll()); }

    public List<String> log() { return repo.readLog(); }

    private Set<String> busy(List<Dispatch> all) {
        Set<String> busy = new HashSet<>();
        for (Dispatch d : all)
            if (d.getStatus() == Dispatch.Status.CONFIRMED) busy.add(d.getStation());
        return busy;
    }
}
