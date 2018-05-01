package ar.com.terminal;

import java.util.Hashtable;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class Color {

	private static final byte MAX_AMOUNT_COLOR = 16;

	public static final Color WHITE = new Color((byte) 0x0F, (byte) 0x0F, (byte) 0x0F);
	public static final Color YELLOW = new Color((byte) 0x0F, (byte) 0x0F, (byte) 0x00);
	public static final Color MAGENTA = new Color((byte) 0x0F, (byte) 0x00, (byte) 0x0F);
	public static final Color RED = new Color((byte) 0x0F, (byte) 0x00, (byte) 0x00);
	public static final Color CYAN = new Color((byte) 0x00, (byte) 0x0F, (byte) 0x0F);
	public static final Color GREEN = new Color((byte) 0x00, (byte) 0x0F, (byte) 0x00);
	public static final Color BLUE = new Color((byte) 0x00, (byte) 0x00, (byte) 0x0F);
	public static final Color NO_COLOR = new Color((byte) 0x00, (byte) 0x00, (byte) 0x00);

	private static final Hashtable<Color, String> hashTable;
	static {
		hashTable = new Hashtable<>();
		hashTable.put(WHITE, "Blanco");
		hashTable.put(YELLOW, "Amarillo");
		hashTable.put(MAGENTA, "Magenta");
		hashTable.put(RED, "Rojo");
		hashTable.put(CYAN, "Cyan");
		hashTable.put(GREEN, "Verde");
		hashTable.put(BLUE, "Azul");
		hashTable.put(NO_COLOR, "Sin Color");
	}

	private final byte red;
	private final byte green;
	private final byte blue;

	private final int hashCode;

	public Color(final byte red, final byte green, final byte blue) {
		if (checkAmountOfColor(red) && checkAmountOfColor(green) && checkAmountOfColor(blue)) {
			this.red = red;
			this.green = green;
			this.blue = blue;
			final HashCodeBuilder hashBuilder = new HashCodeBuilder();
			hashBuilder.append(red);
			hashBuilder.append(green);
			hashBuilder.append(blue);
			this.hashCode = hashBuilder.toHashCode();
		} else {
			throw new IllegalArgumentException("<< COLOR >> La cantidad de color ingresada debe ser un valor entre 0 y " + MAX_AMOUNT_COLOR + "exclusive");
		}
	}

	private boolean checkAmountOfColor(final byte color) {
		return color >= 0 && color < MAX_AMOUNT_COLOR;
	}

	public byte getRed() {
		return red;
	}

	public byte getGreen() {
		return green;
	}

	public byte getBlue() {
		return blue;
	}

	@Override
	public String toString() {
		final String result = hashTable.get(this);
		if (result == null)
			return "RED = " + red + " || GREEN = " + green + " || BLUE = " + blue;
		else
			return result;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Color) {
			final Color c = (Color) obj;
			return red == c.red && green == c.green && blue == c.blue;
		} else {
			return false;
		}
	}

}
