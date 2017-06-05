/**
 * 
 */
package ParkNPark.tests;

class ProcessInfo
{
	public Process process;
	public InputCopier stdout;
	public InputCopier stderr;
	public String name;
	protected boolean ready = false;
	protected boolean exitExpected = false;
	protected Thread interruptThread = Thread.currentThread();
	protected boolean abnormalExit = false;
	protected boolean exited = false;
}
