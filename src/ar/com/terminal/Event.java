package ar.com.terminal;

import java.net.InetAddress;

import ar.com.terminal.QSYPacket.ToucheArgs;

/**
 * La clase Event provee una interfaz para trabajar con eventos.
 */
public abstract class Event {

	public Event() {
	}

	/**
	 * La clase ExternalEvent provee una interfaz para trabajar con eventos que se
	 * generan dentro del sistema pero que pueden ser enviados hacia aplicaciones
	 * externas.
	 */
	public abstract static class ExternalEvent extends Event {

		public ExternalEvent() {
		}

		public abstract void accept(ExternalEventVisitor visitor);

		/**
		 * La interfaz que permite realizar double dispatcher con cada subclase de
		 * ExternalEvent.
		 */
		public static interface ExternalEventVisitor {

			public default void visit(ConnectedNode event) {
			}

			public default void visit(DisconnectedNode event) {
			}

			public default void visit(Touche event) {
			}

			public default void visit(ExecutionStarted event) {
			}

			public default void visit(ExecutionFinished event) {
			}

			public default void visit(StepTimeOut event) {
			}

		}

		/**
		 * El evento que determina que un nuevo nodo ha sido conectado al sistema.
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
		 * El evento que determina que un nodo ha sido desconectado del sistema.
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
		 * El evento que determina que se ha recibido un paquete Touche por parte de
		 * algun nodo conectado al sistema.
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

		static final class ExecutionStarted extends ExternalEvent {

			public ExecutionStarted() {
			}

			@Override
			public void accept(ExternalEventVisitor visitor) {
				visitor.visit(this);
			}

		}

		static final class ExecutionFinished extends ExternalEvent {

			public ExecutionFinished() {
			}

			@Override
			public void accept(ExternalEventVisitor visitor) {
				visitor.visit(this);
			}
		}

		static final class StepTimeOut extends ExternalEvent {

			public StepTimeOut() {
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

		static interface InternalEventVisitor {

			public abstract void visit(IncomingPacket event) throws Exception;

			public abstract void visit(KeepAliveError event) throws Exception;

			public abstract void visit(CloseSignal event) throws Exception;

			public abstract void visit(ExecutionStarted event) throws Exception;

			public abstract void visit(ExecutionFinished event) throws Exception;

			public abstract void visit(StepTimeOut event) throws Exception;
		}

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

		static final class CloseSignal extends InternalEvent {

			public CloseSignal() {
			}

			@Override
			public void accept(InternalEventVisitor visitor) throws Exception {
				visitor.visit(this);
			}

		}

		static final class ExecutionStarted extends InternalEvent {

			public ExecutionStarted() {
			}

			@Override
			public void accept(InternalEventVisitor visitor) throws Exception {
				visitor.visit(this);
			}

		}

		static final class ExecutionFinished extends InternalEvent {

			public ExecutionFinished() {
			}

			@Override
			public void accept(InternalEventVisitor visitor) throws Exception {
				visitor.visit(this);
			}
		}

		static final class StepTimeOut extends InternalEvent {

			public StepTimeOut() {
			}

			@Override
			public void accept(InternalEventVisitor visitor) throws Exception {
				visitor.visit(this);
			}
		}
	}

}
