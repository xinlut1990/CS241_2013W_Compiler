package DataStructures;

import java.util.ArrayList;
import java.util.List;


public class BasicBlock {
	
	private static final int normal = 1;
	private static final int follow = 1;
	private static final int join = 1;
	private static int curId = 0;
	private final int id;
	private List<Boolean> isElses = new ArrayList<Boolean>();
	private int type;
	
	private List<BasicBlock> dominatedBlocks = new ArrayList<BasicBlock>();

	private BasicBlock directSuccessor = null;
	private BasicBlock elseSuccessor = null;
	//used in if-else statement for traversing CFG
	private BasicBlock joinSuccessor = null;
	//used in while statement for traversing CFG
	private BasicBlock backSuccessor = null;
	
	
	private BasicBlock directPredecessor = null;
	private BasicBlock elsePredecessor = null;


	private List<Instruction> instructions = new ArrayList<Instruction>();
	
	//phi function list
	private List<Instruction> phiFuncs = new ArrayList<Instruction>();
	
	//backup values from two branch direction
	private List<SSA> backup1s = new ArrayList<SSA>();
	private List<SSA> backup2s = new ArrayList<SSA>();
	
	//used for loop
	private List<SSA> backupsBeforeLoop = new ArrayList<SSA>();
	
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
		
