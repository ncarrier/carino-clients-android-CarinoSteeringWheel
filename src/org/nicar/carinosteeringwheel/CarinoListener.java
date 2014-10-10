package org.nicar.carinosteeringwheel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

public class CarinoListener extends java.lang.Thread {
	private static final String TAG = SteeringWheel.TAG;
	private Selector selector;
	private SocketChannel socketChannel;
	private InetSocketAddress address;
	private ConcurrentLinkedQueue<ByteBuffer> messageQueue;
	ByteBuffer readBuffer = ByteBuffer.allocate(0x100);
	ByteBuffer writeBuffer = null;
	private final int RO = SelectionKey.OP_READ;
	private final int RW = SelectionKey.OP_WRITE | RO;

	public CarinoListener() {
		super();

		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		address = new InetSocketAddress("192.168.44.161", 28259);
		messageQueue = new ConcurrentLinkedQueue<ByteBuffer>();
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

		try {
			socketChannel.configureBlocking(false);
			socketKey = socketChannel.register(selector, RO);
			while (true) {
				nbEvents = selector.select();
				if (nbEvents == 0 && !messageQueue.isEmpty())
						socketChannel.register(selector, RW);
				if (selector.selectedKeys().contains(socketKey)) {
					if (socketKey.isReadable()) {
						readBuffer.clear();
						read = socketChannel.read(readBuffer);
						if (read == -1)
							break;
						readBuffer.flip();
						while (readBuffer.hasRemaining())
							Log.e(TAG, Byte.toString(readBuffer.get()));
						// TODO consume the data, ie, create an object
						// containing the interpreted data from
						// get(byte[] dst)
					}
					if (socketKey.isWritable()) {
						while (!messageQueue.isEmpty()) {
							writeBuffer = messageQueue.peek();
							socketChannel.write(writeBuffer);
							if (writeBuffer.hasRemaining())
								break;

							messageQueue.remove(writeBuffer);
						}
						if (messageQueue.isEmpty())
							socketChannel.register(selector, RO);
					}
					selector.selectedKeys().remove(socketKey);
				}
			}
		} catch (Exception e1) {
			/* nothing to do, just close and try to start again */
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
			/* drop the old messages which could be in the queue */
			messageQueue.clear();
			select();
			Log.d(TAG, "connection lost :(");
		}
	}

	public void postMessage(byte msg[]) {
		if (this.socketChannel.isConnected()) {
			ByteBuffer bb = ByteBuffer.wrap(msg);
			messageQueue.add(bb);
			selector.wakeup();
		}
	}
}
