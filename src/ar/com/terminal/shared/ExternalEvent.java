package ar.com.terminal.shared;

import java.net.InetAddress;

import ar.com.terminal.shared.QSYPacket.ToucheArgs;

public abstract class ExternalEvent extends Event {

	public ExternalEvent() {
	}

	public abstract void accept(ExternalEventVisitor visitor);

	/**
	 * La <b>Terminal</b> detecta que un nuevo nodo se ha conectado y envia la
	 * informacion hacia afuera del sistema.
	 */
	public static final class ConnectedNode extends ExternalEvent {

		private final InetAddress nodeAddress;
		private final int physicalId;

		public ConnectedNode(int physicalId, InetAddress nodeAddress) {
			this.physicalId = physicalId;
			this.nodeAddress = nodeAddress;
		}

		public InetAddress getNodeAddress() {
			return nodeAddress;
		}

		public int getPhysicalId() {
			return physicalId;
		}

		@Override
		public void accept(ExternalEventVisitor visitor) {
			visitor.visit(this);
		}

	}

	/**
	 * La <b>Terminal</b> detecta que un nodo se ha desconectado y envia la
	 * información hacia afuera del sistema.
	 */
	public static final class DisconnectedNode extends ExternalEvent {

		private final InetAddress nodeAddress;
		private final int physicalId;

		public DisconnectedNode(int physicalId, InetAddress nodeAddress) {
			this.physicalId = physicalId;
			this.nodeAddress = nodeAddress;
		}

		public InetAddress getNodeAddress() {
			return nodeAddress;
		}

		public int getPhysicalId() {
			return physicalId;
		}

		@Override
		public void accept(ExternalEventVisitor visitor) {
			visitor.visit(this);
		}
	}

	/**
	 * La <b>Terminal</b> detecta que un nuevo nodo ha sido tocado y envia la
	 * informacion hacia afuera del sistema.
	 */
	public static final class Touche extends ExternalEvent {

		private final ToucheArgs toucheArgs;

		public Touche(ToucheArgs toucheArgs) {
			this.toucheArgs = toucheArgs;
		}

		public ToucheArgs getToucheArgs() {
			return toucheArgs;
		}

		@Override
		public void accept(ExternalEventVisitor visitor) {
			visitor.visit(this);
		}
	}

	/**
	 * Todas las tareas en la terminal, pueden producir un error inesperado en el
	 * sistema. A traves de este evento se puede enviar la informacion a quien este
	 * interesado.
	 */
	public static final class InternalException extends ExternalEvent {

		private final Exception exception;

		public InternalException(Exception e) {
			this.exception = e;
		}

		public Exception getException() {
			return exception;
		}

		@Override
		public void accept(ExternalEventVisitor visitor) {
			visitor.visit(this);
		}

	}
}
