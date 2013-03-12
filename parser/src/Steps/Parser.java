package Steps;


import java.util.ArrayList;
import java.util.List;

import DataStructures.Array;
import DataStructures.BasicBlock;
import DataStructures.ControlFlowGraph;
import DataStructures.Function;
import DataStructures.Instruction;
import DataStructures.Operand;
import DataStructures.SSA;
import DataStructures.VariableManager;

import cs241_compiler.Token;

public class Parser {
		
	private static final int TEMP = -1;
	
	
	
	private int scannerSym;
	private Scanner scanner;
	private int[] opCode;
	private int[] negatedBranchOp;

	private List<Function> functionList;
	private List<Array> arrayList;
	
	private ControlFlowGraph cfg;
	
	public Parser(Scanner sc) {
		this.scanner = sc;
		this.next();
		
		//operator code of computational tokens
		this.opCode = new int[20];
		opCode[Token.timesToken] = Instruction.mul;
		opCode[Token.divToken] = Instruction.div;
		opCode[Token.plusToken] = Instruction.add;
		opCode[Token.minusToken] = Instruction.sub;
		
		this.negatedBranchOp = new int[30];
		negatedBranchOp[Token.eqlToken] = Instruction.bne;
		negatedBranchOp[Token.neqToken] = Instruction.beq;
		negatedBranchOp[Token.lssToken] = Instruction.bge;
		negatedBranchOp[Token.geqToken] = Instruction.blt;
		negatedBranchOp[Token.leqToken] = Instruction.bgt;
		negatedBranchOp[Token.gtrToken] = Instruction.ble;
		
		functionList = new ArrayList<Function>();
		//default functions
		functionList.add(new Function(0, null));
		functionList.add(new Function(1, null));
		functionList.add(new Function(2, null));
		
		arrayList = new ArrayList<Array>();
	}
	
	public void parse() {
		this.computation();
		//cfg.printCFG();
		//cfg.printDominatorTree();
	}

	public ControlFlowGraph getCfg() {
		return cfg;
	}
	
	//get token from the scanner
	private void next() {
		scannerSym = scanner.getSym();
		//System.out.println(scannerSym);
	}
	
	public boolean functionExists(int ident) {
		for(int i = 0; i < functionList.size(); i++) {
			if( functionList.get(i).equals(ident)) {
				return true;
			}
		}
		return false;
	}
	
	public Function getFunction(int functionIdent) {
		for(Function func: functionList) {
			if(func.getIdent() == functionIdent && functionIdent > 2) {
				return func;
			}
		}
		return null;
	}
	
	public Array getArray(int arrayIdent) {
		for(Array arr: arrayList) {
			if(arr.getIdent() == arrayIdent) {
				return arr;
			}
		}
		return null;
	}
	
	//get the identifier of variable
	private Operand ident() {
		
		Operand x = new Operand();
		x.ident = this.scanner.getId();
		this.next();
		return x;
	}
	
