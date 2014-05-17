/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player.networking.messages;

import player.networking.*;
import player.networking.node.*;

import org.apache.axis.encoding.XMLType;

/*
 * An Invitation Message is sent when the user wants to invite a
 * potential opponent to begin a match.
 */

public class Invitation extends Message
{
   /*
    * Invitation Constructor.
    */
   public Invitation()
   {
      super();
   }

   /*
    * Invitation Constructor (when receiving an Invitation as the SOAP, soapMsg).
    */
   public Invitation(String soapMsg)
   {
      super(soapMsg);
   }

   /*
    * See description of isSelf in Message.java
    */
   public static boolean isSelf(String soapMsg)
   {
      return player.Parser.stringMatches(soapMsg, "<soapenv:Body><invitation");
   }

   /*
    * Sends an Invitation Message to the player listening at "endpoint".
    */
   protected void send(final Endpoint endpoint, final Object data)
   {
      ConnectionState connectionState = playerNode.getConnectionState();

      connectionState.setWaitingResponse(true);
      connectionState.setWaitingResponseURL(endpoint.url);

      playerNode.allowInvitations(false);

      //send an Invitation in another thread. Use a copy of the synchronized state
      //in case it changes in the interim.

      final String snapshotUserName = connectionState.getUserName();

      PlayerNode.startThread
      (
         new Runnable()
         {
            public void run()
            {
               try
               {
                  //Invitation is implemented as a web service function returning void and taking two parameters:
                  //(1) the user name, and (2) the URL of the player sending the Invitation (both Strings)
                  Object params[] = {snapshotUserName, playerNode.getListeningURL()};
                  String paramNames[] = {ParameterNames.UserName, ParameterNames.URL};

                  Sender.callWebserviceFunction(endpoint.url, "invitation", 2, XMLType.XSD_ANYTYPE, params, paramNames);
               }
               catch (StrangeAxisException e)
               {
               }
               catch (WebServiceException e)
               {
                  //Error in sending the invitation. Show the error if this invitation is still relevant.
                  ConnectionState acquiredConnectionState = playerNode.getAndAcquireConnectionState();

                  try
                  {
                     boolean invitationIsStillRelevant = acquiredConnectionState.getWaitingResponse() && (acquiredConnectionState.getWaitingResponseURL().equals(endpoint.url));
                     if(invitationIsStillRelevant)
                     {
                        playerNode.endGame(player.ui.GUI.MessageType.ERROR, "Error in sending invitation to: " + endpoint.url + ".\nPlease check the connection information provided.", "Error in sending invitation to: " + endpoint.url);
                     }
                  }
                  finally
                  {
                     acquiredConnectionState.release();
                  }
               }
            }
         }
      );
   }

   /*
    * Receives an Invitation Message. The user name and url of the player sending the
    * Invitation is parsed from the SOAP. A Response Message (described in Response.java)
    * is then sent.
    */
   protected void receive()
   {
      final Endpoint invitingEndpoint = new Endpoint(Message.parseArg(ParameterNames.URL, soap), Message.parseArg(ParameterNames.UserName, soap));

      final ConnectionState connectionState = playerNode.getConnectionState();

      if(connectionState.getPlaying())
      {
         playerNode.acceptSendMessage(new Response(), invitingEndpoint, "playing");
      }
      else if(connectionState.getWaitingConfirmation())
      {
         playerNode.acceptSendMessage(new Response(), invitingEndpoint, "no");
      }
      else
      {
         //Ask the user if they want to play with the player who sent the Invitation.
         //This is done in a separate thread to allow other Messages to be received
         //and handled.
         PlayerNode.startThread
         (
            new Runnable()
            {
               public void run()
               {
                  //does this player want to play with the inviting player?
                  boolean wantToPlay = playerNode.doesUserWantToPlay(invitingEndpoint.userName, invitingEndpoint.url);

                  //If the user doesn't have a user name, then we need to ask them to provide one.
                  //However, getUserNameFromUser is a blocking call so we should ask for the user
                  //name before acquiring the connection state. If we see that the user has a user
                  //name after acquiring the connection state, then we don't assign them this one
                  //as the user must have entered a user name in another thread.
                  //
                  //See GUI.java for an explanation of this awkwardness.

                  boolean shouldChangeUserName = false;
                  String newUserName = "";

                  if(wantToPlay)
                  {
                     String currentUserName = playerNode.getConnectionState().getUserName();

                     if(currentUserName.equals(""))
                     {
                        newUserName = playerNode.getUserNameFromUser();
                        shouldChangeUserName = true;
                     }
                  }

                  ConnectionState acquiredConnectionState = playerNode.getAndAcquireConnectionState();

                  try
                  {
                     //since this is in another thread, this player may have started
                     //another game already. Double check if they're already playing.
                     boolean canPlay = !acquiredConnectionState.getPlaying() && !acquiredConnectionState.getWaitingConfirmation();

                     if(wantToPlay && !canPlay)
                     {
                        String cantPlayReason = "";

                        if(acquiredConnectionState.getPlaying())
                        {
                           cantPlayReason = " because you're already playing.";
                        }
                        else if(acquiredConnectionState.getWaitingConfirmation())
                        {
                           cantPlayReason = " because you've already accepted an invitation.";
                        }

                        playerNode.showMessage("Could not start a match with " + invitingEndpoint.userName + cantPlayReason, player.ui.GUI.MessageType.ERROR);
                     }

                     //assign the new user name, if applicable
                     if(shouldChangeUserName && acquiredConnectionState.getUserName().equals(""))
                     {
                        acquiredConnectionState.setUserName(newUserName);
                     }

                     //send the Response
                     String response = "";

                     if(acquiredConnectionState.getPlaying())
                     {
                        response = "playing";
                     }
                     else if(acquiredConnectionState.getWaitingConfirmation())
                     {
                        response = "no";
                     }
                     else
                     {
                        response = wantToPlay ? "yes" : "no";
                     }

                     playerNode.acceptSendMessage(new Response(), invitingEndpoint, response);
                  }
                  finally
                  {
                     acquiredConnectionState.release();
                  }
               }
            }
         );
      }
   }

   /*
    * The following is an example of the SOAP of an Invitation Message
    *
    * <?xml version="1.0" encoding="UTF-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/so
    * ap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSche
     * ma-instance"><soapenv:Body><invitation soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encodi
    * ng/"><UserName xsi:type="xsd:string">Bob</UserName><URL xsi:type="xsd:string">http://192.168.1.64:90
    * 91</URL></invitation></soapenv:Body></soapenv:Envelope>
    */
}