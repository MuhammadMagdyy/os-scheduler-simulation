import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A simple binary mutex for the realistic simulation mode.
 *
 * <p>Ownership is tracked by PID, and a FIFO blocked queue is used to decide
 * which process to unblock on {@code semSignal}.</p>
 */
public class Mutex {
	public final String name;
	public Integer ownerPid = null;
	public final Queue<SimProcess> blockedQueue = new ArrayDeque<>();

	public Mutex(String name) {
		this.name = name;
	}

	public boolean isFree() {
		return ownerPid == null;
	}
}
