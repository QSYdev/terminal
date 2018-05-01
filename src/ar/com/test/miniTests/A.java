package ar.com.test.miniTests;

public abstract class A {

	public void imprimir() {
		synchronized (this) {
			System.out.println("Mensaje desde A");
		}
	}

}
