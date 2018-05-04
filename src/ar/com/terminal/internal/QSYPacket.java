package ar.com.terminal.internal;

import java.net.InetAddress;
import java.util.Arrays;

import ar.com.terminal.internal.Color.ColorFactory;

public final class QSYPacket {

	public static final long QSY_PROTOCOL_VERSION = 170901;

	public static final byte PACKET_SIZE = 16;
	public static final int KEEP_ALIVE_MS = 500;

	public static final String MULTICAST_ADDRESS = "224.0.0.12";
	public static final int MULTICAST_PORT = 3000;
	public static final int TCP_PORT = 3000;

	public static final int MIN_ID_SIZE = 0;
	public static final int MAX_ID_SIZE = (int) (Math.pow(2, 16) - 1);
	private static final long MIN_DELAY_SIZE = 0;
	private static final long MAX_DELAY_SIZE = (long) (Math.pow(2, 32) - 1);

	private static final byte Q_INDEX = 0x00;
	private static final byte S_INDEX = 0x01;
	private static final byte Y_INDEX = 0x02;
	private static final byte TYPE_INDEX = 0x03;
	private static final byte ID_INDEX = 0x04;
	private static final byte COLOR_RG_INDEX = 0x06;
	private static final byte COLOR_B_INDEX = 0x07;
	private static final byte DELAY_INDEX = 0x08;
	private static final byte STEP_INDEX = 0x0C;
	private static final byte CONFIGURATION_INDEX = 0x0E;

	private static final byte TYPE_HELLO = 0x00;
	private static final byte TYPE_COMMAND = 0x01;
	private static final byte TYPE_TOUCHE = 0x02;
	private static final byte TYPE_KEEPALIVE = 0x03;

	public enum PacketType {
		Hello, Command, Touche, Keepalive
	}

	private final InetAddress nodeAddress;
	private final PacketType packetType;
	private final int id;
	private final Color color;
	private final long delay;
	private final int numberOfStep;
	private final boolean touch;
	private final boolean sound;
	private final byte[] rawData;

	private QSYPacket(InetAddress nodeAddress, PacketType type, int id, Color color, long delay, int numberOfStep, boolean touch, boolean sound) {
		this.rawData = new byte[PACKET_SIZE];
		rawData[Q_INDEX] = 'Q';
		rawData[S_INDEX] = 'S';
		rawData[Y_INDEX] = 'Y';
		this.nodeAddress = nodeAddress;
		this.id = id;
		setDataIntoArray(id, (byte) 16, rawData, ID_INDEX);
		this.color = color;
		setDataIntoArray(getIntFromColor(color), (byte) 16, rawData, COLOR_RG_INDEX);
		this.delay = delay;
		setDataIntoArray(delay, (byte) 32, rawData, DELAY_INDEX);
		this.packetType = type;
		setDataIntoArray(getShortFromPacketType(packetType), (byte) 8, rawData, TYPE_INDEX);
		this.numberOfStep = numberOfStep;
		setDataIntoArray(numberOfStep, (byte) 16, rawData, STEP_INDEX);

		int configuration = 0x00;
		if (this.touch = touch) {
			configuration += 0x02;
		}
		if (this.sound = sound) {
			configuration += 0x01;
		}
		setDataIntoArray(configuration, (byte) 16, rawData, CONFIGURATION_INDEX);
	}

