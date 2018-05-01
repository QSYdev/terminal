package ar.com.terminal;

import java.net.InetAddress;

import ar.com.terminal.QSYPacket.ToucheArgs;

public abstract class Event {

	public Event() {
	}

	public abstract static class ExternalEvent extends Event {

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

			public Exception getExceptio() {
				return exception;
			}

			@Override
			public void accept(ExternalEventVisitor visitor) {
				visitor.visit(this);
			}

		}
	}

	abstract static class InternalEvent extends Event {

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

}
