package ar.com.terminal.shared;

import ar.com.terminal.shared.ExternalEvent.ConnectedNode;
import ar.com.terminal.shared.ExternalEvent.DisconnectedNode;
import ar.com.terminal.shared.ExternalEvent.InternalException;
import ar.com.terminal.shared.ExternalEvent.Touche;

public interface ExternalEventVisitor {

	public default void visit(ConnectedNode event) {
	}

	public default void visit(DisconnectedNode event) {
	}

	public default void visit(Touche event) {
	}

	public default void visit(InternalException event) {
	}

}
