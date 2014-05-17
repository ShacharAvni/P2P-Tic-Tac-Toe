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
 * A NonUniqueUsernameException is thrown by the Registry when a Player tries to
 * join the Registry but provides a user name that is already in use.
 */

public class NonUniqueUsernameException extends Exception implements Serializable
{
   private static final long serialVersionUID = 1L;

   /*
    * NonUniqueUsernameException Constructor
    */
   public NonUniqueUsernameException()
   {
      super("Non Unique User");
   }
}