package terminal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

final class ExpressionTree {

	private final ExpressionNode expressionRootNode;

	public ExpressionTree(String expression) throws IllegalArgumentException {
		this.expressionRootNode = buildExpressionTree(expression);
	}

	private static ExpressionNode buildExpressionTree(String expression) throws IllegalArgumentException {
		Stack<ExpressionNode> stack = new Stack<>();
		int[] exp = Utils.fromInfixToPostfix(expression);
		for (int value : exp) {
			if (value == Utils.AND_INT_VALUE || value == Utils.OR_INT_VALUE) {
				stack.push(new ExpressionNode(value, stack.pop(), stack.pop()));
			} else {
				stack.push(new ExpressionNode(value));
			}
		}
		return stack.pop();
	}

	public boolean evaluateExpressionTree(boolean[] touchedNodes) {
		return evaluateExpressionTree(expressionRootNode, touchedNodes);
	}

	private boolean evaluateExpressionTree(ExpressionNode node, boolean[] touchedNodes) {
		if (node.isLeaf()) {
			int nodeId = node.getValue();
			return touchedNodes[nodeId];
		} else {
			switch (node.getValue()) {
			case Utils.AND_INT_VALUE: {
				return evaluateExpressionTree(node.getLeft(), touchedNodes) && evaluateExpressionTree(node.getRight(), touchedNodes);
			}
			case Utils.OR_INT_VALUE: {
				return evaluateExpressionTree(node.getLeft(), touchedNodes) || evaluateExpressionTree(node.getRight(), touchedNodes);
			}
			default: {
				return false;
			}
			}
		}
	}

	public static List<Integer> getValuesFromExpression(String expression) {
		List<Integer> result = new LinkedList<>();
		int[] exp = Utils.fromInfixToPostfix(expression);
		for (int value : exp) {
			if (value != Utils.AND_INT_VALUE && value != Utils.OR_INT_VALUE)
				result.add(value);
		}
		return result;
	}

	private static final class ExpressionNode {

		private final int value;
		private final ExpressionNode right;
		private final ExpressionNode left;

		public ExpressionNode(int value) {
			this(value, null, null);
		}

		public ExpressionNode(int value, ExpressionNode right, ExpressionNode left) {
			this.value = value;
			this.right = right;
			this.left = left;
		}

		public int getValue() {
			return value;
		}

		public ExpressionNode getRight() {
			return right;
		}

		public ExpressionNode getLeft() {
			return left;
		}

		public boolean isLeaf() {
			return right == null && left == null;
		}

	}

	private static final class Utils {

		public static final int AND_INT_VALUE = -1;
		public static final int OR_INT_VALUE = -2;
		private static final int OPEN_PARENTHESIS_INT_VALUE = -3;
		private static final int CLOSE_PARENTHESIS_INT_VALUE = -4;

		private Utils() {
		}

		public static int[] fromInfixToPostfix(String expression) throws IllegalArgumentException {
			Stack<Integer> stack = new Stack<>();
			LinkedList<Integer> queue = new LinkedList<>();

			int[] exp = fromStringToIntArray(expression);

			byte index = 0;
			while (index < exp.length) {
				int c = exp[index++];
				if (c >= QSYPacket.MIN_ID_SIZE && c <= QSYPacket.MAX_ID_SIZE) {
					queue.add(c);
				} else if (c == AND_INT_VALUE || c == OR_INT_VALUE) {
					while (!stack.isEmpty() && getPrior(c) <= getPrior(stack.peek())) {
						queue.add(stack.pop());
					}
					stack.push(c);
				} else if (c == OPEN_PARENTHESIS_INT_VALUE) {
					stack.push(c);
				} else if (c == CLOSE_PARENTHESIS_INT_VALUE) {
					int elem = 0;
					while (!stack.isEmpty() && (elem = stack.pop()) != OPEN_PARENTHESIS_INT_VALUE) {
						queue.add(elem);
					}
					if (elem != OPEN_PARENTHESIS_INT_VALUE) {
						throw new IllegalArgumentException("La expresion '" + expression + "' es invalida.");
					}
				} else {
					throw new IllegalArgumentException("La expresion '" + expression + "' es invalida.");
				}
			}
			while (!stack.isEmpty()) {
				int elem;
				if ((elem = stack.pop()) != OPEN_PARENTHESIS_INT_VALUE) {
					queue.add(elem);
				} else {
					throw new IllegalArgumentException("La expresion '" + expression + "' es invalida.");
				}
			}

			return toIntArray(queue);
		}

		private static int getPrior(int c) {
			switch (c) {
			case OR_INT_VALUE: {
				return 1;
			}
			case AND_INT_VALUE: {
				return 2;
			}
			case OPEN_PARENTHESIS_INT_VALUE: {
				return 0;
			}
			default: {
				return -1;
			}
			}
		}

		private static int[] fromStringToIntArray(String expression) throws IllegalArgumentException {
			LinkedList<Integer> queue = new LinkedList<>();
			LinkedList<Integer> number = new LinkedList<>();
			byte index = 0;
			while (index < expression.length()) {
				char value = expression.charAt(index++);
				if (value >= '0' && value <= '9') {
					number.add(value - 48);
				} else {
					if (!number.isEmpty()) {
						int num = fromIntBufferToInt(number);
						if (num >= QSYPacket.MIN_ID_SIZE && num <= QSYPacket.MAX_ID_SIZE) {
							queue.add(num);
							number.clear();
						} else {
							throw new IllegalArgumentException("La expresion '" + expression + "' es invalida.");
						}
					}
					if (value == '&') {
						queue.add(AND_INT_VALUE);
					} else if (value == '|') {
						queue.add(OR_INT_VALUE);
					} else if (value == '(') {
						queue.add(OPEN_PARENTHESIS_INT_VALUE);
					} else if (value == ')') {
						queue.add(CLOSE_PARENTHESIS_INT_VALUE);
					} else if (value == ' ') {

					} else {
						throw new IllegalArgumentException("La expresion '" + expression + "' es invalida.");
					}
				}
			}
			if (!number.isEmpty()) {
				int num = fromIntBufferToInt(number);
				if (num >= QSYPacket.MIN_ID_SIZE && num <= QSYPacket.MAX_ID_SIZE) {
					queue.add(num);
					number.clear();
				}
			}

			return toIntArray(queue);
		}

		private static Integer fromIntBufferToInt(LinkedList<Integer> number) {
			int result = 0;
			byte pow = 0;
			Iterator<Integer> iterator = number.descendingIterator();
			while (iterator.hasNext()) {
				result += Math.pow(10, pow++) * iterator.next();
			}
			return result;
		}

		private static int[] toIntArray(LinkedList<Integer> list) {
			int[] result = new int[list.size()];
			byte i = 0;
			for (int c : list) {
				result[i++] = c;
			}

			return result;
		}
	}

}
