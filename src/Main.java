import com.nortal.ping.simple.PingService;
import com.nortal.ping.simple.PingServiceImpl;
import com.nortal.ping.simple.PingServiceLogger;

/**
 * @author Margus Hanni <margus.hanni@nortal.com>
 */
public class Main {
    public static void main(String[] args) throws Exception {
        PingServiceLogger.setup();
        PingService pingService = new PingServiceImpl();
        pingService.ping();
    }
}