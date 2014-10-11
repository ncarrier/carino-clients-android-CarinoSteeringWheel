package org.nicar.carinosteeringwheel;

import java.nio.ByteBuffer;

public class Command {
	private final static int CAMERA_ANGLE_MIN = 0;
	private final static int CAMERA_ANGLE_MAX = 180;

	private final static int START = 0x55;

	private static final int IDX_START = 0;
	private static final int IDX_LMS_HIGH = 1;
	private static final int IDX_LMS_LOW = 2;
	private static final int IDX_RMS_HIGH = 3;
	private static final int IDX_RMS_LOW = 4;
	private static final int IDX_DIRECTION = 5;
	private static final int IDX_CAMERA_X_ANGLE = 6;
	private static final int IDX_CAMERA_Y_ANGLE = 7;
	private static final int IDX_BEEP = 8;
	private static final int IDX_CHECKSUM = 9;

	private int mLeftMotorSpeed;
	private int mRightMotorSpeed;
	private byte mServoAngle;
	private byte mCameraXAngle;
	private byte mCameraZAngle;
	private boolean mBeep;

	private boolean mDirty;

	static byte checksum(byte packet[]) {
		int len = packet.length - 1;
		byte checksum = 0;

		while (len-- != 0)
			checksum ^= packet[len];

		return checksum;
	}

	public Command() {
		mDirty = true;

		mLeftMotorSpeed = 0;
		mRightMotorSpeed = 0;
		mServoAngle = 0;
		mCameraXAngle = 0;
		mCameraZAngle = 0;
		mBeep = false;
	}

	public void updateMotorsSpeed(int commonSpeed) {
		if (commonSpeed > 255)
			commonSpeed = 255;
		if (commonSpeed < -255)
			commonSpeed = -255;

		if (mLeftMotorSpeed != commonSpeed || mRightMotorSpeed != commonSpeed)
			mDirty = true;

		mLeftMotorSpeed = mRightMotorSpeed = commonSpeed;
	}

	public boolean isDirty() {
		return mDirty;
	}

	public void enableBeep() {
		if (!mBeep)
			mDirty = true;

		mBeep = true;
	}

	public void disableBeep() {
		if (!mBeep)
			mDirty = false;

		mBeep = false;
	}

	public void toggleBeep() {
		mDirty = true;

		mBeep = !mBeep;
	}

	public void setDirection(byte direction) {
		if (direction != mServoAngle)
			mDirty = true;

		mServoAngle = direction;
	}

	public void setCameraXAngle(byte cameraXAngle) {
		if (cameraXAngle != mCameraXAngle)
			mDirty = true;

		mCameraXAngle = cameraXAngle;
	}

	public void setCameraZAngle(byte cameraZAngle) {
		if (cameraZAngle != mCameraZAngle)
			mDirty = true;

		mCameraZAngle = cameraZAngle;
	}

	public ByteBuffer prepare() {
		byte packet[] = new byte[10];

		packet[IDX_START] = START;
		// TODO should not work with negative values
		packet[IDX_LMS_HIGH] = (byte) ((mLeftMotorSpeed & 0xFF00) >> 8);
		packet[IDX_LMS_LOW] = (byte) (mLeftMotorSpeed & 0xFF);
		packet[IDX_RMS_HIGH] = (byte) ((mRightMotorSpeed & 0xFF00) >> 8);
		packet[IDX_RMS_LOW] = (byte) (mRightMotorSpeed & 0xFF);

		packet[IDX_DIRECTION] = mServoAngle;
		packet[IDX_CAMERA_X_ANGLE] = mCameraXAngle;
		packet[IDX_CAMERA_Y_ANGLE] = mCameraZAngle;

		packet[IDX_BEEP] = (byte) (mBeep ? 1 : 0);
		packet[IDX_CHECKSUM] = checksum(packet);

		mDirty = false;

		return ByteBuffer.wrap(packet);
	}
}
