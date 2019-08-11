package gov.usgs.volcanoes.winston.tools.pannel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.Scnl;
import gov.usgs.volcanoes.core.legacy.plot.Plot;
import gov.usgs.volcanoes.core.legacy.plot.PlotException;
import gov.usgs.volcanoes.core.legacy.plot.render.HelicorderRenderer;
import gov.usgs.volcanoes.core.time.CurrentTime;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.core.time.TimeSpan;
import gov.usgs.volcanoes.winston.tools.FilePanel;
import gov.usgs.volcanoes.winston.tools.ScnlPanel;
import gov.usgs.volcanoes.winston.tools.WinstonToolsRunnablePanel;
import gov.usgs.volcanoes.wwsclient.WWSClient;

/**
 * A simple wrapper around gov.usgs.volcanoes.winston.PlotHelicorder
 *
 * @author Tom Parker
 */
public class PlotHelicorderPanel extends WinstonToolsRunnablePanel {
  public static enum FileType {
    JPEG("jpg"), PNG("png"), PS("ps");

    private String extension;

    private FileType(String s) {
      extension = s;
    }

    public static FileType fromExtenstion(String s) {
      int i = s.lastIndexOf('.');
      if (i != -1)
        s = s.substring(s.indexOf('.'));

      for (FileType m : FileType.values())
        if (m.getExtension() == s)
          return m;

      return null;
    }

    public String getExtension() {
      return extension;
    }
  }


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
  private static final int[] spanValues =
      new int[] {2, 4, 6, 12, 24, 48, 72, 96, 120, 144, 168, 192, 216, 240, 264, 288, 312, 336};
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

  private JTextField barRangeField;
  private JComboBox chunkListBox;
  private JTextField clipValueField;
  private JTextField endTimeField;
  private FilePanel filePanel;
  private ButtonGroup fileTypeGroup;
  private JPanel fileTypePanel;
  private JTextField heightField;
  private JTextField widthField;
  private JButton plotButton;
  private JTextField portField;
  private ScnlPanel scnlPanel;
  private JCheckBox showClipBox;
  private JPanel showClipPanel;
  private JComboBox spanListBox;
  private JComboBox timeZoneBox;
  private JTextField waveServerF;

  private String server;
  private int port;
  private transient TimeSpan timeSpan;
  private transient Scnl scnl;
  private long hours;
  private int timeChunk;
  private int heightPx;
  private int widthPx;
  private int barRange;
  private int clipValue;
  private boolean showClip;
  private TimeZone timeZone;
  private FileType fileType;
  private String fileName;

  public PlotHelicorderPanel() {
    super("Plot Helicorder");
  }

