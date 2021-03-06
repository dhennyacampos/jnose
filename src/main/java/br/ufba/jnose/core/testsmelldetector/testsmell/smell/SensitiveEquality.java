package br.ufba.jnose.core.testsmelldetector.testsmell.smell;

import br.ufba.jnose.core.testsmelldetector.testsmell.MethodUsage;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import br.ufba.jnose.core.testsmelldetector.testsmell.AbstractSmell;
import br.ufba.jnose.core.testsmelldetector.testsmell.TestMethod;
import br.ufba.jnose.core.testsmelldetector.testsmell.Util;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class SensitiveEquality extends AbstractSmell {
	ArrayList<MethodUsage> methodSensitiveEquality = null;
	
    public SensitiveEquality() {
        super("Sensitive Equality");
        methodSensitiveEquality = new ArrayList<MethodUsage>();
    }

    /**
     * Analyze the test file for test methods the 'Sensitive Equality' smell
     */
    @Override
    public void runAnalysis(CompilationUnit testFileCompilationUnit, CompilationUnit productionFileCompilationUnit, String testFileName, String productionFileName) throws FileNotFoundException {
        classVisitor = new SensitiveEquality.ClassVisitor();
        classVisitor.visit(testFileCompilationUnit, null);
        
        for (MethodUsage method : methodSensitiveEquality) {
            TestMethod testClass = new TestMethod(method.getTestMethodName());
            testClass.addDataItem("begin", method.getBegin());
            testClass.addDataItem("end", method.getEnd());
            testClass.setHasSmell(true);
            smellyElementList.add(testClass);
        }
    }

    private class ClassVisitor extends VoidVisitorAdapter<Void> {
        private MethodDeclaration currentMethod = null;
        private int sensitiveCount = 0;
        TestMethod testMethod;

        // examine all methods in the test class
        @Override
        public void visit(MethodDeclaration n, Void arg) {
            if (Util.isValidTestMethod(n)) {
                currentMethod = n;
                testMethod = new TestMethod(n.getNameAsString());
                testMethod.setHasSmell(false); //default value is false (i.e. no smell)
                
                super.visit(n, arg);

                //reset values for next method
                currentMethod = null;
                sensitiveCount = 0;
            }
        }

        // examine the methods being called within the test method
        @Override
        public void visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);
            if (currentMethod != null) {
                // if the name of a method being called start with 'assert'
                if (n.getNameAsString().startsWith(("assert"))) {
                    // assert methods that contain toString
                    for (Expression argument : n.getArguments()) {
                        if (argument.toString().contains("toString")) {
                            sensitiveCount++;
                            methodSensitiveEquality.add(new MethodUsage(currentMethod.getNameAsString(), "", String.valueOf(n.getRange().get().begin.line), String.valueOf(n.getRange().get().begin.line)));
                        }
                    }
                }
                // if the name of a method being called is 'fail'
                else if (n.getNameAsString().equals("fail")) {
                    // fail methods that contain toString
                    for (Expression argument : n.getArguments()) {
                        if (argument.toString().contains("toString")) {
                            sensitiveCount++;
                            methodSensitiveEquality.add(new MethodUsage(currentMethod.getNameAsString(), "", String.valueOf(n.getRange().get().begin.line), String.valueOf(n.getRange().get().begin.line)));
                        }
                    }
                }
            }
        }
    }
}
