package ar.com.terminal.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import ar.com.terminal.shared.QSYPacket;

class Node implements AutoCloseable {

	private final int physicalId;
	private final InetAddress nodeAddress;
	private final SocketChannel nodeSocketChannel;

	public Node(final QSYPacket qsyPacket) throws IOException {
		if (qsyPacket.getType() == QSYPacket.PacketType.Hello) {
			final InetSocketAddress hostAddress = new InetSocketAddress(qsyPacket.getNodeAddress().getHostAddress(), QSYPacket.TCP_PORT);
			final SocketChannel nodeSocketChannel = SocketChannel.open(hostAddress);
			nodeSocketChannel.socket().setTcpNoDelay(true);
			nodeSocketChannel.configureBlocking(false);
			this.physicalId = qsyPacket.getPhysicalId();
			this.nodeAddress = qsyPacket.getNodeAddress();
			this.nodeSocketChannel = nodeSocketChannel;
		} else {
			throw new IllegalArgumentException("<< NODE >> El QSYPacket recibido no es un QSYHelloPacket.");
		}
	}

	public int getPhysicalId() {
		return physicalId;
	}

	public InetAddress getNodeAddress() {
		return nodeAddress;
	}

	public SocketChannel getNodeSocketChannel() {
		return nodeSocketChannel;
	}

	@Override
	public void close() throws IOException {
		nodeSocketChannel.close();
	}

}
