package se.bitcraze.crazyfliecontrol2;

import lombok.extern.slf4j.Slf4j;
import se.bitcraze.crazyflie.lib.crazyflie.ConnectionAdapter;
import se.bitcraze.crazyflie.lib.crazyflie.Crazyflie;
import se.bitcraze.crazyflie.lib.crazyradio.ConnectionData;
import se.bitcraze.crazyflie.lib.crtp.CommanderPacket;
import se.bitcraze.crazyflie.lib.crtp.CrtpDriver;
import se.bitcraze.crazyflie.lib.crtp.CrtpPacket;
import se.bitcraze.crazyflie.lib.crtp.ZDistancePacket;
import se.bitcraze.crazyflie.lib.log.LogAdapter;
import se.bitcraze.crazyflie.lib.log.LogConfig;
import se.bitcraze.crazyflie.lib.log.Logg;
import se.bitcraze.crazyflie.lib.param.Param;
import se.bitcraze.crazyflie.lib.param.ParamListener;
import se.bitcraze.crazyflie.lib.toc.Toc;
import se.bitcraze.crazyflie.lib.toc.VariableType;
import se.bitcraze.crazyfliecontrol.console.ConsoleListener;
import se.bitcraze.crazyfliecontrol.controller.Controls;
import se.bitcraze.crazyfliecontrol.controller.IController;
import se.bitcraze.crazyfliecontrol.controller.JoystickView;
import se.bitcraze.crazyfliecontrol.controller.TouchController;

import java.io.File;
import java.util.Map;

@Slf4j
public class MainPresenter {

    private static final String LOG_TAG = "Crazyflie-MainPresenter";

    private Crazyflie mCrazyflie;
    private CrtpDriver mDriver;

    private Logg mLogg;
    private LogConfig mDefaultLogConfig = null;

    private Toc mLogToc;
    private Toc mParamToc;

    private boolean mHeadlightToggle = false;
    private boolean mSoundToggle = false;
    private int mRingEffect = 0;
    private int mNoRingEffect = 0;
    private int mCpuFlash = 0;
    private boolean isZrangerAvailable = false;
    private boolean heightHold = false;

    private Thread mSendJoystickDataThread;
    private ConsoleListener mConsoleListener;

    private Controls mControls;
    private IController mController;
    private JoystickView mJoystickViewLeft =  new JoystickView();
    private JoystickView mJoystickViewRight =  new JoystickView();

    public MainPresenter() {
    }

    private ConnectionAdapter crazyflieConnectionAdapter = new ConnectionAdapter() {

        @Override
        public void connectionRequested() {
            log.info("Connecting ...");
        }

        @Override
        public void connected() {
            log.info("Connected");
            if (mCrazyflie == null) {
                return;
            }
            CrtpDriver driver = mCrazyflie.getDriver();
            mCrazyflie.startConnectionSetup_BLE();
        }

        @Override
        public void setupFinished() {
            Param param = mCrazyflie.getParam();
            if (param != null) {
                final Toc paramToc = param.getToc();
                if (paramToc != null) {
                    mParamToc = paramToc;
                    log.info("Parameters TOC fetch finished: " + paramToc.getTocSize());
                    checkForBuzzerDeck();
                    checkForNoOfRingEffects();
                    checkForZRanger();
                }
            }
            mLogg = mCrazyflie.getLogg();
            if (mLogg != null) {
                final Toc logToc = mLogg.getToc();
                if (logToc != null) {
                    mLogToc = logToc;
                    log.info("Log TOC fetch finished: " + logToc.getTocSize());
                    mDefaultLogConfig = createDefaultLogConfig();
                    startLogConfigs(mDefaultLogConfig);
                }
            }
            startSendJoystickDataThread();
        }

        @Override
        public void connectionLost(final String msg) {
            log.info(msg);
            disconnect();
        }

        @Override
        public void connectionFailed(final String msg) {
            log.info(msg);
            disconnect();
        }

        @Override
        public void disconnected() {
            log.info("Disconnected");
            stopLogConfigs(mDefaultLogConfig);
        }

        @Override
        public void linkQualityUpdated(final int quality) {
            log.info("quality {}%", quality);
        }
    };

