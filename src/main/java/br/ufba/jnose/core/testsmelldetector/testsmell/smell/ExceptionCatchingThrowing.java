package br.ufba.jnose.core.testsmelldetector.testsmell.smell;

import br.ufba.jnose.core.testsmelldetector.testsmell.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import sun.tools.tree.FinallyStatement;
import sun.tools.tree.ThrowStatement;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/*
This class checks if test methods in the class either catch or throw exceptions. Use Junit's exception handling to automatically pass/fail the test
If this code detects the existence of a catch block or a throw statement in the methods body, the method is marked as smelly
 */
public class ExceptionCatchingThrowing extends AbstractSmell {

    private List<MethodUsage> methodExceptions;
    public ExceptionCatchingThrowing() {
        super("Exception Catching Throwing");
        methodExceptions = new ArrayList<>();
    }

    /**
     * Analyze the test file for test methods that have exception handling
     */
    @Override
    public void runAnalysis(CompilationUnit testFileCompilationUnit, CompilationUnit productionFileCompilationUnit, String testFileName, String productionFileName) throws FileNotFoundException {
        classVisitor = new ExceptionCatchingThrowing.ClassVisitor();
        classVisitor.visit(testFileCompilationUnit, null);

        for (MethodUsage method : methodExceptions) {
            TestMethod testClass = new TestMethod(method.getTestMethodName());
            testClass.addDataItem("begin", method.getBegin());
            testClass.addDataItem("end", method.getEnd());
            testClass.setHasSmell(true);
            smellyElementList.add(testClass);
        }
    }

    private class ClassVisitor extends VoidVisitorAdapter<Void> {
        private MethodDeclaration currentMethod = null;
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
            }
        }


//        @Override
//        public void visit(ThrowStmt n, Void arg) {
//            super.visit(n, arg);
//
//            if (currentMethod != null) {
//                methodExceptions.add(new MethodUsage(currentMethod.getNameAsString(), "", String.valueOf(n.getRange().get().begin.line), String.valueOf(n.getRange().get().end.line)));
//            }
//        }


        @Override
        public void visit(TryStmt n, Void arg) {
            methodExceptions.add(new MethodUsage(currentMethod.getNameAsString(), "", String.valueOf(n.getRange().get().begin.line), String.valueOf(n.getRange().get().end.line)));
        }

    }
}
