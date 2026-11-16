package bridges;

import java.nio.file.Path;

final class Libs {
    private Libs() {}

    static Path path(String fileName) {
        return Path.of(System.getProperty("bench.lib.dir", "lib"), fileName).toAbsolutePath();
    }
}
