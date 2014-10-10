/*
 * Methods implemented my
 * Matthew Bowyer
 * 156009078
 * Oct 10th 2014
 */

package apps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;

import structures.Stack;

public class Expression {

	/**
	 * Expression to be evaluated
	 */
	String expr;                
    
	/**
	 * Scalar symbols in the expression 
	 */
	ArrayList<ScalarSymbol> scalars;   
	
	/**
	 * Array symbols in the expression
	 */
	ArrayList<ArraySymbol> arrays;
    
	/**
	 * Positions of opening brackets
	 */
	ArrayList<Integer> openingBracketIndex; 
    
	/**
	 * Positions of closing brackets
	 */
	ArrayList<Integer> closingBracketIndex; 

    /**
     * String containing all delimiters (characters other than variables and constants), 
     * to be used with StringTokenizer
     */
    public static final String delims = " \t*+-/()[]";
    
    /**
     * Initializes this Expression object with an input expression. Sets all other
     * fields to null.
     * 
     * @param expr Expression
     */
    public Expression(String expr) {
        this.expr = expr;
        scalars = null;
        arrays = null;
        openingBracketIndex = null;
        closingBracketIndex = null;
    }

    /**
     * Matches parentheses and square brackets. Populates the openingBracketIndex and
     * closingBracketIndex array lists in such a way that closingBracketIndex[i] is
     * the position of the bracket in the expression that closes an opening bracket
     * at position openingBracketIndex[i]. For example, if the expression is:
     * <pre>
     *    (a+(b-c))*(d+A[4])
     * </pre>
     * then the method would return true, and the array lists would be set to:
     * <pre>
     *    openingBracketIndex: [0 3 10 14]
     *    closingBracketIndex: [8 7 17 16]
     * </pe>
     * 
     * See the FAQ in project description for more details.
     * 
     * @return True if brackets are matched correctly, false if not
     */
    public boolean isLegallyMatched() { //O(n)
    	openingBracketIndex=new ArrayList<Integer>();
    	closingBracketIndex=new ArrayList<Integer>();
    	
    	Stack<Bracket> brackets = new Stack<Bracket>();
    	
    	for(int i=0;i<expr.length();i++){
    		// Go through the characters and if the bracket is open add it, if closed subtract it
    		if(expr.charAt(i)=='['||expr.charAt(i)=='('){
    			brackets.push(new Bracket(expr.charAt(i), i));
    			openingBracketIndex.add(i);
    		}
    		else if(expr.charAt(i)==']'){
    			if(brackets.isEmpty()||brackets.pop().ch!='[')return false;
    			closingBracketIndex.add(i);
    		}
    		else if(expr.charAt(i)==')'){
    			if(brackets.isEmpty()||brackets.pop().ch!='(')return false;
    			closingBracketIndex.add(i);
    		}
    	}
    	
    	if(brackets.isEmpty())return true; //if the stack ends empty, all the brackets matched up
    	return false;
    }

