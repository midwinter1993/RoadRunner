package rr.instrument.di;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.NotFoundException;


public class InstrRuntime {
    public static Class<?>[] parseSignature(String signature) {
        List<Class<?>> klassList = new ArrayList();


        String parameters = signature.substring(signature.indexOf('(') + 1, signature.indexOf(')'));

        if (!parameters.isEmpty()) {
            System.out.println(parameters);
        }

        Class<?>[] buf = new Class<?>[klassList.size()];
        buf = klassList.toArray(buf);
        return buf;
    }

    public static Method lookupMethod(Object obj, String methodName, String signature) {
        Class<?> klass = obj.getClass();
        Method method = null;

        try {
            method = klass.getDeclaredMethod(methodName, parseSignature(signature));
        } catch (NoSuchMethodException e) {
            // e.printStackTrace();
        }
        if (method != null) {
            return method;
        }

        try {
            method = klass.getMethod(methodName, parseSignature(signature));
            return method;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();

            for (Method m: klass.getMethods()) {
                System.out.println("  > " + m.toString());
            }
            System.out.println("" + methodName + " " + signature);
            System.exit(-1);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }
        return null;
    }

    public static void enterMethod(Object target,
                                   String methodName,
                                   String signature,
                                   String callLocation) {
        // ClassPool cp = ClassPool.getDefault();
        // CtMethod method = null;
        // String className = target.getClass().getName();
        Method method = lookupMethod(target, methodName, signature);
        // try {
            // method = cp.getMethod(className, methodName);
        // } catch (NotFoundException e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
        // }

        // Method method = lookupMethod(target, methodName, signature);

        if (method == null) {
            System.out.println("XXXXXXXXX " + target.toString() + "|" + methodName + " " + signature);
            return;
        }

        if (target == null) {
            System.out.println("Enter " + callLocation + method.toString());
        } else {
            System.out.println("$$$$ Enter " + target.toString() + callLocation + method.toString());
        }
    }

    public static void exitMethod(Object target, String callLocation) {
        // System.out.println("Exit " + callLocation);
        if (target == null) {
            System.out.println("Exit " + callLocation);
        } else {
            System.out.println("Exit " + target.toString() + callLocation);
        }
    }
}