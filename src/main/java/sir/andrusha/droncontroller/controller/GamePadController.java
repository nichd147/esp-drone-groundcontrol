package sir.andrusha.droncontroller.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import se.bitcraze.crazyfliecontrol2.EspDrone;
import sir.andrusha.droncontroller.dto.SticksPosition;
import sir.andrusha.droncontroller.service.DroneRegistry;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class GamePadController {

    @Autowired
    DroneRegistry droneRegistry;

    @GetMapping
    @RequestMapping(path = "/test")
    public String position() {
        return "Hello World";
    }

    @PostMapping
    @RequestMapping(path = "/api/position")
    public void position(@RequestBody SticksPosition position) {
//        log.info("got position: {}", position);
        final Set<EspDrone> drones = new HashSet();
        if (position.getDroneName() == null || position.getDroneName().isEmpty()) {
            drones.addAll(droneRegistry.getAllDrones());
        } else {
            droneRegistry.getDrone(position.getDroneName()).ifPresent(espDrone -> drones.add(espDrone));
        }
        drones.stream().forEach(drone -> {
            drone.getController()
                    .getLeftJoy().processMoveEvent(position.getLeftJoyX(), position.getLeftJoyY());
            drone.getController()
                    .getRightJoy().processMoveEvent(position.getRightJoyX(), position.getRightJoyY());
        });
    }

    @GetMapping
    @RequestMapping(path = "/api/drones")
    public Set<String> drones() {
        return droneRegistry.getDronesName();
    }
}

