/**
 * ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 * ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 * <p>
 * Copyright (C) 2013 Bitcraze AB
 * <p>
 * Crazyflie Nano Quadcopter Client
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package se.bitcraze.crazyfliecontrol.controller;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * This class encapsulates the common preferences for all types of controllers.
 * <p>
 * TODO: rename Class
 */

@Slf4j
public class Controls {

    // Raw input values
    private float mRight_analog_x;
    private float mRight_analog_y;
    private float mLeft_analog_x;
    private float mLeft_analog_y;

    // Trim values
    private static final float TRIM_MAX = 0.5f;
    private static final float TRIM_INCREMENTS = 0.02f;
    private float mRollTrim;
    private float mPitchTrim;
    private String mTrimDefaultValue;

    private int mMode; // Controller axis mapping (Mode 1-4)
    private float mDeadzone;

    private int mControllerType;
    private String mControllerTypeDefaultValue;

    private String mAlt1ActionDefaultValue;
    private String mAlt2ActionDefaultValue;
    private String mAlt1Action;
    private String mAlt2Action;

    //Gyro sensor values
    private boolean mUseGyro;
    private int mGyroAmplification;
    private String mGyroAmplificationDefaultValue;
    private boolean mTouchThrustFullTravel;

    private String mModeDefaultValue;
    private String mDeadzoneDefaultValue;

    private boolean mXmode; // determines Crazyflie flight configuration (false = +, true = x)

    //Advanced flight control
    private int mMaxRollPitchAngle;
    private int mMaxYawAngle;
    private int mMaxThrust;
    private int mMinThrust;

    //Advanced flight control default values
    private String mMaxRollPitchAngleDefaultValue;
    private String mMaxYawAngleDefaultValue;
    private String mMaxThrustDefaultValue;
    private String mMinThrustDefaultValue;

    public Controls() {
    }

    public void setDefaultPreferenceValues() {
        mModeDefaultValue = "2";
        mDeadzoneDefaultValue = "0.2";

        mTrimDefaultValue = "0.0";

        mGyroAmplificationDefaultValue = "2";

        mControllerTypeDefaultValue = "0";

        mAlt1ActionDefaultValue = "ring.headlightEnable";
        mAlt2ActionDefaultValue = "ring.effect";

        //Advanced flight control
        mMaxRollPitchAngleDefaultValue = "15";
        mMaxYawAngleDefaultValue = "90";
        mMaxThrustDefaultValue = "90";
        mMinThrustDefaultValue = "25";
    }

    public void setControlConfig() {
        this.mMode = Integer.parseInt(System.getProperty("KEY_PREF_MODE", mModeDefaultValue));
        this.mDeadzone = Float.parseFloat(System.getProperty("KEY_PREF_DEADZONE", mDeadzoneDefaultValue));

        this.mRollTrim = Float.parseFloat(System.getProperty("KEY_PREF_ROLLTRIM", mTrimDefaultValue));
        this.mPitchTrim = Float.parseFloat(System.getProperty("KEY_PREF_PITCHTRIM", mTrimDefaultValue));

        this.mControllerType = Integer.parseInt(System.getProperty("KEY_PREF_CONTROLLER", mControllerTypeDefaultValue));

        this.mUseGyro = Boolean.parseBoolean(System.getProperty("KEY_PREF_USE_GYRO_BOOL", "false"));
        this.mGyroAmplification = Integer.parseInt(System.getProperty("KEY_PREF_GYRO_AMP", mGyroAmplificationDefaultValue));

        this.mTouchThrustFullTravel = Boolean.parseBoolean(System.getProperty("KEY_PREF_TOUCH_THRUST_FULL_TRAVEL", "true"));

        this.mAlt1Action = System.getProperty("KEY_PREF_ALT1_ACTION", mAlt1ActionDefaultValue);
        this.mAlt2Action = System.getProperty("KEY_PREF_ALT2_ACTION", mAlt2ActionDefaultValue);

        //Advanced flight control
        if (Boolean.parseBoolean(System.getProperty("KEY_PREF_AFC_BOOL", "false"))) {
            this.mMaxRollPitchAngle = Integer.parseInt(System.getProperty("KEY_PREF_MAX_ROLLPITCH_ANGLE", mMaxRollPitchAngleDefaultValue));
            this.mMaxYawAngle = Integer.parseInt(System.getProperty("KEY_PREF_MAX_YAW_ANGLE", mMaxYawAngleDefaultValue));
            this.mMaxThrust = Integer.parseInt(System.getProperty("KEY_PREF_MAX_THRUST", mMaxThrustDefaultValue));
            this.mMinThrust = Integer.parseInt(System.getProperty("KEY_PREF_MIN_THRUST", mMinThrustDefaultValue));
            this.mXmode = Boolean.parseBoolean(System.getProperty("KEY_PREF_XMODE", "false"));
        } else {
            this.mMaxRollPitchAngle = Integer.parseInt(mMaxRollPitchAngleDefaultValue);
            this.mMaxYawAngle = Integer.parseInt(mMaxYawAngleDefaultValue);
            this.mMaxThrust = Integer.parseInt(mMaxThrustDefaultValue);
            this.mMinThrust = Integer.parseInt(mMinThrustDefaultValue);
            this.mXmode = false;
        }
    }

