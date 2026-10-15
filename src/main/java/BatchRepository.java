import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** A: persistence gateway, file batches.csv (id,ref,sku,quantity,status). */
public class BatchRepository {
    private final Path file;

    public BatchRepository(Path dir) {
        try { Files.createDirectories(dir); } catch (IOException e) { throw new UncheckedIOException(e); }
        file = dir.resolve("batches.csv");
    }

    public List<Batch> loadAll() {
        List<Batch> result = new ArrayList<>();
        try {
            if (Files.exists(file))
                for (String line : Files.readAllLines(file)) {
                    result.add(Batch.fromCsv(line));
                }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    public void saveAll(List<Batch> batches) {
        List<String> lines = new ArrayList<>();
        for (Batch b : batches) {
            lines.add(b.toCsv());
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
