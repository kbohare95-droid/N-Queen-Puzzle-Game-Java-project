import java.util.Arrays;

/**
 * Custom array-backed Stack ADT (LIFO) built from scratch for QueenMove objects.
 *
 * Used in two core parts of this DSA project:
 * 1. Undo/Redo move history during interactive N-Queen puzzle gameplay.
 * 2. Explicit call-stack simulation in the iterative backtracking solver.
 *
 * Time Complexity:
 * - push: amortized O(1) (doubles capacity on overflow)
 * - pop / peek / isEmpty / size: O(1)
 */
public class MoveStack {
    private static final int DEFAULT_CAPACITY = 16;

    private QueenMove[] data;
    private int top;

    public MoveStack() {
        this(DEFAULT_CAPACITY);
    }

    public MoveStack(int initialCapacity) {
        int capacity = Math.max(4, initialCapacity);
        this.data = new QueenMove[capacity];
        this.top = -1;
    }

    public void push(QueenMove move) {
        if (move == null) {
            throw new IllegalArgumentException("Cannot push null move onto stack.");
        }
        if (top + 1 == data.length) {
            resize(data.length * 2);
        }
        data[++top] = move;
    }

    public QueenMove pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Stack underflow: cannot pop from an empty MoveStack.");
        }
        QueenMove removed = data[top];
        data[top] = null; // Avoid loitering
        top--;
        return removed;
    }

    public QueenMove peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot peek at an empty MoveStack.");
        }
        return data[top];
    }

    public boolean isEmpty() {
        return top == -1;
    }

    public int size() {
        return top + 1;
    }

    public void clear() {
        Arrays.fill(data, 0, top + 1, null);
        top = -1;
    }

    /**
     * Returns moves ordered from bottom (oldest) to top (most recent).
     */
    public QueenMove[] toArray() {
        return Arrays.copyOf(data, top + 1);
    }

    private void resize(int newCapacity) {
        data = Arrays.copyOf(data, newCapacity);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i <= top; i++) {
            sb.append(data[i]);
            if (i < top) {
                sb.append(" -> ");
            }
        }
        sb.append("] (top)");
        return sb.toString();
    }
}
