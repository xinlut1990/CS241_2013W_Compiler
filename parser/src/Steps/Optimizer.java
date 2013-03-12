package Steps;

import java.util.ArrayList;
import java.util.List;


import DataStructures.BasicBlock;
import DataStructures.ControlFlowGraph;
import DataStructures.Instruction;
import DataStructures.Result;
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
				Result toBeReplaced = instList.get(i).getOperand2();
				Result toReplace = instList.get(i).getOperand1();
				//no copy propagation for constant assignments
				if(toReplace.kind != Result.constant) {
					for(int j = 0; j < instList.size(); j++) {
						Result curOperand = instList.get(j).getOperand1();
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
		for(int i = instList.size() - 1; i >= 0; i--) {
			if(instList.get(i).getOperator() == Instruction.move && instList.get(i).getOperand1().kind != Result.constant && instList.get(i).isCopiable()) {
				//remove ssa of temp 
				VariableManager.removeSSA(VariableManager.getSSAByVersion(instList.get(i).getId()));
				//remove use
				Result op1 = instList.get(i).getOperand1();
				Result op2 = instList.get(i).getOperand2();
				op1.ssa.removeUse(op1);
				op2.ssa.removeUse(op2);
				
				this.cfg.removeFromBasicBlock(instList.get(i).getId());
				instList.remove(i);
			}
		}
	}
	
	private void replaceAllUse(Result curOperand, Result toReplace) {
		SSA ssa = curOperand.ssa;
		List<Result> useChain = ssa.getUseChain();
		List<Result> replacedUses = new ArrayList<Result>();
		for(Result use : useChain) {
			replacedUses.add(use);
			toReplace.copy(curOperand);
		}
		
		useChain.removeAll(replacedUses);
		
		for(Result use : replacedUses) {
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
