package robot;

import exception.WarehouseException;

import java.util.List;
import java.util.Set;

// B: pure business rules. Chooses a robot or explains why none can do the job
public class RoutePlanner {
    public static final int ROBOT_CAPACITY = 40;
    public static final List<String> ROBOTS = List.of("R1", "R2", "R3");

    public String chooseRobot(int quantity, Set<String> busyRobots) {
        if (quantity > ROBOT_CAPACITY) {
            throw new WarehouseException("Load " + quantity + " is more than a robot can carry (" + ROBOT_CAPACITY + ")");
        }
        for (String robot : ROBOTS) {
            if (!busyRobots.contains(robot)) {
                return robot;
            }
        }
        throw new WarehouseException("No free robot");
    }
}
