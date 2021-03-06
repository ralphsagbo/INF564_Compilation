package mini_c;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class Typing implements Pvisitor {

	// le résultat du typage sera mis dans cette variable
	private File file;
	
	private int nb_args; 
	private int cursor = 0;
	
	final private static int IDENT_DECL= 0, IDENT_SRETURN = 2, IDENT_SIF = 3, IDENT_PEVAL = 4, IDENT_SWHILE = 5;
	private int ident_block = 0;
	 
	final private static int IDENT_EUNOP = 5, IDENT_EBINOP = 6, IDENT_ECALL = 7,  IDENT_PASSIGN=10; 
	
	final private static int STRUCT_NAME=1, STRUCT_VARNAME=2, STRUCT_FIELD=3;
	
	private String NAME_FIELD;
	private Typ TYP_FIELD;
	
	private int nb_pblocs=0;
	private int FIN_FCT=0;
	
	public Stack<Integer> stack_expr = new Stack<>(); 
	public Stack<Integer> stack_block = new Stack<>(); 
	public Stack<Expr> stack_addr_expr = new Stack<>(); 
	public Stack<Sblock> stack_addr_block = new Stack<>(); 
	public Stack<Stmt> stack_addr_stmt = new Stack<>();
	
	public Stack<Integer> stack_struct = new Stack<>();
	
	public String name_struct;
	
	public int nb_main = 0;
	
	
	
	public String type_field;
	
	//public LinkedList<Structure> structure;
	public HashMap<String, Structure> structure;
	// et renvoyé par cette fonction
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}
	
	public void nop() {
		System.out.println("NOPEEEEEEEEEEEEEEEEEEEEEE " + stack_addr_expr.pop());
	}
	
	public void op() {
		System.out.println("NOPEEEEEEEEEEEEEEEEEEEEEE " + stack_addr_expr.peek());
	}


	// il faut compléter le visiteur ci-dessous pour réaliser le typage

	@Override
	public void visit(Pfile n) {
		// TODO Auto-generated method stub
		file = new File(new LinkedList<Decl_fun>());
		stack_struct.add(0);
		structure = new HashMap<>();
		for (Pdecl tmp: n.l) {
			if(tmp instanceof Pstruct) {
				this.visit((Pstruct) tmp);
			} 
			else if(tmp instanceof Pfun) {
				this.visit((Pfun) tmp);
			}
			else {
				throw new Error("Visit Pfile"); 
			}
		}
		if(nb_main==0)
			throw new Error("main function not found");
	}

	@Override
		public void visit(Pident n) {
			// TODO Auto-generated method stub
			System.out.println("Pident Deb");
	
			if(stack_struct.peek()==0) {
				
				if(cursor==0 && stack_struct.peek()==0) {
					for(Decl_fun funs : file.funs) {
						if(funs.fun_name != null && funs.fun_name.equals(n.id))
							throw new Error("redefinition of function at location "+n.loc);
					}
					file.funs.getLast().fun_name = n.id;
					if(n.id.equals("main")) nb_main++;
					if(nb_main>1)
						throw new Error(nb_main + " functions named main");
				}
				else if(cursor <=nb_args && stack_struct.peek()==0) {
					for(Decl_var var : file.funs.getLast().fun_formals) {
						if(var.name != null && var.name.equals(n.id))
							throw new Error("redefinition of variable at location "+n.loc);
					}
					file.funs.getLast().fun_formals.getLast().name = n.id;
				}
				else {
					
					Sblock b = stack_addr_block.pop(); 
					stack_addr_block.push(b); 
					
					ident_block = stack_block.peek();  
					
					switch(ident_block) {
						case IDENT_DECL:
							for(Decl_var var : b.dl) {
								if(var.name != null && var.name.equals(n.id)) {
									throw new Error("redefinition of variable at "+n.loc);
								}
							}
							b.dl.getLast().name = n.id;
							TYP_FIELD = b.dl.getLast().t;
							break; 
						default:
							int present = 0;
							for(int i=stack_addr_block.size()-1; i>=0; i--) {
								Sblock blc = stack_addr_block.get(i);
								if(blc==null)
									break;
								for(Decl_var var : blc.dl) {
									if(var.name.equals(n.id)) {
										present = 1;
										TYP_FIELD = var.t;
										break;
									}
								}
							}
							
							if(present==0) {
								for(Decl_var var : file.funs.getLast().fun_formals) {
									if(var.name.equals(n.id)) {
										present = 1;
										TYP_FIELD = var.t;
										break;
									}
								}
							}
							if(present==0)
								throw new Error("unknown variable at " + n.loc);
							stack_addr_expr.add(new Eaccess_local(n.id));
					}
				}
			}
			else if (stack_struct.peek()==STRUCT_NAME) {
				structure.put(n.id,new Structure(n.id));
				name_struct = n.id; 
			}
			else if (stack_struct.peek()==STRUCT_FIELD) {
				if (structure.get(name_struct).fields.containsKey(n.id))
					throw new Error("redefinition of variable at location " + n.loc);
				if(type_field.equals("0"))
					structure.get(name_struct).fields.put(n.id, new Field(n.id, new Tint()));
				else {
					structure.get(name_struct).fields.put(n.id, new Field(n.id, new Tstructp(structure.get(type_field))));
				}
	
			}
			else if(stack_struct.peek()==STRUCT_VARNAME) {
				Sblock b = stack_addr_block.pop(); 
	
				b.dl.getLast().name = n.id;
				stack_addr_block.push(b);
			}
			System.out.println("Pident Fin"); 
	
		}

	@Override
	public void visit(PTint n) {
		// TODO Auto-generated method stub
		
		System.out.println("Ptint deb "); 
		if(stack_struct.peek()==0) {
			if(cursor==0)
				file.funs.add(new Decl_fun(new Tint(), null, null, null));
			else if(cursor <=nb_args) {
				file.funs.getLast().fun_formals.add(new Decl_var(new Tint(), null)); 
			}
			else {
				Sblock b = stack_addr_block.pop();  
				b.dl.add(new Decl_var(new Tint(), null)); 
				stack_addr_block.push(b); 
			}
		}
		else if(stack_struct.peek()==STRUCT_FIELD) {
			type_field = "0";
		}
		System.out.println("Ptint Fin"); 
	}

	@Override
	public void visit(PTstruct n) {
		// TODO Auto-generated method stub
		System.out.println("Deb PTstruct");
		Structure s; 
		if(stack_struct.peek()==0) {
			if(cursor==0) {
				s = structure.get(n.id);
				if(s == null)
					throw new Error("No such struct "+n.loc); 
				else
					file.funs.add(new Decl_fun(new Tstructp(s), null, null, null));
			}	
			else if(cursor <=nb_args) {
				s = structure.get(n.id);
				if(s == null)
					throw new Error("No such struct "+n.loc); 
				else
					file.funs.getLast().fun_formals.add(new Decl_var(new Tstructp(s), null)); 
			}
			else {
				Sblock b = stack_addr_block.pop(); 
				b.dl.add(new Decl_var(new Tstructp( structure.get(n.id)), null)); 
				stack_addr_block.push(b); 
			}
		}
		else if(stack_struct.peek()==STRUCT_FIELD) {
			if(structure.get(n.id)==null)
				throw new Error("structure not defined at location " + n.loc);
			type_field = n.id;
		}
		System.out.println("Fin PTstruct");
	}

	@Override
	public void visit(Pint n) {
		// TODO Auto-generated method stub
		System.out.println("Deb Pint");
		stack_addr_expr.push(new Econst(n.n));
		TYP_FIELD = new Tint();
		System.out.println("Fin Pint"); 	
	}

	@Override
	public void visit(Punop n) {
		// TODO Auto-generated method stub
 
		System.out.println("Eunop Deb"); 
		Sblock temp = stack_addr_block.peek();
	
		if(temp != null) {
			if(n.e1 instanceof Pident) {
				for(Decl_var var : temp.dl) {
					if(var.t instanceof Tstructp && var.name.equals(((Pident)n.e1).id) && n.op.toString().equals("Uneg"))
						throw new Error("incompatible type operation at location " + n.loc);
				}
			}
		}
		else		
			throw new Error("incompatible type");
		
		
		stack_expr.push(IDENT_EUNOP);
		ident_block = stack_block.peek();
		
		Expr e = new Eunop(n.op, null); 
		stack_addr_expr.push(e);
		this.visit(n.e1);
		((Eunop)e).e = stack_addr_expr.pop();
		
		stack_expr.pop(); 
		System.out.println("Eunop Fin");
	
	}

	@Override
	public void visit(Passign n) {
		// TODO Auto-generated method stub
		System.out.println("Passign Deb"); 
		stack_expr.push(IDENT_PASSIGN); 
		Expr e;
		Typ t1=null, t2=null;
		if(n.e1 instanceof Parrow) {
			e = new Eassign_field(null, null, null);
			
			this.visit(n.e1);
			
			if(TYP_FIELD instanceof Tint)
				t1 = new Tint();
			else if(TYP_FIELD instanceof Tstructp)
				t1 = new Tstructp(structure.get(((Tstructp)TYP_FIELD).s.str_name));
			
			((Eassign_field)e).e1 = stack_addr_expr.pop();
			
			
			if (TYP_FIELD instanceof Tint) {
				((Eassign_field)e).f = new Field(NAME_FIELD, new Tint());
			}
			else if(TYP_FIELD instanceof Tstructp) {
				((Eassign_field)e).f = new Field(NAME_FIELD, new Tstructp(structure.get(((Tstructp)TYP_FIELD).s.str_name)));
			}
			
			
			this.visit(n.e2);
			if(TYP_FIELD instanceof Tint)
				t2 = new Tint();
			else if(TYP_FIELD instanceof Tstructp)
				t2 = new Tstructp(structure.get(((Tstructp)TYP_FIELD).s.str_name));
			
			
			if(t1 instanceof Tint && t2 instanceof Tstructp)
				throw new Error("incompatible type operation at location " + n.loc);
			if(t1 instanceof Tstructp && t2 instanceof Tstructp) {
				if(((Tstructp)t1).s.str_name != ((Tstructp)t2).s.str_name)
					throw new Error("incompatible type operation at location " + n.loc);
			}
			if(t2 instanceof Tint && t1 instanceof Tstructp) {
				if(!(((Eassign_field)e).e1 instanceof Econst && ((Econst)((Eassign_field)e).e1).i==0))
					throw new Error("incompatible type operation at location " + n.loc);
			}
			
			((Eassign_field)e).e2 = stack_addr_expr.pop();
			stack_addr_expr.push(e);
		}
		else {
			
			this.visit(((Pident)n.e1));
			if(TYP_FIELD instanceof Tint)
				t1 = new Tint();
			else if(TYP_FIELD instanceof Tstructp)
				t1 = new Tstructp(structure.get(((Tstructp)TYP_FIELD).s.str_name));
			
			stack_addr_expr.pop();
			e = new Eassign_local(((Pident)n.e1).id, null);
			this.visit(n.e2);
			if(TYP_FIELD instanceof Tint)
				t2 = new Tint();
			else if(TYP_FIELD instanceof Tstructp)
				t2 = new Tstructp(structure.get(((Tstructp)TYP_FIELD).s.str_name));
			
			
			((Eassign_local)e).e = stack_addr_expr.pop();
			
			if(t1 instanceof Tint && t2 instanceof Tstructp)
				throw new Error("incompatible type operation at location " + n.loc);
			if(t1 instanceof Tstructp && t2 instanceof Tstructp) {
				if(((Tstructp)t1).s.str_name != ((Tstructp)t2).s.str_name)
					throw new Error("incompatible type operation at location " + n.loc);
			}
			if(t2 instanceof Tint && t1 instanceof Tstructp) {
				if(!(((Eassign_local)e).e instanceof Econst && ((Econst)((Eassign_local)e).e).i==0))
					throw new Error("iincompatible type operation at location " + n.loc);
			}
			
			stack_addr_expr.push(e);
		}
		stack_expr.pop();
		System.out.println("Passign Fin"); 
	}

	@Override
	public void visit(Pbinop n) {
		// TODO Auto-generated method stub
		System.out.println("Binop Deb");
		
		Expr e = new Ebinop(n.op, null, null);
		Typ t1=null, t2=null;
		
		stack_expr.push(IDENT_EBINOP);
		ident_block = stack_block.peek();
				
		
		this.visit(n.e1);
		if(TYP_FIELD instanceof Tint)
			t1 = new Tint();
		else if(TYP_FIELD instanceof Tstructp)
			t1 = new Tstructp(structure.get(((Tstructp)TYP_FIELD).s.str_name));
		((Ebinop)e).e1 = stack_addr_expr.pop(); 
		
		
		this.visit(n.e2);
		if(TYP_FIELD instanceof Tint)
			t2 = new Tint();
		else if(TYP_FIELD instanceof Tstructp)
			t2 = new Tstructp(structure.get(((Tstructp)TYP_FIELD).s.str_name));
		((Ebinop)e).e2 = stack_addr_expr.pop();
		
		
		if(t1 instanceof Tint && t2 instanceof Tstructp)
			throw new Error("incompatible type operation at location " + n.loc);
		if(t1 instanceof Tstructp && t2 instanceof Tstructp) {
			if(((Tstructp)t1).s.str_name != ((Tstructp)t2).s.str_name)
				throw new Error("incompatible type operation at location " + n.loc);
			if(n.op.toString().equals("Beq") == false && n.op.toString().equals("Bneq") == false) {
				throw new Error("incompatible type operation at location " + n.loc);
			}
		}
		
		
		stack_addr_expr.push(e);	
		stack_expr.pop(); 
		System.out.println("Binop Fin"); 
	}

	@Override
	public void visit(Parrow n) {
		System.out.println("Parrow Deb");
		Expr e = new Eaccess_field(null, new Field(n.f, null));
		
		this.visit(n.e);
		
		
		int present = 0;
		Expr f = stack_addr_expr.peek();
		
		if(f instanceof Eaccess_local) {
			
			for(int i=stack_addr_block.size()-1; i>=0; i--) {
				Sblock blc = stack_addr_block.get(i);
				if(blc==null)
					break;
				for(Decl_var var : blc.dl) {
					if(var.name.equals(((Eaccess_local)f).i) && var.t instanceof Tstructp) {
						present = 1;
						if(structure.get(((Tstructp)var.t).s.str_name).fields.get(n.f)==null)
							throw new Error("this structure don't have a such field location: " + n.loc);
						TYP_FIELD = structure.get(((Tstructp)var.t).s.str_name).fields.get(n.f).field_typ;
						break;
					}
				}
			}
			if(present==0) {
				for(Decl_var var : file.funs.getLast().fun_formals) {
					if(var.name.equals(((Eaccess_local)f).i) && var.t instanceof Tstructp) {
						present = 1;
						if(structure.get(((Tstructp)var.t).s.str_name).fields.get(n.f)==null)
							throw new Error("this structure don't have a such field location: " + n.loc);
						TYP_FIELD = structure.get(((Tstructp)var.t).s.str_name).fields.get(n.f).field_typ;
						break;
					}
				}
			}
			if(present==0)
				throw new Error("Not a structure at location " + n.loc);
		}
		else if(f instanceof Eaccess_field) {
			
			TYP_FIELD = structure.get(((Tstructp)TYP_FIELD).s.str_name).fields.get(n.f).field_typ;
		}
		else
			throw new Error("Not a structure at location " + n.loc);
		((Eaccess_field)e).e = stack_addr_expr.pop();
		((Eaccess_field)e).f.field_typ = TYP_FIELD;
		stack_addr_expr.push(e);
				
		System.out.println("Parrow Fin");
	}

	@Override
	public void visit(Pcall n) {
		// TODO Auto-generated method stub
		System.out.println("Pcall Deb");

		int cmp=0;
		for(Decl_fun funs: file.funs) {
			if(funs.fun_name.equals(n.f)) break;
			cmp++;
		}
		if(cmp==file.funs.size() && n.f.equals("putchar")==false && n.f.equals("sbrk")==false)
			throw new Error("function " + n.f + " not found");
		int taille = (cmp==file.funs.size()) ? 1 : n.l.size();
		if(cmp < file.funs.size() && file.funs.get(cmp).fun_formals.size() != taille)
			throw new Error("bad arguments number");
		else if(cmp == file.funs.size() && n.l.size()!=1)
			throw new Error("bad arguments number");
		else {
			Expr e;
			stack_block.push(IDENT_PEVAL);
			stack_expr.push(IDENT_ECALL);
			e = new Ecall(n.f, new LinkedList<>());
			stack_addr_expr.push(e);
			if(n.f.equals("sbrk")) {
				this.visit(n.l.get(0));
				if (!(stack_addr_expr.peek() instanceof Esizeof)) {
					throw new Error("bad argument type");
				}
				
				Expr f = stack_addr_expr.pop();
				e = stack_addr_expr.pop();
				((Ecall)e).el.add(f);
				stack_addr_expr.push(e);
			}
			else if(n.f.equals("putchar")) {
				this.visit(n.l.get(0));
				if (!(TYP_FIELD instanceof Tint)) {
					
					throw new Error("bad argument type at location "+ n.loc);
				}
				
				Expr f = stack_addr_expr.pop();
				e = stack_addr_expr.pop();
				((Ecall)e).el.add(f);
				stack_addr_expr.push(e);
			}
			else {
				for(int i=0; i<taille; i++) {
					Pexpr vars = n.l.get(i);
					this.visit(vars);
					
					if(file.funs.get(cmp).fun_formals.get(i).t instanceof Tint && TYP_FIELD instanceof Tstructp)
						throw new Error("bad argument type at location "+ n.loc);
					if(file.funs.get(cmp).fun_formals.get(i).t instanceof Tstructp && TYP_FIELD instanceof Tint) {
						Expr temp = stack_addr_expr.peek();
						if(!(temp instanceof Econst && ((Econst)temp).i==0)) {
							throw new Error("bad argument type at location "+ n.loc);
						}						
					}
					if(file.funs.get(cmp).fun_formals.get(i).t instanceof Tstructp && TYP_FIELD instanceof Tstructp)
					{
						Typ t2 = file.funs.get(cmp).fun_formals.get(i).t;
						if(((Tstructp)t2).s.str_name != ((Tstructp)TYP_FIELD).s.str_name)
							throw new Error("bad argument type at location "+ n.loc);
					}
					
					Expr f = stack_addr_expr.pop();
					e = stack_addr_expr.pop();
					((Ecall)e).el.add(f);
					stack_addr_expr.push(e);	
				}
				TYP_FIELD = file.funs.get(cmp).fun_typ;
			}
			stack_block.pop();
			stack_expr.pop();
			
			System.out.println("Pcall Fin");
		}
	}

	@Override
	public void visit(Psizeof n) {
		// TODO Auto-generated method stub
		System.out.println("Psizeof Deb");
		if(structure.get(n.id)==null)
			throw new Error("Psizeof: structure not found at location " + n.loc);
		Expr e = new Esizeof(structure.get(n.id));
		stack_addr_expr.push(e);
		System.out.println("Psizeof Fin");
	}

	@Override
	public void visit(Pskip n) {
		// TODO Auto-generated method stub
		System.out.println("Pskip Deb");
		stack_addr_stmt.push(new Sskip());
		System.out.println("Pskip Fin");
	}

	@Override
	public void visit(Peval n) {
		// TODO Auto-generated method stub
		System.out.println("Peval Deb");
		stack_block.push(IDENT_PEVAL);
		
		Pexpr tmp = n.e; 

		Expr e = null; 
		stack_addr_expr.push(e);
		this.visit(tmp);
		
		stack_addr_stmt.add(new Sexpr(stack_addr_expr.pop())) ; 
		
		stack_block.pop();
		System.out.println("Peval Fin");
	}

	@Override
	public void visit(Pif n) {
		// TODO Auto-generated method stub
		System.out.println("Pif Deb");
		 
		stack_block.push(IDENT_SIF);
		
		Pexpr tmp = n.e; 
		int nb_return=0;
		
		Sif b = new Sif(null, null, null); 
		
		 
		stack_addr_expr.push(null); 
		this.visit(tmp);
		
		
		b.e = stack_addr_expr.pop() ; 
		 
		
		if(n.s1 instanceof Pbloc) {
			this.visit(n.s1);
			b.s1 = stack_addr_block.pop();
			if(nb_pblocs==1 && !((Sblock)b.s1).sl.isEmpty() && ((Sblock)b.s1).sl.getLast() instanceof Sreturn) nb_return++;
		}
		else{
			this.visit(n.s1);
			b.s1 = stack_addr_stmt.pop();
			if(nb_pblocs==1 && b.s1 instanceof Sreturn) nb_return++;
		}
		
		if(n.s2 instanceof Pbloc) {
			this.visit(n.s2);
			b.s2 = stack_addr_block.pop();
			if(nb_pblocs==1 && !((Sblock)b.s2).sl.isEmpty() && ((Sblock)b.s2).sl.getLast() instanceof Sreturn) nb_return++;
		}
		else{
			this.visit(n.s2);
			b.s2 = stack_addr_stmt.pop();
			if(nb_pblocs==1 && b.s2 instanceof Sreturn) nb_return++;
		}
		
		
		if (nb_return == 2) 
			FIN_FCT=1;
		
		stack_addr_stmt.push(b);
		stack_block.pop();
		System.out.println("Pif fin"); 
	}

	@Override
	public void visit(Pwhile n) {
		// TODO Auto-generated method stub
		System.out.println("Pwhile Deb");
		stack_block.push(IDENT_SWHILE);
		
		Swhile b = new Swhile(null, null); 
		stack_addr_expr.push(null); 
		this.visit(n.e);
		b.e = stack_addr_expr.pop() ; 
			
		this.visit(n.s1);
		if(n.s1 instanceof Pbloc)			
			b.s = stack_addr_block.pop();
		else
			b.s = stack_addr_stmt.pop();
		
		stack_addr_stmt.push(b);

		stack_block.pop();
		System.out.println("Pwhile Fin");
	}

	@Override
	public void visit(Pbloc n) {
		// TODO Auto-generated method stub
		System.out.println("Pbloc deb");
		nb_pblocs++;
		
		int temp;
		try {
			temp = stack_block.peek();
		}
		catch(EmptyStackException e){
			temp=-1;
		}
		stack_block.push(IDENT_DECL);

		Sblock b = new Sblock(new LinkedList<Decl_var>(), new LinkedList<Stmt>()); 
		stack_addr_block.push(b);
					
		for(Pdeclvar tmp: n.vl) {
			if(tmp.typ instanceof PTint)
				this.visit((PTint)tmp.typ );
			else if(tmp.typ instanceof PTstruct)
				this.visit((PTstruct)tmp.typ );
			else
				throw new Error("visit Pfun Pdeclvar "+tmp.loc); 
			this.visit(new Pident(new Pstring(tmp.id, tmp.loc)));
		} 
		stack_block.pop();
		
		
		int rreturn = 0;
		for(Pstmt tmp: n.sl) {
			if(FIN_FCT==1)
				throw new Error("code not reachable " + tmp.loc);
			if(rreturn==0) {
				if(tmp instanceof Preturn) {
					rreturn = 1;
					if(nb_pblocs==1 && temp != IDENT_SIF && temp != IDENT_SWHILE) FIN_FCT=1;
				}
				this.visit(tmp);
				if(tmp instanceof Pbloc) {
					b.sl.add(stack_addr_block.pop());
				}
				else {
					b.sl.add(stack_addr_stmt.pop());
				}
			}
			
			else {
				throw new Error("code not reachable " + tmp.loc);
			}
		}
		
		nb_pblocs--;
		System.out.println("Pbloc fin");
	}

	@Override
	public void visit(Preturn n) {
		// TODO Auto-generated method stub
		System.out.println("Preturn deb"); 
		stack_block.push(IDENT_SRETURN); 
		
		stack_addr_expr.push(null);
		this.visit(n.e);
		stack_addr_stmt.add(new Sreturn(stack_addr_expr.pop())); 

		stack_block.pop();
		System.out.println("Preturn fin");
	}

	@Override
	public void visit(Pstruct n) {
		// TODO Auto-generated method stub
		System.out.println("Pstruct Deb");
		
		stack_struct.add(STRUCT_NAME);
		this.visit(new Pident(new Pstring(n.s, n.fl.getFirst().loc)));
		stack_struct.pop();
		
		for(Pdeclvar tmp : n.fl) {
			type_field="0";
			stack_struct.add(STRUCT_FIELD);
			
			if (tmp.typ instanceof PTint) {
				this.visit((PTint)tmp.typ);
			} else if (tmp.typ instanceof PTstruct) {
				this.visit((PTstruct)tmp.typ);
			} else {
				throw new Error("visit Pfun Ptype "+tmp.loc);
			}
			this.visit(new Pident(new Pstring(tmp.id, tmp.loc)));
			stack_struct.pop();
		}
		
		System.out.println("Pstruct Fin");
	}

	@Override
	public void visit(Pfun n) {
		// TODO Auto-generated method stub
		FIN_FCT=0;
		nb_pblocs=0;
		Ptype ty = n.ty;
		nb_args = n.pl.size();
		
		if (ty instanceof PTint) {
			this.visit((PTint)ty);
		} else if (ty instanceof PTstruct) {
			this.visit((PTstruct)ty);
		} else {
			throw new Error("visit Pfun Ptype "+n.loc); 
		}
		this.visit(new Pident(new Pstring(n.s, n.loc)));
		
		cursor = 1;
		file.funs.getLast().fun_formals = new LinkedList<>(); 
		for (Pdeclvar tmp : n.pl) {
			if(tmp.typ instanceof PTint) {
				this.visit((PTint)tmp.typ );
				this.visit(new Pident(new Pstring(tmp.id, tmp.loc)));
			}
			else if(tmp.typ instanceof PTstruct) {
				this.visit((PTstruct)tmp.typ );
				this.visit(new Pident(new Pstring(tmp.id, tmp.loc)));
			} 
			else {
				throw new Error("visit Pfun Pdeclvar "+tmp.loc); 
			}
			cursor++; 
		}
		 
		stack_addr_stmt.push(null); 
		this.visit(n.b);
		try {
			file.funs.getLast().fun_body = stack_addr_block.pop();
		}
		catch(EmptyStackException e) {
		}
		cursor = 0;
		if(file.funs.getLast().fun_typ instanceof Tint || file.funs.getLast().fun_typ instanceof Tstructp) {
			if(FIN_FCT==0)
				throw new Error("return of function " + file.funs.getLast().fun_name + " not found at location");
		}
		stack_addr_block.push(null);
	}

	public void visit(Pexpr tmp) {
		// TODO Auto-generated method stub	
		if(tmp instanceof Pint)
			this.visit((Pint)tmp);
		else if (tmp instanceof Pident)
			this.visit((Pident)tmp);
		else if (tmp instanceof Parrow)
			this.visit((Parrow)tmp);
		else if (tmp instanceof Plvalue)
			this.visit((Plvalue)tmp);
		else if (tmp instanceof Passign)
			this.visit((Passign)tmp);
		else if (tmp instanceof Pbinop)
			this.visit((Pbinop)tmp);
		else if (tmp instanceof Punop)
			this.visit((Punop)tmp);
		else if (tmp instanceof Pcall)
			this.visit((Pcall)tmp);
		else if (tmp instanceof Psizeof)
			this.visit((Psizeof)tmp);
		else; 
	}

	public void visit(Pstmt tmp) {
		// TODO Auto-generated method stub	
		if(tmp instanceof Pbloc)
			this.visit((Pbloc)tmp);
		else if(tmp instanceof Pskip)
			this.visit((Pskip)tmp);
		else if(tmp instanceof Preturn)
			this.visit((Preturn)tmp);
		else if(tmp instanceof Pif) {
			this.visit((Pif)tmp); }
		else if(tmp instanceof Peval)
			this.visit((Peval)tmp);
		else if(tmp instanceof Pwhile)
			this.visit((Pwhile)tmp);
		else
			throw new Error("Visit Pstmt "+tmp.loc); 
	} 	
}