    // TODO: Replace with specific test for buzzer deck
    private void checkForBuzzerDeck() {
        //activate buzzer sound button when a CF2 is recognized (a buzzer can not yet be detected separately)
        mCrazyflie.getParam().addParamListener(new ParamListener("cpu", "flash") {
            @Override
            public void updated(String name, Number value) {
                mCpuFlash = mCrazyflie.getParam().getValue("cpu.flash").intValue();
                //enable buzzer action button when a CF2 is found (cpu.flash == 1024)
                if (mCpuFlash == 1024) {
                    log.info("setBuzzerSoundButtonEnablement NOT IMPLEMENTED");
                }
                log.debug("CPU flash: " + mCpuFlash);
            }
        });
        mCrazyflie.getParam().requestParamUpdate("cpu.flash");
    }

    private void checkForZRanger() {
        //this should return true when either a zRanger or a flow deck is connected
        mCrazyflie.getParam().addParamListener(new ParamListener("deck", "bcZRanger") {
            @Override
            public void updated(String name, Number value) {
                isZrangerAvailable = mCrazyflie.getParam().getValue("deck.bcZRanger").intValue() == 1;
                // TODO: indicate in the UI that the zRanger sensor is installed
                if (isZrangerAvailable) {
                    log.info("Found zRanger sensor.");
                }
                log.debug("is zRanger installed: " + isZrangerAvailable);
            }
        });
        mCrazyflie.getParam().requestParamUpdate("deck.bcZRanger");
    }

    private void checkForNoOfRingEffects() {
        //set number of LED ring effects
        mCrazyflie.getParam().addParamListener(new ParamListener("ring", "neffect") {
            @Override
            public void updated(String name, Number value) {
                mNoRingEffect = mCrazyflie.getParam().getValue("ring.neffect").intValue();
                //enable LED ring action buttons only when ring.neffect parameter is set correctly (=> hence it's a CF2 with a LED ring)
                if (mNoRingEffect > 0) {
                    log.info("setRingEffectButtonEnablement NOT IMPLEMENTED");
                    log.info("setHeadlightButtonEnablement NOT IMPLEMENTED");
                }
                log.debug("No of ring effects: " + mNoRingEffect);
            }
        });
        mCrazyflie.getParam().requestParamUpdate("ring.neffect");
    }

    private void sendPacket(CrtpPacket packet) {
        if (mCrazyflie != null) {
            mCrazyflie.sendPacket(packet);
        }
    }

