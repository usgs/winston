package gov.usgs.winston.tools.pannel;

import gov.usgs.winston.in.metadata.AbstractMetadataImporter;
import gov.usgs.winston.in.metadata.ImportDataless;
import gov.usgs.winston.in.metadata.ImportHypoinverse;
import gov.usgs.winston.tools.FilePanel;
import gov.usgs.winston.tools.WinstonToolsRunnablePanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ImportInstrumentLocations extends WinstonToolsRunnablePanel {

	private static final long serialVersionUID = 1L;
	private static final String CONFIG_FILE = "Winston.config";

	private FilePanel fileP;
	private JButton importB;
	private JComboBox fileType;

	private enum FileType {
		DATALESS_SEED("Dataless SEED", new ImportDataless(CONFIG_FILE)), 
		HYPOINVERSE("Hypoinverse", new ImportHypoinverse(CONFIG_FILE));

		public final String text;
		public final AbstractMetadataImporter importer;

		private FileType(String text, AbstractMetadataImporter importer) {
			this.text = text;
			this.importer = importer;
		}

		public String toString() {
			return text;
		}
		public AbstractMetadataImporter getImporter() {
			return importer;
		}

	}

	public ImportInstrumentLocations() {
		super("Instrument Locations");
	}

	protected void createUI() {
		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
				"Update Instrument Locations"));

		FormLayout layout = new FormLayout("right:max(40dlu;p), 4dlu, left:p", "");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();

		builder.append("Source file", fileP);
		builder.nextLine();
		builder.append("File type", fileType);
		builder.nextLine();
		builder.appendUnrelatedComponentsGapRow();
		builder.nextLine();
		builder.append("", importB);
		this.add(builder.getPanel(), BorderLayout.CENTER);
	}

	protected void createFields() {
		fileP = new FilePanel(FilePanel.Type.OPEN);

		fileType = new JComboBox(FileType.values());
		fileType.setSelectedItem(FileType.HYPOINVERSE);;
		
		importB = new JButton("import metadata");
		importB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				start();
			}
		});

	}

	protected void go() {
		AbstractMetadataImporter importer = ((FileType) fileType.getSelectedItem()).getImporter();
		importer.updateInstruments(fileP.getFileName());
	}

	public boolean needsWinston() {
		return true;
	}

}
