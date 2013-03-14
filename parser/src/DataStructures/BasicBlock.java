package DataStructures;

import java.util.ArrayList;
import java.util.List;


public class BasicBlock {
	
	private static int curId = 0;
	private final int id;
	private List<Boolean> isElses = new ArrayList<Boolean>();
	
	private List<BasicBlock> dominatedBlocks = new ArrayList<BasicBlock>();

	private BasicBlock directSuccessor = null;
	private BasicBlock elseSuccessor = null;
	//used in if-else statement for traversing CFG
	private BasicBlock joinSuccessor = null;
	//used in while statement for traversing CFG
	private BasicBlock backSuccessor = null;
	
	private BasicBlock directPredecessor = null;
	private BasicBlock elsePredecessor = null;

	private PhiFuncManager phiManager = null;

	private List<Instruction> instructions = new ArrayList<Instruction>();
	
	private boolean isLoop = false;

	public boolean isLoop() {
		return isLoop;
	}

	public void setLoop(boolean isLoop) {
		this.isLoop = isLoop;
	}

	public BasicBlock(List<Boolean> isElse) {
		this.isElses = isElse;
		this.id = BasicBlock.curId;
		ControlFlowGraph.addBB(this);
		BasicBlock.curId ++;
		this.phiManager = new PhiFuncManager();
		//this.printBlockTrace();
	}
	
	public boolean getIsElse(int idx) {
		if(isElses.size() != 0) {
			return isElses.get(idx);
		} else {
			return false;
		}
		
	}
	
	public List<Boolean> getIsElses() {
		return isElses;
	}
	
	public int getId() {
		return id;
	}
	
	public void setJoinSuccessor(BasicBlock joinSucc) {
		this.joinSuccessor = joinSucc;
	}
	
	public void setBackSuccessor(BasicBlock backSucc) {
		this.backSuccessor = backSucc;
	}
	
	public void setDirectSuccessor(BasicBlock directSucc) {
		this.directSuccessor = directSucc;
	}
	
	public void setElseSuccessor(BasicBlock elseSucc) {
		this.elseSuccessor = elseSucc;
	}
	
	public BasicBlock getJoinSuccessor() {
		return this.joinSuccessor;
	}
	
	public BasicBlock getBackSuccessor() {
		return this.backSuccessor;
	}
	
	public BasicBlock getDirectSuccessor() {
		return this.directSuccessor;
	}
	
	public BasicBlock getElseSuccessor() {
		return this.elseSuccessor;
	}
	
	public BasicBlock getDirectPredecessor() {
		return directPredecessor;
	}

	public void setDirectPredecessor(BasicBlock directPredecessor) {
		this.directPredecessor = directPredecessor;
	}

	public BasicBlock getElsePredecessor() {
		return elsePredecessor;
	}

	public void setElsePredecessor(BasicBlock elsePredecessor) {
		this.elsePredecessor = elsePredecessor;
	}
	
	public BasicBlock makeDirectSuccessor(boolean isElse) {
		List<Boolean> isElses = new ArrayList<Boolean>(this.isElses);
		isElses.add(0, isElse);
		BasicBlock newBB = new BasicBlock(isElses);
		this.directSuccessor = newBB;
		this.dominates(newBB);
		return this.getDirectSuccessor();
	}
	
	public BasicBlock makeElseSuccessor(boolean isElse) {
		List<Boolean> isElses = new ArrayList<Boolean>(this.isElses);
		isElses.add(0, isElse);
		BasicBlock newBB = new BasicBlock(isElses);
		this.elseSuccessor = newBB;
		this.dominates(newBB);
		return this.getElseSuccessor();
	}
	
	public List<BasicBlock> getDominatedBlocks() {
		return dominatedBlocks;
	}
	
	public PhiFuncManager getPhiManager() {
		return this.phiManager;
	}
	
	public void insertBefore(Instruction before, Instruction insert) {
		for(int i = instructions.size() - 1; i >= 0; i--) {
			if(instructions.get(i) == before) {
				instructions.add(i, insert);
			}
		}
	}
	
	public void addInstruction(ControlFlowGraph cfg, Instruction inst) {
		instructions.add(inst);
		cfg.addInst(inst);
	}
	
	public List<Instruction> getInstructions() {
		return this.instructions;
	}
	
	
	public void generateIntermediateCode(ControlFlowGraph cfg, int operator, Operand operand1, Operand operand2) {
		Instruction inst = new Instruction(operator,operand1, operand2);
		this.addInstruction(cfg, inst);
	}
	
	

	
	public void dominates(BasicBlock dominated) {
		this.dominatedBlocks.add(dominated);
	}
	

	
	
	
//	public void addUseFromPhis() {
//		List<Instruction> phiFunctions = this.getPhiFuncs();
//		for(Instruction phi : phiFunctions) {
//			phi.getOperand1().ssa.addUse(phi.getOperand1());
//			phi.getOperand2().ssa.addUse(phi.getOperand2());
//		}
//		
//	}
	
	//put a move instruction to the end of a block before branch
	public void relocatePhiOperand(Instruction phiOperand) {
		List<Instruction> instList = this.getInstructions();
		if(instList.size() != 0) {
			Instruction lastInst = instList.get(instList.size() - 1);
			if(lastInst.isBranch()) {
				instList.add(instList.size() - 1, phiOperand);
			} else {
				instList.add(phiOperand);
			}
		} else {
			instList.add(phiOperand);
		}	
	}
	
	//get the id of the first instruction for branch
	public int getAddrOfFirstInst() {
		if(this.instructions.size() == 0) {
			BasicBlock nextBlock = this;
			while(nextBlock.instructions.size() == 0) {
				if(nextBlock.getDirectSuccessor() != null) {
					nextBlock = nextBlock.getDirectSuccessor();
				} else {
					nextBlock = nextBlock.getJoinSuccessor();
				}
			}
			return nextBlock.instructions.get(0).getId();
		} else {
			return this.instructions.get(0).getId();
		}
	}
	
	public void replaceInst(Instruction toBeReplaced, Instruction toReplace) {
		int idx = this.instructions.indexOf(toBeReplaced);
		this.instructions.set(idx, toReplace);
	}
	
	
	private void printBlockTrace() {
		System.out.println(this.id);
		for(int i = 0; i < this.isElses.size(); i++) {
			System.out.println(this.isElses.get(i));
		}
		System.out.println();
	}
	
	private void producePhiFuncs(List<Instruction> phiFuncs) {
		instructions.addAll(0, phiFuncs); 
	}
	
	public void updatePhiFunction(Operand x, BasicBlock curBB, List<BasicBlock> joinBlockChain) {
		this.phiManager.updatePhiFunction(x, this, curBB, joinBlockChain);
	}
	
	public void finalizePhiFuncs(ControlFlowGraph cfg, List<BasicBlock> joinBlockChain) {
		List<Instruction> phiFunctions = this.phiManager.finalizePhiFuncs(cfg, this, joinBlockChain);
		if(instructions.size() != 0) {
			//add finished phi function to instruction list and insert before first instruction of current block
			cfg.insertInstsBefore(phiFunctions, instructions.get(0));
		} else {
			cfg.addInsts(phiFunctions);
		}
		this.producePhiFuncs(this.phiManager.getPhiFuncs());		
	}
	
//	public Instruction getLastInstruction() {
//	Instruction last = null;
//	if(this.instructions.size() != 0) {
//		last = this.instructions.get(0);
//	}
//	
//	for(Instruction inst: this.instructions) {
//		if(inst.getId() > last.getId()) {
//			last = inst;
//		}
//	}
//	return last;
//}
}
