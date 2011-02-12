package com.redhat.ceylon.compiler.analyzer;

import java.util.List;

import com.redhat.ceylon.compiler.model.Class;
import com.redhat.ceylon.compiler.model.Declaration;
import com.redhat.ceylon.compiler.model.GenericType;
import com.redhat.ceylon.compiler.model.Import;
import com.redhat.ceylon.compiler.model.Module;
import com.redhat.ceylon.compiler.model.Package;
import com.redhat.ceylon.compiler.model.Type;
import com.redhat.ceylon.compiler.model.Typed;
import com.redhat.ceylon.compiler.model.Unit;
import com.redhat.ceylon.compiler.tree.Tree;
import com.redhat.ceylon.compiler.tree.Tree.Alias;
import com.redhat.ceylon.compiler.tree.Tree.Identifier;
import com.redhat.ceylon.compiler.tree.Tree.TypeOrSubtype;
import com.redhat.ceylon.compiler.tree.Visitor;
import com.redhat.ceylon.compiler.util.PrintUtil;

/**
 * Second phase of type analysis.
 * Scan the compilation unit looking for literal type 
 * declarations and maps them to the associated model 
 * objects. Also builds up a list of imports for the 
 * compilation unit. Finally, assigns types to the 
 * associated model objects of declarations declared 
 * using an explicit type (this must be done in
 * this phase, since shared declarations may be used
 * out of order in expressions).
 * 
 * @author Gavin King
 *
 */
public class TypeVisitor extends Visitor {
    
    Unit unit;
    
    Type outerType;
    
    Package importPackage;
    
    public TypeVisitor(Unit u) {
        unit = u;
    }
    
    @Override
    public void visit(Tree.ImportPath that) {
        Module m = unit.getPackage().getModule();
        for (Package mp: m.getAllPackages()) {
            if ( hasName(that.getIdentifiers(), mp) ) {
                importPackage = mp;
                return;
            }
        }
        that.getErrors().add( new AnalysisError(that, 
                "Package not found: " + 
                PrintUtil.importNodeToString(that.getIdentifiers()) ) );
    }

    private boolean hasName(List<Identifier> importPath, Package mp) {
        if (mp.getName().size()==importPath.size()) {
            for (int i=0; i<mp.getName().size(); i++) {
                if (!mp.getName().get(i).equals(importPath.get(i).getText())) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }
    
    @Override
    public void visit(Tree.ImportMemberOrType that) {
        Import i = new Import();
        Alias alias = that.getAlias();
        if (alias==null) {
            i.setAlias(that.getIdentifier().getText());
        }
        else {
            i.setAlias(alias.getIdentifier().getText());
        }
        Declaration d = Util.getDeclaration(importPackage, that);
        if (d==null) {
            that.getErrors().add( new AnalysisError(that, 
                    "imported declaration not found: " + 
                    that.getIdentifier().getText() ) );
        }
        else {
            i.setDeclaration(d);
            unit.getImports().add(i);
        }
    }
        
    @Override 
    public void visit(Tree.Type that) {
        Type type = new Type();
        that.setModelNode(type);
        type.setTreeNode(that);
        GenericType d = Util.getDeclaration(that);
        if (d==null) {
            that.getErrors().add( new AnalysisError(that, 
                    "type declaration not found: " + 
                    that.getIdentifier().getText() ) );
        }
        else {
            type.setGenericType(d);
            //TODO: handle type arguments by substitution
            that.setTypeModel(type);
            if (outerType!=null) {
                outerType.getTypeArguments().add(type);
            }
            Type o = outerType;
            outerType = type;
            super.visit(that);
            outerType = o;
        }
    }
    
    @Override 
    public void visit(Tree.VoidModifier that) {
        Type type = new Type();
        that.setModelNode(type);
        type.setTreeNode(that);
        //TODO: use the Void from the language package!
        Class c = new Class();
        c.setName("Void");
        type.setGenericType(c);
        that.setTypeModel(type);
    }
    
    @Override 
    public void visit(Tree.TypedDeclaration that) {
        super.visit(that);
        TypeOrSubtype type = that.getTypeOrSubtype();
        if (!(type instanceof Tree.LocalModifier)) { //if the type declaration is missing, we do type inference later
            Type t = (Type) type.getModelNode();
            ( (Typed) that.getModelNode() ).setType(t);
        }
    }
        
    /**
     * Suppress resolution of types that appear after the
     * member selection operator "."
     */
    @Override
    public void visit(Tree.MemberExpression that) {
        that.getPrimary().visit(this);            
    }
    
}
