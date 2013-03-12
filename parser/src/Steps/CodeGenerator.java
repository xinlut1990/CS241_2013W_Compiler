package Steps;

import java.util.List;

import DataStructures.ControlFlowGraph;
import DataStructures.Instruction;
import DataStructures.Result;
import DataStructures.VariableManager;

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
	
	private int[] buf;
	private int pc;
	private int fp;
	
	private ControlFlowGraph cfg = null;
	public CodeGenerator(RegisterAllocator ra) {
		this.cfg = ra.getCfg();
		this.pc = 0;
		this.buf = new int[ControlFlowGraph.getInstList().size() + 1000];
		this.fp = ControlFlowGraph.getInstList().size() * 4;
		
	}
	
	public int[] getProgram() {
		return buf;
	}
	
	public void generateCode() {
		List<Instruction> instList = ControlFlowGraph.getInstList();
		PutF1(ADDI, 28, 0, fp);
		for(int i = 1; i < instList.size(); i ++) {
			int a = 0;
			int b = 0;
			int c = 0;
			//inst.getOperand1()
			switch(instList.get(i).getOperator()) {
			case Instruction.add: 
				this.generateCompInst(instList.get(i), ADD);
				break;
			case Instruction.sub:
				this.generateCompInst(instList.get(i), SUB);
				break;
			case Instruction.mul:
				this.generateCompInst(instList.get(i), MUL);
				break;
			case Instruction.div:
				this.generateCompInst(instList.get(i), DIV);
				break;
			case Instruction.cmp: 
				this.generateCompInst(instList.get(i), CMP);
				break;
			case Instruction.beq: 
				this.generateBranchInst(instList.get(i), BEQ);
				break;
			case Instruction.bge: 
				this.generateBranchInst(instList.get(i), BGE);
				break;
			case Instruction.bgt: 
				this.generateBranchInst(instList.get(i), BGT);
				break;
			case Instruction.ble: 
				this.generateBranchInst(instList.get(i), BLE);
				break;
			case Instruction.blt: 
				this.generateBranchInst(instList.get(i), BLT);
				break;
			case Instruction.bne: 
				this.generateBranchInst(instList.get(i), BNE);
				break;
			case Instruction.bra: 
				c = 27;
				PutF2(RET, a, b, c);
				break;
			case Instruction.read:
				a = VariableManager.getTempRegIn(instList.get(i).getId());
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
				a = VariableManager.getTempRegIn(instList.get(i).getId());
				if(instList.get(i).getOperand1().kind == Result.constant) {
					c = instList.get(i).getOperand1().val;
					PutF1(LDW, a, b, c);
				} else {
					b = instList.get(i).getOperand1().regno;
					PutF1(LDW, a, b, c);
				}
				break;
			case Instruction.store:
				a = instList.get(i).getOperand1().regno;
				b = instList.get(i).getOperand2().regno;
				PutF1(STW, a, b, c);
				break;
			case Instruction.push:
				PutF1(PSH, (pc + 2) * 4, 29, 4);
				break;
			case Instruction.pop:
				PutF1(POP, 27, 29, 4);
				break;
			case Instruction.subroutine:
				c = instList.get(i).getOperand2().block.getAddrOfFirstInst() * 4;
				PutF1(JSR, a, b, c);
				break;
			case Instruction.retrn:
				PutF1(RET, a, b, 31);
				break;
			case Instruction.move:
				if(instList.get(i).getOperand1().kind == Result.constant) {
					a = instList.get(i).getOperand2().regno;
					b = 0;
					c = instList.get(i).getOperand1().val;
					if(a != 0) {
						PutF1(ADDI, a, b, c);
					}
					
				} else if(instList.get(i).getOperand1().kind == Result.var) {
					a = instList.get(i).getOperand2().regno;
					b = 0;
					c = instList.get(i).getOperand1().regno;
					if(a != 0) {
						PutF1(ADD, a, b, c);
					}

				} else if(instList.get(i).getOperand1().kind == Result.branch) {
					
					a = instList.get(i).getOperand2().regno;
					b = 0;
					c = instList.get(i).getOperand1().block.getAddrOfFirstInst() * 4;
					if(a != 0) {
						PutF1(ADDI, a, b, c);
					}
				}
				break;
			case Instruction.adda: 
				a = VariableManager.getTempRegIn(instList.get(i).getId());
				b = instList.get(i).getOperand2().regno;
				if(instList.get(i).getOperand1().kind == Result.constant) {
					c = instList.get(i).getOperand1().val;
					PutF1(ADDI, a, b, c);
				} else {
					c = instList.get(i).getOperand1().regno;
					PutF1(ADD, a, b, c);
				}


				break;
			default:
				break;	
			}
		}
	}
	
	//generate computational instructions
	private void generateCompInst(Instruction inst, int opCode) {
		int a = 0;
		int b = 0;
		int c = 0;
		boolean const1 = false;
		boolean const2 = false;
		a = VariableManager.getTempRegIn(inst.getId());
		if(inst.getOperand1().kind == Result.constant) {
			b = inst.getOperand1().val;
			if(b != 0) {
				const1 = true;
			}
		} else {
			b = inst.getOperand1().regno;
		}
		
		if(inst.getOperand2().kind == Result.constant) {
			c = inst.getOperand2().val;
			if(c != 0) {
				const2 = true;
			}
		} else {
			c = inst.getOperand2().regno;
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
	
	private void generateBranchInst(Instruction inst, int opCode) {
		int a = 0;
		int b = 0;
		int c = 0;
		a = inst.getOperand1().regno;
		c = inst.getOperand2().block.getAddrOfFirstInst() - this.pc;
		PutF2(opCode, a, b, c);
	}

	private void PutF1(int op, int a, int b, int c) {
		System.out.println("instruction " + pc + ": " + op + " "+ a+ " " + b+ " " + c);
		buf[pc++] = op << 26 | a << 21 | b << 16 | c & 0xffff;
		
	}
	
	private void PutF2(int op, int a, int b, int c) {
		System.out.println("instruction " + pc + ": " + op + " "+ a+ " " + b+ " " + c);
		buf[pc++] = op << 26 | a << 21 | b << 16 | c & 0xffff;
		
	}
}
