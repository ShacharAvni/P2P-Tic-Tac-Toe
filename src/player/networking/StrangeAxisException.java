/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player.networking;

import java.io.Serializable;

/*
 * A StrangeAxisException (SAE) is an Exception that intermittently
 * gets thrown after certain web service function calls through Axis. The
 * SAE is thrown as a java.rmi.RemoteException. Strangely, this Exception
 * only gets thrown after certain transactions. Even more strangely, this
 * "Exception" doesn't seem to have any effect on the sent data. Simply
 * ignoring this Exception does not seem to cause any adverse effects
 * to the system. Note that this Exception is swallowed in all but one
 * case (see the connectToRegistry method in PlayerNode.java).
 */

public class StrangeAxisException extends Exception implements Serializable
{
   private static final long serialVersionUID = 1L;

   /*
    * StrangeAxisException constructor
    */
   public StrangeAxisException(String msg)
   {
      super(msg);
   }

   /*
    * StrangeAxisException constructor
    */
   public StrangeAxisException()
   {
      super("StrangeAxisException");
   }
}