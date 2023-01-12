package rs.ac.bg.etf.pp1;

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
	Stack<Obj> actParsCalls = new Stack<>();
	Obj currMethod = null;
	boolean isReturnStmtFound = false;
	
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
		// TODO: Staviti nes na Code??
	}
	
	/* ***** Print & Read ***** */
	// TODO: Vidi da li moze da se uradi sa Print kako si ti prvobitno uradio - vidi diff
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
	
	public void visit(ExprWith wMinus) {
		// If the expression is negative
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
	
	//TODO: VELIKI PROBLEM OVDE!!!
	/*public void visit(FactorNoParens fdesignator) {
		Code.load(fdesignator.getDesignator().obj); 
	}*/
	 
	
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
		// TODO: zasto ovde 1 i 0??
		if(farray.struct.getElemType() == Tab.charType) {
			Code.put(Code.newarray);
			Code.put(0);
		}
		else {
			Code.put(Code.newarray);
			Code.put(1);
		}
	}
	
	/* ***** Designators ***** */
	
	// TODO: Da li mi treba? Treba ovde jos posla...
	/*
	 * public void visit(DesignatorIdent designatorIdent) {
	 * 
	 * }
	 */
	
	public void visit(ArryDesignator arrayDesignator) {
		Code.load(arrayDesignator.getDesignator().obj);
	}
	
	/* ***** DesignatorStatement ***** */
	
	public void visit(DesignatorAssignop designatorAssignop) {
		Code.store(designatorAssignop.getDesignator().obj);
	}
	
	public void visidDesignatorIncDec(Obj object, int code) {
		Code.put(Code.dup2);
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
	
	
}
