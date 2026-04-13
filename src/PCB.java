/**
 * Legacy Process Control Block (PCB) used by {@link Scheduler}.
 *
 * <p>This class stores the minimal process metadata needed by the legacy scheduler:
 * state, program counter, and memory boundaries.</p>
 */
public class PCB {
	/**
	 * Process identifier.
	 */
	int processID;
	/**
	 * Process state string (e.g., Ready/Running/Finished) as used by the legacy scheduler.
	 */
	String processState;
	/**
	 * Program counter (number of executed instructions).
	 */
	int PC;
	/**
	 * Memory boundaries [from, to] that contain this process data + instructions in the legacy memory array.
	 */
	int [] memoryBoundaries;
	
	/**
	 * Creates a new PCB instance.
	 *
	 * @param processID process identifier
	 * @param processState state string
	 * @param PC program counter
	 * @param from start of memory segment (inclusive)
	 * @param to end of memory segment (inclusive)
	 */
	public PCB (int processID , String processState , int PC, int from,int to) {
		this.processID = processID;
		this.processState = processState;
		this.PC = PC;
		this.memoryBoundaries = new int [2]; 
		memoryBoundaries[0] = from;
		memoryBoundaries[1] = to;	
	}

}
