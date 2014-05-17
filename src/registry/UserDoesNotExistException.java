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
 * A UserDoesNotExistException is thrown by the Registry when a Player requests
 * the URL of another player whose user name is not in the list of currently
 * connected players.
 */

public class UserDoesNotExistException extends Exception implements Serializable
{
   private static final long serialVersionUID = 1L;

   /*
    * UserDoesNotExistException Constructor
    */
   public UserDoesNotExistException()
   {
      super("User Does Not Exist");
   }
}