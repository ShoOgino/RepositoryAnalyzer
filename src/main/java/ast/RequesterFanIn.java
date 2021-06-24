package ast;
import data.Module;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;

public class RequesterFanIn extends FileASTRequestor {
    private final HashMap<String, Module> modules;
    public ArrayList<String> methodsCalled = new ArrayList<>();

    public void acceptAST(String pathFile, CompilationUnit ast) {
        //System.out.println("---------------" + pathFile + "-----------------------------------------");
        pathFile = pathFile.replace("\\", "/");
        VisitorFanIn visitorFanIn = new VisitorFanIn(ast, pathFile, modules);
        ast.accept(visitorFanIn);
        methodsCalled.addAll(visitorFanIn.methodsCalled);
    }
    public RequesterFanIn(HashMap<String, Module> modules){
        this.modules=modules;
    }
}
