package rr.instrument.di;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.CodeIterator;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class InstrMethodCall extends ExprEditor {
    @Override
    public void edit(MethodCall mCall) throws CannotCompileException {
        String className = mCall.getClassName();
        if (className.startsWith("java.")) {
            return;
        }

        System.out.println("======" + mCall.getSignature());
        try {
            System.out.println("======" + mCall.getMethod().getLongName());
        } catch (NotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String methodName = mCall.getMethodName();
        String callLocation = String.format("%s @ %s:%d", methodName, mCall.getFileName(), mCall.getLineNumber());

        String beforeCallCode = String.format("rr.instrument.di.InstrRuntime.enterMethod($0, \"%s\", \"%s\", \"%s\");", methodName, mCall.getSignature(), callLocation);
        String afterCallCode = String.format("rr.instrument.di.InstrRuntime.exitMethod($0, \"%s\");", callLocation);

        final StringBuffer buffer = new StringBuffer();
        buffer.append("{")
                .append(beforeCallCode)
                .append("$_ = $proceed($$);")
                .append(afterCallCode)
                .append("}");
        System.out.println(buffer.toString());
        // if (mCall.getClassName().equals("Point") && mCall.getMethodName().equals("move"))
        mCall.replace(buffer.toString());
    }
}

public class InstrTransformer implements ClassFileTransformer {
    static Set<String> WHITELIST = new HashSet(Arrays.asList("other/Stuff"));
    static Set<String> BLACKLIST = new HashSet(Arrays.asList(
        "java/", "sun/", "agent/", "rr/"
    ));

    boolean filter(String className) {
        if (WHITELIST.contains(className)) {
            return false;
        }
        for (String prefix: BLACKLIST) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public byte[] transform(String className, byte[] bytes) {
        try {
				System.out.println("==============FUCK");

            return transform(null, className, null, null, bytes);
        } catch (IllegalClassFormatException e) {
            //TODO: handle exception
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] transform(ClassLoader classLoader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] bytes) throws IllegalClassFormatException {

        if (!filter(className)) {
            System.out.println(className);
            // Javassist
            try {

                ClassPool cp = ClassPool.getDefault();
                // ClassPool cp = ClassPool.getDefault();
                // CtClass cc = cp.get(className.replace('/', '.'));
                CtClass cc = cp.makeClass(new ByteArrayInputStream(bytes));

                if (cc.isInterface()) {
                    throw new IllegalArgumentException("Cannot instrument interfaces");
                }

                CtMethod[] methods = cc.getDeclaredMethods();

                for(CtMethod method: methods) {
                    CodeIterator iter = method.getMethodInfo().getCodeAttribute().iterator();
                    while (iter.hasNext()) {
                        int pos = iter.next();
                    }
                    String name = method.getName();
                    System.out.println("[ Instrumenting ]" + name);
                    // System.out.println(InstrRuntime.class.getName());

                    method.instrument(new InstrMethodCall());
                }
                // m.addLocalVariable("elapsedTime", CtClass.longType);
                // m.insertBefore("elapsedTime = System.currentTimeMillis();");
                // m.insertAfter("{elapsedTime = System.currentTimeMillis() - elapsedTime;"
                // + "System.out.println(\"Method Executed in ms: \" + elapsedTime);}");
                byte[] byteCode = cc.toBytecode();
                cc.detach();
                return byteCode;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }
}