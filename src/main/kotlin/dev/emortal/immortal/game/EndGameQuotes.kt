package dev.emortal.immortal.game

import net.minestom.server.entity.Player
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material

object EndGameQuotes {

    val victory = setOf(
        "pepeD",
        "gg ez no re",
        "All luck",
        "NoHacksJustSkill",
        "Were you hacking?",
        "Were you teaming?",
        "no skill issues here",
        "i am better",
        "I dropped my popcorn",
        "Defeat... but not for you",
        "Teamwork makes the dream work",
        "vic roy",
        "call an ambulance... but not for me",
        "What chair do you have?",
        "I won? It must have been my chair!",
        "No GC this time",
        "Moulberry is proud of you",
        "tryhard == true",
        "touch grass",
        "take a shower",
        "take a cookie for your efforts",
        "[username] never dies!",
        "average rust enjoyer",
        "average valence enjoyer",
        "average minestom enjoyer",
        "i wasn't looking, do it again",
        "built different",
        "carried",
        "don't forget to breathe!",
        "want a medal?",
        "you lost the game",
        "BLEHHH"
    )

    val defeat = setOf(
        "I guess you weren't trying",
        "no",
        "L",
        "rip bozo",
        "no zaza??",
        "what is your excuse?",
        "skill issue",
        "uffeyman was here",
        "Better luck next time!",
        "Worse luck next time!",
        "Should have used a Minestom client",
        "Teamwork... towards defeat",
        "No one matches your skill... at losing",
        "Victory... but not for you",
        "You tried and that's what matters!",
        "Zzzzzz",
        "Did your cat step on your keyboard?",
        "PepeHands",
        "At least someone won, too bad it wasn't you",
        "cry about it",
        "were you afk?",
        "smh my head",
        "I bet you didn't even say good game",
        "You did your best... but it wasn't enough",
        "\"i lagged\" said an Australian",
        "If I had a dollar every time you won a game, I'd be in crippling debt",
        "widepeepoSad",
        "try harder",
        "You clearly weren't using Graphite",
        "I saw the GC spike",
        "python user",
        "BLEHHH"
    )

    val draw = setOf(
        "Well that wasn't worth my time",
        "Hopefully one of you win next time",
        "Well... that was unexpected",
        "You're both too good",
        "Feeling a bit indecisive?",
        "It would have been better if someone won",
        "You're both average",
        "You both suck!",
        "What did I just witness?",
        "Twins?"
    )

    fun getVictoryQuote(quote: String, player: Player): String {
        val newQuote = quote.replace("[username]", player.username)

        if (newQuote == "take a cookie for your efforts") {
            player.itemInMainHand = ItemStack.of(Material.COOKIE)
        }

        return quote
    }
    fun getDefeatQuote(quote: String, player: Player): String {
        return quote.replace("[username]", player.username)
    }
    fun getDrawQuote(quote: String, player: Player): String {
        return quote.replace("[username]", player.username)
    }
}