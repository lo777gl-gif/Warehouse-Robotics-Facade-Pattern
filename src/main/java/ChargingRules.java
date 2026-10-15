import java.util.List;
import java.util.Set;

/** C: pure business rules. Chooses a free charging station. */
public class ChargingRules {
    public static final List<String> STATIONS = List.of("S1", "S2");

    public String chooseStation(Set<String> busyStations) {
        for (String station : STATIONS)
            if (!busyStations.contains(station)) return station;
        throw new WarehouseException("No free charging station");
    }
}
