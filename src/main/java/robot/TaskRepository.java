package robot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

// B: persistence gateway, file tasks.csv (id,ref,robot,status)
public class TaskRepository {
    private final Path file;

    public TaskRepository(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        file = dir.resolve("tasks.csv");
    }

    public List<RobotTask> loadAll() {
        List<RobotTask> result = new ArrayList<>();
        try {
            if (Files.exists(file))
                for (String line : Files.readAllLines(file)) {
                    result.add(RobotTask.fromCsv(line));
                }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    public void saveAll(List<RobotTask> tasks) {
        List<String> lines = new ArrayList<>();
        for (RobotTask t : tasks) {
            lines.add(t.toCsv());
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
