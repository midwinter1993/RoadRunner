package acme.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Timestamp;
import acme.util.io.NamedFileWriter;
import acme.util.io.SplitOutputWriter;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

public class XLog {
	static PrintWriter out;

	public static CommandLineOption<String> outputFileOption =
        CommandLine.makeString("xout", "", CommandLineOption.Kind.STABLE, "Clean Log file name for XLog.out.",
            new Runnable() { public void run() {
                String outFile = outputFileOption.get();
                Assert.assertTrue(outFile.length() > 0, "Bad File");
                XLog.out = new SyncPrintWriter(
								new PrintWriter(
									XLog.openLogFile(outFile),
									true));

				out.println(new Timestamp(System.currentTimeMillis()));

                // XLog.out = new SyncPrintWriter(
				// 				new PrintWriter(
				// 					new SplitOutputWriter(
				// 						out,
				// 						XLog.openLogFile(outFile)),
				// 				true));
            }
        } );

    public static void logf(String s, Object... ops) {
		synchronized(XLog.class) {
			// pad();
			out.printf(s + "\n", ops);
		}
    }

    public static String makeLogFileName(String relName) {
		new File(Util.outputPathOption.get()).mkdirs();
		String path = Util.outputPathOption.get();
		if (!path.equals("") && path.charAt(path.length() - 1) != File.separatorChar) {
			path += File.separatorChar;
		}
		return path + relName;
	}

	static public NamedFileWriter openLogFile(String name) {
		try {
//			new File(outputPathOption.get()).mkdirs();
			return new NamedFileWriter(makeLogFileName(name));
		} catch (IOException e) {
			Assert.fail(e);
			return null;
		}
    }

	private static class SyncPrintWriter extends PrintWriter {

		public SyncPrintWriter(PrintStream out) {
			super(out, true);
			this.lock = Util.class;
		}

		public SyncPrintWriter(Writer out) {
			super(out, true);
			this.lock = Util.class;
		}
	}

	static {
		out = new SyncPrintWriter(System.out);
	}
}