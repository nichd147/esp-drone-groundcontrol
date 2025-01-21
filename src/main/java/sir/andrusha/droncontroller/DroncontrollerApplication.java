package sir.andrusha.droncontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import se.bitcraze.crazyfliecontrol2.MainPresenter;

import java.io.File;

@SpringBootApplication
public class DroncontrollerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DroncontrollerApplication.class, args);
	}

//    public static void main(String[] args) throws InterruptedException {
//		new UdpServer().start();
//    }

}
