/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player.networking.node;

import java.util.Vector;

import player.Parser;
import player.networking.Endpoint;
import player.networking.Receiver;
import player.networking.Sender;
import player.networking.StrangeAxisException;
import player.networking.Subscriber;
import player.networking.WebServiceException;
import player.networking.messages.Forfeit;
import player.networking.messages.Invitation;
import player.networking.messages.Message;
import player.networking.messages.Move;
import player.ui.GUI;

/*
 * The PlayerNode class represents a node in the Tic-Tac-Toe network. Its
 * main task is to maintain the state of the network connection, including
 * connecting and disconnecting from the Registry (an Axis web service which
 * is the server part of this project) if the player is connected there. All
 * incoming network messages are sent through the PlayerNode for response and
 * it forwards any relevant game state updates to the GUI (described in GUI.java).
 * In short, it is the main decision maker on the player side of the Tic-Tac-Toe
 * network.
 */

public final class PlayerNode implements Subscriber
{
   // This reference is used for showing message boxes and for forwarding opponent moves.
   // The PlayerNode also controls when certain menu items are enabled.
   private GUI gui;

   // All incoming Messages (described in Message.java) come through the Receiver (described
   // in Receiver.java). The PlayerNode starts the Receiver's listening loop as well as kills
   // it on program termination
   private Receiver receiver;

   // Connection state. This state needs to be synchronized as it is accessed by both the
   // GUI and Receiver threads. Any methods in this class emanating from the GUI thread
   // or the Receiver thread need to acquire a lock on this state. Message objects obtain
   // and update this state. There would be less state to synchronize if multiple games
   // were allowed to be played at once. (see ConnectionState.java for the class definition)
   private ConnectionState connectionState;

   // This state doesn't need to be synchronized since it's controlled by the GUI thread.
   private boolean connectedToRegistry;
   private String registryURL; // URL where the Registry resides

   private static final String Registry_Name = "Registry";

   /*
    * PlayerNode Constructor
    */
   public PlayerNode()
   {
      connectionState = new ConnectionState();

      connectedToRegistry = false;
      registryURL = "";
   }

   /*
    * Convenience method for starting threads.
    */
   public static void startThread(Runnable r)
   {
      (new Thread(r)).start();
   }

   /*
    * Getter which gives the caller access to the PlayerNode's ConnectionState.
    * This is OK since the ConnectionState doesn't let anyone modify it unless
    * its mutex has been acquired.
    */
   public ConnectionState getConnectionState()
   {
      return connectionState;
   }

   /*
    * Acquires the mutex of the ConnectionState and returns the ConnectionState.
    * The caller must release() the ConnectionState when it's done.
    */
   public ConnectionState getAndAcquireConnectionState()
   {
      connectionState.acquire();
      return connectionState;
   }

   /***********************************************************\
   *                        Setup                              *
   \***********************************************************/

   /*
    * Attaches a Receiver and GUI to this PlayerNode.
    * The Receiver's listening loop is started here.
    */
   public void begin(Receiver r, GUI g)
   {
      receiver = r;
      gui = g;

      PlayerNode.startThread(receiver);
   }

   /*
    * Shuts down the PlayerNode. This happens just before program termination.
    */
   public void quit()
   {
      if (connectionState.getPlaying())
      {
         forfeit();
      }

      receiver.kill();

      if (connectedToRegistry)
      {
         disconnectFromRegistry();
      }
   }

   /*
    * Obtains the URL where the Receiver is listening.
    */
   public String getListeningURL()
   {
      return receiver.listeningURL();
   }

   /***********************************************************\
   *                    Tic-Tac-Toe Events                     *
   \***********************************************************/

   /*
    * Sets the PlayerNode to the default state. This is the state
    * it's in at the beginning of the program as well as when a game
    * of tic-tac-toe has been completed.
    */
   public void setNotPlaying()
   {
      gui.allowMoves(false);
      gui.allowInvitations(true);
      gui.allowForfeits(false);

      connectionState.setPlaying(false);
      connectionState.setPlayingEndpoint(new Endpoint("", ""));

      connectionState.setWaitingResponse(false);
      connectionState.setWaitingResponseURL("");
   }

