package Steps;

import java.util.ArrayList;
import java.util.List;


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
		copyPropagation();
		//commonSubexpressionElimination();
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
		//In-block CSE 
		for(BasicBlock curBB : ControlFlowGraph.getBBList()) {
			List<Instruction> insts = curBB.getInstructions();
			for(int i = insts.size() - 1; i >= 0; i--) {
				for(int j = insts.size() - 1; j >= i + 1; j--) {
					Instruction instI = insts.get(i);
					Instruction instJ = insts.get(j);
					if(instI.getOperator() == instJ.getOperator() 
							&& instI.getOperand1().ssa == instJ.getOperand1().ssa
							&& instI.getOperand2().ssa == instJ.getOperand2().ssa) {
						insts.remove(instJ);
					}
				}
			}
		}
	}
}
