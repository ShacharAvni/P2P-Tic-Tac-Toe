/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player;

import java.util.concurrent.CountDownLatch;

import player.networking.Receiver;
import player.networking.node.PlayerNode;
import player.ui.GUI;

/*
 * The Main class describes the entry point of the
 * Tic-Tac-Toe Player program.
 */

public final class Main
{
   /*
    * Player program entry point
    */
   public static void main(String[] args)
   {
      // 1. Instantiate the PlayerNode (main network node),
      // the Receiver, and GUI.

      PlayerNode playerNode = new PlayerNode();
      Receiver receiver = new Receiver(playerNode);

      // This latch is counted down when the GUI is closed
      CountDownLatch guiClosedLatch = new CountDownLatch(1);

      GUI gui = new GUI("Tic Tac Toe", guiClosedLatch, playerNode);
      gui.setDefaultCloseOperation(GUI.DISPOSE_ON_CLOSE);

      gui.toFront();

      gui.addMessage("Listening at " + receiver.listeningURL());

      // 2. Attach the Receiver and GUI to the PlayerNode
      playerNode.begin(receiver, gui);

      // 3. Wait for the user to close the GUI. At this point, the main
      // thread sleeps. Two threads compete for the PlayerNode's
      // attention (the GUI thread through the user's actions, and
      // the Receiver thread through incoming Messages).
      try
      {
         guiClosedLatch.await();
      }
      catch (InterruptedException e)
      {
         // the program will finish at this point anyway. Just log the error.
         Logger.logError("Unexpected Thread Interruption", e);
         Thread.currentThread().interrupt();
      }

      // 4. When the user closes the GUI, the PlayerNode is terminated
      // and the program is finished.
      playerNode.quit();
   }
}