   /*
    * Starts a match with the player at playingEndpoint. firstMove
    * is true if this player has the first move.
    */
   public void startGame(Endpoint playingEndpoint, boolean firstMove)
   {
      // This method is called by the Confirmation Message object.
      // Double check that the connection state has been acquired before
      // starting the game.
      if (connectionState.acquired())
      {
         connectionState.setPlaying(true);
         gui.allowForfeits(true);

         connectionState.setWaitingResponse(false);
         connectionState.setWaitingResponseURL("");

         connectionState.setPlayingEndpoint(playingEndpoint);

         gui.resetBoard(firstMove);

         gui.addMessage("Playing against: " + playingEndpoint.userName);
         gui.showMessage(firstMove ? "You get the first move!" : playingEndpoint.userName + " gets the first move!", GUI.MessageType.INFORMATION);
      }
   }

   /*
    * Shows an end game message and adds the message to the GUI's log.
    * Sets the PlayerNode back to the default state.
    */
   public void endGame(final GUI.MessageType messageType, String messageToShow, String logMessage)
   {
      // This method is called by a few different Message objects.
      // Double check that the connection state has been acquired before
      // ending the game.
      if (connectionState.acquired())
      {
         if (logMessage != null)
         {
            gui.addMessage(logMessage);
         }

         setNotPlaying();
         gui.showMessage(messageToShow, messageType);
      }
   }

   /*
    * The player has won the match. This method is called by the Board object (described in Board.java).
    * This method may emanate from the GUI thread or the Receiver thread.
    */
   public void won()
   {
      connectionState.acquire();

      try
      {
         endGame(GUI.MessageType.INFORMATION, "You Won!", "You Won!");
      }
      finally
      {
         connectionState.release();
      }
   }

   /*
    * The player has lost the match. This method is called by the Board object.
    * This method may emanate from the GUI thread or the Receiver thread.
    */
   public void lost()
   {
      connectionState.acquire();

      try
      {
         String lostMessage = connectionState.getPlayingEndpoint().userName + " has unfortunately won the match...";
         endGame(GUI.MessageType.ERROR, lostMessage, lostMessage);
      }
      finally
      {
         connectionState.release();
      }
   }

   /*
    * It was a tie game. This method is called by the Board object.
    * This method may emanate from the GUI thread or the Receiver thread.
    */
   public void catsGame()
   {
      connectionState.acquire();

      try
      {
         endGame(GUI.MessageType.INFORMATION, "It's a cat's game. Nobody wins!", "It's a cat's game. Nobody wins!");
      }
      finally
      {
         connectionState.release();
      }
   }

   /*
    * Forfeits the current match. This method may be called after the GUI has been closed. This happens if the player
    * is playing but chooses to close the GUI. In this case, they are asked if they want to forfeit and if so, the GUI
    * is closed. After which, this method is called.
    */
   private void forfeit()
   {
      // send a Forfeit Message.
      acceptSendMessage(new Forfeit(), connectionState.getPlayingEndpoint(), null);
      setNotPlaying();

      // GUI may not be visible at this point
      if (gui.isVisible())
      {
         gui.showMessage("You have forfeited the match", GUI.MessageType.INFORMATION);
      }
   }

   /*
    * Sends a Move message (described in Move.java)
    */
   public void sendMove(final String moveXML)
   {
      connectionState.acquire();

      try
      {
         acceptSendMessage(new Move(), connectionState.getPlayingEndpoint(), moveXML);
      }
      finally
      {
         connectionState.release();
      }
   }

