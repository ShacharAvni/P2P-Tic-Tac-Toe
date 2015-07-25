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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import player.networking.node.PlayerNode;

/*
 * The GUI class represents the interface to the user. It maintains the main window
 * including the menus, and it is responsible for placement of the Tic-Tac-Toe game
 * board, the list of players connected to the Registry, and the message log.
 */

/*
 * The methods of the GUI class that show modal dialogs are blocking calls. Care has been
 * taken to ensure that these methods are not called when the PlayerNode's ConnectionState
 * has been acquired. This is so these dialogs don't inadvertently block Messages from
 * being received. This also makes it so calling these methods is rather awkward in certain
 * places (for instance, see Invitation.receive() and PlayerNode.tryConnectToPlayer()).
 * Sometimes these methods are even called in separate threads (for instance, see
 * PlayerNode.tryQuit()). All this is because of the fact that there is only one game
 * board and thus the player's connection state needs to be synchronized to ensure that
 * they are playing with only one player at a time. If multiple game boards were allowed,
 * then the player's connection state wouldn't have to be synchronized and thus this wouldn't
 * be an issue.
 */

public final class GUI extends JFrame implements ActionListener
{
   private static final long serialVersionUID = 1L;

   // User requests are forwarded to the PlayerNode (see: PlayerNode.java)
   private PlayerNode playerNode;

   // The Tic-Tac-Toe game Board (see: Board.java). The GUI's responsibility is to place
   // it in the main window.
   private player.game.Board gameBoard;

   // This mutex is so no synchronization errors occur when accessing various GUI elements
   private Semaphore guiMutex;

   // This latch is counted down to zero when the GUI is closed. The main thread waits
   // for this event to occur before program termination
   private CountDownLatch guiClosedLatch;

   // window dimensions and placement constants
   private final int WIDTH = 800;
   private final int HEIGHT = 675;
   private final int INITIAL_LOC_X = 50;
   private final int INITIAL_LOC_Y = 50;

   // the list of players currently connected to the Registry
   private JList<String> playerList;

   // the player presses this button when they want to play with one of the players
   // listed in the above list
   private JButton playButton;

   // this text area stores a log of messages. It gets updated, for example, when a player
   // joins or leaves the Registry or when a match begins or ends
   private JTextArea messageLog;

   // all GUI objects (except the menus, naturally) get placed on this panel (See: ImagePanel.java)
   private ImagePanel backgroundPanel;

   // this label shows whether the player is currently connected to the Registry by means of an image
   private JLabel onlineLabel;

   // is the player allowed to send connection requests?
   private boolean invitationsAllowed;

   // menu items
   private JMenuItem registryConnectItem;
   private JMenuItem registryDisconnectItem;
   private JMenuItem quitItem;
   private JMenuItem xItem;
   private JMenuItem oItem;
   private JMenuItem connectDirectlyToPlayerItem;
   private JMenuItem cancelInvitationItem;
   private JMenuItem forfeitItem;

   // online status shown at the top of the player list
   private ImageIcon offlineImageIcon;
   private ImageIcon onlineImageIcon;

   /*
    * This enum is used when other objects call the GUI methods which show
    * dialogs. It is a wrapper around the JOptionPane enum which selects the
    * icon shown in a dialog.
    */
   public enum MessageType
   {
      ERROR, INFORMATION, QUESTION, PLAIN;
   }

