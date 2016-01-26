package servlets;

import static utilities.Utilities.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import models.*;
import models.Card.Colour;
import models.SubGame.Direction;
//import models.CardList;
//import models.Game;
//import models.GamesMap;
//import models.Player;
//import models.SubGame;
//import models.User;

@WebServlet("/playSubGame")
public class PlaySubGameServlet extends HttpServlet {

    @Inject
    GamesMap gamesMap;

    RequestDispatcher rd = null;

    // need the ArrayList, else NullPointerException on add
    public List<Game> gamesList = new ArrayList();
//    public Map<Long, Game> gamesMap = new HashMap<>();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

//        String strGameId, strTableNo;
        HttpSession session = req.getSession();
//        ServletContext appScopeServlet = req.getServletContext();
        Long lonMapGameId;
        int i, roundNo = 0, roundMoveNo = 1
                , drawPileCount = 0, discardPileCount = 0, loginPlayerIdx = 0
                , matchingCardsCount = 0, addUpHandPoints = 0;
        Boolean loadError = false
                , afterDrawingCardFromDrawPile = false
                , loginPlayerTurn = false; // adcfdp

        User loginUser;
        Game currentGame;
        SubGame currentSubGame;
        Player turnPlayer = null, loginPlayer, currentPlayer = null;
        Card topDiscardPileCard = null;

        String strMapGameId, strGameName, loginUserName;
        String strRoundNo = "X";

        List<Player> currentSubGamePlayers;

        loginUser = (User) session.getAttribute("loginuser");
        loginUserName = loginUser.getUsername();

        strMapGameId = (String) session.getAttribute("mapGameId");
        System.out.println("> loginUser=" + loginUserName + "\t sesAtt mapGameId=" + strMapGameId);

        lonMapGameId = Long.valueOf(strMapGameId);
        currentGame = gamesMap.get(lonMapGameId);
        strGameName = currentGame.getGameName();

        currentSubGame = currentGame.getCurrentSubGame();
        
        if(null != req.getAttribute("doDraw")) { // from returned drawCard
            afterDrawingCardFromDrawPile = (Boolean) req.getAttribute("doDraw");
//            session.setAttribute("doDraw", afterDrawingCardFromDrawPile);
            if(afterDrawingCardFromDrawPile)
                resp.setHeader("doDraw", afterDrawingCardFromDrawPile.toString());
        } else if(null != req.getParameter("adcfdp")) { // from refresh setHeader
            if(req.getParameter("adcfdp").equals("true")) {
                afterDrawingCardFromDrawPile = true;
            } else if(req.getParameter("adcfdp").equals("false")) {
                afterDrawingCardFromDrawPile = false;
            } else {
                System.out.println("### ERROR: req.getParameter(\"adcfdp\") = " + req.getParameter("adcfdp"));
            }
//            System.out.println(">>> req.getParameter(\"adcfdp\") = " + req.getParameter("adcfdp"));
        } else if(null != req.getAttribute("adcfdp")) { // from returned playCard
            if(req.getAttribute("adcfdp").equals("true")) {
                afterDrawingCardFromDrawPile = true;
            } else if(req.getAttribute("adcfdp").equals("false")) {
                afterDrawingCardFromDrawPile = false;
            } else {
                System.out.println("### ERROR: req.getAttribute(\"adcfdp\") = " + req.getAttribute("adcfdp"));
            }
            System.out.println(">>> req.getAttribute(\"adcfdp\") = " + req.getAttribute("adcfdp"));
        } else {
//            afterDrawingCardFromDrawPile = (Boolean) session.getAttribute("doDraw");
            afterDrawingCardFromDrawPile = resp.containsHeader("doDraw");
            if(afterDrawingCardFromDrawPile)
                resp.setHeader("doDraw", afterDrawingCardFromDrawPile.toString());
        }
        
//        System.out.println("### req.getAttribute(\"adcfdp\") = " + req.getAttribute("adcfdp")); // this was not it
//        System.out.println("### req.getParameter(\"adcfdp\") = " + req.getParameter("adcfdp")); // this worked

        if (null == currentSubGame) {
            // return back to StartGameServLet
            loadError = true;
//            resp.setHeader("Refresh", "0; startGame");
        }

