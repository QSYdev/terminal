package ar.com.terminal.test;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

import ar.com.terminal.internal.EventListener;
import ar.com.terminal.internal.QSYPacket;

public final class MulticastReceiverTest extends EventListener<InternalEvent> implements Runnable, InternalEventVisitor {

	private volatile boolean running = true;

	@Override
	public void run() {
		while (running) {
			try {
				InternalEvent event = getEvent();
				event.accept(this);
			} catch (InterruptedException e) {
				running = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void visit(IncomingPacket event) {
		System.out.println(event.getPacket().getPhysicalId());
	}

	public static void main(String[] args) throws SocketException, UnknownHostException, IOException, InterruptedException {
		MulticastReceiverTest test = new MulticastReceiverTest();
		Thread t = new Thread(test, "Test");
		t.start();

		MulticastReceiver mr = new MulticastReceiver((Inet4Address) Inet4Address.getByName("192.168.1.112"), (InetAddress) InetAddress.getByName(QSYPacket.MULTICAST_ADDRESS),
				QSYPacket.MULTICAST_PORT);
		mr.addListener(test);

		Scanner scanner = new Scanner(System.in);
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
			case 'd':
				mr.removeNode(19);
				break;
			}

		} while (command != 'q');
		scanner.close();

		t.interrupt();
		t.join();

		mr.close();
	}

}
