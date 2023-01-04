package rs.ac.bg.etf.pp1;

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
	
}






















