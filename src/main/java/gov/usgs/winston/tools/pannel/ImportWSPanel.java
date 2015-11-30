package gov.usgs.winston.tools.pannel;

import gov.usgs.util.ConfigFile;
import gov.usgs.util.Time;
import gov.usgs.winston.in.ew.ImportWS;
import gov.usgs.winston.tools.ScnlPanel;
import gov.usgs.winston.tools.WinstonToolsStoppablePanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.ParseException;
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

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ImportWSPanel extends WinstonToolsStoppablePanel {

	private static final long serialVersionUID = 1L;
	private static final Color RED = new Color(0xFFA07A);
	
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
	
	protected void createUI() 
	{
		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Import From Wave Server"));

		FormLayout layout = new FormLayout(
				"right:max(40dlu;p), 4dlu, p",
				"");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
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
	
	protected void createFields()
	{
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
		ButtonGroup timeRangeG = new ButtonGroup();
		timeRangeG.add(explicitB);
		timeRangeG.add(relativeB);
		
		start = new JTextField();
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		start.setText(gov.usgs.util.Time.format(gov.usgs.util.Time.INPUT_TIME_FORMAT, cal.getTime()));
		start.setToolTipText(gov.usgs.util.Time.INPUT_TIME_FORMAT);
		start.addFocusListener(new TimeRangeFocusListener(explicitB));
		start.getDocument().addDocumentListener(new TimeRangeDocumentListener(start));

		end = new JTextField();
		end.setText(gov.usgs.util.Time.format(gov.usgs.util.Time.INPUT_TIME_FORMAT, new Date()));
		end.setToolTipText(gov.usgs.util.Time.INPUT_TIME_FORMAT);
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
		importB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				start();
			}});
	}
	
	protected void go()
	{
		String s;
		
		importB.setEnabled(false);
		ws.setQuit(false);
		ConfigFile config = new ConfigFile("Winston.config");
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
		
//		config.put("rsam.delta", rsamDelta.getText());
//		config.put("rsam.duration", rsamDuration.getText());
		
		ws.setConfig(config);
		ws.processConfig();
		ws.createJobs();
		ws.startImport();
		importB.setEnabled(true);
	}
	

	public void stop() {
		ws.quit();
	}


	public boolean needsWinston() {
		return true;
	}
	private String getTimeRange()
	{
		String s;
		if (explicitB.isSelected())
			s = start.getText() + "," + end.getText();
		else
		{
			s = ((TimeRangeOption) rangeList.getSelectedItem()).getValue();
			s += "," + gov.usgs.util.Time.format(gov.usgs.util.Time.INPUT_TIME_FORMAT, new Date());
		}
	
		return s;
	}
	
	public class TimeRangeOption {
		String title;
		String value;
		public TimeRangeOption(String t, String v)
		{
			title = t;
			value = v;
		}
		
		public String toString()
		{
			return title;
		}
		
		public String getValue()
		{
			return value;
		}
	}
	
	public class TimeRangeFocusListener implements FocusListener {
		
		JRadioButton b;
		TimeRangeFocusListener(JRadioButton b)
		{
			this.b = b;
		}
		public void focusGained(FocusEvent e) {
			b.setSelected(true);
		}

		public void focusLost(FocusEvent e) {}
	}
	
	public class TimeRangeDocumentListener implements DocumentListener {

		JTextField f;
		public TimeRangeDocumentListener(JTextField f)
		{
			this.f = f;
		}
		
		public void insertUpdate(DocumentEvent e) {
			f.setBackground(validateTime()? Color.white : RED);
		}
		public void removeUpdate(DocumentEvent e) {
			f.setBackground(validateTime()? Color.white : RED);
		}

		public void changedUpdate(DocumentEvent e) {}
		
		private boolean validateTime()
		{
			try
			{
				Time.parseTimeRange(f.getText());
				return true;
			} catch (ParseException ex) {
				return false;
			}
		}
	}
}
