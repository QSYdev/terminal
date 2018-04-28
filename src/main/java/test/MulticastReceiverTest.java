package main.java.test;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import main.java.libterminal.lib.network.MulticastReceiver;
import main.java.libterminal.lib.protocol.QSYPacket;
import main.java.libterminal.patterns.observer.Event.IncomingPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent;
import main.java.libterminal.patterns.observer.EventListener;
import main.java.libterminal.patterns.visitor.event.InternalEventVisitor;

public class MulticastReceiverTest extends EventListener<InternalEvent> implements Runnable, InternalEventVisitor {

	private boolean running = true;

	@Override
	public void run() {
		while (running) {
			try {
				final InternalEvent event = getEvent();
				event.accept(this);
			} catch (final InterruptedException e) {
				running = false;
			}
		}
	}

	@Override
	public void visit(final IncomingPacket event) {
		System.out.println("Paquete recibido");
	}

	public static void main(String[] args) throws SocketException, UnknownHostException, IOException, InterruptedException {
		final MulticastReceiverTest test = new MulticastReceiverTest();
		final Thread t = new Thread(test, "Test");
		t.start();

		final MulticastReceiver mr = new MulticastReceiver((Inet4Address) Inet4Address.getByName("192.168.1.112"), (InetAddress) InetAddress.getByName(QSYPacket.MULTICAST_ADDRESS),
				QSYPacket.MULTICAST_PORT);
		mr.addListener(test);

		final Scanner scanner = new Scanner(System.in);
		char command = 0;
		do {
			command = scanner.nextLine().charAt(0);
			switch (command) {
			case 'a':
				mr.acceptPackets(true);
				break;
			case 'f':
				mr.acceptPackets(false);
				break;
			}

		} while (command != 'q');
		scanner.close();

		t.interrupt();
		t.join();

		mr.close();
	}

}
