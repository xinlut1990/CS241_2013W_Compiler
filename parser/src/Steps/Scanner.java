package Steps;
import java.lang.StringBuilder;
import java.util.List;
import java.util.ArrayList;
import java.io.File;


public class Scanner {
	
	//predefinition of token values
	private static final int errorToken = 0; //error
	
	private static final int timesToken = 1; //'*'
	private static final int divToken = 2; //'/'
	
	private static final int plusToken = 11; //'+'
	private static final int minusToken = 12; //'-'
	
	private static final int eqlToken = 20; //'=='
	private static final int neqToken = 21; //'!='
	private static final int lssToken = 22; //'<'
	private static final int geqToken = 23; //'>='
	private static final int leqToken = 24; //'<='
	private static final int gtrToken = 25; //'>'
	
	private static final int periodToken = 30; //'.'
	private static final int commaToken = 31; //','
	private static final int openbracketToken = 32; //'['
	private static final int closebracketToken = 34; //']'
	private static final int closeparenToken = 35; //')'
	
	private static final int becomesToken = 40; //'<-'
	private static final int thenToken = 41; //'then'	
	private static final int doToken = 42; //'do'
	
	private static final int openparenToken = 50; //'('
	
	private static final int number = 60; //number
	private static final int identifier = 61; //identifier
	
	private static final int semiToken = 70; //';'
	
	private static final int endToken = 80; //'}'
	private static final int odToken = 81; //'od'
	private static final int fiToken = 82; //'fi'
	private static final int elseToken = 90; //'else'
	
	private static final int letToken = 100; //'let'
	private static final int callToken = 101; //'call'
	private static final int ifToken = 102; //'if'
	private static final int whileToken = 103; //'while'
	private static final int returnToken = 104; //'return'
	
	private static final int varToken = 110; //'var'
	private static final int arrToken = 111; //'array'
	private static final int funcToken = 112; //'function'
	private static final int procedureToken = 113; //'procedure'
	
	private static final int beginToken = 150; //'{'
	private static final int mainToken = 200; //'main'
	public static final int commentToken = 201; //'main'
	private static final int eofToken = 255; // end of line
	
	
	private Reader reader;
	private int inputSym;
	
	private int val;
	private int id;
	
	public List<String> identTable;
	
	public int curLine = 1;
	
	
	private void next() { //advance to the next character
		inputSym = reader.getSym();
	}
	
	private void nextLine() {
		reader.nextLine();
		next();
	}
	
	public Scanner(File input) {// constructor: open file and scan the first token into 'sym'
		
		reader = new Reader(input);
		next();
		identTable = new ArrayList<String>();
		identTable.add("InputNum");
		identTable.add("OutputNum");
		identTable.add("OutputNewLine");
	}
	
	public int getKeywordCode(String word) {
		int code = 0;
		if(word.equals("then")) {
			code = thenToken;
		} else if(word.equals("do")) {
			code = doToken;
		} else if(word.equals("od")) {
			code = odToken;
		} else if(word.equals("fi")) {
			code = fiToken;
		} else if(word.equals("else")) {
			code = elseToken;
		} else if(word.equals("let")) {
			code = letToken;
		} else if(word.equals("call")) {
			code = callToken;
		} else if(word.equals("if")) {
			code = ifToken;
		} else if(word.equals("while")) {
			code = whileToken;
		} else if(word.equals("return")) {
			code = returnToken;
		} else if(word.equals("var")) {
			code = varToken;
		} else if(word.equals("array")) {
			code = arrToken;
		} else if(word.equals("function")) {
			code = funcToken;
		} else if(word.equals("procedure")) {
			code = procedureToken;
		}else if(word.equals("main")) {
			code = mainToken;
		}

		return code;
	}
	
