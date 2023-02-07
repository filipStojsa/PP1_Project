package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }

// Program start
"program"   { return new_symbol(sym.PROG, yytext());}

// Functions 
"return" 	{ return new_symbol(sym.RETURN, yytext()); }
"void" 		{ return new_symbol(sym.VOID, yytext()); }

// If statements
"if"		{ return new_symbol(sym.IF, yytext()); }
"else" 		{ return new_symbol(sym.ELSE, yytext()); }

// Loop statements
"while"		{ return new_symbol(sym.WHILE, yytext()); }
"foreach" 	{ return new_symbol(sym.FOREACH, yytext()); }
"do"		{ return new_symbol(sym.DO, yytext()); }
"continue" 	{ return new_symbol(sym.CONTINUE, yytext()); }
"break"		{ return new_symbol(sym.BREAK, yytext()); }

// Vars terminals
"const" 	{ return new_symbol(sym.CONST, yytext()); }
"new"		{ return new_symbol(sym.NEW, yytext()); }

// Class terminals
"class"		{ return new_symbol(sym.CLASS, yytext()); }
"enum"		{ return new_symbol(sym.ENUM, yytext()); }
"extends"	{ return new_symbol(sym.EXTENDS, yytext()); }

// Print and read predefined
"print" 	{ return new_symbol(sym.PRINT, yytext()); }
"read" 		{ return new_symbol(sym.READ, yytext()); }
"skip" 		{ return new_symbol(sym.SKIP, yytext()); }

// Operator terminals
"+" 		{ return new_symbol(sym.PLUS, yytext()); }
"-" 		{ return new_symbol(sym.MINUS, yytext()); }
"=" 		{ return new_symbol(sym.EQ, yytext()); }
"*" 		{ return new_symbol(sym.MUL, yytext()); }
"/" 		{ return new_symbol(sym.DIV, yytext()); }
"%" 		{ return new_symbol(sym.MOD, yytext()); }

"==" 		{ return new_symbol(sym.EQ_CMP, yytext()); }
"!=" 		{ return new_symbol(sym.NEQ_CMP, yytext()); }
">" 		{ return new_symbol(sym.GREATER, yytext()); }
">=" 		{ return new_symbol(sym.GREATER_EQ, yytext()); }
"<" 		{ return new_symbol(sym.LESS, yytext()); }
"<=" 		{ return new_symbol(sym.LESS_EQ, yytext()); }

"&&" 		{ return new_symbol(sym.AND, yytext()); }
"||" 		{ return new_symbol(sym.OR, yytext()); }

"++" 		{ return new_symbol(sym.INCREMENT, yytext()); }
"--" 		{ return new_symbol(sym.DECREMENT, yytext()); }
":" 		{ return new_symbol(sym.COL, yytext()); }
";" 		{ return new_symbol(sym.SEMICOL, yytext()); }
"," 		{ return new_symbol(sym.COMMA, yytext()); }
"." 		{ return new_symbol(sym.DOT, yytext()); }

"(" 		{ return new_symbol(sym.LPAREN, yytext()); }
")" 		{ return new_symbol(sym.RPAREN, yytext()); }
"{" 		{ return new_symbol(sym.LBRACE, yytext()); }
"}"			{ return new_symbol(sym.RBRACE, yytext()); }
"[" 		{ return new_symbol(sym.LBRACKET, yytext()); }
"]"			{ return new_symbol(sym.RBRACKET, yytext()); }

"=>"		{ return new_symbol(sym.ARROW, yytext()); }

// Comment section
"//" {yybegin(COMMENT);}
<COMMENT> . {yybegin(COMMENT);}
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

// Numbers
[0-9]+  { return new_symbol(sym.NUMBER, new Integer (yytext())); }

// Boolean
("true"|"false")	{ return new_symbol(sym.BOOLEAN, new Boolean(yytext())); }

// Chars
'([a-z]|[A-Z])'		{ return new_symbol(sym.CHAR, new Character(yytext().charAt(1))); }

// Identifier
([a-z]|[A-Z])[a-zA-Z0-9_]* 	{return new_symbol (sym.IDENT, yytext()); }

. { System.err.println("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)); }










