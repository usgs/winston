package gov.usgs.winston.tools;

import javax.swing.JPanel;
import javax.swing.JTextField;

public class ScnlPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private JTextField sta;
	private JTextField comp;
	private JTextField net;
	private JTextField loc;
	
	public ScnlPanel () 
	{
		super();
		
		sta = new JTextField(5);
		add(sta);
		comp = new JTextField(3);
		add(comp);
		net = new JTextField(2);
		add(net);
		loc = new JTextField(2);
		add(loc);
	}
	
	public void setSCNL(String s)
	{
		String[] scnl = s.split("\\s");
		sta.setText(scnl[0]);
		comp.setText(scnl[1]);
		net.setText(scnl[2]);
		loc.setText(scnl[3]);
	}
	
	public String getSCNL(char c)
	{
		String s = sta.getText() + c + comp.getText() + c + net.getText();
		String l = loc.getText();
		if (l.equals("") || l.equals("--"))
			return s;
		else
			return s + c + l;
	}
	
	public String getSCNLasSCNL(char c)
	{
		String s = sta.getText() + c + comp.getText() + c + net.getText();
		String l = loc.getText();
		if (l.equals(""))
			l = "--";

		return s + c + l;
	}
	public String getSCNL()
	{
		return getSCNL(' ');
	}
}
