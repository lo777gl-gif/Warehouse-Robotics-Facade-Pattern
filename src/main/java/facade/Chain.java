package facade;

// Saved progress of one order going through A -> B -> C.
public class Chain {
    public enum Status {
        PENDING_B, PENDING_C, COMPLETED, COMPENSATING, COMPENSATED
    }

    private final String id, sku, batchId;
    private final int quantity;
    private Status status;

    public Chain(String id, String sku, int quantity, String batchId, Status status) {
        this.id = id; this.sku = sku; this.quantity = quantity; this.batchId = batchId; this.status = status;
    }

    public String toCsv() {
        return id + "," + sku + "," + quantity + "," + batchId + "," + status;
    }

    public static Chain fromCsv(String line) {
        String[] p = line.split(",");
        return new Chain(p[0], p[1], Integer.parseInt(p[2]), p[3], Status.valueOf(p[4]));
    }

    public String getId() { return id; }
    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
    public String getBatchId() { return batchId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
