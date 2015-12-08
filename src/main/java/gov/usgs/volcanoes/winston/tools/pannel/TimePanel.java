package gov.usgs.volcanoes.winston.tools.pannel;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;

import gov.usgs.volcanoes.core.time.Ew;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.winston.tools.WinstonToolsPanel;

/**
 * A small time panel which alerts user to bad input times
 *
 * @author Tom Parker
 *
 */
public class TimePanel extends WinstonToolsPanel {

  public class EwBox extends JTextField implements Observer {

    private static final long serialVersionUID = 1L;

    public EwBox() {
      super(20);
      time.addObserver(this);
      setToolTipText("seconds past 1970-01-01 00:00:00 UTC");


      addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent evt) {
          updateTime(getText());
        }
      });
      addFocusListener(new FocusListener() {
        public void focusGained(final FocusEvent e) {}

        public void focusLost(final FocusEvent e) {
          updateTime(getText());
        }
      });

    }

    public void update(final Observable o, final Object arg) {
      final double d = ((TimeSubject) o).getEW();
      setText(timeFormatter.format(d));
      setBackground(WHITE);
    }

    public final void updateTime(final String s) {
      try {
        time.setDate(Ew.asDate(Double.parseDouble(s)));
      } catch (final NumberFormatException ex) {
        setBackground(RED);
      }
    }
  }
  public class InputTimeBox extends JTextField implements Observer {

    private static final long serialVersionUID = 1L;

    public InputTimeBox() {
      super(20);
      time.addObserver(this);
      setToolTipText(Time.INPUT_TIME_FORMAT);

      addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent evt) {
          updateTime(getText());
        }
      });
      addFocusListener(new FocusListener() {
        public void focusGained(final FocusEvent e) {}

        public void focusLost(final FocusEvent e) {
          updateTime(getText());
        }
      });
    }

    public void update(final Observable o, final Object arg) {
      setText("" + ((TimeSubject) o).getInputTime());
      setBackground(WHITE);
    }

    public final void updateTime(final String s) {
      SimpleDateFormat dateF = new SimpleDateFormat(Time.INPUT_TIME_FORMAT);
      Date d;
      try {
        d = dateF.parse(getText());
        time.setDate(d);
      } catch (ParseException e) {
        setBackground(RED);
      }
    }
  }
  public class J2kBox extends JTextField implements Observer {

    private static final long serialVersionUID = 1L;

    public J2kBox() {
      super(20);
      time.addObserver(this);
      setToolTipText("seconds past 2000-01-01 12:00:00 UTC");

      addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent evt) {
          updateTime(getText());
        }
      });
      addFocusListener(new FocusListener() {
        public void focusGained(final FocusEvent e) {}

        public void focusLost(final FocusEvent e) {
          updateTime(getText());
        }
      });


    }

    public void update(final Observable o, final Object arg) {
      final double d = ((TimeSubject) o).getJ2k();
      setText(timeFormatter.format(d));
      setBackground(WHITE);
    }

    public final void updateTime(final String s) {
      try {
        time.setDate(J2kSec.asDate(Double.parseDouble(s)));
      } catch (final NumberFormatException ex) {
        setBackground(RED);
      }

    }

  }
  public class StandardTimeBox extends JTextField implements Observer {

    private static final long serialVersionUID = 1L;

    public StandardTimeBox() {
      super(20);
      time.addObserver(this);
      setToolTipText(Time.STANDARD_TIME_FORMAT);
      addActionListener(new ActionListener() {
        public void actionPerformed(final ActionEvent evt) {
          updateTime(getText());
        }
      });
      addFocusListener(new FocusListener() {
        public void focusGained(final FocusEvent e) {}

        public void focusLost(final FocusEvent e) {
          updateTime(getText());
        }
      });

    }

    public void update(final Observable o, final Object arg) {
      setText("" + ((TimeSubject) o).getStandardTime());
      setBackground(WHITE);
    }

    public final void updateTime(final String s) {
      SimpleDateFormat dateF = new SimpleDateFormat(Time.STANDARD_TIME_FORMAT);
        try {
          time.setDate(dateF.parse(getText()));
          setBackground(RED);
        } catch (ParseException e) {
        }
    }
  }

  public class TimeSubject extends Observable {

    Date date;

    public TimeSubject() {
      setDate();
    }


    public double getEW() {
      return gov.usgs.volcanoes.core.time.Ew.fromEpoch(date.getTime());
    }

    public String getInputTime() {
      return Time.format(Time.INPUT_TIME_FORMAT, date);
    }

    public double getJ2k() {
      return J2kSec.fromDate(date);
    }

    public String getStandardTime() {
      return Time.format(Time.STANDARD_TIME_FORMAT, date);
    }

    public void setDate() {
      setDate(new Date());
    }

    public void setDate(final Date d) {
      date = d;
      setChanged();
      notifyObservers();
    }
  }

  private static final Color RED = new Color(0xFFA07A);
  private static final long serialVersionUID = 1L;
  private static final NumberFormat timeFormatter = new DecimalFormat("#0.000");
  private static final Color WHITE = new Color(0xFFFFFF);
  private JTextField ewF;

  private JTextField inputF;

  private JTextField j2kF;

  private JButton nowB;

  private JTextField standardF;

  private TimeSubject time;

  public TimePanel() {
    super("Time");
  }

  @Override
  protected void createFields() {

    time = new TimeSubject();

    standardF = new StandardTimeBox();
    inputF = new InputTimeBox();
    ewF = new EwBox();
    j2kF = new J2kBox();

    time.setDate();

    nowB = new JButton("Now");
    nowB.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        time.setDate();
      }
    });
  }

  @Override
  protected void createUI() {
    setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
        "Time Conversion"));

    final FormLayout layout = new FormLayout("right:max(40dlu;p), 4dlu, left:p", "");

    final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
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

  @Override
  public boolean needsWinston() {
    return false;
  }
}