        if (null == currentSubGame.getSubGamePlayers()) {
            loadError = true;
//            resp.setHeader("Refresh", "0; startGame");
        } else {
            currentSubGamePlayers = currentSubGame.getSubGamePlayers();
            roundNo = currentSubGame.getRoundNo();
            turnPlayer = currentSubGame.getCurrentPlayer();
            drawPileCount = currentSubGame.getDrawPile().size();
            discardPileCount = currentSubGame.getDiscardPile().size();
            topDiscardPileCard = currentSubGame.getDiscardPile().getTopCard();
            loginPlayer = currentSubGame.getPlayerFromUserObject(loginUser);
            loginPlayerIdx = currentSubGame.getSubGamePlayers().indexOf(loginPlayer);
            loginPlayerTurn = currentSubGame.getCurrentPlayer().equals(loginPlayer);
            currentPlayer = currentSubGame.getCurrentPlayer();
            matchingCardsCount = loginPlayer.countHowManyMatchingCards(topDiscardPileCard);
            addUpHandPoints = loginPlayer.addUpHandPoints();
        }
        
        if(loadError) {
            System.out.println("### Error currentSubGame=" + currentSubGame.toString());
            System.out.println("### Error currentSubGamePlayers=" + currentSubGame.getSubGamePlayers().toString());
            resp.setHeader("Refresh", "0; startGame");            
        }

        // Set refresh, autoload time as 5 seconds
        if(afterDrawingCardFromDrawPile) {
            resp.setHeader("Refresh", "5; playSubGame?adcfdp=true");        
        } else {
            resp.setHeader("Refresh", "5; playSubGame?adcfdp=false");
        }

        // Set resp content type
        resp.setContentType("text/html");

        String CT = getCurrentTimeString();

        PrintWriter out = resp.getWriter();
        String pageTitle = "Uno Game " + strGameName;
//        String bodyTitle = "Welcome to Table " + TableNo + " " + loginUserName + "!";
        String bodyTitle = loginUserName + " @ Game " + strGameName + " Round " + roundNo;
        String docType
                = "<!doctype html public \"-//w3c//dtd html 4.0 "
                + "transitional//en\">\n";
        out.println(docType
                + "<html>\n"
                + "<head>"
                + "<title>" + pageTitle + "</title>"
                + "</head>\n"
                + "<body bgcolor=\"#f0f0f0\">\n"
                + "<h1 align=\"center\">" + bodyTitle + "</h1>\n"
                + "<hr>"
                + "<p>Current Time is: " + CT + "</p>\n"
                //                + "<a href=\"listGames\">Return to Games Lounge</a><br><br>\n"
                + "<h3>Move No.&nbsp" + roundMoveNo + "&nbspfor Player&nbsp" 
                + turnPlayer.getPlayer().getUsername() + "</h3><br>"
                + "<form method=\"POST\" action=\"playCard\">\n"
                + "<table border=\"1\">\n"
                // Player Avatars row(s)
                + getPlayersRow(currentSubGame)
                // draw and discard row
                + "<tr>\n"
                // draw pile cell
                + getDrawPileCell(drawPileCount, afterDrawingCardFromDrawPile, loginPlayerTurn)
                // empty cell
//                + "<td align=\"center\" valign=\"bottom\">"
                + "<td align=\"center\">"
                + "<table><tr><td align=\"center\" valign=\"bottom\">"
//                + "<h1 style=\"color:red;\">Your<br>Turn<br></h1>"
//                + "You have " + 0 + "<br>playable cards<br>"
                + showMessageToPlayer(loginPlayerTurn, currentPlayer, matchingCardsCount)
                + "</td></tr><tr><td align=\"center\" valign=\"bottom\">"
//                + "Skip Turn - <input type=\"radio\" name=cardChoice value=\"skipTurn\"/>"
                + getSkipTurnBtn(afterDrawingCardFromDrawPile, loginPlayerTurn)
                + "</td></tr></table>"
                + "</td>\n"
                // discard pile cell
                + getDiscardPileCell(discardPileCount, topDiscardPileCard)
                + "</tr>\n"
                // loginPlayer hand row 1
                + getHandRow(loginPlayerIdx, currentSubGame, topDiscardPileCard)
                + "<input type=\"hidden\" name=\"adcfdp\" value=\"" 
                + afterDrawingCardFromDrawPile + "\">" // to use if playCard was made without a cardChoice
                + "</table><br>\n"
                + "You have a total of " + addUpHandPoints + " points in your hand.<br><br>\n"
                + getPlayOrDrawCardBtn(strMapGameId)
                //                + playOrDrawCardBtnVisibility
                + "</form>\n"
                + "</body>\n"
                + "</html>\n"
                + "");

