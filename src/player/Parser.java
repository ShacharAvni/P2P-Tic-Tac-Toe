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

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Vector;

/*
 * Parser is a utility class that handles String parsing.
 */

public final class Parser
{
   /*
    * Determines if the String s has content that matches the Regular Expression
    * in regex.
    */
   public static boolean stringMatches(String s, String regex)
   {
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(s);

      return m.find();
   }

   /*
    * Obtains the first match of the Regular Expression regex in the String s.
    * If there is no match, then the empty String is returned.
    */
   public static String findFirstMatch(String s, String regex)
   {
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(s);

      if (m.find())
      {
         return m.group();
      }
      else
      {
         // pattern not found
         return "";
      }
   }

   /*
    * Obtains the nth group of the matched Regular Expression regex of the
    * String s if possible. If s is not matched by regex, then the empty String
    * is returned. groupIndex is the index of the group to be returned. This
    * method does not check if it is a valid index.
    *
    * e.g.
    *
    * Suppose: s = "name@example.com" regex = "(\w.+?)@(\w.+?)\.(\w.+?)
    *
    * This regular expression validates a subset of all possible e-mail
    * addresses. Namely, those having only letters, digits, and an underscore in
    * the local part and domain, and having only one '@' and '.' characters in
    * the usual locations.
    *
    * If groupIndex is 0, then this method returns: "name@example.com" If
    * groupIndex is 1, then this method returns: "name" If groupIndex is 2, then
    * this method returns: "example" If groupIndex is 3, then this method
    * returns: "com"
    */
   public static String findGroup(String s, String regex, int groupIndex)
   {
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(s);

      if (m.find())
      {
         return m.group(groupIndex);
      }
      else
      {
         // pattern not found
         return "";
      }
   }

   /*
    * Finds all matches of the Regular Expression, regex in the String, s and
    * returns them as a Vector. The empty Vector is returned if there are no
    * matches.
    */
   public static Vector<String> findAllMatches(String s, String regex)
   {
      Pattern p = Pattern.compile(regex);
      Matcher m = p.matcher(s);

      Vector<String> matches = new Vector<String>();

      while (m.find())
      {
         matches.add(m.group());
      }

      return matches;
   }

   /*
    * This method parses out the player names from the player XML received from
    * the Registry into a Vector.
    *
    * The XML is of the form: <players> <player>User Name 1</player>
    * <player>User Name 2</player> <player>User Name 3</player> ... </players>
    */
   public static Vector<String> getPlayerListFromXML(String playerXML)
   {
      String startPlayerTag = "<player>";
      String endPlayerTag = "</player>";

      // Get all tags of the form <player>(something)</player>
      Vector<String> fullPlayerTags = findAllMatches(playerXML, startPlayerTag + ".+?" + endPlayerTag);

      Vector<String> vectorOfPlayers = new Vector<String>();

      // For each tag received above, place the (something) into the Vector
      for (String fullPlayerTag : fullPlayerTags)
      {
         vectorOfPlayers.add( fullPlayerTag.substring(startPlayerTag.length(), fullPlayerTag.length() - endPlayerTag.length()) );
      }

      return vectorOfPlayers;
   }

   /*
    * Determines if the String url is a valid URL.
    */
   public static boolean isValidURL(String url)
   {
      boolean isValid = true;

      try
      {
         new java.net.URL(url);
      }
      catch (java.net.MalformedURLException e)
      {
         isValid = false;
      }

      return isValid;
   }

   /*
    * Takes in a URL and completes it by placing "http://" in front, if need be.
    */
   public static String completeURL(String url)
   {
      String completedURL = url;
      completedURL = completedURL.trim();

      if (!completedURL.startsWith("http://"))
      {
         completedURL = "http://" + completedURL;
      }

      return completedURL;
   }
}