    public float getRollTrim() {
        return mRollTrim;
    }

    public float getPitchTrim() {
        return mPitchTrim;
    }

    public void increaseTrim(String prefKey) {
        changeTrim(prefKey, true);
    }

    public void decreaseTrim(String prefKey) {
        changeTrim(prefKey, false);
    }

    public static final String KEY_PREF_ROLLTRIM = "pref_rolltrim";

    private void changeTrim(String prefKey, boolean increase) {
        float axis;
        String axisName;
        if (KEY_PREF_ROLLTRIM.equals(prefKey)) {
            axisName = "Roll";
            axis = mRollTrim;
        } else {
            axisName = "Pitch";
            axis = mPitchTrim;
        }

        if (increase && axis < TRIM_MAX) {
            axis += TRIM_INCREMENTS;
        } else if (!increase && axis > (TRIM_MAX * -1)) {
            axis -= TRIM_INCREMENTS;
        }

        setPreference(prefKey, String.valueOf(axis));
        log.info(axisName + " Trim: " + round(axis));

        if (KEY_PREF_ROLLTRIM.equals(prefKey)) {
            mRollTrim = axis;
        } else {
            mPitchTrim = axis;
        }
    }

    public static double round(double unrounded) {
        try {
            BigDecimal bd = new BigDecimal(unrounded);
            BigDecimal rounded = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            return rounded.doubleValue();
        } catch (NumberFormatException nfe) {
            log.error("unrounded: " + unrounded + ", NumberFormatException: " + nfe);
            return Double.NaN;
        }
    }

    private void setPreference(String pKey, String pDefaultValue) {
        log.info("setting pref {}: {} NOT IMPLEMENTED", pKey, pDefaultValue);
    }

    public void resetAxisValues() {
        mRight_analog_y = 0;
        mRight_analog_x = 0;
        mLeft_analog_y = 0;
        mLeft_analog_x = 0;
    }

    public int getMode() {
        return mMode;
    }

    public float getDeadzone(float axis) {
        if (axis < mDeadzone && axis > mDeadzone * -1) {
            return 0;
        }
        return 1;
    }

    public float getDeadzone() {
        return mDeadzone;
    }

    public int getControllerType() {
        return mControllerType;
    }

    public String getAlt1Action() {
        return mAlt1Action;
    }

    public String getAlt2Action() {
        return mAlt2Action;
    }

    public boolean isUseGyro() {
        return mUseGyro;
    }

    public int getGyroAmplification() {
        return mGyroAmplification;
    }

    public boolean isTouchThrustFullTravel() {
        return mTouchThrustFullTravel;
    }

    public float getRightAnalog_X() {
        return mRight_analog_x;
    }

    public float getRightAnalog_Y() {
        return mRight_analog_y;
    }

    public float getLeftAnalog_X() {
        return mLeft_analog_x;
    }

    public float getLeftAnalog_Y() {
        return mLeft_analog_y;
    }

    // TODO: move methods to AbstractController?

    public void setRightAnalogX(float right_analog_x) {
        this.mRight_analog_x = right_analog_x;
    }

    public void setRightAnalogY(float right_analog_y) {
        this.mRight_analog_y = right_analog_y;
    }

    public void setLeftAnalogX(float left_analog_x) {
        this.mLeft_analog_x = left_analog_x;
    }

    public void setLeftAnalogY(float left_analog_y) {
        this.mLeft_analog_y = left_analog_y;
    }

    public boolean isXmode() {
        return this.mXmode;
    }

    public int getMaxRollPitchAngle() {
        return mMaxRollPitchAngle;
    }

    public int getMaxYawAngle() {
        return mMaxYawAngle;
    }

    public int getMaxThrust() {
        return mMaxThrust;
    }

    public int getMinThrust() {
        return mMinThrust;
    }

    // TODO: move methods to Controls?
    public float getRollPitchFactor() {
        return getMaxRollPitchAngle();
    }

    public float getYawFactor() {
        return getMaxYawAngle();
    }

    public float getThrustFactor() {
        int addThrust = 0;
        if ((getMaxThrust() - getMinThrust()) < 0) {
            addThrust = 0; // do not allow negative values
        } else {
            addThrust = (getMaxThrust() - getMinThrust());
        }
        return addThrust;
    }
}
