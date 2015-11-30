package gov.usgs.winston.tools;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;

public class WinstonToolsHelpDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	
	JButton dismiss;
	JPanel helpPanel;
	
	
	public WinstonToolsHelpDialog(WinstonToolsPanel p)
	{
		super();
		
        JScrollPane scrollPane = new JScrollPane(getHelpPane(p.getHelpFileName()));
        scrollPane.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(500, 500));

		this.add(scrollPane, BorderLayout.NORTH);
		dismiss = new JButton("Close");
		dismiss.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				dispose();
			}});
		
		this.add(dismiss, BorderLayout.SOUTH);
		pack();
	}

	private JEditorPane getHelpPane(String s) {
		JEditorPane editorPane;
//		s = "file:gov/usgs/winston/tools/" + s + "Help.html";
		try {
			editorPane = new JEditorPane(s);
		} catch (IOException e) {
			editorPane = new JEditorPane();
			try {
				editorPane.getDocument().insertString(0, "No help available for " + s, null);
//				editorPane.getDocument().insertString(0, e.getMessage() + s, null);
			} catch (BadLocationException e1) {}
		}
		
		return editorPane;
	}
}
