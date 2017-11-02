package gov.usgs.volcanoes.winston.tools.pannel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.winston.in.ew.ImportWS;
import gov.usgs.volcanoes.winston.tools.ScnlPanel;
import gov.usgs.volcanoes.winston.tools.WinstonToolsStoppablePanel;

public class ImportWSPanel extends WinstonToolsStoppablePanel {

  private static final long serialVersionUID = 1L;
  private static final Color RED = new Color(0xFFA07A);
private static final Logger LOGGER = LoggerFactory.getLogger(ImportWSPanel.class);
  private ImportWS ws;
  private static JTextField waveServer;
  private static JTextField port;
  private static JTextField chunkSize;
  private static JTextField chunkDelay;
  private static JCheckBox createChannels;
  private static JCheckBox findGaps;
  private static ScnlPanel scnlPanel;
  private static JRadioButton explicitB;
  private static JRadioButton relativeB;
  private static JTextField start;
  private static JTextField end;
  private static JComboBox rangeList;
  private static JButton importB;

  public ImportWSPanel() {
    super("Import WS");
  }

  @Override
  protected void createUI() {
    this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
        "Import From Wave Server"));

    final FormLayout layout = new FormLayout("right:max(40dlu;p), 4dlu, p", "");

    final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();
    builder.appendSeparator("Source Wave Server");
    builder.append("Host", waveServer);
    builder.nextLine();
    builder.append("Port", port);
    builder.nextLine();
    builder.appendSeparator("Import Settings");
    builder.append("Chunk Size", chunkSize);
    builder.nextLine();
    builder.append("Chunk Delay", chunkDelay);
    builder.nextLine();
    builder.appendSeparator("Winston Settings");
    builder.append(createChannels, 3);
    builder.nextLine();
    builder.append(findGaps, 3);
    builder.nextLine();
    builder.appendSeparator("Channel");
    builder.append("SCNL", scnlPanel);
    builder.nextLine();
    builder.appendSeparator("Time Range");
    builder.append(explicitB, 3);
    builder.nextLine();
    builder.append("Start", start);
    builder.nextLine();
    builder.append("End", end);
    builder.append(relativeB, 3);
    builder.nextLine();
    builder.append("", rangeList);
    builder.nextLine();
    builder.appendUnrelatedComponentsGapRow();
    builder.nextLine();
    builder.append(importB, 3);

    this.add(builder.getPanel(), BorderLayout.CENTER);
  }

  @Override
  protected void createFields() {
    ws = new ImportWS();

    waveServer = new JTextField();
    port = new JTextField();
    port.setText("16022");

    chunkSize = new JTextField();
    chunkSize.setText("3600");
    chunkDelay = new JTextField();
    chunkDelay.setText("0");

    createChannels = new JCheckBox("Create Channels", true);
    findGaps = new JCheckBox("Find Gaps", true);

    scnlPanel = new ScnlPanel();
    scnlPanel.setSCNL("* * * *");

    explicitB = new JRadioButton("Explicit");
    relativeB = new JRadioButton("Relative", true);
    final ButtonGroup timeRangeG = new ButtonGroup();
    timeRangeG.add(explicitB);
    timeRangeG.add(relativeB);

    SimpleDateFormat dateF = new SimpleDateFormat(Time.INPUT_TIME_FORMAT);
    start = new JTextField();
    final Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -7);
    start.setText(dateF.format(cal.getTime()));
    start.setToolTipText(Time.INPUT_TIME_FORMAT);
    start.addFocusListener(new TimeRangeFocusListener(explicitB));
    start.getDocument().addDocumentListener(new TimeRangeDocumentListener(start));

    end = new JTextField();
    end.setText(dateF.format(new Date()));
    end.setToolTipText(Time.INPUT_TIME_FORMAT);
    end.addFocusListener(new TimeRangeFocusListener(explicitB));
    end.getDocument().addDocumentListener(new TimeRangeDocumentListener(end));

    rangeList = new JComboBox();
    rangeList.addItem(new TimeRangeOption("1 Day", "-1d"));
    rangeList.addItem(new TimeRangeOption("1 Week", "-1w"));
    rangeList.addItem(new TimeRangeOption("1 Month", "-1m"));
    rangeList.addItem(new TimeRangeOption("3 Months", "-3m"));
    rangeList.addItem(new TimeRangeOption("1 Year", "-1y"));
    rangeList.setSelectedIndex(0);
    rangeList.addFocusListener(new TimeRangeFocusListener(relativeB));

    importB = new JButton("Import");
    importB.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        start();
      }
    });
  }

  @Override
  protected void go() {
    String s;

    importB.setEnabled(false);
    ws.setQuit(false);
    final ConfigFile config = new ConfigFile("Winston.config");
    config.put("createDatabase", "true");

    s = waveServer.getText().trim() + ":" + port.getText().trim();
    config.put("waveServer", s);

    s = (createChannels.isSelected()) ? "true" : "flase";
    config.put("createChannels", s);

    config.put("channel", scnlPanel.getSCNL());

    s = getTimeRange();
    config.put("timeRange", s);

    config.put("chunkSize", chunkSize.getText());
    config.put("chunkDelay", chunkDelay.getText());

    // config.put("rsam.delta", rsamDelta.getText());
    // config.put("rsam.duration", rsamDuration.getText());

    ws.setConfig(config);
    try {
      ws.processConfig();
    } catch (ParseException e) {
      LOGGER.error("Cannot parse config. ({})", e.getLocalizedMessage());
      return;
    }
    ws.createJobs();
    ws.startImport();
    importB.setEnabled(true);
  }


  @Override
  public void stop() {
    ws.quit();
  }


  @Override
  public boolean needsWinston() {
    return true;
  }

  private String getTimeRange() {
    String s;
    SimpleDateFormat dateF = new SimpleDateFormat(Time.INPUT_TIME_FORMAT);
    if (explicitB.isSelected())
      s = start.getText() + "," + end.getText();
    else {
      s = ((TimeRangeOption) rangeList.getSelectedItem()).getValue();
      s += "," + dateF.format(new Date());
    }

    return s;
  }

  public class TimeRangeOption {
    String title;
    String value;

    public TimeRangeOption(final String t, final String v) {
      title = t;
      value = v;
    }

    @Override
    public String toString() {
      return title;
    }

    public String getValue() {
      return value;
    }
  }

  public class TimeRangeFocusListener implements FocusListener {

    JRadioButton b;

    TimeRangeFocusListener(final JRadioButton b) {
      this.b = b;
    }

    public void focusGained(final FocusEvent e) {
      b.setSelected(true);
    }

    public void focusLost(final FocusEvent e) {}
  }

  public class TimeRangeDocumentListener implements DocumentListener {

    JTextField f;

    public TimeRangeDocumentListener(final JTextField f) {
      this.f = f;
    }

    public void insertUpdate(final DocumentEvent e) {
      f.setBackground(validateTime() ? Color.white : RED);
    }

    public void removeUpdate(final DocumentEvent e) {
      f.setBackground(validateTime() ? Color.white : RED);
    }

    public void changedUpdate(final DocumentEvent e) {}

    private boolean validateTime() {
      try {
        Time.parseTimeRange(f.getText());
        return true;
      } catch (final ParseException ex) {
        return false;
      }
    }
  }
}
