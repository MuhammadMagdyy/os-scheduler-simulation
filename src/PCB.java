public class PCB {
	int processID;
	String processState;
	int PC;
	int [] memoryBoundaries;
	
	public PCB (int processID , String processState , int PC, int from,int to) {
		this.processID = processID;
		this.processState = processState;
		this.PC = PC;
		this.memoryBoundaries = new int [2]; 
		memoryBoundaries[0] = from;
		memoryBoundaries[1] = to;	
	}

}
