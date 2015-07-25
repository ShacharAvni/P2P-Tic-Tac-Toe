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
 * A Move Message is sent when the user has made a move
 * in the Tic-Tac-Toe match.
 */

public class Move extends Message
{
   /*
    * Move Constructor.
    */
   public Move()
   {
      super();
   }

   /*
    * Move Constructor (when receiving a Move as the SOAP, soapMsg).
    */
   public Move(String soapMsg)
   {
      super(soapMsg);
   }

   /*
    * See description of isSelf in Message.java
    */
   public static boolean isSelf(String soapMsg)
   {
      return player.Parser.stringMatches(soapMsg, "<soapenv:Body><move");
   }

   /*
    * Parses the column ("left", "middle", or "right") if coord is "x", or row
    * ("top", "middle", or "bottom"), if coord is "y", out of the moveXML.
    */
   private static String parseFromMove(String coord, String moveXML)
   {
      //The moveXML is of the form <move x='left|middle|right' y='top|middle|bottom' />
      //We are interested in extracting either the (left|middle|right) or the (top|middle|bottom)
      //depending on the coord parameter.
      return player.Parser.findGroup(moveXML, coord + "='(.+?)'", 1);
   }

   /*
    * Sends a Move Message to the player listening at "endpoint". "data" contains
    * the moveXML, which is construced in Board.java. moveXML is a single tag and
    * it is of the form <move x='left|middle|right' y='top|middle|bottom' />
    */
   @Override
   protected void send(final Endpoint endpoint, final Object data)
   {
      // send a Move in another thread. Store current user name
      // in case it gets changed in the interim.
      final String snapshotUserName = playerNode.getConnectionState().getUserName();

      PlayerNode.startThread(new Runnable()
      {
         @Override
         public void run()
         {
            try
            {
               // Move is implemented as a web service function (called "move") that returns void and takes three
               // parameters: (1) the XML representing the move (this XML is constructed in Board.java), (2) the
               // user name, and (3) the URL of the player sending the Move (all Strings)
               Object params[] = { data.toString(), snapshotUserName, playerNode.getListeningURL() };
               String paramNames[] = { ParameterNames.MoveXML, ParameterNames.UserName, ParameterNames.URL };

               Sender.callWebserviceFunction(endpoint.url, "move", 3, XMLType.XSD_ANYTYPE, params, paramNames);
            }
            catch (StrangeAxisException e)
            {
            }
            catch (WebServiceException e)
            {
               // Error in sending the move. Show an error if this move is still relevant.
               ConnectionState acquiredConnectionState = playerNode.getAndAcquireConnectionState();

               try
               {
                  boolean moveIsStillRelevant = acquiredConnectionState.getPlaying() && acquiredConnectionState.getPlayingEndpoint().equals(endpoint);
                  if (moveIsStillRelevant)
                  {
                     String showMessage = "Error in sending move to: " + endpoint.userName + "\nThey may have left or their connection is down\nYou have been disconnected from the game";
                     playerNode.endGame(player.ui.GUI.MessageType.ERROR, showMessage, "Error in sending move to: " + endpoint.userName);
                  }
               }
               finally
               {
                  acquiredConnectionState.release();
               }
            }
         }
      });
   }

   /*
    * Receives a Move Message. The moveXML as well as the user name and url of
    * the player sending the Move is parsed from the SOAP. If the Move is from
    * the player we're currently playing against, then the x and y (column and
    * row) is parsed from the moveXML and sent to the PlayerNode.
    */
   @Override
   protected void receive()
   {
      Endpoint sendingMoveEndpoint = new Endpoint(Message.parseArg(ParameterNames.URL, soap), Message.parseArg(ParameterNames.UserName, soap));

      boolean isMoveFromExpectedPlayer = sendingMoveEndpoint.url.equals(playerNode.getConnectionState().getPlayingEndpoint().url);
      if (isMoveFromExpectedPlayer)
      {
         // parse the moveXML out of the SOAP
         String moveXML = Message.parseArg(ParameterNames.MoveXML, soap);

         // obtain the column and row of the incoming move and forward
         // it to the PlayerNode (described in PlayerNode.java)
         String xMove = parseFromMove("x", moveXML);
         String yMove = parseFromMove("y", moveXML);

         playerNode.concedeMove(xMove, yMove);
      }
   }

   /*
    * The following is an example of the SOAP of a Move Message
    *
    * <?xml version="1.0" encoding="UTF-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/so
    * ap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSche
    * ma-instance"><soapenv:Body><move soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"><
    * MoveXML xsi:type="xsd:string">&lt;move x='left' y='top' /&gt;</MoveXML><UserName xsi:type="xsd:strin
    * g">Bob</UserName><URL xsi:type="xsd:string">http://192.168.1.64:9091</URL></move></soapenv:Body></so
    * apenv:Envelope>
    */
}