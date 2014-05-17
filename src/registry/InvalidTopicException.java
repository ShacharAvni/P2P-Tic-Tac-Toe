/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package registry;

import java.io.Serializable;

/*
 * An InvalidTopicException is thrown by the Registry when a player passes
 * in to a Registry method a topic name that is not supported.
 */

public class InvalidTopicException extends Exception implements Serializable
{
   private static final long serialVersionUID = 1L;

   /*
    * InvalidTopicException Constructor
    */
   public InvalidTopicException()
   {
      super("Invalid Topic");
   }
}