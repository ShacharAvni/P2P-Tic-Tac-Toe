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

/*
 * The FileSystem class is a utility class for functions involving files.
 */

public final class FileSystem
{
   /*
    * Get the absolute path of the jar file where Class c resides
    */
   public static String getExecutingDirectory(Class<?> c)
   {
      try
      {
         String fullPathToJar = c.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

         int indexOfLastSlash = fullPathToJar.lastIndexOf('/');

         return fullPathToJar.substring(0, indexOfLastSlash + 1);
      }
      catch (java.net.URISyntaxException e)
      {
         Logger.logError("Failed to get the executing directory", e);
         return "";
      }
   }
}