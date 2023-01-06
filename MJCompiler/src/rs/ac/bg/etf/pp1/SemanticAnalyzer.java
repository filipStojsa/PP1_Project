package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Obj;
import rs.etf.pp1.symboltable.concepts.Struct;

public class SemanticAnalyzer extends VisitorAdaptor {
	
	public SemanticAnalyzer() {
		// Redefine constructor to add boolean object
		Tab.currentScope().addToLocals(new Obj(Obj.Type, "boolean", new Struct(Struct.Bool)));
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
	
	List<Obj> globalMethods = new ArrayList<>();
	
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
}






















