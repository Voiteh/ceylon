/*
 * Copyright Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the authors tag. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License version 2.
 * 
 * This particular file is subject to the "Classpath" exception as provided in the 
 * LICENSE file that accompanied this code.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.eclipse.ceylon.compiler.java.codegen;

import org.eclipse.ceylon.compiler.java.codegen.recovery.Errors;
import org.eclipse.ceylon.compiler.typechecker.tree.Node;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree;
import org.eclipse.ceylon.compiler.typechecker.tree.Visitor;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.AnyMethod;
import org.eclipse.ceylon.model.typechecker.model.Class;
import org.eclipse.ceylon.model.typechecker.model.Declaration;
import org.eclipse.ceylon.model.typechecker.model.Function;

/**
 * Creates an {@link AnnotationInvocation} for each annotation 
 * constructor function.
 * It is necessary to do this before the {@link AnnotationModelVisitor}
 * runs because there could be mutual recursion between annotation constructors.
 * We will detect the mutual recursion, but need the AnnotationInvocations 
 * set up first. 
 */
public class AnnotationDeclarationVisitor extends Visitor {

    /** The annotation constructor we are currently visiting */
    private AnyMethod annotationConstructor;
    /** The instantiation in the body of the constructor, or in its default parameters */
    private AnnotationInvocation instantiation;
//    private boolean checkingArguments;
//    private boolean checkingInvocationPrimary;
    private final Errors errors;
    
    public AnnotationDeclarationVisitor(CeylonTransformer gen) {
        this.errors = gen.errors();
    }
    
    @Override
    public void handleException(Exception e, Node node) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException)e;
        } else {
            throw new RuntimeException(e);
        }
    }
    
    public static boolean isAnnotationConstructor(AnyMethod def) {
        return isAnnotationConstructor(def.getDeclarationModel());
    }
    
    public static boolean isAnnotationConstructor(Declaration def) {
        return def.isToplevel()
                && def instanceof Function
                && def.isAnnotation();
    }

    public static boolean isAnnotationClass(Tree.ClassOrInterface def) {
        return isAnnotationClass(def.getDeclarationModel());
    }

    public static boolean isAnnotationClass(Declaration declarationModel) {
        return (declarationModel instanceof Class)
                && declarationModel.isAnnotation();
    }
    
    @Override
    public void visit(Tree.MethodDefinition d) {
        if (errors.hasAnyError(d)) {
            return;
        }
        if (isAnnotationConstructor(d)) {
            annotationConstructor = d;
            instantiation = new AnnotationInvocation();
            instantiation.setConstructorDeclaration(d.getDeclarationModel());
            d.getDeclarationModel().setAnnotationConstructor(instantiation);
        }
        super.visit(d);
        if (isAnnotationConstructor(d)) {
            instantiation = null;
            annotationConstructor = null;
        }
    }
    
    @Override
    public void visit(Tree.MethodDeclaration d) {
        if (errors.hasAnyError(d)) {
            return;
        }
        if (isAnnotationConstructor(d)
                && d.getSpecifierExpression() != null) {
            annotationConstructor = d;
            instantiation = new AnnotationInvocation();
            instantiation.setConstructorDeclaration(d.getDeclarationModel());
            d.getDeclarationModel().setAnnotationConstructor(instantiation);
        }
        super.visit(d);
        if (isAnnotationConstructor(d)) {
            instantiation = null;
            annotationConstructor = null;
        }
    }
    
    @Override
    public void visit(Tree.Parameter p) {
        if (annotationConstructor != null) {
            AnnotationConstructorParameter acp = new AnnotationConstructorParameter();
            acp.setParameter(p.getParameterModel());
            instantiation.addConstructorParameter(acp);
        }
    }
    
}

