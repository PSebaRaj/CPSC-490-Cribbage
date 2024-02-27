import cpsc474.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CFRTrainTogether {

    static CribbageGame game = new CribbageGame();
    static HashMap<String, Integer> rankToVal = new HashMap<>();
    static HashMap<String, PegNode> pegNodes = new HashMap<>();
    static HashMap<String, ThrowNode> throwNodes = new HashMap<>();
    static int[][] combs = {{0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {1, 2}, {1, 3}, {1, 4}, {1, 5}, {2, 3}, {2, 4}, {2, 5}, {3, 4}, {3, 5}, {4, 5}};

    public static void main(String[] args) {
        populateRanks();
//        PegPolicy pegPolicy = new CFRPeggingPolicy(
//                new GreedyPegger(), pegNodes, true, true
//        );
//        KeepPolicy keepPolicy = new CFRThrower(
//                game, new GreedyThrower(game), throwNodes, false, true
//        );


        int[] scores = new int[] {0, 0};
        for (int i = 0; i < 10000 * 1000 + 1; i++){

            PegPolicy pegPolicy = new CFRPeggingPolicy(
                    new GreedyPegger(), pegNodes, true, true
            );
            KeepPolicy keepPolicy = new CFRThrower(
                    game, new GreedyThrower(game), throwNodes, false, true
            );

            cfrThrowing(
                    1.0, 1.0, pegPolicy, 0, null, null, null, false, null
            );
            cfrPegging(
                    null, 1.0, 1.0, scores, 0, keepPolicy, 0, null, 0
            );

            if (i%1000==0) {
                System.out.println("***");
                System.out.println(i);
                System.out.println("Throwing nodes size: " + throwNodes.size());
                System.out.println("Pegging nodes size: " + pegNodes.size());
            }
//            if (i > 0 && i % 250000 == 0) {
            if (i > 0 && i % 10000 == 0) {
                int k = (i) / 1000;
                try {
                    NodeLoader.saveNodes("thrownodes_v7_" + k + "k.txt", throwNodes);
                    NodeLoader.saveNodes("pegnodes_v7_" + k + "k.txt", pegNodes);
                } catch (IOException e) {
                    e.printStackTrace();
                    System.err.println("Error saving nodes");
                }
            }
        }
    }

    public static void populateRanks() {
        // construct mapping rank to value
        rankToVal.put("A", 0);
        for (int i = 2; i <= 9; i++) {
            rankToVal.put(Integer.toString(i), i-1);
        }
        // not the point values - just mapping to make sure we can sort correctly and reproducible
        rankToVal.put("T", 9);
        rankToVal.put("J", 10);
        rankToVal.put("Q", 11);
        rankToVal.put("K", 12);
    }

    //
    // Start Pegging impl
    //

    /**
     * The CFR Algorithm. Outputs point difference between player 0 and 1.
     * @param history
     * @param p0
     * @param p1
     * @param scores
     * @param roundNum (0 to start, 1 or 2 when going)
     * @param keepPolicy
     * @param dealer
     * @param playerHands
     * @param player
     * @return nodeUtil
     */
    public static double cfrPegging(
            PeggingHistory history,
            double p0,
            double p1,
            int[] scores,
            int roundNum,
            KeepPolicy keepPolicy,
            int dealer,
            CribbageHand[] playerHands,
            int player
    ) {

        // stopping condition to ensure that we are saving info states where few cards left
        // use Greedy pegging strategy for the remainder

        // top line used for complex agent that makes all 3 moves (peg v3 or peg v4) - bottom for the simpler agent (peg v2 and peg v1)
        //if (roundNum > 0 && (playerHands[player].size() < 2 || playerHands[0].size() == 0 || playerHands[1].size() == 0)) {
        if (roundNum > 0 && (playerHands[player].size() <= 2 || playerHands[0].size() == 0 || playerHands[1].size() == 0)) {
            GreedyPegger pegger = new GreedyPegger();
            int turn = player;

            // play out the rest of the game using backup greedy policy
            while (!history.isTerminal()) {
                CribbageCard play = pegger.peg(playerHands[turn], history, null, turn == dealer);
                if (play != null) {
                    playerHands[turn] = playerHands[turn].remove(play);
                }
                history = history.play(play, turn == dealer ? 0 : 1);

                int[] playScore = history.getScore();
                if (playScore[0] > 0)
                {
                    scores[turn] += playScore[0];
                }
                else if (playScore[0] < 0)
                {
                    scores[1 - turn] += -playScore[0];
                }

                turn = 1 - turn;
            }
        }

        // return score difference
        if (history != null && history.isTerminal()) {
            int scoreDiff = scores[0] - scores[1];
            return scoreDiff;
        }

        // initializes everything at the start of the algorithm
        if (roundNum == 0) {
            roundNum += 1;
            history = new PeggingHistory(game);
            dealer = 1;

            // dealer pegs second, so player to start is not the dealer
            player = 1 - dealer;

            // deal cards
            CribbageHand[] cardsInPlay = game.deal();

            // get keep/throw for each player
            CribbageHand[][] keeps = new CribbageHand[2][];
            for (int p = 0; p < 2; p++)
            {
                keeps[p] = keepPolicy.keep(cardsInPlay[p], null, p == dealer);

                // error checking
                if (!cardsInPlay[p].isLegalSplit(keeps[p]))
                {
                    throw new RuntimeException(Arrays.toString(keeps[p]) + " does not partition " + cardsInPlay[p]);
                }
                else if (keeps[p][0].size() != 4)
                {
                    throw new RuntimeException("Invalid partition sizes " + Arrays.toString(keeps[p]));
                }
            }

            playerHands = new CribbageHand[]{keeps[0][0], keeps[1][0]};

            // need sorted for correct infoset string making
            Collections.sort(playerHands[0].cards, new SortCardsPegging());
            Collections.sort(playerHands[1].cards, new SortCardsPegging());
        }

        String infoSet = getInfoSetPegging(playerHands[player].cards, getCardsPlayed(history, player, dealer), getCardsPlayed(history, 1-player, dealer), getCurSeq(history));

        double param = player == 0 ? p0 : p1;

        // get legal actions
        List<CribbageCard> actions = new ArrayList<>();
        for (CribbageCard c : playerHands[player]) {
            if (history.isLegal(c, player == dealer ? 0 : 1)){
                actions.add(c);
            }
        }

        PegNode node;
        if (!pegNodes.containsKey(infoSet)) {
            // create a new infoSet
            node = new PegNode((byte) actions.size());
            if (actions.size() > 0) pegNodes.put(infoSet, node);
        }
        else {
            node = pegNodes.get(infoSet);
        }

        if (node.numActions != actions.size()) {
            throw new RuntimeException("CFR Node and legal actions disagree on # of valid actions. CFR Node: " + infoSet + " " + node.numActions + ". Possible actions: " + actions);
        }

        // for the time we cannot play a card
        if (actions.size() == 0) {
            actions.add(null);
        }

        // initialize util array
        float[] strategy = node.getStrategy(param);
        double[] util = new double[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            util[i] = 0.0;
        }

        double nodeUtil = 0;
        for (int i = 0; i < actions.size(); i++) {
            PeggingHistory nextHistory = history.play(actions.get(i), player == dealer ? 0 : 1);

            // make new hands, removing cards from hand if played
            CribbageHand nextHand0 = playerHands[0];
            CribbageHand nextHand1 = playerHands[1];

            if (actions.get(i) != null) {
                if (player == 0) {
                    nextHand0 = playerHands[0].remove(actions.get(i));
                } else {
                    nextHand1 = playerHands[1].remove(actions.get(i));
                }
            }

            int[] newScores = scores.clone();

            // score the play
            int[] playScore = nextHistory.getScore();
            if (playScore[0] > 0)
            {
                newScores[player] += playScore[0];
            }
            else if (playScore[0] < 0)
            {
                newScores[1 - player] += -playScore[0];
            }

            double strat_i = 1.0;
            if (actions.get(i) != null){
                strat_i = strategy[i];
            }


            // call CFR recursively
            if (player == 0) {
                util[i] = cfrPegging(nextHistory, p0 * strat_i, p1, newScores, roundNum, keepPolicy, dealer, new CribbageHand[]{nextHand0, nextHand1}, 1-player);
            }
            else {
                util[i] = -cfrPegging(nextHistory, p0, p1 * strat_i, newScores, roundNum, keepPolicy, dealer, new CribbageHand[]{nextHand0, nextHand1}, 1-player);
            }

            nodeUtil += strat_i * util[i];
        }

        // update regretSums
        if (node.numActions > 0) {
            for (int i = 0; i < actions.size(); i++) {
                double regret = util[i] - nodeUtil;
                node.regretSum[i] += regret * (player == 0 ? p1 : p0);
            }
        }

        return player == 0 ? nodeUtil : -nodeUtil;
    }

    /**
     * Get list of cards played in the current round
     * @param history
     * @return list of cards
     */
    public static List<CribbageCard> getCurSeq(PeggingHistory history) {
        List<CribbageCard> cards = new ArrayList<>();

        if (history.startRound()) {
            return cards;
        }

        while (history != null) {
            CribbageCard card = history.card;
            if (card != null) {
                cards.add(card);
            }
            history = history.prevPlay;
        }
        Collections.reverse(cards);
        return cards;
    }

    /**
     * Get all cards played by a specific player in the current crib/pegging round.
     * @param history
     * @param player
     * @param dealer
     * @return list of cards
     */
    public static List<CribbageCard> getCardsPlayed(PeggingHistory history, int player, int dealer) {
        List<CribbageCard> cards = new ArrayList<>();
        int historyPlayer = player == dealer ? 0 : 1;
        while (history != null) {
            CribbageCard card = history.card;
            if (card != null && history.player == historyPlayer) {
                // if the player matches the player of interest, add that card
                cards.add(card);
            }
            if (history.prevPlay == null) {
                // go to previous round if no more rounds in this one
                history = history.prevRound;
            }
            else {
                history = history.prevPlay;
            }
        }
        return cards;
    }


    /**
     * Combine current information into a string representation of infoset.
     *
     * @param hand0: hand of hero
     * @param plays0: plays of hero
     * @param plays1: plays of opponent
     * @param curSeq: current sequence of cards on the board
     * @return String representation of infoset
     */
    public static String getInfoSetPegging(List<CribbageCard> hand0, List<CribbageCard> plays0, List<CribbageCard> plays1, List<CribbageCard> curSeq) {
        // this is for the complete agent
        return getRankString(hand0, true) + "|" +  getRankString(plays0, true) + "|" + getRankString(plays1, true) + "|" + getRankString(curSeq, false);

        // this is for the incomplete agent (does not consider cards previously played)
        //return getRankString(hand0, true) + "|" + getRankString(curSeq, false);

    }
    /**
     * Get a String of card ranks (ignoring suit) from inputted list
     * @param cards list
     * @param sort boolean
     * @return String representing the ranks of the shown cards
     */
    public static String getRankString(List<CribbageCard> cards, boolean sort) {
        if (sort){
            Collections.sort(cards, new SortCardsPegging());
        }
        List<CardRank> ranks = cards.stream().map(CribbageCard::getRank).collect(Collectors.toList());
        List<String> rankStr = ranks.stream().map(CardRank::getName).collect(Collectors.toList());
        return String.join("", rankStr);
    }


    /**
     * Comparator for cards using rankToVal mapping
     */
    static class SortCardsPegging implements Comparator<CribbageCard>
    {
        public int compare(CribbageCard a, CribbageCard b)
        {
            return rankToVal.get(a.getRank().toString()) - rankToVal.get(b.getRank().toString());
        }
    }

    //
    // End Pegging impl
    //

    //
    // Start Throwing impl
    //

    /**
     * CFR for throwing strategy.
     * @param p0
     * @param p1
     * @param pegPolicy
     * @param player
     * @param keep0
     * @param throw0
     * @param hand1
     * @param secondToAct
     * @param turn
     * @return
     */
    public static double cfrThrowing(
            double p0,
            double p1,
            PegPolicy pegPolicy,
            int player,
            CribbageHand keep0,
            CribbageHand throw0,
            CribbageHand hand1,
            boolean secondToAct,
            CribbageCard turn
    ) {
        String infoSet = "";
        CribbageHand myCards = null;
        if (!secondToAct) {
            CribbageHand[] cardsInPlay = game.deal();
            turn = cardsInPlay[2].iterator().next();
            myCards = cardsInPlay[0];
            hand1 = cardsInPlay[1];
            Collections.sort(myCards.cards, new SortCardsThrowing(getFlushSuit(myCards.cards)));
            Collections.sort(hand1.cards, new SortCardsThrowing(getFlushSuit(hand1.cards)));
            infoSet = getInfoSetThrowing(myCards.cards, true);
        }
        else {
            infoSet = getInfoSetThrowing(hand1.cards, false);
        }


        ThrowNode node;
        if (!throwNodes.containsKey(infoSet)) {
            // create a new infoSet
            node = new ThrowNode();
            throwNodes.put(infoSet, node);
        }
        else {
            node = throwNodes.get(infoSet);
        }


        // for every action I can choose
        // split the card accordingly and run CFR
        // then on the next turn, do the same for opponent
        // in the opponent for loop, play out the game and evaluate
        // then switch sides
        double nodeUtil = 0.0;
        double param = player == 0 ? p0 : p1;
        float[] strategy = node.getStrategy(param);
        double[] util = new double[15];
        for (int i = 0; i < 15; i++) {
            util[i] = 0.0;
        }
        for (int i = 0; i < combs.length; i++) {

            if (!secondToAct) {
                // split the card accordingly and run cfr_pkg.CFR
                CribbageCard throw1 = myCards.cards.get(combs[i][0]);
                CribbageCard throw2 = myCards.cards.get(combs[i][1]);
                CribbageHand throwCards = new CribbageHand(List.of(new CribbageCard[]{throw1, throw2}));
                CribbageHand keptCards = myCards.remove(throw1);
                keptCards = keptCards.remove(throw2);

                util[i] = -cfrThrowing(p0 * strategy[i], p1, pegPolicy, 1 - player, keptCards, throwCards, hand1, true, turn);

                nodeUtil += strategy[i] * util[i];
            }
            else {
                CribbageCard throw1 = hand1.cards.get(combs[i][0]);
                CribbageCard throw2 = hand1.cards.get(combs[i][1]);
                CribbageHand throwCards = new CribbageHand(List.of(new CribbageCard[]{throw1, throw2}));
                CribbageHand keptCards = hand1.remove(throw1);
                keptCards = keptCards.remove(throw2);

                util[i] = -play(pegPolicy, keep0, keptCards, throw0, throwCards, turn);

                // negative because this is for second player
                nodeUtil += strategy[i] * util[i];
            }

        }

        for (int i = 0; i < combs.length; i++) {
            double regret = util[i] - nodeUtil;
            node.regretSum[i] += regret * (player == 0 ? p1 : p0);
        }

        return nodeUtil;
    }

    private static String getFlushSuit(List<CribbageCard> cards) {
        return null; // for training non flush method


        // uncomment below to train suit-conscious method
        /*HashMap<String, Integer> suitCounts = new HashMap<>();
        for (cpsc474.CribbageCard c : cards) {
            String suit = String.valueOf(c.getSuit());
            if (!suitCounts.containsKey(suit)) {
                suitCounts.put(suit, 0);
            }
            suitCounts.put(suit, suitCounts.get(suit) + 1);
        }


        String flushSuit = null; // appears at least 4 times
        for (String suit : suitCounts.keySet()) {
            if (suitCounts.get(suit) >= 4) {
                flushSuit = suit;
                break;
            }
        }
        return flushSuit;*/
    }

    public static int play(PegPolicy peggingPolicy, CribbageHand keep0, CribbageHand keep1, CribbageHand throw0, CribbageHand throw1, CribbageCard turn)
    {
        int[] scores = new int[] {0, 0};
        int dealer = 0;

        // check for 2 for heels (turned card is a Jack)
        int heels = game.turnCardValue(turn);
        scores[dealer] += heels;

        // initialize pegging
        int pegTurn = 1 - dealer;
        PeggingHistory history = new PeggingHistory(game);
        CribbageHand[] pegCards = new CribbageHand[] {keep0, keep1};
        while (!history.isTerminal())
        {
            // get player's played card
            CribbageCard play = peggingPolicy.peg(pegCards[pegTurn], history, pegTurn == 0 ? Arrays.copyOf(scores, scores.length) : MoreArrays.reverse(scores), pegTurn == dealer);

            // check for legality of chosen card
            if (play == null && history.hasLegalPlay(pegCards[pegTurn], pegTurn == dealer ? 0 : 1))
            {
                throw new RuntimeException("passing when " + pegCards[pegTurn] + " contains a valid card");
            }
            else if (play != null && !history.isLegal(play, pegTurn == dealer ? 0 : 1))
            {
                throw new RuntimeException("chosen card " + play + " us not legal");
            }

            history = history.play(play, pegTurn == dealer ? 0 : 1);

            // score the play
            int[] playScore = history.getScore();
            if (playScore[0] > 0)
            {
                scores[pegTurn] += playScore[0];
            }
            else if (playScore[0] < 0)
            {
                scores[1 - pegTurn] += -playScore[0];
            }

            // remove played card from hand
            if (play != null)
            {
                CribbageHand newHand = pegCards[pegTurn].remove(play);
                if (newHand == null)
                {
                    throw new RuntimeException("played card " + play + " not in hand " + pegCards[pegTurn]);
                }
                pegCards[pegTurn] = newHand;
            }

            // next player's turn
            pegTurn = 1 - pegTurn;
        }

        // score non-dealer's hand
        int[] handScore = game.score(keep1, turn, false);
        scores[1 - dealer] += handScore[0];

        // score dealer's hand
        handScore = game.score(keep0, turn, false);
        scores[dealer] += handScore[0];

        // score crib
        CribbageHand crib = new CribbageHand(throw0, throw1);
        handScore = game.score(crib, turn, true);
        scores[dealer] += handScore[0];

        return scores[0] - scores[1];


        // return game.gameValue(scores);
    }

    public static String getInfoSetThrowing(List<CribbageCard> myCards, boolean amDealer) {
        return (amDealer ? 1 : 0) + getSortedCardString(myCards);
    }

    /**
     * Get a String of sorted cards, including majority suit if exists
     * @param cards list
     * @return String representing the ranks of the shown cards
     */
    public static String getSortedCardString(List<CribbageCard> cards) {
        String flushSuit = getFlushSuit(cards);
        Collections.sort(cards, new SortCardsThrowing(flushSuit));
        if (flushSuit == null) {
            return concatCardRanks(cards);
        }
        else {
            List<CribbageCard> suitedCards = new ArrayList<>();
            List<CribbageCard> otherCards = new ArrayList<>();
            for (CribbageCard c: cards) {
                if (String.valueOf(c.getSuit()).equals(flushSuit)) {
                    suitedCards.add(c);
                }
                else {
                    otherCards.add(c);
                }
            }

            return concatCardRanks(suitedCards) + "|" + concatCardRanks(otherCards);
        }

    }

    private static String concatCardRanks(List<CribbageCard> cards) {
        String s = "";
        for (CribbageCard c : cards) {
            s += c.getRank().toString();
        }
        return s;
    }

    /**
     * Comparator for cards using rankToVal mapping
     */
    static class SortCardsThrowing implements Comparator<CribbageCard>
    {
        private String flushSuit;
        public SortCardsThrowing(String flushSuit){
            super();
            this.flushSuit = flushSuit;
        }
        public int compare(CribbageCard a, CribbageCard b)
        {
            int aVal = rankToVal.get(a.getRank().toString());
            int bVal = rankToVal.get(b.getRank().toString());

            // put the cards of the majority suit to the very right of all cards with same rank
            if (flushSuit != null && b.getRank() == a.getRank()) return (String.valueOf(a.getSuit()).equals(flushSuit)) ? 1 : -1;
            return aVal - bVal;
        }
    }

    //
    // End Throwing impl
    //

}
