import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Saves chains in chains.csv (id,sku,quantity,batchId,status).
public class ChainRepository {
    private final Path file;

    public ChainRepository(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        file = dir.resolve("chains.csv");
    }

    public List<Chain> all() {
        List<Chain> result = new ArrayList<>();
        try {
            if (Files.exists(file))
                for (String line : Files.readAllLines(file)) {
                    result.add(Chain.fromCsv(line));
                }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    public String nextId() {
        return "CH-" + (all().size() + 1);
    }

    // Insert or update
    public void save(Chain chain) {
        List<Chain> all = all();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(chain.getId())) {
                all.set(i, chain);
                found = true;
            }
        }
        if (!found) {
            all.add(chain);
        }

        List<String> lines = new ArrayList<>();
        for (Chain c : all) {
            lines.add(c.toCsv());
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Optional<Chain> find(String id) {
        for (Chain c : all()) {
            if (id.equals(c.getId())) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
