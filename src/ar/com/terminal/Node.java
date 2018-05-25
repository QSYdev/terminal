package ar.com.terminal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

class Node implements AutoCloseable {

	private final int physicalId;
	private final InetAddress nodeAddress;
	private final SocketChannel nodeSocketChannel;

	public Node(QSYPacket qsyPacket) throws IOException, IllegalArgumentException {
		if (qsyPacket.getType() == QSYPacket.PacketType.Hello) {
			InetSocketAddress hostAddress = new InetSocketAddress(qsyPacket.getNodeAddress().getHostAddress(), QSYPacket.TCP_PORT);
			SocketChannel nodeSocketChannel = SocketChannel.open(hostAddress);
			nodeSocketChannel.socket().setTcpNoDelay(true);
			nodeSocketChannel.configureBlocking(false);
			this.physicalId = qsyPacket.getPhysicalId();
			this.nodeAddress = qsyPacket.getNodeAddress();
			this.nodeSocketChannel = nodeSocketChannel;
		} else {
			throw new IllegalArgumentException("El QSYPacket recibido no es un QSYHelloPacket.");
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
