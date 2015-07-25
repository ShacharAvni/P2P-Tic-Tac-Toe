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

import org.apache.axis.encoding.XMLType;

import player.networking.Endpoint;
import player.networking.Sender;
import player.networking.StrangeAxisException;
import player.networking.WebServiceException;
import player.networking.node.ConnectionState;
import player.networking.node.PlayerNode;

/*
 * A Confirmation Message is sent by a player who has received
 * an affirmative Response (see: Response.java). If the player is
 * still waiting for the Response and is able to play, then the player
 * sends "yes" in the Confirmation Message, otherwise the player sends
 * "no". If a player receives "yes" in a Confirmation, then the match
 * begins. The player sending the Confirmation determines who gets the
 * first move.
 */

public class Confirmation extends Message
{
   /*
    * Confirmation Constructor.
    */
   public Confirmation()
   {
      super();
   }

   /*
    * Confirmation Constructor (when receiving a Confirmation as the SOAP, soapMsg).
    */
   public Confirmation(String soapMsg)
   {
      super(soapMsg);
   }

   /*
    * See description of isSelf in Message.java
    */
   public static boolean isSelf(String soapMsg)
   {
      return player.Parser.stringMatches(soapMsg, "<soapenv:Body><confirmation");
   }

   /*
    * Sends a Confirmation Message to the player listening at "endpoint". If the Confirmation is a "yes",
    * this means we received a "yes" Response from the player we invited to play. In this case, we randomly
    * determine who gets the first move, send that as part of the "yes" Confirmation, and start the game.
    * Otherwise, we send a "no" Confirmation to the other player letting them know the game should not begin.
    */
   @Override
   protected void send(final Endpoint endpoint, final Object data)
   {
      String confirmation = data.toString();

      final ConnectionState connectionState = playerNode.getConnectionState();

      if (confirmation.equals("yes"))
      {
         // In this case the Confirmation is "yes". Send a Confirmation and block until the Confirmation
         // is sent. This is important since the game is about to begin

         // randomly determine who gets the first move
         boolean receivingPlayerGetsFirstMove = (new java.util.Random()).nextBoolean();

         try
         {
            doSend(endpoint.url, new Endpoint(playerNode.getListeningURL(), connectionState.getUserName()), new ConfirmationData(true, receivingPlayerGetsFirstMove));

            // Confirmation sent successfully. Start the game!
            playerNode.startGame(endpoint, !receivingPlayerGetsFirstMove);
         }
         catch (WebServiceException e)
         {
            playerNode.endGame(player.ui.GUI.MessageType.ERROR, "Error in starting match with " + endpoint.userName, "Administration Says: Error in starting match with " + endpoint.userName);
         }
      }
      else
      {
         //In this case the Confirmation is "no". Send the Confirmation in another thread.

         // Use a copy of the userName in case it changes in the interim.
         final String snapshotUserName = connectionState.getUserName();

         PlayerNode.startThread(new Runnable()
         {
            @Override
            public void run()
            {
               try
               {
                  doSend(endpoint.url, new Endpoint(playerNode.getListeningURL(), snapshotUserName), new ConfirmationData(false, false));
               }
               catch (WebServiceException e)
               {
                  // Error occurred in sending the Confirmation. In this case, the player doesn't care
                  // since the player sending the affirmative response was not the one we were waiting
                  // for. Log the error for posterity.
                  player.Logger.logError("Error in sending negative Confirmation to: " + endpoint.userName + " at " + endpoint.url, e);
               }
            }
         });
      }
   }

   /*
    * Receives a Confirmation Message. The confirmation ("yes" or "no"), whether this player gets the
    * first move ("yes" or "no") and the user name and url of the player sending the Confirmation is
    * parsed from the SOAP. If the Confirmation is "yes" (and the Confirmation is from the player
    * we expected), then the game is started.
    */
   @Override
   protected void receive()
   {
      String confirmation = Message.parseArg(ParameterNames.Answer, soap);
      String hasFirstMove = Message.parseArg(ParameterNames.HasFirstMove, soap);
      Endpoint confirmingEndpoint = new Endpoint(Message.parseArg(ParameterNames.URL, soap), Message.parseArg(ParameterNames.UserName, soap));

      ConnectionState connectionState = playerNode.getConnectionState();

      if(connectionState.getWaitingConfirmation() && connectionState.getWaitingConfirmationURL().equals(confirmingEndpoint.url))
      {
         connectionState.setWaitingConfirmation(false);
         connectionState.setWaitingConfirmationURL("");

         if (confirmation.equals("yes"))
         {
            playerNode.startGame(confirmingEndpoint, hasFirstMove.equals("yes"));
         }
         else
         {
            playerNode.endGame(player.ui.GUI.MessageType.ERROR, "Error in starting match with " + confirmingEndpoint.userName + ". They may have cancelled their request to play", "Administration Says: Error in starting match with " + confirmingEndpoint.userName);
         }
      }
   }

   /*
    * This method does the actual work of sending the Confirmation Message.
    */
   private void doSend(String endpoint, Endpoint thisEndpoint, final Object data) throws WebServiceException
   {
      try
      {
         // Confirmation is implemented as a web service function returning void and taking four parameters:
         // (1) the confirmation ("yes" or "no"), (2) whether the player receiving the confirmation gets the
         // first move ("yes" or "no", note: "no" is always sent here if the confirmation is a "no")), (3) the
         // user name, and (4) the URL of the player sending the Confirmation (all Strings)

         ConfirmationData confirmationData = (ConfirmationData) data;

         Object params[] = { confirmationData.confirmed ? "yes" : "no", ( confirmationData.confirmed && confirmationData.hasFirstMove ) ? "yes" : "no",
                             thisEndpoint.userName, thisEndpoint.url };

         String paramNames[] = {ParameterNames.Answer, ParameterNames.HasFirstMove, ParameterNames.UserName, ParameterNames.URL};

         Sender.callWebserviceFunction(endpoint, "confirmation", 4, XMLType.XSD_ANYTYPE, params, paramNames);
      }
      catch (StrangeAxisException e)
      {
      }
   }

   /*
    * The following is an example of the SOAP of a Confirmation Message
    *
    * <?xml version="1.0" encoding="UTF-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/so
    * ap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSche
    * ma-instance"><soapenv:Body><confirmation soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/enco
    * ding/"><Answer xsi:type="xsd:string">yes</Answer><HasFirstMove xsi:type="xsd:string">yes</HasFirstMo
    * ve><UserName xsi:type="xsd:string">Bob</UserName><URL xsi:type="xsd:string">http://192.168.1.64:9091
    * </URL></confirmation></soapenv:Body></soapenv:Envelope>
    */
}