	private Operand designator(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func, boolean isAssign) {
		Operand x = new Operand();
		List<Operand> dimensions = new ArrayList<Operand>();
		if(scannerSym == Token.identifier) {
			//get identifier from scanner
			x = this.ident();
			//see if variable has been declared in function
			if(func != null) {
				if(func.variableExists(x.ident)) {
					x.kind = Operand.var;
				} 
			}
			//see if variable has been declared in main
			if(x.kind == Operand.unknown) {
				if(VariableManager.variableExists(x.ident)) {
					x.kind = Operand.var;
					//add use of global variable in function
					if(func != null) {
						func.addGlobalVarsUsed(x.ident);
					}
				} else {
					this.printError("identifier does not exist.");
					x = null;
				}
			}
			//array access parser
			if(x != null) {
				while(scannerSym == Token.openbracketToken) {
					this.next();
					
					Operand y = this.expression(curBB, joinBlockChain, func);
					dimensions.add(y);
					
					if(scannerSym == Token.closebracketToken) {
						this.next();
					} else {
						this.printError("[ must be followed by ]");
					}
				}
			}
			
			//generate array instructions
			if(!dimensions.isEmpty()) {
				x.kind = Operand.array;
				x.arr = this.getArray(x.ident);
				//address interval
				Operand temp = Operand.makeConst(0);
				int size = 4;
				//traverse back from the last dimension
				for(int i = dimensions.size() - 1; i >= 0; i--) {
					if(dimensions.get(i).kind == Operand.constant) {
						//if index is constant, directly calculate relative address
						if(temp.kind == Operand.constant){
							temp.val += dimensions.get(i).val * size;
							
						} else if(temp.kind == Operand.var){
							Operand tempBefore = temp;
							temp = createTempResult();	
							curBB.generateIntermediateCode(Instruction.mul, dimensions.get(i), Operand.makeConst(size));
							Operand tempBetween = temp;
							temp = createTempResult();
							curBB.generateIntermediateCode(Instruction.add, tempBefore, tempBetween);
						}
						
					} else if(dimensions.get(i).kind == Operand.var) {
						Operand tempBefore = temp;
						temp = createTempResult();	
						curBB.generateIntermediateCode(Instruction.mul, dimensions.get(i), Operand.makeConst(size));
						Operand tempBetween = temp;
						temp = createTempResult();
						curBB.generateIntermediateCode(Instruction.add, tempBefore, tempBetween);
					}
					size *= x.arr.getDimension(i); 
				}
				Operand plusIndex = temp;
				
				Operand baseAddress = createTempResult();
				Operand fp = Operand.makeReg(28);
				x.ssa = x.arr.getBase();
				curBB.generateIntermediateCode(Instruction.add, fp, x);
				
				Operand arrResult = createTempResult();
				curBB.generateIntermediateCode(Instruction.adda, plusIndex, baseAddress);
				
				arrResult.kind = Operand.array;
				
				return arrResult;
			} else {//not array
				return x;
			}
			
			
		} else {
			return null;
		}

	}

	private Operand createTempResult() {
		Operand temp = Operand.makeVar(TEMP);
		VariableManager.addAssignment(Instruction.getPC(), temp);
		return temp;
	}
	
	private Operand factor(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand x = null;
		if(scannerSym == Token.number) { //number
			x = Operand.makeConst(scanner.getVal());
			this.next();
		} else if(scannerSym == Token.openparenToken) { //(expression)
			this.next();
			x = this.expression(curBB, joinBlockChain, func);
			if(scannerSym == Token.closeparenToken) { 
				this.next();
			} else { 
				//error
				printError("no \")\" after \"(\"");
			}
		} else if(scannerSym == Token.callToken) { //funcCall
			x = this.funcCall(curBB, joinBlockChain, func);
		} else if(scannerSym == Token.identifier) {
			x = this.designator(curBB, joinBlockChain, func, false);
			if(x.kind != Operand.array) {
				//if global variable is first used in a function
				if(func != null && func.getGlobalVarsUsed().contains(x.ident)) {
					//move reference of global to local 
					this.registerVariable(x.ident, func);
					func.removeGlobalVarsUsed(x.ident);
					//TODO: tricky!!!
					VariableManager.addAssignment(func.getBlock().getInstructions().get(0).getId(), x);
					func.addLocalVar(x.ssa);
					func.addLocalizedGlobalVars(x.ssa);
					BasicBlock joinBlock = null;
					if(joinBlockChain.size() > 0) {
						joinBlock = joinBlockChain.get(0);
					}
					if(joinBlock != null && joinBlock.isLoop()) {
						joinBlock.updatePhiFunctionLocalizedGlobal(x, true);
					}
				} else {// other cases
					boolean isFound = false;
					for(int i = 0; i < joinBlockChain.size(); i++) {
						SSA backupVar = joinBlockChain.get(i).findBackup(x.ident, curBB.getIsElse(i));
						//get last version of a variable with such identifier
						if( backupVar != null) {
							isFound = true;
							x.ssa = backupVar;
							break;
						}
					} 
					
					if(!isFound) {
						//if the variable is first used in the branch, find last definition of it in SSA list
						x.ssa = VariableManager.getLastVersionVarOf(x.ident);
					}
				}	
			} else {
				Operand address = x;
				x = createTempResult();
				curBB.generateIntermediateCode(Instruction.load, address, null);
			}
			

		} else {
			//error
			printError("not valid factor.");
		}
		return x;
	}
	//TODO: compute
	//avoid generating code when both operands are constant
	private Operand compute(BasicBlock curBB, int op, Operand x, Operand y) {
		Operand temp = null;
		
		//if both operands are constant, than temp var is constant, no need to generate computation instruction
		if(x.kind == Operand.constant && y.kind == Operand.constant) {
			int computeResult = 0;
			
			if(op == Instruction.cmp){
				computeResult = x.val - y.val;
				temp = createTempResult();	
				Operand cmpConst = Operand.makeConst(computeResult);
				curBB.generateIntermediateCode(Instruction.move, cmpConst, temp);
				return temp;
			}
			else if(op == Instruction.add) 
				computeResult = x.val + y.val;
			else if(op == Instruction.sub)
				computeResult = x.val - y.val;
			else if(op == Instruction.mul)
				computeResult = x.val * y.val;
			else if(op == Instruction.div)
				computeResult = x.val / y.val;	
			
			//create a constant to store computation result
			temp = Operand.makeConst(computeResult);
			
		} else {
			temp = createTempResult();	
			curBB.generateIntermediateCode(op, x, y);
		}
		return temp;		
	}
	
