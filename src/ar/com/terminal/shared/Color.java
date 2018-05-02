package ar.com.terminal.shared;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class Color {

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

	private static final ArrayList<Color> colors;
	static {
		colors = new ArrayList<>(8);
		colors.add(NO_COLOR);
		colors.add(BLUE);
		colors.add(GREEN);
		colors.add(CYAN);
		colors.add(RED);
		colors.add(MAGENTA);
		colors.add(YELLOW);
		colors.add(WHITE);
	}

	private final byte red;
	private final byte green;
	private final byte blue;

	private final int hashCode;

	private Color(byte red, byte green, byte blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		HashCodeBuilder hashBuilder = new HashCodeBuilder();
		hashBuilder.append(red);
		hashBuilder.append(green);
		hashBuilder.append(blue);
		this.hashCode = hashBuilder.toHashCode();
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
		String result = hashTable.get(this);
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
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Color) {
			Color c = (Color) obj;
			return red == c.red && green == c.green && blue == c.blue;
		} else {
			return false;
		}
	}

	public static final class ColorFactory {

		public static Color createColor(byte red, byte green, byte blue) {
			int redValue = (red == 0x0F) ? 1 : 0;
			int greenValue = (green == 0x0F) ? 1 : 0;
			int blueValue = (blue == 0x0F) ? 1 : 0;
			int index = blueValue + greenValue * 2 + redValue * 4;
			return colors.get(index);
		}
	}

}
