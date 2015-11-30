package gov.usgs.winston.tools.pannel;

import gov.usgs.winston.db.Upgrade;
import gov.usgs.winston.tools.WinstonToolsRunnablePanel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class AdminPanel extends WinstonToolsRunnablePanel {

	private static final long serialVersionUID = 1L;
	Upgrade ug;
	JTextField currentVersion;
	JTextArea upgradeDescription;
	JButton upgradeB;
	
	public AdminPanel() 
	{
		super("Winston Admin");
	}
	
	protected void createUI()
	{
		
		
		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Winston Admin"));
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		FormLayout layout = new FormLayout(
				"left:p, 4dlu, fill:p:G",
				"");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();
		
		createFields();
		builder.append("Current schema", currentVersion);
		builder.nextLine();
		builder.append("Available upgrade", upgradeDescription);
		builder.nextLine();
		builder.appendUnrelatedComponentsGapRow();
		builder.nextLine();
		builder.append("", upgradeB);
	    add(builder.getPanel());
	}
	
	protected void createFields()
	{
		ug = new Upgrade();
		
		currentVersion = new JTextField(ug.getCurrentVersion());
		
		upgradeDescription = new JTextArea(ug.getUpgraderDescription());
		upgradeDescription.setLineWrap(true);
		upgradeDescription.setWrapStyleWord(true);
		
		upgradeB = new JButton("upgrade");
		upgradeB.setEnabled(ug.upgradeAvailable());
		upgradeB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				start();
			}});
	}
	
	protected void go()
	{
		ug.doUpgrade();
		removeAll();
		createUI();
	}

	public boolean needsWinston() {
		return true;
	}
}
