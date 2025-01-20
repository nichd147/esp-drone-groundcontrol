package sir.andrusha.droncontroller.controller;

import org.springframework.web.bind.annotation.*;
import sir.andrusha.droncontroller.dto.SticksPosition;

@RestController
public class GamePadController {

    @GetMapping
    @RequestMapping(path = "/api/v1/test")
    public String position(){
        return "Hello World";
    }

    @PostMapping
    @RequestMapping(path = "/api/v1/position")
    public String position(@RequestBody SticksPosition position){
        return "Hello World";
    }
}
