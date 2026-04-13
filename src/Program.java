import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class Program {
	Queue <String> instruction = new LinkedList<String>();
	int arrivalTime;
	int programID;
	String name;
	int instructionPointer;
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
