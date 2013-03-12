package Steps;

import java.util.ArrayList;
import java.util.List;


import DataStructures.BasicBlock;
import DataStructures.Cluster;
import DataStructures.ControlFlowGraph;
import DataStructures.Instruction;
import DataStructures.InterferenceGraph;
import DataStructures.Result;
import DataStructures.SSA;
import DataStructures.VariableManager;

public class RegisterAllocator {
	private boolean[] registers;
	private static int NUM_OF_REGS = 32;
	
	private ControlFlowGraph cfg = null;
	public RegisterAllocator(Optimizer op) {
		
		this.setCfg(op.getCfg());
		
		this.registers = new boolean[NUM_OF_REGS];
		
		//cannot use reg0
		for(int i = 1; i < NUM_OF_REGS; i++) {
			this.registers[i] = false;
		}
	}
	
	public void RegisterAllocate() {
		//rearrange the order of instructions
		List<Instruction> instList = ControlFlowGraph.getInstList();
		
		//instruction for initialization of program environment
		this.cfg.getFirstBlock().getInstructions().add(0, new Instruction(Instruction.move, null, null));
		
		this.cfg.updateInstList();
		//this.cfg.printCFG();
		
		//add move before branch because branch cannot use immediate number
		for(int i = instList.size() - 1; i >= 0; i --) {
			if(instList.get(i).getOperator() == Instruction.bra || instList.get(i).getOperator() == Instruction.write) {
				Result branchReg = Result.makeReg(27);
				Instruction move = new Instruction(Instruction.move, instList.get(i).getOperand2(), branchReg);
				BasicBlock blockOfBranch = this.cfg.findBlockOf(instList.get(i));
				blockOfBranch.insertBefore(instList.get(i), move);
			}
		}

		//no change of instruction from now !!!!!!!!!!!!!!
		
		//this.cfg.printCFG();
		//build interference graph
		InterferenceGraph ig = new InterferenceGraph();
		//ig.buildInterferenceGraph();
		ig.buildIG(this.cfg);
		//ig.printGraph();
		//this.cfg.printCFG();
		
		ig.clustering();
		ig.color();


		//synchronize for operands
		for( Instruction inst : instList) {
			if(inst.getOperand1() != null && inst.getOperand1().ssa != null) {
				inst.getOperand1().regno = inst.getOperand1().ssa.getReg();
			}
			
			if(inst.getOperand2() != null && inst.getOperand2().ssa != null) {
				inst.getOperand2().regno = inst.getOperand2().ssa.getReg();
			}

		}
		this.cfg.printCFG();
		this.resolvePhiConflict();
		
		this.cfg.updateInstList();
		
		this.cfg.printCFG();

	}
	
	private void resolvePhiConflict() {
		List<Instruction> instList = ControlFlowGraph.getInstList();
		for(int i = instList.size() - 1; i >= 0; i --) {
			if(instList.get(i).getOperator() == Instruction.phi) {
				SSA phiSSA = VariableManager.getSSAByVersion(instList.get(i).getId());

				Result phiOperand = Result.makeVar(-1);
				phiOperand.ssa = phiSSA;
				phiOperand.regno = phiSSA.getReg();
				
				BasicBlock blockOfPhi = this.cfg.findBlockOf(instList.get(i));
				//eliminate phi
				blockOfPhi.getInstructions().remove(instList.get(i));
				
				//if the temp variable has been assigned 0 register, it has no use
				if(phiSSA.getReg() != 0) {
					//if the register of operand
					if(phiSSA.getReg() != instList.get(i).getOperand1().regno) {
						Instruction move = new Instruction(Instruction.move, instList.get(i).getOperand1(), phiOperand);

						blockOfPhi.getDirectPredecessor().relocatePhiOperand(move);
						instList.add(move);
					}
					if(phiSSA.getReg() != instList.get(i).getOperand2().regno) {
						Instruction move = new Instruction(Instruction.move, instList.get(i).getOperand2(), phiOperand);

						blockOfPhi.getElsePredecessor().relocatePhiOperand(move);
						instList.add(move);
					}
					if(phiSSA.getReg() == instList.get(i).getOperand1().regno && phiSSA.getReg() == instList.get(i).getOperand2().regno){
						VariableManager.removeSSA(phiSSA);
					}
				}
				
			}
		}
	}

	public ControlFlowGraph getCfg() {
		return cfg;
	}

	public void setCfg(ControlFlowGraph cfg) {
		this.cfg = cfg;
	}
	
	
}
