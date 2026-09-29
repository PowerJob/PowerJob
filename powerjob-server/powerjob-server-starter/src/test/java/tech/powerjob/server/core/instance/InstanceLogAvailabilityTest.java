package tech.powerjob.server.core.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tech.powerjob.common.enums.LogLevel;
import tech.powerjob.common.model.InstanceLogContent;
import tech.powerjob.server.common.utils.OmsFileUtils;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.local.LocalInstanceLogDO;
import tech.powerjob.server.persistence.local.LocalInstanceLogRepository;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InstanceLogAvailabilityTest {
    @TempDir Path temporary;

    @Test void earlyQueryDoesNotReplaceLogsReportedBeforeSync() throws Exception {
        node("owner", "early-query-archive");
    }

    @Test void independentServerCanReadArchiveAfterAnEarlierMiss() throws Exception {
        node("reader", "early-query");
        node("owner", "archive");
        node("reader", "read-archive");
    }

    @Test void syncWithoutLogsDoesNotArchivePlaceholderOrDeleteLocalData() throws Exception {
        node("owner", "early-sync");
    }

    @Test void placeholderFromPreviousVersionDoesNotHideSharedArchive() throws Exception {
        node("owner", "archive");
        node("reader", "legacy-placeholder-read");
    }

    @Test void placeholderFromPreviousVersionDoesNotReplaceNewLocalReports() throws Exception {
        node("owner", "legacy-placeholder-archive");
    }

    @Test void validStableFileAndRepeatedSyncKeepTheSameBytes() throws Exception {
        node("owner", "stable-cache");
    }

    private void node(String name, String scenario) throws Exception {
        Path home = temporary.resolve(name);
        Path dfs = temporary.resolve("shared-dfs");
        Files.createDirectories(home); Files.createDirectories(dfs);
        Path output = temporary.resolve(name + "-" + scenario + ".log");
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        // OmsFileUtils fixes user.home in a static field: separate JVMs model separate Server caches
        // without changing the test runner's home or touching a real PowerJob workspace.
        Process process = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
                "-Duser.home=" + home, "-cp", classpath, Node.class.getName(), scenario, dfs.toString())
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Isolated log service probe exceeded deadline");
        }
        assertEquals(0, process.exitValue(), new String(Files.readAllBytes(output), StandardCharsets.UTF_8));
    }

    /** Invoked only in a child JVM whose entire home is a JUnit temporary directory. */
    public static final class Node {
        private static final long INSTANCE = 40L;
        private static final String NO_LOG = "SYSTEM: There is no online log for this job instance.";
        private final List<LocalInstanceLogDO> rows = new ArrayList<>();
        private final LocalInstanceLogRepository repository = mock(LocalInstanceLogRepository.class);
        private final InstanceLogService service = new InstanceLogService();
        private final DirectoryDfs dfs;

        private Node(Path shared) {
            dfs = new DirectoryDfs(shared);
            when(repository.saveAll(anyList())).thenAnswer(call -> {
                List<LocalInstanceLogDO> input = call.getArgument(0); rows.addAll(input); return input;
            });
            when(repository.findByInstanceIdOrderByLogTime(INSTANCE)).thenAnswer(call -> new ArrayList<>(rows).stream());
            when(repository.deleteByInstanceId(INSTANCE)).thenAnswer(call -> { long size = rows.size(); rows.clear(); return size; });
            PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
            when(transactions.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
            ReflectionTestUtils.setField(service, "localTransactionTemplate", new TransactionTemplate(transactions));
            ReflectionTestUtils.setField(service, "localInstanceLogRepository", repository);
            ReflectionTestUtils.setField(service, "dFsService", dfs);
            ReflectionTestUtils.setField(service, "powerJobBackgroundPool", new ConcurrentTaskExecutor(Runnable::run));
        }

        public static void main(String[] args) throws Exception {
            new Node(Paths.get(args[1])).execute(args[0]);
        }

        private void execute(String scenario) throws Exception {
            switch (scenario) {
                case "early-query":
                    assertEquals(NO_LOG, read(service.downloadInstanceLog(INSTANCE)));
                    return;
                case "early-query-archive":
                    assertEquals(NO_LOG, read(service.downloadInstanceLog(INSTANCE)));
                    archive(true);
                    return;
                case "archive":
                    archive();
                    return;
                case "early-sync":
                    service.sync(INSTANCE);
                    assertFalse(Files.exists(dfs.archived()), "A missing log is not an archive");
                    verify(repository, never()).deleteByInstanceId(INSTANCE);
                    archive();
                    return;
                case "legacy-placeholder-read":
                    placeholder();
                    readArchive();
                    return;
                case "read-archive":
                    readArchive();
                    return;
                case "legacy-placeholder-archive":
                    placeholder();
                    archive();
                    return;
                case "stable-cache":
                    archive();
                    byte[] original = Files.readAllBytes(stable());
                    Files.delete(dfs.archived());
                    assertArrayEquals(original, Files.readAllBytes(service.downloadInstanceLog(INSTANCE).toPath()));
                    service.sync(INSTANCE);
                    assertArrayEquals(original, Files.readAllBytes(dfs.archived()));
                    verify(repository, times(1)).findByInstanceIdOrderByLogTime(INSTANCE);
                    assertEquals(0, dfs.downloads);
                    return;
                default: throw new AssertionError("Unknown fixture scenario");
            }
        }

        private void archive() throws Exception {
            archive(false);
        }

        private void archive(boolean inspectLiveLog) throws Exception {
            service.submitLogs("worker:27777", Arrays.asList(
                    new InstanceLogContent(INSTANCE, 1700000000000L, LogLevel.INFO.getV(), "first-中文😀"),
                    new InstanceLogContent(INSTANCE, 1700000001000L, LogLevel.INFO.getV(), "second-quote\"&")));
            assertEquals(2, rows.size());
            if (inspectLiveLog) assertRealLogs(read(service.downloadInstanceLog(INSTANCE)));
            service.sync(INSTANCE);
            String archived = new String(Files.readAllBytes(dfs.archived()), StandardCharsets.UTF_8);
            assertRealLogs(archived);
            assertTrue(rows.isEmpty(), "Local rows are removed only after generating the real archive");
            assertArrayEquals(Files.readAllBytes(dfs.archived()), Files.readAllBytes(stable()));
            assertArrayEquals(Files.readAllBytes(dfs.archived()), Files.readAllBytes(service.downloadInstanceLog(INSTANCE).toPath()));
        }

        private void readArchive() throws Exception {
            assertTrue(rows.isEmpty(), "Reader has no local report data");
            File file = service.downloadInstanceLog(INSTANCE);
            assertRealLogs(read(file));
            assertArrayEquals(Files.readAllBytes(dfs.archived()), Files.readAllBytes(file.toPath()));
            assertEquals(1, dfs.downloads, "Earlier DFS miss must not suppress a later download");
            assertEquals(read(file), read(service.downloadInstanceLog(INSTANCE)));
            assertEquals(1, dfs.downloads, "Valid stable file should remain cached");
        }

        private void placeholder() throws Exception {
            Files.createDirectories(stable().getParent());
            Files.write(stable(), NO_LOG.getBytes(StandardCharsets.UTF_8));
        }

        private static Path stable() { return Paths.get(OmsFileUtils.genLogDirPath(), INSTANCE + "-stable.log"); }
        private static String read(File file) throws Exception { return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8); }
        private static void assertRealLogs(String value) {
            assertTrue(value.contains("[worker:27777] INFO first-中文😀"), value);
            assertTrue(value.contains("[worker:27777] INFO second-quote\"&"), value);
            assertEquals(2, value.split("\\r?\\n").length);
            assertFalse(value.contains(NO_LOG));
        }

        private static final class DirectoryDfs implements DFsService {
            private final Path directory;
            private int downloads;
            private DirectoryDfs(Path directory) { this.directory = directory; }
            private Path archived() { return directory.resolve("oms-" + INSTANCE + ".log"); }
            @Override public void store(StoreRequest request) throws Exception {
                assertEquals("oms-" + INSTANCE + ".log", request.getFileLocation().getName());
                Files.copy(request.getLocalFile().toPath(), archived(), StandardCopyOption.REPLACE_EXISTING);
            }
            @Override public void download(DownloadRequest request) throws Exception {
                assertEquals("oms-" + INSTANCE + ".log", request.getFileLocation().getName());
                Files.copy(archived(), request.getTarget().toPath(), StandardCopyOption.REPLACE_EXISTING); downloads++;
            }
            @Override public Optional<FileMeta> fetchFileMeta(FileLocation location) throws Exception {
                return Files.exists(archived()) ? Optional.of(new FileMeta().setLength(Files.size(archived()))) : Optional.empty();
            }
        }
    }
}
