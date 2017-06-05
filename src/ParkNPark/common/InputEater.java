package ParkNPark.common;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Input eater OutputStream that does nothing with its input
 */
public class InputEater extends OutputStream
{
	@Override
	public void close() throws IOException {
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
	}

	@Override
	public void write(byte[] b) throws IOException {
	}

	@Override
	public void write(int b) throws IOException {
	}
}
