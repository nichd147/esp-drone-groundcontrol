package se.bitcraze.crazyfliecontrol2;

import lombok.extern.slf4j.Slf4j;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class EspUdpDriver extends CrtpDriver {
    private static final String TAG = "EspUdpDriver";

    private static final int APP_PORT = 2399;
    private static final int DEVICE_PORT = 2390;
    private static final String DEVICE_ADDRESS = "192.168.43.42";

    private volatile boolean mConnectMark = false;
    private volatile DatagramSocket mSocket;
    private AtomicReference<Integer> port = new AtomicReference();
    private AtomicReference<InetAddress> address = new AtomicReference();

    private volatile ReceiveThread mReceiveThread;
    private volatile PostThread mPostThread;

    private final BlockingQueue<CrtpPacket> mInQueue;

//    private Observer<String> mObserver = new Observer<String>(){
//        @Override
//        public void onChanged(String s) {
//            int networkId = mWifiManager.getConnectionInfo().getNetworkId();
//            if (networkId == -1) {
//                disconnect();
//                notifyConnectionLost("No SoftAP connection");
//            } else {
//                if (mConnectMark) {
//                    mConnectMark = false;
//                    try {
//                        InetAddress deviceAddress = InetAddress.getByName(DEVICE_ADDRESS);
//                        mSocket = new DatagramSocket(null);
//                        mSocket.setReuseAddress(true);
//                        mSocket.bind(new InetSocketAddress(APP_PORT));
//                        mReceiveThread = new ReceiveThread(mSocket);
//                        mReceiveThread.setPacketQueue(mInQueue);
//                        mReceiveThread.start();
//                        mPostThread = new PostThread(mSocket, deviceAddress);
//                        mPostThread.start();
//                        notifyConnected();
//                    } catch (IOException e) {
//                        if (mSocket != null) {
//                            mSocket.close();
//                            mSocket = null;
//                        }
//                        mActivity.removeBroadcastObserver(mObserver);
//                        notifyConnectionFailed("Create socket failed");
//                    }
//                }
//            }
//        }
//    };

    public EspUdpDriver(DatagramSocket mSocket) {
//        mActivity = activity;
        this.mSocket = mSocket;
        this.mInQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void connect() throws IOException {
        log.info("Connect()");
        mConnectMark = true;
        notifyConnectionRequested();

        if (mConnectMark) {
            mConnectMark = false;
            mReceiveThread = new ReceiveThread(mSocket, address, port);
            mReceiveThread.setPacketQueue(mInQueue);
            mReceiveThread.start();
            mPostThread = new PostThread(mSocket, address, port);
            mPostThread.start();
            notifyConnected();
        }
    }

    @Override
    public void disconnect() {
        if (mSocket != null) {
            mSocket.close();
            mReceiveThread.interrupt();
            mReceiveThread.setPacketQueue(null);
            mReceiveThread = null;
            mPostThread.interrupt();
            mPostThread = null;
            mSocket = null;
            notifyDisconnected();
        }
    }

    @Override
    public boolean isConnected() {
        return mSocket != null && !mSocket.isClosed();
    }

    @Override
    public void sendPacket(CrtpPacket packet) {
        if (mSocket == null || mPostThread == null) {
            return;
        }

        mPostThread.sendPacket(packet);
    }

    @Override
    public CrtpPacket receivePacket(int wait) {
        try {
            return mInQueue.poll(wait, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info("ReceivePacket Interrupted");
            return null;
        }
    }

    private static class PostThread extends Thread {
        private BlockingQueue<CrtpPacket> mmQueue = new LinkedBlockingQueue<>();
        private DatagramSocket mmSocket;
        private final AtomicReference<InetAddress> address;
        private final AtomicReference<Integer> port;

        PostThread(DatagramSocket socket, AtomicReference<InetAddress> address, AtomicReference<Integer> port) {
            this.mmSocket = socket;
            this.address = address;
            this.port = port;
        }

        void sendPacket(CrtpPacket packet) {
            mmQueue.add(packet);
        }

        @Override
        public void run() {
            while (!mmSocket.isClosed() && !isInterrupted()) {
                try {
                    CrtpPacket packet = mmQueue.take();
                    byte[] data = packet.toByteArray();
                    byte[] buf = new byte[data.length + 1];
                    System.arraycopy(data, 0, buf, 0, data.length);
                    int checksum = 0;
                    for (byte b : data) {
                        checksum += (b & 0xff);
                    }
                    buf[buf.length - 1] = (byte) checksum;

                    if (address.get() == null || port.get() == null) {
                        continue;
                    }

                    log.info("run: PostData: " + Arrays.toString(buf));

                    DatagramPacket udpPacket = new DatagramPacket(buf, buf.length, address.get(), port.get());
                    mmSocket.send(udpPacket);
                } catch (IOException e) {
                    log.info("sendPacket: IOException: " + e.getMessage());
//                    mmSocket.close();
                    break;
                } catch (InterruptedException e) {
                    break;
                }
            }

            log.debug("run: PostThread End");
        }
    }

    private static class ReceiveThread extends Thread {
        private DatagramSocket mmSocket;
        private final AtomicReference<InetAddress> address;
        private final AtomicReference<Integer> port;
        private BlockingQueue<CrtpPacket> mmQueue;

        ReceiveThread(DatagramSocket socket, AtomicReference<InetAddress> address, AtomicReference<Integer> port) {
            this.mmSocket = socket;
            this.address = address;
            this.port = port;
        }

        void setPacketQueue(BlockingQueue<CrtpPacket> queue) {
            mmQueue = queue;
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            DatagramPacket udpPacket = new DatagramPacket(buf, buf.length);
            while (!mmSocket.isClosed() && !isInterrupted()) {
                try {
                    mmSocket.receive(udpPacket);
                    log.info("run: ReceiveData");

                    byte[] raw = udpPacket.getData();

                    if(new String(raw).contains("ping")){
                        if (!Integer.valueOf(udpPacket.getPort()).equals(port.get())) {
                            port.set(udpPacket.getPort());
                        }
                        if (!udpPacket.getAddress().equals(address.get())) {
                            address.set(udpPacket.getAddress());
                        }
                        continue;
                    }

                    byte[] data = new byte[udpPacket.getLength() - 1];
                    System.arraycopy(udpPacket.getData(), udpPacket.getOffset(), data, 0, data.length);
                    int checksum = 0;
                    for (byte b : data) {
                        checksum += (b & 0xff);
                    }
                    if (raw[udpPacket.getLength() - 1] != (byte) checksum) {
                        log.info("Receive Invalid packet");
                        continue;
                    }
                    CrtpPacket packet = new CrtpPacket(data);
                    if (mmQueue != null) {
                        mmQueue.add(packet);
                    }
                } catch (IOException e) {
                    log.info("receivePacket: IOException: " + e.getMessage());
//                    mmSocket.close();
                    break;
                }
            }

            log.debug("run: ReceiveThread End");
        }
    }
}
