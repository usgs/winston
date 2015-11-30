package gov.usgs.winston.tools;


public abstract class WinstonToolsRunnablePanel extends WinstonToolsPanel {

	private static final long serialVersionUID = 1L;
	protected abstract void go();
	
	public WinstonToolsRunnablePanel(String s) {
		super(s);
	}
	
	protected void start()
	{
		final WinstonToolsRunnablePanel p = this;
		Thread launchThread = new Thread(new Runnable()
		{
			public void run()
			{
				WinstonTools.setRunning(p);
				p.go();
				WinstonTools.setRunning(null);
			}
		});
		launchThread.start();
		TermIO.showTerm(true);
	}
}
