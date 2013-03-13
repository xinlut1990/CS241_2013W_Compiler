package cs241_compiler;

import java.io.File;
import java.io.IOException;

import DataStructures.Instruction;
import DataStructures.ControlFlowGraph;
import DataStructures.VariableManager;
import Steps.CodeGenerator;
import Steps.Optimizer;
import Steps.Parser;
import Steps.RegisterAllocator;
import Steps.Scanner;

public class test {
	public static void testOutput(int op, int a, int b, int c) {
		String operator = " ";
		switch(op){
		case 0: operator = "ADD";
			break;
		case 1: operator = "SUB";
			break;	
		case 2: operator = "MUL";
			break;
		case 3: operator = "DIV";
			break;
		case 4: operator = "MOD";
			break;
		case 5: operator = "CMP";
			break;
		case 8: operator = "OR";
			break;
		case 9: operator = "AND";
			break;
		case 10: operator = "BIC";
			break;
		case 11: operator = "XOR";
			break;
		case 12: operator = "LSH";
			break;
		case 13: operator = "ASH";
			break;
		case 14: operator = "CHK";
			break;
		case 16: operator = "ADDI";
			break;
		case 17: operator = "SUBI";
			break;
		case 18: operator = "MULI";
			break;
		case 19: operator = "DIVI";
			break;
		case 20: operator = "MODI";
			break;
		case 21: operator = "CMPI";
			break;
		case 24: operator = "ORI";
			break;
		case 25: operator = "ANDI";
			break;
		case 26: operator = "BICI";
			break;
		case 27: operator = "XORI";
			break;
		case 28: operator = "LSHI";
			break;
		case 29: operator = "ASHI";
			break;
		case 30: operator = "CHKI";
			break;
		case 32: operator = "LDW";
			break;
		case 33: operator = "LDX";
		 	break;
		case 34: operator = "POP";
			break;
		case 36: operator = "STW";
			break;
		case 37: operator = "STX";
			break;
		case 38: operator = "PSH";
			break;
		default: 
			break;
		}
		System.out.println(operator + " " + a + " " + b + " " + c);
	}
	
	public static void main(String[] args) {
		for(int i = 1; i < 32; i++) {
			//test programs with dead loops
			if(i == 8 || i == 10 || i == 11 || i == 16 || i == 22 || i == 24 || i == 25) {
				continue;
			}
			String testName = "testprogs/test" + String.format("%03d", i) + ".txt";
			Scanner sc = new Scanner(new File(testName));
			System.out.println(testName);
			try {
				Parser ps = new Parser(sc);
				ps.parse();
				
				Optimizer op = new Optimizer(ps);
				op.optimize();
				
				RegisterAllocator ra = new RegisterAllocator(op);
				ra.RegisterAllocate();
				
				CodeGenerator cg = new CodeGenerator(ra);
				cg.generateCode();
				
				DLX.load(cg.getProgram());
				//DLX.load(cg.getProgram());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}

			try {
				DLX.execute();
				System.out.println("Test " + i + " passed.");
				switch(i) {
				case 12:
					System.out.println("Output should be 3.");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
			}
			
			VariableManager.reset();
			ControlFlowGraph.reset();
			Instruction.reset();
		}

		
	}

}