    /**
     * Start thread to periodically send commands containing the user input
     */
    private void startSendJoystickDataThread() {
        mSendJoystickDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (mCrazyflie != null) {
                    IController controller = getController();
                    if (controller == null) {
                        log.debug("SendJoystickDataThread: controller is null.");
                        break;
                    }
                    float roll = controller.getRoll();
                    float pitch = controller.getPitch();
                    float yaw = controller.getYaw();
                    float thrustAbsolute = controller.getThrustAbsolute();
                    boolean xmode = getControls().isXmode();
                    if (heightHold) {
                        float targetHeight = controller.getTargetHeight();
                        sendPacket(new ZDistancePacket(roll, pitch, yaw, targetHeight));
                    } else {
                        sendPacket(new CommanderPacket(roll, pitch, yaw, (char) thrustAbsolute, xmode));
                    }
                    try {
//                        Thread.sleep(1000);
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        log.debug("SendJoystickDataThread was interrupted.");
                        break;
                    }
                }
            }
        });
        mSendJoystickDataThread.start();
    }

    public Controls getControls() {
        if (mControls == null) {
            mControls = new Controls();
            mControls.setDefaultPreferenceValues();
            mControls.setControlConfig();
        }
        return mControls;
    }

    public IController getController() {
        if (mController == null) {
            mController = new TouchController(getControls(), mJoystickViewLeft, mJoystickViewRight);
            mController.enable();
        }

        return mController;
    }

    public void connect(File cacheDir) {
        log.debug("connectUDP()");
        disconnect();
        mDriver = null;
        mDriver = new EspUdpDriver();
        connect(cacheDir, null);
    }

    private void connect(File mCacheDir, ConnectionData connectionData) {
        if (mDriver != null) {
            // add listener for connection status
            mDriver.addConnectionListener(crazyflieConnectionAdapter);

            mCrazyflie = new Crazyflie(mDriver, mCacheDir);
            // connect
            mCrazyflie.connect();

            // add console listener
            if (mCrazyflie != null) {
                mConsoleListener = new ConsoleListener();
                mConsoleListener.setMainActivity();
                mCrazyflie.addDataListener(mConsoleListener);
            }
        } else {
            log.info("Cannot connect: Crazyradio not attached and Bluetooth LE not available");
        }
    }

    public void disconnect() {
        log.debug("disconnect()");
        // kill sendJoystickDataThread first to avoid NPE
        if (mSendJoystickDataThread != null) {
            mSendJoystickDataThread.interrupt();
            mSendJoystickDataThread = null;
        }

        if (mCrazyflie != null) {
            mCrazyflie.removeDataListener(mConsoleListener);
            mCrazyflie.disconnect();
            mCrazyflie = null;
        }

        if (mDriver != null) {
            mDriver.removeConnectionListener(crazyflieConnectionAdapter);
        }

        // link quality is not available when there is no active connection
        log.info("setLinkQualityText(\"N/A\") NOT IMPLEMENTED");
    }

    //TODO: make runAltAction more universal
    public void runAltAction(String action) {
        log.info("runAltAction: " + action);
        if (mCrazyflie != null) {
            if ("ring.headlightEnable".equalsIgnoreCase(action)) {
                // Toggle LED ring headlight
                mHeadlightToggle = !mHeadlightToggle;
                mCrazyflie.setParamValue(action, mHeadlightToggle ? 1 : 0);
                log.info("toggleHeadlightButtonColor(mHeadlightToggle) NOT IMPLEMENTED");
            } else if ("ring.effect".equalsIgnoreCase(action)) {
                // Cycle through LED ring effects
                log.info("Ring effect: " + mRingEffect);
                mCrazyflie.setParamValue(action, mRingEffect);
                mRingEffect++;
                mRingEffect = (mRingEffect > mNoRingEffect) ? 0 : mRingEffect;
            } else if (action.startsWith("sound.effect")) {
                // Toggle buzzer deck sound effect
                String[] split = action.split(":");
                log.info("Sound effect: " + split[1]);
                mCrazyflie.setParamValue(split[0], mSoundToggle ? Integer.parseInt(split[1]) : 0);
                mSoundToggle = !mSoundToggle;
            }
        } else {
            log.debug("runAltAction - crazyflie is null");
        }
    }

    public Crazyflie getCrazyflie() {
        return mCrazyflie;
    }

    private LogAdapter standardLogAdapter = new LogAdapter() {

        public void logDataReceived(LogConfig logConfig, Map<String, Number> data, int timestamp) {
            super.logDataReceived(logConfig, data, timestamp);

            if ("Standard".equals(logConfig.getName())) {
                final float battery = (float) data.get("pm.vbat");
                log.info("setBatteryLevel(battery); NOT IMPLEMENTED");
            }
            for (Map.Entry<String, Number> entry : data.entrySet()) {
                log.debug("\t Name: " + entry.getKey() + ", data: " + entry.getValue());
            }
        }

    };

    private LogConfig createDefaultLogConfig() {
        LogConfig logConfigStandard = new LogConfig("Standard", 1000);
        logConfigStandard.addVariable("pm.vbat", VariableType.FLOAT);
        return logConfigStandard;
    }

    /**
     * Start logging config
     */
    private void startLogConfigs(LogConfig logConfig) {
        if (mLogg == null) {
            log.error("startLogConfigs: mLogg was null!!");
            return;
        }
        if (logConfig == null) {
            log.error("startLogConfigs: Logg was null!!");
            return;
        }
        mLogg.addLogListener(standardLogAdapter);
        mLogg.addConfig(logConfig);
        mLogg.start(logConfig);
    }

    /**
     * Stop logging config
     */
    private void stopLogConfigs(LogConfig logConfig) {
        if (mLogg == null) {
            log.error("stopLogConfigs: mLogg was null!!");
            return;
        }
        if (logConfig == null) {
            log.error("stopLogConfigs: Logg was null!!");
            return;
        }
        mLogg.stop(logConfig);
        mLogg.delete(logConfig);
        mLogg.removeLogListener(standardLogAdapter);
    }
}
