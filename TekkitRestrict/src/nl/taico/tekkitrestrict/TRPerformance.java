package nl.taico.tekkitrestrict;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class TRPerformance {
	//public static void reload() {
	//	ServerThreads = tekkitrestrict.config.getInt("ServerThreads");
	//	MaxTicks = tekkitrestrict.config.getInt("MaxTicks");
	//}

	public static void getThreadLag(CommandSender sender) {
		// java.lang.management.ThreadInfo ti = new ThreadInfo(null, 0, ti,
		// null, 0, 0, 0, 0, null);
		final ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
		final long[] threadIds = mxBean.getAllThreadIds();
		final ThreadInfo[] threadInfos = mxBean.getThreadInfo(threadIds);

		final File fss = new File("threadinfo.txt");
		try {
			fss.createNewFile();
			final FileWriter fstream = new FileWriter(fss);
			final BufferedWriter out = new BufferedWriter(fstream);

			float cputotal = 0;
			final List<ThreadInfo> threadInfot = new LinkedList<ThreadInfo>();
			for (final ThreadInfo threadInfo : threadInfos) {
				long cputime = ManagementFactory.getThreadMXBean().getThreadCpuTime(threadInfo.getThreadId());
				cputotal += cputime;
				threadInfot.add(threadInfo);
			}

			// order threads manually
			boolean done = false;

			final List<ThreadInfo> threadInfoz = new LinkedList<ThreadInfo>();
			while (!done) {
				if (threadInfot.size() == 0) {
					break;
				}
				ThreadInfo max = null;
				float maxcputime = 0;
				for (final ThreadInfo threadInfo : threadInfot) {
					final float cputime = ManagementFactory.getThreadMXBean().getThreadCpuTime(threadInfo.getThreadId());
					if (max == null) {
						max = threadInfo;
						maxcputime = cputime;
					} else {
						if (cputime >= maxcputime) {
							max = threadInfo;
							maxcputime = cputime;
						}
					}
				}
				threadInfot.remove(max);
				threadInfoz.add(max);
			}

			for (final ThreadInfo threadInfo : threadInfoz) {
				final float cputime = ManagementFactory.getThreadMXBean().getThreadCpuTime(threadInfo.getThreadId());
				// long cputime = ManagementFactory.getThreadMXBean().getT
				/*
				 * if(cputime > 0){
				 * p.sendRawMessage(threadInfo.getThreadName()+"-T:"
				 * +threadInfo.getBlockedTime()+"ms-CPU:"+cputime);
				 * 
				 * }
				 */
				if (cputime > 0) {
					// dump the rest into a file...
					out.write("Thread [" + threadInfo.getThreadId() + "] \"" + threadInfo.getThreadName() + "\"\n");
					out.write("  Blocked Count: " + threadInfo.getBlockedCount() + " time:" + threadInfo.getBlockedTime() + "\n");
					out.write("  CPU time: " + String.format("%.2f", 100.0f * (cputime / cputotal)) + "%\n");

					for (final Thread t : Thread.getAllStackTraces().keySet()) {
						if (t.getId() == threadInfo.getThreadId()) {
							for (final StackTraceElement eee : t.getStackTrace()) {
								out.write("    " + eee.toString() + "\n");
							}
						}
					}

				}
			}
			out.close();
			sender.sendMessage(ChatColor.YELLOW + "File 'threadinfo.txt' generated at serverdir.");
		} catch (final Exception ex) {
			sender.sendMessage(ChatColor.RED + "An error occurred while trying to generate threadinfo!");
			Log.debugEx(ex);
		}
	}

	//public static double getThreadTimeRatio() {
	//	return new Double(MaxTicks) / new Double(ServerThreads);
	//}

	//public static int ServerThreads, MaxTicks;
	//public static int x, z;
	//public static net.minecraft.server.Chunk chunk;
	//public static net.minecraft.server.WorldServer wo;
	//public static boolean didInit = false;
}