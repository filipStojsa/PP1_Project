package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class CodeGenerator extends VisitorAdaptor {
	
	/* ***** MainPC part ***** */
	int mainPC = 0;
	
	public int getMainPc() {
		return mainPC;
	}
	
	/* ***** Helper fields ***** */
	Stack<Obj> funcCalls = new Stack<>();
	Obj currMethod = null;
	boolean isReturnStmtFound = false;
	
	/* Stacks */
	
	Stack<List<Integer>> andPatchs = new Stack<>();
	Stack<List<Integer>> orPatchs = new Stack<>();
	Stack<List<Integer>> elsePatchs = new Stack<>();
	Stack<List<Integer>> breakPatchs = new Stack<>();
	
	Stack<Integer> loopsStack = new Stack<>();
	Stack<Integer> foreachStack = new Stack<>();
	
	/* ***** Methods ***** */
	
	// Entering the method
	public void visit(MethodRetVoid methodRetVoid) {
		currMethod = methodRetVoid.obj;
		
		if("main".equals(methodRetVoid.getNameMeth())) {
			mainPC = Code.pc;
		}
		
		methodRetVoid.obj.setAdr(Code.pc);
		
		Code.put(Code.enter);
		Code.put(methodRetVoid.obj.getLevel());
		Code.put(methodRetVoid.obj.getLocalSymbols().size());
	}
	
	public void visit(MethodRetType methodRetType) {
		currMethod = methodRetType.obj;
		
		if("main".equals(methodRetType.getNameMeth())) {
			mainPC = Code.pc;
		}
		
		methodRetType.obj.setAdr(Code.pc);
		
		Code.put(Code.enter);
		Code.put(methodRetType.obj.getLevel());
		Code.put(methodRetType.obj.getLocalSymbols().size());
	}
	
	private void resetMethodHelpers() {
		currMethod = null;
		isReturnStmtFound = false;
	}
	
	// Exiting the method
	public void visit(MethodDecl methodDecl) {
		if(isReturnStmtFound == false) {
			// there is no return statement
			if(methodDecl.obj.getType() == Tab.noType) {
				// it's void -> OK
				Code.put(Code.exit);
				Code.put(Code.return_);
			}
			else {
				// it should have return -> error
				Code.put(Code.trap);
				Code.put(1);
			}
			
			resetMethodHelpers();
		}
		else {
			resetMethodHelpers();
		}
	}
	
	// ReturnStmt is found
	public void visit(ReturnStmt returnStmt) {
		isReturnStmtFound = true;
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(ReturnStmtNoExpr returnStmt) {
		isReturnStmtFound = true;
		
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	/* ***** Print & Read ***** */
	
	public void visit(PrintStmt printStmt) {
		if(printStmt.getExpr().struct == Tab.intType || printStmt.getExpr().struct == SemanticAnalyzer.boolType) {
			Code.put(Code.print);
		}
		else {
			Code.put(Code.bprint);
		}
	}
	
	public void visit(WithAdditionalPrint wPrint) {
		int w = wPrint.getPrintW();
		Code.loadConst(w);
	}
	
	public void visit(NoAdditionalPrint woPrint) {
		SyntaxNode syntaxNode = woPrint.getParent();
		PrintStmt parent = (PrintStmt) syntaxNode;
		Struct structType = parent.getExpr().struct;
		
		if(structType == Tab.charType) {
			Code.loadConst(1);
		}
		else {
			Code.loadConst(5);
		}
	}
	
	public void visit(ReadStmt readStmt) {
		Struct type = readStmt.getDesignator().obj.getType();
		if(type == Tab.charType) {
			Code.put(Code.bread);
		}
		else {
			Code.put(Code.read);
		}
		
		Code.store(readStmt.getDesignator().obj);
	}
	
	/* ***** Term ***** */
	
	public void visit(TermMultipleFactors term) {
		if(term.getMulop() instanceof MultiplyOp) {
			Code.put(Code.mul);
		}
		else if(term.getMulop() instanceof DivOp) {
			Code.put(Code.div);
		}
		else if(term.getMulop() instanceof ModOp) {
			Code.put(Code.rem);
		}
	}
	
	/* ***** Expr ***** */
	
	public void visit(ExpryMinusDummy wMinus) {
		// If the expression is negative
		if(((ExprWith)wMinus.getParent()).getExprMinus() instanceof ExprMinusWith)
			Code.put(Code.neg);
	}
	
	public void visit(ExprTermMultiple exprTermMultiple) {
		if(exprTermMultiple.getAddop() instanceof MinusOp) {
			Code.put(Code.sub);
		}
		else if(exprTermMultiple.getAddop() instanceof PlusOp) {
			Code.put(Code.add);
		}
	}
	
	/* ***** Factor ***** */
	
	public void visit(FactorNoParens fdesignator) {
		Code.load(fdesignator.getDesignator().obj); 
	}
	 
	
	public void visit(FactorNumConst fnum) {
		Code.loadConst(fnum.getN1());
	}
	
	public void visit(FactorCharConst fchar) {
		int asciiChar = fchar.getC1();
		Code.loadConst(asciiChar);
	}
	
	public void visit(FactorBoolConst fbool) {
		int boolConst = fbool.getB1() ? 1 : 0;
		Code.loadConst(boolConst);
	}
	
	public void visit(FactorNewExpr farray) {
		if(farray.struct.getElemType() == Tab.charType) {
			Code.put(Code.newarray);
			Code.put(0);
		}
		else {
			Code.put(Code.newarray);
			Code.put(1);
		}
	}
	
	// Push to stack designator for function call
	public void visit(DesignatorForActPars factorFuncCall) {
		funcCalls.push(factorFuncCall.getDesignator().obj);
	}
	
	// Actual call of function from factors
	public void visit(FactorWithActPars funcCall) {
		funcCalls.pop();

		String fName = funcCall.getDesignatorForActPars().getDesignator().obj.getName();
		int fAdr = funcCall.getDesignatorForActPars().getDesignator().obj.getAdr();

		switch (fName) {
		case "ord":
			// Code.put(Code.call);
			// Code.put2(1 + fAdr - Code.pc);

			break;

		case "len":
			Code.put(Code.arraylength);
			break;

		case "chr":
			// Code.put(Code.call);
			// Code.put2(1 + fAdr - Code.pc);

			break;

		default:
			Code.put(Code.call);
			Code.put2(1 + fAdr - Code.pc);
			break;
		}
		
		// Code.put2(1 + fAdr - Code.pc);
		
	}
	
	/* ***** Designators ***** */

	public void visit(ArryDesignator arrayDesignator) {
		Code.load(arrayDesignator.getDesignator().obj);
	}
	
	/* ***** DesignatorStatement ***** */
	
	public void visit(DesignatorAssignop designatorAssignop) {
		Code.store(designatorAssignop.getDesignator().obj);
	}
	
	public void visidDesignatorIncDec(Obj object, int code) {
		//Code.put(Code.dup2);
		Code.load(object);
		Code.loadConst(1);
		Code.put(code);
		Code.store(object);
	}
	
	public void visit(DesignatorIncrement designatorIncrement) {
		Obj designatorIncObj = designatorIncrement.getDesignator().obj;
		visidDesignatorIncDec(designatorIncObj, Code.add);
	}
	
	public void visit(DesignatorDecrement designatorDecrement) {
		Obj designatorDecObj = designatorDecrement.getDesignator().obj;
		visidDesignatorIncDec(designatorDecObj, Code.sub);
	}
	
	public void visit(DesignatorForFunc factorFuncCall) {
		funcCalls.push(factorFuncCall.getDesignator().obj);
	}
	
	// The actual function call itself
	public void visit(DesignatorActPars funcCall) {
		funcCalls.pop();

		String fName = funcCall.getDesignatorForActPars().getDesignator().obj.getName();
		int fAdr = funcCall.getDesignatorForActPars().getDesignator().obj.getAdr();

		switch (fName) {
		case "ord":
			// Code.put(Code.call);
			break;

		case "len":
			Code.put(Code.arraylength);
			break;

		case "chr":
			// Code.put(Code.call);
			break;

		default:
			int tmp = fAdr - Code.pc;
			Code.put(Code.call);
			Code.put2(tmp);
			break;
		}
		
		
		
		if(funcCall.getDesignatorForActPars().getDesignator().obj.getType() == Tab.noType) {
			Code.put(Code.pop);
		}
		// Popped form calls stack on top!
	}
	
	// Assignment to arrays
	
	List<Obj> listToAssing = new ArrayList<>();
	
	public void visit(DesignatorsList designatorsList) {
		int numOfParams = listToAssing.size();
		Code.loadConst(numOfParams);
		
		Obj rightDesignator = designatorsList.getDesignator().obj;
		Code.load(rightDesignator);
		
		Code.put(Code.arraylength);
		
		Code.put(Code.jcc + Code.le);
		
		Code.put2(5);
		Code.put(Code.trap);
		Code.put(2);
		
		int i = 0;
		while(i < numOfParams) {
			Obj node = listToAssing.get(i);
			
			if(node == null) {
				i++;
			}
			else {
				// get value of right side
				Code.load(rightDesignator);
				
				// load position of obj in index
				int j = listToAssing.indexOf(node);
				Code.loadConst(j);
				
				// put function code
				Code.put(Code.aload);
				
				// store modified objects
				Code.store(node);
				i++;
			}
		}
		
		listToAssing.clear();
		
	}
	
	public void visit(MoreDesignators moreDesignators) {
		OptDesignator optDes = moreDesignators.getOptDesignator();
		if(optDes instanceof OptDesignatorExsists) {
			Obj obj = ((OptDesignatorExsists)optDes).getDesignator().obj;
			listToAssing.add(obj);
		}
		else {
			listToAssing.add(null);
		}
	}
	
	public void visit(SingleDesignator singleDesignator) {
		OptDesignator optDes = singleDesignator.getOptDesignator();
		if(optDes instanceof OptDesignatorExsists) {
			Obj obj = ((OptDesignatorExsists)optDes).getDesignator().obj;
			listToAssing.add(obj);
		}
		else {
			listToAssing.add(null);
		}
	}
	
	/* ***** CondFact ***** */
	
	public void visit(CondFactMultipleExpr condFactMultipleExpr) {
		List<Integer> tmp = andPatchs.pop();
		tmp.add(Code.pc + 1);
		andPatchs.push(tmp);
		
		Relop relop = condFactMultipleExpr.getRelop();
		
		if(relop instanceof EqOp) {
			Code.putFalseJump(Code.eq, 0);
		}
		else if(relop instanceof NeqOp) {
			Code.putFalseJump(Code.ne, 0);
		}
		else if(relop instanceof GreaterOp) {
			Code.putFalseJump(Code.gt, 0);
		}
		else if(relop instanceof GreaterEqOp) {
			Code.putFalseJump(Code.ge, 0);
		}
		else if(relop instanceof LessOp) {
			Code.putFalseJump(Code.lt, 0);
		}
		else if(relop instanceof LessEqOp) {
			Code.putFalseJump(Code.le, 0);
		}
	}
	
	public void visit(CondFactSingleExpr condFactSingleExpr) {
		Code.loadConst(1);
		
		List<Integer> tmp = null;
		tmp = andPatchs.pop();
		
		int adrAdd = 0;
		adrAdd = Code.pc + 1;
		
		tmp.add(adrAdd);
		andPatchs.push(tmp);
		
		Code.putFalseJump(Code.eq, 0);
	}
	
	/* ***** CondTerm ***** */
	
	public void visit(HelperCondFact helperCondFact) {
		// Helper nonterminal after If Conditions
		List<Integer> orTmp = orPatchs.pop();
		orTmp.add(Code.pc + 1);
		orPatchs.push(orTmp);
		
		Code.putJump(0);
		
		List<Integer> andTmp = andPatchs.pop();
		while(!andTmp.isEmpty()) {
			Code.fixup(andTmp.remove(0));
		}
		
		// Push empty list on the stack
		andPatchs.push(andTmp);
	}
	
	public void visit(HelperCondTerm helperCondTerm) {
		// Helper nonterminal before OR call
		List<Integer> orTmp = orPatchs.pop();
		while(!orTmp.isEmpty()) {
			Code.fixup(orTmp.remove(0));
		}
		
		// Push empty list on the stack
		orPatchs.push(orTmp);
	}
	
	public void visit(IfElseEpsilon ifElseEpsilon) {
		// Helper nonterminal to patch conditions
		List<Integer> elseTmp = elsePatchs.pop();
		elseTmp.add(Code.pc + 1);
		elsePatchs.push(elseTmp);
		
		Code.putJump(0);
		
		List<Integer> andTmp = andPatchs.pop();
		while(!andTmp.isEmpty()) {
			Code.fixup(andTmp.remove(0));
		}
		
		// Push empty list on the stack
		andPatchs.push(andTmp);
	}
	
	/* ***** If-Else ***** */
	
	public void visit(IfStartHelper ifStartHelper) {
		andPatchs.push(new ArrayList<>());
		orPatchs.push(new ArrayList<>());
		elsePatchs.push(new ArrayList<>());
	}
	
	public void visit(IfStmt ifStmt) {
		// Fixup ands
		List<Integer> andTmp = andPatchs.pop();
		elsePatchs.pop();
		orPatchs.pop();
		while(!andTmp.isEmpty()) {
			Code.fixup(andTmp.remove(0));
		}
	}
	
	public void visit(IfElseStmt ifElseStmt) {
		// Fixup elses
		List<Integer> elseTmp = elsePatchs.pop();
		andPatchs.pop();
		orPatchs.pop();
		while(!elseTmp.isEmpty()) {
			Code.fixup(elseTmp.remove(0));
		}
	}
	
	/* ***** Loops ***** */
	
	void patchCodeFixup(List<Integer> patchList) {
		for(int i = 0; i < patchList.size(); i++) {
			int fixupAdr = 0;
			fixupAdr = patchList.get(i);
			
			Code.fixup(fixupAdr);
		}
	}
	
	// While loop ends
	public void visit(WhileStmt whileStmt) {
		int whileFromStackAdr = 0;
		whileFromStackAdr = loopsStack.pop();
		
		Code.putJump(whileFromStackAdr);
		
		// Patch and jumps
		List<Integer> andTmpList = andPatchs.pop();
		patchCodeFixup(andTmpList);
		
		// Patch break jumps
		List<Integer> breakTmpList = breakPatchs.pop();
		patchCodeFixup(breakTmpList);
		
		// Pop two other stacks
		orPatchs.pop();
		elsePatchs.pop();
	}
	
	// While loop starts
	public void visit(BeginWhile beginWhile) {
		int whilePcAdress = 0;
		whilePcAdress = Code.pc;
		
		loopsStack.push(whilePcAdress);
		
		breakPatchs.push(new ArrayList<>());
		elsePatchs.push(new ArrayList<>());
		andPatchs.push(new ArrayList<>());
		orPatchs.push(new ArrayList<>());
	}
	
	// Break and Continue ends
	public void visit(ContinueStmt continueStmt) {
		int lastWhileAdr = loopsStack.pop();
		Code.putJump(lastWhileAdr);
		loopsStack.push(lastWhileAdr);
	}
	
	public void visit(BreakStmt breakStmt) {
		List<Integer> breakList = breakPatchs.pop();
		int adressToAdd = Code.pc + 1;
		breakList.add(adressToAdd);
		breakPatchs.push(breakList);
		
		Code.putJump(0);
	}
	
	// Foreach
	
	public void visit(ForeachStmt foreachStmt) {
		int condAdr = 0;
		condAdr = loopsStack.pop();	// This is address of foreach
		Code.putJump(condAdr);
		
		int fixupAdr = 0;
		fixupAdr = foreachStack.pop();
		Code.fixup(fixupAdr);		// In place of false jump bellow
		
		List<Integer> breaksList = breakPatchs.pop();
		patchCodeFixup(breaksList);
		
		// Pop the remaining
		Code.put(Code.pop);
		Code.put(Code.pop);
	}
	
	public void visit(ForeachDesignator foreachDesignator) {
		Obj designatorNode = null;
		designatorNode = foreachDesignator.getDesignator().obj;
		Code.load(designatorNode);
		
		int arrayAdr = -1;
		Code.loadConst(arrayAdr);
		
		// Push foreach to loop stack
		int foreachAdr = Code.pc;
		loopsStack.push(foreachAdr);
		breakPatchs.push(new ArrayList<>());
		
		codeManipulationForeach();
		// Expression stack: arrAdr, i, i, arrLen
		
		int passForeach = Code.pc + 1;
		foreachStack.push(passForeach);
		
		// If we didn't go through whole arr, go to top
		Code.putFalseJump(Code.lt, 0);
		Code.put(Code.dup2);
		
		Obj node = null;
		
		if(foreachDesignator.getParent() instanceof ForeachStmt) {
			node = ((ForeachStmt)foreachDesignator.getParent()).getForeachIdent().obj;
			
			if(node.getType() != Tab.charType) {
				Code.put(Code.aload);
			}
			else {
				Code.put(Code.baload);
			}
			
			Code.store(node);
		}
		
		// You shouldn't be here...
	}

	private void codeManipulationForeach() {
		
		Code.loadConst(1);
		Code.put(Code.add);
    	Code.put(Code.dup2);
    	Code.put(Code.pop);
    	
    	// Put array length - for num of iterations
    	Code.put(Code.arraylength);
    	
    	Code.put(Code.dup2);
    	Code.put(Code.pop);
    	
    	Code.put(Code.dup_x1);
    	Code.put(Code.pop);
	}
	
}
