package gov.usgs.volcanoes.winston.tools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class FilePanel extends JPanel {

  public static enum Type {
    OPEN, SAVE
  }

  private static final long serialVersionUID = 1L;

  private final JTextField fileF;
  private final JButton fileB;
  private final JFileChooser fc;

  public FilePanel(final Type t) {
    super();

    fc = new JFileChooser();
    fileF = new JTextField(15);
    fileB = new JButton("Browse");
    fileB.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (t == Type.OPEN)
          fc.showOpenDialog(FilePanel.this);
        else
          fc.showSaveDialog(FilePanel.this);
        fileF.setText(fc.getSelectedFile().getAbsolutePath());
      }
    });

    add(fileF);
    add(fileB);
  }

  public String getFileName() {
    return fileF.getText();
  }

  public void setFileName(final String fn) {
    fileF.setText(fn);
  }
}
