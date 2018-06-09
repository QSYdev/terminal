package ar.com.terminal;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import ar.com.terminal.Color.ColorFactory;
import ar.com.terminal.Routine.NodeConfiguration;
import ar.com.terminal.Routine.Step;

public final class RoutineManager {

	static {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(Routine.class, new RoutineSerializer());
		gsonBuilder.registerTypeAdapter(Step.class, new StepSerializer());
		gsonBuilder.registerTypeAdapter(NodeConfiguration.class, new NodeConfigurationSerializer());
		gsonBuilder.registerTypeAdapter(Color.class, new ColorSerializer());
		gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
	}

	private static final Gson gson;

	/**
	 * Lee una rutina desde un archivo fisico. Siempre y cuando la rutina no haya
	 * sido manipulada fuera de este sistema, se garantiza que la misma contendra un
	 * formato valido para ser ejecutada en la terminal.
	 */
	public static Routine loadRoutine(String path) throws UnsupportedEncodingException, IOException {
		Reader reader = null;
		Routine routine = null;
		try {
			reader = new FileReader(path);
			routine = gson.fromJson(reader, Routine.class);
		} finally {
			if (reader != null)
				reader.close();
		}

		return routine;
	}

	/**
	 * Almacena la rutina en un archivo fisico especificado.
	 */
	public static void storeRoutine(String path, Routine routine) throws IOException, IllegalArgumentException {
		Writer writer = null;
		try {
			writer = new FileWriter(path);
			gson.toJson(routine, writer);
		} finally {
			if (writer != null)
				writer.close();
		}
	}

	/**
	 * Determina si la rutina que se envia por parametro es valida. La misma no
	 * puede ser nula.
	 */
	public static void validateRoutine(Routine routine) throws IllegalArgumentException {
		if (routine.getPlayersCount() <= 0)
			throw new IllegalArgumentException("La cantidad de jugadores debe ser mayor a 0.");
		else if (routine.getNumberOfNodes() <= 0)
			throw new IllegalArgumentException("La cantidad de nodos debe ser mayor a 0.");
		else if (routine.getTotalTimeOut() < 0)
			throw new IllegalArgumentException("El timeOut total debe ser mayor o igual a 0.");

		int i = 0;
		for (Step step : routine.getSteps()) {
			if (step.getTimeOut() < 0) {
				throw new IllegalArgumentException("StepTimeOut del step " + i + " debe ser mayor o igual a 0.");
			} else {

				List<Integer> values = ExpressionTree.getValuesFromExpression(step.getExpression());
				for (int value : values) {
					if (value >= routine.getNumberOfNodes())
						throw new IllegalArgumentException("El nodo " + value + " del step " + i + " debe ser menor a la cantidad de nodos elegidos");
				}

				int j = 0;
				for (NodeConfiguration config : step.getNodeConfigurationList()) {
					if (config.getDelay() < 0)
						throw new IllegalArgumentException("El delay de la configuracion " + j + " en el step " + i + " debe ser mayor o igual a 0.");
					else if (config.getLogicalId() >= routine.getNumberOfNodes())
						throw new IllegalArgumentException("El nodo " + config.getLogicalId() + " de la configuracion " + j + " en el step " + i + " debe ser menor a la cantidad de nodos elegidos.");

					values.remove((Object) config.getLogicalId());
					j++;
				}

				if (!values.isEmpty())
					throw new IllegalArgumentException("No hay ninguna configuracion que encienda el nodo " + values.get(0) + " en el step " + i);

			}
			++i;
		}
	}

	private static final class RoutineSerializer implements JsonDeserializer<Routine>, JsonSerializer<Routine> {

		private static final String PLAYERS_COUNT_ATT = "playerCount";
		private static final String NUMBER_OF_NODES_ATT = "numberOfNodes";
		private static final String STEPS_ATT = "steps";
		private static final String TOTAL_TIME_OUT_ATT = "totalTimeOut";
		private static final String NAME_ATT = "name";

		@Override
		public Routine deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();
			byte playersCount = jsonObject.get(PLAYERS_COUNT_ATT).getAsByte();
			byte numberOfNodes = jsonObject.get(NUMBER_OF_NODES_ATT).getAsByte();
			long totalTimeOut = jsonObject.get(TOTAL_TIME_OUT_ATT).getAsLong();
			Step[] steps = context.deserialize(jsonObject.get(STEPS_ATT), Step[].class);
			String name = jsonObject.get(NAME_ATT).getAsString();

