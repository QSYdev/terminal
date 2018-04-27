package main.java.test.miniTests;

import java.util.concurrent.LinkedBlockingQueue;

public class D implements Runnable {

	@SuppressWarnings("unused")
	private final LinkedBlockingQueue<Integer> list;

	public D() {
		this.list = new LinkedBlockingQueue<>();
	}

	@Override
	public void run() {
		for (long i = 0; i < 10000000000L; i++) {

		}
		System.out.println("Se ejecuto una vuelta");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			System.out.println("Se interrumpio");
		}
		System.out.println("Se ejecuta?");
	}

	/*
	 * Conclusion, en take de blocking queue antes de dormirse se comprueba que si
	 * se interrumpio. Idem en thread.sleep();
	 */
	public static void main(String[] args) {
		final Thread t = new Thread(new D());
		t.start();
		t.interrupt();
	}

}