   /*
    * Concedes a Move from the opponent. "x" is either "left", "middle", or "right" (the column of the move),
    * and "y" is either "top", "middle", or "bottom" (the row of the move). This method is called by the Move
    * Message object.
    */
   public void concedeMove(final String x, final String y)
   {
      // We concede the Move in another thread. We need to do this
      // because addOpponentMove may call won(), lost(), or catsGame().
      // These methods attempt to acquire the connectionState because
      // those methods may emanate from the GUI thread. However, in this
      // case we have already acquired the connectionState (in
      // acceptReceiveMessage). We can't check in won(), lost(), and
      // catsGame() if we have acquired the connectionState because we
      // might end up letting the GUI thread through even though the Receiver
      // thread has acquired it. Thus, we simply send this in another thread,
      // call acquire() to block and wait for the calling thread to call
      // release(), then we immediately release() the state so that the new
      // thread doesn't block forever if it reaches won(), lost(), or catsGame().

      PlayerNode.startThread(new Runnable()
      {
         @Override
         public void run()
         {
            connectionState.acquire();
            connectionState.release();

            gui.addOpponentMove(x, y);
         }
      });
   }

   /***********************************************************\
   *                 Connection Maintenance                    *
   \***********************************************************/

   /*
    * Sets whether or not the player is connected to the Registry. If the player is connected,
    * then the GUI is updated such that the player can't connect again. If the player is
    * disconnected, then the GUI is updated such that the player can't disconnect again.
    */
   private void setConnectedToRegistry(boolean connected)
   {
      connectedToRegistry = connected;
      gui.allowRegistryConnect(!connected);
      gui.allowRegistryDisconnect(connected);
      gui.showRegistryConnection(connected);
   }

   /*
    * Connects this PlayerNode to the tic-tac-toe player Registry. "url" is the
    * URL of the Registry.
    */
   private void connectToRegistry(final String url)
   {
      gui.allowRegistryConnect(false);

      boolean errorInConnecting = true;

      // Connect to the Registry by calling the Registry's "join" function and by requesting
      // to be added to the subscriber list.

      // no need to grab a copy of the userName because we want to send the most up-to-date
      // userName to the registry

      try
      {
         // If an Axis webservice, with the name "Service", is listening at 123.1.123 and port 1212,
         // then its full URL is http://123.1.123:1212/axis/services/Service
         String newRegistryURL = url + "/axis/services/" + PlayerNode.Registry_Name;

         // the following two calls may produce a WebServiceException (described in WebServiceException.java)
         Sender.sendJoin(newRegistryURL, connectionState.getUserName(), receiver.listeningURL());
         Sender.addSubscriber(newRegistryURL, receiver.listeningURL());

         // the rest of the calls in this block don't throw exceptions
         setConnectedToRegistry(true);
         errorInConnecting = false;

         registryURL = newRegistryURL;

         // get player list from the Registry
         updatePlayers();

         gui.addMessage("Connected to " + PlayerNode.Registry_Name + " at: " + url);
      }
      catch (StrangeAxisException e)
      {
         // in this case, the "StrangeAxisError" means the player tried to connect to some URL that
         gui.showMessage("Error in connecting to: " + PlayerNode.Registry_Name + " at " + url + "\nError Message: There is no Registry at this URL", GUI.MessageType.ERROR);
      }
      catch (WebServiceException e)
      {
         if (Sender.isNonUniqueUserException(e.getMessage()))
         {
            gui.showMessage("A user with this name already exists. Please select a new one", GUI.MessageType.ERROR);
         }
         else
         {
            gui.showMessage("Error in connecting to: " + PlayerNode.Registry_Name + " at " + url + "\nError Message: " + e.getMessage(), GUI.MessageType.ERROR);
         }
      }
      finally
      {
         if (errorInConnecting)
         {
            gui.allowRegistryConnect(true);
         }
      }
   }

   /*
    * Disconnects this PlayerNode from the tic-tac-toe player Registry.
    */
   private void disconnectFromRegistry()
   {
      // disconnect from the Registry by calling the Registry's "leave" function and requesting
      // to be removed from the subscriber list. This is run in a separate thread to keep the
      // GUI responsive.

      // use a copy of the userName and registryURL in case they change in the interim.
      final String snapshotUserName = connectionState.getUserName();
      final String snapshotRegistryURL = registryURL;

      PlayerNode.startThread(new Runnable()
      {
         @Override
         public void run()
         {
            try
            {
               Sender.removeSubscriber(snapshotRegistryURL, receiver.listeningURL());
               Sender.sendLeave(snapshotRegistryURL, snapshotUserName);

               gui.addMessage("Disconnected from Registry");
            }
            catch (StrangeAxisException e)
            {
            }
            catch (WebServiceException e)
            {
               gui.showMessage("Error in disconnecting from the Registry. Reverting to unconnected state.\nError Message: " + e.getMessage(), GUI.MessageType.ERROR);
            }
         }
      });

      setConnectedToRegistry(false);
      gui.clearPlayers();
   }

