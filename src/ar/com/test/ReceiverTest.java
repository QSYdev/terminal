package ar.com.test;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import ar.com.terminal.EventListener;
import ar.com.terminal.InternalEventVisitor;
import ar.com.terminal.QSYPacket;
import ar.com.terminal.Receiver;
import ar.com.terminal.Event.IncomingPacket;
import ar.com.terminal.Event.InternalException;

public class ReceiverTest implements AutoCloseable {

	private static final AtomicInteger i = new AtomicInteger(0);
	private final Receiver receiver;
	private final Thread server;
	private final Thread terminal;

	public ReceiverTest() throws IOException {
		this.receiver = new Receiver();
		this.server = new Thread(new ServerTask(), "Server");
		final TerminalTask terminal;
		this.terminal = new Thread(terminal = new TerminalTask(), "Terminal");
		this.receiver.addListener(terminal);
		this.server.start();
		this.terminal.start();
	}

	@Override
	public void close() throws InterruptedException, IOException {
		receiver.close();
		server.interrupt();
		terminal.interrupt();
		server.join();
		terminal.join();
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		final ReceiverTest receiver = new ReceiverTest();
		final Scanner sc = new Scanner(System.in);
		while (sc.nextLine().charAt(0) != 'q')
			;
		sc.close();
		receiver.close();
	}

	private final class ServerTask implements Runnable {

		private boolean running = true;

		@Override
		public void run() {
			try {
				final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
				serverSocketChannel.socket().bind(new InetSocketAddress(Inet4Address.getByName("192.168.1.112"), QSYPacket.TCP_PORT));
				while (running) {
					final SocketChannel channel = serverSocketChannel.accept();
					System.out.println("Se ha conectado un cliente");
					channel.socket().setTcpNoDelay(true);
					channel.configureBlocking(false);
					receiver.newNode(i.getAndIncrement(), channel);
				}
			} catch (final ClosedByInterruptException e) {
				running = false;
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	private final class TerminalTask extends EventListener<InternalException> implements Runnable, InternalEventVisitor {

		private boolean running;

		public TerminalTask() {
			this.running = true;
		}

		@Override
		public void run() {
			while (running) {
				try {
					final InternalException event = getEvent();
					event.accept(this);
				} catch (final InterruptedException e) {
					running = false;
				}
			}
		}

		@Override
		public void visit(final IncomingPacket event) {
			System.out.println("Se recibió un paquete del nodo " + event.getPacket().getPhysicalId());
		}

	}

}
