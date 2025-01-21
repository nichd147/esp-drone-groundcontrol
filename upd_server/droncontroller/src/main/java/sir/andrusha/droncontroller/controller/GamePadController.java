package sir.andrusha.droncontroller.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import se.bitcraze.crazyfliecontrol2.MainPresenter;
import sir.andrusha.droncontroller.dto.SticksPosition;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class GamePadController {

    @Autowired
    MainPresenter mainPresenter;

    @GetMapping
    @RequestMapping(path = "/test")
    public String position() {
        return "Hello World";
    }

    @PostMapping
    @RequestMapping(path = "/api/position")
    public void position(@RequestBody SticksPosition position) {
        log.info("got position: {}", position);



    }
}

