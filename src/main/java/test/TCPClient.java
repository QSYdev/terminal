package main.java.test;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Random;
import java.util.Scanner;

import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.lib.protocol.QSYPacket.CommandArgs;
import main.java.libterminal.lib.routine.Color;

public final class TCPClient implements Runnable {

	private static int count = 0;

	private final int id = count++;
	private final SocketChannel channel;
	private final ByteBuffer byteBuffer;

	private final Random random = new Random();

	private boolean running = true;

	public TCPClient() throws IOException {
		byteBuffer = ByteBuffer.allocate(QSYPacket.PACKET_SIZE);
		channel = SocketChannel.open(new InetSocketAddress(Inet4Address.getByName("192.168.1.106"), QSYPacket.TCP_PORT));
		channel.configureBlocking(false);
		channel.socket().setTcpNoDelay(false);
		byteBuffer.put((QSYPacket.createCommandPacket(new CommandArgs(id, Color.RED, 500, 3, false, true))).getRawData());
	}

	@Override
	public void run() {
		while (running) {
			try {
				Thread.sleep(500 + random.nextInt(300) - 150);
				byteBuffer.flip();
				byte bytesSent = 0;
				while ((bytesSent += channel.write(byteBuffer)) != QSYPacket.PACKET_SIZE)
					;
			} catch (final Exception e) {
				running = false;
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		final LinkedList<Thread> list = new LinkedList<>();
		final Scanner scanner = new Scanner(System.in);
		char command = 0;
		while (command != 'q') {
			command = scanner.nextLine().charAt(0);
			switch (command) {
			case 'a':
				final TCPClient client = new TCPClient();
				final Thread thread = new Thread(client, "Client " + client.id);
				list.add(thread);
				System.out.println("Se ha creado el cliente con id " + client.id);
				thread.start();
				break;
			case 'r':
				if (!list.isEmpty()) {
					final Thread t = list.removeLast();
					t.interrupt();
					t.join();
					System.out.println("Se ha eliminado el último cliente agregado");
					break;
				}
			}
		}

		scanner.close();
		for (final Thread thread : list) {
			thread.interrupt();
			thread.join();
		}
	}

}
