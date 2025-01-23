package sir.andrusha.droncontroller.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sir.andrusha.droncontroller.service.DroneRegistry;
import sir.andrusha.droncontroller.service.UdpPingPongThread;

import java.io.IOException;

@Configuration
public class MainPresenterConfiguration {

    @Bean
    public UdpPingPongThread udpPingPongThread(DroneRegistry droneRegistry) throws IOException {
        UdpPingPongThread udpPingPongThread = new UdpPingPongThread(droneRegistry);
        udpPingPongThread.start();
        return udpPingPongThread;
    }
}
