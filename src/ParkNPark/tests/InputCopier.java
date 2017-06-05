package ParkNPark.tests;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Copies lines from a Reader to a Writer, one line at a time,
 * as a background Thread
 */
class InputCopier extends Thread
{
	protected BufferedReader reader;
	protected BufferedWriter writer;
	
	public InputCopier(Reader reader, Writer writer) {
		this.reader = new BufferedReader(reader);
		this.writer = new BufferedWriter(writer);
	}
	
	/**
	 * Processes the given line and returns the line that should
	 * be written to the writer or null if the copier
	 * should stop copying
	 * @param line The line returned from the reader
	 * @return The line to write to the writer or null if the
	 * copier should stop copying
	 */
	protected String processLine(String line) {
		return line;
	}
	
	/**
	 * Called when the processing finishes, just before
	 * the reader and writer are closed
	 */
	protected void processingFinished() {		
	}
	
	/**
	 * Called after a line has been written to the writer
	 * @param line The line that was written to the writer
	 */
	protected void lineProcessed(String line) {
	}
	
	/**
	 * Called when the reader is ready to be closed
	 */
	protected void closeReader()
	{
		// Close the reader
		try {
		    reader.close();
		} catch (IOException e) {
			System.out.println("IOException while closing reader: " + e.getMessage());
		}
	}
	
	/**
	 * Called when the writer is ready to be closed
	 */
	protected void closeWriter()
	{
		// Close the writer
		try {
		    writer.close();
		} catch (IOException e) {
			System.out.println("IOException while closing writer: " + e.getMessage());
		}
	}

	/**
	 * Copies lines from the reader to the writer
	 */
	@Override
	public void run()
	{
		// Copy all lines from the reader to the writer and consult
		// with processLine() to determine if we should stop early
		String line = null;
		boolean stop = false;
		try {
			while (!stop && (line = reader.readLine()) != null) {
				if ((line = processLine(line)) != null) {
				    writer.write(line + "\n");
				    lineProcessed(line);
				} else
					stop = true;
			}
		} catch (IOException e) {
			System.out.println("IOException during input copying: " + e.getMessage());
		}
		
		// Notify extenders that we are finished processing
		processingFinished();
		
		// Close the reader
		closeReader();
		
		// Close the writer
		closeWriter();
	}
}