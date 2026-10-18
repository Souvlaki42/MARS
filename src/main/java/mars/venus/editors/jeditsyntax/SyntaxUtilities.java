/*
 * SyntaxUtilities.java - Utility functions used by syntax colorizing
 * Copyright (C) 1999 Slava Pestov
 *
 * You may use and modify this package for any purpose. Redistribution is
 * permitted, in both source and binary form, provided that this notice
 * remains intact in all source distributions of this package.
 */

package mars.venus.editors.jeditsyntax;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import mars.Globals;
import mars.venus.editors.jeditsyntax.tokenmarker.*;

/**
 * Class with several utility functions used by jEdit's syntax colorizing subsystem.
 *
 * @author Slava Pestov
 * @version $Id: SyntaxUtilities.java,v 1.9 1999/12/13 03:40:30 sp Exp $
 */
public class SyntaxUtilities {
  /**
   * Checks if a subregion of a <code>Segment</code> is equal to a string.
   *
   * @param ignoreCase True if case should be ignored, false otherwise
   * @param text The segment
   * @param offset The offset into the segment
   * @param match The string to match
   */
  public static boolean regionMatches(boolean ignoreCase, Segment text, int offset, String match) {
    int length = offset + match.length();
    char[] textArray = text.array;
    if (length > text.offset + text.count) return false;
    for (int i = offset, j = 0; i < length; i++, j++) {
      char c1 = textArray[i];
      char c2 = match.charAt(j);
      if (ignoreCase) {
        c1 = Character.toUpperCase(c1);
        c2 = Character.toUpperCase(c2);
      }
      if (c1 != c2) return false;
    }
    return true;
  }

  /**
   * Checks if a subregion of a <code>Segment</code> is equal to a character array.
   *
   * @param ignoreCase True if case should be ignored, false otherwise
   * @param text The segment
   * @param offset The offset into the segment
   * @param match The character array to match
   */
  public static boolean regionMatches(boolean ignoreCase, Segment text, int offset, char[] match) {
    int length = offset + match.length;
    char[] textArray = text.array;
    if (length > text.offset + text.count) return false;
    for (int i = offset, j = 0; i < length; i++, j++) {
      char c1 = textArray[i];
      char c2 = match[j];
      if (ignoreCase) {
        c1 = Character.toUpperCase(c1);
        c2 = Character.toUpperCase(c2);
      }
      if (c1 != c2) return false;
    }
    return true;
  }

  /**
   * Returns the default style table. This can be passed to the <code>setStyles()</code> method of
   * <code>SyntaxDocument</code> to use the default syntax styles.
   */
  public static SyntaxStyle[] getDefaultSyntaxStyles() {
    SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

    // SyntaxStyle constructor params: color, italic?, bold?
    // All need to be assigned even if not used by language (no gaps in array)
    styles[Token.NULL] =
        new SyntaxStyle(Globals.isDark ? new Color(0x000000) : new Color(0x000000), false, false);
    styles[Token.COMMENT1] =
        new SyntaxStyle(Globals.isDark ? new Color(0x7f848e) : new Color(0x8c8c8c), true, false);
    styles[Token.COMMENT2] =
        new SyntaxStyle(Globals.isDark ? new Color(0xbe5046) : new Color(0x990033), true, false);
    styles[Token.KEYWORD1] =
        new SyntaxStyle(Globals.isDark ? new Color(0x61afef) : new Color(0x0033b3), false, false);
    styles[Token.KEYWORD2] =
        new SyntaxStyle(Globals.isDark ? new Color(0xc678dd) : new Color(0x871094), false, false);
    styles[Token.KEYWORD3] =
        new SyntaxStyle(Globals.isDark ? new Color(0xe06c75) : new Color(0xc4302b), false, false);
    styles[Token.LITERAL1] =
        new SyntaxStyle(Globals.isDark ? new Color(0x98c379) : new Color(0x067d17), false, false);
    styles[Token.LITERAL2] =
        new SyntaxStyle(Globals.isDark ? new Color(0x98c379) : new Color(0x067d17), false, false);
    styles[Token.LABEL] =
        new SyntaxStyle(Globals.isDark ? new Color(0xe5c07b) : new Color(0x00627a), false, false);
    styles[Token.OPERATOR] =
        new SyntaxStyle(Globals.isDark ? new Color(0x000000) : new Color(0x000000), false, true);
    styles[Token.INVALID] =
        new SyntaxStyle(Globals.isDark ? new Color(0xf44747) : new Color(0xff0000), false, false);
    styles[Token.MACRO_ARG] =
        new SyntaxStyle(Globals.isDark ? new Color(0xd19a66) : new Color(0x9e880d), false, false);
    return styles;
  }

