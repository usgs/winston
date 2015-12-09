package gov.usgs.volcanoes.winston.tools.pannel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import gov.usgs.util.Time;
import gov.usgs.volcanoes.winston.PlotHelicorder;
import gov.usgs.volcanoes.winston.PlotHelicorder.FileType;
import gov.usgs.volcanoes.winston.tools.FilePanel;
import gov.usgs.volcanoes.winston.tools.ScnlPanel;
import gov.usgs.volcanoes.winston.tools.WinstonToolsRunnablePanel;

/**
 * A simple wrapper around gov.usgs.volcanoes.winston.PlotHelicorder
 *
 * @author Tom Parker
 */
public class PlotHelicorderPanel extends WinstonToolsRunnablePanel {

  public class FileTypeActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      String sn = filePanel.getFileName();
      final int i = sn.lastIndexOf('.');
      if (i != -1) {
        sn = sn.substring(0, i);
      }

      sn = sn + "." + e.getActionCommand();
      filePanel.setFileName(sn);
    }
  }
  public class TimeRangeDocumentListener implements DocumentListener {

    JTextField f;

    public TimeRangeDocumentListener(final JTextField f) {
      this.f = f;
    }

    public void changedUpdate(final DocumentEvent e) {}

    public void insertUpdate(final DocumentEvent e) {
      f.setBackground(validateTime() ? Color.white : RED);
    }

    public void removeUpdate(final DocumentEvent e) {
      f.setBackground(validateTime() ? Color.white : RED);
    }

    private boolean validateTime() {
      try {
        Time.parseTimeRange(f.getText());
        return true;
      } catch (final ParseException ex) {
        return false;
      }
    }
  }

  public class TimeRangeOption {
    String title;
    String value;

    public TimeRangeOption(final String t, final String v) {
      title = t;
      value = v;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return title;
    }
  }

  private static final int[] chunkValues = new int[] {10, 15, 20, 30, 60, 120, 180, 360};

  private static final String DEFAULT_BAR_RANGE = "auto";
  private static final int DEFAULT_CHUNK = 30;

  private static final String DEFAULT_CLIP_VALUE = "auto";
  private static final String DEFAULT_FILE_NAME = "heli.png";
  private static final FileType DEFAULT_FILE_TYPE = FileType.PNG;
  private static final int DEFAULT_HEIGHT = 1280;

  private static final int DEFAULT_SPAN = 24;
  private static final String DEFAULT_TIME_ZONE = "UTC";

  private static final int DEFAULT_WIDTH = 1024;
  private static final Color RED = new Color(0xFFA07A);
  private static final long serialVersionUID = 1L;
  private static final int[] spanValues =
      new int[] {2, 4, 6, 12, 24, 48, 72, 96, 120, 144, 168, 192, 216, 240, 264, 288, 312, 336};
  private JTextField barRange;
  private JComboBox chunkList;
  private JTextField clipValue;
  private JTextField end;
  private FilePanel filePanel;
  private ButtonGroup fileTypeGroup;
  private JPanel fileTypePanel;
  private JTextField height;
  private JButton plotB;
  private JTextField portF;
  private ScnlPanel scnlPanel;
  private JCheckBox showClip;
  private JPanel showClipPanel;
  private JComboBox spanList;


  private JComboBox timeZones;

  private JTextField waveServerF;

  private JTextField width;

  public PlotHelicorderPanel() {
    super("Plot Helicorder");
  }

  @Override
  protected void createFields() {
    waveServerF = new JTextField(15);
    portF = new JTextField();
    portF.setText("16022");

    scnlPanel = new ScnlPanel();

    filePanel = new FilePanel(FilePanel.Type.SAVE);

    end = new JTextField(15);
    SimpleDateFormat dateF = new SimpleDateFormat(Time.INPUT_TIME_FORMAT);
    end.setText(dateF.format(new Date()));
    end.setToolTipText(Time.INPUT_TIME_FORMAT);
    end.getDocument().addDocumentListener(new TimeRangeDocumentListener(end));

    fileTypePanel = new JPanel();
    fileTypeGroup = new ButtonGroup();

    for (final FileType ft : FileType.values()) {
      final JRadioButton b = new JRadioButton(ft.name());
      b.addActionListener(new FileTypeActionListener());
      b.setActionCommand(ft.getExtension());
      fileTypePanel.add(b);
      fileTypeGroup.add(b);
      if (ft == DEFAULT_FILE_TYPE) {
        b.setSelected(true);
      }
    }

    filePanel.setFileName(DEFAULT_FILE_NAME);


    final String[] chunks = new String[chunkValues.length];
    for (int i = 0; i < chunks.length; i++) {
      chunks[i] = "" + chunkValues[i];
    }

    chunkList = new JComboBox(chunks);
    chunkList.setSelectedItem("" + DEFAULT_CHUNK);

    final String[] spans = new String[spanValues.length];
    for (int i = 0; i < spans.length; i++) {
      spans[i] = "" + spanValues[i];
    }

    spanList = new JComboBox(spans);
    spanList.setSelectedItem("" + DEFAULT_SPAN);

    final String[] tzs = TimeZone.getAvailableIDs();
    Arrays.sort(tzs);
    timeZones = new JComboBox(tzs);
    timeZones.setSelectedItem(DEFAULT_TIME_ZONE);

    height = new JTextField(6);
    height.setText("" + DEFAULT_HEIGHT);
    width = new JTextField(6);
    width.setText("" + DEFAULT_WIDTH);

    barRange = new JTextField(6);
    barRange.setText(DEFAULT_BAR_RANGE);

    clipValue = new JTextField(6);
    clipValue.setText(DEFAULT_CLIP_VALUE);

    showClip = new JCheckBox();
    showClip.setSelected(true);
    showClip.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        clipValue.setEnabled(showClip.isSelected());
      }
    });

    showClipPanel = new JPanel();
    showClipPanel.add(showClip);
    showClipPanel.add(clipValue);

    plotB = new JButton("Generate Plot");
    plotB.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        start();
      }
    });

  }

  @Override
  protected void createUI() {
    setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
        "Plot Helicorder"));

    final FormLayout layout = new FormLayout("right:max(40dlu;p), 4dlu, left:max(40dlu;p)", "");

    final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();
    builder.appendSeparator("Source Wave Server");
    builder.append("Host", waveServerF);
    builder.nextLine();
    builder.append("Port", portF);
    builder.nextLine();
    builder.appendSeparator("Channel");
    builder.append("SCNL", scnlPanel);
    builder.nextLine();
    builder.appendSeparator("Destination File");
    builder.append("File", filePanel);
    builder.nextLine();
    builder.append("File Type", fileTypePanel);
    builder.nextLine();
    builder.append("Height, px", height);
    builder.nextLine();
    builder.append("Width, px", width);
    builder.nextLine();
    builder.append("Show Clip", showClipPanel);
    builder.nextLine();
    builder.append("Bar Range", barRange);
    builder.nextLine();
    builder.appendSeparator("Time Range");
    builder.nextLine();
    builder.append("End", end);
    builder.nextLine();
    builder.append("Time Zone", timeZones);
    builder.nextLine();
    builder.append("X, minutes", chunkList);
    builder.nextLine();
    builder.append("Y, hours", spanList);
    builder.nextLine();
    builder.appendUnrelatedComponentsGapRow();
    builder.nextLine();
    builder.append("", plotB);

    this.add(builder.getPanel(), BorderLayout.CENTER);
  }

  @Override
  protected void go() {
    plotB.setEnabled(false);

    final String server = waveServerF.getText().trim();
    final int port = Integer.parseInt(portF.getText().trim());
    final String scnl = scnlPanel.getSCNLasSCNL('_');

    final String timeZone = (String) timeZones.getSelectedItem();
    final SimpleDateFormat dateFormat = new SimpleDateFormat(Time.INPUT_TIME_FORMAT);
    long date = 0;
    try {
      date = dateFormat.parse(end.getText()).getTime();
    } catch (final ParseException e) {
      e.printStackTrace();
    }

    // TODO fix error handling
    final ArrayList<String> args = new ArrayList<String>();

    args.add("-wws");
    args.add(server + ":" + port);
    args.add("-s");
    args.add(scnl);
    args.add("-e");
    args.add(end.getText());
    args.add("-m");
    args.add(chunkList.getSelectedItem().toString());
    args.add("-h");
    args.add(spanList.getSelectedItem().toString());
    args.add("-tz");
    args.add(timeZone);
    args.add("-to");
    args.add("" + TimeZone.getTimeZone(timeZone).getOffset(date));
    args.add("-x");
    args.add("1000");
    args.add("-y");
    args.add("1000");
    args.add("-lm");
    args.add("70");
    args.add("-rm");
    args.add("70");
    args.add("-o");
    args.add(filePanel.getFileName());
    if (!clipValue.getText().equalsIgnoreCase("auto")) {
      args.add("-c");
      args.add(clipValue.getText());
    }
    args.add("-b");
    args.add(null);
    args.add("-r");
    args.add("" + (showClip.isSelected() ? 1 : 0));
    args.add("-ft");
    args.add(fileTypeGroup.getSelection().getActionCommand());

    new PlotHelicorder(args.toArray(new String[0]));
    System.out.println("Done.");

    plotB.setEnabled(true);
  }

  @Override
  public boolean needsWinston() {
    return false;
  }
}
