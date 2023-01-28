package tech.powerjob.remote.framework.base;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * test address
 *
 * @author tjq
 * @since 2023/1/20
 */
@Slf4j
class AddressTest {

    @Test
    void testAddress() {
        String ip = "192.168.1.1:10085";
        final Address address = Address.fromIpv4(ip);
        log.info("[AddressTest] parse address: {}", address);
        assert ip.equals(address.toFullAddress());
    }
}