package mars.util;

import java.awt.Toolkit;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import mars.Globals;

public enum IconLoader {
  ALU_CONTROL("ALUcontrol", 0),
  ASSEMBLE_SMALL("Assemble", 16),
  ASSEMBLE_BIG("Assemble", 22),
  CONTROL("control", 0),
  UNDO_BIG("Undo", 22);

  private final String filename;
  private final Integer size;
  private final String extension;

  private final Toolkit tk = Toolkit.getDefaultToolkit();

  IconLoader(String filename, Integer size) {
    this.filename = filename;
    this.size = size;
    this.extension = "png";
  }

  public String filename() {
    return this.filename;
  }

  public Integer size() {
    return this.size;
  }

  public String extension() {
    return this.extension;
  }

  public Icon load() {
    String size_str = "";
    if (this.size() != 0) {
      size_str = this.size().toString();
    }
    return new ImageIcon(
      this.tk.getImage(
        Globals.imagesPath + this.filename() + size_str + "." + this.extension()
      )
    );
  }
}