   /*
    * A Message has been received by the Receiver and forwarded here.
    * We acquire the connectionState because this call emanates from
    * the Receiver thread, and we let the Message receive itself.
    * acceptReceiveMessage and m.receive correspond to the "accept" and
    * "visit" methods of the visitor design pattern.
    */
   public void acceptReceiveMessage(Message m)
   {
      connectionState.acquire();

      try
      {
         m.receive(this);
      }
      finally
      {
         connectionState.release();
      }
   }

   /*
    * Here we send a Message by taking the instantiated Message and having the
    * Message send itself. acceptSendMessage and m.send correspond to the
    * "accept" and "visit" methods of the visitor design pattern.
    */
   public void acceptSendMessage(Message m, Endpoint endpoint, Object data)
   {
      m.send(this, endpoint, data);
   }

   /*
    * Send an Invitaion Message (see: Invitation.java) to the player listening
    * at opponentURL.
    */
   private void sendInvitation(String opponentURL)
   {
      // We want to use the fully qualified URL. Replace "localhost" with the
      // IP of the localhost if need be.

      opponentURL = opponentURL.toLowerCase().replace("localhost", receiver.getLocalHostIP());
      acceptSendMessage(new Invitation(), new Endpoint(opponentURL), null);
   }

   /*
    * This method is called when a player has either joined or left the network. This method is called
    * from the Update Message object and this Message originates from the Registry. "topic" is either
    * "join" or "leave" and "userName" is the user name of the player either joining or leaving the
    * network. This method is required to implement the Subscriber interface. (described in
    * Subscriber.java)
    */
   @Override
   public void update(String topic, String userName)
   {
      gui.addMessage("Server says: " + userName + " has " + (topic.equals("join") ? "joined" : "left"));
      updatePlayers();
   }

   /*
    * Updates the GUI's list that shows the players currently connected to the Registry.
    */
   private void updatePlayers()
   {
      // Request for a player list update from the Registry in a separate thread.

      // Use a copy of the registryURL in case it changes in the interim
      final String snapshotRegistryURL = registryURL;

      PlayerNode.startThread(new Runnable()
      {
         @Override
         public void run()
         {
            try
            {
               // Sender.getPlayerList may throw an Exception. It's also a blocking call. We may have disconnected
               // from the registry before we got a response.
               Vector<String> playerList = Parser.getPlayerListFromXML(Sender.getPlayerList(snapshotRegistryURL));

               // check if we're still connected to the registry we sent the request to
               if (snapshotRegistryURL.equals(registryURL))
               {
                  connectionState.acquire();

                  try
                  {
                     playerList.remove(connectionState.getUserName());
                  }
                  finally
                  {
                     connectionState.release();
                  }

                  gui.updatePlayers(playerList);
               }

            }
            catch (StrangeAxisException e)
            {
            }
            catch (WebServiceException e)
            {
               gui.addMessage("Administration says: Error in updating player list");
            }
         }
      });
   }

   /***********************************************************\
   *                      GUI Accessors                        *
   \***********************************************************/

   public boolean doesUserWantToPlay(String invitingUserName, String invitingURL)
   {
      return gui.getWantToPlay(invitingUserName, invitingURL);
   }

   public void allowInvitations(boolean allowInvitations)
   {
      gui.allowInvitations(allowInvitations);
   }

   public String getUserNameFromUser()
   {
      return gui.getUserNameFromUser(false);
   }

   public void showMessage(String msg, final GUI.MessageType type)
   {
      gui.showMessage(msg, type);
   }

