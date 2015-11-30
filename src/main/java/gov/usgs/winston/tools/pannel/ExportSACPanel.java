package gov.usgs.winston.tools.pannel;

import gov.usgs.util.Time;
import gov.usgs.winston.server.WWSClient;
import gov.usgs.winston.tools.FilePanel;
import gov.usgs.winston.tools.ScnlPanel;
import gov.usgs.winston.tools.WinstonToolsRunnablePanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ExportSACPanel extends WinstonToolsRunnablePanel {

	private static final long serialVersionUID = 1L;
	private static final Color RED = new Color(0xFFA07A);
	private static final String DEFAULT_CHUNK_SIZE = "900";
	private static final String DEFAULT_WAIT_TIME = "0";

	private JTextField waveServerF;
	private JTextField portF;
	private ScnlPanel scnlPanel;
	private FilePanel filePanel;
	private JTextField start;
	private JTextField end;
	private JTextField chunkSize;
	private JTextField waitTime;
	private JButton exportB;

	public ExportSACPanel() {
		super("Export SAC");
	}

	protected void createUI() {
		this.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder(Color.black), "Export SAC File"));

		FormLayout layout = new FormLayout(
				"right:max(40dlu;p), 4dlu, left:max(40dlu;p)", "");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
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
		builder.append("SAC File", filePanel);
		builder.nextLine();
		builder.appendSeparator("Time Range");
		builder.append("Start", start);
		builder.nextLine();
		builder.append("End", end);
		builder.nextLine();
		builder.appendSeparator("Schedule");
		builder.append("Gulp Size (s)", chunkSize);
		builder.nextLine();
		builder.append("Gulp Delay (s)", waitTime);
		builder.nextLine();
		builder.appendUnrelatedComponentsGapRow();
		builder.nextLine();
		builder.append("", exportB);

		this.add(builder.getPanel(), BorderLayout.CENTER);
	}

	protected void createFields() {
		waveServerF = new JTextField(15);
		portF = new JTextField();
		portF.setText("16022");

		scnlPanel = new ScnlPanel();

		filePanel = new FilePanel(FilePanel.Type.SAVE);

		start = new JTextField(15);
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -7);
		start.setText(gov.usgs.util.Time.format(
				gov.usgs.util.Time.INPUT_TIME_FORMAT, cal.getTime()));
		start.setToolTipText(gov.usgs.util.Time.INPUT_TIME_FORMAT);
		start.getDocument().addDocumentListener(
				new TimeRangeDocumentListener(start));

		end = new JTextField(15);
		end.setText(gov.usgs.util.Time.format(
				gov.usgs.util.Time.INPUT_TIME_FORMAT, new Date()));
		end.setToolTipText(gov.usgs.util.Time.INPUT_TIME_FORMAT);
		end.getDocument().addDocumentListener(
				new TimeRangeDocumentListener(end));

		chunkSize = new JTextField(5);
		chunkSize.setText(DEFAULT_CHUNK_SIZE);

		waitTime = new JTextField(5);
		waitTime.setText(DEFAULT_WAIT_TIME);

		exportB = new JButton("Export");
		exportB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				start();
			}
		});
	}

	protected void go() {
		exportB.setEnabled(false);

		String server = waveServerF.getText().trim();
		int port = Integer.parseInt(portF.getText().trim());
		String scnl = scnlPanel.getSCNL('$');
		String file = filePanel.getFileName();

		double cs;
		try {
			cs = Double.parseDouble(chunkSize.getText());
		} catch (Exception e) {
			cs = 0;
		}

		double wt;
		try {
			wt = Double.parseDouble(waitTime.getText());
		} catch (Exception e) {
			wt = 0;
		}

		// TODO fix error handling
		double[] d;

		try {
			d = Time.parseTimeRange(start.getText() + "," + end.getText());
			System.out.println("Starting export...");
			if (cs == 0)
				WWSClient.outputSac(server, port, d[0], d[1], scnl, file);
			else
				WWSClient.outputSac(server, port, d[0], d[1], scnl, file, cs,
						wt);
		} catch (ParseException e) {
		}

		System.out.println("Done.");

		exportB.setEnabled(true);
	}

	public boolean needsWinston() {
		return false;
	}

	public class TimeRangeOption {
		String title;
		String value;

		public TimeRangeOption(String t, String v) {
			title = t;
			value = v;
		}

		public String toString() {
			return title;
		}

		public String getValue() {
			return value;
		}
	}

	public class TimeRangeDocumentListener implements DocumentListener {

		JTextField f;

		public TimeRangeDocumentListener(JTextField f) {
			this.f = f;
		}

		public void insertUpdate(DocumentEvent e) {
			f.setBackground(validateTime() ? Color.white : RED);
		}

		public void removeUpdate(DocumentEvent e) {
			f.setBackground(validateTime() ? Color.white : RED);
		}

		public void changedUpdate(DocumentEvent e) {
		}

		private boolean validateTime() {
			try {
				Time.parseTimeRange(f.getText());
				return true;
			} catch (ParseException ex) {
				return false;
			}
		}
	}
}
