import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/**
 * CLI entrypoint for the "Realistic" scheduler mode.
 *
 * <p>This keeps the same default programs as the legacy mode but runs them using
 * {@link RealisticScheduler}, which supports blocking/unblocking with semaphores
 * and executes a small instruction set.</p>
 */
public class RealisticOS {
	/**
	 * Runs realistic mode and prompts for any {@code assign X input} operations.
	 */
	public static void main(String[] args) throws IOException {
		Scanner sc = new Scanner(System.in);
		System.out.println("Please enter the value of quantum time:");
		int quantum = Integer.parseInt(sc.nextLine().trim());

		Queue<Program> programs = new LinkedList<>();
		programs.add(new Program(1, "Program_1.txt", 0, 0));
		programs.add(new Program(2, "Program_2.txt", 1, 0));
		programs.add(new Program(3, "Program_3.txt", 4, 0));

		RealisticScheduler scheduler = new RealisticScheduler(programs, quantum, (pid, var) -> {
			System.out.print("P" + pid + " enter value for " + var + ": ");
			return sc.nextLine();
		});

		scheduler.run();
	}
}