	private Operand term(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand x, y;
		int op;
		x = this.factor(curBB, joinBlockChain, func);
		while(scannerSym == Token.timesToken || scannerSym == Token.divToken){
			op = scannerSym;
			this.next();
			
			y = this.factor(curBB, joinBlockChain, func);
			x = this.compute(curBB, opCode[op], x, y);
		} 
		return x;
	}
	
	private Operand expression(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand x, y;
		int op;
		x = this.term(curBB, joinBlockChain, func);
		while(scannerSym == Token.plusToken || scannerSym == Token.minusToken){
			op = scannerSym;
			this.next();
			
			y = this.term(curBB, joinBlockChain, func);
			x = this.compute(curBB, opCode[op], x, y);
		} 
		return x;
	}
	
	private Operand relation(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand x, y;
		Operand cond = null;
		int op;
		x = this.expression(curBB, joinBlockChain, func);
		if(scannerSym >= Token.eqlToken && scannerSym <= Token.gtrToken) {
			op = scannerSym;
			this.next();

			y = this.expression(curBB, joinBlockChain, func);
			
			x = this.compute(curBB, Instruction.cmp, x, y);
			cond = new Operand();
			if(x.kind == Operand.constant) {
				cond.kind = Operand.constant;
				cond.val = x.val;
			} else {
				cond.kind = Operand.condition;
				cond.ssa = x.ssa;
			}
			
			cond.cond = op;
			cond.fixuplocation = 0;
		} else {
			//error
			printError("there should be a relational operator between two expressions.");
		}
		return cond;
	}
	//TODO: assign
	private void assign(BasicBlock curBB, List<BasicBlock> joinBlockChain, Operand y, Operand x) {
		if(x.kind != Operand.constant) {
			
//			int assignVal = 0;
//			
//			//if y is constant, assign y to x immediately
//			if(y.kind == Result.constant) {
//				assignVal = y.val;		
//			} else {			
//				assignVal = VariableManager.getValueAt(y.ssaIdx);
//			}
//			x.val = assignVal;

			
			//look up the constant table, if exists the same constant, use previous ssa
			if(y.kind == Operand.constant) {
				if(!VariableManager.constantExist(y.val)) {
					VariableManager.addAssignment(Instruction.getPC(), x);
					VariableManager.addConstant(y.val, x.ssa);
				} else {
					y.ssa = VariableManager.getSSAOfConstant(y.val);
					VariableManager.addAssignment(Instruction.getPC(), x);
				}
			} else {
				VariableManager.addAssignment(Instruction.getPC(), x);
			}
			curBB.generateIntermediateCode(Instruction.move, y, x);
			//if there exists a join block, need to generate or update phi function
			BasicBlock joinBlock = null;
			if(joinBlockChain.size() > 0) {
				joinBlock = joinBlockChain.get(0);
			}
			if(joinBlock != null) {
				joinBlock.updatePhiFunction(x, curBB, joinBlockChain);
			}

		} else {
			//error
			printError("left operand cannot be constant.");
		}
	}
	
