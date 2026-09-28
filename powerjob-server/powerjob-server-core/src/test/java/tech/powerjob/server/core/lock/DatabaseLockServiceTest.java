package tech.powerjob.server.core.lock;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import tech.powerjob.server.persistence.remote.model.OmsLockDO;
import tech.powerjob.server.persistence.remote.repository.OmsLockRepository;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseLockServiceTest {

    private final OmsLockRepository repository = mock(OmsLockRepository.class);
    private final DatabaseLockService service = new DatabaseLockService(repository);

    @Test
    void releasedLockBetweenInsertConflictAndLookupCanBeRetried() throws Exception {
        AtomicReference<OmsLockDO> row = new AtomicReference<>();
        CountDownLatch insertConflict = new CountDownLatch(1);
        CountDownLatch ownerReleased = new CountDownLatch(1);
        when(repository.saveAndFlush(any(OmsLockDO.class))).thenAnswer(invocation -> {
            OmsLockDO attempted = invocation.getArgument(0);
            if (row.compareAndSet(null, attempted)) {
                return attempted;
            }
            insertConflict.countDown();
            assertTrue(ownerReleased.await(5, TimeUnit.SECONDS));
            throw new DataIntegrityViolationException("duplicate lock name");
        });
        when(repository.findByLockName("server_init_lock")).thenAnswer(invocation -> row.get());
        when(repository.deleteByLockName("server_init_lock")).thenAnswer(invocation -> {
            row.set(null);
            ownerReleased.countDown();
            return 1;
        });

        assertTrue(service.tryLock("server_init_lock", 15000));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> contender = executor.submit(() -> service.tryLock("server_init_lock", 15000));
            assertTrue(insertConflict.await(5, TimeUnit.SECONDS));
            service.unlock("server_init_lock");

            // A failed insert must not grant ownership, even if its conflicting row disappeared.
            assertFalse(contender.get(5, TimeUnit.SECONDS));
            assertTrue(service.tryLock("server_init_lock", 15000));
            verify(repository, times(3)).saveAndFlush(any(OmsLockDO.class));
            verify(repository, times(1)).deleteByLockName("server_init_lock");
        } finally {
            ownerReleased.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void successfulInsertGrantsOwnershipWithoutLookingUpTheRow() {
        assertTrue(service.tryLock("new_lock", 15000));
        verify(repository, never()).findByLockName(anyString());
    }

    @Test
    void activeContendedLockRemainsOwnedByItsCurrentHolder() {
        when(repository.saveAndFlush(any(OmsLockDO.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate lock name"));
        when(repository.findByLockName("active_lock"))
                .thenReturn(new OmsLockDO("active_lock", "other-owner", 60000L));

        assertFalse(service.tryLock("active_lock", 15000));
        verify(repository, never()).deleteByLockName(anyString());
    }

    @Test
    void failedInsertWithoutExistingRowDoesNotGrantOwnership() {
        when(repository.saveAndFlush(any(OmsLockDO.class)))
                .thenThrow(new IllegalStateException("synthetic write failure"));

        assertFalse(service.tryLock("failed_lock", 15000));
        verify(repository, times(1)).saveAndFlush(any(OmsLockDO.class));
        verify(repository, never()).deleteByLockName(anyString());
    }

    @Test
    void expiredLockCanStillBeReleasedAndAcquired() {
        OmsLockDO expired = new OmsLockDO("expired_lock", "other-owner", 1000L);
        expired.setGmtCreate(new Date(System.currentTimeMillis() - 60000));
        when(repository.saveAndFlush(any(OmsLockDO.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate lock name"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByLockName("expired_lock")).thenReturn(expired);

        assertTrue(service.tryLock("expired_lock", 15000));
        verify(repository).deleteByLockName("expired_lock");
        verify(repository, times(2)).saveAndFlush(any(OmsLockDO.class));
    }
}