		//this.printBlockTrace();
	}
	
	private void printBlockTrace() {
		System.out.println(this.id);
		for(int i = 0; i < this.isElses.size(); i++) {
			System.out.println(this.isElses.get(i));
		}
		System.out.println();
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
	
	private List<Instruction> getPhiFuncs() {
		return phiFuncs;
	}
	
	public void insertBefore(Instruction before, Instruction insert) {
		for(int i = instructions.size() - 1; i >= 0; i--) {
			if(instructions.get(i) == before) {
				instructions.add(i, insert);
			}
		}
	}
	
	public void addInstruction(Instruction inst) {
		instructions.add(inst);
	}
	
	public void addInstruction(int idx, Instruction inst) {
		instructions.add(idx, inst);
	}
	
	public List<Instruction> getInstructions() {
		return this.instructions;
	}
	
	private void producePhiFuncs() {
		instructions.addAll(0, phiFuncs);
	}
	
	private void makePhiFunc(Operand x, SSA backupVar, boolean isElse) {
		this.backupsBeforeLoop.add(backupVar);
		Operand backup = Operand.makeVar(backupVar.getIdentifier());
		backup.ssa = backupVar;
		Operand variation = x.copy();
		
		if(!isElse) {
			this.backup1s.add(x.ssa);
			this.backup2s.add(backupVar);
			this.phiFuncs.add(new Instruction(Instruction.phi, variation, backup));
		} else {
			this.backup1s.add(backupVar);
			this.backup2s.add(x.ssa);
			this.phiFuncs.add(new Instruction(Instruction.phi, backup, variation));
		}	
		
	}
	
	private void setPhiFuncOperand(Operand x, boolean isElse) {
		int idx = 0;
		//find phi function containing x

		if(!isElse) {
			for(int i = 0; i < backup1s.size(); i++) {
				if(backup1s.get(i).getIdentifier() == x.ident) {
					idx = i;
				}
			}
			backup1s.set(idx, x.ssa);
			this.phiFuncs.get(idx).setOperand1(x.copy());
		} else {
			for(int i = 0; i < backup2s.size(); i++) {
				if(backup2s.get(i).getIdentifier() == x.ident) {
					idx = i;
				}
			}
			backup2s.set(idx, x.ssa);
			this.phiFuncs.get(idx).setOperand2(x.copy());
		}
	}
	
	
	public void generateIntermediateCode(int operator, Operand operand1, Operand operand2) {
		Instruction inst = new Instruction(operator,operand1, operand2);
		ControlFlowGraph.addInst(inst);
		this.addInstruction(inst);
	}
	
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
	
	public void dominates(BasicBlock dominated) {
		this.dominatedBlocks.add(dominated);
	}
	
	public SSA findBackup(int id, boolean isElse) {
		if(!isElse) {
			for(int i = 0; i < phiFuncs.size(); i++) {
				if( phiFuncs.get(i).getOperand1().ident == id) {
					return backup1s.get(i);
				}
			}
		} else {
			for(int i = 0; i < phiFuncs.size(); i++) {
				if( phiFuncs.get(i).getOperand1().ident == id) {
					return backup2s.get(i);
				}
			}
		}
		return null;
	}
	
	public boolean phiIscreated(Operand x) {
		for(SSA ssa: backup1s) {
			if(ssa.getIdentifier() == x.ident) {
				return true;
			}
		}
		
		return false;
	}
	
	//Either create or update the corresponding phi function of a variable.
	public void updatePhiFunction(Operand x, BasicBlock curBB, List<BasicBlock> joinBlockChain) {
		//if there exists a join block, need to generate or update phi function
			if(this.phiIscreated(x)) {
				this.setPhiFuncOperand(x, curBB.getIsElse(0));
			} else {
				
				SSA previousBackupVar = null;
				for(int i = 1; i < joinBlockChain.size(); i++) {
					//pass the backup variable of previous block to it
					previousBackupVar = joinBlockChain.get(i).findBackup(x.ident, curBB.getIsElse(i));
					//get last version of a variable with such identifier
					if(previousBackupVar != null) {
						break;
					}

				} 
				
				if(previousBackupVar == null) {
					//don't want the assignment of move. move is before phi.
					previousBackupVar = VariableManager.getSecondLastOf(x.ident);
				}		
				
				//if the outer block is loop, need to use backup2 to fix backuploop
				if(this.isLoop() && curBB.isLoop()) {
					previousBackupVar = curBB.findBackup(x.ident, true);
				}
	
				this.makePhiFunc(x, previousBackupVar, curBB.getIsElse(0));
				
				if(joinBlockChain.size() > 1) {
					List<BasicBlock> newChain = new ArrayList<BasicBlock>(joinBlockChain);
					newChain.remove(0);
					BasicBlock outerJoinBlock = null;
					outerJoinBlock = newChain.get(0);
					outerJoinBlock.updatePhiFunction(x, this, newChain);
				}
			}
	}
	
	
	public void updatePhiFunctionLocalizedGlobal(Operand x, boolean isElse) {
		//if there exists a join block, need to generate or update phi function
			if(this.phiIscreated(x)) {
				this.setPhiFuncOperand(x, isElse);
			} else {
				this.makePhiFunc(x, x.ssa, isElse);
			}
	}
	
	//finalize the phi functions of a join block, pass the outer join block to propagate phi function results
	public void finalizePhiFuncs(List<BasicBlock> joinBlockChain) {
		List<Instruction> phiFunctions = this.getPhiFuncs();
		for(Instruction phi : phiFunctions) {
			
			Operand x = Operand.makeVar(phi.getOperand1().ident);
			
			VariableManager.addAssignment(phi.getId(), x);
			BasicBlock outerJoinBlock = null;
			if(joinBlockChain.size() > 0) {
				outerJoinBlock = joinBlockChain.get(0);
			}
			if(outerJoinBlock != null) {
				outerJoinBlock.updatePhiFunction(x, this, joinBlockChain);
			}

	    }
		if(instructions.size() != 0) {
			//add finished phi function to instruction list and insert before first instruction of current block
			ControlFlowGraph.insertInstsBefore(phiFunctions, instructions.get(0));
		} else {
			ControlFlowGraph.addInsts(phiFunctions);
		}
		this.producePhiFuncs();		
	}
	
	public void renameOldUse(int SSABeforeWhile, int SSABeforePhi) {
		for(int i = 0; i < this.backupsBeforeLoop.size(); i ++) {
			SSA oldDefine = this.backupsBeforeLoop.get(i);
			List<Operand> useChain = oldDefine.getUseChain();
			List<Operand> renamedUses = new ArrayList<Operand>();
			
			List<Instruction> instList = ControlFlowGraph.getInstList();
			for(int j = 0; j < instList.size(); j++) {
				int instId = instList.get(j).getId();
				if(instId >= SSABeforeWhile && instId < SSABeforePhi) {
					Operand use = instList.get(j).getOperand1();
					if(useChain.contains(use) && use.inst != this.phiFuncs.get(i).getId()) {
						renamedUses.add(use);
						use.ssa = VariableManager.getSSAByVersion(this.phiFuncs.get(i).getId());
					}
					use = instList.get(j).getOperand2();
					if(useChain.contains(use) && use.inst != this.phiFuncs.get(i).getId()) {
						renamedUses.add(use);
						use.ssa = VariableManager.getSSAByVersion(this.phiFuncs.get(i).getId());
					}
				}
			}
			
			useChain.removeAll(renamedUses);
			
			for(Operand use : renamedUses) {
				use.ssa.addUse(use);
			}
		}
				
	}
	
//	public void addUseFromPhis() {
//		List<Instruction> phiFunctions = this.getPhiFuncs();
//		for(Instruction phi : phiFunctions) {
//			phi.getOperand1().ssa.addUse(phi.getOperand1());
//			phi.getOperand2().ssa.addUse(phi.getOperand2());
//		}
//		
//	}
	
	public void fixLoopBackup() {
		for(int i = 0; i < this.backup2s.size(); i++) {
			this.backup2s.set(i, this.backupsBeforeLoop.get(i));
			Operand op2 = this.phiFuncs.get(i).getOperand2();
			op2.ssa = this.backupsBeforeLoop.get(i);
		}

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
	
	public Instruction getLastInstruction() {
		Instruction last = null;
		if(this.instructions.size() != 0) {
			last = this.instructions.get(0);
		}
		
		for(Instruction inst: this.instructions) {
			if(inst.getId() > last.getId()) {
				last = inst;
			}
		}
		return last;
	}
	
	public void relocatePhiOperand(Instruction phiOperand) {
		List<Instruction> predeInsts = this.getInstructions();
		if(predeInsts.size() != 0) {
			Instruction lastInst = predeInsts.get(predeInsts.size() - 1);
			if(lastInst.getOperator() >= Instruction.bra && lastInst.getOperator() <= Instruction.bgt) {
				predeInsts.add(predeInsts.size() - 1, phiOperand);
			} else {
				predeInsts.add(phiOperand);
			}
		} else {
			predeInsts.add(phiOperand);
		}
		
	}
	
	public List<SSA> getPhiResults() {
		List<SSA> results = new ArrayList<SSA>();
		for(Instruction phi : this.phiFuncs) {
			results.add(VariableManager.getSSAByVersion(phi.getId()));
		}
		return results;
	}
	
	public List<SSA> getPhiDirects() {
		List<SSA> directs = new ArrayList<SSA>();
		for(Instruction phi : this.phiFuncs) {
			directs.add(phi.getOperand1().ssa);
		}
		return directs;
	}
	
	public List<SSA> getPhiElses() {
		List<SSA> elses = new ArrayList<SSA>();
		for(Instruction phi : this.phiFuncs) {
			elses.add(phi.getOperand1().ssa);
		}
		return elses;
	}
}