	public QSYPacket(InetAddress nodeAddress, byte[] data) throws IllegalArgumentException {
		if (nodeAddress == null) {
			throw new IllegalArgumentException("<< QSY_PACKET_ERROR >> La direccion del nodo debe ser valida");
		} else if (data.length != PACKET_SIZE) {
			throw new IllegalArgumentException("<< QSY_PACKET_ERROR >> La longitud del QSYPacket debe ser de " + PACKET_SIZE + ".");
		} else if (data[Q_INDEX] != 'Q' || data[S_INDEX] != 'S' || data[Y_INDEX] != 'Y') {
			throw new IllegalArgumentException("<< QSY_PACKET_ERROR >> El QSYPacket posee una firma invalida.");
		}

		this.nodeAddress = nodeAddress;
		this.packetType = getPacketTypeFromShort(((short) convertBytesToLong(data, TYPE_INDEX, TYPE_INDEX)));
		this.id = (int) convertBytesToLong(data, ID_INDEX, (byte) (ID_INDEX + 1));
		this.color = getColorFromInt(data);
		this.delay = (long) convertBytesToLong(data, DELAY_INDEX, (byte) (DELAY_INDEX + 3));
		this.numberOfStep = (int) convertBytesToLong(data, STEP_INDEX, (byte) (STEP_INDEX + 1));

		short configuration = (short) convertBytesToLong(data, CONFIGURATION_INDEX, (byte) (CONFIGURATION_INDEX + 1));
		this.touch = ((configuration & 0x0002) == 0x0002);
		this.sound = ((configuration & 0x0001) == 0x0001);
		rawData = Arrays.copyOf(data, PACKET_SIZE);
	}

	private static Color getColorFromInt(byte[] data) {
		byte red = (byte) convertBytesToLong(new byte[] { (byte) ((data[COLOR_RG_INDEX] >> 4) & 0x0F) }, (byte) 0, (byte) 0);
		byte green = (byte) convertBytesToLong(new byte[] { (byte) ((data[COLOR_RG_INDEX]) & 0x0F) }, (byte) 0, (byte) 0);
		byte blue = (byte) convertBytesToLong(new byte[] { (byte) ((data[COLOR_B_INDEX] >> 4) & 0x0F) }, (byte) 0, (byte) 0);
		return ColorFactory.createColor(red, green, blue);
	}

	private static int getIntFromColor(Color color) throws IllegalArgumentException {
		return (int) (color.getRed() * Math.pow(2, 12) + color.getGreen() * Math.pow(2, 8) + color.getBlue() * Math.pow(2, 4));
	}

	private static PacketType getPacketTypeFromShort(short type) {
		PacketType packetType;
		switch (type) {
		case TYPE_HELLO:
			packetType = PacketType.Hello;
			break;
		case TYPE_COMMAND:
			packetType = PacketType.Command;
			break;
		case TYPE_TOUCHE:
			packetType = PacketType.Touche;
			break;
		case TYPE_KEEPALIVE:
			packetType = PacketType.Keepalive;
			break;
		default:
			throw new IllegalArgumentException("<< QSY_PACKET_ERROR >> El QSYPacket posee un type invalido.");
		}

		return packetType;
	}

	private static short getShortFromPacketType(PacketType packetType) {
		short type;
		switch (packetType) {
		case Hello: {
			type = TYPE_HELLO;
			break;
		}
		case Command: {
			type = TYPE_COMMAND;
			break;
		}
		case Keepalive: {
			type = TYPE_KEEPALIVE;
			break;
		}
		case Touche: {
			type = TYPE_TOUCHE;
			break;
		}
		default: {
			throw new IllegalArgumentException("<< QSY_PACKET_ERROR >> El QSYPacket posee un type invalido.");
		}
		}
		return type;
	}

	public InetAddress getNodeAddress() {
		return nodeAddress;
	}

	public PacketType getType() {
		return packetType;
	}

	public int getPhysicalId() {
		return id;
	}

	public Color getColor() {
		return color;
	}

	public long getDelay() {
		return delay;
	}

	public int getNumberOfStep() {
		return numberOfStep;
	}

	public boolean touchEnabled() {
		return touch;
	}

	public boolean soundEnabled() {
		return sound;
	}

	public byte[] getRawData() {
		return rawData;
	}

	private static long convertBytesToLong(byte[] data, byte firstIndex, byte lastIndex) {
		long value = 0;

		long mul = 1;
		for (byte i = lastIndex; i >= firstIndex; i--) {
			long b = data[i];
			for (byte j = 0; j < 8; j++) {
				value += mul * (0x1 & b);
				b >>>= 1;
				mul <<= 1;
			}
		}

		return value;
	}

