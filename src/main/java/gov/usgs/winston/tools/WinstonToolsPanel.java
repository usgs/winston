package gov.usgs.winston.tools;

import javax.swing.JPanel;

public abstract class WinstonToolsPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final String helpFileLocation = "file:gov/usgs/winston/tools/helpFiles/";

	protected final String panelTitle;
	private final String panelName;
	
	public abstract boolean needsWinston();
	protected abstract void createUI();
	protected abstract void createFields();
	
	public WinstonToolsPanel(String s)
	{
		super();
		panelTitle = s;
		createFields();
		createUI();
		String className = this.getClass().getSimpleName();
		int i = className.lastIndexOf('.');
		panelName = className.substring(i == -1 ? 0 : i);
	}
	

	public final String getTitle() 
	{
		return panelTitle;
	}
	
	// prohibit override to enforce file naming standard.
	public final String getHelpFileName()
	{
		return helpFileLocation + panelName + ".html";
	}
}

