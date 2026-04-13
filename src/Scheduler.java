import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Legacy scheduler implementation (original project version).
 *
 * <p>This class runs the scheduling simulation inside its constructor and prints a verbose trace.
 * It also simulates a fixed-size memory and simple swapping to a text file on disk.</p>
 *
 * <p>For a more OS-like model (blocking/unblocking + instruction execution), see {@link RealisticScheduler}.</p>
 */
public class Scheduler {
	
	/**
	 * Legacy ready queue.
	 */
	Queue<Program> readyQueue = new LinkedList<Program>();
	/**
	 * Legacy finished queue.
	 */
	Queue<Program> finishedQueue = new LinkedList<>();
	/**
	 * Legacy PCB queue.
	 */
	Queue<PCB>pcbQueue =  new LinkedList<>();
	/**
	 * Temporary queue used by the legacy logic.
	 */
	Queue<Program> tmpQueue =  new LinkedList<>();
	/**
	 * Current PCB working reference (legacy).
	 */
	PCB pcb;
	/**
	 * Fixed-size memory array used by the legacy simulation.
	 */
	Object [] memory = new Object [40];
	/**
	 * Current process (program) being executed (legacy).
	 */
	Program process;
	/**
	 * Buffer used during swapping (saved memory segment written to disk).
	 */
	ArrayList <Object> saveMem = new ArrayList<>() ;
	/**
	 * Reference to the disk swap file.
	 */
	File textFile;
	
	/**
	 * Returns a snapshot of the memory array after/while running the legacy simulation.
	 */
	public Object[] getMemorySnapshot() {
		return memory.clone();
	}