	//The following five functions have the assumption that their keywords have already been detected. 
	private void assignment(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand x, y;
		//consume "let"
		this.next();
		x = this.designator(curBB, joinBlockChain, func, true);
		//if global variable is first used in a function
		if(func != null && func.getGlobalVarsUsed().contains(x.ident)) {
			//move reference of global to local 
			this.registerVariable(x.ident, func);
			func.removeGlobalVarsUsed(x.ident);
			//TODO: tricky!!!
			VariableManager.addAssignment(func.getBlock().getInstructions().get(0).getId(), x);
			func.addLocalVar(x.ssa);
			func.addLocalizedGlobalVars(x.ssa);
			BasicBlock joinBlock = null;
			if(joinBlockChain.size() > 0) {
				joinBlock = joinBlockChain.get(0);
			}
			if(joinBlock != null && joinBlock.isLoop()) {
				joinBlock.updatePhiFunctionLocalizedGlobal(x, true);
			}
		} 
		
		if(scannerSym == Token.becomesToken){
			this.next();
			y = this.expression(curBB, joinBlockChain, func);
			
			if(x.kind != Operand.array){
				//assign y to x	
				assign(curBB, joinBlockChain, y, x);	
			} else {
				if(y.kind == Operand.constant) {
					Operand temp = this.createTempResult();
					curBB.generateIntermediateCode(Instruction.move, y, temp);
					curBB.generateIntermediateCode(Instruction.store, temp, x);
				} else {
					curBB.generateIntermediateCode(Instruction.store, y, x);
				}
			}
					
		} 
	}
	
	private void subroutineBranch(int functionIdent, BasicBlock curBB) {
		//push current address to stack
		//Result returnAddress = Result.makeVar(ident);
		//the address after push and branch
		//returnAddress.val = Instruction.getPC() + 2;
		
		//jump to subroutineBranch
		Operand branch = Operand.makeBranch(null);
		branch.block = this.getFunction(functionIdent).getBlock();
		curBB.generateIntermediateCode(Instruction.subroutine, null, branch);
	}
	
	private Operand funcCall(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function outerFunc) {
		Operand funcIdent, argument, returnValue;
		int paramIdx = 0;
		//consume "call"
		this.next();

		List<Operand> arguments = new ArrayList<Operand>();
		
		if(scannerSym == Token.identifier) {
			
			funcIdent = this.ident();
			int functionIdent = funcIdent.ident;
			Function curFunc = this.getFunction(functionIdent);

			if(scannerSym == Token.openparenToken) { //(expression,..) call with paren
				this.next();
				//if there is param
				if(scannerSym != Token.closeparenToken) {
					//first param
					argument = this.expression(curBB, joinBlockChain, outerFunc);
					//add param
					arguments.add(argument);
					
					if(curFunc != null) {
						//assign argument to parameter
						curBB.generateIntermediateCode(Instruction.move, argument, curFunc.getParamAt(paramIdx));
					}
					
					paramIdx ++;
					while(scannerSym == Token.commaToken) {
						//consume comma
						this.next();
						argument = this.expression(curBB, joinBlockChain, outerFunc);
						arguments.add(argument);
						
						if(curFunc != null) {
							//assign argument to parameter
							//assign(curBB, joinBlockChain, argument, curFunc.getParamAt(paramIdx));
							curBB.generateIntermediateCode(Instruction.move, argument, curFunc.getParamAt(paramIdx));
						}
						paramIdx ++;
					}

				}
				if(scannerSym == Token.closeparenToken) { 
					this.next();
				} else { 
					//error
					printError("missing \")\" in function call.");
				}
			}
			//call function
			if(functionIdent == 0) {//inputNum
				Operand input = Operand.makeVar(TEMP);
				
				VariableManager.addAssignment(Instruction.getPC(), input);	
				curBB.generateIntermediateCode(Instruction.read, input, null);
				return input;
				
			} else if (functionIdent == 1) {//outputNum
				curBB.generateIntermediateCode(Instruction.write, null, arguments.get(0));
				return null;
				
			} else if (functionIdent == 2) {//outputNewLine
				
				curBB.generateIntermediateCode(Instruction.wln, null, null);
				return null;
				
			} else{
				if(curFunc == null) {
					this.printError("function doesn't exist.");
				}
				//assign the use of global variables
				for(SSA localGlobal : curFunc.getLocalizedGlobalVars()) {
					Operand x = Operand.makeVar(localGlobal.getIdentifier());
					Operand localX = Operand.makeVar(x.ident);
					localX.ssa = localGlobal;
					
					boolean isFound = false;
					for(int i = 0; i < joinBlockChain.size(); i++) {
						SSA backupVar = joinBlockChain.get(i).findBackup(x.ident, curBB.getIsElse(i));
						//get last version of a variable with such identifier
						if( backupVar != null) {
							isFound = true;
							x.ssa = backupVar;
							break;
						}
					} 
					
					if(!isFound) {
						//if the variable is first used in the branch, find last definition of it in SSA list
						x.ssa = VariableManager.getLastVersionVarOf(x.ident);
					}
					curBB.generateIntermediateCode(Instruction.move, x, localX);
				}
				//call regular functions
				subroutineBranch(functionIdent, curBB);
				return curFunc.getReturnVar();
			}	
		} else{
			printError("no identifier following function call");
			return null;
		}
		
	}
	
