/** C: a robot docked on a charging station and the dispatch confirmed. */
public class Dispatch {
    public enum Status { NEW, CONFIRMED, CANCELLED }

    private final String id, ref, robotId;
    private String station;
    private Status status;

    public Dispatch(String id, String ref, String robotId) { this(id, ref, robotId, "-", Status.NEW); }

    public Dispatch(String id, String ref, String robotId, String station, Status status) {
        this.id = id; this.ref = ref; this.robotId = robotId; this.station = station; this.status = status;
    }

    public void confirm(String station) { change(Status.NEW, Status.CONFIRMED); this.station = station; }
    public void cancel() { change(Status.CONFIRMED, Status.CANCELLED); }

    private void change(Status from, Status to) {
        if (status != from) throw new IllegalStateException(id + ": cannot go " + status + " -> " + to);
        status = to;
    }

    public String toCsv() { return id + "," + ref + "," + robotId + "," + station + "," + status; }

    public static Dispatch fromCsv(String line) {
        String[] p = line.split(",");
        return new Dispatch(p[0], p[1], p[2], p[3], Status.valueOf(p[4]));
    }

    public String getId() { return id; }
    public String getRef() { return ref; }
    public String getRobotId() { return robotId; }
    public String getStation() { return station; }
    public Status getStatus() { return status; }
}
