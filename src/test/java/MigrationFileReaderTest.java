import org.example.migrations.fileReader.MigrationFile;
import org.example.migrations.fileReader.MigrationFileReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MigrationFileReaderTest {

    private MigrationFileReader migrationFileReader;

    @BeforeEach
    void setUp() {
        migrationFileReader = new MigrationFileReader();
    }

    @Test
    void testGetMigrationFiles() throws IOException {
        // Assuming there are migration files in the resources folder
        List<MigrationFile> migrationFiles = migrationFileReader.getMigrationFiles();

        assertNotNull(migrationFiles);
        assertTrue(migrationFiles.size() > 0);
    }

    @Test
    void testExtractVersion() {
        String version = migrationFileReader.extractVersion("V1__Initial_Migration.sql");
        assertEquals("1", version);
    }

    @Test
    void testExtractDescription() {
        String description = migrationFileReader.extractDescription("V1__Initial_Migration.sql");
        assertEquals("Initial_Migration", description);
    }
}
