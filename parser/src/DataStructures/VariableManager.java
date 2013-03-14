package DataStructures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class VariableManager {
    //list of variables (unique only by identifier)
    private static List<Integer> varList = new ArrayList<Integer>();

    //version(assignment line) and identifier(-1 if is temp)
	private static List<SSA> SSAList = new ArrayList<SSA>();
	private static Map<Integer, SSA> constAssignments = new HashMap<Integer, SSA>();
	
	public static void reset() {
		varList = new ArrayList<Integer>();
		SSAList = new ArrayList<SSA>();
		constAssignments = new HashMap<Integer, SSA>();
	}
	
	public static int getTempRegIn(int instId) {
		for(SSA SSA : SSAList) {
			if(SSA.getVersion() == instId) {
				return SSA.getReg();
			}
		}
		return -1;
	}
	
	public static void removeSSA(SSA ssa) {
		SSAList.remove(ssa);
	}
	
	public static int getSSASize() {
		return SSAList.size();
	}
	
	public static void addAssignment(int version, Operand var) {
		SSAList.add(new SSA(version, var.val, var.ident));
		var.ssa = SSAList.get(SSAList.size() - 1);
	}
	
	public static SSA getSSA(int ssaIdx) {
		return SSAList.get(ssaIdx);
	}
	
	public static int getLastSSAPosition(){
		return SSAList.get( SSAList.size() - 1).getVersion();
	}
	
	public static SSA getLastVersionVarOf(int identifier) {
		for(int i = SSAList.size() - 1; i >= 0; i--) {
			if(SSAList.get(i).getIdentifier() == identifier) {
				return SSAList.get(i);
			}
		}
		return null;
	}
	
	public static SSA getSecondLastOf(int identifier) {
		for(int i = SSAList.size() - 2; i >= 0; i--) {
			if(SSAList.get(i).getIdentifier() == identifier) {
				return SSAList.get(i);
			}
		}
		return null;
	}
	
	public static SSA getSSAByVersion(int version) {
		for(int i = 0; i < SSAList.size(); i++) {
			if(SSAList.get(i).getVersion() == version) {
				return SSAList.get(i);
			}
		}
		return null;
	}
	
	public static List<SSA> getSSAsByVersion(int version) {
		List<SSA> ssaList = new ArrayList<SSA>();
		for(int i = 0; i < SSAList.size(); i++) {
			if(SSAList.get(i).getVersion() == version && !SSAList.get(i).isParam()) {
				ssaList.add(SSAList.get(i));
			}
		}
		return ssaList;
	}
	
	//variable identifier related
	
	public static void addVariable(int id) {
		varList.add(id);
	}
	
	public static boolean variableExists(int id) {
		if(varList.contains(id)) {
			return true;
		} else {
			return false;
		}
	}
	
	//constant related
	
	public static boolean constantExist(int constant) {
		return constAssignments.containsKey(constant);
	}
	
	public static SSA getSSAOfConstant(int constant) {
		return constAssignments.get(constant);
	}
	
	public static void addConstant(int constant, SSA ssa) {
		constAssignments.put(constant, ssa);
	}
		
}
