// A: a batch of goods reserved on the shelf for one order.
public class Batch {
    public enum Status {
        NEW, RESERVED, RELEASED
    }

    private final String id, ref, sku;
    private final int quantity;
    private Status status;

    public Batch(String id, String ref, String sku, int quantity) {
        this(id, ref, sku, quantity, Status.NEW);
    }

    public Batch(String id, String ref, String sku, int quantity, Status status) {
        this.id = id; this.ref = ref; this.sku = sku; this.quantity = quantity; this.status = status;
    }

    public void reserve() {
        change(Status.NEW, Status.RESERVED);
    }
    public void release() {
        change(Status.RESERVED, Status.RELEASED);
    }

    private void change(Status from, Status to) {
        if (status != from) throw new IllegalStateException(id + ": cannot go " + status + " -> " + to);
        status = to;
    }

    public String toCsv() {
        return id + "," + ref + "," + sku + "," + quantity + "," + status;
    }

    public static Batch fromCsv(String line) {
        String[] p = line.split(",");
        return new Batch(p[0], p[1], p[2], Integer.parseInt(p[3]), Status.valueOf(p[4]));
    }

    public String getId() { return id; }
    public String getRef() { return ref; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public Status getStatus() { return status; }
}
