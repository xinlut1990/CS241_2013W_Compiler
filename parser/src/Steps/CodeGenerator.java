package Steps;

import java.util.List;

import VariableManagement.VariableManager;

import ControlFlowGraph.ControlFlowGraph;
import DataStructures.Instruction;
import DataStructures.Operand;

public class CodeGenerator {
	//opcode
	private static final int ADD = 0;
	private static final int SUB = 1;
	private static final int MUL = 2;
	private static final int DIV = 3;
	private static final int MOD = 4;
	private static final int CMP = 5;
	private static final int OR = 8;
	private static final int AND = 9;
	private static final int BIC = 10;
	private static final int XOR = 11;
	
	private static final int LSH = 12;
	private static final int ASH = 13;
	
	private static final int CHK = 14;
	
	private static final int ADDI = 16;
	private static final int SUBI = 17;
	private static final int MULI = 18;
	private static final int DIVI = 19;
	private static final int MODI = 20;
	private static final int CMPI = 21;
	private static final int ORI = 24;
	private static final int ANDI = 25;
	private static final int BICI = 26;
	private static final int XORI = 27;
	
	private static final int LSHI = 28;
	private static final int ASHI = 29;
	
	private static final int CHKI = 30;
	
	private static final int LDW = 32;
	private static final int LDX = 33;
	private static final int POP = 34;
	private static final int STW = 36;
	private static final int STX = 37;
	private static final int PSH = 38;
	
	private static final int BEQ = 40;
	private static final int BNE = 41;
	private static final int BLT = 42;
	private static final int BGE = 43;
	private static final int BLE = 44;
	private static final int BGT = 45;
	
	private static final int BSR = 46;
	private static final int JSR = 48;
	private static final int RET = 49;
	
	private static final int RDI = 50;
	private static final int WRD = 51;
	private static final int WRH = 52;
	private static final int WRL = 53;
	
	private static final int WORDLEN = 4;
	private int[] buf;
	private int pc;
	private int fp;
	
	private ControlFlowGraph cfg = null;
	public ControlFlowGraph getCfg() {
		return cfg;
	}

	public CodeGenerator(RegisterAllocator ra) {
		this.cfg = ra.getCfg();
		this.pc = 0;
		this.buf = new int[cfg.getInstList().size() + 1000];
		this.fp = cfg.getInstList().size() * WORDLEN;
		
	}
	
	public int[] getProgram() {
		return buf;
	}
	
	public void generateCode() {
		List<Instruction> instList = this.cfg.getInstList();
		PutF1(ADDI, 28, 0, fp);
		for(int i = 1; i < instList.size(); i ++) {
			
			//initialize operands
			int a = 0;
			int b = 0;
			int c = 0;
			
			Instruction inst = instList.get(i);
			int instId = inst.getId();
			Operand operand1 = inst.getOperand1();
			Operand operand2 = inst.getOperand2();
			
			switch(inst.getOperator()) {
			case Instruction.add: 
				this.generateCompInst(instId, ADD, operand1, operand2);
				break;
			case Instruction.sub:
				this.generateCompInst(instId, SUB, operand1, operand2);
				break;
			case Instruction.mul:
				this.generateCompInst(instId, MUL, operand1, operand2);
				break;
			case Instruction.div:
				this.generateCompInst(instId, DIV, operand1, operand2);
				break;
			case Instruction.cmp: 
				this.generateCompInst(instId, CMP, operand1, operand2);
				break;
			case Instruction.beq: 
				this.generateBranchInst(BEQ, operand1, operand2);
				break;
			case Instruction.bge: 
				this.generateBranchInst(BGE, operand1, operand2);
				break;
			case Instruction.bgt: 
				this.generateBranchInst(BGT, operand1, operand2);
				break;
			case Instruction.ble: 
				this.generateBranchInst(BLE, operand1, operand2);
				break;
			case Instruction.blt: 
				this.generateBranchInst(BLT, operand1, operand2);
				break;
			case Instruction.bne: 
				this.generateBranchInst(BNE, operand1, operand2);
				break;
			case Instruction.bra: 
				c = 27;
				PutF2(RET, a, b, c);
				break;
			case Instruction.read:
				a = VariableManager.getTempRegIn(instId);
				PutF2(RDI, a, b, c);
				break;
			case Instruction.write:
				b = 27;
				PutF2(WRD, a, b, c);
				break;
			case Instruction.wln:
				PutF2(WRL, a, b, c);
				break;
			case Instruction.end:
				PutF2(RET, a, b, c);
				break;
			case Instruction.load:
				a = VariableManager.getTempRegIn(instId);
				if(operand2 != null && operand2.kind == Operand.reg) {
					a = operand2.regno;
				} else {
					a = VariableManager.getTempRegIn(instId);
				}
				if(operand1.kind == Operand.constant) {
					c = operand1.val;
					PutF1(LDW, a, b, c);
				} else {
					b = operand1.regno;
					PutF1(LDW, a, b, c);
				}
				break;
			case Instruction.store:
				a = operand1.regno;
				if(operand2.kind == Operand.constant) {
					c = operand1.val;
					PutF1(STW, a, b, c);
				} else {
					b = operand2.regno;
					PutF1(STW, a, b, c);
				}
				break;
			case Instruction.push:
				PutF1(PSH, (pc + 2) * WORDLEN, 29, WORDLEN);
				break;
			case Instruction.pop:
				PutF1(POP, 27, 29, WORDLEN);
				break;
			case Instruction.subroutine:
				c = operand2.block.getAddrOfFirstInst() * WORDLEN;
				PutF1(JSR, a, b, c);
				break;
			case Instruction.retrn:
				PutF1(RET, a, b, 31);
				break;
			case Instruction.move:
				this.generateMoveInst(operand1, operand2);
				break;
			case Instruction.adda: 
				a = VariableManager.getTempRegIn(instId);
				b = operand2.regno;
				if(operand1.kind == Operand.constant) {
					c = operand1.val;
					PutF1(ADDI, a, b, c);
				} else {
					c = operand1.regno;
					PutF1(ADD, a, b, c);
				}

				break;
			default:
				break;	
			}
		}
	}
	
