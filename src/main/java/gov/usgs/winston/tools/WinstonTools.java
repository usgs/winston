package gov.usgs.winston.tools;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.Log;
import gov.usgs.util.Util;
import gov.usgs.winston.db.WinstonDatabase;
import gov.usgs.winston.tools.pannel.AdminPanel;
import gov.usgs.winston.tools.pannel.ExportSACPanel;
import gov.usgs.winston.tools.pannel.ImportInstrumentLocations;
import gov.usgs.winston.tools.pannel.ImportSACPanel;
import gov.usgs.winston.tools.pannel.ImportSeedPanel;
import gov.usgs.winston.tools.pannel.ImportWSPanel;
import gov.usgs.winston.tools.pannel.PlotHelicorderPanel;
import gov.usgs.winston.tools.pannel.TimePanel;
import gov.usgs.winston.tools.pannel.WinstonUpgradePanel;

import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

public class WinstonTools extends JFrame
{
	private static final long serialVersionUID = -1;
	private JTabbedPane tabbedPane;
	private WinstonToolsMenu menuBar;
	private static final String TITLE = "Winston Tools";
	private static final String VERSION = "1.0.0.20120313";
	private final Logger logger;
	private static WinstonTools application;
	private static final int HEIGHT = 650;
	private static final int WIDTH = 550;
	
	public WinstonTools(String[] args)
	{
		super(TITLE + " [" + VERSION + "]");
		
		logger = Log.getLogger("gov.usgs.winston");
		logger.fine("WinstonTools version: " + VERSION);
		String[] ss = Util.getVersion("gov.usgs.winston.winstonTools");
		if (ss == null)
			logger.fine("no build version information available");
		else
			logger.fine("build version/date: " + ss[0] + "/" + ss[1]);
		
		application = this;
	}
	

	public static void setRunning(WinstonToolsRunnablePanel p)
	{
		if (p == null)
		{
			TermIO.stopRunning();
			enableWinstonPanels(true);
		}
		else
		{
			enableWinstonPanels(false);
			TermIO.startRunning(p instanceof WinstonToolsStoppablePanel);
		}
	}
	
	private static void enableWinstonPanels(boolean b)
	{
		JTabbedPane tabbedPane = getApplication().tabbedPane;
	 	for (int i = 0; i < tabbedPane.getTabCount(); i++)
	 	{
	 		WinstonToolsPanel wtp = (WinstonToolsPanel) tabbedPane.getComponentAt(i);
	 		if (wtp.needsWinston() && i != tabbedPane.getSelectedIndex())
	 			tabbedPane.setEnabledAt(i, b);
	 	}
	}
	
	private void createUI()
	{
		
		menuBar = new WinstonToolsMenu();
		getContentPane().setLayout(
			    new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS)
			);

		this.setSize(WIDTH, HEIGHT);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setVisible(true);
		this.setJMenuBar(menuBar);
		
	 	tabbedPane = new JTabbedPane(JTabbedPane.LEFT); 
	 	addPanel(new TimePanel());
	 	addPanel(new ExportSACPanel());
	 	addPanel(new PlotHelicorderPanel());
	 	addPanel(new ImportSACPanel());
	 	addPanel(new ImportSeedPanel());
	 	addPanel(new ImportWSPanel());
//	 	addPanel(new AdminPanel());
	 	addPanel(new ImportInstrumentLocations());
	 	addPanel(new WinstonUpgradePanel());
	 	this.add(tabbedPane);

	 	WinstonDatabase winston = WinstonDatabase.processWinstonConfigFile(new ConfigFile("Winston.config"));
	 	for (int i = 0; i < tabbedPane.getTabCount(); i++)
	 	{
	 		WinstonToolsPanel wtp = (WinstonToolsPanel) tabbedPane.getComponentAt(i);
	 		tabbedPane.setTitleAt(i, wtp.getTitle());
	 	}
	 	
	 	boolean winstonAlive = winston.checkConnect();
	 	enableWinstonPanels(winstonAlive);
	 	
	 	if (!winstonAlive)
	 	{
	 		String message = "I couldn't find a valid Winston.config file in \n" 
	 			+ System.getProperty("user.dir") + "." + "\nSome features have been disabled.";

	 		JOptionPane.showMessageDialog(this, message , "Cannot connect to winston", JOptionPane.ERROR_MESSAGE);
	 	}
	}
	
	private void addPanel(WinstonToolsPanel p)
	{
		tabbedPane.add(p);
		menuBar.addHelpItem(p);
	}
	
	private static WinstonTools getApplication()
	{
		return application;
	}

	public static void main(String[] args)
	{
		// initialize console window
		TermIO.getTerm();
		
		try {
			UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
		} catch (UnsupportedLookAndFeelException e1) {}
		
		WinstonTools tools = new WinstonTools(args);
		tools.createUI();
	}
}