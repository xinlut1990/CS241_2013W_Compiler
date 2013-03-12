package cs241_compiler;

public class Token {
	//predefinition of token values
	public static final int errorToken = 0; //error
	
	public static final int timesToken = 1; //'*'
	public static final int divToken = 2; //'/'
	
	public static final int plusToken = 11; //'+'
	public static final int minusToken = 12; //'-'
	
	public static final int eqlToken = 20; //'=='
	public static final int neqToken = 21; //'!='
	public static final int lssToken = 22; //'<'
	public static final int geqToken = 23; //'>='
	public static final int leqToken = 24; //'<='
	public static final int gtrToken = 25; //'>'
	
	public static final int periodToken = 30; //'.'
	public static final int commaToken = 31; //','
	public static final int openbracketToken = 32; //'['
	public static final int closebracketToken = 34; //']'
	public static final int closeparenToken = 35; //')'
	
	public static final int becomesToken = 40; //'<-'
	public static final int thenToken = 41; //'then'	
	public static final int doToken = 42; //'do'
	
	public static final int openparenToken = 50; //'('
	
	public static final int number = 60; //number
	public static final int identifier = 61; //identifier
	
	public static final int semiToken = 70; //';'
	
	public static final int endToken = 80; //'}'
	public static final int odToken = 81; //'od'
	public static final int fiToken = 82; //'fi'
	public static final int elseToken = 90; //'else'
	
	public static final int letToken = 100; //'let'
	public static final int callToken = 101; //'call'
	public static final int ifToken = 102; //'if'
	public static final int whileToken = 103; //'while'
	public static final int returnToken = 104; //'return'
	
	public static final int varToken = 110; //'var'
	public static final int arrToken = 111; //'array'
	public static final int funcToken = 112; //'function'
	public static final int procedureToken = 113; //'procedure'
	
	public static final int beginToken = 150; //'{'
	public static final int mainToken = 200; //'main'
	public static final int commentToken = 201; //'main'
	public static final int eofToken = 255; // end of line
}
