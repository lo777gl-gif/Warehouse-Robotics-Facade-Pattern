import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** B: gives an order to a free robot and frees the robot again. */
public class RobotNavigator {
    private final TaskRepository repo;
    private final RoutePlanner planner = new RoutePlanner();

    public RobotNavigator(TaskRepository repo) {
        this.repo = repo;
    }

    public RobotTask assign(String ref, int quantity) {
        List<RobotTask> all = repo.loadAll();
        String robot = planner.chooseRobot(quantity, busy(all));
        RobotTask task = new RobotTask("TASK-" + (all.size() + 1), ref);
        task.assign(robot);
        all.add(task);
        repo.saveAll(all);
        return task;
    }

    private Set<String> busy(List<RobotTask> all) {
        Set<String> busy = new HashSet<>();
        for (RobotTask t : all)
            if (t.getStatus() == RobotTask.Status.ASSIGNED) {
                busy.add(t.getRobotId());
            }
        return busy;
    }

    public void release(String taskId) {
        List<RobotTask> all = repo.loadAll();
        for (RobotTask t : all) {
            if (t.getId().equals(taskId)) {
                t.release();
                repo.saveAll(all);
                return;
            }
        }
        throw new WarehouseException("Unknown task: " + taskId);
    }

    public Optional<RobotTask> findByRef(String ref) {
        return repo.loadAll().stream().filter(t -> t.getRef().equals(ref)).findFirst();
    }

    public Set<String> busyRobots() {
        return busy(repo.loadAll());
    }


}
