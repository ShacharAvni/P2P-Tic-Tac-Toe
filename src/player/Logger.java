/*******************************************************************************************************\
*                                                                                                       *
*   This file is part of P2P Tic-Tac-Toe                                                                *
*                                                                                                       *
*   Copyright (c) 2014 Shachar Avni. All rights reserved.                                               *
*                                                                                                       *
*   Use of this file is governed by a BSD-style license. See the accompanying LICENSE.txt for details   *
*                                                                                                       *
\*******************************************************************************************************/

package player;

import java.io.*;
import java.util.Date;

 /*
  * The Logger class is responsible for logging errors.
  */

public final class Logger
{
   /*
    * Utility method for logging a message when an error has occurred.
    */
   public static void logError(String message, final Exception exception)
   {
      try
      {
         BufferedWriter errorLog = new BufferedWriter(new FileWriter("errorLog.txt", true));

         errorLog.write( (new Date()).toString() );
         errorLog.newLine();

         errorLog.write(message);
         errorLog.newLine();

         //write the stack trace to the error log (if applicable)
         if(exception != null)
         {
            StackTraceElement[] stackTrace = exception.getStackTrace();

            for(StackTraceElement stackTraceElement : stackTrace)
            {
               errorLog.write(stackTraceElement.toString());
               errorLog.newLine();
            }
         }

         errorLog.newLine();

         errorLog.close();
      }
      catch(IOException e)
      {
         //we probably don't have a console but it doesn't hurt to
         //try writing this anyway
         System.err.println("Error printing the following error to the log file:");
         System.err.println(message);

         if(exception != null)
         {
            exception.printStackTrace();
         }

         System.err.println("");
         System.err.println("Because of: " + e.getMessage());
         e.printStackTrace();
      }
   }

   /*
    * Utility method for logging a message when an unrecoverable error has occurred.
    * The program is abruptly terminated.
    */
   public static void fatalError(String message, final Exception exception)
   {
      logError("Fatal Error: " + message, exception);
      System.exit(1);
   }
}