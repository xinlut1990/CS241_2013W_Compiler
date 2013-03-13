package DataStructures;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
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

	private PrintWriter writer;
	private Scanner scanner;
	//intermediate instruction list of the program
	private static List<Instruction> instList = new ArrayList<Instruction>();
	
	public ControlFlowGraph(Scanner scanner) {
		this.scanner = scanner;
		this.firstBlock = new BasicBlock(new ArrayList<Boolean>());
		try{
			writer = new PrintWriter(new FileWriter("cfg.vcg"));
		} catch (IOException ex) {
			System.out.println(ex.getMessage());
		}
		
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
	
	//functions used to print visualized graph
	
	public void printCFG() {
		writer.println("graph: { title: \"Control Flow Graph\"");
		writer.println("layoutalgorithm: dfs");
		writer.println("manhattan_edges: yes");
		writer.println("smanhattan_edges: yes");
		for(BasicBlock bb : BBList) {
			if(bb != null) {
				printNode(bb);
			}
		}
		writer.println("}");
		writer.close();
	}
	
	public void printDominatorTree() {
		writer.println("graph: { title: \"Dominator Tree\"");
		writer.println("layoutalgorithm: dfs");
		writer.println("manhattan_edges: yes");
		writer.println("smanhattan_edges: yes");
		for(BasicBlock bb : BBList) {
			if(bb != null) {
				printDominatorNode(bb);
			}
		}
		writer.println("}");
		writer.close();
	}
	
	private void printDominatorNode(BasicBlock bb) {
		writer.println("node: {");
		writer.println("title: \"" + bb.getId() + "\"");
		writer.println("label: \"" + bb.getId() + "[");
		writer.println("]\"");
		writer.println("}");
		
		for(BasicBlock dominatedBlock : bb.getDominatedBlocks()) {
			writer.println("edge: { sourcename: \"" + bb.getId() + "\"");
			writer.println("targetname: \"" + dominatedBlock.getId() + "\"");
			writer.println("color: blue");
			writer.println("}");
		}
		
	}
	
	private void printNode(BasicBlock bb) {
		writer.println("node: {");
		writer.println("title: \"" + bb.getId() + "\"");
		writer.println("label: \"" + bb.getId() + "[");
		List<Instruction> instList = bb.getInstructions();
		for(Instruction inst : instList) {
			this.printInstruction(inst);
		}
		writer.println("]\"");
		writer.println("}");
		
		if(bb.getDirectSuccessor() != null) {
			writer.println("edge: { sourcename: \"" + bb.getId() + "\"");
			writer.println("targetname: \"" + bb.getDirectSuccessor().getId() + "\"");
			writer.println("color: blue");
			writer.println("}");
		}
		
		if(bb.getElseSuccessor() != null) {
			writer.println("edge: { sourcename: \"" + bb.getId() + "\"");
			writer.println("targetname: \"" + bb.getElseSuccessor().getId() + "\"");
			writer.println("color: blue");
			writer.println("}");
		}
		
		if(bb.getBackSuccessor() != null) {
			writer.println("edge: { sourcename: \"" + bb.getId() + "\"");
			writer.println("targetname: \"" + bb.getBackSuccessor().getId() + "\"");
			writer.println("color: blue");
			writer.println("}");
		}
		
		if(bb.getJoinSuccessor() != null && bb.getDirectSuccessor() == null) {
			writer.println("edge: { sourcename: \"" + bb.getId() + "\"");
			writer.println("targetname: \"" + bb.getJoinSuccessor().getId() + "\"");
			writer.println("color: blue");
			writer.println("}");
		}
		
	}
	
	private String operandToString(Operand operand) {
		String result = "";
		if(operand != null) {
			if(operand.kind == Operand.constant) {
				result = String.valueOf(operand.val);
			} else if(operand.kind == Operand.branch) {
				result = "[" + operand.block.getId() + "]";
			} else if(operand.kind == Operand.condition) {
				result = "(" + operand.ssa.getVersion() + ")";
			} else if(operand.kind == Operand.var) {
				int id = operand.ssa.getIdentifier();
				if(id == -1) {
					result += "(" + operand.ssa.getVersion() + ")" ;
				} else {
					result += scanner.identTable.get(id)
							+ "_" + operand.ssa.getVersion();
				}
				if(operand.regno != 0) {
					result += " R" + operand.regno;
				}
			} else if(operand.kind == Operand.reg) {
				result = "R" + operand.regno;
			} else if(operand.kind == Operand.array) {
				result = "(" + operand.ssa.getVersion() + ")" ;
			}
		}
		return result;
	}
	
	private void printInstruction(Instruction inst) {
		if(inst.getOperator() == Instruction.phi) {
			String phiResult = null;
			if(inst.getOperand1().ident != -1) {
				phiResult = scanner.identTable.get(inst.getOperand1().ident) + "_" + inst.getId();
			} else {
				phiResult = "(" + inst.getId() + ")";
			}
			writer.println(inst.getId() + " : " 
					+ getOperatorName(inst.getOperator()) + " " 
								+ phiResult  + " := " + operandToString(inst.getOperand1()) + " " 
					+ operandToString(inst.getOperand2()) + " ,");
		} else {
			writer.println(inst.getId() + " : " 
					+ getOperatorName(inst.getOperator()) + " " 
								+ operandToString(inst.getOperand1()) + "  " 
					+ operandToString(inst.getOperand2()) + " ,");
		}	
	}
	
	private String getOperatorName(int operator) {
		String result ="";
		switch (operator) {
		case Instruction.neg:
			result = "neg";
			break;
		case Instruction.add:
			result = "add";
			break;
		case Instruction.sub:
			result = "sub";
			break;
		case Instruction.mul:
			result = "mul";
			break;
		case Instruction.div:
			result = "div";
			break;
		case Instruction.cmp:
			result = "cmp";
			break;
		case Instruction.adda:
			result = "adda";
			break;
		case Instruction.load:
			result = "load";
			break;
		case Instruction.store:
			result = "store";
			break;
		case Instruction.move:
			result = "move";
			break;
		case Instruction.phi:
			result = "PHI";
			break;
		case Instruction.end:
			result = "end";
			break;
		case Instruction.bra:
			result = "bra";
			break;
		case Instruction.bne:
			result = "bne";
			break;
		case Instruction.beq:
			result = "beq";
			break;
		case Instruction.ble:
			result = "ble";
			break;
		case Instruction.blt:
			result = "blt";
			break;
		case Instruction.bge:
			result = "bge";
			break;
		case Instruction.bgt:
			result = "bgt";
			break;
		case Instruction.read:
			result = "read";
			break;
		case Instruction.write:
			result = "write";
			break;
		case Instruction.wln:
			result = "wln";
			break;
		case Instruction.push:
			result = "push";
			break;
		case Instruction.pop:
			result = "pop";
			break;
		default:
			break;
		}

		return result;
	}
}
