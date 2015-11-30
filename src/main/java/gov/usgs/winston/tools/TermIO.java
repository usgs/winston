package gov.usgs.winston.tools;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class TermIO extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private static JButton closeB = new JButton("Close");
	private static JButton stopB = new JButton("Stop");
	private static TermIO term = new TermIO();

//	private PrintStream stdout;
//	private PrintStream stderr;
	private Document d;
	private boolean autoScroll;
	
	private TermIO()
	{
		super("Winston Tools Console");
		
//        stdout = System.out;                                       
//        stderr = System.err;                                       
        autoScroll = true;

        FormLayout layout = new FormLayout(
				"fill:p:G",
				"fill:p:G, p");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		
		builder.append(createContentPane());
		builder.nextLine();
	    builder.append(createControlPanel());
		getContentPane().add(builder.getPanel());
        pack();
        setSize(640, 480);	

        PrintStream o = new PrintStream(new DocumentOutputStream(d));
		System.setOut(o);
		System.setErr(o);
	}
	
	public static TermIO getTerm()
	{
		return term;
	}
	
	public static void showTerm(boolean b)
	{
		term.setVisible(b);
	}

	private JScrollPane createContentPane()
	{
        JTextArea t = new JTextArea("\n");
        d = t.getDocument();
        d.addDocumentListener(new ScrolledDocumentListener(t));
        
        JScrollPane areaScrollPane = new JScrollPane(t);
        areaScrollPane.setVerticalScrollBarPolicy(
        		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        areaScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        areaScrollPane.setPreferredSize(new Dimension(320, 240));
        
        return areaScrollPane;
		
	}

	private JPanel createControlPanel()
	{
		JPanel p = new JPanel();
		closeB.setEnabled(true);
        closeB.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		TermIO.this.dispose();
        	}});
        p.add(closeB);
        
    	addStopListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				stopB.setText("Stopping");
				stopB.setEnabled(false);
			}});
    	stopB.setEnabled(false);
    	p.add(stopB);
        
        JCheckBox autoScrollB = new JCheckBox("Auto scroll", autoScroll);
        autoScrollB.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				autoScroll = e.getStateChange() == ItemEvent.SELECTED;
				}
			});
        p.add(autoScrollB);
        
        return p;
	 
	}
	
	public void addStopListener(ActionListener a)
	{
		stopB.addActionListener(a);
	}
	
	public static void startRunning(boolean b) {
		closeB.setEnabled(false);
		stopB.setEnabled(b);
	}

	public static void stopRunning() {
		closeB.setEnabled(true);
		stopB.setEnabled(false);
		stopB.setText("Stop");
	}
	
	private class ScrolledDocumentListener implements DocumentListener
	{
		JTextArea ta;
		public ScrolledDocumentListener(JTextArea t)
		{
			ta = t;
		}

		public void changedUpdate(DocumentEvent e) {
			if (autoScroll)
				ta.setCaretPosition(ta.getText().length());
		}

		public void insertUpdate(DocumentEvent e) {
			if (autoScroll)
				ta.setCaretPosition(ta.getText().length());
		}

		public void removeUpdate(DocumentEvent e) {
			if (autoScroll)
				ta.setCaretPosition(ta.getText().length());
		}
		
	}

	public class DocumentOutputStream extends OutputStream
	{
	  private Document doc;
	  private boolean closed;

	  public DocumentOutputStream (Document d) 
	  {
	    super ();
	    doc = d;
	    closed = false;
	  }
	  
	  public void write (int i) throws IOException
	  {
	    if (closed)
	      return;

	    try  
	    {
	      doc.insertString( doc.getLength()-1, String.valueOf ( (char) i ), null);
	    }
	    catch (BadLocationException e) {}
	  }
	  
	  public void write (byte[] b, int offset, int length) throws IOException
	  {
	    if (closed)
	      return;

	    if (b == null)
	      throw new NullPointerException ("The byte array is null");
	    if (offset < 0 || length < 0 || (offset+length) > b.length)
	      throw new IndexOutOfBoundsException ("offset and length are negative or extend outside array bounds");
	    
	    String str = new String (b, offset, length);
	    try  
	    {
	      doc.insertString (doc.getLength()-1, str, null);
	    }
	   catch (BadLocationException e) {}
	  }
	  
	  public void close () 
	  {
	    doc = null;
	    closed = true;
	  }
	  
	}
}
