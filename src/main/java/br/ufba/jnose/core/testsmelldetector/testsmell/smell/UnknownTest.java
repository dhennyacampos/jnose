package br.ufba.jnose.core.testsmelldetector.testsmell.smell;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import br.ufba.jnose.core.testsmelldetector.testsmell.AbstractSmell;
import br.ufba.jnose.core.testsmelldetector.testsmell.SmellyElement;
import br.ufba.jnose.core.testsmelldetector.testsmell.TestMethod;
import br.ufba.jnose.core.testsmelldetector.testsmell.Util;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UnknownTest extends AbstractSmell {

    public UnknownTest() {
        super("Unknown Test");
    }

    /**
     * Analyze the test file for test methods that do not have assert statement or exceptions
     */
    @Override
    public void runAnalysis(CompilationUnit testFileCompilationUnit, CompilationUnit productionFileCompilationUnit, String testFileName, String productionFileName) throws FileNotFoundException {
        classVisitor = new UnknownTest.ClassVisitor();
        classVisitor.visit(testFileCompilationUnit, null);
    }

    private class ClassVisitor extends VoidVisitorAdapter<Void> {
        private MethodDeclaration currentMethod = null;
        TestMethod testMethod;
        List<String> assertMessage = new ArrayList<>();
        boolean hasAssert = false;
        boolean hasExceptionAnnotation = false;


        // examine all methods in the test class
        @Override
        public void visit(MethodDeclaration n, Void arg) {
            if (Util.isValidTestMethod(n)) {
                Optional<AnnotationExpr> assertAnnotation = n.getAnnotationByName("Test");
                if (assertAnnotation.isPresent()) {
                    for (int i = 0; i < assertAnnotation.get().getNodeLists().size(); i++) {
                        NodeList<?> c = assertAnnotation.get().getNodeLists().get(i);
                        for (int j = 0; j < c.size(); j++)
                            if (c.get(j) instanceof MemberValuePair) {
                                if (((MemberValuePair) c.get(j)).getName().equals("expected") && ((MemberValuePair) c.get(j)).getValue().toString().contains("Exception"))
                                    ;
                                hasExceptionAnnotation = true;
                            }
                    }
                }
                currentMethod = n;
                testMethod = new TestMethod(n.getNameAsString());
                testMethod.setHasSmell(false); //default value is false (i.e. no smell)
                testMethod.addDataItem("begin",String.valueOf(n.getRange().get().begin.line));
                testMethod.addDataItem("end",String.valueOf(n.getRange().get().end.line));
                super.visit(n, arg);

                // if there are duplicate messages, then the smell exists
                if (!hasAssert && !hasExceptionAnnotation)
                    testMethod.setHasSmell(true);

                smellyElementList.add(testMethod);

                //reset values for next method
                currentMethod = null;
                assertMessage = new ArrayList<>();
                hasAssert = false;
            }
        }


        // examine the methods being called within the test method
        @Override
        public void visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);
            if (currentMethod != null) {
                // if the name of a method being called start with 'assert'
                if (n.getNameAsString().startsWith(("assert"))) {
                    hasAssert = true;
                }
                // if the name of a method being called is 'fail'
                else if (n.getNameAsString().equals("fail")) {
                    hasAssert = true;
                }

            }
        }

    }
}

