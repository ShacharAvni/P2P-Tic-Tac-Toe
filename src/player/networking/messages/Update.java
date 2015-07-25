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

import player.networking.Endpoint;

/*
 * An Update Message is sent by the Registry, notifying the player that
 * someone has either joined or left the Tic-Tac-Toe network.
 */

public class Update extends Message
{
   /*
    * Update Constructor.
    */
   public Update()
   {
      super();
   }

   /*
    * Update Constructor (when receiving an Update as the SOAP, soapMsg).
    */
   public Update(String soapMsg)
   {
      super(soapMsg);
   }

   /*
    * See description of isSelf in Message.java
    */
   public static boolean isSelf(String soapMsg)
   {
      return player.Parser.stringMatches(soapMsg, "<soapenv:Body><update");
   }

   /*
    * Sends an Update Message. (The Update is sent by the Registry, not
    * by the player).
    */
   @Override
   protected void send(final Endpoint endpoint, final Object data)
   {
   }

   /*
    * Receives an Update Message. The topic (either "join" or "leave") and the user name of the player
    * that either joined or left is parsed from the SOAP. This info is simply passed to the playerNode.
    */
   @Override
   protected void receive()
   {
      playerNode.update(Message.parseArg(ParameterNames.Topic, soap), Message.parseArg(ParameterNames.UserName, soap));
   }

   /*
    * The following is an example of the SOAP of an Update Message
    *
    * <?xml version="1.0" encoding="UTF-8"?><soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/so
    * ap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSche
    * ma-instance"><soapenv:Body><update soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"
    * ><Topic xsi:type="xsd:string">join</Topic><UserName xsi:type="xsd:string">Bob</UserName></update></s
    * oapenv:Body></soapenv:Envelope>
    */
}