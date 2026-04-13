//import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/**
 * Legacy CLI entrypoint for the original scheduler implementation.
 *
 * <p>This class preserves the original behavior (hardcoded 3 programs, asks for quantum on stdin).
 * For the UI, run {@link SchedulerUI}. For the more realistic scheduler engine, run {@link RealisticOS}.</p>
 */
public class OS {
	 String program;
	 Scheduler scheduler;
	 
	 Queue <Program> programs=new LinkedList<Program>();
	
	 
	 /**
	  * Creates a legacy OS instance with an explicit quantum time (useful for UI/wrappers).
	  */
	 public OS(Queue<Program> programs, int quantumTime) throws IOException {
		 this.scheduler = new Scheduler(programs, quantumTime);
	 }

	 /**
	  * Creates a legacy OS instance by prompting the user for a quantum time.
	  */
	 public OS(Queue<Program> programs) throws IOException {
		 Scanner sc = new Scanner(System.in);
		 System.out.println("Please enter the value of quantum time:");
		 int timeSlice = sc.nextInt();
		 this.scheduler = new Scheduler(programs, timeSlice);
		 // Do not close System.in; leaving Scanner open intentionally.
	 }

	 /**
	  * Runs the legacy simulation with the default program files in the project root.
	  */
	 public static void main (String [] args) throws IOException {
		    Queue<Program> programs =new LinkedList<Program>();
		    
		 	Program p1 =new Program(1,"Program_1.txt",0,0);
			Program p2 =new Program(2,"Program_2.txt",1,0);
			Program p3 =new Program(3,"Program_3.txt",4,0);

			
			programs.add(p1);
			programs.add(p2);
			programs.add(p3);
			


			@SuppressWarnings("unused")
			OS os = new OS (programs);  
	
}
}
