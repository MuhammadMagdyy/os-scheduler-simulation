import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

/**
 * Legacy program representation loaded from a text file.
 *
 * <p>Each line of the program file is treated as one instruction string and is placed into a FIFO queue.</p>
 *
 * <p>Note: The realistic mode uses {@link SimProcess} instead of this class.</p>
 */
public class Program {
	/**
	 * The instruction stream (FIFO) used by the legacy scheduler.
	 */
	Queue <String> instruction = new LinkedList<String>();

	/**
	 * Arrival time of the program (time unit when it becomes eligible for scheduling).
	 */
	int arrivalTime;

	/**
	 * Program/process identifier.
	 */
	int programID;

	/**
	 * File name/path of the program text.
	 */
	String name;

	/**
	 * The current memory index where the next instruction is stored/executed (legacy model).
	 */
	int instructionPointer;

	/**
	 * Loads the program instructions from the provided file.
	 *
	 * @param programID program identifier
	 * @param name file name/path
	 * @param arrivalTime arrival time (time unit)
	 * @param instructionPointer initial instruction pointer
	 */
	public Program(int programID,String name, int arrivalTime,int instructionPointer) throws FileNotFoundException  {
			this.programID=programID;
			this.arrivalTime=arrivalTime;
			this.name=name;
			this.instructionPointer= instructionPointer;
		 	File file = new File(name);
		    Scanner sc = new Scanner(file);
		 
		    while (sc.hasNextLine())
		     this.instruction.add(sc.nextLine());
		    
		    sc.close();
		
	}
	
}
