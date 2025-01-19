package sir.andrusha.droncontroller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

//@SpringBootApplication
public class DroncontrollerApplication {

//	public static void main(String[] args) {
//		SpringApplication.run(DroncontrollerApplication.class, args);
//	}

	public static void main(String[] args) throws IOException {
		new UdpServer().start();
	}

}
