package rr.simple;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class $ {

    public static long getTsc() {
        // return System.currentTimeMillis();
        return System.nanoTime();
    }

    public static long milliDelta(long beforeTsc, long afterTsc) {
		return (afterTsc - beforeTsc) / 1000000;
    }

    public static long nanoDelta(long beforeTsc, long afterTsc) {
		return afterTsc - beforeTsc ;
    }

    // ===============================================================

    /**
     * We put a static obj here to avoid frequent obj allocation
     */
    static Random rand = new Random();

	public static int randProb10000() {
		return rand.nextInt(10000);
    }

    // ===============================================================

    public static String getStackTrace() {
		StringBuilder builder = new StringBuilder();
		StackTraceElement[] stackTraces = Thread.currentThread().getStackTrace();

		int level = 0;
		for (int i = 1; i < stackTraces.length; ++i) {
			String currentTrace = stackTraces[i].toString();
			if (currentTrace.startsWith("io.github.midwinter1993") ||
				currentTrace.contains("__$rr")) {
				continue;
			}
			level += 1;

			builder.append(String.join("", Collections.nCopies(level, " ")));
			builder.append("-> ");
			builder.append(currentTrace);
			builder.append("\n");
		}
		//
		// Remove the last `\n`
		//
		builder.setLength(builder.length() - 1);

		return builder.toString();
    }

    public static long getTid() {
        return Thread.currentThread().getId();
    }

    // ===============================================================
    // private static final Logger logger = LogManager.getLogger("instrLog");

    public static void dumpClassLoader(ClassLoader loader) {
        if (loader == null) {
            System.err.println("bootstrap");
            return;
        }
        System.err.format("%s\n", loader.toString());
        dumpClassLoader(loader.getParent());
    }

    // ===============================================================

    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void warn(String title, String format, Object ...args) {
        System.err.format("%s[ %s ]%s ", ANSI_RED, title, ANSI_RESET);
        System.err.format(format, args);
    }

    public static void info(String format, Object ...args) {
        System.err.format(format, args);
    }

    // ===============================================================

    public static void mkdir(String dirPath) {
        File directory = new File(dirPath);
        if (! directory.exists()){
            directory.mkdir();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }
    }

    public static String pathJoin(String dirPath, String fileName) {
        Path path = Paths.get(dirPath, fileName);
        return path.toString();
    }
}