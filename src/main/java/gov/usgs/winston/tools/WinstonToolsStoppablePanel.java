package gov.usgs.winston.tools;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public abstract class WinstonToolsStoppablePanel extends WinstonToolsRunnablePanel {
	
	private static final long serialVersionUID = 1L;
	public abstract void stop();

	public WinstonToolsStoppablePanel(String s) {
		super(s);
		
		TermIO.getTerm().addStopListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				stop();
			}});
	}
}
