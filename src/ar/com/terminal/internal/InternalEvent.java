package ar.com.terminal.internal;

import ar.com.terminal.shared.Event;
import ar.com.terminal.shared.QSYPacket;

abstract class InternalEvent extends Event {

	public InternalEvent() {
	}

	public abstract void accept(InternalEventVisitor visitor) throws Exception;

	/**
	 * El Modulo de <b>KeepAlive</b> detecta que los paquetes keepalive de un nodo
	 * no han llegado a tiempo. El parametro recibido corresponde al id fisico del
	 * nodo cuyos paquetes no llegaron a tiempo.
	 */
	static final class KeepAliveError extends InternalEvent {

		private final int physicalId;

		public KeepAliveError(int physicalId) {
			this.physicalId = physicalId;
		}

		public int getPhysicalId() {
			return physicalId;
		}

		@Override
		public void accept(InternalEventVisitor visitor) throws Exception {
			visitor.visit(this);
		}

	}

	/**
	 * El modulo de <b>MulticastReceiver</b> y <b>Receiver</b> detectan un nuevo
	 * paquete proveniente de un nodo. El parametro recibido corresponde al paquete
	 * recibido.
	 */
	static final class IncomingPacket extends InternalEvent {

		private final QSYPacket packet;

		public IncomingPacket(QSYPacket packet) {
			this.packet = packet;
		}

		public QSYPacket getPacket() {
			return packet;
		}

		@Override
		public void accept(InternalEventVisitor visitor) throws Exception {
			visitor.visit(this);
		}

	}

	/**
	 * Todas las tareas en la terminal, pueden producir un error inesperado en el
	 * sistema. A traves de este evento se puede enviar la informacion a quien este
	 * interesado.
	 */
	static final class InternalException extends InternalEvent {

		private final Exception exception;

		public InternalException(Exception e) {
			this.exception = e;
		}

		public Exception getException() {
			return exception;
		}

		@Override
		public void accept(InternalEventVisitor visitor) throws Exception {
			visitor.visit(this);
		}
	}

	/**
	 * La Terminal envia al MainController una señal que debe finalizarse.
	 */
	static final class CloseSignal extends InternalEvent {

		public CloseSignal() {
		}

		@Override
		public void accept(InternalEventVisitor visitor) throws Exception {
			visitor.visit(this);
		}

	}
}