package gov.usgs.volcanoes.winston.tools.pannel;


import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import gov.usgs.volcanoes.winston.db.Upgrade;
import gov.usgs.volcanoes.winston.tools.WinstonToolsRunnablePanel;

public class WinstonUpgradePanel extends WinstonToolsRunnablePanel {

  private static final long serialVersionUID = 1L;
  Upgrade ug;
  JTextField currentVersion;
  JTextArea upgradeDescription;
  JButton upgradeB;

  public WinstonUpgradePanel() {
    super("Winston Upgrade");
  }

  @Override
  protected void createUI() {


    this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black),
        "Upgrade Winston Schema"));
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    final FormLayout layout = new FormLayout("left:p, 4dlu, fill:p:G", "");

    final DefaultFormBuilder builder = new DefaultFormBuilder(layout);
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

  @Override
  protected void createFields() {
    ug = new Upgrade();

    currentVersion = new JTextField(ug.getCurrentVersion());

    upgradeDescription = new JTextArea(ug.getUpgraderDescription());
    upgradeDescription.setLineWrap(true);
    upgradeDescription.setWrapStyleWord(true);

    upgradeB = new JButton("upgrade");
    upgradeB.setEnabled(ug.upgradeAvailable());
    upgradeB.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        start();
      }
    });
  }

  @Override
  protected void go() {
    ug.doUpgrade();
    removeAll();
    createUI();
  }

  @Override
  public boolean needsWinston() {
    return true;
  }
}
