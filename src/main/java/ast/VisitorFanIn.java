package ast;

import data.Module;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VisitorFanIn extends ASTVisitor {
    CompilationUnit file;
    String pathFile ="";
    String pathMethod ="";
    ArrayList<String> methodsCalled= new ArrayList<>();
    HashMap<String, Module> modules = new HashMap<>();

    public VisitorFanIn(CompilationUnit ast, String pathFile, HashMap<String, Module> modules) {
        this.pathFile =pathFile.replace("\\", "/");
        this.file=ast;
        this.modules = modules;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        pathMethod = calculatePathMethod(node);

        Module module=modules.get(pathMethod);
        if(module!=null) {
            module.id = calculateIDMethod(node);
        }
        //System.out.println(pathMethod);

        return super.visit(node);
    }

    public boolean visit(MethodInvocation node) {
        String id = calculateIDMethod(node);
        methodsCalled.add(id);
        return super.visit(node);
    }

    //purpose: skip anonymous class.
    public boolean visit(AnonymousClassDeclaration classDeclarationStatement) {
        return false;
    }

    public String calculateIDMethod(MethodInvocation node) {
        IMethodBinding iMethodBinding = node.resolveMethodBinding();
        if(iMethodBinding!=null) {
            String namePackage = iMethodBinding.getDeclaringClass().getPackage().getName();

            String nameClass="";
            ITypeBinding classLast = iMethodBinding.getDeclaringClass();
            while(classLast!=null){
                String temp = classLast.getName().replaceAll("<\\w*>","");
                nameClass = "/" + temp + nameClass;
                classLast = classLast.getDeclaringClass();
            }

            String nameMethod = iMethodBinding.getName();

            String nameArgment = "";
            List<String> argments = Arrays.stream(iMethodBinding.getMethodDeclaration().getParameterTypes()).map(ITypeBinding::getName).collect(Collectors.toList());
            nameArgment = String.join(",",argments);

            return namePackage + nameClass + "/" + nameMethod + "(" + nameArgment + ")";
        }else {
            return null;
        }
    }


    public String calculateIDMethod(MethodDeclaration node){
        IMethodBinding iMethodBinding = node.resolveBinding();
        if(iMethodBinding!=null) {
            String namePackage = iMethodBinding.getDeclaringClass().getPackage().getName();

            String nameClass = "";
            ITypeBinding classLast = iMethodBinding.getDeclaringClass();
            while(classLast!=null){
                String temp = classLast.getName().replaceAll("<\\w*>","");
                nameClass = "/" + temp + nameClass;
                classLast = classLast.getDeclaringClass();
            }

            String nameMethod = iMethodBinding.getName();
            String nameArgment = "";
            List<String> argments = Arrays.stream(iMethodBinding.getMethodDeclaration().getParameterTypes()).map(ITypeBinding::getName).collect(Collectors.toList());
            nameArgment = String.join(",",argments);

            return namePackage + nameClass + "/" + nameMethod + "(" + nameArgment + ")";
        }else{
            return null;
        }
    }

    public String calculatePathMethod(MethodDeclaration node) {
        IMethodBinding iMethodBinding = node.resolveBinding();
        if(iMethodBinding!=null) {
            String pathMethod = "";
            File file = new File(pathFile);
            String regex = "(?<=repositoryFile_\\d/).+|(?<=repositoryFile_\\d\\d/).+|(?<=repositoryFile_\\d\\d\\d/).+";
            Pattern p = Pattern.compile(regex);
            String parent = file.getParent().replace("\\", "/");
            Matcher m = p.matcher(parent);
            String dirFile = null;
            if (m.find()) {
                dirFile = m.group();
            } else {
                System.out.println(pathFile);
                try {
                    throw new Exception();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            String nameFile = file.getName().split(".java")[0];

            String classMethod = "";
            ArrayList<String> listClass = new ArrayList<String>();
            ITypeBinding classLast = iMethodBinding.getDeclaringClass();
            classMethod = classLast.getName();
            while (classLast != null) {
                listClass.add(0, classLast.getName());
                classLast = classLast.getDeclaringClass();
            }
            for (int i = 0; i < listClass.size(); i++) {
                String nameClass = listClass.get(i);
                if (i == 0) {
                    if (nameClass.equals(nameFile)) {
                        classMethod = nameClass;
                    } else {
                        classMethod = nameClass + "[" + nameFile + "]";
                    }
                } else {
                    classMethod = classMethod + "." + nameClass;
                }
            }

            String nameMethod = new MethodNameGenerator(node).generate();

            pathMethod = dirFile + "/" + classMethod + "#" + nameMethod + ".mjava";
            pathMethod = pathMethod.replace("\\", "/");
            return pathMethod;
        }else{
            return null;
        }
    }

    public class MethodNameGenerator {
        private final MethodDeclaration node;
        private final StringBuilder buffer = new StringBuilder();

        public MethodNameGenerator(final MethodDeclaration node) {
            this.node = node;
        }

        public String generate() {
            generateTypeParameters();
            // generateReturnType();
            generateName();
            generateParameters();
            return buffer.toString();
        }

        protected void generateTypeParameters() {
            final List<Object> types = node.typeParameters();
            if (types != null && !types.isEmpty()) {
                final String typenames = types.stream()
                        .map(o -> escape(o.toString()))
                        .collect(Collectors.joining(","));
                buffer.append("[").append(typenames).append("]_");
            }
        }

        protected void generateReturnType() {
            final Type type = node.getReturnType2();
            if (type != null) {
                buffer.append(escape(type.toString())).append("_");
            }
        }

        protected void generateName() {
            buffer.append(node.getName().getIdentifier());
        }

        protected void generateParameters() {
            final List<Object> params = node.parameters();
            final String names = params.stream()
                    .map(o -> getTypeName((SingleVariableDeclaration) o))
                    .collect(Collectors.joining(","));
            buffer.append("(").append(names).append(")");
        }

        protected String getTypeName(final SingleVariableDeclaration v) {
            final StringBuilder sb = new StringBuilder();
            sb.append(escape(v.getType().toString()));
            for (int i = 0; i < v.getExtraDimensions(); i++) {
                sb.append("[]");
            }
            if (v.isVarargs()) {
                sb.append("...");
            }
            return sb.toString();
        }

        protected String escape(final String s) {
            return s.replace(' ', '-')
                    .replace('?', '#')
                    .replace('<', '[')
                    .replace('>', ']');
        }
    }
}
