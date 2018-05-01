package ar.com.test;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import ar.com.terminal.Color;
import ar.com.terminal.QSYPacket;
import ar.com.terminal.Sender;
import ar.com.terminal.QSYPacket.CommandArgs;

public class SenderTest {

	private final AtomicInteger i;

	private final Sender sender;
	private final Thread server;
	private final Thread input;

	public SenderTest() {
		this.i = new AtomicInteger(0);

		this.sender = new Sender();
		this.server = new Thread(new ServerTask(), "Server");
		this.input = new Thread(new InputTask(), "Input");

		this.input.start();
		this.server.start();

	}

	private final class InputTask implements Runnable {

		private final Scanner scanner = new Scanner(System.in);

		@Override
		public void run() {
			char command = 0;
			int indx = 0;
			do {
				command = scanner.nextLine().charAt(0);
				switch (command) {
				case 's':
					for (int index = 0; index < i.get(); index++) {
						final CommandArgs params = new CommandArgs(index, Color.CYAN, 500, 1, false, false);
						sender.command(QSYPacket.createCommandPacket(params));
					}
					break;
				case 'a':
					try {
						new Thread(new ClientTask(), "Client " + indx++).start();
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			} while (command != 'q');
			scanner.close();

			server.interrupt();
			try {
				server.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			try {
				sender.close();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private final class ServerTask implements Runnable {

		private boolean running = true;

		@Override
		public void run() {
			try {
				final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
				serverSocketChannel.socket().bind(new InetSocketAddress(Inet4Address.getByName("192.168.1.106"), QSYPacket.TCP_PORT));
				while (running) {
					final SocketChannel channel = serverSocketChannel.accept();
					channel.socket().setTcpNoDelay(true);
					channel.configureBlocking(false);
					sender.newNode(i.getAndIncrement(), channel);
				}
			} catch (final IOException e) {
				running = false;
				e.printStackTrace();
			}
		}
	}

	private static final class ClientTask implements Runnable {

		private static int count = 0;

		private final int id = count++;
		private final SocketChannel channel;
		private final ByteBuffer byteBuffer;
		private final byte[] data;

		private boolean running = true;

		public ClientTask() throws IOException {
			byteBuffer = ByteBuffer.allocate(QSYPacket.PACKET_SIZE);
			data = new byte[QSYPacket.PACKET_SIZE];
			channel = SocketChannel.open();
			// channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(Inet4Address.getByName("192.168.1.106"), QSYPacket.TCP_PORT));
		}

		@Override
		public void run() {
			while (running) {
				try {
					if (channel.read(byteBuffer) == QSYPacket.PACKET_SIZE) {
						byteBuffer.flip();
						byteBuffer.get(data);
						// System.out.println(new QSYPacket(channel.socket().getInetAddress(), data));
						System.out.println("El nodo id = " + id + " ha recibido un paquete");
						byteBuffer.clear();
					}
				} catch (final IOException e) {
					running = false;
					e.printStackTrace();
				}
			}
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static void main(String[] args) {
		new SenderTest();
	}

}
