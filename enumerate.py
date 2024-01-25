"""

The goal is to enumerate through all of the possible info states to determine
a reasonable level of abstraction.

We are ignoring suits as well.

Results: 26M info states if we store sorted(hand), p1_played, p2_played, current_seq (0.7 Gb file)
If we only consider infostates where we have 3-4 cards in our hand, this drops to 1.6M.
"""

import itertools
import pickle

STATES = {}

CARDS = ["2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"]
MAPPING = {
    "2": 2, "3": 3, "4": 4, "5": 5, "6": 6, "7": 7, "8": 8, "9": 9, "T": 10, "J": 10, "Q": 10, "K": 10, "A": 1    
}

def run():
    possible_hands = list(itertools.combinations_with_replacement(CARDS, 4))
    to_play = len(possible_hands) ** 2
    c = 0
    for i in range(len(possible_hands)):
        for j in range(len(possible_hands)):
            hand1 = possible_hands[i]
            hand2 = possible_hands[j]
            c += 1
            if c % 100000 == 0:
                print(f"{round(c/to_play*100, 2)}% Done")
                with open("enums.pickle", "wb") as f:
                    pickle.dump(STATES, f)
                with open('hands_stopped_at.txt', 'w') as f:
                    f.write(f"{i}, {j}")
            if are_valid_hands(hand1, hand2):
                play("".join(sorted(hand1)), "".join(sorted(hand2)), [], [], "", 0, 0, False)
                play("".join(sorted(hand1)), "".join(sorted(hand2)), [], [], "", 1, 0, False)
                
    print(f"Num states: {len(STATES)}")
            
            
def are_valid_hands(hand1, hand2):
    # ensures that we do not have more than 4 of any card
    counts = {}
    for c in hand1:
        counts[c] = counts.get(c, 0) + 1
    for c in hand2:
        counts[c] = counts.get(c, 0) + 1
    for k,v in counts.items():
        if v > 4: return False
    return True


# assuming hands are sorted
def play(hand1, hand2, sorted_plays_1, sorted_plays_2, cur_seq, turn, cur_total, prev_go):
    
    # can stop at 2 hands for 1.6M states or 1 hand for 26M
    if len(hand1) <= 2 or len(hand2) == 0: return
    
    if turn == 0:
        save_state(hand1, sorted_plays_1, sorted_plays_2, cur_seq)
        cards = get_valid_play(hand1, cur_total)
        if not cards:
            if not prev_go:
                play(hand1, hand2, sorted_plays_1, sorted_plays_2, cur_seq, int(not turn), cur_total, True)
                return
            else:
                play(hand1, hand2, sorted_plays_1, sorted_plays_2, "", int(not turn), 0, False)
                return
        else:
            for card in cards:
                play(hand1.replace(card, "", 1), hand2, sorted(sorted_plays_1 + [card]), sorted_plays_2, cur_seq + card, int(not turn), cur_total + MAPPING[card], False)
            
    else:
        cards = get_valid_play(hand2, cur_total)
        if not cards:
            if not prev_go:
                play(hand1, hand2, sorted_plays_1, sorted_plays_2, cur_seq, int(not turn), cur_total, True)
                return
            else:
                play(hand1, hand2, sorted_plays_1, sorted_plays_2, "", int(not turn), 0, False)
                return
        else:
            for card in cards:
                play(hand1, hand2.replace(card, "", 1), sorted_plays_1, sorted(sorted_plays_2 + [card]), cur_seq + card, int(not turn), cur_total + MAPPING[card], False)
        
    
    
    
def get_valid_play(hand, cur_total):
    """Returns list of cards in our hand that we can play at this point"""
    valid = []
    for c in hand:
        if cur_total + MAPPING[c] <= 31:
            valid.append(c)
    return valid
    
def save_state(hand1, sorted_plays_1, sorted_plays_2, cur_seq):
    """Creates the state and saves it to dict"""
    global STATES
    state = (hand1, "".join(sorted_plays_1), "".join(sorted_plays_2), cur_seq)
    if state not in STATES:
        STATES[state] = 1
    
    
    
    
    
    
    
    