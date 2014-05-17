/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player.game;

import player.networking.node.PlayerNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/*
 * The Board class represents the Tic-Tac-Toe game board. It is responsible
 * for maintaining the game state and for updating the PlayerNode (see: PlayerNode.java)
 * whenever the player makes a Move (see: Move.java) and when the game is over. It is
 * also responsible for drawing itself.
 */

public final class Board extends JPanel
{
   private static final long serialVersionUID = 1L;

   //This reference is used for forwarding the user's moves as well as
   //for sending end of game notifications
   private PlayerNode playerNode;

   //This flag is true when it's the user's turn
   private boolean acceptingMoves;

   //piece constants
   private final int PIECE_X = 0;
   private final int PIECE_O = 1;

   //which piece the user is playing with (either X or O)
   private int playerPiece;

   //grid constants
   private final int NUM_ROWS = 3;
   private final int NUM_COLUMNS = 3;
   private final int NUM_CELLS = NUM_ROWS * NUM_COLUMNS;

   //constants for drawing
   private final int BLOCK_WIDTH = 130; //the width (and height) of a square in the grid
   private final int X_OFFSET = 55;     //the number of pixels to the left of the board from the left of the JPanel's drawing area
   private final int Y_OFFSET = 5;      //the number of pixels to the top of the board from the top of the JPanel's drawing area
   private final int INNER_OFFSET = 10; //the number of pixels from the left (or top) of a square in the grid to the left (or top)
                                        //of where the piece is to be drawn. This is so the drawn piece does not touch the grid lines
   private final int PIECE_WIDTH = BLOCK_WIDTH - 2 * INNER_OFFSET; //the width (and height) of a game piece

   /*
    * A Cell represents a location in the Tic-Tac-Toe grid. The locations are indexed from 0 (top-left)
    * to 8 (bottom-right). A Cell stores its index in the grid and knows how to get the XML (hereafter MoveXML)
    * required for a Move Message (see: Move.java) corresponding to its index.
    */

   private enum Cell
   {
      //Individual Cell constants
         TOP_LEFT(0),    TOP_MIDDLE(1),    TOP_RIGHT(2),
      MIDDLE_LEFT(3), MIDDLE_MIDDLE(4), MIDDLE_RIGHT(5),
      BOTTOM_LEFT(6), BOTTOM_MIDDLE(7), BOTTOM_RIGHT(8);

      //index into the grid
      private int index;

      /*
       * Cell constructor
       */
      Cell(int idx)
      {
         index = idx;
      }

      /*
       * Getter
       */
      int getIndex()
      {
         return index;
      }

      /*
       * Utility method for retrieving the MoveXML from the name of the cell. "cellName" is
       * one of {"TOP_LEFT", "TOP_MIDDLE", ... , "BOTTOM_RIGHT"}
       */
      static String toMoveXML(String cellName)
      {
         //get the y value for this name (either top, middle, or bottom)
         String yVal = player.Parser.findGroup(cellName, "(.*)_", 1).toLowerCase();

         //get the x value for this name (either left, middle, or right)
         String xVal = player.Parser.findGroup(cellName, ".*_(.*)", 1).toLowerCase();

         //construct the XML tag of the MoveXML
         return "<move x='" + xVal + "' y='" + yVal + "' />";
      }

      /*
       * Utility method for retrieving the MoveXML corresponding to the given grid index
       */
      static String toMoveXML(int gridIndex)
      {
         return toMoveXML(Cell.values()[gridIndex].toString());
      }
   }

   //the actual board data. Indices in this array match the indices of the cells.
   //The element at index i stores which piece (if any) is in cell i. A null entry
   //means the cell is empty
   private Integer[] moves;

   //the total number of moves (for both players) that have occurred so far in the current match
   private int numMoves;

   //The following member variable (after the explanatory paragraph) stores all the possible winning
   //combinations of cells. This list is iterated over to check for a possible victor after each move
   //that can possibly end the match.
   /*
    * There are more optimal solutions for this, such as maintaining counters for each row, column
    * and diagonal (e.g. if one of the counters is at 3 then the current player wins, if one of the
    * counters is at -3 then the opposing player wins). I implemented the above solution and found that
    * it added a degree of obfuscation to the code. Also, there is little chance that the board will
    * increase in size so the unscalability of the current solution is not a concern. Speed is not a
    * concern either as checking (up to) 8 combinations for each possible game ending move is peanuts.
    * Thus, I decided to stay with the more legible, brute force solution.
    */
   private final int[][] winningCombinations = { //horizontals
                                         {Cell.TOP_LEFT.getIndex()   , Cell.TOP_MIDDLE.getIndex()   , Cell.TOP_RIGHT.getIndex()},
                                         {Cell.MIDDLE_LEFT.getIndex(), Cell.MIDDLE_MIDDLE.getIndex(), Cell.MIDDLE_RIGHT.getIndex()},
                                         {Cell.BOTTOM_LEFT.getIndex(), Cell.BOTTOM_MIDDLE.getIndex(), Cell.BOTTOM_RIGHT.getIndex()},

                                         //verticals
                                         {Cell.TOP_LEFT.getIndex()   , Cell.MIDDLE_LEFT.getIndex()  , Cell.BOTTOM_LEFT.getIndex()},
                                         {Cell.TOP_MIDDLE.getIndex() , Cell.MIDDLE_MIDDLE.getIndex(), Cell.BOTTOM_MIDDLE.getIndex()},
                                         {Cell.TOP_RIGHT.getIndex()  , Cell.MIDDLE_RIGHT.getIndex() , Cell.BOTTOM_RIGHT.getIndex()},

                                         //diagonals
                                         {Cell.TOP_LEFT.getIndex()   , Cell.MIDDLE_MIDDLE.getIndex(), Cell.BOTTOM_RIGHT.getIndex()},
                                         {Cell.TOP_RIGHT.getIndex()  , Cell.MIDDLE_MIDDLE.getIndex(), Cell.BOTTOM_LEFT.getIndex()} };

