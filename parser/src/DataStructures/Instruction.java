package DataStructures;

import java.util.ArrayList;
import java.util.List;


//data structure of intermediate instruction
public class Instruction {
	private static int pc = 0;
	private int Id;
	public static final int neg = 0;
	public static final int add = 1;
	public static final int sub = 2;
	public static final int mul = 3;
	public static final int div = 4;
	public static final int cmp = 5;
	
	public static final int adda = 6;
	public static final int load = 7;
	public static final int store = 8;
	public static final int move = 9;
	public static final int phi = 10;
	
	public static final int end = 11;
	public static final int bra = 12;
	public static final int bne = 13;
	public static final int beq = 14;
	public static final int ble = 15;
	public static final int blt = 16;
	public static final int bge = 17;
	public static final int bgt = 18;
	
	public static final int read = 19;
	public static final int write = 20;
	public static final int wln = 21;
	
	public static final int push = 22;
	public static final int pop = 23;
	public static final int subroutine = 24;
	public static final int retrn = 25;
	
	//Each kind of instruction has no more than 2 operands. If some instruction doesn't have 2 operands, leave one to be null
	private int operator; 
	private Operand operand1;
	private Operand operand2;
	
	private boolean copiable = true;
	
	public static void reset(){
		pc = 0;
	}
	
	public Instruction() {
		
	}
	
	public Instruction(int op, Operand operand1, Operand operand2){
		this.operator = op;
		this.operand1 = operand1;
		this.operand2 = operand2;
		//The id of a new instruction is the current value of program counter.
		this.Id = pc;
		if(operand1 != null) {
			this.operand1.inst = this.getId();
			if(operand1.ssa != null) {
				operand1.ssa.addUse(operand1);
			}
		}
		
		if(operand2 != null) {
			this.operand2.inst = this.getId();
			if(operand2.ssa != null) {
				operand2.ssa.addUse(operand2);
			}
		}
		Instruction.pc ++;
	}
	
	public static Instruction noUseInstruction(int op, Operand operand1, Operand operand2){
		Instruction inst = new Instruction();
		inst.operator = op;
		inst.operand1 = operand1;
		inst.operand2 = operand2;
		//The id of a new instruction is the current value of program counter.
		inst.Id = pc;
		if(operand1 != null) {
			inst.operand1.inst = inst.getId();
		}
		
		if(operand2 != null) {
			inst.operand2.inst = inst.getId();
		}
		Instruction.pc ++;
		return inst;
	}
	
	public boolean isBranch() {
		return this.getOperator() >= Instruction.bra && this.getOperator() <= Instruction.bgt;
	}
	
	public boolean isComputational() {
		return this.getOperator() >= Instruction.add && this.getOperator() <= Instruction.adda;
	}
	
	public int getId() {
		return Id;
	}

	public void setId(int id) {
		Id = id;
	}

	public int getOperator() {
		return operator;
	}

	public void setOperator(int operator) {
		this.operator = operator;
	}

	public Operand getOperand1() {
		return operand1;
	}

	public void setOperand1(Operand operand1) {
		this.operand1 = operand1;
		if(operand1 != null) {
			this.operand1.inst = this.getId();
		}
	}

	public Operand getOperand2() {
		return operand2;
	}

	public void setOperand2(Operand operand2) {
		this.operand2 = operand2;
		if(operand2 != null) {
			this.operand2.inst = this.getId();
		}
	}

	public static int getPC() {
		return Instruction.pc;
	}
	
	public void setBranchDest(BasicBlock destBlock) {
		if(this.operand2 == null) {//The case when it's BRA instruction
			this.operand1.block = destBlock;
		} else {
			this.operand2.block = destBlock;
		}	
	}

	public boolean isCopiable() {
		return copiable;
	}

	public void setCopiable(boolean copiable) {
		this.copiable = copiable;
	}
	
	
}