	/**
	 * Returns a snapshot of the final disk buffer (what was written to {@code FinalDisk.txt}).
	 */
	public List<Object> getFinalDiskSnapshot() {
		return new ArrayList<>(saveMem);
	}

	
	// memory [40] [p1 0 -- 14] [p2 15 --- 29][p3 30 -- 47]
	/**
	 * Constructs and immediately runs the legacy scheduler simulation.
	 *
	 * @param programs programs queue (arrival order)
	 * @param quantumTime RR time slice
	 */
	public Scheduler(Queue<Program> programs , int quantumTime) throws IOException {
		 int startOfinstructions=0; 
		 int end = 0;
		 int start = 0;
		 int time = 0;
		 int swapped3 = 0;
		 int swapped2 = 0;
		 int initP3 = 3;
		 int firstTimeP3 = 0;
		 int usedOnceP3 = 1;
		 int usedOnceP2 = 1;
		 boolean insertedInReadyQueue = false;
		 int tmp = 0;
		 
		while(readyQueue.size()!= programs.size()) {
			 for(int i = 0; i < quantumTime ; i++) 	 {
				 System.out.println("-------"+"\n"+"At time = "+time+"\n" + "-------" );
				 if(programs.peek() != null) 
	     			 if(programs.peek().arrivalTime== time) { 
	     				 
	     				 process = programs.remove();
	    	     				 if(insertedInReadyQueue) {
	    	     					 end = end + ((process.instruction.size()))+8;  
	    							 if (end > memory.length) {
	    								 swapped3 = process.programID;
	    								 initP3 = swapped3;
	    								 swapped2 = (process.programID) - 1;
	    								 while(pcbQueue.peek().processID!=swapped2)
	    									   pcbQueue.add(pcbQueue.remove());
	    								 pcb = new PCB (process.programID,"Ready",0,pcbQueue.peek().memoryBoundaries[0],(pcbQueue.peek().memoryBoundaries[0]+process.instruction.size()+8)-1);
	    								 pcbQueue.add(pcb);
	    							 }
	    							 else {
	    							 pcb = new PCB(process.programID,"Ready",0,start,end-1);
	    							 pcbQueue.add(pcb);
	    							 memory[start] = "Var 1";     			
	    							 memory[start+1] = "Var 2";			
	    							 memory[start+2] = "Var 3";
	    							 memory[start+3] = pcb.processID;
	    							 memory[start+4] = pcb.processState;
	    							 memory[start+5] = pcb.PC;
	    							 memory[start+6] = pcb.memoryBoundaries[0];
	    							 memory[start+7] = pcb.memoryBoundaries[1];
	    							 startOfinstructions = start+8;   
	    							 process.instructionPointer = startOfinstructions;
	    							 start = end; 
	    							 
	    							 while (pcbQueue.peek().processID != readyQueue.peek().programID)
	    								 pcbQueue.remove(pcbQueue.remove());
	    							 }
	    							 insertedInReadyQueue =false;
	    	     				 }
	    	     				 else {
	    	     					System.out.println("Program_"+process.programID+" arrived at time = "+time);
	    	     					 readyQueue.add(process);
	    	     					 end = end + ((process.instruction.size()))+8;  
	    	     					 if (end > memory.length) {
	    	     						 swapped2 = readyQueue.peek().programID;  
	    	     						 pcb = new PCB (process.programID,"Ready",0,pcbQueue.peek().memoryBoundaries[0],(pcbQueue.peek().memoryBoundaries[0]+process.instruction.size()+8)-1);
	    	     						 pcbQueue.add(pcb);
	    	     						 swapped3 = process.programID;
	    	     					 }
	    	     					 else {
	    	     						 pcb = new PCB(process.programID,"Ready",0,start,end-1);
	    	     						 pcbQueue.add(pcb);
	    	     						 memory[start] = "Var 1";     			
		    							 memory[start+1] = "Var 2";			
		    							 memory[start+2] = "Var 3";
	    	     						 memory[start+3] = pcb.processID;
	    	     						 memory[start+4] = pcb.processState;
	    	     						 memory[start+5] = pcb.PC;
	    	     						 memory[start+6] = pcb.memoryBoundaries[0];
	    	     						 memory[start+7] = pcb.memoryBoundaries[1];
	    	     						 startOfinstructions = start+8;   
	    	     						 process.instructionPointer = startOfinstructions;
	    	     						 start = end; 									
						 }	 
	     			 }
	     			 }
	     		
	     			 else {
	     				 System.out.println("Programs queue is empty");
	     			 }
				 
				 
	     		 if(this.readyQueue.peek() == null)   
	     			 break;
	     		 process = readyQueue.peek(); 
	     		 System.out.println("ReadyQueue peek is program " + process.programID );
	     		 System.out.println("Program_"+process.programID+" is currently executing");
	     		 
	     		 while(process.programID != pcbQueue.peek().processID) {
	     			 pcbQueue.add(pcbQueue.remove());
	     		 }
	     		
	     		 if(  tmp == 1 && finishedQueue.peek() != null && finishedQueue.size() == 2) {
					 
					 
					  
					 if(process.instruction.peek() != null) {
					 
					 String word1 =  process.instruction.remove();
					 memory[process.instructionPointer]=word1;
					 
					 System.out.println(process.instructionPointer);
					 
					 System.out.println(memory[process.instructionPointer]);
					 
					 
					 time++;
					 (pcbQueue.peek().PC) ++;
					 memory[pcbQueue.peek().memoryBoundaries[0]+5] = pcbQueue.peek().PC;
					 }
				 }
	     		 else {
	     		 
	     		if (process.programID == swapped3) {
	     			
	     			if (usedOnceP3 == 1) {
	     			 while(pcbQueue.peek().processID != swapped2) {
		     				pcbQueue.add(pcbQueue.remove());
		     			 }
	     			int s = 0;   
	     	//		saveMem2 =  new ArrayList <> () ;	
	     //			saveMem = new ArrayList <> () ;	
	     			for (int j = pcbQueue.peek().memoryBoundaries[0]; j<pcbQueue.peek().memoryBoundaries[1]+1; j++) {
	     				saveMem.add(s,memory[j]);
	     				 s++;
	     			}
	     			 while(process.programID != pcbQueue.peek().processID) {
		     			 pcbQueue.add(pcbQueue.remove());
		     		 }
	     			
	     			if (firstTimeP3 > 0) { 
		     			loadMemory(pcbQueue.peek().memoryBoundaries[0],pcbQueue.peek().memoryBoundaries[1]);
		     			System.out.println("Load program_" + pcbQueue.peek().processID + " to memory");
		     			}
	     			
	     			while(pcbQueue.peek().processID != swapped2) {
	     				pcbQueue.add(pcbQueue.remove());
	     			 }
	     			
	     			loadDisk(pcbQueue.peek().memoryBoundaries[0],pcbQueue.peek().memoryBoundaries[1]);
	     			System.out.println("Load program_" + pcbQueue.peek().processID + " in Disk");
	     			
	     			 while(process.programID != pcbQueue.peek().processID) {
		     			 pcbQueue.add(pcbQueue.remove());
		     		 }
	     			}
	     			usedOnceP3++;
	     			firstTimeP3++;
	     		 }
	     		
	     		 
	     		if (process.programID == initP3) {
	     				
	     			while(pcbQueue.peek().processID != process.programID) {
	     				pcbQueue.add(pcbQueue.remove());
	     			 }
	     			
	     				 start = pcbQueue.peek().memoryBoundaries[0];
	     				 end =  pcbQueue.peek().memoryBoundaries[1];
	     				 memory[start] = "Var 1";     			
						 memory[start+1] = "Var 2";			
						 memory[start+2] = "Var 3";
	     				 memory[start+3] = pcbQueue.peek().processID;
	     				 memory[start+4] = pcbQueue.peek().processState;
	     				 memory[start+5] = pcbQueue.peek().PC;
	     				 memory[start+6] = pcbQueue.peek().memoryBoundaries[0];
	     				 memory[start+7] = pcbQueue.peek().memoryBoundaries[1];
	     				 startOfinstructions = start+8;   
	     				 process.instructionPointer = startOfinstructions;
	     				 start = end; 	
	     				 initP3++;
	     		 }
	     		 
	     		 if (process.programID == swapped2 && swapped3!=0) {
	     			if (usedOnceP2 == 1) {
		     			 while(pcbQueue.peek().processID != swapped3) {
			     				pcbQueue.add(pcbQueue.remove());
			     			 }
		     			int s = 0;   
		     		//	saveMem3 =  new ArrayList <> () ;
		     		//	saveMem = new ArrayList <> () ;
		     			for (int j = pcbQueue.peek().memoryBoundaries[0]; j<pcbQueue.peek().memoryBoundaries[1]+1; j++) {
		     				 
		     				saveMem.add(s,memory[j]);
		     				 s++;
		     			}
		     			
		     			 while(process.programID != pcbQueue.peek().processID) {
			     			 pcbQueue.add(pcbQueue.remove());
			     		 }
		     			
		     			loadMemory(pcbQueue.peek().memoryBoundaries[0],pcbQueue.peek().memoryBoundaries[1]);
		     			System.out.println("Load program_" + pcbQueue.peek().processID + " to memory");
		     			
		     			
		     			while(pcbQueue.peek().processID != swapped3) {
		     				pcbQueue.add(pcbQueue.remove());
		     			 }
		     			
		     			loadDisk(pcbQueue.peek().memoryBoundaries[0],pcbQueue.peek().memoryBoundaries[1]);
		     			System.out.println("Load program_" + pcbQueue.peek().processID + " in Disk");
		     		
		     			 while(process.programID != pcbQueue.peek().processID) {
			     			 pcbQueue.add(pcbQueue.remove());
			     		 }
		     			usedOnceP2++;
	     			}
	     		 }
	     		while(process.programID != pcbQueue.peek().processID) {
	     			 pcbQueue.add(pcbQueue.remove());
	     		 }
	     		 
	     		 pcbQueue.peek().processState = "Running"; 
				 memory[pcbQueue.peek().memoryBoundaries[0]+4] = pcbQueue.peek().processState;
				 
				 System.out.println("Program_"+pcbQueue.peek().processID+ " is "+memory[pcbQueue.peek().memoryBoundaries[0]+4]);
				 
				 String word =  process.instruction.remove();
				 memory[process.instructionPointer]=word;
				 
				 System.out.println(process.instructionPointer);
				 
				 System.out.println(memory[process.instructionPointer]);
				 
				 (process.instructionPointer)++;
				 time++;
				 (pcbQueue.peek().PC) ++;
				 memory[pcbQueue.peek().memoryBoundaries[0]+5] = pcbQueue.peek().PC;
				 
				 System.out.println("Program_" +pcbQueue.peek().processID+" executed " +pcbQueue.peek().PC+ " instructions");
				 
				 if(  i == 1 && finishedQueue.peek() != null && finishedQueue.size() == 2) 
				       tmp++;
	     		 }
				
				 
				 if(process.instruction.isEmpty()) {   
					 while(process.programID != pcbQueue.peek().processID) {
		     			 pcbQueue.add(pcbQueue.remove());
		     		 }
					 pcbQueue.peek().processState = "Finished";
					 memory[pcbQueue.peek().memoryBoundaries[0]+4] = pcbQueue.peek().processState;
					 
					 System.out.println("Program_"+pcbQueue.peek().processID+ " is "+memory[pcbQueue.peek().memoryBoundaries[0]+4]);
					 finishedQueue.add(this.readyQueue.remove());  
					 
					 if(readyQueue.peek() != null)
					 readyQueue.add(readyQueue.remove()); 

					 break;
	     		 }
				 
				 if(programs.peek()!=null) {
					 if (programs.peek().arrivalTime == time) {
						 System.out.println("Program_"+programs.peek().programID+" arrived at time = "+time);
						 readyQueue.add(programs.peek());
					 	 insertedInReadyQueue = true;
					 }
			 }
				 
			 }
			 displayMemory();
			 usedOnceP3 = 1;
			 usedOnceP2 = 1;
			 System.out.println("------------------------------");
			 
			 if(readyQueue.peek() == null)
		     		continue;
			 
		     readyQueue.add(readyQueue.remove()); 
			     
			
		     if (pcbQueue.peek() == null){
		    	    continue;
		     }
		     if(pcbQueue.peek().processState == "Running") {
		     pcbQueue.peek().processState = "Ready";
		     memory[pcbQueue.peek().memoryBoundaries[0]+4] = pcbQueue.peek().processState;
		     }
		     pcbQueue.add(pcbQueue.remove());
		}
		
		System.out.println("All processes are finished :)");
		displayDisk();
		
	}
	