   /*
    * Board Constructor
    */
   public Board(PlayerNode p)
   {
      //initialize JPanel base class
      super();

      playerNode = p;
      acceptingMoves = false; //The PlayerNode updates this when it's the user's turn
      playerPiece = PIECE_X;

      moves = new Integer[NUM_CELLS];
      clear();

      //so we can see the background behind the board
      setOpaque(false);

      //Start listening for when the player clicks on the board
      addMouseListener(new MouseAdapter()
      {
         public void mouseClicked(MouseEvent e)
         {
            if(acceptingMoves)
            {
               processMouseClick(e.getX(), e.getY());
            }
         }
      });
   }

   /*
    * Setter
    */
   public void setAcceptingMoves(boolean isAcceptingMoves)
   {
      acceptingMoves = isAcceptingMoves;
   }

   /*
    * Clears the board.
    */
   public void clear()
   {
      for(int i = 0; i < NUM_CELLS; i++)
      {
         moves[i] = null;
      }

      numMoves = 0;
   }

   /*
    * Helper method that gets the piece opposite to the one passed in.
    */
   private int oppositePiece(int piece)
   {
      return (piece == PIECE_X) ? PIECE_O : PIECE_X;
   }

   /*
    * Goes over the whole board and changes all X's to O's and all
    * O's to X's. This gets called when the user changes their
    * piece.
    */
   private void toggleBoard()
   {
      for(int i = 0; i < NUM_CELLS; i++)
      {
         if(moves[i] != null)
         {
            moves[i] = new Integer( oppositePiece(moves[i].intValue()) );
         }
      }

      drawBoard();
   }

   /*
    * Changes the player's piece if need be. "piece" is either "X" or "O".
    */
   public void setPlayerPiece(String piece)
   {
      int newPlayerPiece = piece.toUpperCase().equals("X") ? PIECE_X : PIECE_O;

      if(playerPiece != newPlayerPiece)
      {
         //change the player's piece
         toggleBoard();
         playerPiece = newPlayerPiece;
      }
   }

   /*
    * Checks if the player holding "checkPiece" has won the game.
    */
   private boolean checkForWinner(int checkPiece)
   {
      //iterate through all the winning combinations of cells. If the player controls
      //all cells in at least one of these combinations, then they have won.
      for(int i = 0; i < winningCombinations.length; i++)
      {
         boolean winnerForCurrentCombination = true;

         //check the next combination. Don't need to keep checking if one of the cells
         //is not controlled by this player
         for(int j = 0; (j < winningCombinations[i].length) && winnerForCurrentCombination; j++)
         {
            if( (moves[winningCombinations[i][j]] == null) || (moves[winningCombinations[i][j]].intValue() != checkPiece) )
            {
               winnerForCurrentCombination = false;
            }
         }

         if(winnerForCurrentCombination)
         {
            return true;
         }
      }

      return false;
   }

   /*
    * Adds a piece to the board. cellIndex is the index of the cell where the
    * piece is to be placed. If isUsersMove is true, then the user's piece is
    * placed. Otherwise, the opponent's piece is placed.
    */
   private void addMove(int cellIndex, boolean isUsersMove)
   {
      if(moves[cellIndex] == null)
      {
         int whichPiece = isUsersMove ? playerPiece : oppositePiece(playerPiece);

         //place the piece
         moves[cellIndex] = new Integer(whichPiece);
         numMoves++;

         acceptingMoves = !isUsersMove; //change turns
         drawBoard();

         if(isUsersMove)
         {
            //send this move to the opponent
            playerNode.sendMove(Cell.toMoveXML(cellIndex));
         }

         if(numMoves >= 5)
         {
            //check for end of match (can't possibly happen with less than 5 moves)

            if(checkForWinner(whichPiece))
            {
               //the player who had this turn has won the match

               if(isUsersMove)
               {
                  playerNode.won();
               }
               else
               {
                  playerNode.lost();
               }
            }
            else if(numMoves == NUM_CELLS)
            {
               playerNode.catsGame();
            }
         }
      }
   }

