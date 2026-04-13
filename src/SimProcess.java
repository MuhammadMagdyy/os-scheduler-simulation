import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A lightweight process model for the "realistic" simulation mode.
 *
 * <p>This is intentionally separate from the legacy {@link Program}/{@link PCB}
 * implementation to keep the legacy project intact while adding a more OS-like
 * execution model (states, blocking, mutexes, variables, metrics).</p>
 */
public class SimProcess {
	public final int pid;
	public final int arrivalTime;
	public final List<String> instructions;
	public final Map<String, String> variables = new HashMap<>();
	public final int priority;

	public int pc = 0;
	public ProcessState state = ProcessState.NEW;
	public Integer firstRunTime = null;
	public Integer finishTime = null;

	public SimProcess(int pid, int arrivalTime, List<String> instructions) {
		this(pid, arrivalTime, instructions, 0);
	}

	/**
	 * Creates a realistic-mode process.
	 *
	 * @param pid process id
	 * @param arrivalTime arrival time (time unit)
	 * @param instructions instruction list (each entry is one instruction line)
	 * @param priority smaller value = higher priority
	 */
	public SimProcess(int pid, int arrivalTime, List<String> instructions, int priority) {
		this.pid = pid;
		this.arrivalTime = arrivalTime;
		this.instructions = instructions;
		this.priority = priority;
	}

	public boolean isFinished() {
		return pc >= instructions.size();
	}
}
