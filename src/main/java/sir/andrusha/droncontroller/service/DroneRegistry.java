package sir.andrusha.droncontroller.service;

import org.springframework.stereotype.Service;
import se.bitcraze.crazyfliecontrol2.EspDrone;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DroneRegistry {

    private final Map<String, EspDrone> map;

    public DroneRegistry() throws IOException {
        this.map = new ConcurrentHashMap<>();
    }

    public Optional<EspDrone> getDrone(String droneName) {
        return Optional.ofNullable(map.get(droneName));
    }

    public Set<EspDrone> getAllDrones() {
        return new HashSet<>(map.values());
    }

    public Integer initDrone(String name) {
        EspDrone pr = map.computeIfAbsent(name, this::createEspDroneAndConnect);
        return pr.getPort();
    }

    private EspDrone createEspDroneAndConnect(String name) {
        EspDrone pr0 = new EspDrone(name);
        pr0.connect();
        return pr0;
    }

    public Set<String> getDronesName() {
        return this.map.keySet();
    }
}
