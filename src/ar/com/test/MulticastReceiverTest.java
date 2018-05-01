package ar.com.test;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import ar.com.terminal.EventListener;
import ar.com.terminal.InternalEventVisitor;
import ar.com.terminal.MulticastReceiver;
import ar.com.terminal.QSYPacket;
import ar.com.terminal.Event.IncomingPacket;
import ar.com.terminal.Event.InternalException;

public class MulticastReceiverTest extends EventListener<InternalException> implements Runnable, InternalEventVisitor {

	private boolean running = true;

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
		System.out.println(event.getPacket().getPhysicalId());
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