    /**
     * Populates the scalars and arrays lists with symbols for scalar and array
     * variables in the expression. For every variable, a SINGLE symbol is created and stored,
     * even if it appears more than once in the expression.
     * At this time, values for all variables are set to
     * zero - they will be loaded from a file in the loadSymbolValues method.
     */
    public void buildSymbols() {
    	arrays = new ArrayList<ArraySymbol>();
    	scalars = new ArrayList<ScalarSymbol>();

    	for(int i=0;i<expr.length();i++){ //Goes through each character of expression
    		String symbolName = "";
       		while(i<expr.length() && Character.isLetter(expr.charAt(i))){
    			symbolName+=expr.charAt(i);
    			i++;
    		} //while the chars are letters add it to the symbols's name
       		boolean found = false;
    		if(symbolName!=""){ //If a symbol was found build it	
    			if(i<expr.length() && expr.charAt(i)=='['){ //if a bracket follows it add it to the arrays
    				for(ArraySymbol as: arrays)if(as.name.equals(symbolName))found=true; // prevents duplicates
    				if(!found)arrays.add(new ArraySymbol(symbolName));
    			}
    			else{//otherwise add it it the scalars
    				for (ScalarSymbol ss: scalars)if(ss.name.equals(symbolName))found=true;
    				if(!found)scalars.add(new ScalarSymbol(symbolName));
    			}
    		}// finish building symbols
    	}
    	printScalars();
    	printArrays();
    }
    
    
    /**
     * Loads values for symbols in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     */
    public void loadSymbolValues(Scanner sc) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String sym = st.nextToken();
            ScalarSymbol ssymbol = new ScalarSymbol(sym);
            ArraySymbol asymbol = new ArraySymbol(sym);
            int ssi = scalars.indexOf(ssymbol);
            int asi = arrays.indexOf(asymbol);
            if (ssi == -1 && asi == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                scalars.get(ssi).value = num;
            } else { // array symbol
            	asymbol = arrays.get(asi);
            	asymbol.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    String tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    asymbol.values[index] = val;              
                }
            }
        }
    }
    
    /**
     * Evaluates the expression, using RECURSION to evaluate subexpressions and to evaluate array 
     * subscript expressions.
     * 
     * @return Result of evaluation
     */
    public float evaluate() {
    	float sum = 0;
    	Stack<Expression> expressions = new Stack<Expression>();
    	Stack<Character> operands = new Stack<Character>(); 	

    	// -----------------------------------------------------------Separates the expression into stacks--------------
    	int pos = expr.length()-1;											//searches for expressions
    	while(pos>=0){														//Goes backwards through the string expression 
    																		//leaves position after expression   		
    		int[] exprIndices = null; 										//searches for subexpression
    		if(expr.charAt(pos)==']'|| expr.charAt(pos)==')'){	
    			exprIndices = new int[2];
    			pos++;
    			exprIndices[1]=pos;
    			Stack<Bracket> brackets = new Stack<Bracket>();				//adds to stack if a
    			do{
    				pos--;
    				if(expr.charAt(pos)==')' || expr.charAt(pos)==']')brackets.push(new Bracket(expr.charAt(pos),pos));
    				if(expr.charAt(pos)=='(' || expr.charAt(pos)=='[')brackets.pop();
    			}while(!brackets.isEmpty());
    			exprIndices[0]=pos;
    			pos--;
    		}
    		int[] symbolIndices = null; 									//searches for a symbol
    		if(pos>=0 && Character.isLetter(expr.charAt(pos))){				//symbol+subexpression = arraySymbol
    			symbolIndices = new int[2];
    			symbolIndices[1]=pos+1;
    			while(pos>0 && Character.isLetter(expr.charAt(pos-1)))pos--;
    			symbolIndices[0]=pos;
    			pos--;												
    		}														
    		int[] numIndices = null;										//searches for a number
    		if(pos>=0 && Character.isDigit(expr.charAt(pos))){
    			numIndices = new int[2];
    			numIndices[1]=pos+1;
    			while(pos>0 && Character.isDigit(expr.charAt(pos-1)))pos--;
    			numIndices[0]=pos;									
    			pos--;												
    		}														
    		int[] operandIndices = null;									//searches for ascii value between 42(*) & 47(/)
    		if(pos>=0 &&((int)expr.charAt(pos)>=42&& (int)expr.charAt(pos)<=47)){
    			operandIndices = new int[2];
    			operandIndices[1]=pos+1;							
    			operandIndices[0]=pos;
    			pos--;
    		}
    		while(pos>=0 && Character.isWhitespace(expr.charAt(pos)))pos--;	//handles white spaces	
    																		//Adds expressions and operands to the stacks
    		String e = null;
    		String o = null;
    		if(symbolIndices!=null&&exprIndices!=null) 	e = expr.substring(symbolIndices[0],exprIndices[1]);
    		if(symbolIndices!=null&&exprIndices==null) 	e = expr.substring(symbolIndices[0],symbolIndices[1]);
    		if(symbolIndices==null&&exprIndices!=null)	e = expr.substring(exprIndices[0],exprIndices[1]);
    		if(numIndices!=null)   						e= expr.substring(numIndices[0], numIndices[1]);
    		if(operandIndices!=null)				   	o = expr.substring(operandIndices[0],operandIndices[1]);
    		
    		if(e!=null){											
    			Expression newExpr = new Expression(e);
    			newExpr.arrays = arrays;
        		newExpr.scalars = scalars;
        		expressions.push(newExpr);
    		}
    		if(o!=null){
    			operands.push(o.charAt(0));
    		}
    		
    	}    	
    	//--------------------------------------------Base Cases-----------------------------------------------
    	if(expressions.size()==0)return 0;
    	else if(expressions.size()==1){
    		Expression tempExpr = expressions.pop();
    		String eval = tempExpr.expr;
    		if(Character.isDigit(eval.charAt(eval.length()-1))){	//if the expression is a number return its value
    			return Float.parseFloat(eval); 						
    		}
    		else if(Character.isLetter(eval.charAt(eval.length()-1))){//if the expression is a symbol return its scalar
    			for(ScalarSymbol ss: scalars) if(ss.name.equals(eval))return ss.value;
    		}														
    		else if(eval.charAt(eval.length()-1)==']'){				//if the expression is an array symbol
    			String arrayName= "";
    			int c = 0;
    			while(Character.isLetter(eval.charAt(c))){			//gets the array name
    				arrayName += eval.charAt(c);					
    				c++;
    			}
    			tempExpr.expr= eval.substring(c+1,eval.length()-1);	//returns the element at the index of the subexpression
    			for(ArraySymbol as: arrays) if(as.name.equals(arrayName)) return as.values[(int)tempExpr.evaluate()];									
    		}
    		else if(eval.charAt(eval.length()-1)==')'){				//if it is a subexpression evaluate the inner
    			tempExpr.expr=eval.substring(1, eval.length()-1);
    			return tempExpr.evaluate();
    		}
    	}
    	//-------------------------------------------Dealing with the stack---------------------------
    	if(operands.size()==expressions.size()) {
    		sum =expressions.pop().evaluate();
    		if(operands.pop()=='-')sum *=-1;						//Checks for first negative operator 
    		
    	}else sum = expressions.pop().evaluate();					//Makes the sum the first expression  
    																  	
    	while(!expressions.isEmpty()){
    		char n = ' ', o = operands.pop();
    		if(!operands.isEmpty()) n = operands.peek();								
    		if(o=='+'||o=='-'){
    			float add;
    			if(n=='*'||n=='/'){									//checks if multiplication is in the next expression
    				add = expressions.pop().evaluate();				//keeps the product to be added to the antecedent
    				while(!expressions.isEmpty()&&(operands.peek()=='*'||operands.peek()=='/')){
    					char operation = operands.pop();
    					float val = expressions.pop().evaluate();
    					if(operation=='*')		add*=val;
    					else if(operation=='/')	add/=val;
    				}
    			}else add=expressions.pop().evaluate();				//if the next operation is not multiplication
    			if(o=='+')sum+=add;									//just add the next expression to the sum
    			else if(o=='-')sum-=add;
    		}else{													//multiply if the original operation was * or /
    			if(o=='*')sum*=expressions.pop().evaluate();
    			if(o=='/')sum/=expressions.pop().evaluate();
    		}	
    	}	
    	return sum;
    	
    }

    /**
     * Utility method, prints the symbols in the scalars list
     */
    public void printScalars() {
        for (ScalarSymbol ss: scalars) {
            System.out.println(ss);
        }
    }
    
    /**
     * Utility method, prints the symbols in the arrays list
     */
    public void printArrays() {
    	for (ArraySymbol as: arrays) {
    		System.out.println(as);
    	}
    }

}
