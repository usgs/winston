package gov.usgs.winston.tools.pannel;

import gov.usgs.util.Util;
import gov.usgs.winston.tools.WinstonToolsPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A small time panel which alerts user to bad input times
 * 
 * @author Tom Parker
 *
 */
public class TimePanel extends WinstonToolsPanel {
	
	private static final long serialVersionUID = 1L;
	private static final Color RED = new Color(0xFFA07A);
	private static final Color WHITE = new Color(0xFFFFFF);
	private static final NumberFormat timeFormatter = new DecimalFormat("#0.000");

    private JTextField standardF;
    private JTextField inputF;
    private JTextField ewF;
    private JTextField j2kF;
    private JButton nowB;
    private Time time;
    
	public TimePanel()
	{
		super("Time");
	}

	protected void createUI() {
		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Time Conversion"));

		FormLayout layout = new FormLayout(
				"right:max(40dlu;p), 4dlu, left:p",
				"");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		builder.appendSeparator("Time Formats");
		builder.append("Standard", standardF);
		builder.nextLine();
		builder.append("Input", inputF);
	    builder.nextLine(); 
		builder.append("J2K", j2kF);
	    builder.nextLine(); 
		builder.append("EW", ewF);
	    builder.nextLine(); 
	    builder.appendUnrelatedComponentsGapRow();
	    builder.nextLine();
	    builder.append("", nowB);
	    this.add(builder.getPanel(), BorderLayout.CENTER);

	}

	protected void createFields() {

		time = new Time();

		standardF = new StandardTimeBox();
		inputF = new InputTimeBox();
		ewF = new EwBox();
		j2kF = new J2kBox();
		
		time.setDate();

		nowB = new JButton("Now");
		nowB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				time.setDate();
			}
		});
	}

	public class StandardTimeBox extends JTextField implements Observer 
	{

		private static final long serialVersionUID = 1L;

		public StandardTimeBox() {
			super(20);
			time.addObserver(this);
			this.setToolTipText(gov.usgs.util.Time.STANDARD_TIME_FORMAT);
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					updateTime(getText());
				}
			});
			addFocusListener(new FocusListener(){
				public void focusGained(FocusEvent e) {}
				public void focusLost(FocusEvent e) {
					updateTime(getText());
				}});

		}

		public final void updateTime(String s)
		{
			double d = gov.usgs.util.Time.parse(gov.usgs.util.Time.STANDARD_TIME_FORMAT, getText());
			if (d != 0)
				time.setDate(Util.j2KToDate(d));
			else
				this.setBackground(RED);
		}
		
		public void update(Observable o, Object arg) {
			this.setText(""+((Time) o).getStandardTime());
			this.setBackground(WHITE);
		}
	}	

	public class InputTimeBox extends JTextField implements Observer 
	{

		private static final long serialVersionUID = 1L;

		public InputTimeBox() {
			super(20);
			time.addObserver(this);
			this.setToolTipText(gov.usgs.util.Time.INPUT_TIME_FORMAT);
			
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					updateTime(getText());
				}
			});
			addFocusListener(new FocusListener(){
				public void focusGained(FocusEvent e) {}
				public void focusLost(FocusEvent e) {
					updateTime(getText());
				}});
		}

		public final void updateTime(String s)
		{
			double d = gov.usgs.util.Time.parse(gov.usgs.util.Time.INPUT_TIME_FORMAT, getText());
			if (d != 0)
				time.setDate(Util.j2KToDate(d));
			else
				this.setBackground(RED);
		}
		public void update(Observable o, Object arg) {
			this.setText(""+((Time) o).getInputTime());
			this.setBackground(WHITE);
		}
	}
	
	public class EwBox extends JTextField implements Observer 
	{

		private static final long serialVersionUID = 1L;

		public EwBox() {
			super(20);
			time.addObserver(this);
			this.setToolTipText("seconds past 1970-01-01 00:00:00 UTC");
		
			
			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					updateTime(getText());
				}
			});
			addFocusListener(new FocusListener(){
				public void focusGained(FocusEvent e) {}
				public void focusLost(FocusEvent e) {
					updateTime(getText());
				}});

		}
		
		public final void updateTime(String s)
		{
			try
			{
				time.setDate(Util.j2KToDate(Util.ewToJ2K(Double.parseDouble(s))));
			} catch (NumberFormatException ex) {
				this.setBackground(RED);
			}
		}

		public void update(Observable o, Object arg) {
			double d = ((Time) o).getEW();
			this.setText(timeFormatter.format(d));
			this.setBackground(WHITE);
		}
	}
	
	public class J2kBox extends JTextField implements Observer {

		private static final long serialVersionUID = 1L;

		public J2kBox() {
			super(20);
			time.addObserver(this);
			setToolTipText("seconds past 2000-01-01 12:00:00 UTC");

			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					updateTime(getText());
				}
			});
			addFocusListener(new FocusListener(){
				public void focusGained(FocusEvent e) {}
				public void focusLost(FocusEvent e) {
					updateTime(getText());
				}});


		}
		
		public final void updateTime(String s)
		{
			try
			{
				time.setDate(Util.j2KToDate(Double.parseDouble(s)));
			} catch (NumberFormatException ex) {
				this.setBackground(RED);
			}

		}
		
		public void update(Observable o, Object arg) {
			double d = ((Time) o).getJ2k();
			this.setText(timeFormatter.format(d));
			this.setBackground(WHITE);
		}
		
	}
	
	public class Time extends Observable {

		Date date;
		
		public Time() {
			setDate();
		}
		
		
		public void setDate() {
			setDate(new Date());
		}
		
		public void setDate(Date d) {
			date = d;
			setChanged();
			notifyObservers();
		}
		
		public double getJ2k()
		{
			return Util.dateToJ2K(date);
		}
		
		public double getEW()
		{
			return Util.j2KToEW(Util.dateToJ2K(date));
		}

		public String getInputTime()
		{
			return gov.usgs.util.Time.format(gov.usgs.util.Time.INPUT_TIME_FORMAT, date);
		}
		
		public String getStandardTime()
		{
			return gov.usgs.util.Time.format(gov.usgs.util.Time.STANDARD_TIME_FORMAT, date);
		}
	}

	public boolean needsWinston() {
		return false;
	}
}