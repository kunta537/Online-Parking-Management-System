package ParkNPark.tests;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;


/**
 * Copies all errors to the given writer and flushes immediately after
 */
public class ErrorCopier extends InputCopier
{
	ProcessInfo processInfo;
	public ErrorCopier(ProcessInfo processInfo, Reader reader, Writer writer) {
		super(reader, writer);
		this.processInfo = processInfo;
	}
	
	@Override
	protected String processLine(String line)
	{
		// Write out the system's name first
		return processInfo.name + ": " + line;
	}

	@Override
	protected void lineProcessed(String line)
	{
		// Flush the output
		try {
		    writer.flush();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Don't close the output writer, because we don't want
	 * to close stderr
	 */
	@Override
	protected void closeWriter() { }
}