  @Override
  protected void createFields() {
    waveServerF = new JTextField(15);
    portField = new JTextField();
    portField.setText("16022");

    scnlPanel = new ScnlPanel();

    filePanel = new FilePanel(FilePanel.Type.SAVE);

    endTimeField = new JTextField(15);
    SimpleDateFormat dateF = new SimpleDateFormat(Time.INPUT_TIME_FORMAT);
    endTimeField.setText(dateF.format(new Date()));
    endTimeField.setToolTipText(Time.INPUT_TIME_FORMAT);
    endTimeField.getDocument().addDocumentListener(new TimeRangeDocumentListener(endTimeField));

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

    chunkListBox = new JComboBox(chunks);
    chunkListBox.setSelectedItem("" + DEFAULT_CHUNK);

    final String[] spans = new String[spanValues.length];
    for (int i = 0; i < spans.length; i++) {
      spans[i] = "" + spanValues[i];
    }

    spanListBox = new JComboBox(spans);
    spanListBox.setSelectedItem("" + DEFAULT_SPAN);

    final String[] tzs = TimeZone.getAvailableIDs();
    Arrays.sort(tzs);
    timeZoneBox = new JComboBox(tzs);
    timeZoneBox.setSelectedItem(DEFAULT_TIME_ZONE);

    heightField = new JTextField(6);
    heightField.setText("" + DEFAULT_HEIGHT);
    widthField = new JTextField(6);
    widthField.setText("" + DEFAULT_WIDTH);

    barRangeField = new JTextField(6);
    barRangeField.setText(DEFAULT_BAR_RANGE);

    clipValueField = new JTextField(6);
    clipValueField.setText(DEFAULT_CLIP_VALUE);

    showClipBox = new JCheckBox();
    showClipBox.setSelected(true);
    showClipBox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        clipValueField.setEnabled(showClipBox.isSelected());
      }
    });

    showClipPanel = new JPanel();
    showClipPanel.add(showClipBox);
    showClipPanel.add(clipValueField);

    plotButton = new JButton("Generate Plot");
    plotButton.addActionListener(new ActionListener() {
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
    builder.append("Port", portField);
    builder.nextLine();
    builder.appendSeparator("Channel");
    builder.append("SCNL", scnlPanel);
    builder.nextLine();
    builder.appendSeparator("Destination File");
    builder.append("File", filePanel);
    builder.nextLine();
    builder.append("File Type", fileTypePanel);
    builder.nextLine();
    builder.append("Height, px", heightField);
    builder.nextLine();
    builder.append("Width, px", widthField);
    builder.nextLine();
    builder.append("Show Clip", showClipPanel);
    builder.nextLine();
    builder.append("Bar Range", barRangeField);
    builder.nextLine();
    builder.appendSeparator("Time Range");
    builder.nextLine();
    builder.append("End", endTimeField);
    builder.nextLine();
    builder.append("Time Zone", timeZoneBox);
    builder.nextLine();
    builder.append("X, minutes", chunkListBox);
    builder.nextLine();
    builder.append("Y, hours", spanListBox);
    builder.nextLine();
    builder.appendUnrelatedComponentsGapRow();
    builder.nextLine();
    builder.append("", plotButton);

    this.add(builder.getPanel(), BorderLayout.CENTER);
  }

  @Override
  protected void go() {
    plotButton.setEnabled(false);

    HelicorderData heliData = null;
    try {
      parseFields();
      heliData = getData();
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    if (heliData.rows() > 0) {
      Plot plot = plot(heliData);

      System.out.print("writing ");
      try {
        switch (fileType) {
          case JPEG:
            System.out.print("JPEG ");
            plot.writeJPEG(fileName);
            break;
          case PNG:
            System.out.print("PNG ");
            plot.writePNG(fileName);
            break;
          case PS:
            System.out.print("PS ");
            plot.writePS(fileName);
            break;
          default:
            System.out.println("I don't know how to write a "
                + fileType + " file format.");
            break;
        }
        plot.writePNG(fileName);
      } catch (PlotException e) {
        e.printStackTrace();
      }
      System.out.println("Done.");
    } else {
      System.err.println("request returned no data.");
    }
    plotButton.setEnabled(true);

    plotButton.setEnabled(true);
  }

  private void parseFields() throws ParseException {
    server = waveServerF.getText().trim();
    port = Integer.parseInt(portField.getText().trim());
    scnl = scnlPanel.getScnlAsScnl();

    String endTimeString = endTimeField.getText();
    long endTime;
    if ("now".equalsIgnoreCase(endTimeString)) {
      endTime = CurrentTime.getInstance().now();
    } else if (endTimeString.startsWith("-")) {
      endTime = CurrentTime.getInstance().now();
      endTime -= (long) (Time.getRelativeTime(endTimeString) * 1000);
    } else {
      DateFormat format = new SimpleDateFormat(Time.INPUT_TIME_FORMAT);
      endTime = format.parse(endTimeString).getTime();
    }

    hours = Integer.parseInt(spanListBox.getSelectedItem().toString());
    timeChunk = Integer.parseInt(chunkListBox.getSelectedItem().toString());
    timeChunk *= 60;

    long span = hours * 60 * 60 * 1000;
    long startTime = endTime - span;
    startTime -= startTime % timeChunk;

    timeSpan = new TimeSpan(startTime, endTime);

    heightPx = Integer.parseInt(heightField.getText());
    widthPx = Integer.parseInt(widthField.getText());

    String clipText = clipValueField.getText();
    if ("auto".equalsIgnoreCase(clipText)) {
      clipValue = -1;
    } else {
      clipValue = Integer.parseInt(clipText);
    }

    String barRangeText = barRangeField.getText();
    if ("auto".equalsIgnoreCase(barRangeText)) {
      barRange = -1;
    } else {
      barRange = Integer.parseInt(barRangeText);
    }

    showClip = showClipBox.isSelected();
    timeZone = TimeZone.getTimeZone(timeZoneBox.getSelectedItem().toString());
    fileType = FileType.valueOf(fileTypeGroup.getSelection().getActionCommand().toUpperCase());
    fileName = filePanel.getFileName();
  }

  private HelicorderData getData() {
    WWSClient winston = new WWSClient(server, port);
    HelicorderData heliData = winston.getHelicorder(scnl, timeSpan, true);
    winston.close();

    return heliData;
  }


  private Plot plot(HelicorderData heliData) {
    HelicorderRenderer heliRenderer = new HelicorderRenderer();
    heliRenderer.setData(heliData);
    heliRenderer.setTimeChunk(timeChunk);
    heliRenderer.setLocation(70, 20, widthPx - (2 * 70), heightPx - (20 + 50));

    double mean = heliData.getMeanMax();
    double bias = heliData.getBias();
    mean = Math.abs(bias - mean);

    if (clipValue == -1) {
      clipValue = (int) (21 * mean);
      System.out.println("Automatic clip value: " + clipValue);
    }
    
    if (barRange == -1) {
      barRange = (int) (3 * mean);
      System.out.println("Automatic bar range: " + barRange);
    }

    heliRenderer.setHelicorderExtents(heliData.getStartTime(), heliData.getEndTime(),
        -1 * Math.abs(barRange), Math.abs(barRange));

    heliRenderer.setClipValue(clipValue);
    heliRenderer.setShowClip(showClip);

    heliRenderer.setClipBars(barRange);
    heliRenderer.setTimeZoneAbbr(timeZone.getDisplayName());
    heliRenderer.setTimeZoneOffset(timeZone.getOffset(timeSpan.startTime));
    heliRenderer.createDefaultAxis();

    Plot plot = new Plot();
    plot.setSize(widthPx, heightPx);
    plot.addRenderer(heliRenderer);

    return plot;
  }

  @Override
  public boolean needsWinston() {
    return false;
  }
}