	private void generateMoveInst(Operand operand1, Operand operand2) {
		int a = 0;
		int b = 0;
		int c = 0;
		
		a = operand2.regno;
		b = 0;
		
		if(operand1.kind == Operand.constant) {
			c = operand1.val;
			if(a != 0) {
				PutF1(ADDI, a, b, c);
			}
		} else if(operand1.kind == Operand.var) {
			c = operand1.regno;
			if(a != 0) {
				PutF1(ADD, a, b, c);
			}

		} else if(operand1.kind == Operand.branch) {
			c = operand1.block.getAddrOfFirstInst() * WORDLEN;
			if(a != 0) {
				PutF1(ADDI, a, b, c);
			}
		} else {
			c = operand1.val;
			if(a != 0) {
				PutF1(ADD, a, b, c);
			}
		}
	}
	
	//generate computational instructions
	private void generateCompInst(int instId, int opCode, Operand operand1, Operand operand2) {
		int a = 0;
		int b = 0;
		int c = 0;
		boolean const1 = false;
		boolean const2 = false;
		
		a = VariableManager.getTempRegIn(instId);
		if(operand1.kind == Operand.constant) {
			b = operand1.val;
			if(b != 0) {
				const1 = true;
			}
		} else {
			b = operand1.regno;
		}
		
		if(operand2.kind == Operand.constant) {
			c = operand2.val;
			if(c != 0) {
				const2 = true;
			}
		} else {
			c = operand2.regno;
		}
		if(a != 0) {
			if(const1) {
				PutF2(opCode + 16, a, c, b);
			} else if(const2) {
				PutF2(opCode + 16, a, b, c);
			} else {
				PutF2(opCode, a, b, c);
			}
		}	

	}
	
	//Generate branch instruction
	private void generateBranchInst(int opCode, Operand operand1, Operand operand2) {
		int a = 0;
		int b = 0;
		int c = 0;
		
		a = operand1.regno;
		c = operand2.block.getAddrOfFirstInst() - this.pc;
		
		PutF2(opCode, a, b, c);
	}

	private void PutF1(int op, int a, int b, int c) {
		//System.out.println("instruction " + pc + ": " + op + " "+ a+ " " + b+ " " + c);
		buf[pc++] = op << 26 | a << 21 | b << 16 | c & 0xffff;
		
	}
	
	private void PutF2(int op, int a, int b, int c) {
		//System.out.println("instruction " + pc + ": " + op + " "+ a+ " " + b+ " " + c);
		buf[pc++] = op << 26 | a << 21 | b << 16 | c & 0xffff;
		
	}
}
