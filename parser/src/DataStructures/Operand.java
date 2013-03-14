package DataStructures;

import VariableManagement.SSA;
import ControlFlowGraph.BasicBlock;

//Intermediate result of a expression or operand
public class Operand{
	public static final int unknown = 0;
	public static final int constant = 1;
	public static final int var = 2;//variable
	public static final int reg = 3;
	public static final int condition = 4;//temporary variable to store the result of comparison
	public static final int branch = 5;//the target location of a branch forward
	public static final int array = 6;//array, the SSA of which is actually base address
	
	public int kind = unknown; // type of result
	public int val = 0; //the value of result
	public int regno = 0;
	public BasicBlock block = null; //target block of a branch forward
	public int ident = -2; //identifier of variable, -1 if is temp
	public SSA ssa = null; //SSA variable
	public int cond = 0; //the type of relation operation
	public int inst = -1; //instruction number
	public int fixuplocation = 0; //fixup location
	public Array arr = null;
	
	public static Operand makeVar(int ident) {
		Operand result = new Operand();
		result.kind = var;
		result.ident = ident;
		return result;
	}
	
	public static Operand makeReg(int regno) {
		Operand result = new Operand();
		result.kind = reg;
		result.regno = regno;
		return result;
	}
	
	public static Operand makeConst(int val) {
		Operand result = new Operand();
		result.kind = constant;
		result.val = val;
		return result;
	}
	
	public static Operand makeBranch(BasicBlock block) {
		Operand result = new Operand();
		result.kind = branch;
		result.block = block;
		return result;
	}
	
	public Operand copy() {
		Operand copy = new Operand();
		copy.kind = this.kind;
		copy.val = this.val;
		copy.regno = this.regno;
		copy.block = this.block;
		copy.ident = this.ident;
		copy.ssa = this.ssa;
		copy.cond = this.cond;
		copy.inst = this.inst;
		copy.fixuplocation = this.fixuplocation;
		copy.arr = this.arr;
		return copy;
	}
	
	public void copy(Operand target) {
		target.kind = this.kind;
		target.val = this.val;
		target.regno = this.regno;
		target.block = this.block;
		target.ident = this.ident;
		target.ssa = this.ssa;
		target.cond = this.cond;
		target.inst = this.inst;
		target.fixuplocation = this.fixuplocation;
		target.arr = this.arr;
	}
	
}