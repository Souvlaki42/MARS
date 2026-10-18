/*
Copyright (c) 2003-2006,  Pete Sanderson and Kenneth Vollmar

Developed by Pete Sanderson (psanderson@otterbein.edu)
and Kenneth Vollmar (kenvollmar@missouristate.edu)

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject
to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

(MIT license, http://www.opensource.org/licenses/mit-license.html)
 */

/**
 * Portal to Mars
 *
 * @author Pete Sanderson
 * @version March 2006
 */
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.jthemedetecor.OsThemeDetector;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import mars.Globals;
import mars.Settings;

public class Mars {

  public static void main(String[] args) {
    // Java fixes the interface scale when graphics start up, so this has to come first.
    String uiScale = Settings.getSavedUIScale();
    if (System.getProperty("sun.java2d.uiScale") != null) {
      Globals.uiScaleFromCommandLine = true;
    } else if (!uiScale.isEmpty()) {
      System.setProperty("sun.java2d.uiScale", uiScale);
    }

    FlatInterFont.install();
    FlatJetBrainsMonoFont.install();
    FlatLaf.setPreferredFontFamily(FlatInterFont.FAMILY);
    FlatLaf.setPreferredLightFontFamily(FlatInterFont.FAMILY_LIGHT);
    FlatLaf.setPreferredSemiboldFontFamily(FlatInterFont.FAMILY_SEMIBOLD);
    FlatLaf.setPreferredMonospacedFontFamily(FlatJetBrainsMonoFont.FAMILY);

    UIManager.put("SplitPaneDivider.style", "plain");
    UIManager.put("ScrollBar.thumbArc", 999);
    UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
    // Hide the Java coffee cup on the Execute tab's internal windows.
    UIManager.put(
        "InternalFrame.icon", new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)));

    final OsThemeDetector detector = OsThemeDetector.getDetector();
    Globals.isDark = detector.isDark();
    FlatLaf.setup(theme(Globals.isDark));

    detector.registerListener(
        isDark -> {
          SwingUtilities.invokeLater(
              () -> {
                try {
                  Globals.isDark = isDark;
                  UIManager.setLookAndFeel(theme(isDark));
                  FlatLaf.updateUI();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              });
        });

    new mars.MarsLaunch(args);
  }

  private static FlatLaf theme(boolean dark) {
    return dark ? new FlatMacDarkLaf() : new FlatMacLightLaf();
  }
}
