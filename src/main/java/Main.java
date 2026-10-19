import charging.ChargingStationManager;
import charging.DispatchRepository;
import exception.WarehouseException;
import facade.AdminFacade;
import facade.Chain;
import facade.ChainRepository;
import facade.WarehouseFacade;
import reserve.BatchRepository;
import reserve.OrderBatcher;
import robot.RobotNavigator;
import robot.TaskRepository;

import java.nio.file.Path;
import java.util.Scanner;

// Data is saved in the ./data folder (we can delete it to start from scratch).
public class Main {
    public static void main(String[] args) {
        Path dir = Path.of("data");

        OrderBatcher batcher = new OrderBatcher(new BatchRepository(dir));
        RobotNavigator navigator = new RobotNavigator(new TaskRepository(dir));
        ChargingStationManager charging = new ChargingStationManager(new DispatchRepository(dir));

        ChainRepository chains = new ChainRepository(dir);

        WarehouseFacade facade = new WarehouseFacade(batcher, navigator, charging, chains);
        AdminFacade admin = new AdminFacade(facade, batcher, navigator, charging, chains);

        int recovered = facade.recover();
        if (recovered > 0) {
            System.out.println("Recovered " + recovered + " interrupted order(s) from disk.");
        }

        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("\n1) Place order   2) Show state   3) Compensate an order   0) Exit");
            System.out.print("> ");
            if (!in.hasNextLine()){
                return;
            }
            String choice = in.nextLine().trim();
            try {
                if (choice.equals("1")) {
                    System.out.print("SKU (e.g. BOX): ");
                    String sku = in.nextLine().trim();
                    System.out.print("Quantity: ");
                    int quantity = Integer.parseInt(in.nextLine().trim());
                    Chain chain = facade.fulfil(sku, quantity);
                    System.out.println("Done: " + chain.getId() + " " + chain.getStatus());
                } else if (choice.equals("2")) {
                    System.out.print(admin.report());
                } else if (choice.equals("3")) {
                    System.out.print("Order id (e.g. CH-1): ");
                    admin.compensate(in.nextLine().trim());
                    System.out.println("Compensated");
                } else if (choice.equals("0")) {
                    return;
                } else {
                    System.out.println("Unknown option");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            } catch (WarehouseException e) {
                System.out.println("FAILED: " + e.getMessage());
            }
        }
    }
}
