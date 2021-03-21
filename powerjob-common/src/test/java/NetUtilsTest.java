import tech.powerjob.common.PowerJobDKey;
import tech.powerjob.common.utils.NetUtils;
import org.junit.jupiter.api.Test;

/**
 * NetUtilsTest
 *
 * @author tjq
 * @since 2020/8/8
 */
public class NetUtilsTest {

    @Test
    public void testOrigin() {
        System.out.println(NetUtils.getLocalHost());
    }

    @Test
    public void testPreferredNetworkInterface() {
        System.setProperty(PowerJobDKey.PREFERRED_NETWORK_INTERFACE, "en5");
        System.out.println(NetUtils.getLocalHost());
    }

    @Test
    public void testIgnoredNetworkInterface() {
        System.setProperty(PowerJobDKey.IGNORED_NETWORK_INTERFACE_REGEX, "utun.|llw.");
        System.out.println(NetUtils.getLocalHost());
    }

}
