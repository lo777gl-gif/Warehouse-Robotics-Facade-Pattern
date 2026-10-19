package charging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

// C: persistence gateway, files dispatches.csv (id,ref,robot,station,status) and dispatch.log
public class DispatchRepository {
    private final Path file, logFile;

    public DispatchRepository(Path dir) {
        try { Files.createDirectories(dir); } catch (IOException e) { throw new UncheckedIOException(e); }
        file = dir.resolve("dispatches.csv");
        logFile = dir.resolve("dispatch.log");
    }

    public List<Dispatch> loadAll() {
        List<Dispatch> result = new ArrayList<>();
        try {
            if (Files.exists(file))
                for (String line : Files.readAllLines(file)) result.add(Dispatch.fromCsv(line));
        } catch (IOException e) { throw new UncheckedIOException(e); }
        return result;
    }

    public void saveAll(List<Dispatch> dispatches) {
        List<String> lines = new ArrayList<>();
        for (Dispatch d : dispatches) lines.add(d.toCsv());
        try { Files.write(file, lines); } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    // The dispatch logger: appends one line to dispatch.log
    public void log(String message) {
        try {
            Files.writeString(logFile, message + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public List<String> readLog() {
        try { return Files.exists(logFile) ? Files.readAllLines(logFile) : List.of(); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
