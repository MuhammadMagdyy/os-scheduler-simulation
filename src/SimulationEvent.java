/**
 * A single structured event emitted by the realistic scheduler.
 *
 * <p>The UI uses this to show a timeline and to export traces for plotting/analysis.</p>
 */
public class SimulationEvent {
	public enum Type {
		ARRIVE,
		CPU_IDLE,
		CPU_DISPATCH,
		INSTRUCTION,
		MUTEX_ACQUIRE,
		MUTEX_RELEASE,
		MUTEX_TRANSFER,
		BLOCK,
		UNBLOCK,
		INPUT_REQUEST,
		INPUT_VALUE,
		OUTPUT,
		FILE_READ,
		FILE_WRITE,
		FINISH,
		DEADLOCK
	}

	public final int time;
	public final Integer pid;
	public final Type type;
	public final String message;

	public SimulationEvent(int time, Integer pid, Type type, String message) {
		this.time = time;
		this.pid = pid;
		this.type = type;
		this.message = message;
	}
}

