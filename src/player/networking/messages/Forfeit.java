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
 * A Forfeit Message is sent to notify the opponent that the user has
 * forfeited the current match.
 */

public class Forfeit extends Message
{
   /*
    * Forfeit Constructor.
    */
   public Forfeit()
   {
      super();
   }

   /*
    * Forfeit Constructor (when receiving a Forfeit as the SOAP, soapMsg).
    */
   public Forfeit(String soapMsg)
   {
      super(soapMsg);
   }

   /*
    * See description of isSelf in Message.java
    */
   public static boolean isSelf(String soapMsg)
   {
      return player.Parser.stringMatches(soapMsg, "<soapenv:Body><forfeit");
   }

   /*
    * Sends a Forfeit Message to the player listening at "endpoint", indicating
    * that we have forfeited the current match. In this case "data" is just a
    * dummy parameter and it is only needed to maintain the send API.
    */
   protected void send(final Endpoint endpoint, final Object data)
   {
      final String snapshotUserName = playerNode.getConnectionState().getUserName();

      PlayerNode.startThread
      (
         new Runnable()
         {
            public void run()
            {
               try
               {
                  //Forfeit is implemented as a web service function returning void and taking two parameters:
                  //(1) the user name, and (2) the URL of the player sending the Forfeit (both Strings)
                  Object params[] = {snapshotUserName, playerNode.getListeningURL()};
                  String paramNames[] = {ParameterNames.UserName, ParameterNames.URL};

                  Sender.callWebserviceFunction(endpoint.url, "forfeit", 2, XMLType.XSD_ANYTYPE, params, paramNames);
               }
               catch (StrangeAxisException e)
               {
               }
               catch (WebServiceException e)
               {
                  //An error occurred while sending the forfeit. The player doesn't care that
                  //there was an error since they've already stopped the match. Log the error
                  //for posterity.
                  player.Logger.logError("Error in forfeiting match with: " + endpoint.userName, e);
               }
            }
         }
      );
   }

   /*
    * Receives a Forfeit Message. The user name and url of the player sending the Forfeit
    * is parsed from the SOAP. We accept the Forfeit if the player sending the Forfeit
    * matches the player we're playing against.
    */
   protected void receive()
   {
      final ConnectionState connectionState = playerNode.getConnectionState();

      if(connectionState.getPlaying())
      {
         Endpoint forfeitingEndpoint = new Endpoint(Message.parseArg(ParameterNames.URL, soap), Message.parseArg(ParameterNames.UserName, soap));
         Endpoint currentPlayingEndpoint = connectionState.getPlayingEndpoint();

         boolean isForfeitFromOpponent = forfeitingEndpoint.url.equals(currentPlayingEndpoint.url);
         if(isForfeitFromOpponent)
         {
            playerNode.endGame(player.ui.GUI.MessageType.INFORMATION, forfeitingEndpoint.userName + " has forfeited! You Win!", forfeitingEndpoint.userName + " has forfeited! You Win!");
         }
      }
   }

   /*
    * The following is an example of the SOAP of a Forfeit Message
    *
    * <?xml version="1.0" encoding="UTF-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/so
     * ap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSche
     * ma-instance"><soapenv:Body><forfeit soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/
     * "><UserName xsi:type="xsd:string">Bob</UserName><URL xsi:type="xsd:string">http://192.168.1.64:9091<
     * /URL></forfeit></soapenv:Body></soapenv:Envelope>
     */
}