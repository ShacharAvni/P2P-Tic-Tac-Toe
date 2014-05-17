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

/*
 * ParameterNames encapsulates the names of the parameters
 * that are sent and received as part of Message transmission
 * (see: Message.java).
 */

public final class ParameterNames
{
   public static final String UserName = "UserName";
   public static final String URL = "URL";
   public static final String Answer = "Answer";
   public static final String HasFirstMove = "HasFirstMove";
   public static final String MoveXML = "MoveXML";
   public static final String Topic = "Topic";
}