	//Negative conditional branch forward
	private void condNegBraFwd(Operand condition, BasicBlock curBB) {
		//the location of instruction negatedBranchOp, record that for later fix
		condition.fixuplocation = Instruction.getPC();
		
		//make a branch address operand pointing to nowhere
		Operand branch = Operand.makeBranch(null);
		curBB.generateIntermediateCode(negatedBranchOp[condition.cond], condition, branch);
	}
	
	private void unCondBraFwd(Operand x, BasicBlock curBB) {
		//build linked list by storing previous value
		Operand branch = Operand.makeBranch(null);
		branch.fixuplocation = x.fixuplocation;
		curBB.generateIntermediateCode(Instruction.bra, null, branch);
		
		//the location of instruction beq
		x.fixuplocation = Instruction.getPC() - 1;
	}
	
	//fix the target block of a branch forward instruction
	private void fixup(int fixupLoc, BasicBlock block) {
		for(Instruction inst : ControlFlowGraph.getInstList()) {
			if(inst.getId() == fixupLoc) {
				inst.setBranchDest(block);
			}
		}
	}
	
	//fix the all branch forward statement after every branch of a if-statement
	private void fixAll(int loc, BasicBlock block) {
		//the next fixup location
		int next = 0;
		while(loc != 0) {
			for(Instruction inst : ControlFlowGraph.getInstList()) {
				if(inst.getId() == loc) {
					next = inst.getOperand2().fixuplocation;
				}
			}
			fixup(loc, block);
			loc = next;
		}
	}
	//TODO: if
	private BasicBlock ifStatement(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		//consume "if"
		this.next();
		Operand follow = new Operand();
		Operand x = relation(curBB, joinBlockChain, func);
		//create a join block linked after if block
		BasicBlock joinBlock = new BasicBlock(curBB.getIsElses());
		joinBlockChain.add(0, joinBlock);
		curBB.setJoinSuccessor(joinBlock);
		curBB.dominates(joinBlock);
		//all statements after "if" will jump to the end of the whole if-statement
		follow.fixuplocation = 0;
		
		BasicBlock directLastBlock = null;
		BasicBlock elseLastBlock = null;
		

		//jump to the code after if-block if the condition is not satisfied
		condNegBraFwd(x, curBB);
		if( scannerSym == Token.thenToken) {
			this.next();
			
			directLastBlock = statSequence(curBB.makeDirectSuccessor(false), new ArrayList<BasicBlock>(joinBlockChain), func);	
		}
		else {
			//error
			printError("if-statement without \"then\" token.");
		}
		
		if( scannerSym == Token.elseToken) {
			this.next();
			
			BasicBlock elseBB = curBB.makeElseSuccessor(true);
			//unconditional branch forward, forward location need to be fixed later
			//this branch statement should be put in the last block of if-branch
			unCondBraFwd(follow, directLastBlock);
			//link the forward location of false condition to the head of else-branch
			fixup(x.fixuplocation, elseBB);
			
			elseLastBlock = statSequence(elseBB, new ArrayList<BasicBlock>(joinBlockChain), func);	
		} else {
			fixup(x.fixuplocation, joinBlock);
		}
		
		if( scannerSym == Token.fiToken) {
			this.next();
			
			//link the end of each branch to the statement following this if-statement
			fixAll(follow.fixuplocation, joinBlock);
			
			//link join block to the last block of if-branch and else-branch statement sequence
		    directLastBlock.setJoinSuccessor(joinBlock);
		    joinBlock.setDirectPredecessor(directLastBlock);
		    if(elseLastBlock == null) {
		    	curBB.setElseSuccessor(joinBlock);
		    	joinBlock.setElsePredecessor(curBB);
		    } else{
		    	elseLastBlock.setJoinSuccessor(joinBlock);
		    	joinBlock.setElsePredecessor(elseLastBlock);
		    }

			//transfer the control point to the join block of an if-statement
			return joinBlock;
			
		}
		else {
			//error
			printError("if-statement without \"fi\" token.");
			return null;
		}
	}
	