   /***********************************************************\
   *             Responses to User/GUI Requests                *
   \***********************************************************/

   /*
    * Asks the user to provide a URL for the Registry, if forRegistry is true, or the URL of a player
    * they want to connect to, otherwise. Returns null if the URL is invalid or if the user cancels.
    */
   private String getURLFromUser(boolean forRegistry)
   {
      String url = gui.getURL("Please input the " + (forRegistry ? "Registry" : "player") + "'s IP Address and port", "localhost:" + receiver.getPort());

      if (url == null)
      {
         return null;
      }

      url = url.trim();

      if (url.length() == 0)
      {
         gui.showMessage("Connection cancelled", GUI.MessageType.INFORMATION);
         return null;
      }

      //the url inputted from the user does not need to have "http://" in front.
      //Place "http://" in front of the inputted URL if it's not there
      url = Parser.completeURL(url);

      if (!Parser.isValidURL(url))
      {
         gui.showMessage("URL entered is invalid", GUI.MessageType.ERROR);
         return null;
      }

      // check if the user inputted their own connection information
      if (Parser.stringMatches(url.toLowerCase(), "localhost:" + receiver.getPort()) || Parser.stringMatches(url, receiver.getLocalHostIP() + ":" + receiver.getPort()))
      {
         gui.showMessage(forRegistry ? "There is no Registry at this URL" : "Sorry, you can't play with yourself", GUI.MessageType.ERROR);
         return null;
      }

      return url;
   }

   /*
    * Nullifies the invitation sent by the user after asking for confirmation. This causes
    * this player to send a "no" as a Confirmation if the invited player responds with a "yes".
    * This method call emanates from the GUI thread. (see: GUI.java)
    */
   public void tryCancelInvitation()
   {
      connectionState.acquire();

      try
      {
         if (connectionState.getWaitingResponse())
         {
            // Ask if the player wants to cancel the invitation in
            // a separate thread. This is because of the blocking
            // call getWantToCancelInvitation(). See GUI.java for
            // an explanation.
            PlayerNode.startThread(new Runnable()
            {
               @Override
               public void run()
               {
                  boolean wantToCancel = gui.getWantToCancelInvitation();

                  if (wantToCancel)
                  {
                     connectionState.acquire();

                     try
                     {
                        if (connectionState.getWaitingResponse())
                        {
                           connectionState.setWaitingResponse(false);
                           connectionState.setWaitingResponseURL("");
                           gui.allowInvitations(true);
                        }
                        else if (connectionState.getPlaying())
                        {
                           gui.showMessage("Error in cancelling the invitation. The match has already started.", GUI.MessageType.ERROR);
                        }
                     }
                     finally
                     {
                        connectionState.release();
                     }
                  }
               }
            });
         }
      }
      finally
      {
         connectionState.release();
      }
   }

   /*
    * Asks the user for the IP Address and port of the Registry as well as the user name
    * that they want to use. If there are no errors in input, then an attempt is made to connect
    * to the Registry. This method call emanates from the GUI thread.
    */
   public void tryConnectToRegistry()
   {
      final String url = getURLFromUser(true);

      if (url != null)
      {
         // See GUI.java for an explanation as to why getUserNameFromUser()
         // is called in a separate thread.
         PlayerNode.startThread(new Runnable()
         {
            @Override
            public void run()
            {
               // the user name used for the registry cancels out
               // the user name entered previously.
               String newUserName = gui.getUserNameFromUser(true);

               connectionState.acquire();

               try
               {
                  connectionState.setUserName(newUserName);
                  connectToRegistry(url);
               }
               finally
               {
                  connectionState.release();
               }
            }
         });
      }
   }

   /*
    * Disconnects from the Registry after asking the player for confirmation.
    * This method call emanates from the GUI thread.
    */
   public void tryDisconnectFromRegistry()
   {
      if (gui.getWantToDisconnectFromRegistry())
      {
         connectionState.acquire();

         try
         {
            disconnectFromRegistry();
         }
         finally
         {
            connectionState.release();
         }
      }
   }

