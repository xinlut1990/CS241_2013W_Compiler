package DataStructures;

import java.util.ArrayList;
import java.util.List;

import VariableManagement.SSA;
import VariableManagement.VariableManager;

import ControlFlowGraph.BasicBlock;

public class Function {
	private int ident;
	private BasicBlock funcBB = null;
    //list of variables (unique only by identifier)
    private List<Integer> varList = new ArrayList<Integer>();
    
	private List<SSA> paramList = new ArrayList<SSA>();
	private List<SSA> localVarList = new ArrayList<SSA>();
	private List<Integer> globalVarsUsed = new ArrayList<Integer>();
	private List<SSA> localizedGlobalVars = new ArrayList<SSA>();


	private Operand returnVar = null;
	
	public Operand getReturnVar() {
		return returnVar;
	}

	public Function(int ident, BasicBlock funcBB) {
		this.ident = ident;
		this.funcBB = funcBB;
		Operand temp = Operand.makeVar(-1);
		VariableManager.addAssignment(Instruction.getPC(), temp);
		returnVar = temp;
	}
	
	public void addParam(SSA ssa) {
		paramList.add(ssa);
	}
	
	public Operand getParamAt(int idx) {
		SSA paramSSA = paramList.get(idx);
		Operand param = Operand.makeVar(paramSSA.getIdentifier());
		param.ssa = paramSSA;
		return param;
	}
	
	public void addLocalVar(SSA ssa) {
		localVarList.add(ssa);
	}
	
	public BasicBlock getBlock() {
		return funcBB;
	}

	public int getIdent() {
		return ident;
	}

	public void setIdent(int ident) {
		this.ident = ident;
	}
	
	//variable identifier related
	
	public void addVariable(int id) {
		varList.add(id);
	}
	
	public boolean variableExists(int id) {
		if(varList.contains(id)) {
			return true;
		} else {
			return false;
		}
	}

	public List<Integer> getGlobalVarsUsed() {
		return globalVarsUsed;
	}

	public void setGlobalVarsUsed(List<Integer> globalVarsUsed) {
		this.globalVarsUsed = globalVarsUsed;
	}
	
	public void addGlobalVarsUsed(int ident) {
		this.globalVarsUsed.add(ident);
	}
	
	public void removeGlobalVarsUsed(int ident) {
		for(int i = 0; i < this.globalVarsUsed.size(); i++) {
			if(this.globalVarsUsed.get(i) == ident) {
				this.globalVarsUsed.remove(i);
				return;
			}
		}
	}
	
	public List<SSA> getLocalizedGlobalVars() {
		return localizedGlobalVars;
	}

	public void setLocalizedGlobalVars(
			List<SSA> localizedGlobalVars) {
		this.localizedGlobalVars = localizedGlobalVars;
	}
	public void addLocalizedGlobalVars(SSA var) {
		this.localizedGlobalVars.add(var);
	}
	
}