	private BasicBlock whileStatement(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand x;
		//consume "while"
		this.next();
		//used to reassign all use of variable in the loop
		//int SSABeforeWhile = VariableManager.getLastSSAPosition();
		int SSABeforeWhile = Instruction.getPC();
		//create a new block from previous sequential statements
		BasicBlock temp = curBB.makeDirectSuccessor(curBB.getIsElse(0));
		temp.setElsePredecessor(curBB);
		curBB = temp;
		
		x = relation(curBB, joinBlockChain, func);
		
		BasicBlock innerJoinBlock = curBB;
		List<BasicBlock> innerJoinBlockChain = new ArrayList<BasicBlock>(joinBlockChain);
		innerJoinBlockChain.add(0, innerJoinBlock);
		innerJoinBlock.setLoop(true);
		BasicBlock doLastBlock = null;
		

		condNegBraFwd(x, curBB);
		
		if( scannerSym == Token.doToken) {
			this.next();
			//the last block of loop body
			//TODO: possible bug
			
			doLastBlock = statSequence(curBB.makeDirectSuccessor(false), innerJoinBlockChain, func);
		}
		else {
			//error
			printError("while-statement without \"do\" token.");
		}
		
		Operand branch = Operand.makeBranch(curBB);
		doLastBlock.generateIntermediateCode(Instruction.bra, null, branch);
		
		//link loop block back to condition
		doLastBlock.setBackSuccessor(curBB);
		curBB.setDirectPredecessor(doLastBlock);
		BasicBlock followBlock = curBB.makeElseSuccessor(false);
		fixup(x.fixuplocation, followBlock);
		
		if( scannerSym == Token.odToken) {
			this.next();
			curBB.fixLoopBackup();
			//int SSABeforePhi = VariableManager.getLastSSAPosition();
			int SSABeforePhi = Instruction.getPC();
			//TODO: bug

			curBB.finalizePhiFuncs(joinBlockChain);
			curBB.renameOldUse(SSABeforeWhile, SSABeforePhi);
			return followBlock;
		}
		else {
			//error
			printError("while-statement without \"od\" token.");
			return null;
		}
	}
	
	private void returnStatement(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand result;
		//consume "return"
		this.next();
		if(scannerSym == Token.identifier 
				|| scannerSym == Token.number 
				|| scannerSym == Token.openparenToken 
				|| scannerSym == Token.callToken) {
			
			result = this.expression(curBB, joinBlockChain, func);
			
			//assign y to x	
			assign(curBB, joinBlockChain, result, func.getReturnVar());
			curBB.getInstructions().get(curBB.getInstructions().size() - 1).setCopiable(false);
		}
		
	}
	
	private BasicBlock statement(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		if(scannerSym == Token.letToken) {
			this.assignment(curBB, joinBlockChain, func);
			return curBB;
		} else if(scannerSym == Token.callToken) {
			this.funcCall(curBB, joinBlockChain, func);
			return curBB;
		} else if(scannerSym == Token.ifToken) {
			//the last(join) block of the if-statement
			BasicBlock lastBlock = this.ifStatement(curBB, new ArrayList<BasicBlock>(joinBlockChain), func);
			lastBlock.finalizePhiFuncs(joinBlockChain);
			return lastBlock;
		} else if(scannerSym == Token.whileToken) {
			return this.whileStatement(curBB, new ArrayList<BasicBlock>(joinBlockChain), func);
		} else if(scannerSym == Token.returnToken) {
			this.returnStatement(curBB, joinBlockChain, func);
			return curBB;
		} else {
			//error
			printError("not valid initial statement keyword.");
			return null;
		}
	}
	
