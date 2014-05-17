/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player.ui;

import javax.swing.*;
import java.awt.*;

/*
 * An ImagePanel is a JPanel that has an image for its background.
 * The GUI (described in GUI.java) uses this for the background of
 * the main window.
 */

public final class ImagePanel extends JPanel
{
   private static final long serialVersionUID = 1L;

   //relative file location of the image
   private String imageLocation;

   /*
    * ImagePanel Constructor
    */
   public ImagePanel(String il)
   {
      super();
      imageLocation = il;
   }

   /*
    * Draws the image stored in the file imageLocation
    * as the background of the Panel.
    */
   protected void paintComponent(Graphics g)
   {
      ImageIcon icon = new ImageIcon(imageLocation);

      //scale image to the size of the panel
      g.drawImage(icon.getImage(), 0, 0, getSize().width, getSize().height, null);

      super.paintComponent(g);
   }
}