        //        req.getRequestDispatcher("clock5secs.jsp")
        //                .forward(req, resp);
    } // doPost

    private String showMessageToPlayer(Boolean loginPlayerTurn, Player currentPlayer, int matchingCardsCount) {
        String strMessage = ""
                , currentPlayerName = currentPlayer.getPlayer().getUsername();
        
        strMessage = strMessage.concat("<h1 style=\"color:red;\">");
        if(loginPlayerTurn)
            strMessage = strMessage.concat("Your");
        else
            strMessage = strMessage.concat(currentPlayerName);
        strMessage = strMessage.concat("<br>Turn<br></h1>");
        if(loginPlayerTurn)
            strMessage = strMessage.concat("You have " + matchingCardsCount + "<br>playable cards");
        
        return strMessage;
    }
    
    private String showDrawPileRadioButton() {
//        if(!afterDrawingCardFromDrawPile) {
//            return(" - <input type=\"radio\" name=cardChoice value=\"drawPile\"/> Draw");
            return(" <button type=\"button\" onclick=\"location.href='drawCard'\">Draw Card<//button>");            
//        }
//        else
//            return "";
    }
    
    private String getSkipTurnBtn(Boolean afterDrawingCardFromDrawPile, Boolean loginPlayerTurn) {
        if(afterDrawingCardFromDrawPile && loginPlayerTurn) {
//            return ("Skip Turn - <input type=\"radio\" name=cardChoice value=\"skipTurn\"/>");
            return("<br><button type=\"button\" onclick=\"location.href='skipTurn'\">Skip Turn<//button>");            
        }
        else
            return "";
    }
    
    public String getPlayOrDrawCardBtn(String strMapGameId) {
        String playOrDrawCardBtnVisibility = "";
        // show button when its the loginUser's turn
        if (true) {
            playOrDrawCardBtnVisibility = playOrDrawCardBtnVisibility.concat("<button type=\"submit\" name=mapGameId value=\"");
            playOrDrawCardBtnVisibility = playOrDrawCardBtnVisibility.concat(strMapGameId);
            playOrDrawCardBtnVisibility = playOrDrawCardBtnVisibility.concat("\">Play Card</button>\n");
        }
        return playOrDrawCardBtnVisibility;
    }

    public String getHandRow(int loginPlayerIdx, SubGame currentSubGame, Card topDiscardPileCard) {
        int i, cardCount = 0, cellCount = 0; // intRandHandCardCount = getRandomInt(1, 15);
        String strHttpHandRow = "";
        strHttpHandRow = strHttpHandRow.concat("<tr>\n");
        CardList loginPlayerHand = currentSubGame.getSubGamePlayers().get(loginPlayerIdx).getHand();
        cardCount = loginPlayerHand.size();
        Boolean loginPlayerTurn = false;
        if(currentSubGame.getSubGamePlayers().indexOf(currentSubGame.getCurrentPlayer()) == loginPlayerIdx)
            loginPlayerTurn = true;
        
        Double d = Math.floor(cardCount/5);
        cellCount = 5 * (1 + d.intValue());
        
//        System.out.println(">> getHandRow loginPlayerIdx=" + loginPlayerIdx 
//                + " cardCount=" + cardCount 
//                + " cellCount=" + cellCount);
        
        for (i = 0; i < cellCount; i++) {
            if ((i != 0) && (i % 5 == 0)) {
                strHttpHandRow = strHttpHandRow.concat("</tr>\n<tr>\n");
            }
            if(i < cardCount) {
//                strHttpHandRow = strHttpHandRow.concat("<td align=\"center\"><table border=\"1\">\n<tr><td align=\"center\">");
                strHttpHandRow = strHttpHandRow.concat("<td align=\"center\""); //><table>\n<tr><td align=\"center\">");
//                if((loginPlayerTurn) && ((currCardColour == matchColour) || (Objects.equals(currCardValue, matchValue))))
                if(loginPlayerTurn && pairOfCardMatchDeterminator(topDiscardPileCard, loginPlayerHand.getListOfCards().get(i)))
                    strHttpHandRow = strHttpHandRow.concat(" style=\"background-color:LimeGreen\"");
                strHttpHandRow = strHttpHandRow.concat("><table>\n<tr><td align=\"center\">");
    //            strHttpHandRow = strHttpHandRow.concat("<img src=\"images\\uno_deck\\" + getRandomUnoCardFileName() + "\" width=\"85\" height=\"128\" alt=\"Player's Hand Card No. " + i + " Face\">");
                strHttpHandRow = strHttpHandRow.concat("<img src=\"images\\uno_deck\\" 
                        + loginPlayerHand.getListOfCards().get(i).getImgFileName() 
                        + "\" width=\"85\" height=\"128\" alt=\"" 
                        + loginPlayerHand.getListOfCards().get(i).getCardName() + "\">");
                strHttpHandRow = strHttpHandRow.concat("</td></tr><tr><td align=\"center\">");
                strHttpHandRow = strHttpHandRow.concat(loginPlayerHand.getListOfCards().get(i).getCardName());
                if(loginPlayerTurn && pairOfCardMatchDeterminator(topDiscardPileCard, loginPlayerHand.getListOfCards().get(i)))
                    strHttpHandRow = strHttpHandRow.concat(
                        " - <input type=\"radio\" name=cardChoice value=\"" 
                        + loginPlayerHand.getListOfCards().get(i).getCardId() + "\"/>");
            } else {
                strHttpHandRow = strHttpHandRow.concat("<td><table>\n<tr><td align=\"center\">");
    //            strHttpHandRow = strHttpHandRow.concat("<img src=\"images\\uno_deck\\" + getRandomUnoCardFileName() + "\" width=\"85\" height=\"128\" alt=\"Player's Hand Card No. " + i + " Face\">");
                strHttpHandRow = strHttpHandRow.concat("&nbsp");
                strHttpHandRow = strHttpHandRow.concat("</td></tr><tr><td>");
                strHttpHandRow = strHttpHandRow.concat("&nbsp");
            }
            strHttpHandRow = strHttpHandRow.concat("</td></tr></table>\n</td>\n");
        }
        strHttpHandRow = strHttpHandRow.concat("</tr>\n");
        return strHttpHandRow;
    }

    public String getDiscardPileCell(int discardPileCount, Card topDiscardPileCard) {
        String strHttpDiscardPileCell = "";
        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("<td colspan=\"2\" align=\"center\">");
//        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("<table border=\"1\">\n<tr><td align=\"center\">");
        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("<table>\n<tr><td align=\"center\">");
//        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("<img src=\"images\\uno_deck\\" + getRandomUnoCardFileName() + "\" alt=\"Discard Pile Card Face\">");
        strHttpDiscardPileCell = strHttpDiscardPileCell.concat(
                "<img src=\"images\\uno_deck\\" 
                + topDiscardPileCard.getImgFileName() 
                + "\" alt=\"" + topDiscardPileCard.getCardName() + "\">");
        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("</td></tr><tr><td align=\"center\">");
//        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("drawPileCardCount + RadioButton");
        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("Discard Pile (" + discardPileCount + ")");
        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("</td></tr></table>\n");
        strHttpDiscardPileCell = strHttpDiscardPileCell.concat("</td>\n");
        return strHttpDiscardPileCell;
    }

    public String getDrawPileCell(int drawPileCount, Boolean afterDrawingCardFromDrawPile, Boolean loginPlayerTurn) {
        String strHttpDrawPileCell = "";
        strHttpDrawPileCell = strHttpDrawPileCell.concat("<td colspan=\"2\" align=\"center\">");
//        strHttpDrawPileCell = strHttpDrawPileCell.concat("<table border=\"1\">\n<tr><td align=\"center\">");
        strHttpDrawPileCell = strHttpDrawPileCell.concat("<table>\n<tr><td align=\"center\">");
        strHttpDrawPileCell = strHttpDrawPileCell.concat("<img src=\"images\\uno_deck\\back.png\" alt=\"Draw Pile Card Back\">");
        strHttpDrawPileCell = strHttpDrawPileCell.concat("</td></tr><tr><td align=\"center\">");
        strHttpDrawPileCell = strHttpDrawPileCell.concat("Draw Pile (" + drawPileCount + ")");
//        strHttpDrawPileCell = strHttpDrawPileCell.concat(" - <input type=\"radio\" name=cardChoice value=\"drawPile\"/> Draw");
        if(!afterDrawingCardFromDrawPile && loginPlayerTurn) {
            strHttpDrawPileCell = strHttpDrawPileCell.concat(showDrawPileRadioButton());
        }
        strHttpDrawPileCell = strHttpDrawPileCell.concat("</td></tr></table>\n");
        strHttpDrawPileCell = strHttpDrawPileCell.concat("</td>\n");
        return strHttpDrawPileCell;
    }

