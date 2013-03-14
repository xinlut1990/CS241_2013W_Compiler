package DataStructures;

import java.util.List;
import java.util.ArrayList;

import VariableManagement.SSA;


public class Array {
	public static int curBase = 0;
	private int ident;

	private List<Integer> dimensions = new ArrayList<Integer>();
	private SSA base = null;
	
	public int getIdent() {
		return ident;
	}

	public void setIdent(int ident) {
		this.ident = ident;
	}
	
	public SSA getBase() {
		return base;
	}

	public void setBase(SSA base) {
		this.base = base;
	}

	public void addDimension(int dimension) {
		this.dimensions.add(dimension);
	}
	
	public int getDimension(int dimension) {
		return this.dimensions.get(dimension);
	}
	
	public void nextBase() {
		int size = 4;
		for(Integer dimension : this.dimensions) {
			size *= dimension;
		}
		curBase += size;
	}
	
	public Array copy() {
		Array newArr = new Array();
		newArr.dimensions = this.dimensions;
		return newArr;
	}
}
