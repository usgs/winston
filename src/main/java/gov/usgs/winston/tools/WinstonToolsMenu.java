package gov.usgs.winston.tools;



import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class WinstonToolsMenu extends JMenuBar {
	private static final long serialVersionUID = 1L;
	private JMenu fileMenu;
	private JMenu helpMenu;
	
	public WinstonToolsMenu()
	{
		super();
		createFileMenu();
		add(Box.createGlue());
		createHelpMenu();
	}
	
	public void addHelpItem(final WinstonToolsPanel p)
	{
		JMenuItem i = new JMenuItem(p.getTitle());
		i.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JDialog d = new WinstonToolsHelpDialog(p);
				d.setVisible(true);
			}});
		
//		helpMenu.add(i);
		helpMenu.add(i, helpMenu.getItemCount()-2);
	}
	
	private void createHelpMenu()
	{
		helpMenu = new JMenu("Help");
		fileMenu.setMnemonic('H');

		helpMenu.addSeparator();
		JMenuItem i = new JMenuItem("About Winston Tools");
		i.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				JDialog d = new WinstonToolsAboutDialog();
				d.setVisible(true);
			}});
		helpMenu.add(i);
		this.add(helpMenu);
	}
	private void createFileMenu()
	{
		fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		
		JMenuItem term = new JMenuItem("Show Console");
		term.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				TermIO.showTerm(true);
			}
		});
		fileMenu.add(term);

		JMenuItem exit = new JMenuItem("Exit");
		exit.setMnemonic('x');
		exit.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						System.exit(0);
					}
				});
		fileMenu.add(exit);

		add(fileMenu);
	}
}
