from selenium import webdriver
from time import sleep
from webdriver_manager.chrome import ChromeDriverManager
import keyboard
import subprocess


VAL_TO_CARD = {1: "A", 2: "2", 3: "3", 4: "4", 5: "5", 6: "6", 7: "7", 8: "8", 9: "9", 10: "T", 11: "J", 12: "Q", 13: "K"}
CARD_TO_VAL = {value: key for key, value in VAL_TO_CARD.items()}


def play(driver):
    throw_process = subprocess.Popen(['java', '-jar', 'DiscardV2.jar'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
    peg_process = subprocess.Popen(['java', '-jar', 'PegV2.jar'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True)
    keyboard.wait('p')
    throw_process.stdout.readline() # skip reading thrownodes line
    peg_process.stdout.readline() # skip reading pegnodes line
    
    keep_playing = True

    while keep_playing:
        # discarding
        hand = driver.execute_script("return window.AI.hand();")
        original_hand = hand[:]
        dealer = driver.execute_script("return window.AI.humanIsDealer();")
        discard_cards = get_discard_cards(hand, dealer, throw_process)
        driver.execute_script(f"window.AI.discardCard({discard_cards[0]['rank']}, '{discard_cards[0]['suit']}')")
        driver.execute_script(f"window.AI.discardCard({discard_cards[1]['rank']}, '{discard_cards[1]['suit']}')")
        
        if not dealer:
            keyboard.wait('p')
            driver.execute_script("window.AI.cutDeck(12);")
        
        # play the 4 cards
        for i in range(4):
            if keep_playing:
                keyboard.wait('p')
                if check_winner(driver):
                    keep_playing = False
                if keep_playing:
                    c = peg(driver, original_hand, peg_process)
                    driver.execute_script(f"window.AI.playCard({c['rank']}, '{c['suit']}');")
                    sleep(1)
                    if check_winner(driver):
                        keep_playing = False
            
        # manually click through scoring
        if keep_playing: keyboard.wait('p')
        
        if check_winner(driver):
            keep_playing = False
            
    if not keep_playing:
        throw_process.stdin.write("quit\n")
        peg_process.stdin.write("quit\n")
        sleep(0.3)
        throw_process.stdin.close()
        throw_process.stdout.close()
        peg_process.stdin.close()
        peg_process.stdout.close()

        
        
    
    

def check_winner(driver):
    scores = driver.execute_script("return window.AI.scores();")
    human_score = sum(scores["human"].values())
    computer_score = sum(scores["computer"].values())
    
    if human_score >= 121:
        print("Human won")
        print(human_score, computer_score)
        return True
    elif computer_score >= 121:
        print("Computer won")
        print(human_score, computer_score)
        return True

    return False
        
    
def get_discard_cards(hand, dealer, process):
    cards = "".join([VAL_TO_CARD[c['rank']] + c['suit'] for c in hand])
    inp = ("1" if dealer else "0") + cards
    print(inp)
    process.stdin.write(inp + "\n")
    process.stdin.flush()
    sleep(1)
    output = process.stdout.readline().strip().split('|')
    print(output)
    return [{'rank': CARD_TO_VAL[output[0][0]], 'suit': output[0][1]}, {'rank': CARD_TO_VAL[output[1][0]], 'suit': output[1][1]}]
    
    
def get_cur_seq(table, hand):
    # this is not correct. Sometimes includes last played card when should not do that
    index = 0
    total = 0
    smallest_hand_card = min([c['rank'] for c in hand])
    smallest_hand_card = min([smallest_hand_card, 10])
    for i in range(len(table)):
        total += min(10, table[i]['rank'])
        if total > 31:
            total = min(10, table[i]['rank'])
            index = i
        elif total == 31:
            index = i + 1
            total = 0
    if index >= len(table): return ""
    
    
    potential_seq = table[index:]
    
    total = 0
    for c in potential_seq:
        total += min([10, c['rank']])
        
    if total + smallest_hand_card > 31: return ""
    return "".join([VAL_TO_CARD[c['rank']] for c in potential_seq])
    


def peg(driver, original_hand, process):
    hand = driver.execute_script("return window.AI.hand();")
    table = driver.execute_script("return window.AI.table();")
    cur_seq = get_cur_seq(table, hand)
    print(f"cur seq: {cur_seq}")
    original_hand_set = {(c['rank'], c['suit']) for c in original_hand}
    played_me = []
    played_opp = []
    for c in table:
        if (c['rank'], c['suit']) in original_hand_set:
            played_me.append(c)
        else:
            played_opp.append(c)
            
            
    played_me = sorted(played_me, key=lambda x: x['rank'])
    played_opp = sorted(played_opp, key=lambda x: x['rank'])
    cur_hand = "".join([VAL_TO_CARD[c['rank']] for c in hand])
    played_me = "".join([VAL_TO_CARD[c['rank']] for c in played_me])
    played_opp = "".join([VAL_TO_CARD[c['rank']] for c in played_opp])
    
    infoset = cur_hand + "|" + played_me + "|" + played_opp + "|" + cur_seq
    
    print(infoset)
    process.stdin.write(infoset + "\n")
    process.stdin.flush()
    sleep(1)
    output = process.stdout.readline().strip()
    print(output)
    
    # find card that matches rank (since jar does not give suit)
    for c in hand:
        if c['rank'] == CARD_TO_VAL[output]:
            return c
    
    
    
    

driver = webdriver.Chrome(ChromeDriverManager().install())

# Navigate to the desired website
website = 'https://cardgames.io/cribbage/'
driver.get(website)

driver.execute_script("localStorage.selenium =  true;")
sleep(0.2)
driver.refresh()

play(driver)