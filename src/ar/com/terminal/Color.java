package ar.com.terminal;

import java.util.ArrayList;

/**
 * La clase Color se utiliza para encapsular colores en RGB. Las instancias
 * definidas son de solo lectura y no permite la creacion de nuevos colores que
 * no sean los que ya estan creados.
 */
public final class Color {

	public static final Color WHITE = new Color((byte) 0x0F, (byte) 0x0F, (byte) 0x0F);
	public static final Color YELLOW = new Color((byte) 0x0F, (byte) 0x0F, (byte) 0x00);
	public static final Color MAGENTA = new Color((byte) 0x0F, (byte) 0x00, (byte) 0x0F);
	public static final Color RED = new Color((byte) 0x0F, (byte) 0x00, (byte) 0x00);
	public static final Color CYAN = new Color((byte) 0x00, (byte) 0x0F, (byte) 0x0F);
	public static final Color GREEN = new Color((byte) 0x00, (byte) 0x0F, (byte) 0x00);
	public static final Color BLUE = new Color((byte) 0x00, (byte) 0x00, (byte) 0x0F);
	public static final Color NO_COLOR = new Color((byte) 0x00, (byte) 0x00, (byte) 0x00);

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

	private Color(byte red, byte green, byte blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
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
		return "RED = " + red + " || GREEN = " + green + " || BLUE = " + blue;
	}

	static final class ColorFactory {

		public static Color createColor(byte red, byte green, byte blue) {
			int redValue = (red == 0x0F) ? 1 : 0;
			int greenValue = (green == 0x0F) ? 1 : 0;
			int blueValue = (blue == 0x0F) ? 1 : 0;
			int index = blueValue + greenValue * 2 + redValue * 4;
			return colors.get(index);
		}
	}

}
