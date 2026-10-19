package exception;

// The one exception type of the project. For ex: invalid input, no free resource, or a rolled-back chain.
public class WarehouseException extends RuntimeException {
    public WarehouseException(String message) {
        super(message);
    }
}
