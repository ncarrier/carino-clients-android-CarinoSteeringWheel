package org.nicar.carinosteeringwheel;


public class Command {
	private static final String TAG = SteeringWheel.TAG;
	private final static int CAMERA_ANGLE_MIN = 0;
	private final static int CAMERA_ANGLE_MAX = 180;
	private final static int DIRECTION_ANGLE_MIN = 50;
	private final static int DIRECTION_ANGLE_MAX = 140;

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
	private int mServoAngle;
	private int mCameraXAngle;
	private int mCameraZAngle;
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
		mCameraXAngle = 90;
		mCameraZAngle = 90;
		mBeep = false;
	}

	/* takes a float in [-1, 1] and put int the [a, b] integer range */
	private int putInRange(float value, int a, int b) {
		float spread = (b - a) / 2.f;

		if (spread < 0)
			throw new IllegalArgumentException();

		value = (value + 1.f) * spread + a;

		return (int) value;
	}

	public void setMotorsSpeed(float speed) {
		int intSpeed;

		if (speed > 1.f)
			speed = 1.f;
		if (speed < -1.f)
			speed = -1.f;

		intSpeed = putInRange(speed, -255, 255);

		if (mLeftMotorSpeed != intSpeed || mRightMotorSpeed != intSpeed)
			mDirty = true;

		mLeftMotorSpeed = mRightMotorSpeed = intSpeed;
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

	public void setDirection(float direction) {
		int intDirection;

		if (direction > 1.f)
			direction = 1.f;
		if (direction < -1.f)
			direction = -1.f;

		intDirection = putInRange(direction, DIRECTION_ANGLE_MIN,
				DIRECTION_ANGLE_MAX);

		if (intDirection != mServoAngle)
			mDirty = true;

		mServoAngle = intDirection;
	}

	public void setCameraXAngle(int cameraXAngle) {
		/* TODO maybe to rework when really used, see setDirection */
		if (cameraXAngle > CAMERA_ANGLE_MIN)
			cameraXAngle = CAMERA_ANGLE_MIN;
		if (cameraXAngle > CAMERA_ANGLE_MAX)
			cameraXAngle = CAMERA_ANGLE_MAX;
		if (cameraXAngle != mCameraXAngle)
			mDirty = true;

		mCameraXAngle = cameraXAngle;
	}

	public void setCameraZAngle(byte cameraZAngle) {
		if (cameraZAngle != mCameraZAngle)
			mDirty = true;

		mCameraZAngle = cameraZAngle;
	}

	public byte[] prepare() {
		byte packet[] = new byte[10];

		packet[IDX_START] = START;
		// TODO should not work with negative values
		packet[IDX_LMS_LOW] = (byte) ((mLeftMotorSpeed & 0xFF00) >> 8);
		packet[IDX_LMS_HIGH] = (byte) (mLeftMotorSpeed & 0xFF);
		packet[IDX_RMS_LOW] = (byte) ((mRightMotorSpeed & 0xFF00) >> 8);
		packet[IDX_RMS_HIGH] = (byte) (mRightMotorSpeed & 0xFF);

		packet[IDX_DIRECTION] = (byte) mServoAngle;
		packet[IDX_CAMERA_X_ANGLE] = (byte) mCameraXAngle;
		packet[IDX_CAMERA_Y_ANGLE] = (byte) mCameraZAngle;

		packet[IDX_BEEP] = (byte) (mBeep ? 1 : 0);
		packet[IDX_CHECKSUM] = checksum(packet);

		mDirty = false;

		return packet;
	}
}
