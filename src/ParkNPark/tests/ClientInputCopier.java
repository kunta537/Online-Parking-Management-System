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
class ClientInputCopier extends InputCopier
{
	protected ProcessInfo processInfo;
	
	public ClientInputCopier(ProcessInfo processInfo, Reader reader, Writer writer) {
		super(reader, writer);
		this.processInfo = processInfo;
	}

	@Override
	protected void lineProcessed(String line)
	{
		// If the client exited, then note that as an error so that testing can cease
		if (line.contains("; exiting")) {
			System.out.println("!!Abnormal exit on client " + processInfo.name);
			processInfo.abnormalExit = true;
		}
	}

	@Override
	protected void processingFinished()
	{
		// The client exited
		synchronized(processInfo.interruptThread) {
			processInfo.exited = true;
			System.out.println("  " + processInfo.name);
			processInfo.interruptThread.interrupt();
		}
	}
}
