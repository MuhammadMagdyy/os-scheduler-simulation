import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * A more OS-like Round Robin scheduler simulation.
 *
 * <p>Key differences vs the legacy implementation:
 * <ul>
 *   <li>Processes can become {@link ProcessState#BLOCKED} via {@code semWait}.</li>
 *   <li>{@code semSignal} unblocks a waiting process (FIFO) and transfers ownership.</li>
 *   <li>A small "instruction set" is executed (assign/input, print, read/write file).</li>
 *   <li>Basic timing metrics are tracked: response/turnaround times.</li>
 * </ul>
 *
 * <p>The engine logs all events to stdout (captured by the UI) and also keeps an
 * in-memory log snapshot for structured UI views.</p>
 */
public class RealisticScheduler {

	/**
	 * Supplies values for {@code assign X input}.
	 *
	 * <p>In CLI mode, this is backed by {@link java.util.Scanner}.
	 * In UI mode, it is backed by a blocking UI input panel.</p>
	 */
	public interface InputProvider {
		String requestInput(int pid, String variableName);
	}

	private final int quantum;
	private final SchedulingAlgorithm algorithm;
	private final Queue<SimProcess> arrivals;
	private final List<SimProcess> allProcesses;
	private final ArrayDeque<SimProcess> readyQueue = new ArrayDeque<>();
	private final List<SimProcess> finished = new ArrayList<>();
	private final Map<String, Mutex> mutexes = new HashMap<>();
	private final InputProvider inputProvider;
	private final int totalProcesses;
	private final List<String> logLines = new ArrayList<>();
	private final List<SimulationEvent> events = new ArrayList<>();

	private int time = 0;

	public RealisticScheduler(Queue<Program> programs, int quantum, InputProvider inputProvider) {
		this(toArrivalsList(programs), quantum, SchedulingAlgorithm.ROUND_ROBIN, inputProvider);
	}

	public RealisticScheduler(Queue<Program> programs, int quantum, SchedulingAlgorithm algorithm, InputProvider inputProvider) {
		this(toArrivalsList(programs), quantum, algorithm, inputProvider);
	}

	public RealisticScheduler(List<SimProcess> processes, int quantum, SchedulingAlgorithm algorithm, InputProvider inputProvider) {
		if (quantum <= 0) throw new IllegalArgumentException("quantum must be > 0");
		this.quantum = quantum;
		this.algorithm = algorithm == null ? SchedulingAlgorithm.ROUND_ROBIN : algorithm;
		this.inputProvider = inputProvider;
		List<SimProcess> initial = new ArrayList<>(processes);
		initial.sort(Comparator.comparingInt(a -> a.arrivalTime));
		this.allProcesses = new ArrayList<>(initial);
		this.totalProcesses = initial.size();
		this.arrivals = new LinkedList<>(initial);

		mutexes.put("userInput", new Mutex("userInput"));
		mutexes.put("userOutput", new Mutex("userOutput"));
		mutexes.put("file", new Mutex("file"));
	}

	/**
	 * Runs the simulation to completion (or until deadlock).
	 *
	 * <p>Time advances by 1 for each executed instruction slot, including a blocked
	 * {@code semWait} attempt. Arrivals are admitted when {@code arrivalTime <= time}.</p>
	 */
	public void run() {
		log("=== Realistic Scheduler ===");
		log("Algorithm = " + algorithm);
		log("Quantum = " + quantum + (algorithm == SchedulingAlgorithm.ROUND_ROBIN ? "" : " (ignored)"));

		while (finished.size() < totalProcesses) {
			admitArrivals();

			if (readyQueue.isEmpty()) {
				if (arrivals.isEmpty() && blockedCount() > 0) {
					log("DEADLOCK at time " + time + " (all processes blocked).");
					event(null, SimulationEvent.Type.DEADLOCK, "all processes blocked");
					break;
				}
				log("t=" + time + " CPU idle");
				event(null, SimulationEvent.Type.CPU_IDLE, "CPU idle");
				time++;
				continue;
			}

			SimProcess process = selectNextReady();
			if (process == null) continue;
			process.state = ProcessState.RUNNING;
			if (process.firstRunTime == null) process.firstRunTime = time;
			log("t=" + time + " -> RUN P" + process.pid);
			event(process.pid, SimulationEvent.Type.CPU_DISPATCH, "dispatch");

			int sliceLimit = (algorithm == SchedulingAlgorithm.ROUND_ROBIN) ? quantum : Integer.MAX_VALUE;
			for (int i = 0; i < sliceLimit; i++) {
				admitArrivals();

				if (process.isFinished()) {
					finish(process);
					break;
				}

				String instruction = process.instructions.get(process.pc).trim();
				if (instruction.isEmpty()) {
					process.pc++;
					time++;
					continue;
				}

				log("t=" + time + " P" + process.pid + " :: " + instruction);
				event(process.pid, SimulationEvent.Type.INSTRUCTION, instruction);
				boolean advancePc = executeInstruction(process, instruction);

				if (advancePc) process.pc++;
				time++;

				if (process.state == ProcessState.BLOCKED) break;
				if (process.state == ProcessState.FINISHED) break;
			}

			if (process.state == ProcessState.RUNNING) {
				process.state = ProcessState.READY;
				if (algorithm == SchedulingAlgorithm.ROUND_ROBIN) {
					readyQueue.addLast(process);
				} else {
					// Non-preemptive algorithms run until block/finish. If we ever reach here,
					// re-queue to avoid losing the process (defensive fallback).
					readyQueue.addFirst(process);
				}
			}
		}

		log("=== Done at time " + time + " ===");
		log("Finished: " + finished.size() + "/" + totalProcesses);
	}

	/**
	 * Returns a snapshot of the simulation log lines.
	 */
	public List<String> getLogLines() {
		return new ArrayList<>(logLines);
	}

	/**
	 * Returns a snapshot of all structured events emitted during the simulation.
	 */
	public List<SimulationEvent> getEvents() {
		return new ArrayList<>(events);
	}

	/**
	 * Returns the processes list (includes finished/blocked/ready processes).
	 *
	 * <p>The returned objects are the live {@link SimProcess} instances; treat as read-only.</p>
	 */
	public List<SimProcess> getAllProcesses() {
		return allProcesses;
	}

	public int getTime() {
		return time;
	}

	/**
	 * Returns a snapshot of the current mutex table.
	 */
	public Map<String, Mutex> getMutexes() {
		return new HashMap<>(mutexes);
	}

	private SimProcess selectNextReady() {
		if (readyQueue.isEmpty()) return null;

		if (algorithm == SchedulingAlgorithm.ROUND_ROBIN || algorithm == SchedulingAlgorithm.FCFS) {
			return readyQueue.removeFirst();
		}

		// For selection-based algorithms, scan the ready queue and remove the chosen process.
		SimProcess best = null;
		for (SimProcess p : readyQueue) {
			if (best == null) {
				best = p;
				continue;
			}
			if (algorithm == SchedulingAlgorithm.SJF) {
				int remP = remainingInstructions(p);
				int remBest = remainingInstructions(best);
				if (remP < remBest || (remP == remBest && tieBreak(p, best) < 0)) best = p;
			} else if (algorithm == SchedulingAlgorithm.PRIORITY) {
				if (p.priority < best.priority || (p.priority == best.priority && tieBreak(p, best) < 0)) best = p;
			}
		}

		if (best != null) {
			readyQueue.remove(best);
		}
		return best;
	}

	private static int remainingInstructions(SimProcess p) {
		return Math.max(0, p.instructions.size() - p.pc);
	}

	private static int tieBreak(SimProcess a, SimProcess b) {
		// Earlier arrival wins; then smaller PID for determinism.
		if (a.arrivalTime != b.arrivalTime) return Integer.compare(a.arrivalTime, b.arrivalTime);
		return Integer.compare(a.pid, b.pid);
	}

	private void admitArrivals() {
		while (!arrivals.isEmpty() && arrivals.peek().arrivalTime <= time) {
			SimProcess p = arrivals.remove();
			p.state = ProcessState.READY;
			readyQueue.addLast(p);
			log("t=" + time + " ARRIVE P" + p.pid);
			event(p.pid, SimulationEvent.Type.ARRIVE, "arrive");
		}
	}

	private void finish(SimProcess p) {
		p.state = ProcessState.FINISHED;
		p.finishTime = time;
		finished.add(p);
		log("t=" + time + " FINISH P" + p.pid);
		event(p.pid, SimulationEvent.Type.FINISH, "finish");
	}

	private int blockedCount() {
		int count = 0;
		for (Mutex m : mutexes.values()) count += m.blockedQueue.size();
		return count;
	}

	private boolean executeInstruction(SimProcess p, String instruction) {
		String[] parts = instruction.split("\\s+");
		String op = parts[0];

		return switch (op) {
			case "semWait" -> {
				requireParts(parts, 2, instruction);
				String mutexName = parts[1];
				yield execSemWait(p, mutexName);
			}
			case "semSignal" -> {
				requireParts(parts, 2, instruction);
				String mutexName = parts[1];
				execSemSignal(p, mutexName);
				yield true;
			}
			case "assign" -> {
				requireParts(parts, 3, instruction);
				String varName = parts[1];
				if ("input".equals(parts[2])) {
					event(p.pid, SimulationEvent.Type.INPUT_REQUEST, "request " + varName);
					String value = inputProvider.requestInput(p.pid, varName);
					event(p.pid, SimulationEvent.Type.INPUT_VALUE, varName + "=" + value);
					p.variables.put(varName, value);
					log("P" + p.pid + " set " + varName + "=" + value);
					yield true;
				}
				if ("readFile".equals(parts[2])) {
					requireParts(parts, 4, instruction);
					String fileVar = parts[3];
					String fileName = p.variables.get(fileVar);
					if (fileName == null) throw new IllegalStateException("P" + p.pid + " variable '" + fileVar + "' is unset");
					String content = readAll(fileName);
					event(p.pid, SimulationEvent.Type.FILE_READ, fileName);
					p.variables.put(varName, content);
					log("P" + p.pid + " set " + varName + "=(readFile " + fileName + ")");
					yield true;
				}
				String literal = joinFrom(parts, 2);
				p.variables.put(varName, literal);
				log("P" + p.pid + " set " + varName + "=" + literal);
				yield true;
			}
			case "print" -> {
				requireParts(parts, 2, instruction);
				String var = parts[1];
				String out = p.variables.getOrDefault(var, "null");
				log("P" + p.pid + " OUTPUT: " + out);
				event(p.pid, SimulationEvent.Type.OUTPUT, out);
				yield true;
			}
			case "printFromTo" -> {
				requireParts(parts, 3, instruction);
				String fromVar = parts[1];
				String toVar = parts[2];
				int from = Integer.parseInt(p.variables.getOrDefault(fromVar, "0"));
				int to = Integer.parseInt(p.variables.getOrDefault(toVar, "0"));
				StringBuilder sb = new StringBuilder();
				for (int x = from; x <= to; x++) {
					if (x != from) sb.append(' ');
					sb.append(x);
				}
				String out = sb.toString();
				log("P" + p.pid + " OUTPUT: " + out);
				event(p.pid, SimulationEvent.Type.OUTPUT, out);
				yield true;
			}
			case "writeFile" -> {
				requireParts(parts, 3, instruction);
				String fileVar = parts[1];
				String contentVar = parts[2];
				String fileName = p.variables.get(fileVar);
				if (fileName == null) throw new IllegalStateException("P" + p.pid + " variable '" + fileVar + "' is unset");
				String content = p.variables.getOrDefault(contentVar, "");
				writeAll(fileName, content);
				log("P" + p.pid + " writeFile " + fileName);
				event(p.pid, SimulationEvent.Type.FILE_WRITE, fileName);
				yield true;
			}
			default -> throw new IllegalArgumentException("Unknown instruction: " + instruction);
		};
	}

	private boolean execSemWait(SimProcess p, String mutexName) {
		Mutex m = mutexes.computeIfAbsent(mutexName, Mutex::new);
		if (m.ownerPid != null && m.ownerPid == p.pid) {
			return true;
		}
		if (m.isFree()) {
			m.ownerPid = p.pid;
			log("P" + p.pid + " acquired " + mutexName);
			event(p.pid, SimulationEvent.Type.MUTEX_ACQUIRE, mutexName);
			return true;
		}
		p.state = ProcessState.BLOCKED;
		m.blockedQueue.add(p);
		log("P" + p.pid + " BLOCKED on " + mutexName + " (owner=P" + m.ownerPid + ")");
		event(p.pid, SimulationEvent.Type.BLOCK, mutexName + " owner=P" + m.ownerPid);
		return false;
	}

	private void execSemSignal(SimProcess p, String mutexName) {
		Mutex m = mutexes.computeIfAbsent(mutexName, Mutex::new);
		if (m.ownerPid == null) return;
		if (m.ownerPid != p.pid) {
			log("P" + p.pid + " ignored semSignal " + mutexName + " (not owner)");
			return;
		}

		if (m.blockedQueue.isEmpty()) {
			m.ownerPid = null;
			log("P" + p.pid + " released " + mutexName);
			event(p.pid, SimulationEvent.Type.MUTEX_RELEASE, mutexName);
			return;
		}

		SimProcess unblocked = m.blockedQueue.remove();
		unblocked.state = ProcessState.READY;
		readyQueue.addLast(unblocked);
		m.ownerPid = unblocked.pid;
		log("P" + p.pid + " passed " + mutexName + " to P" + unblocked.pid);
		event(unblocked.pid, SimulationEvent.Type.UNBLOCK, mutexName);
		event(p.pid, SimulationEvent.Type.MUTEX_TRANSFER, mutexName + " -> P" + unblocked.pid);
	}

	private void log(String line) {
		logLines.add(line);
		System.out.println(line);
	}

	private void event(Integer pid, SimulationEvent.Type type, String message) {
		events.add(new SimulationEvent(time, pid, type, message));
	}

	private static void requireParts(String[] parts, int expectedMin, String instruction) {
		if (parts.length < expectedMin) throw new IllegalArgumentException("Bad instruction: " + instruction);
	}

	private static String joinFrom(String[] parts, int start) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < parts.length; i++) {
			if (i > start) sb.append(' ');
			sb.append(parts[i]);
		}
		return sb.toString();
	}

	private static List<SimProcess> toArrivalsList(Queue<Program> programs) {
		List<SimProcess> list = new ArrayList<>();
		for (Program p : programs) {
			list.add(new SimProcess(p.programID, p.arrivalTime, new ArrayList<>(p.instruction)));
		}
		list.sort(Comparator.comparingInt(a -> a.arrivalTime));
		return list;
	}

	private static String readAll(String fileName) {
		try {
			Path path = Path.of(fileName);
			if (!Files.exists(path)) return "";
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void writeAll(String fileName, String content) {
		try {
			Path path = Path.of(fileName);
			Files.writeString(path, content, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
