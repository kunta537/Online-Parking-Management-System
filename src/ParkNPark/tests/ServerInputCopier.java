/**
 * 
 */
package ParkNPark.tests;

import java.io.Reader;
import java.io.Writer;


/**
 * Copies server input and specially handles the input lines, looking for
 * patterns that signal certain events
 */
class ServerInputCopier extends InputCopier
{
	protected ProcessInfo processInfo;
	
	public ServerInputCopier(ProcessInfo processInfo, Reader reader, Writer writer) {
		super(reader, writer);
		this.processInfo = processInfo;
	}

	@Override
	protected String processLine(String line)
	{
		// If this is the "Server running." message, then the server is up
		if (line.equals("Server running.")) {
			synchronized(processInfo.interruptThread) {
				processInfo.ready = true;
				processInfo.interruptThread.interrupt();
			}
		}
		
		// Resume processing
		return line;
	}

	@Override
	protected void processingFinished()
	{
		// If the server finished prematurely, then notify the main thread
		synchronized(processInfo.interruptThread) {
			processInfo.exited = true;
			if (!processInfo.exitExpected) {
				System.out.println("!!Exit detected on server " + processInfo.name);
				processInfo.abnormalExit = true;
				processInfo.interruptThread.interrupt();
			} else
				System.out.println("  " + processInfo.name);
		}
	}
}
