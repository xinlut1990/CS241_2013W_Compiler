package cs241_compiler;
import java.io.File;
import java.io.IOException;

import Steps.CodeGenerator;
import Steps.Optimizer;
import Steps.Parser;
import Steps.RegisterAllocator;
import Steps.Scanner;


public final class Compiler {
	private Compiler() {
		
	}
	
	public static void main(String[] args) {
		
		Scanner sc = new Scanner(new File("testprogs/test002.txt"));
		
		Parser ps = new Parser(sc);
		ps.parse();
		
		Optimizer op = new Optimizer(ps);
		op.optimize();
		
		RegisterAllocator ra = new RegisterAllocator(op);
		ra.RegisterAllocate();
		
		CodeGenerator cg = new CodeGenerator(ra);
		cg.generateCode();
		
		DLX.load(cg.getProgram());
		try {
			DLX.execute();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
