package gov.usgs.volcanoes.winston.tools.pannel;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.winston.in.metadata.AbstractMetadataImporter;
import gov.usgs.volcanoes.winston.in.metadata.ImportDataless;
import gov.usgs.volcanoes.winston.in.metadata.ImportHypoinverse;
import gov.usgs.volcanoes.winston.tools.FilePanel;
import gov.usgs.volcanoes.winston.tools.WinstonToolsRunnablePanel;

public class ImportInstrumentLocations extends WinstonToolsRunnablePanel {

  private static final long serialVersionUID = 1L;
  private static final String CONFIG_FILE = "Winston.config";

  private FilePanel fileP;
  private JButton importB;
  private JComboBox fileType;

  private enum FileType {
    DATALESS_SEED("Dataless SEED", new ImportDataless(CONFIG_FILE)), HYPOINVERSE("Hypoinverse",
        new ImportHypoinverse(CONFIG_FILE));

    public final String text;
    public final AbstractMetadataImporter importer;

    private FileType(final String text, final AbstractMetadataImporter importer) {
      this.text = text;
      this.importer = importer;
    }

    @Override
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

  @Override
  protected void createUI() {
    this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
        "Update Instrument Locations"));

    final FormLayout layout = new FormLayout("right:max(40dlu;p), 4dlu, left:p", "");

    final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
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

  @Override
  protected void createFields() {
    fileP = new FilePanel(FilePanel.Type.OPEN);

    fileType = new JComboBox(FileType.values());
    fileType.setSelectedItem(FileType.HYPOINVERSE);;

    importB = new JButton("import metadata");
    importB.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        start();
      }
    });

  }

  @Override
  protected void go() {
    final AbstractMetadataImporter importer = ((FileType) fileType.getSelectedItem()).getImporter();
    importer.updateInstruments(fileP.getFileName());
  }

  @Override
  public boolean needsWinston() {
    return true;
  }

}
