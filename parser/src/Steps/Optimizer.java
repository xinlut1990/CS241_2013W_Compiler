package Steps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import DataStructures.BasicBlock;
import DataStructures.ControlFlowGraph;
import DataStructures.Instruction;
import DataStructures.Operand;
import DataStructures.SSA;
import DataStructures.VariableManager;


public class Optimizer {
	
	private ControlFlowGraph cfg = null; 
	
	public Optimizer(Parser parser) {
		this.cfg = parser.getCfg();
	}
	
	public ControlFlowGraph getCfg() {
		return cfg;
	}
	
	public void optimize() {
		//this.cfg.printDominatorTree();
		copyPropagation();
		commonSubexpressionElimination();
		//this.cfg.printCFG();
	}
	
	private void copyPropagation() {
		List<Instruction> instList = ControlFlowGraph.getInstList();
		for(int i = 0; i < instList.size(); i++) {
			//get each move instruction
			if(instList.get(i).getOperator() == Instruction.move && instList.get(i).isCopiable()) {
				
				Operand toBeReplaced = instList.get(i).getOperand2();
				Operand toReplace = instList.get(i).getOperand1();
				
				//no copy propagation for constant assignments
				//TODO: change this to one using define-use chain
				if(toReplace.kind != Operand.constant) {
					for(int j = 0; j < instList.size(); j++) {
						Operand curOperand = instList.get(j).getOperand1();
						if(curOperand != null) {
							if(curOperand.ssa == toBeReplaced.ssa && curOperand != toBeReplaced) {
								replaceAllUse(curOperand, toReplace);
								curOperand.ssa.removeUse(curOperand);
								toReplace.copy(curOperand);
								curOperand.ssa.addUse(curOperand);
							}
						}
						
						curOperand = instList.get(j).getOperand2();
						if(curOperand != null) {
							if(curOperand.ssa == toBeReplaced.ssa && curOperand != toBeReplaced) {
								replaceAllUse(curOperand, toReplace);
								curOperand.ssa.removeUse(curOperand);
								toReplace.copy(curOperand);
								curOperand.ssa.addUse(curOperand);
							}
						}
					}
				}
				
			}
		}
		
		//remove all the move instructions
		for(int i = instList.size() - 1; i >= 0; i--) {
			if(instList.get(i).getOperator() == Instruction.move && instList.get(i).getOperand1().kind != Operand.constant && instList.get(i).isCopiable()) {
				//remove ssa of temp 
				VariableManager.removeSSA(VariableManager.getSSAByVersion(instList.get(i).getId()));
				//remove use
				Operand op1 = instList.get(i).getOperand1();
				Operand op2 = instList.get(i).getOperand2();
				op1.ssa.removeUse(op1);
				op2.ssa.removeUse(op2);
				
				this.cfg.removeFromBasicBlock(instList.get(i).getId());
				instList.remove(i);
			}
		}
	}
	
	private void replaceAllUse(Operand curOperand, Operand toReplace) {
		SSA ssa = curOperand.ssa;
		
		List<Operand> useChain = ssa.getUseChain();
		//put uses to remove here to avoid concurrent modification
		List<Operand> replacedUses = new ArrayList<Operand>();
		
		for(Operand use : useChain) {
			replacedUses.add(use);
			toReplace.copy(curOperand);
		}
		
		useChain.removeAll(replacedUses);
		
		for(Operand use : replacedUses) {
			use.ssa.addUse(use);
		}
	}
	
	// eliminate same expression
	private void commonSubexpressionElimination() {
		this.localCSE();
		//this.globalCSE();
	}
	
	private void localCSE() {
		//local CSE 
		for(BasicBlock curBB : ControlFlowGraph.getBBList()) {
			List<Instruction> instList = curBB.getInstructions();
			for(int i = instList.size() - 1; i >= 0; i--) {
				for(int j = instList.size() - 1; j > i; j--) {
					Instruction instI = instList.get(i);
					Instruction instJ = instList.get(j);
					//eliminate
					if(this.isIdentical(instI, instJ)) {
						instList.remove(instJ);
						SSA tempToBeReplaced = VariableManager.getSSAByVersion(instJ.getId());
						SSA tempToReplace = VariableManager.getSSAByVersion(instI.getId());
						tempToBeReplaced.replaceAllUse(tempToReplace);
					}
				}
			}
		}
	}
	
	private boolean isIdentical(Instruction instI, Instruction instJ) {
		if(instI.getOperator() == instJ.getOperator() && instI.isComputational()) {
			boolean op1equal = false;
			if(instI.getOperand1().kind == Operand.constant && instJ.getOperand1().kind == Operand.constant) {
				if(instI.getOperand1().val == instJ.getOperand1().val) {
					op1equal = true;
				}
			} else if(instI.getOperand1().kind == Operand.var && instJ.getOperand1().kind == Operand.var) {
				if(instI.getOperand1().ssa == instJ.getOperand1().ssa) {
					op1equal = true;
				}
			}
			
			boolean op2equal = false;
			if(instI.getOperand2().kind == Operand.constant && instJ.getOperand2().kind == Operand.constant) {
				if(instI.getOperand2().val == instJ.getOperand2().val) {
					op2equal = true;
				}
			} else if(instI.getOperand2().kind == Operand.var && instJ.getOperand2().kind == Operand.var) {
				if(instI.getOperand2().ssa == instJ.getOperand2().ssa) {
					op2equal = true;
				}
			}
			
			if(op1equal && op2equal) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	private void globalCSE() {
		this.globalCSERecursive(this.cfg.getFirstBlock());
	}
	
	private Set<BasicBlock> globalCSERecursive(BasicBlock curBB) {
		//all blocks dominated by current block and current block itself
		Set<BasicBlock> subTree = new HashSet<BasicBlock>();
		for(BasicBlock bb : curBB.getDominatedBlocks()) {
			subTree.addAll(globalCSERecursive(bb));
		}
		for(BasicBlock dominatedBlock : subTree) {
			List<Instruction> instListI = curBB.getInstructions();
			List<Instruction> instListJ = dominatedBlock.getInstructions();
			for(int i = instListI.size() - 1; i >= 0; i--) {
				for(int j = instListJ.size() - 1; j >= 0; j--) {
					Instruction instI = instListI.get(i);
					Instruction instJ = instListJ.get(j);
					//eliminate
					if(this.isIdentical(instI, instJ)) {
						instListJ.remove(instJ);
						SSA tempToBeReplaced = VariableManager.getSSAByVersion(instJ.getId());
						SSA tempToReplace = VariableManager.getSSAByVersion(instI.getId());
						tempToBeReplaced.replaceAllUse(tempToReplace);
					}
				}
			}
		}
		subTree.add(curBB);
		return subTree;
	}
	
}