  /**
   * Returns the CURRENT style table. This can be passed to the <code>setStyles()</code> method of
   * <code>SyntaxDocument</code> to use the current syntax styles. If changes have been made via
   * MARS Settings menu, the current settings will not be the same as the default settings.
   */
  public static SyntaxStyle[] getCurrentSyntaxStyles() {
    SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

    styles[Token.NULL] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.NULL);
    styles[Token.COMMENT1] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.COMMENT1);
    styles[Token.COMMENT2] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.COMMENT2);
    styles[Token.KEYWORD1] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.KEYWORD1);
    styles[Token.KEYWORD2] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.KEYWORD2);
    styles[Token.KEYWORD3] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.KEYWORD3);
    styles[Token.LITERAL1] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.LITERAL1);
    styles[Token.LITERAL2] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.LITERAL2);
    styles[Token.LABEL] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.LABEL);
    styles[Token.OPERATOR] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.OPERATOR);
    styles[Token.INVALID] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.INVALID);
    styles[Token.MACRO_ARG] = Globals.getSettings().getEditorSyntaxStyleByPosition(Token.MACRO_ARG);
    return styles;
  }

  public static boolean popupShowing = false;
  public static Popup popup;

  /**
   * Paints the specified line onto the graphics context. Note that this method munges the offset
   * and count values of the segment.
   *
   * @param line The line segment
   * @param tokens The token list for the line
   * @param styles The syntax style list
   * @param expander The tab expander used to determine tab stops. May be null
   * @param gfx The graphics context
   * @param x The x co-ordinate
   * @param y The y co-ordinate
   * @return The x co-ordinate, plus the width of the painted string
   */
  public static int paintSyntaxLine(
      Segment line,
      Token tokens,
      SyntaxStyle[] styles,
      TabExpander expander,
      Graphics gfx,
      int x,
      int y) {
    Font defaultFont = gfx.getFont();
    Color defaultColor = gfx.getColor();

    int offset = 0;
    for (; ; ) {
      byte id = tokens.id;
      if (id == Token.END) break;

      int length = tokens.length;
      if (id == Token.NULL) {
        if (!defaultColor.equals(gfx.getColor())) gfx.setColor(defaultColor);
        if (!defaultFont.equals(gfx.getFont())) gfx.setFont(defaultFont);
      } else styles[id].setGraphicsFlags(gfx, defaultFont);
      line.count = length;

      if (id == Token.KEYWORD1) {
        // System.out.println("Instruction: "+line);
        if (!popupShowing) { // System.out.println("creating popup");
          //                   JComponent paintArea = (JComponent) expander;
          //                   JToolTip tip = paintArea.createToolTip();
          //                   tip.setTipText("Instruction: "+line);
          //                   Point screenLocation = paintArea.getLocationOnScreen();
          //                   PopupFactory popupFactory = PopupFactory.getSharedInstance();
          //                   popup = popupFactory.getPopup(paintArea, tip, screenLocation.x + x,
          // screenLocation.y + y);
          //                   popupShowing = true;
          //                   popup.show();
          //                   int delay = 200; //milliseconds
          //                   ActionListener taskPerformer =
          //                       new ActionListener() {
          //                          public void actionPerformed(ActionEvent evt) {
          //                            //popupShowing = false;
          //                            if (popup!= null) {
          //                               popup.hide();
          //                            }
          //                         }
          //                      };
          //                   Timer popupTimer = new Timer(delay, taskPerformer);
          //                   popupTimer.setRepeats(false);
          //                   popupTimer.start();

        }

        // ToolTipManager.sharedInstance().mouseMoved(
        //	   new MouseEvent((Component)expander, MouseEvent.MOUSE_MOVED, new
        // java.util.Date().getTime(), 0, x, y, 0, false));
        //    new InstructionMouseEvent((Component)expander, x, y, line));
      }

      x = Utilities.drawTabbedText(line, x, y, gfx, expander, 0);
      line.offset += length;
      offset += length;

      tokens = tokens.next;
    }

    return x;
  }

  // private members
  private SyntaxUtilities() {}
}

class InstructionMouseEvent extends MouseEvent {
  private Segment line;

  public InstructionMouseEvent(Component component, int x, int y, Segment line) {
    super(component, MouseEvent.MOUSE_MOVED, new java.util.Date().getTime(), 0, x, y, 0, false);
    System.out.println("Create InstructionMouseEvent " + x + " " + y + " " + line);
    this.line = line;
  }

  public Segment getLine() {
    return this.line;
  }
}