//    public String getPlayersRow(int currGameNoOfPlayers) {
    public String getPlayersRow(SubGame currentSubGame) {
        int i, currGameNoOfPlayers, currentUserIdx, picInt, cellCount = 0;
        String strHttpPlayersRow = "", ACW_Arrow = "<-- ", CW_Arrow = " -->";
        strHttpPlayersRow = strHttpPlayersRow.concat("<tr>\n");

        if (currentSubGame.getLastDirection().equals(Direction.CLOCKWISE)) {
            // clockwise
            ACW_Arrow = ""; // clear this so it won't be printed
        } else {
            // anti-clockwise
            CW_Arrow = ""; // clear this so it won't be printed
        }

        currentUserIdx = currentSubGame.getSubGamePlayers().indexOf(currentSubGame.getCurrentPlayer());
//        System.out.println(">> getPlayersRow currentUserIdx = " + currentUserIdx);

        currGameNoOfPlayers = currentSubGame.getSubGamePlayers().size();

        if (currGameNoOfPlayers <= 5) {
            cellCount = 5;
        } else if (currGameNoOfPlayers <= 10) {
            cellCount = 10;
        }

        for (i = 0; i < cellCount; i++) {
            if (i == 5) {
                strHttpPlayersRow = strHttpPlayersRow.concat("</tr>\n<tr>\n");
            }
            if (i < currGameNoOfPlayers) {
//                strHttpPlayersRow = strHttpPlayersRow.concat("<td align=\"center\"><table border=\"1\">\n<tr><td align=\"center\">");
                strHttpPlayersRow = strHttpPlayersRow.concat("<td align=\"center\""); // ><table>\n<tr><td align=\"center\">");
                if(i == currentUserIdx)
                    strHttpPlayersRow = strHttpPlayersRow.concat(" style=\"background-color:Yellow\"");
                strHttpPlayersRow = strHttpPlayersRow.concat("><table>\n<tr><td align=\"center\">");
                picInt = currentSubGame.getSubGamePlayers().get(i).getPlayer().getIntForRandomAvatar();
//                System.out.println(">> User = " + currentSubGame.getSubGamePlayers().get(i).getPlayer().getUsername() + " randNo=" + picInt);
                strHttpPlayersRow = strHttpPlayersRow.concat("<img src=\"images\\avatars\\Avatar_"
                        + picInt + ".png\" width=\"100\" height=\"100\" alt=\"Player " + i + " Avatar\">");
                strHttpPlayersRow = strHttpPlayersRow.concat("</td></tr><tr><td align=\"center\">");
                if (i == currentUserIdx) {
                    strHttpPlayersRow = strHttpPlayersRow.concat(ACW_Arrow); // Anti-Clockwise Arrow
                }//            strHttpPlayersRow = strHttpPlayersRow.concat("userName - ");
                strHttpPlayersRow = strHttpPlayersRow.concat(currentSubGame.getSubGamePlayers().get(i).getPlayer().getUsername());
//            strHttpPlayersRow = strHttpPlayersRow.concat("handCount");
                strHttpPlayersRow = strHttpPlayersRow.concat(" (" + Integer.toString(currentSubGame.getSubGamePlayers().get(i).getHand().size()) + ") ");
                if (i == currentUserIdx) {
                    strHttpPlayersRow = strHttpPlayersRow.concat(CW_Arrow); // Clockwise Arrow
                }
            } else {
                strHttpPlayersRow = strHttpPlayersRow.concat("<td><table>\n<tr><td align=\"center\">");
  //            strHttpPlayersRow = strHttpPlayersRow.concat("<img src=\"images\\avatars\\Avatar_" + getRandomInt(1, 16) + ".png\" width=\"100\" height=\"100\" alt=\"Player " + i + " Avatar\">");
                strHttpPlayersRow = strHttpPlayersRow.concat("&nbsp");
                strHttpPlayersRow = strHttpPlayersRow.concat("</td></tr><tr><td align=\"center\">");
                strHttpPlayersRow = strHttpPlayersRow.concat("&nbsp");
            }
            strHttpPlayersRow = strHttpPlayersRow.concat("</td></tr></table>\n</td>\n");
        }
        strHttpPlayersRow = strHttpPlayersRow.concat("</tr>\n");
        return strHttpPlayersRow;
    }

    // Method to handle GET method request.
    @Override
    public void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }
} // playSubGamesServlet