	private BasicBlock statSequence(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		//return last block of a statement sequence for block joining
		BasicBlock lastBlock = this.statement(curBB, joinBlockChain, func);
		if(scannerSym == Token.commentToken) {
			this.next();
		}
		while(scannerSym == Token.semiToken) {
			this.next();
			//transfer control to the last block of last statement
			curBB = lastBlock;
			lastBlock = this.statement(curBB, joinBlockChain, func);
			if(scannerSym == Token.commentToken) {
				this.next();
			}
		}
		return lastBlock;
	}
	
	private void registerVariable(int ident, Function func) {
		if(func == null) {
			if(!VariableManager.variableExists(ident)) {
				VariableManager.addVariable(ident);
			} else {
				printError("variable declaration duplicated.");
			}
		} else {
			if(!func.variableExists(ident)) {
				func.addVariable(ident);
			} else {
				printError("variable declaration duplicated within function.");
			}
		}	
		
	}
	
	private Function registerFunction(int ident) {
		if(!functionExists(ident)) {
			
			BasicBlock funcBB = new BasicBlock(new ArrayList<Boolean>());
			this.cfg.addFirstBlockOfFunc(funcBB);
			Function func = new Function(ident, funcBB);
			functionList.add(func);
			return func;
		} else {
			printError("function declaration duplicated.");
			return null;
		}
	}
	
	private void typeDecl(Operand var) {
		if(scannerSym == Token.varToken) {
			this.next();
			
			var.kind = Operand.var;
		} else if(scannerSym == Token.arrToken) {
			this.next();
			
			var.kind = Operand.array;
			Array arr = new Array();
			var.arr = arr;
			
			if(scannerSym == Token.openbracketToken) {
				this.next();
				
				if(scannerSym == Token.number) {
					this.next();
					//
					arr.addDimension(this.scanner.getVal());
				} else {
					printError("array declaration must use number as dimension size");
				}			
				
				if(scannerSym == Token.closebracketToken) {
					this.next();
				} else {
					this.printError("[ must be followed by ]");
				}
			} else {
				printError("no dimension information after the identifier of array");
			}
			
			while(scannerSym == Token.openbracketToken) {
				this.next();
				
				if(scannerSym == Token.number) {
					this.next();
					//
					arr.addDimension(this.scanner.getVal());
				} else {
					printError("array declaration must use number as dimension size");
				}			
				
				if(scannerSym == Token.closebracketToken) {
					this.next();
				} else {
					this.printError("[ must be followed by ]");
				}
			}
			
		} else {
			printError("missing type declaration keyword.");
		}
	}
	
	private void declareVariable(BasicBlock curBB, Operand var, Function func) {
		if(var.kind == Operand.var) {
			Operand defaultVal = Operand.makeConst(0);
			
			var = this.ident();
			var.kind = Operand.var;
			this.registerVariable(var.ident, func);
			
			if(!VariableManager.constantExist(defaultVal.val)) {
				VariableManager.addAssignment(Instruction.getPC(), var);
				VariableManager.addConstant(defaultVal.val, var.ssa);
			} else {
				VariableManager.addAssignment(Instruction.getPC(), var);
			}
			//if the declaration is within a function
			if(func != null) {
				func.addLocalVar(var.ssa);
			}
			curBB.generateIntermediateCode(Instruction.move, defaultVal, var);
		} else if(var.kind == Operand.array) {
			Array arr = var.arr.copy();
			
			var = this.ident();
			arr.setIdent(var.ident);
			this.arrayList.add(arr);
			
			var.kind = Operand.array;
			this.registerVariable(var.ident, func);
			
			var.arr = arr;

			VariableManager.addAssignment(Instruction.getPC(), var);
			var.arr.setBase(var.ssa);
			Operand base = Operand.makeConst(Array.curBase);
			curBB.generateIntermediateCode(Instruction.move, base, var);
			var.arr.nextBase();
		} else {
			this.printError("nani?");
		}
		
	}
	
