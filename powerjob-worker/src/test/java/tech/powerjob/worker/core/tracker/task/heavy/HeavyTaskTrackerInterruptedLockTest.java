package tech.powerjob.worker.core.tracker.task.heavy;

import org.junit.jupiter.api.Test;
import tech.powerjob.common.model.InstanceDetail;
import tech.powerjob.common.request.ServerQueryInstanceStatusReq;
import tech.powerjob.common.request.ServerScheduleJobReq;
import tech.powerjob.worker.common.WorkerRuntime;
import tech.powerjob.worker.common.constants.TaskStatus;
import tech.powerjob.worker.persistence.TaskDO;
import tech.powerjob.worker.persistence.TaskPersistenceService;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeavyTaskTrackerInterruptedLockTest {

    @Test
    void interruptedLockAcquisitionDoesNotReleaseAnUnownedLock() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        HeavyTaskTracker tracker = tracker(reads, writes);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread interruptedReporter = new Thread(() -> {
            Thread.currentThread().interrupt();
            try {
                // ReentrantLock.lockInterruptibly must throw even when its lock is free.
                tracker.updateTaskStatus(1L, "task-1", TaskStatus.WORKER_PROCESSING.getValue(), 1L, "");
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "interrupted-task-reporter");
        interruptedReporter.setDaemon(true);
        interruptedReporter.start();
        interruptedReporter.join(3000);

        assertFalse(interruptedReporter.isAlive(), "Interrupted lock acquisition must return");
        assertNull(failure.get(), "A failed acquisition must not unlock a lock it never acquired");
        assertEquals(0, reads.get(), "Interrupted acquisition must not enter persistence");
        assertEquals(0, writes.get());

        reportFromAnotherThread(tracker);
        assertEquals(1, reads.get());
        assertEquals(1, writes.get(), "A later reporter must still be able to acquire the lock");
    }

    @Test
    void successfulUpdateReleasesTheLockForAnotherReporter() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        AtomicInteger writes = new AtomicInteger();
        HeavyTaskTracker tracker = tracker(reads, writes);

        tracker.updateTaskStatus(1L, "task-1", TaskStatus.WORKER_PROCESSING.getValue(), 1L, "");
        reportFromAnotherThread(tracker);

        assertEquals(1, reads.get(), "The second report uses the existing task cache");
        assertEquals(2, writes.get(), "Both reporters must enter persistence");
    }

    private static void reportFromAnotherThread(HeavyTaskTracker tracker) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread reporter = new Thread(() -> {
            try {
                tracker.updateTaskStatus(1L, "task-1", TaskStatus.WORKER_PROCESSING.getValue(), 2L, "");
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "ordinary-task-reporter");
        reporter.setDaemon(true);
        reporter.start();
        reporter.join(3000);
        if (reporter.isAlive()) {
            reporter.interrupt();
            reporter.join(3000);
            throw new AssertionError("A previous update retained the task lock");
        }
        assertNull(failure.get());
    }

    private static HeavyTaskTracker tracker(AtomicInteger reads, AtomicInteger writes) {
        TaskPersistenceService persistence = (TaskPersistenceService) Proxy.newProxyInstance(
                HeavyTaskTrackerInterruptedLockTest.class.getClassLoader(),
                new Class<?>[]{TaskPersistenceService.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getTask":
                            reads.incrementAndGet();
                            TaskDO task = new TaskDO();
                            task.setStatus(TaskStatus.WORKER_RECEIVED.getValue());
                            task.setLastReportTime(0L);
                            return Optional.of(task);
                        case "updateTaskStatus":
                            writes.incrementAndGet();
                            return true;
                        default:
                            throw new AssertionError("Unexpected persistence operation: " + method.getName());
                    }
                });
        WorkerRuntime runtime = new WorkerRuntime();
        runtime.setTaskPersistenceService(persistence);
        ServerScheduleJobReq request = new ServerScheduleJobReq();
        request.setInstanceId(517901L);
        request.setJobId(1L);
        request.setExecuteType("STANDALONE");
        request.setTimeExpressionType("API");
        request.setThreadConcurrency(1);
        request.setTaskRetryNum(0);
        request.setAllWorkerAddress(Collections.emptyList());
        return new HeavyTaskTracker(request, runtime) {
            @Override
            protected void initTaskTracker(ServerScheduleJobReq req) {
                // This test needs the real segmented lock, without background scheduling.
            }

            @Override
            public InstanceDetail fetchRunningStatus(ServerQueryInstanceStatusReq req) {
                throw new UnsupportedOperationException("No status query in this fixture");
            }
        };
    }
}
