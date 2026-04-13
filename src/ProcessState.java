/**
 * High-level lifecycle state for a simulated process.
 *
 * <p>Used by the "Realistic" scheduler mode.</p>
 */
public enum ProcessState {
	NEW,
	READY,
	RUNNING,
	BLOCKED,
	FINISHED
}
