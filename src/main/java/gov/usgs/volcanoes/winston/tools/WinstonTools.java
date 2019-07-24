package gov.usgs.volcanoes.winston.tools;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.winston.Version;
import gov.usgs.volcanoes.winston.db.WinstonDatabase;
import gov.usgs.volcanoes.winston.tools.pannel.ExportSACPanel;
import gov.usgs.volcanoes.winston.tools.pannel.ImportInstrumentLocations;
import gov.usgs.volcanoes.winston.tools.pannel.ImportSACPanel;
import gov.usgs.volcanoes.winston.tools.pannel.ImportSeedPanel;
import gov.usgs.volcanoes.winston.tools.pannel.ImportWSPanel;
import gov.usgs.volcanoes.winston.tools.pannel.PlotHelicorderPanel;
import gov.usgs.volcanoes.winston.tools.pannel.TimePanel;
import gov.usgs.volcanoes.winston.tools.pannel.WinstonUpgradePanel;

public class WinstonTools extends JFrame {
  private static final Logger LOGGER = LoggerFactory.getLogger(WinstonTools.class);

  private static final long serialVersionUID = -1;
  private JTabbedPane tabbedPane;
  private WinstonToolsMenu menuBar;
  private static final String TITLE = "Winston Tools";
  private static WinstonTools application;
  private static final int HEIGHT = 650;
  private static final int WIDTH = 550;

  public WinstonTools(final String[] args) {
    super(TITLE + " [" + Version.VERSION_STRING + "]");

    LOGGER.info("WinstonTools version: " + Version.VERSION_STRING);
    application = this;
  }


  public static void setRunning(final WinstonToolsRunnablePanel p) {
    if (p == null) {
      TermIO.stopRunning();
      enableWinstonPanels(true);
    } else {
      enableWinstonPanels(false);
      TermIO.startRunning(p instanceof WinstonToolsStoppablePanel);
    }
  }

  private static void enableWinstonPanels(final boolean b) {
    final JTabbedPane tabbedPane = getApplication().tabbedPane;
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      final WinstonToolsPanel wtp = (WinstonToolsPanel) tabbedPane.getComponentAt(i);
      if (wtp.needsWinston() && i != tabbedPane.getSelectedIndex())
        tabbedPane.setEnabledAt(i, b);
    }
  }

  private void createUI() {

    menuBar = new WinstonToolsMenu();
    getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

    this.setSize(WIDTH, HEIGHT);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setVisible(true);
    this.setJMenuBar(menuBar);

    tabbedPane = new JTabbedPane(SwingConstants.LEFT);
    addPanel(new TimePanel());
//    addPanel(new ExportSACPanel());
    addPanel(new PlotHelicorderPanel());
    addPanel(new ImportSACPanel());
    addPanel(new ImportSeedPanel());
    addPanel(new ImportWSPanel());
    // addPanel(new AdminPanel());
    addPanel(new ImportInstrumentLocations());
    addPanel(new WinstonUpgradePanel());
    this.add(tabbedPane);

    final WinstonDatabase winston =
        WinstonDatabase.processWinstonConfigFile(new ConfigFile("Winston.config"));
    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
      final WinstonToolsPanel wtp = (WinstonToolsPanel) tabbedPane.getComponentAt(i);
      tabbedPane.setTitleAt(i, wtp.getTitle());
    }

    final boolean winstonAlive = winston.checkConnect();
    enableWinstonPanels(winstonAlive);

    if (!winstonAlive) {
      final String message = "I couldn't find a valid Winston.config file in \n"
          + System.getProperty("user.dir") + "." + "\nSome features have been disabled.";

      JOptionPane.showMessageDialog(this, message, "Cannot connect to winston",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void addPanel(final WinstonToolsPanel p) {
    tabbedPane.add(p);
    menuBar.addHelpItem(p);
  }

  private static WinstonTools getApplication() {
    return application;
  }

  public static void main(final String[] args) {
    // initialize console window
    TermIO.getTerm();

    try {
      UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
    } catch (final UnsupportedLookAndFeelException e1) {
    }

    final WinstonTools tools = new WinstonTools(args);
    tools.createUI();
  }
}