   /*
    * Asks the user for the IP Address and port of the opponent they wish to play with. If the user does
    * not have a user name yet, then they will be asked to provide one. If there are no errors in input,
    * then an Invitation message is sent to the opponent. This method call emanates from the GUI thread.
    */
   public void tryConnectToPlayer()
   {
      String url = getURLFromUser(false);

      if (url != null)
      {
         // If the user doesn't have a user name, then we need to ask them to provide one.
         // However, getUserNameFromUser is a blocking call so we should ask for the user
         // name before acquiring the connection state. If we see that the user has a user
         // name after acquiring the connection state, then we don't assign them this one
         // as the user must have entered a user name in another thread.
         //
         // See GUI.java for an explanation of this awkwardness.

         boolean shouldChangeUserName = connectionState.getUserName().equals("");
         String newUserName = "";

         if (shouldChangeUserName)
         {
            newUserName = gui.getUserNameFromUser(false);
         }

         connectionState.acquire();

         try
         {
            if (!connectionState.getPlaying() && !connectionState.getWaitingConfirmation())
            {
               // assign the new user name if applicable
               if (shouldChangeUserName && connectionState.getUserName().equals(""))
               {
                  connectionState.setUserName(newUserName);
               }

               sendInvitation(url);
            }
            else
            {
               // this happens if this player has started another match or has
               // accepted an invitation before entering the URL or user name
               gui.showMessage("Invitation cancelled: You have already " + (connectionState.getPlaying() ? "started a match" : "accepted another invitation"), GUI.MessageType.INFORMATION);
            }
         }
         finally
         {
            connectionState.release();
         }
      }
   }

   /*
    * Attempts to start a game with an opponent. Here, the opponent was selected
    * from the GUI's list of users currently connected to the Registry. This
    * method call emanates from the GUI thread.
    */
   public void tryPlay(String opponentUserName)
   {
      connectionState.acquire();

      try
      {
         sendInvitation(Sender.getConnectionInfo(registryURL, opponentUserName)); //Sender.getConnectionInfo might throw an exception
      }
      catch (StrangeAxisException e)
      {
      }
      catch (WebServiceException e)
      {
         if (Sender.isUserDoesNotExistException(e.getMessage()))
         {
            endGame(GUI.MessageType.ERROR, "User " + opponentUserName + " has unexpectedly disconnected. Reacquiring player list", "Error in sending invitation to: " + opponentUserName);
            updatePlayers();
         }
         else
         {
            String errorMessage = "Error in getting connection information for: " + opponentUserName + "\nRegistry may be down...Please re-initialize connection";
            endGame(GUI.MessageType.ERROR, errorMessage, "Error in sending invitation to: " + opponentUserName);

            setConnectedToRegistry(false);
         }
      }
      finally
      {
         connectionState.release();
      }
   }

   /*
    * Forfeits the current match after asking the player for confirmation. This
    * method call emanates from the GUI thread.
    */
   public void tryForfeit()
   {
      if (gui.getWantToForfeit())
      {
         connectionState.acquire();

         try
         {
            if (connectionState.getPlaying())
            {
               forfeit();
            }
            else
            {
               // this occurs when the player selects forfeit, but then the game ends from
               // the opponent's next move.
               gui.showMessage("Error in forfeiting the match: the match has already finished.", GUI.MessageType.ERROR);
            }
         }
         finally
         {
            connectionState.release();
         }
      }
   }

   /*
    * Attempts to close the GUI window. This method call emanates from the GUI thread.
    */
   public void tryQuit()
   {
      connectionState.acquire();

      try
      {
         if (connectionState.getPlaying())
         {
            // See GUI.java for an explanation as to why getWantToForfeitAndQuit()
            // is called in a separate thread.
            PlayerNode.startThread(new Runnable()
            {
               @Override
               public void run()
               {
                  if (gui.getWantToForfeitAndQuit())
                  {
                     gui.quit();
                  }
               }
            });
         }
         else
         {
            gui.quit();
         }
      }
      finally
      {
         connectionState.release();
      }
   }
}