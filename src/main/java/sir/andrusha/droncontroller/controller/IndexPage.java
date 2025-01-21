package sir.andrusha.droncontroller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import sir.andrusha.droncontroller.dto.SticksPosition;

@Controller
public class IndexPage {

    @RequestMapping("/")
    public String index() {
        return "index.html";
    }

}