	public int getSym(){
		
		String tempIdent = new String();
		
		while(inputSym == 9 || inputSym == 10 || inputSym == 13 || inputSym == 32 || inputSym == '#' || inputSym == '/') {
			if(inputSym == 9 || inputSym == 10 || inputSym == 13 || inputSym == 32 ) {//Space
				if(inputSym == 10) {
					curLine ++;
				}
				next();
			}
			
			if(inputSym == '#') {
				this.nextLine();
			}
			
			if(inputSym == '/') {
				next();
				if(inputSym == '/') {
					this.nextLine();
				} else {
					return divToken;
				}
			}
		}
		
		if(inputSym == -1) {
			return eofToken;
		}
		
		if(inputSym >= 48 && inputSym <= 57) {//If digit, number
			
			//First digit of the number
			//token = curchar;
			this.setVal(inputSym - 48);
			while(inputSym != -1) {
				next();
				if(inputSym >= 48 && inputSym <= 57) {
					//move previous digits left and put current digit in.
					this.setVal(this.getVal() * 10 + inputSym - 48);
				} else {
					return number;
				}
			}
			return eofToken;
			
		} else if((inputSym >= 97 && inputSym <= 122) || (inputSym >= 65 && inputSym <= 90)) {//If letter, Ident
			
			//First letter of the ident
			tempIdent = new StringBuilder().append((char)inputSym).toString();
			while(inputSym != -1) {
				next();
				// if current char is letter, continue scanning
				if((inputSym >= 97 && inputSym <= 122) || (inputSym >= 65 && inputSym <= 90) || (inputSym >= 48 && inputSym <= 57)) {
					tempIdent = new StringBuilder(tempIdent).append((char)inputSym).toString();
				} else {//Get complete identifier
					//First see if an identifier is keyword
					int keywordCode = this.getKeywordCode(tempIdent);
					if(keywordCode != 0) {
						return keywordCode;
					} else {
						if(this.identTable.contains(tempIdent)) {
							this.setId(this.identTable.indexOf(tempIdent));
						} else {
							this.identTable.add(tempIdent);
							this.setId(this.identTable.size() - 1);
						}
						return identifier;
					}

				}
			}
			return eofToken;
			
		} else if(inputSym == '{') {
			
			next();
			return beginToken;
			
		} else if(inputSym == '}') {
			
			next();
			return endToken;
			
		} else if(inputSym == '[') {
			
			next();
			return openbracketToken;
			
		} else if(inputSym == ']') {
			
			next();
			return closebracketToken;
			
		} else if(inputSym == '(') {
			
			next();
			return openparenToken;
			
		} else if(inputSym == ')') {
			
			next();
			return closeparenToken;
			
		} else if(inputSym == '+') {
			
			next();
			return plusToken;
			
		} else if(inputSym == '-') {
			
			next();
			return minusToken;
			
		} else if(inputSym == '*') {
			
			next();
			return timesToken;
			
		} else if(inputSym == '/') {

			next();
			if(inputSym == '/' ) {
				return commentToken;
			} else {
				return divToken;
			}
			
		} else if(inputSym == ',') {
			
			next();
			return commaToken;
			
		} else if(inputSym == ';') {
			
			next();
			return semiToken;
			
		} else if(inputSym == '.') {
			
			next();
			return periodToken;
			
		} else if(inputSym == '<') {
			
			next();
			if(inputSym == '=') {
				next();
				return leqToken;
			} else if(inputSym == '-') {
				next();
				return becomesToken;
			} else {
				return lssToken;
			}		
	
		} else if(inputSym == '>') {
			
			next();
			if(inputSym == '=') {
				next();
				return geqToken;
			} else {
				return gtrToken;
			}		

		} else if(inputSym == '!') {
				next();
				if(inputSym == '=') {
					next();
					return neqToken;
				} else {
					//error
					return errorToken;
				}
		}  else if(inputSym == '=') {
			next();
			if(inputSym == '=') {
				next();
				return eqlToken;
			} else {
				//error
				printError("\"=\" should be followed by \"=\"");
				return errorToken;
			}
		} else {
			next();
			//error
			printError("illegal character: " + inputSym);
			return errorToken;
		}	
	}

	public int getVal() {
		return val;
	}

	public void setVal(int val) {
		this.val = val;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	private void printError(String errMsg) {
		System.out.println("Syntax error at " + curLine + ": " + errMsg);
	}
}