	private static void setDataIntoArray(long value, byte bits, byte[] data, int firstIndex) {
		long val = value;

		long mul = 1;
		for (byte i = (byte) (bits - 1); i >= 0; i--) {
			data[i / 8 + firstIndex] += mul * (val % 2);
			mul <<= 1;
			if (mul == 256) {
				mul = 1;
			}
			val >>= 1;
		}
	}

	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		switch (getType()) {
		case Hello: {
			stringBuilder.append("QSYHelloPacket");
			break;
		}
		case Command: {
			stringBuilder.append("QSYCommandPacket");
			break;
		}
		case Touche: {
			stringBuilder.append("QSYTouchePacket");
			break;
		}
		case Keepalive: {
			stringBuilder.append("QSYKeepalivePacket");
			break;
		}
		}
		stringBuilder.append(" From = " + getNodeAddress() + "\n");
		stringBuilder.append("ID = " + getPhysicalId() + " || ");
		stringBuilder.append(getColor());
		stringBuilder.append(" || DELAY = " + getDelay());
		stringBuilder.append("\nSTEP = " + getNumberOfStep());
		stringBuilder.append(" || DISTANCE = " + touchEnabled() + " || SOUND = " + soundEnabled() + "\n");

		return stringBuilder.toString();
	}

	public static QSYPacket createCommandPacket(CommandArgs params) {

		int physicalId = params.getPhysicialId();
		Color color = params.getColor();
		long delay = params.getDelay();
		int numberOfStep = params.getNumberOfStep();
		boolean touchEnabled = params.isTouch();
		boolean soundEnabled = params.isSound();

		if (physicalId < MIN_ID_SIZE || physicalId > MAX_ID_SIZE) {
			throw new IllegalArgumentException("<< QSY_PACKET_ERROR >> El id debe estar entre [" + MIN_ID_SIZE + " ; " + MAX_ID_SIZE + "]");
		} else if (color == null) {
			throw new IllegalArgumentException("<< QSY_PACKET_ERROR >> El QSYPacket no posee el color correspondiente.");
		} else if (delay < MIN_DELAY_SIZE || delay > MAX_DELAY_SIZE) {
			throw new IllegalArgumentException("<< QSY_PACKET_ERROR >> El delay debe estar entre [" + MIN_DELAY_SIZE + " ; " + MAX_DELAY_SIZE + "]");
		} else {
			return new QSYPacket(null, PacketType.Command, physicalId, color, delay, numberOfStep, touchEnabled, soundEnabled);
		}
	}

	public static final class CommandArgs {

		private final int physicalId;
		private final Color color;
		private final long delay;
		private final int numberOfStep;
		private final boolean touch;
		private final boolean sound;

		public CommandArgs(int physicalId, Color color, long delay, int numberOfStep, boolean touch, boolean sound) {
			this.physicalId = physicalId;
			this.color = color;
			this.delay = delay;
			this.numberOfStep = numberOfStep;
			this.touch = touch;
			this.sound = sound;
		}

		public CommandArgs(int physicalId, Color color, long delay, int numberOfStep) {
			this(physicalId, color, delay, numberOfStep, false, false);
		}

		public int getPhysicialId() {
			return physicalId;
		}

		public Color getColor() {
			return color;
		}

		public long getDelay() {
			return delay;
		}

		public int getNumberOfStep() {
			return numberOfStep;
		}

		public boolean isTouch() {
			return touch;
		}

		public boolean isSound() {
			return sound;
		}

	}

	public static final class ToucheArgs {

		private final int physicalId;
		private final long delay;
		private final Color color;

		public ToucheArgs(int physicalId, long delay, Color color) {
			this.physicalId = physicalId;
			this.delay = delay;
			this.color = color;
		}

		public int getPhysicalId() {
			return physicalId;
		}

		public long getDelay() {
			return delay;
		}

		public Color getColor() {
			return color;
		}

	}
}
