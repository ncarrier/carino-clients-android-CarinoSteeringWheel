package org.nicar.carinosteeringwheel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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

	public CarinoListener() {
		super();

		try {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		address = new InetSocketAddress("192.168.44.161", 28259);
		messageQueue = new ConcurrentLinkedQueue<ByteBuffer>();
	}

	@Override
	public void run() {
		SelectionKey socketKey;
		int operations;
		ByteBuffer readBuffer = ByteBuffer.allocate(0x100);
		ByteBuffer writeBuffer = null;
		int nbEvents;

		try {
			operations = SelectionKey.OP_READ;
			if (!socketChannel.connect(address)) {
				operations |= SelectionKey.OP_CONNECT;
			} else {
				socketChannel.finishConnect();
			}
			socketKey = socketChannel.register(selector, operations);
			while (true) {
				nbEvents = selector.select();
				if (nbEvents == 0) {
					if (!messageQueue.isEmpty())
						socketChannel.register(selector, SelectionKey.OP_WRITE
								| SelectionKey.OP_READ);
				}
				if (selector.selectedKeys().contains(socketKey)) {
					if (socketKey.isConnectable()) {
						socketChannel.finishConnect();
						Log.d(TAG, "socket connected");
						socketChannel.register(selector, SelectionKey.OP_READ);
					}
					if (socketKey.isReadable()) {
						readBuffer.clear();
						socketChannel.read(readBuffer);
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
							// msg.flip();
							if (writeBuffer.hasRemaining())
								break;

							messageQueue.remove(writeBuffer);
						}
						if (messageQueue.isEmpty())
							socketChannel.register(selector, SelectionKey.OP_READ);
					}
					selector.selectedKeys().remove(socketKey);
				}
			}
		} catch (ClosedChannelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
