package se.bitcraze.crazyfliecontrol.controller;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JoystickView implements IJoystickView {
    public static final int INVALID_POINTER_ID = -1;
    private static final String TAG = "JoystickView";

    private boolean isLeft = true;

    private int innerPadding;
    private int bgRadius;
    private int handleRadius;
    private int movementRadius;
    private int handleInnerBoundaries;

    private JoystickMovedListener moveListener;

    // # of pixels movement required between reporting to the listener
    private float moveResolution;

    public final static int AUTO_RETURN_NONE = 0;
    public final static int AUTO_RETURN_CENTER = 1;
    public final static int AUTO_RETURN_BOTTOM = 2;
    private int autoReturnMode;
    private volatile int autoReturnSequenceNum;

    // Max range of movement in user coordinate system
    public final static int CONSTRAIN_BOX = 0;
    public final static int CONSTRAIN_CIRCLE = 1;
    private int movementConstraint;
    private float movementRange;

    public final static int COORDINATE_CARTESIAN = 0; // Regular cartesian coordinates
    public final static int COORDINATE_DIFFERENTIAL = 1; // Uses polar rotation of 45 degrees to calc differential drive parameters
    private int userCoordinateSystem;

    private float prefRatio = 1;

    // Last touch point in view coordinates
    private int pointerId = INVALID_POINTER_ID;
    private float touchX, touchY;

    // Last reported position in view coordinates (allows different reporting sensitivities)
    private float reportX, reportY;

    // Handle center in view coordinates
    private float handleX, handleY;

    // Center of the view in view coordinates
    private int circleCenterX, circleCenterY;

    // Size of the view in view coordinates
    private int dimX, dimY;

    // Cartesian coordinates of last touch point - joystick center is (0,0)
    private int cartX, cartY;

    // Polar coordinates of the touch point from joystick center
    private double radial;
    private double angle;

    // User coordinates of last touch point
    private float userX, userY;

    // Offset co-ordinates (used when touch events are received from parent's coordinate origin)
    private int offsetX;
    private int offsetY;

    private boolean verticalLocked = false;
    private boolean horizontalLocked = false;

    // =========================================
    // Constructors
    // =========================================

    public JoystickView() {
        initJoystickView();
    }

    // =========================================
    // Initialization
    // =========================================

    private void initJoystickView() {
        innerPadding = 10;

        movementRadius = 100;
        setMovementRange(10);
        setMoveResolution(1.0f);
        setUserCoordinateSystem(COORDINATE_CARTESIAN);
        setAutoReturnToCenter(true);
    }

    public void setAutoReturnToCenter(boolean autoReturnToCenter) {
        this.autoReturnMode = autoReturnToCenter ? AUTO_RETURN_CENTER : AUTO_RETURN_NONE;
    }

    public void setAutoReturnMode(int autoReturnMode) {
        this.autoReturnMode = autoReturnMode;
    }

    private void setUserCoordinateSystem(int userCoordinateSystem) {
        if (userCoordinateSystem < COORDINATE_CARTESIAN || movementConstraint > COORDINATE_DIFFERENTIAL) {
            log.error("invalid value for userCoordinateSystem");
        } else {
            this.userCoordinateSystem = userCoordinateSystem;
        }
    }

    public void setMovementConstraint(int movementConstraint) {
        if (movementConstraint < CONSTRAIN_BOX || movementConstraint > CONSTRAIN_CIRCLE) {
            log.error("invalid value for movementConstraint");
        } else {
            this.movementConstraint = movementConstraint;
        }
    }

    public void setMovementRange(float movementRange) {
        this.movementRange = movementRange;
    }

    private void setMoveResolution(float moveResolution) {
        this.moveResolution = moveResolution;
    }

    public void setOnJoystickMovedListener(JoystickMovedListener listener) {
        this.moveListener = listener;
    }


    public void setPreferences() {
        prefRatio = Float.parseFloat(System.getProperty("KEY_PREF_JOYSTICK_SIZE", "100"));
        prefRatio /= 100.0;
    }

    // Constrain touch within a box
    private void constrainBox() {
        touchX = Math.max(Math.min(touchX, movementRadius), -movementRadius);
        touchY = Math.max(Math.min(touchY, movementRadius), -movementRadius);
    }

    // Constrain touch within a circle
    private void constrainCircle() {
        float diffX = touchX;
        float diffY = touchY;
        double radial = Math.sqrt((diffX * diffX) + (diffY * diffY));
        if (radial > movementRadius) {
            touchX = (int) ((diffX / radial) * movementRadius);
            touchY = (int) ((diffY / radial) * movementRadius);
        }
    }


    private void reportOnMoved() {
        if (movementConstraint == CONSTRAIN_CIRCLE) {
            constrainCircle();
        } else {
            constrainBox();
        }
        calcUserCoordinates();
        if (moveListener != null) {
            boolean rx = Math.abs(touchX - reportX) >= moveResolution;
            boolean ry = Math.abs(touchY - reportY) >= moveResolution;
            if (rx || ry) {
                this.reportX = touchX;
                this.reportY = touchY;

                // log.debug(String.format("moveListener.OnMoved(%d,%d)", (int)userX, (int)userY));
                moveListener.OnMoved(userX, userY);
            }
        }
    }

    private void calcUserCoordinates() {
        // First convert to cartesian coordinates
        cartX = (int) (touchX / movementRadius * movementRange);
        cartY = (int) (touchY / movementRadius * movementRange);

        radial = Math.sqrt((cartX * cartX) + (cartY * cartY));
        angle = Math.atan2(cartY, cartX);

        // Invert Y axis by default
        cartY *= -1;

        if (userCoordinateSystem == COORDINATE_CARTESIAN) {
            userX = cartX / movementRange;
            userY = cartY / movementRange;
        } else if (userCoordinateSystem == COORDINATE_DIFFERENTIAL) {
            userX = cartY + cartX / 4;
            userY = cartY - cartX / 4;

            if (userX < -movementRange) {
                userX = (int) -movementRange;
            }
            if (userX > movementRange) {
                userX = (int) movementRange;
            }
            if (userY < -movementRange) {
                userY = (int) -movementRange;
            }
            if (userY > movementRange) {
                userY = (int) movementRange;
            }
        }

    }

    public void autoReturn(boolean immediate) {
        if (autoReturnMode != AUTO_RETURN_NONE) {
            final int numberOfFrames = immediate ? 1 : 5;
            final double intervalsX = (0 - touchX) / numberOfFrames;
            final double returnY = autoReturnMode == AUTO_RETURN_BOTTOM ? movementRadius : 0;
            final double intervalsY = (returnY - touchY) / numberOfFrames;

            ++autoReturnSequenceNum;
            final int thisAutoReturnSequence = autoReturnSequenceNum;

            for (int i = 0; i < numberOfFrames; i++) {
                final int j = i;
                withDelay(new Runnable() {
                    public void run() {
                        if (thisAutoReturnSequence != autoReturnSequenceNum) {
                            return;
                        }

                        touchX += intervalsX;
                        touchY += intervalsY;

                        reportOnMoved();

                        if (moveListener != null && j == numberOfFrames - 1) {
                            moveListener.OnReturnedToCenter();
                        }
                    }
                }, i * 40);
            }

            if (moveListener != null) {
                moveListener.OnReleased();
            }
        }
    }


    public boolean processMoveEvent(float x, float y ) {
        // Translate touch position to center of view
        touchX = x;
        touchY = y;

        // Log.d(TAG, String.format("ACTION_MOVE: (%03.0f, %03.0f) => (%03.0f, %03.0f)", x, y, touchX, touchY));
        reportOnMoved();
        return true;
    }

    private void withDelay(Runnable runnable, int i) {
        log.info("Run task with delay NOT IMPLEMENTED");
    }

    private void setTouchOffsetX(int x) {
        offsetX = x;
    }

    private void setTouchOffsetY(int y) {
        offsetY = y;
    }

    public boolean isLeft() {
        return isLeft;
    }

    public void setLeft(boolean left) {
        isLeft = left;
    }

    public void setVerticalLocked(boolean locked) {
        this.verticalLocked = locked;
    }

    public boolean isVerticalLocked() {
        return verticalLocked;
    }

    public void setHorizontalLocked(boolean locked) {
        this.horizontalLocked = locked;
    }

    public boolean isHorizontalLocked() {
        return horizontalLocked;
    }
}
