package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor {
	
	public SemanticAnalyzer() {
		// Redefine constructor to add boolean object
		Tab.currentScope().addToLocals(new Obj(Obj.Type, "bool", new Struct(Struct.Bool)));
	}
	
	Logger log = Logger.getLogger(getClass());
	
	/* Helper variables */
	boolean errorDetected = false;
	int nVars = 0;
	Struct currType;
	
	Obj currentMethod = Tab.noObj;
	String currentMethodName = "";
	boolean currentMethodreturnCheck = false;
	int currentMethodParamCnt = 0;
	
	List<Obj> parsList = new ArrayList<>();
	
	List<Obj> globalMethods = new ArrayList<>();

	int nestedLoops = 0;
	
	Obj foreachObj = null;
	
	List<Obj> listToAssign = new ArrayList<>();
	
	enum Ops {
		PLUS, MINUS, EQ, MUL, DIV, MOD, EQ_CMP, NEQ_CMP, GREATER, GREATER_EQ, LESS, LESS_EQ
	};
	Ops currOperator = null;
	
	/* Helper functions */
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	public boolean passed() {
		return !errorDetected;
	}
	
	/* ************ Program ************ */
	
	// Program start
	public void visit(ProgName progName) {
		progName.obj = Tab.insert(Obj.Prog, progName.getProgName(), Tab.noType);
		Tab.openScope();
	}
	
	// Program end
	public void visit(Program program) {
		nVars = Tab.currentScope.getnVars();
    	Tab.chainLocalSymbols(program.getProgName().obj);
    	Tab.closeScope();
	}
	
	/* ************ Type ************ */
	
	public void visit(Type type){
    	Obj typeNode = Tab.find(type.getTypeName());
    	if(typeNode == Tab.noObj){
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
    		type.struct = Tab.noType;
    	}else{
    		if(Obj.Type == typeNode.getKind()){
    			type.struct = typeNode.getType();
    		}else{
    			report_error("Greska: Ime " + type.getTypeName() + " ne predstavlja tip!", type);
    			type.struct = Tab.noType;
    		}
    	}
    	
    	currType = type.struct;
    }
	
	/* ************ Constants ************ */
	
	public boolean isConstantValid(String nameConst, SyntaxNode info, Struct typeConst) {
		if(!typeConst.equals(currType)) {
			report_error("Greska u tipu prilikom dodele vrednosti za konstantu " + nameConst, info);
			return false;
		}
		else if(Tab.find(nameConst) != Tab.noObj) {
			report_error("Greska pri deklarisanju imena konstante, ime " + nameConst + " vec postoji!", info);
			return false;
		}
		
		return true;
	}
	
	// Int Consts
	public void visit(ConstDeclarationNum constDeclarationNum) {
		String nameConst = constDeclarationNum.getNameConst();
		if(isConstantValid(nameConst, constDeclarationNum, Tab.intType)) {
			Obj node = Tab.insert(Obj.Con, nameConst, currType);
			report_info("Kreirana konstanta int tipa - " + nameConst, constDeclarationNum);
			
			node.setAdr(constDeclarationNum.getValue());
		}
			
	}
	
	// Char Consts
	public void visit(ConstDeclarationChar constDeclarationChar) {
		String nameConst = constDeclarationChar.getNameConst();
		if(isConstantValid(nameConst, constDeclarationChar, Tab.charType)) {
			Obj node = Tab.insert(Obj.Con, nameConst, currType);
			report_info("Kreirana konstanta char tipa - " + nameConst, constDeclarationChar);
			
			node.setAdr(constDeclarationChar.getValue());
		}
	}
	
	// Bool Consts 		!!!!! TODO: proveriti ovo za boolean
	public void visit(ConstDeclarationBool constDeclarationBool) {
		String nameConst = constDeclarationBool.getNameConst();
		if(isConstantValid(nameConst, constDeclarationBool, Tab.find("boolean").getType())) {
			Obj node = Tab.insert(Obj.Con, nameConst, currType);
			report_info("Kreirana konstanta char tipa - " + nameConst, constDeclarationBool);
			
			node.setAdr(constDeclarationBool.getValue() == true ? 1 : 0);
		}
	}
	
	/* ************ Variables ************ */
	// TODO: Da li treba da prolazi provera varijable ako njen tip ne postoji?
	// TODO: Da li treba razdvojiti prepoznavanje za globalne i lokalne?
	
	public boolean isVariableValid(String nameVar, SyntaxNode info) {
		Obj node = Tab.find(nameVar);
		if(node == Tab.noObj || Tab.currentScope().findSymbol(nameVar) == null) {
			return true;
		}
		else {
			report_error("Promenljiva imena " + nameVar + " vec deklarisana!", info);
			return false;
		}
	}
	
	// Variable arrays
	public void visit(LastVarDeclArray lastVarDeclArray) {
		String name = lastVarDeclArray.getNameVar();
		if(isVariableValid(name, lastVarDeclArray)) {
			Tab.insert(Obj.Var, name, new Struct(Struct.Array, currType));
			report_info("Uspesna deklaracija niza " + name, lastVarDeclArray);
		}
		else return;
	}
	
	public void visit(MoreVarDeclsArray moreVarDeclArray) {
		String name = moreVarDeclArray.getNameVar();
		if(isVariableValid(name, moreVarDeclArray)) {
			Tab.insert(Obj.Var, name, new Struct(Struct.Array, currType));
			report_info("Uspesna deklaracija niza " + name, moreVarDeclArray);
		}
		else return;
	}
	
	// Non variable arrays
	public void visit(LastVarDeclNoArray lastVarDeclNoArray) {
		String name = lastVarDeclNoArray.getNameVar();
		if(isVariableValid(name, lastVarDeclNoArray)) {
			Tab.insert(Obj.Var, name, currType);
			report_info("Uspesna deklaracija promenljive " + name, lastVarDeclNoArray);
		}
		else return;
	}
	
	public void visit(MoreVarDeclNoArray moreVarDeclNoArray) {
		String name = moreVarDeclNoArray.getNameVar();
		if(isVariableValid(name, moreVarDeclNoArray)) {
			Tab.insert(Obj.Var, name, currType);
			report_info("Uspesna deklaracija promenljive " + name, moreVarDeclNoArray);
		}
		else return;
	}
	
	/* ************ Method declaration ************ */
	
	private boolean isFirstGlobalMethodDeclaration(String nameMeth) {
		// TODO: Proveri dodatno da li je u istom opsegu eventualno
		Obj tmp = Tab.find(nameMeth);
		if(!tmp.equals(Tab.noObj)) {
			report_error("Globalna metoda imena " + nameMeth + " vec postoji!", null);
			return false;
		}
		return true;
	}
	
	// return types open new scope of the method
	public void visit(MethodRetVoid methodRetVoid) {
		Struct returnType = Tab.noType;
		currentMethodName = methodRetVoid.getNameMeth();
		if(!isFirstGlobalMethodDeclaration(currentMethodName)) {
			// TODO: Da li otvarati novi scope ili ne?
			currentMethod = Tab.noObj;
			methodRetVoid.obj = Tab.noObj;
			return;	
		}
		
		currentMethod = Tab.insert(Obj.Meth, currentMethodName, returnType);
		methodRetVoid.obj = currentMethod;
		Tab.openScope();
		
		report_info("Uspesno otvoren opseg za metodu : " + currentMethodName, methodRetVoid);
		
	}
	
	
	public void visit(MethodRetType methodRetType) {
		Struct returnType = methodRetType.getType().struct;
		currentMethodName = methodRetType.getNameMeth();
		if(!isFirstGlobalMethodDeclaration(currentMethodName)) {
			// TODO: Da li otvarati novi scope ili ne?
			currentMethod = Tab.noObj;
			methodRetType.obj = Tab.noObj;
			return;			
		}
		
		currentMethod = Tab.insert(Obj.Meth, currentMethodName, returnType);
		methodRetType.obj = currentMethod;
		Tab.openScope();
		
		report_info("Uspesno otvoren opseg za metodu : " + currentMethodName, methodRetType);
		
	}
	
	public void isMethodValid(Obj currentMethod) {
		Struct retType = currentMethod.getType();
		
		if(currentMethodParamCnt > 0 && currentMethodName.equals("main")) {
			report_error("Metoda main ne sme da ima parametre!", null);
		}
		
		if(!currentMethodreturnCheck && retType != Tab.noType) {
			report_error("Metoda " + currentMethodName + " nema return metodu, a nije void!", null);
		}
		
	}
	
	public void resetHelperFieldsMethods() {
		currentMethodreturnCheck = false;
		currentMethod = Tab.noObj;
		currentMethodName = "";
		currentMethodParamCnt = 0;
	}
	
	// the parent node closes the method's scope
	public void visit(MethodDecl methodDecl) {
		methodDecl.obj = currentMethod;
		
		// TODO: Da li ove provere uopste trebaju ovde??
		isMethodValid(currentMethod);	// used to report any errors
		
		// TODO: Da li proveravati overloaded metode?
		
		// set method parameter count and chain it to the table
		currentMethod.setLevel(currentMethodParamCnt);
		Tab.chainLocalSymbols(currentMethod);
		
		Tab.closeScope();
		
		report_info("Uspesno zatvoren opseg za metodu : " + currentMethodName, methodDecl);
		
		globalMethods.add(currentMethod);
		
		resetHelperFieldsMethods();
		
	}
	
	/* ************ Return ************ */

	private boolean checkReturnStatementValidity(Statement returnStmt) {
		// if return is not in function
		if(currentMethod == Tab.noObj) {
			report_error("Return naredba se ne nalazi ni u jednoj funkciji!", returnStmt);
			return false;
		}
		
		return true;
	}
	
	public void visit(ReturnStmt returnStmt) {
		if(!checkReturnStatementValidity(returnStmt)) {
			returnStmt.struct = Tab.noType;
		}
		else {
			// return statement found
			currentMethodreturnCheck = true;
			Struct returnType = currentMethod.getType();
			if(returnType == Tab.noType || returnType != returnStmt.getExpr().struct) {
				report_error("Pogresan tip return naredbe!", returnStmt);
				returnStmt.struct = Tab.noType;
				return;
			}
			
			// return is valid, set it
			returnStmt.struct = returnStmt.getExpr().struct;
			report_info("Return definisan korektno!", returnStmt);
		}
	}
	

	public void visit(ReturnStmtNoExpr returnStmtNoExpr) {
		if(!checkReturnStatementValidity(returnStmtNoExpr)) {
			returnStmtNoExpr.struct = Tab.noType;
		}
		else {
			// return statement found
			currentMethodreturnCheck = true;
			Struct returnType = currentMethod.getType();
			if(returnType != Tab.noType) {
				report_error("Tip return naredbe mora biti void!", returnStmtNoExpr);
				returnStmtNoExpr.struct = Tab.noType;
				return;
			}
			
			// return is valid, set it
			returnStmtNoExpr.struct = Tab.noType;
			report_info("Return definisan korektno!", returnStmtNoExpr);
		}
	}
	
	/* ************ FormPars ************ */
	
	private boolean isParamDefined(Obj node, FormParsDecl formParsDecl) {
		if(node != Tab.noObj && Tab.currentScope().findSymbol(node.getName()) != null) {
			// already defined in current scope -> error
			report_error("Parametar " + node.getName() + " je vec definisan u listi funkcije", formParsDecl);
			return true;
		}
		return false;
	}
	
	public void visit(FormParsDeclNoBrackets formPars) {
		// if it's NOT array
		String paramName = formPars.getName();
		Obj node = Tab.find(paramName);
		if(!isParamDefined(node, formPars)) {
			node = Tab.insert(Obj.Var, paramName, currType);
			report_info("Deklarisan parametar " + paramName + " ", formPars);
			
			parsList.add(node);
		}
	}

	public void visit(FormParsDeclBrackets formPars) {
		// if it's array
		String paramName = formPars.getName();
		Obj node = Tab.find(paramName);
		if(!isParamDefined(node, formPars)) {
			node = Tab.insert(Obj.Var, paramName, new Struct(Struct.Array, formPars.getType().struct));
			report_info("Deklarisan parametar kao niz " + paramName + " ", formPars);
		
			parsList.add(node);
		}
		
	}
	
	/* ************ ClassDecl  ************ */
	
	/*
	 * public void visit(BeginClassDecl classDecl) { String className =
	 * classDecl.getClassName(); Obj node = Tab.find(className);
	 * 
	 * if(node != Tab.noObj) { report_error("Vec postoji klasa imena " + className +
	 * "!", classDecl); return; }
	 * 
	 * Struct superclass = classDecl.getOptExtend().struct; //String superclassName
	 * = classes.get(superclass);
	 * 
	 * currClassName = className; currClassStruct = new Struct(Struct.Class,
	 * superclass); currClassStruct.setElementType(superclass);
	 * 
	 * Obj tmp = Tab.insert(Obj.Type, className, currClassStruct); classDecl.obj =
	 * tmp;
	 * 
	 * Tab.openScope();
	 * 
	 * report_info("Klasa " + className + " je kreirana i doseg otvoren!",
	 * classDecl); }
	 */
	
	/* ************ Loops ************ */
	
	// start of while loop
	public void visit(WhileCondition whileCondition) {
		nestedLoops++;
	}
	
	// end of while loop
	public void visit(WhileStmt whileStmt) {
		nestedLoops--;
	}
	
	public void visit(BreakStmt breakStmt) {
		if(nestedLoops != 0) {
			return;
		}
		else {
			report_error("BREAK naredba se mora naci unutar perlje!", breakStmt);
		}
	}
	
	public void visit(ContinueStmt continueStmt) {
		if(nestedLoops != 0) {
			return;
		}
		else {
			report_error("CONTINUE naredba se mora naci unutar perlje!", continueStmt);
		}
	}
	
	public void visit(ForeachDesignator foreachDesignator) {
		nestedLoops++;
		
		// foreach can't be used on non arrays
		int kind = foreachDesignator.getDesignator().obj.getKind();
		if(kind == Struct.Array) {
			// set the corresponding struct to statement
			foreachDesignator.struct = foreachDesignator.getDesignator().obj.getType().getElemType();
		}
		else {
			report_error("Foreach perlja mora koristiti nad nizovima!", foreachDesignator);
		}
	}
	
	public void visit(ForeachIdent foreachIdent) {
		Obj node = Tab.find(foreachIdent.getName());
		if(node != Tab.noObj) {
			// there is an object named like this
			foreachIdent.obj = node;
			if(node.getKind() == Obj.Var) {
				foreachObj = node;
			}
			else {
				// it's not variable, error
				report_error("Iterirajuci objekat nije promenljiva!", foreachIdent);
				foreachIdent.obj = Tab.noObj;
			}
		}
		else {
			// there is not
			report_error("Ne postoji objekat imena " + foreachIdent.getName(), foreachIdent);
			foreachIdent.obj = Tab.noObj;
		}
	}
	
	public void visit(ForeachStmt foreachStmt) {
		// closing of foreach loop
		nestedLoops--;
		if(!foreachStmt.getForeachDesignator().struct.equals(foreachObj.getType())) {
			report_error("Postoji neslaganje u tipu prilikom iteriranja!", foreachStmt);
		}
		
		foreachObj = null;
	}
	
	/* ************ Read & Print ************ */
	
	public void visit(PrintStmt printStmt) {
		Struct typeExpr = printStmt.getExpr().struct;
		if(
				typeExpr != Tab.intType &&
				typeExpr != Tab.charType &&
				typeExpr != new Struct(Struct.Bool)
		) {
			report_error("Prvi arg. print naredbe nije tipa int, char ili bool!", printStmt);
		}
			
	}
	
	public void visit(ReadStmt readStmt) {
		Struct designatorType = readStmt.getDesignator().obj.getType();
		int designatorKind = readStmt.getDesignator().obj.getKind();
		
		if(!isValidType(designatorType)) {
			report_error("READ nije ni char ni int ni bool!", readStmt);
		}
		else if(!isValidKind(designatorKind)) {
			report_error("READ izraz nije promenljiva ili element niza!", readStmt);
		}
	}

	private boolean isValidKind(int designatorKind) {
		if(designatorKind != Obj.Var && designatorKind != Obj.Elem) {
			return false;
		}
		return true;
	}

	private boolean isValidType(Struct designatorType) {
		if(designatorType != Tab.intType && designatorType != Tab.charType && designatorType != new Struct(Struct.Bool)) {
			return false;
		}
		return true;
	}
	
	/* ************ Designator ************ */
	
	public void visit(DesignatorIdent designatorIdent) {
		Obj node = Tab.find(designatorIdent.getNameDesignator());
		if(node != Tab.noObj) {
			// there is object
			designatorIdent.obj = node;
			int kind = node.getKind();
			
			switch (kind) {
				case Obj.Var:
					boolean found = false;
					
					for (Obj obj : parsList) {
						
						if(obj.equals(node)) {
							found = true;
							break;
						}
					}
					if(found) {
						report_info("Pristupa se parametru " + designatorIdent.getNameDesignator(), designatorIdent);
					}
					else {
						report_info("Pristupa se promenljivoj " + designatorIdent.getNameDesignator(), designatorIdent);
					}
					break;
					
				case Obj.Con:
					report_info("Pristupa se konstanti " + designatorIdent.getNameDesignator(), designatorIdent);
					break;
	
				default:
					break;
			}
		}
		else {
			designatorIdent.obj = Tab.noObj;
			report_error("Pokusaj koriscena nedeklarisane promenljive " + designatorIdent.getNameDesignator(), designatorIdent);
		}
	}
	
	public void visit(DesignatorForExpression exprDesignator) {
		Obj node = exprDesignator.getArryDesignator().getDesignator().obj;
		
		if(isCheckDesignatorOk(node, exprDesignator)) {
			exprDesignator.obj = new Obj(Obj.Elem, node.getName(), node.getType().getElemType());
			report_info("Pristupa se nizu " + node.getName(), exprDesignator);
		}
	}

	private boolean isCheckDesignatorOk(Obj node, DesignatorForExpression designator) {
		String nodeName = node.getName();
		Struct nodeType = node.getType();
		int nodeKind = nodeType.getKind();
		
		if(Struct.Array != nodeKind) {
			report_error("Promenljiva " + nodeName + " nije niz!", designator);
			return false;
		}
		else if(designator.getExpr().struct != Tab.intType) {
			report_error("Izraz u [] nije int!", designator);
			return false;
		}
		
		return true;
	}
	
	/* ************ DesignatorStatement ************ */
	
	public void visit(DesignatorAssignop designatorAssignop) {
		Struct leftDesType = designatorAssignop.getDesignator().obj.getType();
		Struct rightExprType = designatorAssignop.getExpr().struct;
		int kind = designatorAssignop.getDesignator().obj.getKind();
		// check if kind is valid
		if(kind != Obj.Var && kind != Obj.Elem) {
			report_error("Sa leve strane izraza se ne nalazi promenljiva ili element niza!", designatorAssignop);
			return;
		}
		// TODO: Proveri ovu dodelu tj proveru
		if(!leftDesType.assignableTo(rightExprType)) {
			report_error("Dodela se ne moze izvrsiti - tipovi nisu kompatibilni!", designatorAssignop);
			return;
		}
		
		if(designatorAssignop.getDesignator().obj.equals(foreachObj)) {
			report_error("Pokusaj koriscenja foreach za dodelu vrednosti - nije dozvoljeno!", designatorAssignop);
			return;
		}
		
	}
	
	public void visit(DesignatorIncrement desInc) {
		if(!(desInc.getDesignator().obj.getKind() == Obj.Elem || 
				desInc.getDesignator().obj.getKind() == Obj.Var)) {
			report_error("Objekat za inkrementiranje mora biti promenljiva ili element niza!", desInc);
			return;
		}
		
		if(desInc.getDesignator().obj.getType() != Tab.intType) {
			report_error("Inkrementiranje se moze vrsiti samo nad INT tipom!", desInc);
			return;
		}
		
	}

	public void visit(DesignatorDecrement desDec) {
		
		if(!(desDec.getDesignator().obj.getKind() == Obj.Elem || 
				desDec.getDesignator().obj.getKind() == Obj.Var)) {
			report_error("Objekat za dekrementiranje mora biti promenljiva ili element niza!", desDec);
			return;
		}
		
		if(desDec.getDesignator().obj.getType() != Tab.intType) {
			report_error("Dekrementiranje se moze vrsiti samo nad INT tipom!", desDec);
		}
	}
	
	public void visit(DesignatorActPars designatorActPars) {
		if(designatorActPars.getDesignator().obj.getKind() == Obj.Meth) {
			if(globalMethods.contains(designatorActPars.getDesignator().obj)) {
				report_info("Pozvana funkcija " + designatorActPars.getDesignator().obj.getName(), designatorActPars);
			}
			else {
				report_error("Poziv metode nije korektan - metoda se ne nalazi u listi!", designatorActPars);
				return;
			}
		}
		else {
			report_error("Poziv metode nije korektan, nije metoda!", designatorActPars);
		}
	}
	
	// For list of assignments - using listToAssign
	public void visit(DesignatorsList designatorsList) {
		int kind = designatorsList.getDesignator().obj.getKind();
		if(kind == Struct.Array) {
			for (Obj node : listToAssign) {
				if(!designatorsList.getDesignator().obj.getType().assignableTo(node.getType())) {
					report_error("Dodela vrednosti u nizu nije korektna!", designatorsList);
				}
			}
		}
		else {
			report_error("Promenljiva sa desne strane nije niz!", designatorsList);
		}
	}
	
	// TODO: Sta raditi sa OPTIONAL_DESIGNATOR???
	
	/* ************ Factors ************ */
	
	public void visit(FactorNumConst factorNum) {
		factorNum.struct = Tab.intType;
	}
	
	public void visit(FactorCharConst factorChar) {
		factorChar.struct = Tab.charType;
	}
	
	public void visit(FactorBoolConst factorBool) {
		factorBool.struct = new Struct(Struct.Bool);
	}
	
	public void visit(FactorNoParens factorDesignator) {
		factorDesignator.struct = factorDesignator.getDesignator().obj.getType();
	}
	
	public void visit(FactorWithActPars factorWithActPars) {
		// TODO: Proveri da li treba stavljati nesto u STRUCT
		if(factorWithActPars.getDesignator().obj.getKind() != Obj.Meth) {
			report_error("Poziv nije korektan! Nije metoda!", factorWithActPars);
			return;
		}
		else {
			report_info("Poziv metode " + factorWithActPars.getDesignator().obj.getName(), factorWithActPars);
		}
	}
	
	public void visit(FactorExpr factorExpr) {
		factorExpr.struct = factorExpr.getExpr().struct;
	}
	
	public void visit(FactorNewActPars factorNewActPars) {
		// TODO: Da li tebi FactorNewActPars tj poziv NEW metode za klasu treba uopste?
		int factorKind = factorNewActPars.getType().struct.getKind();
	}
	
	public void visit(FactorNewExpr factorNewExpr) {
		// for creating Arrays
		Struct arrSizeExpr = factorNewExpr.getExpr().struct;
		if(arrSizeExpr == Tab.intType) {
			Struct tmp = factorNewExpr.getType().struct;
			factorNewExpr.struct = new Struct(Struct.Array, tmp);
		}
		else {
			factorNewExpr.struct = Tab.noType;
			report_error("Niz se ne moze inicirati ako vrednost izraza nije int!", factorNewExpr);
		}
	}
	
	/* ************ ActPars ************ */
	
	/* ************ Expr ************ */
	
	boolean minusExpr = false;
	
	public void visit(ExprTermSingle exprTerm) {
		exprTerm.struct = exprTerm.getTerm().struct;
	}
	
	public void visit(ExprWith negExpr) {
		minusExpr = true;
		// TODO: Dodaj proveru da li je int ili ne i uradi samo ovo iznad
		
		negExpr.struct = negExpr.getExprTermList().struct;		
	}
	
	public void visit(ExprTermMultiple addExpr) {
		// TODO: Provera oba param??
		if(addExpr.getTerm().struct != Tab.intType) {
			report_error("Nije moguce sabirati necelobrojne vrednosti!", addExpr);
			addExpr.struct = Tab.noType;
		}
		else {
			addExpr.struct = addExpr.getTerm().struct;
		}
	}
	
	public void visit(ExptrWithout exprWithout) {
		exprWithout.struct = exprWithout.getExprTermList().struct;
	}
	
	/* ************ Operators ************ */
	
	public void visit(Assignop assignop) {
		currOperator = Ops.EQ;
	}
	
	public void visit(EqOp EqOp) {
		currOperator = Ops.EQ_CMP;
	}
	
	public void visit(NeqOp NeqOp) {
		currOperator = Ops.NEQ_CMP;
	}
	
	public void visit(GreaterOp GreaterOp) {
		currOperator = Ops.GREATER;
	}
	
	public void visit(GreaterEqOp GreaterEqOp) {
		currOperator = Ops.GREATER_EQ;
	}
	
	public void visit(LessOp LessOp) {
		currOperator = Ops.LESS;
	}
	
	public void visit(LessEqOp LessEqOp) {
		currOperator = Ops.LESS_EQ;
	}
	
	public void visit(PlusOp PlusOp) {
		currOperator = Ops.PLUS;
	}
	
	public void visit(MinusOp MinusOp) {
		currOperator = Ops.MINUS;
	}
	
	public void visit(MultiplyOp MultiplyOp) {
		currOperator = Ops.MUL;
	}
	
	public void visit(DivOp DivOp) {
		currOperator = Ops.DIV;
	}
	
	public void visit(ModOp ModOp) {
		currOperator = Ops.MOD;
	}
	
	/* ************ CondFact ************ */
	
	/* ************ Term ************ */
	
	public void visit(TermSingleFactors termSingleFactors) {
		termSingleFactors.struct = termSingleFactors.getFactor().struct;
	}
	
	public void visit(Term term) {
		term.struct = term.getFactorList().struct;
	}
	
	public void visit(TermMultipleFactors term) {
		if(term.getFactorList().struct == Tab.intType && term.getFactor().struct == Tab.intType) {
			term.struct = Tab.intType;
		}
		else {
			report_error("Neki cinioci nisu INT!", term);
			term.struct = Tab.noType;
		}
	}
}






















