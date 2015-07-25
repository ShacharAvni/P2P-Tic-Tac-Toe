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
 * ConfirmationData is a basic storage class that encapsulates
 * content of a Confirmation Message (see: Confirmation.java).
 */
public final class ConfirmationData
{
   public boolean confirmed; // true if the Confirmation is "yes"
   public boolean hasFirstMove; // true if the player receiving the Confirmation gets the first move

   public ConfirmationData(boolean confirmationIsYes, boolean receivingPlayerHasFirstMove)
   {
      confirmed = confirmationIsYes;
      hasFirstMove = receivingPlayerHasFirstMove;
   }
}