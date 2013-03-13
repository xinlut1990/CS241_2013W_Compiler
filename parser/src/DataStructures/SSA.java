package DataStructures;

import java.util.ArrayList;
import java.util.List;



public class SSA {
    //the version of a SSA variable, by line number
	private int version = 0;
	private int value = 0;
	//the identifier number of variable
	private int identifier = 0;
	private int reg = 0;
	
	//define-use chain
	private List<Operand> useChain = new ArrayList<Operand>();
	
	private boolean isParam = false;
	
	public boolean isParam() {
		return isParam;
	}

	public void setParam(boolean isParam) {
		this.isParam = isParam;
	}

	public int getReg() {
		return reg;
	}

	public void setReg(int reg) {
		this.reg = reg;
	}
	
	public List<Operand> getUseChain() {
		return useChain;
	}

	public SSA(int version, int value, int identifier) {
		this.version = version;
		this.value = value;
		this.identifier = identifier;
	}
	
	//add use of ssa variable to define-use chain
	public void addUse(Operand x) {
		this.useChain.add(x);
	}
	
	public void removeUse(Operand x) {
		this.useChain.remove(x);
	}
	
	public void replaceAllUse(SSA toReplace) {
		for(Operand use : this.useChain) {
			if(use.kind == Operand.var) {
				use.ssa = toReplace;
			} else {
				System.out.println("CSE error!");
			}
		}
	}
	
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
		this.value = value;
	}
	public int getIdentifier() {
		return identifier;
	}
	public void setIdentifier(int identifier) {
		this.identifier = identifier;
	}

}
