package gov.usgs.volcanoes.winston.tools.pannel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.core.legacy.Arguments;
import gov.usgs.volcanoes.winston.in.ImportSAC;
import gov.usgs.volcanoes.winston.in.StaticImporter;
import gov.usgs.volcanoes.winston.tools.FilePanel;
import gov.usgs.volcanoes.winston.tools.ScnlPanel;
import gov.usgs.volcanoes.winston.tools.WinstonToolsRunnablePanel;

public class ImportSACPanel extends WinstonToolsRunnablePanel {

  private static final long serialVersionUID = 1L;
  private ImportSAC is;

  private ScnlPanel scnlP;
  private FilePanel fileP;
  private JTextField rsamDeltaF;
  private JTextField rsamDurationF;
  private JButton importB;

  public ImportSACPanel() {
    super("Import SAC");
  }

  @Override
  protected void createUI() {
    this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
        "Import SAC File"));

    final FormLayout layout = new FormLayout("right:max(40dlu;p), 4dlu, left:p", "");

    final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();

    builder.append("SCNL", scnlP);
    builder.nextLine();
    builder.appendUnrelatedComponentsGapRow();
    builder.nextLine();
    builder.append("file", fileP);
    builder.nextLine();
    builder.appendUnrelatedComponentsGapRow();
    builder.nextLine();
    builder.append("RSAM Delta", rsamDeltaF);
    builder.nextLine();
    builder.append("RSAM Duration", rsamDurationF);
    builder.nextLine();
    builder.appendUnrelatedComponentsGapRow();
    builder.nextLine();
    builder.append("", importB);
    this.add(builder.getPanel(), BorderLayout.CENTER);
  }

  @Override
  protected void createFields() {
    scnlP = new ScnlPanel();

    fileP = new FilePanel(FilePanel.Type.OPEN);
    rsamDeltaF = new JTextField(5);
    rsamDeltaF.setText("10");
    rsamDurationF = new JTextField(5);
    rsamDurationF.setText("60");
    importB = new JButton("import");
    importB.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        start();
      }
    });
  }

  @Override
  protected void go() {

    final ArrayList<String> as = new ArrayList<String>();
    as.add("-rd");
    as.add(rsamDeltaF.getText());
    as.add("-rl");
    as.add(rsamDurationF.getText());
    as.add(fileP.getFileName());
    final Set<String> kvs = new HashSet<String>();
    kvs.add("-rd");
    kvs.add("-rl");
    kvs.add("-c");

    final Set<String> flags = new HashSet<String>();
    final Arguments args = new Arguments(as.toArray(new String[0]), flags, kvs);
    is = new ImportSAC();
    is.processArguments(args);
    is.setChannel(scnlP.getSCNL('$'));
    final List<String> files = args.unused();
    StaticImporter.process(files, is);
  }

  @Override
  public boolean needsWinston() {
    return true;
  }

}
