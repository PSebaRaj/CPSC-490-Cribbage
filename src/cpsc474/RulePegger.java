package cpsc474;

import java.util.Map;
import java.util.HashMap;

public class RulePegger implements PegPolicy
{
    private CribbageGame game;

    public RulePegger(CribbageGame game)
    {
	this.game = game;
    }
    
    @Override
    public CribbageCard peg(CribbageHand cards, PeggingHistory hist, int[] scores, boolean amDealer)
    {
	// count how many of each value we have
	Map<Integer, Integer> countByValue = new HashMap<>();
	Map<CardRank, Integer> countByRank = new HashMap<>();
	for (CribbageCard c : cards)
	    {
		CardRank rank = c.getRank();
		int value = game.rankValue(rank);
		if (!countByValue.containsKey(value))
		    {
			countByValue.put(value, 0);
		    }
		if (!countByRank.containsKey(rank))
		    {
			countByRank.put(rank, 0);
		    }
		countByValue.put(value, countByValue.get(value) + 1);
		countByRank.put(rank, countByRank.get(rank) + 1);
	    }
	
	// maximize score earned over all cards played, breaking ties
	// uniformly randomly, with bonuses for various advantageous
	// situations
	int ties = 0;
	CribbageCard bestPlay = null;
	double bestScore = Integer.MIN_VALUE;
	for (CribbageCard c : cards)
	    {
		if (hist.isLegal(c, amDealer ? 0 : 1))
		    {
			int[] score = hist.score(c, amDealer ? 0 : 1);
			CardRank rank = c.getRank();
			int value = game.rankValue(rank);
			
			// remove card from count
			countByValue.put(value, countByValue.get(value) - 1);
			countByRank.put(rank, countByRank.get(rank) - 1);
			
			double currScore = score[0];

			// apply bonuses
			if (hist.startRound() && value < 5)
			    {
				// lead with low card
				currScore += 0.25;
			    }

			if (hist.getTotal() < 15 && hist.getTotal() + value > 15)
			    {
				// going past 15
				currScore += 0.25;
			    }

			if (hist.getTotal() + value < 15)
			    {
				int complement = 15 - (hist.getTotal() + value);
				if (countByValue.containsKey(complement) && countByValue.get(complement) > 0)
				    {
					// can make pair if opponent makes 15
					currScore += 0.25;
				    }
			    }

			if (countByRank.get(rank) > 0 && hist.getTotal() + 3 * value <= game.getPeggingLimit())
			    {
				// can make triple if opponent makes pair
				currScore += 0.25;
			    }

			if (hist.hasPassed(amDealer ? 1 : 0)
			    && countByRank.get(rank) > 0
			    && hist.getTotal() + 2 * value <= game.getPeggingLimit())
			    {
				// opponent has passed and we can make pair
				currScore += 2.0;
			    }
			else if (hist.getTotal() + value > game.getPeggingLimit() - 10
				 && countByRank.get(rank) > 0
				 && hist.getTotal() + value * 2 <= game.getPeggingLimit())
			    {
				// can make pair if opponent passes
				currScore += (hist.getTotal() + value - 21) * 0.05;
			    }

			
			    // compare score of current card to best so far
			if (currScore > bestScore)
			    {
				bestScore = currScore;
				bestPlay = c;
				ties = 0;
			    }
			else if (currScore == bestScore)
			    {
				ties++;
				if (Math.random() < 1.0 / ties)
				    {
					bestPlay = c;
				    }
			    }

			// add card back into counts
			countByValue.put(value, countByValue.get(value) + 1);
			countByRank.put(rank, countByRank.get(rank) + 1);
		    }
	    }
	return bestPlay;
    }
}
