package robot;

// B: one robot assigned to carry one order to the conveyor.
public class RobotTask {
    public enum Status {
        NEW, ASSIGNED, RELEASED
    }

    private final String id, ref;
    private String robotId;
    private Status status;

    public RobotTask(String id, String ref) {
        this(id, ref, "-", Status.NEW);
    }

    public RobotTask(String id, String ref, String robotId, Status status) {
        this.id = id; this.ref = ref; this.robotId = robotId; this.status = status;
    }

    public void assign(String robotId) {
        change(Status.NEW, Status.ASSIGNED);
        this.robotId = robotId;
    }

    public void release() {
        change(Status.ASSIGNED, Status.RELEASED);
    }

    private void change(Status from, Status to) {
        if (status != from) {
            throw new IllegalStateException(id + ": cannot go " + status + " -> " + to);
        }
        status = to;
    }

    public String toCsv() {
        return id + "," + ref + "," + robotId + "," + status;
    }

    public static RobotTask fromCsv(String line) {
        String[] p = line.split(",");
        return new RobotTask(p[0], p[1], p[2], Status.valueOf(p[3]));
    }

    public String getId() { return id; }
    public String getRef() { return ref; }
    public String getRobotId() { return robotId; }
    public Status getStatus() { return status; }
}