	//TODO: varDecl
	private void varDecl(BasicBlock curBB, Function func) {
		Operand var = new Operand();
		
		this.typeDecl(var);
		
		this.declareVariable(curBB, var, func);
		
		while(scannerSym == Token.commaToken) {
			this.next();
			
			this.declareVariable(curBB, var, func);
		}
		if(scannerSym == Token.semiToken) {
			this.next();
		} else {
			printError("variable declaration must end with a \";\" .");
		}	
		
	}
	
	private void funcDecl() {
		Operand func = new Operand();
		//consume "function" or "procedure"
		this.next();
		
		func = this.ident();
		Function function = this.registerFunction(func.ident);
		if(scannerSym != Token.semiToken) {
			formalParam(function);
		}
		
		if(scannerSym == Token.semiToken) {

			this.next();
			funcBody(function);
		} else {
			printError("no \";\" before function body");
		}
		
		if(scannerSym == Token.semiToken) {
			this.next();
		} else {
			printError("function body must end with \";\".");
		}
		
	}
	
	private void formalParam(Function func) {
		Operand param = new Operand();
		
		if(scannerSym == Token.openparenToken) {
			this.next();
		} else {
			printError("parameter declaration must start with \"(\" .");
		}
		
		if(scannerSym != Token.closeparenToken) {
			param = this.ident();
			this.registerVariable(param.ident, func);
			if(func != null) {
				VariableManager.addAssignment(Instruction.getPC(), param);
				func.addParam(param.ssa);
				param.ssa.setParam(true);
			}
			
			while(scannerSym == Token.commaToken) {
				this.next();
				
				param = this.ident();
				this.registerVariable(param.ident, func);
				if(func != null) {
					VariableManager.addAssignment(Instruction.getPC(), param);
					func.addParam(param.ssa);
					param.ssa.setParam(true);
				}
			}
		} 

		if(scannerSym == Token.closeparenToken) {
			this.next();
		} else {
			printError("parameter declaration must end with \")\" .");
		}
	}
	
	private void funcBody(Function func) {
		while(scannerSym == Token.varToken || scannerSym == Token.arrToken) {
			varDecl(func.getBlock(), func);
		} 
		if(scannerSym == Token.beginToken) {
			this.next();
			//empty statement sequence
			if(scannerSym != Token.endToken) {
				BasicBlock lastBlock = statSequence(func.getBlock(), new ArrayList<BasicBlock>(), func);
				lastBlock.generateIntermediateCode(Instruction.retrn, null, null);
			}
			if(scannerSym == Token.endToken) {
				this.next();
			} else {
				printError("main block must end with end token");
			}
		} else {
			printError("no main function block.");
		}
	}
	
	private void computation() {
		if(scannerSym == Token.mainToken) {
			
			this.next();
			
			//build a new control flow graph
			cfg = new ControlFlowGraph(scanner);

			while(scannerSym == Token.varToken || scannerSym == Token.arrToken) {
				varDecl(cfg.getFirstBlock(), null);
			} 
			while(scannerSym == Token.funcToken || scannerSym == Token.procedureToken) {
				funcDecl();
			}
			if(scannerSym == Token.beginToken) {
				this.next();
				//pass the first block of a CFG into function for adding nodes
				BasicBlock lastBlock = statSequence(cfg.getFirstBlock(), new ArrayList<BasicBlock>(), null);
				if(scannerSym == Token.endToken) {
					this.next();
					if(scannerSym == Token.periodToken) {
						this.next();
						//parsing ends
						lastBlock.generateIntermediateCode(Instruction.end, null, null);
						System.out.println("Parsing is over.");
					}
					else {
						printError("program must end with period token.");
					}
				} else {
					printError("main function block must end with \"}\" token.");
				}
			} else {
				printError("there is no main function block.");
			}
		}
	}
	
	private void printError(String errMsg) {
		System.out.println("Syntax error at " + scanner.curLine + ": " + errMsg);
		//System.exit(0);
	}
	

}
