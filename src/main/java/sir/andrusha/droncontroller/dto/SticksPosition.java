package sir.andrusha.droncontroller.dto;

import lombok.Data;

@Data
public class SticksPosition {
    String droneName;
    int leftJoyX;
    int leftJoyY;
    int rightJoyX;
    int rightJoyY;

    public String toString() {
        return "[" + leftJoyX + ";" + leftJoyY + "] [" + rightJoyX + ";" + rightJoyY + "]";
    }
}
