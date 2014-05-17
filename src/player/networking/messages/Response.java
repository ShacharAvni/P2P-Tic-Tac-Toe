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
 * A Response Message is sent as a response to a player that sent an
 * Invitation (described in Invitation.java). The content of the Response
 * is either "yes", meaning the player has accepted the Invitation and
 * wants to play, "no", meaning the player has rejected the Invitation
 * and does not want to play, or "playing", meaning the player is already
 * playing a match and cannot start a new one. After sending a Response,
 * the player waits for a Confirmation Message (see: Confirmation.java).
 */

public class Response extends Message
{
   /*
    * Response Constructor.
    */
   public Response()
   {
      super();
   }

   /*
    * Response Constructor (when receiving a Response as the SOAP, soapMsg).
    */
   public Response(String soapMsg)
   {
      super(soapMsg);
   }

   /*
    * See description of isSelf in Message.java
    */
   public static boolean isSelf(String soapMsg)
   {
      return player.Parser.stringMatches(soapMsg, "<soapenv:Body><response");
   }

   /*
    * Sends a Response to the player listening at "endpoint". If the Response is "yes", then
    * the game is started with the opponent receiving the first move.
    */
   protected void send(final Endpoint endpoint, final Object data)
   {
      final String response = data.toString();

      ConnectionState connectionState = playerNode.getConnectionState();

      if(response.equals("yes"))
      {
         connectionState.setWaitingConfirmation(true);
         connectionState.setWaitingConfirmationURL(endpoint.url);
         playerNode.allowInvitations(false);
      }

      //send a Response in another thread. Use a copy of the user name
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
                  //Response is implemented as a webservice function that returns void and accepts three parameters:
                  //(1) the response (either "yes", "no", or "playing"), (2) the user name, and (3) the URL of the
                  //player sending the Response (all Strings)
                  Object params[] = {response, snapshotUserName, playerNode.getListeningURL()};
                  String paramNames[] = {ParameterNames.Answer, ParameterNames.UserName, ParameterNames.URL};

                  Sender.callWebserviceFunction(endpoint.url, "response", 3, XMLType.XSD_ANYTYPE, params, paramNames);
               }
               catch (StrangeAxisException e)
               {
               }
               catch (WebServiceException e)
               {
                  //Error occurred in sending the Response to url. Show an error if this Response is still relevant.
                  ConnectionState acquiredConnectionState = playerNode.getAndAcquireConnectionState();

                  try
                  {
                     boolean responseIsStillRelevant = acquiredConnectionState.getWaitingConfirmation() && acquiredConnectionState.getWaitingConfirmationURL().equals(endpoint.url);
                     if(responseIsStillRelevant)
                     {
                        acquiredConnectionState.setWaitingConfirmation(false);
                        acquiredConnectionState.setWaitingConfirmationURL("");
                        playerNode.endGame(player.ui.GUI.MessageType.ERROR, "Error in sending response to: " + endpoint.userName + "\nThey may have lost their connection or may have left", "Administration says: Error in sending response to: " + endpoint.userName);
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
    * Receives a Response Message. The response and the user name and URL of the player sending
    * the Response is parsed from the SOAP. If the Response is "yes", then this player sends
    * a Confirmation Message, specifying whether or not the game will begin.
    */
   protected void receive()
   {
      String response = Message.parseArg(ParameterNames.Answer, soap);
      Endpoint respondingEndpoint = new Endpoint(Message.parseArg(ParameterNames.URL, soap), Message.parseArg(ParameterNames.UserName, soap));

      ConnectionState connectionState = playerNode.getConnectionState();

      //is the player who sent this Response the one we're currently waiting for?
      boolean responseIsFromExpectedPlayer = connectionState.getWaitingResponse() && connectionState.getWaitingResponseURL().equals(respondingEndpoint.url);

      if(responseIsFromExpectedPlayer)
      {
         connectionState.setWaitingResponse(false);
         connectionState.setWaitingResponseURL("");
      }

      if(response.equals("yes"))
      {
         //a Confirmation must be sent to any player who responded with a "yes".
         playerNode.acceptSendMessage(new Confirmation(), respondingEndpoint, responseIsFromExpectedPlayer ? "yes" : "no");
      }
      else if(responseIsFromExpectedPlayer)
      {
         //response is either "no" or "playing". Either way, a game is not started.
         String cantPlayMessage = "";

         if(response.equals("playing"))
         {
            cantPlayMessage = respondingEndpoint.userName + " is in the midst of a heated battle.\nTry again later.";
         }
         else if(respondingEndpoint.userName.equals(""))
         {
            cantPlayMessage = "The player at " + respondingEndpoint.url + " has reluctantly passed off your offer";
         }
         else
         {
            cantPlayMessage = respondingEndpoint.userName + " has reluctantly passed off your offer";
         }

         playerNode.endGame(player.ui.GUI.MessageType.ERROR, cantPlayMessage, null);
      }
   }

   /*
    * The following is an example of the SOAP of a Response Message
    *
    * <?xml version="1.0" encoding="UTF-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/so
    * ap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSche
    * ma-instance"><soapenv:Body><response soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding
    * /"><Answer xsi:type="xsd:string">yes</Answer><UserName xsi:type="xsd:string">Bob</UserName><URL xsi:
    * type="xsd:string">http://192.168.1.64:9092</URL></response></soapenv:Body></soapenv:Envelope>
    */
}