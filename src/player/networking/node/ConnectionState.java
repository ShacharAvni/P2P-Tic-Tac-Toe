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

import player.Logger;
import player.networking.Endpoint;

import java.util.concurrent.Semaphore;

/*
 * The ConnectionState class is a storage class that contains
 * all the PlayerNode's state that may be the victim of
 * synchronization problems. The mutex member of this class
 * should be acquired whenever any of its members need to be
 * modified. There is only one instance of this class in this
 * program and it is a member of the PlayerNode. The PlayerNode
 * gives Message objects access to this object but that's OK
 * since the ConnectionState prohibits modifications to itself
 * if its mutex has not been acquired.
 */

public final class ConnectionState implements Cloneable
{
   private boolean playing; //Are we currently playing a match?

   private String userName; //user name chosen for this player

   private Endpoint playingEndpoint; //user name of the opponent and full URL where the opponent resides (including http://, :, and port)

   private boolean waitingResponse; //Have we sent an Invitation and are awaiting a Response?
   private String waitingResponseURL; //the URL of the player from whom we're awaiting a Response

   private boolean waitingConfirmation; //Have we sent a Response and are awaiting a Confirmation?
   private String waitingConfirmationURL; //the URL of the player from whom we're awaiting a Confirmation

   //This mutex is used so no synchronization errors occur when accessing the above members.
   private Semaphore mutex;

   /*
    * ConnectionState constructor.
    */
   public ConnectionState()
   {
      mutex = new Semaphore(1);

      userName = "";

      playing = false;
      playingEndpoint = new Endpoint("", "");

      waitingResponse = false;
      waitingResponseURL = "";

      waitingConfirmation = false;
      waitingConfirmationURL = "";
   }

   /*
    * Attempts to acquire the mutex protecting this
    * object's members. The program is abruptly
    * terminated if an InterruptedException occurs.
    * This exception would be completely unexpected
    * since this program does not interrupt any of
    * its threads.
    */
   public void acquire()
   {
      try
      {
         mutex.acquire();
      }
      catch(InterruptedException e)
      {
         Logger.fatalError("Unexpected Thread Interruption.", e);
      }
   }

   /*
    * Releases the mutex protecting this object's members.
    */
   public void release()
   {
      mutex.release();
   }

   /*
    * Determines if this object's mutex is in the acquired state.
    */
   public boolean acquired()
   {
      return (mutex.availablePermits() == 0);
   }

   /*
    * Standard clone method. The cloned instance has its own mutex.
    */
   public ConnectionState clone()
   {
      try
      {
         ConnectionState cloned = (ConnectionState) super.clone();
         cloned.playingEndpoint = playingEndpoint.clone();
         cloned.mutex = new Semaphore(1);
         return cloned; //shallow copy is OK for the rest of the members
      }
      catch(CloneNotSupportedException e)
      {
         //this should never happen
         player.Logger.logError("Unexpected Error in cloning ConnectionState", e);

         ConnectionState cloned = new ConnectionState();
         cloned.userName = userName;
         cloned.playing = playing;
         cloned.playingEndpoint = playingEndpoint.clone();
         cloned.waitingResponse = waitingResponse;
         cloned.waitingResponseURL = waitingResponseURL;
         cloned.waitingConfirmation = waitingConfirmation;
         cloned.waitingConfirmationURL = waitingConfirmationURL;

         return cloned;
      }
   }

   /***********************************************************\
   *                        Setters                            *
   \***********************************************************/

   /*
    * The members are set only if the mutex has been acquired.
    */

   public void setPlaying(boolean newPlaying)
   {
      if(acquired())
      {
         playing = newPlaying;
      }
   }

   public void setUserName(String newUserName)
   {
      if(acquired())
      {
         userName = newUserName;
      }
   }

   public void setPlayingEndpoint(Endpoint newPlayingEndpoint)
   {
      if(acquired())
      {
         playingEndpoint = newPlayingEndpoint.clone();
      }
   }

   public void setWaitingResponse(boolean newWaitingResponse)
   {
      if(acquired())
      {
         waitingResponse = newWaitingResponse;
      }
   }

   public void setWaitingResponseURL(String newWaitingResponseURL)
   {
      if(acquired())
      {
         waitingResponseURL = newWaitingResponseURL;
      }
   }

   public void setWaitingConfirmation(boolean newWaitingConfirmation)
   {
      if(acquired())
      {
         waitingConfirmation = newWaitingConfirmation;
      }
   }

   public void setWaitingConfirmationURL(String newWaitingConfirmationURL)
   {
      if(acquired())
      {
         waitingConfirmationURL = newWaitingConfirmationURL;
      }
   }

   /***********************************************************\
   *                        Getters                            *
   \***********************************************************/

   public boolean getPlaying()
   {
      return playing;
   }

   public String getUserName()
   {
      return userName;
   }

   public Endpoint getPlayingEndpoint()
   {
      return playingEndpoint.clone();
   }

   public boolean getWaitingResponse()
   {
      return waitingResponse;
   }

   public String getWaitingResponseURL()
   {
      return waitingResponseURL;
   }

   public boolean getWaitingConfirmation()
   {
      return waitingConfirmation;
   }

   public String getWaitingConfirmationURL()
   {
      return waitingConfirmationURL;
   }
}