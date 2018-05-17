package ar.com.terminal.test;

import java.util.concurrent.LinkedBlockingQueue;

public class PruebaRigor implements Runnable {

	private final LinkedBlockingQueue<Integer> message = new LinkedBlockingQueue<>();
	private final Thread thread;

	public PruebaRigor() {
		message.add(1);
		this.thread = new Thread(this);
		this.thread.start();
	}

	@Override
	public void run() {
		while (true) {
			try {
				Integer inte = message.take();
				System.out.println(inte);
				executeSynchronizedMethod();
			} catch (InterruptedException e) {
				System.out.println("Se metio por aca");
			}
		}
	}

	private synchronized void executeSynchronizedMethod() {
		thread.interrupt();
		message.add(2);
	}

	public static void main(String[] args) {
		new PruebaRigor();
	}

}
