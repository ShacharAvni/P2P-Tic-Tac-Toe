/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                               *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player.networking.messages;

import player.networking.node.PlayerNode;
import player.networking.Endpoint;

/*
 * The Message class represents a message that can be sent from the PlayerNode
 * (described in PlayerNode.java) and received by the Receiver (described in
 * Receiver.java) from another PlayerNode in the network or from the Registry. A
 * Message knows how to send and receive itself and it knows how to recognize itself
 * given a SOAP string. Messages are transmitted using Axis, which actually forms
 * their SOAP string. Message is an abstract base class. Its derived classes are:
 * Confirmation, Forfeit, Invitation, Move, Response, and Update (see their respective
 * .java files for more info).
 */

public abstract class Message
{
   protected String soap; //the SOAP of this Message (this is retrieved by the Receiver)
   protected PlayerNode playerNode; //the PlayerNode that this Message is sent from or received to

   /*
    * Message Constructor.
    */
   public Message()
   {
   }

   /*
    * Message Constructor (when receiving a Message as the SOAP, soapMsg).
    */
   public Message(String soapMsg)
   {
      soap = soapMsg;
   }

   /*
    * Parses an argument from a SOAP string.
    */
   protected static String parseArg(String paramName, String soapMsg)
   {
      //an argument is of the form <paramName xsi:type="xsd:string">(something)</paramName>
      //We are interested in parsing out the (something)

      return player.Parser.findGroup(soapMsg, "<" + paramName + " xsi:type=\"xsd:string\">" + "([^<]*?)" + "</" + paramName + ">", 1);
   }

   /*
    * Determines if SOAP is a SOAP string corresponding to this Message.
    * This method would be abstract but static abstract methods are not
    * allowed in Java.
    */
   public static boolean isSelf(String soapMsg)
   {
      return false;
   }

   /*
    * Sends the Message containing data to the player listening at endpoint.
    * In most cases, "data" is a simple String. In the case of the Invitation
    * Message, "data" is null. In the case of the Confirmation Message, "data"
    * is an instance of ConfirmationData (see: ConfirmationData.java)
    */
   protected abstract void send(final Endpoint endpoint, final Object data);

   /*
    * Receives the Message.
    */
   protected abstract void receive();

   /*
    * Sets the PlayerNode this Message will forward the results to and sends
    * the Message. This method is the "visit" method in the visitor design
    * pattern. The PlayerNode "visits" the Message.
    */
   public void send(PlayerNode pNode, final Endpoint endpoint, final Object data)
   {
      playerNode = pNode;
      send(endpoint, data);
   }

   /*
    * Sets the PlayerNode this Message will forward the results to and receives
    * the Message. This method is the "visit" method in the visitor design
    * pattern. The PlayerNode "visits" the Message.
    */
   public void receive(PlayerNode pNode)
   {
      playerNode = pNode;
      receive();
   }
}