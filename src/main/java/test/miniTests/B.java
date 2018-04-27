package main.java.test.miniTests;

public final class B extends A {

	public void iniciar() {
		synchronized (this) {
			System.out.println("Mensaje desde B");
			super.imprimir();
			synchronized (this) {
				System.out.println("Otro mensaje");
			}
		}
	}

	/*
	 * Conclusion, hacer synchronized anidados no bloquea el thread.
	 */
	public static void main(String[] args) {
		final B b = new B();
		b.iniciar();
	}

}
