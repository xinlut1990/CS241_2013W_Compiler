package DataStructures;

import java.util.ArrayList;
import java.util.List;

import Steps.Scanner;

public class ControlFlowGraph {
	private BasicBlock firstBlock;
	private List<BasicBlock> firstBlockOfFuncs = new ArrayList<BasicBlock>();

	public List<BasicBlock> getFirstBlockOfFuncs() {
		return firstBlockOfFuncs;
	}

	public void setFirstBlockOfFuncs(List<BasicBlock> firstBlockOfFuncs) {
		this.firstBlockOfFuncs = firstBlockOfFuncs;
	}

	private static List<BasicBlock> BBList = new ArrayList<BasicBlock>();

	private VCGPrinter vcgPrinter;
	//intermediate instruction list of the program
	private static List<Instruction> instList = new ArrayList<Instruction>();
	
	public ControlFlowGraph(Scanner scanner) {
		
		vcgPrinter = new VCGPrinter(scanner, BBList);
		this.firstBlock = new BasicBlock(new ArrayList<Boolean>());
	}
	
	public void printCFG() {
		this.vcgPrinter.printCFG();
	}
	
	public static void reset() {
		BBList = new ArrayList<BasicBlock>();
		instList = new ArrayList<Instruction>();
	}
	
	public static List<BasicBlock> getBBList() {
		return BBList;
	}
	
	public static void addInsts(List<Instruction> inst) {
		instList.addAll(inst);
	}
	
	public static void addInst(Instruction inst) {
		instList.add(inst);
	}
	
	public static List<Instruction> getInstList() {
		return instList;
	}
	
	public static void addBB(BasicBlock bb) {
		BBList.add(bb);
	}
	
	public BasicBlock getFirstBlock() {
		return this.firstBlock;
	}
	
	public void addFirstBlockOfFunc(BasicBlock BB) {
		this.firstBlockOfFuncs.add(BB);
	}
	
	public void updateInstList() {
		instList = new ArrayList<Instruction>();
		traverse(firstBlock);
		for(BasicBlock bb : this.firstBlockOfFuncs) {
			traverse(bb);
		}

		this.rearrangeInstructions();
	}
	
	public void traverse(BasicBlock curBB) {
		
		instList.addAll(curBB.getInstructions());
		
		if(curBB.getJoinSuccessor() != null && curBB.getDirectSuccessor() != null) {
			
			traverse(curBB.getDirectSuccessor());
			
			if(curBB.getElseSuccessor() != curBB.getJoinSuccessor()) {
				traverse(curBB.getElseSuccessor());
			}
			
			traverse(curBB.getJoinSuccessor());
			
		} else {
			
			if(curBB.getDirectSuccessor() != null) {
				traverse(curBB.getDirectSuccessor());
			}
			
			if(curBB.getElseSuccessor() != null) {
				traverse(curBB.getElseSuccessor());
			}

		}
		
	}
	
	private void rearrangeInstructions() {
		
		List<Instruction> instList = ControlFlowGraph.getInstList();
		List<Integer> oldVersions = new ArrayList<Integer>();
		
		for(int i = 0; i < instList.size(); i++) {
			
			Instruction curInst = instList.get(i);
			
			oldVersions.add(curInst.getId());
			curInst.setId(i);
			
			if(curInst.getOperand1() != null) {
				curInst.getOperand1().inst = i;
			} 
			
			if(curInst.getOperand2() != null) {

				curInst.getOperand2().inst = i;
			}
		}
		
		for(int i = 0; i < VariableManager.getSSASize(); i++) {
			int oldVersion = VariableManager.getSSA(i).getVersion();
			for(int j = 0; j < oldVersions.size(); j++) {
				if(oldVersion == oldVersions.get(j)) {
					VariableManager.getSSA(i).setVersion(j);
				}
			}
			
		}
	}
	
	public static BasicBlock findBlockOf(Instruction inst) {
		for(BasicBlock bb : BBList) {
			for(Instruction curInst : bb.getInstructions()) {
				if(curInst == inst) {
					return bb;
				}
			}
		}
		return null;
	}
	
	public static Instruction getInstruction(int id) {
		for(BasicBlock bb : BBList) {
			for(Instruction curInst : bb.getInstructions()) {
				if(curInst.getId() == id) {
					return curInst;
				}
			}
		}
		return null;
	}
	
	public static void insertInstsBefore(List<Instruction> insts, Instruction before) {
		for(int i = instList.size() - 1; i >= 0; i--) {
			if(instList.get(i) == before) {
				instList.addAll(i, insts);
			}
		}
	}
	
	public void removeFromBasicBlock(int idx) {
		for(BasicBlock BB : BBList) {
			List<Instruction> instList = BB.getInstructions();
			for(int i = 0; i < instList.size(); i++) {
				if(instList.get(i).getId() == idx) {
					instList.remove(i);
				}
			}
		}
	}
	
	
}
