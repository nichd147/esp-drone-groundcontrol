package sir.andrusha.droncontroller;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@Slf4j
public class UdpServer extends Thread {

    protected DatagramSocket socket = null;
    protected boolean running;
    protected byte[] buf = new byte[1024];

    public UdpServer() throws IOException {
        InetAddress deviceAddress = InetAddress.getByName("192.168.5.2");
        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(8070));
    }

    public void run() {
        log.info("Server is running");
        running = true;

        while (running) {
            try {
                DatagramPacket in = new DatagramPacket(buf, buf.length);
                socket.receive(in);
                String received = new String(in.getData(), 0, in.getLength());
                log.info("received {}", received);
                if (received.contains("ping")) {
                    InetAddress address = in.getAddress();
                    int port = in.getPort();
                    DatagramPacket out = new DatagramPacket(buf, buf.length, address, port);
                    out.setData("pong".getBytes());
                    socket.send(out);
                }
            } catch (IOException e) {
                log.error("Error occurred: ", e);
                running = false;
            }
        }
        socket.close();
    }
}