   /*
    * Computes the index of the cell that the given mouse point is in, if any.
    * Returns -1 if the mouse point is not within the board.
    */
   private int getCellIndexFromMousePoint(int x, int y)
   {
      if( (x >= X_OFFSET) && (x <= (X_OFFSET + NUM_COLUMNS * BLOCK_WIDTH)) &&
          (y >= Y_OFFSET) && (y <= (Y_OFFSET +    NUM_ROWS * BLOCK_WIDTH)) )
      {
         //mouse point is in the board

         int cellColumn = (x - X_OFFSET) / BLOCK_WIDTH; //cellColumn is 0, 1, or 2 for left column, middle column, and right column, respectively
         int cellRow = (y - Y_OFFSET) / BLOCK_WIDTH; //cellRow is 0, 1, or 2 for top row, middle row, and bottom row, respectively

         //compute the cell index from the row and column
         return (cellRow * NUM_COLUMNS) + cellColumn;
      }
      else
      {
         //mouse point is not in the board
         return -1;
      }
   }

   /*
    * Determines the cell corresponding to a user's mouse click (x, y) and
    * places the user's piece there if it is a valid cell. This method only
    * gets called if it's the user's turn.
    */
   private void processMouseClick(int x, int y)
   {
      int cellIndex = getCellIndexFromMousePoint(x, y);

      if(cellIndex != -1)
      {
         addMove(cellIndex, true);
      }
   }

   /*
    * Given an incoming move by the opponent (notified by the PlayerNode), determines
    * the corresponding cell and adds the opponents piece there. x is one of "left",
    * "middle", or "right" and y is one of "top", "middle", or "bottom".
    */
   public void addOpponentMove(String x, String y)
   {
      //get the name of the cell (TOP_LEFT, BOTTOM_RIGHT, etc.)
      String cellName = y.toUpperCase() + "_" + x.toUpperCase();

      int cellIndex = Cell.valueOf(cellName).getIndex();

      addMove(cellIndex, false);
   }

   /*
    * Forces the board to be redrawn. This is called by the GUI (see: GUI.java) as
    * well as by this class when the board is toggled or when there has been a move.
    */
   public void drawBoard()
   {
      repaint();
   }

   /*
    * Draws the board.
    */
   protected void paintComponent(Graphics g)
   {
      //Tell the base class (JPanel) to do what needs to be done
      super.paintComponent(g);

      //Tic-Tac-Toe hash and pieces are black
      g.setColor(new Color((float)0, (float)0, (float)0));

      //draw Tic-Tac-Toe hash
      g.drawLine(X_OFFSET + BLOCK_WIDTH, Y_OFFSET, X_OFFSET + BLOCK_WIDTH,  Y_OFFSET + (3 * BLOCK_WIDTH)); //left vertical line
      g.drawLine(X_OFFSET + (2 * BLOCK_WIDTH), Y_OFFSET, X_OFFSET + (2 * BLOCK_WIDTH), Y_OFFSET + (3 * BLOCK_WIDTH)); //right vertical line
      g.drawLine(X_OFFSET, Y_OFFSET + BLOCK_WIDTH, X_OFFSET + (3 * BLOCK_WIDTH), Y_OFFSET + BLOCK_WIDTH); //top horizontal line
      g.drawLine(X_OFFSET, Y_OFFSET + (2 * BLOCK_WIDTH), X_OFFSET + (3 * BLOCK_WIDTH), Y_OFFSET + (2 * BLOCK_WIDTH)); //bottom horizontal line

      //draw the pieces (if any)
      if(numMoves > 0)
      {
         for(int i = 0; i < moves.length; i++)
         {
            if(moves[i] != null)
            {
               //there's a piece in this cell, so draw it

               int x = i % NUM_COLUMNS; //x is either 0, 1, or 2, for left column, middle column, or right column respectively
               int y = i / NUM_COLUMNS; //y is either 0, 1, or 2, for top row, middle row, or bottom row respectively

               int left = X_OFFSET + (x * BLOCK_WIDTH) + INNER_OFFSET; //left is the number of pixels from the left of the JPanel's drawing area to the left of the piece
               int top  = Y_OFFSET + (y * BLOCK_WIDTH) + INNER_OFFSET; //top is the number of pixels from the top of the JPanel's drawing area to the top of the piece

               if(moves[i].intValue() == PIECE_X)
               {
                  //draw an X
                  g.drawLine(left, top, left + PIECE_WIDTH, top + PIECE_WIDTH); // the \ part of the X
                  g.drawLine(left, top + PIECE_WIDTH, left + PIECE_WIDTH, top); // the / part of the X
               }
               else
               {
                  //draw an O
                  g.drawOval(left, top, PIECE_WIDTH, PIECE_WIDTH);
               }
            }
         }
      }
   }
}