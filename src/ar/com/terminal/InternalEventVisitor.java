package ar.com.terminal;

import ar.com.terminal.Event.InternalEvent.CloseSignal;
import ar.com.terminal.Event.InternalEvent.IncomingPacket;
import ar.com.terminal.Event.InternalEvent.InternalException;
import ar.com.terminal.Event.InternalEvent.KeepAliveError;

interface InternalEventVisitor {

	public default void visit(IncomingPacket event) throws Exception {
	}

	public default void visit(KeepAliveError event) throws Exception {
	}

	public default void visit(InternalException event) throws Exception {
	}

	public default void visit(CloseSignal event) throws Exception {
	}

}
