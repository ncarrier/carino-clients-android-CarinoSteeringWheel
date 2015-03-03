package org.nicar.carinosteeringwheel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import android.util.Log;

public class CarinoListener extends java.lang.Thread {
	private static final String TAG = SteeringWheel.TAG;
	private Selector selector;
	private SocketChannel socketChannel;
	private InetSocketAddress address;
	ByteBuffer readBuffer = ByteBuffer.allocate(0x100);
	ByteBuffer writeBuffer = null;
	private final int RO = SelectionKey.OP_READ;
	private final int RW = SelectionKey.OP_WRITE | RO;
	private Command command = new Command();

	public CarinoListener() {
		super();

		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		// address = new InetSocketAddress("192.168.44.161", 28259);
		address = new InetSocketAddress("10.10.10.1", 28259);
		Log.e(TAG, "address used is " + address.toString());
	}

	private void connect() {
		while (true) {
			if (socketChannel.isConnected())
				return;

			try {
				if (!socketChannel.isOpen())
					socketChannel = SocketChannel.open();

				if (!socketChannel.isConnectionPending()) {
					socketChannel.connect(address);
					if (socketChannel.finishConnect())
						return;
				} else {
					if (socketChannel.finishConnect())
						return;
				}
			} catch (Exception e) {
				/* nothing to do, just retry */
			}

			Log.d(TAG, "connection failed, retry in one second");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	private void select() {
		SelectionKey socketKey;
		int nbEvents;
		int read;

		readBuffer.clear();

		try {
			socketChannel.configureBlocking(false);
			socketKey = socketChannel.register(selector, RO);
			while (true) {
				nbEvents = selector.select();
				synchronized (command) {
					if (nbEvents == 0 && command.isDirty())
						socketChannel.register(selector, RW);
				}
				if (selector.selectedKeys().contains(socketKey)) {
					if (socketKey.isReadable()) {
						read = socketChannel.read(readBuffer);
						if (read == -1)
							break;
						if (read == 0) {
							readBuffer.clear();
						} else if (readBuffer.get(readBuffer.position() - 1) == '\n') {
							byte dst[] = new byte[1024];
							readBuffer.flip();
							readBuffer.get(dst, 0, readBuffer.remaining());
							Log.e(TAG,
									new String(dst, 0, readBuffer.remaining()));
							readBuffer.clear();
						}
					}
					if (socketKey.isWritable()) {
						synchronized (command) {
							writeBuffer = ByteBuffer.wrap(command.prepare());
							socketChannel.write(writeBuffer);
							socketChannel.register(selector, RO);
						}
					}
					selector.selectedKeys().remove(socketKey);
				}
			}
		} catch (Exception e1) {
			/* nothing to do, just close and try to start again */
			e1.printStackTrace();
		}
		try {
			socketChannel.close();
		} catch (IOException e) {
			/* nothing interesting to do here, IMHO */
		}
	}

	@Override
	public void run() {
		while (true) {
			connect();
			Log.d(TAG, "connected !");
			select();
			Log.d(TAG, "connection lost :(");
		}
	}

	public void setDirectionAndSpeed(float direction, float speed) {
		synchronized (command) {
			if (this.socketChannel.isConnected()) {
				command.setMotorsSpeed(speed);
				command.setDirection(direction);
				selector.wakeup();
			}
		}
	}
}
