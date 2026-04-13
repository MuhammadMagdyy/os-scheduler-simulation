//import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class OS {
	 String program;
	 Scheduler scheduler;
	 
	 Queue <Program> programs=new LinkedList<Program>();
	
	 
	 public OS(Queue<Program> programs ) throws IOException {
		 Scanner sc = new Scanner(System.in);
		 System.out.println("Please enter the value of quantum time:");
		  
		 int timeSlice= sc.nextInt();
		 this.scheduler = new Scheduler(programs, timeSlice); 
		 
		 sc.close();
}

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
