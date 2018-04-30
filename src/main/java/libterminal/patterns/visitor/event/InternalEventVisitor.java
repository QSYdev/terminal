package main.java.libterminal.patterns.visitor.event;

import main.java.libterminal.patterns.observer.Event.InternalEvent.IncomingPacket;
import main.java.libterminal.patterns.observer.Event.InternalEvent.InternalException;
import main.java.libterminal.patterns.observer.Event.InternalEvent.KeepAliveError;

public interface InternalEventVisitor {

	public default void visit(IncomingPacket event) throws Exception {
	}

	public default void visit(KeepAliveError event) throws Exception {
	}

	public default void visit(InternalException internalError) throws Exception {
	}

}
