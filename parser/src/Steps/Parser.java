package Steps;


import java.util.ArrayList;
import java.util.List;

import DataStructures.Array;
import DataStructures.BasicBlock;
import DataStructures.ControlFlowGraph;
import DataStructures.Function;
import DataStructures.Instruction;
import DataStructures.IntermCodeGenerator;
import DataStructures.Operand;
import DataStructures.VariableManager;

import cs241_compiler.Token;

public class Parser {
		
	private static final int TEMP = -1;
	
	private int scannerSym;
	private Scanner scanner;
	private int[] opCode;

	private ControlFlowGraph cfg;
	
	private IntermCodeGenerator icGen;
	
	public Parser(Scanner sc) {
		this.scanner = sc;
		
		//build a new control flow graph
		cfg = new ControlFlowGraph(scanner);
		
		this.icGen = new IntermCodeGenerator(this.scanner, this.cfg);
		this.next();
		
		//operator code of computational tokens
		this.opCode = new int[20];
		opCode[Token.timesToken] = Instruction.mul;
		opCode[Token.divToken] = Instruction.div;
		opCode[Token.plusToken] = Instruction.add;
		opCode[Token.minusToken] = Instruction.sub;
		
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
	
	//get the identifier of variable
	private Operand ident() {
		
		Operand identifier = new Operand();
		identifier.ident = this.scanner.getId();
		this.next();
		return identifier;
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
				return icGen.locateAccessToArray(curBB, x, dimensions);
			} else {//not array
				return x;
			}
			
			
		} else {
			return null;
		}

	}
	
	private Operand factor(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		
		if(scannerSym == Token.number) { //number
			int constVal = scanner.getVal();
			this.next();
			
			return Operand.makeConst(constVal);

		} else if(scannerSym == Token.openparenToken) { //(expression)
			this.next();
			
			Operand expResult = this.expression(curBB, joinBlockChain, func);
			
			if(scannerSym == Token.closeparenToken) { 
				this.next();
			} else { 
				//error
				printError("no \")\" after \"(\"");
			}
			
			return expResult;
			
		} else if(scannerSym == Token.callToken) { //funcCall
			
			return this.funcCall(curBB, joinBlockChain, func);
			
		} else if(scannerSym == Token.identifier) {
			
			Operand designator = this.designator(curBB, joinBlockChain, func, false);
			
			return icGen.parseUse(curBB, designator, joinBlockChain, func);
			
		} else {
			//error
			printError("not valid factor.");
			return null;
		}
	}
	
	private Operand term(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand x, y;
		int op;
		x = this.factor(curBB, joinBlockChain, func);
		while(scannerSym == Token.timesToken || scannerSym == Token.divToken){
			op = scannerSym;
			this.next();
			
			y = this.factor(curBB, joinBlockChain, func);
			x = icGen.compute(curBB, opCode[op], x, y);
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
			x = icGen.compute(curBB, opCode[op], x, y);
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
			
			x = icGen.compute(curBB, Instruction.cmp, x, y);
			
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
	
	//The following five functions have the assumption that their keywords have already been detected. 
	private void assignment(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function func) {
		Operand x, y;
		//consume "let"
		this.next();
		x = this.designator(curBB, joinBlockChain, func, true);
		//if global variable is first used in a function
		if(func != null && func.getGlobalVarsUsed().contains(x.ident)) {
			icGen.handleGlobalInFunc(x, joinBlockChain, func);
		} 
		
		if(scannerSym == Token.becomesToken){
			this.next();
			y = this.expression(curBB, joinBlockChain, func);
			
			if(x.kind != Operand.array){
				//assign y to x	
				icGen.assign(curBB, joinBlockChain, y, x);	
			} else {
				icGen.storeToArray(curBB, y, x);
			}
					
		} 
	}
	
	private Operand funcCall(BasicBlock curBB, List<BasicBlock> joinBlockChain, Function outerFunc) {
		Operand funcIdent, argument;
		int paramIdx = 0;
		//consume "call"
		this.next();

		List<Operand> arguments = new ArrayList<Operand>();
		
		if(scannerSym == Token.identifier) {
			
			funcIdent = this.ident();
			int functionIdent = funcIdent.ident;
			Function curFunc = icGen.getFunction(functionIdent);

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
						curBB.generateIntermediateCode(this.cfg, Instruction.move, argument, curFunc.getParamAt(paramIdx));
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
							curBB.generateIntermediateCode(this.cfg, Instruction.move, argument, curFunc.getParamAt(paramIdx));
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
				curBB.generateIntermediateCode(this.cfg, Instruction.read, input, null);
				return input;
				
			} else if (functionIdent == 1) {//outputNum
				Operand argu = arguments.get(0);
				curBB.generateIntermediateCode(this.cfg, Instruction.write, null, argu);
				return null;
				
			} else if (functionIdent == 2) {//outputNewLine
				
				curBB.generateIntermediateCode(this.cfg, Instruction.wln, null, null);
				return null;
				
			} else{
				return icGen.callRegularFunction(curFunc, curBB, joinBlockChain);
			}	
		} else{
			printError("no identifier following function call");
			return null;
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
		icGen.condNegBraFwd(x, curBB);
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
			icGen.unCondBraFwd(follow, directLastBlock);
			//link the forward location of false condition to the head of else-branch
			icGen.fixup(x.fixuplocation, elseBB);
			
			elseLastBlock = statSequence(elseBB, new ArrayList<BasicBlock>(joinBlockChain), func);	
		} else {
			icGen.fixup(x.fixuplocation, joinBlock);
		}
		
		if( scannerSym == Token.fiToken) {
			this.next();
			
			//link the end of each branch to the statement following this if-statement
			icGen.fixAll(follow.fixuplocation, joinBlock);
			
			//link join block to the last block of if-branch and else-branch statement sequence
			this.linkJoinBlock(joinBlock, directLastBlock, elseLastBlock, curBB);

			//transfer the control point to the join block of an if-statement
			return joinBlock;
			
		}
		else {
			//error
			printError("if-statement without \"fi\" token.");
			return null;
		}
	}
	
	private void linkJoinBlock(BasicBlock joinBlock, BasicBlock directLastBlock, BasicBlock elseLastBlock, BasicBlock curBB) {
	    directLastBlock.setJoinSuccessor(joinBlock);
	    joinBlock.setDirectPredecessor(directLastBlock);
	    
	    if(elseLastBlock == null) {
	    	
	    	curBB.setElseSuccessor(joinBlock);
	    	joinBlock.setElsePredecessor(curBB);
	    } else{
	    	
	    	elseLastBlock.setJoinSuccessor(joinBlock);
	    	joinBlock.setElsePredecessor(elseLastBlock);
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

		icGen.condNegBraFwd(x, curBB);
		
		BasicBlock doLastBlock = null;
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
		doLastBlock.generateIntermediateCode(this.cfg, Instruction.bra, null, branch);
		
		//link loop block back to condition
		doLastBlock.setBackSuccessor(curBB);
		curBB.setDirectPredecessor(doLastBlock);
		
		BasicBlock followBlock = curBB.makeElseSuccessor(false);
		icGen.fixup(x.fixuplocation, followBlock);
		
		if( scannerSym == Token.odToken) {
			this.next();
			curBB.getPhiManager().fixLoopBackup();
			//int SSABeforePhi = VariableManager.getLastSSAPosition();
			int SSABeforePhi = Instruction.getPC();
			//TODO: bug

			curBB.finalizePhiFuncs(this.cfg, joinBlockChain);
			curBB.getPhiManager().renameOldUse(this.cfg.getInstList(), SSABeforeWhile, SSABeforePhi);
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
			icGen.assign(curBB, joinBlockChain, result, func.getReturnVar());
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
			lastBlock.finalizePhiFuncs(this.cfg, joinBlockChain);
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
		curBB = lastBlock;
		if(scannerSym == Token.commentToken) {
			this.next();
		}
		while(scannerSym == Token.semiToken) {
			this.next();
			//transfer control to the last block of last statement

			lastBlock = this.statement(curBB, joinBlockChain, func);
			curBB = lastBlock;
			if(scannerSym == Token.commentToken) {
				this.next();
			}
		}
		return lastBlock;
	}
	
	private void parseDimensionDecl(Array arr) {
		if(scannerSym == Token.number) {
			this.next();
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
				
				this.parseDimensionDecl(arr);
			} else {
				printError("no dimension information after the identifier of array");
			}
			
			while(scannerSym == Token.openbracketToken) {
				this.next();
				
				this.parseDimensionDecl(arr);
			}
			
		} else {
			printError("missing type declaration keyword.");
		}
	}
	
	//TODO: varDecl
	private void varDecl(BasicBlock curBB, Function func) {
		Operand var = new Operand();
		
		this.typeDecl(var);
		
		Operand newVar = var.copy();
		Operand id = this.ident();
		newVar.ident = id.ident;
		icGen.declareVariable(curBB, newVar, func, newVar.kind);
		
		while(scannerSym == Token.commaToken) {
			this.next();
			
			newVar = var.copy();
			id = this.ident();
			newVar.ident = id.ident;
			icGen.declareVariable(curBB, newVar, func, newVar.kind);
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
		Function function = icGen.registerFunction(func.ident, this.cfg);
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
			icGen.registerVariable(param.ident, func);
			if(func != null) {
				VariableManager.addAssignment(Instruction.getPC(), param);
				func.addParam(param.ssa);
				param.ssa.setParam(true);
			}
			
			while(scannerSym == Token.commaToken) {
				this.next();
				
				param = this.ident();
				icGen.registerVariable(param.ident, func);
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
				lastBlock.generateIntermediateCode(this.cfg, Instruction.retrn, null, null);
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
						lastBlock.generateIntermediateCode(this.cfg, Instruction.end, null, null);
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