   /*
    * GUI Constructor. It places all the menus and GUI objects on the main panel
    * into their respective locations, as well as sets up the action listeners
    * for the appropriate objects.
    */
   public GUI(String aName, CountDownLatch latch, PlayerNode p)
   {
      super(aName);

      guiClosedLatch = latch;
      playerNode = p;

      guiMutex = new Semaphore(1);

      //////////////////// Menu////////////////////////

      // individual menu items
      registryConnectItem = new JMenuItem("connect...");
      registryDisconnectItem = new JMenuItem("disconnect...");
      quitItem = new JMenuItem("Exit");
      xItem = new JMenuItem("X");
      oItem = new JMenuItem("O");
      connectDirectlyToPlayerItem = new JMenuItem("Send Invitation");
      cancelInvitationItem = new JMenuItem("Cancel Invitation");
      forfeitItem = new JMenuItem("Forfeit");

      // "File" menu
      // Has and option for quitting the game
      JMenu file = new JMenu("File"); // File -> Exit
      file.add(quitItem);

      // "Registry" menu
      // Allows the player to connect or disconnect from the Registry
      JMenu registry = new JMenu("Registry");
      registry.add(registryConnectItem); // Registry -> connect...
      registry.add(registryDisconnectItem); // Registry -> disconnect...

      // initially aren't connected to the registry
      registryDisconnectItem.setEnabled(false);

      // "Weapon" menu
      // Allows the player to change their desired Tic-Tac-Toe piece back-and-forth
      // between "X" and "O". If the player changes their piece, the opponent's board
      // does not get changed. This resolves any potential disputes between players
      // centered around "Who is X"
      JMenu weapon = new JMenu("Weapon");
      weapon.add(xItem); // Weapon -> X
      weapon.add(oItem); // Weapon -> O

      // "Game" menu
      // Has an option for connecting to a player directly (knowing their
      // IP Address and port), an option for cancelling that request,
      // and an option to forfeit the game.
      JMenu game = new JMenu("Game");
      game.add(connectDirectlyToPlayerItem); // Game -> Send Invitation
      game.add(cancelInvitationItem); // Game -> Cancel Invitation
      game.add(forfeitItem); // Game -> Forfeit

      // initially haven't sent a connection request
      cancelInvitationItem.setEnabled(false);

      // initially aren't playing a game
      forfeitItem.setEnabled(false);

      // add the menu to the GUI
      JMenuBar mainMenu = new JMenuBar();
      mainMenu.add(file);
      mainMenu.add(registry);
      mainMenu.add(weapon);
      mainMenu.add(game);
      setJMenuBar(mainMenu);

      // setup Action Listeners for the menu items. See the "actionPerformed" method of this class
      quitItem.addActionListener(this);
      registryConnectItem.addActionListener(this);
      registryDisconnectItem.addActionListener(this);
      xItem.addActionListener(this);
      oItem.addActionListener(this);
      connectDirectlyToPlayerItem.addActionListener(this);
      cancelInvitationItem.addActionListener(this);
      forfeitItem.addActionListener(this);

      /////////////////// Main UI//////////////////////

      playerList = new JList<String>();

      messageLog = new JTextArea();

      gameBoard = new player.game.Board(p);

      playButton = new JButton("Play");

      offlineImageIcon = new ImageIcon(player.FileSystem.getExecutingDirectory(GUI.class) + "offline.jpg");
      onlineImageIcon = new ImageIcon(player.FileSystem.getExecutingDirectory(GUI.class) + "online.jpg");

      GridBagLayout layout = new GridBagLayout();
      GridBagConstraints layoutConstraints = new GridBagConstraints();

      // create the background panel. All main GUI objects get placed on this panel
      backgroundPanel = new ImagePanel(player.FileSystem.getExecutingDirectory(GUI.class) + "background.jpg");

      backgroundPanel.setLayout(layout);
      backgroundPanel.setOpaque(false);

      // place the Tic-Tac-Toe board on the GUI (this is placed roughly on the
      // top-left)
      layoutConstraints.gridx = 0;
      layoutConstraints.gridy = 0;
      layoutConstraints.gridwidth = 10;
      layoutConstraints.gridheight = 10;
      layoutConstraints.weightx = 100;
      layoutConstraints.weighty = 1;
      layoutConstraints.fill = GridBagConstraints.BOTH;
      layoutConstraints.anchor = GridBagConstraints.NORTHEAST;
      layout.setConstraints(gameBoard, layoutConstraints);
      backgroundPanel.add(gameBoard, layoutConstraints);

      // this spacer is placed on the GUI for alignment purposes
      JLabel spacer = new JLabel(" ");

      layoutConstraints.gridx = 10;
      layoutConstraints.gridy = 0;
      layoutConstraints.gridwidth = 3;
      layoutConstraints.gridheight = 10;
      layoutConstraints.weightx = 1;
      layoutConstraints.weighty = 1;
      layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
      layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
      layout.setConstraints(spacer, layoutConstraints);
      backgroundPanel.add(spacer, layoutConstraints);

      // place an image (showing the word "Online") on the GUI. This
      // image gets placed directly above the player list. (this is
      // placed roughly on the top-right)
      onlineLabel = new JLabel();

      layoutConstraints.gridx = 13;
      layoutConstraints.gridy = 0;
      layoutConstraints.gridwidth = 1;
      layoutConstraints.gridheight = 1;
      layoutConstraints.weightx = 1.0;
      layoutConstraints.weighty = 0.0;
      layoutConstraints.fill = GridBagConstraints.VERTICAL;
      layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
      layout.setConstraints(onlineLabel, layoutConstraints);
      backgroundPanel.add(onlineLabel, layoutConstraints);

      showRegistryConnection(false); // have the onlineLabel show that the user is offline

      // place the player list on the GUI. (this is placed roughly on
      // the right)
      layoutConstraints.gridx = 13;
      layoutConstraints.gridy = 1;
      layoutConstraints.gridwidth = 3;
      layoutConstraints.gridheight = 8;
      layoutConstraints.weightx = 0.0;
      layoutConstraints.weighty = 1;
      layoutConstraints.fill = GridBagConstraints.BOTH;
      layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
      layout.setConstraints(playerList, layoutConstraints);
      backgroundPanel.add(playerList, layoutConstraints);

      // add a scroll bar to the player list
      JScrollPane playerListScroll = new JScrollPane(playerList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      layout.setConstraints(playerListScroll, layoutConstraints);
      backgroundPanel.add(playerListScroll, layoutConstraints);

      // place the play button on the GUI. This is placed directly below
      // the player list
      layoutConstraints.gridx = 13;
      layoutConstraints.gridy = 9;
      layoutConstraints.gridwidth = 1;
      layoutConstraints.gridheight = 1;
      layoutConstraints.weightx = 0.1;
      layoutConstraints.weighty = 1;
      layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
      layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
      layout.setConstraints(playButton, layoutConstraints);
      backgroundPanel.add(playButton, layoutConstraints);

      // this spacer is placed on the GUI for alignment purposes
      JLabel spacer2 = new JLabel(" ");

      layoutConstraints.gridx = 0;
      layoutConstraints.gridy = 10;
      layoutConstraints.gridwidth = 15;
      layoutConstraints.gridheight = 2;
      layoutConstraints.weightx = 1;
      layoutConstraints.weighty = 0.1;
      layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
      layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
      layout.setConstraints(spacer2, layoutConstraints);
      backgroundPanel.add(spacer2, layoutConstraints);

      // place an image (showing the word "Messages") on the GUI. This
      // image gets placed directly above the message log text area, which
      // is at the bottom of the panel.
      JLabel messagesLabel = new JLabel(new ImageIcon(player.FileSystem.getExecutingDirectory(GUI.class) + "messages.jpg"));

      layoutConstraints.gridx = 0;
      layoutConstraints.gridy = 12;
      layoutConstraints.gridwidth = 1;
      layoutConstraints.gridheight = 1;
      layoutConstraints.weightx = 0.0;
      layoutConstraints.weighty = 0.0;
      layoutConstraints.fill = GridBagConstraints.HORIZONTAL;
      layoutConstraints.anchor = GridBagConstraints.NORTHWEST;
      layout.setConstraints(messagesLabel, layoutConstraints);
      backgroundPanel.add(messagesLabel, layoutConstraints);

      // place the message log text area on the GUI. (this is placed
      // roughly at the bottom of the panel)
      messageLog.setEditable(false);
      messageLog.setRows(3); // this fixes the messageLog's height
      layoutConstraints.gridx = 0;
      layoutConstraints.gridy = 13;
      layoutConstraints.gridwidth = 15;
      layoutConstraints.gridheight = 5;
      layoutConstraints.weightx = 1;
      layoutConstraints.weighty = 1;
      layoutConstraints.fill = GridBagConstraints.BOTH;
      layoutConstraints.anchor = GridBagConstraints.SOUTHWEST;
      layout.setConstraints(messageLog, layoutConstraints);
      backgroundPanel.add(messageLog, layoutConstraints);

      // add a scroll bar to the message log text area
      JScrollPane messageAreaScroll = new JScrollPane(messageLog, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      layout.setConstraints(messageAreaScroll, layoutConstraints);
      backgroundPanel.add(messageAreaScroll, layoutConstraints);

      getContentPane().add(backgroundPanel);

      // setup Action Listeners for the main GUI objects. See the "actionPerformed" method of this class

      playButton.addActionListener(this);

      // other setup for main GUI objects

      playButton.setEnabled(false); // no players in the players list yet

      ////////////////////////////////////////////

      invitationsAllowed = true; // PlayerNode has not sent an Invitation yet

      gameBoard.drawBoard();

      addMessage("Administration says: You are not connected to the player registry. If you don't know the player's connection information, please connect\nto the registry at this time.");

      // setup the window listener. This is needed because the "x" button doesn't do anything
      // by default.
      addWindowListener(new WindowAdapter()
      {
         @Override
         public void windowClosing(WindowEvent e)
         {
            playerNode.tryQuit();
         }
      });

      // initialize the size and location of the GUI
      setSize(WIDTH, HEIGHT);
      setLocation(INITIAL_LOC_X, INITIAL_LOC_Y);
      setResizable(false);
      setVisible(true);
   }

   /*
    * Closes the latch so that the main thread can start the tear down process.
    */
   public void quit()
   {
      guiClosedLatch.countDown();
   }

   /*
    * Attempts to acquire the GUI's internal mutexes.
    */
   private void acquireMutex()
   {
      try
      {
         guiMutex.acquire();
      }
      catch (InterruptedException e)
      {
         player.Logger.fatalError("Unexpected Thread Interruption.", e);
      }
   }

   /*
    * Allows the player to send Invitation Messages (see: Invitation.java), if canInvite
    * is true, or nullifies their ability otherwise. This method is called by the PlayerNode.
    */
   public void allowInvitations(boolean canInvite)
   {
      acquireMutex();

      try
      {
         invitationsAllowed = canInvite;

         connectDirectlyToPlayerItem.setEnabled(invitationsAllowed);
         cancelInvitationItem.setEnabled(!invitationsAllowed);
         playButton.setEnabled(invitationsAllowed && (playerList.getModel().getSize() > 0));
      }
      finally
      {
         guiMutex.release();
      }
   }

   /*
    * Allows the player to connect to the Registry if canConnect is true or nullifies their
    * ability otherwise. This method is called by the PlayerNode.
    */
   public void allowRegistryConnect(boolean canConnect)
   {
      acquireMutex();

      try
      {
         registryConnectItem.setEnabled(canConnect);
      }
      finally
      {
         guiMutex.release();
      }
   }

   /*
    * Allows the player to disconnect from the Registry if canDisconnect is true or nullifies
    * their ability otherwise. This method is called by the PlayerNode.
    */
   public void allowRegistryDisconnect(boolean canDisconnect)
   {
      acquireMutex();

      try
      {
         registryDisconnectItem.setEnabled(canDisconnect);
      }
      finally
      {
         guiMutex.release();
      }
   }

   /*
    * Allows the player to forfeit a match if canForfeit is true or nullifies
    * their ability otherwise. This method is called by the PlayerNode.
    */
   public void allowForfeits(boolean canForfeit)
   {
      acquireMutex();

      try
      {
         forfeitItem.setEnabled(canForfeit);

         // We take advantage here that allowForfeits is only called at the beginning or end of a game.
         // If we allow forfeits, then a game is started, so the user can't cancel an invitation. At
         // that point, the user can't send or cancel an invitation. If we disallow forfeits, then a
         // game is ended, in which case invitations are allowed but we can't cancel an invitation
         // because one hasn't been sent yet.
         cancelInvitationItem.setEnabled(false);
      }
      finally
      {
         guiMutex.release();
      }
   }

   /*
    * Allows the player to send Moves if canSendMoves is true or nullifies their ability otherwise.
    * This method is called by the PlayerNode. The PlayerNode enforces that only one thread at a time
    * may call this method (and the GUI doesn't call this method itself). Hence, no internal
    * synchronization is necessary by the GUI.
    */
   public void allowMoves(boolean canSendMoves)
   {
      gameBoard.setAcceptingMoves(canSendMoves);
   }

   /*
    * Add the move made by the opposing player to the game board. This method is
    * called from the PlayerNode class. There should be only one Move coming at
    * a time from the opposing player. Hence, this method is "thread safe".
    */
   public void addOpponentMove(String x, String y)
   {
      gameBoard.addOpponentMove(x, y);
   }

   /*
    * Updates the player list. This method is called from the PlayerNode class.
    */
   public void updatePlayers(final Vector<String> playerVector)
   {
      acquireMutex();

      try
      {
         playerList.setListData(playerVector);
         playButton.setEnabled(playerVector.size() > 0 && invitationsAllowed);
      }
      finally
      {
         guiMutex.release();
      }

      repaint();
   }

   /*
    * Clears the player list. This method is called from the PlayerNode class.
    */
   public void clearPlayers()
   {
      acquireMutex();

      try
      {
         playerList.setListData(new Vector<String>());
         playButton.setEnabled(false);
      }
      finally
      {
         guiMutex.release();
      }

      repaint();
   }

   /*
    * Updates the label above the player list to show the current Registry
    * connection status.
    */
   public void showRegistryConnection(boolean isConnected)
   {
      acquireMutex();

      try
      {
         onlineLabel.setIcon(isConnected ? onlineImageIcon : offlineImageIcon);
      }
      finally
      {
         guiMutex.release();
      }
   }

   /*
    * Appends a new message to the message log.
    */
   public void addMessage(String message)
   {
      acquireMutex();

      try
      {
         messageLog.append(message + "\n");
      }
      finally
      {
         guiMutex.release();
      }
   }

   /*
    * Empties the game board and sets whether the Board. This is called by the PlayerNode class at
    * the beginning of every Tic-Tac-Toe match. The PlayerNode enforces that only one thread at a time
    * may call this method (and the GUI doesn't call this method itself). Hence, no internal
    * synchronization is necessary by the GUI.
    */
   public void resetBoard(boolean acceptingMoves)
   {
      gameBoard.clear();
      gameBoard.setAcceptingMoves(acceptingMoves);
      repaint();
   }

   /*
    * Displays a yes or no question to the user in a dialog. "message" is the question to be asked. "options" are the
    * possible choices where options[0] is the affirmative, and options[1] is the negative. "highlightedOption" (either
    * 0 or 1) sets which options (either affirmative or negative) is highlighted. If highlightedOption is 0, then the
    * result would be affirmative if the user presses the enter key (the user would have to press tab to select the other
    * option). If the flag "warning" is true, then this dialog will show a warning icon, otherwise it will show the question
    * icon.
    */
   private boolean askYesNoQuestion(String message, final Object[] options, int highlightedOption, boolean warning)
   {
      int n = JOptionPane.showOptionDialog(this,
                                           message,
                                           "Tic Tac Toe",
                                           JOptionPane.YES_NO_OPTION,
                                           warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE,
                                           null,
                                           options,
                                           options[highlightedOption]);

      return (n == 0);
   }

   /*
    * Displays "msg" in a modal dialog in a separate thread. "type" is used to
    * select which icon will be used in the dialog. This method is called when
    * the dialog is only showing a status update and thus does not need to
    * block.
    */
   public void showMessage(final String msg, final MessageType type)
   {
      // the isDisplayable() check is so that the message box does not get shown after
      // the GUI has been disposed. Calling JOptionPane.showMessageDialog with a
      // disposed parent may keep the JVM from closing. This case may happen in tear-down
      // calls (for instance, PlayerNode.disconnectFromRegistry)
      if(isDisplayable())
      {
         final GUI parentComponent = this;

         PlayerNode.startThread(new Runnable()
         {
            @Override
            public void run()
            {
               int msgType = 0;

               if (type == MessageType.ERROR)
               {
                  msgType = JOptionPane.WARNING_MESSAGE;
               }
               else if (type == MessageType.INFORMATION)
               {
                  msgType = JOptionPane.INFORMATION_MESSAGE;
               }
               else if (type == MessageType.QUESTION)
               {
                  msgType = JOptionPane.QUESTION_MESSAGE;
               }
               else // if(type == MessageType.PLAIN)
               {
                  msgType = JOptionPane.PLAIN_MESSAGE;
               }

               JOptionPane.showMessageDialog(parentComponent, msg, "Tic Tac Toe", msgType);
            }
         });
      }
   }

   /*
    * Asks the user to provide a user name. The user must provide a name so it keeps asking
    * until the user provides one. This method is called from the PlayerNode class.
    */
   public String getUserNameFromUser(boolean askingForRegistryUserName)
   {
      String newUserName = null;

      // showInputDialog is a blocking call and this method is always
      // called when the PlayerNode's ConnectionState has not been
      // acquired. Thus, there is a chance that the user enters a
      // user name in another thread while we're still asking the
      // user for a user name here. If this happens, break out of
      // the loop no matter what the user has entered.
      //
      // But, only do this if the user name is not the one for the
      // Registry. If it's the user name used for the Registry, then
      // we must always ask for a new user name.
      boolean earlyExit = false;

      do
      {
         newUserName = (String) JOptionPane.showInputDialog(
                                                    this,
                                                    "Please choose a user name",
                                                    "Tic Tac Toe",
                                                    JOptionPane.PLAIN_MESSAGE,
                                                    null,
                                                    null,
                                                    "");

         earlyExit = !askingForRegistryUserName && !playerNode.getConnectionState().getUserName().equals("");

         if (newUserName != null)
         {
            newUserName = newUserName.trim();
         }
      } while (((newUserName == null) || (newUserName.length() == 0)) && !earlyExit);

      if (earlyExit)
      {
         if ((newUserName != null) && (newUserName.length() > 0))
         {
            addMessage("Could not assign new user name, " + newUserName + " because a user name has already been assigned");
         }

         newUserName = playerNode.getConnectionState().getUserName();
      }

      return newUserName;
   }

   /*
    * Asks the user for a URL in a dialog. "message" is the text to be shown
    * above the text box. "defaultText" is the text to be shown within the text
    * box. This method is called from the PlayerNode class.
    */
   public String getURL(String message, String defaultText)
   {
      return (String)JOptionPane.showInputDialog(
                                         this,
                                         message,
                                         "Tic Tac Toe",
                                         JOptionPane.PLAIN_MESSAGE,
                                         null,
                                         null,
                                         defaultText);
   }

   /*
    * Asks if the player wants to play against the opponent "uName"
    * listening at "url".
    */
   public boolean getWantToPlay(String uName, String url)
   {
      Object[] options = { "Yes!", "Maybe later..." };
      return askYesNoQuestion(uName + " at " + url + " has invited you to play, will you?", options, 0, false);
   }

   /*
    * Asks if the player wants to disconnect from the Registry.
    */
   public boolean getWantToDisconnectFromRegistry()
   {
      Object[] options = { "Yes", "Wait, I've changed my mind" };
      return askYesNoQuestion("Are you sure you want to disconnect from the registry?", options, 0, true);
   }

   /*
    * Asks if the player wants to cancel the Invitation that has been sent.
    */
   public boolean getWantToCancelInvitation()
   {
      Object[] options = { "Yes", "Wait, I've changed my mind" };
      return askYesNoQuestion("Are you sure you want to cancel the invitation?", options, 0, true);
   }

   /*
    * Asks if the player wants to forfeit the current match.
    */
   public boolean getWantToForfeit()
   {
      Object[] options = { "Yes", "Wait, I've changed my mind" };
      return askYesNoQuestion("The current game will be forfeited, continue?", options, 1, true);
   }

   /*
    * Asks if the player wants to forfeit the current match and quit the game.
    */
   public boolean getWantToForfeitAndQuit()
   {
      Object[] options = { "Yes", "Wait, I've changed my mind" };
      return askYesNoQuestion("The current game will be forfeited and the program will close, proceed?", options, 1, true);
   }

   /*
    * Main Action Listener for this class. Events sent from all GUI objects
    * (including the menu items) get processed here.
    */
   @Override
   public void actionPerformed(final ActionEvent e)
   {
      if (e.getSource() == playButton)
      {
         if (playerList.getSelectedIndex() == -1)
         {
            showMessage("Must select a user to play with", GUI.MessageType.ERROR);
         }
         else
         {
            playerNode.tryPlay(playerList.getSelectedValue());
         }
      }
      else if (e.getSource() == registryConnectItem)
      {
         playerNode.tryConnectToRegistry();
      }
      else if (e.getSource() == registryDisconnectItem)
      {
         playerNode.tryDisconnectFromRegistry();
      }
      else if (e.getSource() == connectDirectlyToPlayerItem)
      {
         playerNode.tryConnectToPlayer();
      }
      else if (e.getSource() == cancelInvitationItem)
      {
         playerNode.tryCancelInvitation();
      }
      else if (e.getSource() == oItem)
      {
         gameBoard.setPlayerPiece("O");
      }
      else if (e.getSource() == xItem)
      {
         gameBoard.setPlayerPiece("X");
      }
      else if (e.getSource() == forfeitItem)
      {
         playerNode.tryForfeit();
      }
      else if (e.getSource() == quitItem)
      {
         playerNode.tryQuit();
      }
   }
}