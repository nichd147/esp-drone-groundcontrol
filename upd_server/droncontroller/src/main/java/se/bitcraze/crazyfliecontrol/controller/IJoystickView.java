package se.bitcraze.crazyfliecontrol.controller;

public interface IJoystickView {
    void setMovementRange(float movementRange);

    void setAutoReturnMode(int i);

    void autoReturn(boolean b);

    void setOnJoystickMovedListener(JoystickMovedListener listenerLeft);

    boolean processMoveEvent(float x, float y );
}