	/**
	 * Prints the full memory array to stdout (legacy debug/trace helper).
	 */
	public void displayMemory () {
 		for (int i =0 ; i<memory.length;i++) {
		 	System.out.println( memory[i]+ " ");

 		}
	}

	/**
	 * Writes the saved swap buffer to {@code FinalDisk.txt} (legacy).
	 */
	public void displayDisk() {
		try {
		try {
			BufferedWriter writer1 = new BufferedWriter (new FileWriter("FinalDisk.txt"));

			for (int i = 0 ; i<saveMem.size();i++) {
				writer1.write(saveMem.get(i)+"\n");
			}
			writer1.close();
             }
  
		catch(NullPointerException e)
		{
			System.out.print("");
		}					
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Loads the contents of {@code Disk.txt} into memory between indices {@code x..y} (inclusive).
	 */
	public  void loadMemory (int x, int y) {  
			try {
				
				BufferedReader reader = new BufferedReader (new FileReader("Disk.txt"));
				String data;
				
				while((data = reader.readLine())!=null && x<=y) {
					memory[x] = data;
					x++;
				}
				reader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} 	

 		
		/**
		 * Writes the current {@link #saveMem} buffer to {@code Disk.txt} to simulate swapping.
		 */
 		public void loadDisk (int x, int y) { 
 			try {
 			    textFile = new File("Disk.txt"); 
 				BufferedWriter writer = new BufferedWriter (new FileWriter(textFile));
 				int s = 0;
 				
 				for (int i = x ; i<=y ; i++) {  
 					writer.write(saveMem.get(s)+"\n");
 					s++;
 				}
 				writer.close();		
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 		}
 		
		/**
		 * Unused legacy main.
		 */
 		public static void main (String [] args) {
 			
 		}
 		
 	}