			return new Routine(playersCount, numberOfNodes, totalTimeOut, new ArrayList<>(Arrays.asList(steps)), name);
		}

		@Override
		public JsonElement serialize(Routine routine, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();

			jsonObject.addProperty(PLAYERS_COUNT_ATT, routine.getPlayersCount());
			jsonObject.addProperty(NUMBER_OF_NODES_ATT, routine.getNumberOfNodes());
			jsonObject.addProperty(TOTAL_TIME_OUT_ATT, routine.getTotalTimeOut());
			jsonObject.add(STEPS_ATT, context.serialize(routine.getSteps()));
			jsonObject.addProperty(NAME_ATT, routine.getName());

			return jsonObject;
		}
	}

	private static final class StepSerializer implements JsonDeserializer<Step>, JsonSerializer<Step> {

		private static final String EXPRESSION_ATT = "expression";
		private static final String TIME_OUT_ATT = "timeOut";
		private static final String STOP_ON_TIMEOUT_ATT = "stopOnTimeout";
		private static final String NODES_CONFIGURATION_ATT = "nodesConfigurations";

		@Override
		public Step deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();

			String expression = jsonObject.get(EXPRESSION_ATT).getAsString();
			long timeOut = jsonObject.get(TIME_OUT_ATT).getAsLong();
			boolean stopOnTimeout = jsonObject.get(STOP_ON_TIMEOUT_ATT).getAsBoolean();
			NodeConfiguration[] nodesConfiguration = context.deserialize(jsonObject.get(NODES_CONFIGURATION_ATT), NodeConfiguration[].class);

			return new Step(new LinkedList<>(Arrays.asList(nodesConfiguration)), timeOut, expression, stopOnTimeout);
		}

		@Override
		public JsonElement serialize(Step step, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();

			jsonObject.addProperty(EXPRESSION_ATT, step.getExpression());
			jsonObject.addProperty(TIME_OUT_ATT, step.getTimeOut());
			jsonObject.addProperty(STOP_ON_TIMEOUT_ATT, step.stopOnTimeOut());
			jsonObject.add(NODES_CONFIGURATION_ATT, context.serialize(step.getNodeConfigurationList()));

			return jsonObject;
		}

	}

	private static final class NodeConfigurationSerializer implements JsonDeserializer<NodeConfiguration>, JsonSerializer<NodeConfiguration> {

		private static final String ID_ATT = "id";
		private static final String DELAY_ATT = "delay";
		private static final String COLOR_ATT = "color";

		@Override
		public NodeConfiguration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();

			int id = jsonObject.get(ID_ATT).getAsInt();
			long delay = jsonObject.get(DELAY_ATT).getAsLong();
			Color color = context.deserialize(jsonObject.get(COLOR_ATT), Color.class);

			return new NodeConfiguration(id, delay, color);
		}

		@Override
		public JsonElement serialize(NodeConfiguration nodeConfiguration, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();

			jsonObject.addProperty(ID_ATT, nodeConfiguration.getLogicalId());
			jsonObject.addProperty(DELAY_ATT, nodeConfiguration.getDelay());
			jsonObject.add(COLOR_ATT, context.serialize(nodeConfiguration.getColor()));

			return jsonObject;
		}

	}

	private static final class ColorSerializer implements JsonDeserializer<Color>, JsonSerializer<Color> {

		private static final String RED_ATT = "red";
		private static final String GREEN_ATT = "green";
		private static final String BLUE_ATT = "blue";

		@Override
		public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = json.getAsJsonObject();

			byte red = jsonObject.get(RED_ATT).getAsByte();
			byte green = jsonObject.get(GREEN_ATT).getAsByte();
			byte blue = jsonObject.get(BLUE_ATT).getAsByte();

			return ColorFactory.createColor(red, green, blue);
		}

		@Override
		public JsonElement serialize(Color color, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();

			jsonObject.addProperty(RED_ATT, color.getRed());
			jsonObject.addProperty(GREEN_ATT, color.getGreen());
			jsonObject.addProperty(BLUE_ATT, color.getBlue());

			return jsonObject;
		}
	}

}
