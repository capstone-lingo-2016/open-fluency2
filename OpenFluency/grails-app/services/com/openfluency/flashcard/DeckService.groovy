package com.openfluency.flashcard

import com.openfluency.auth.User
import com.openfluency.flashcard.Share
import com.openfluency.language.Language
import grails.transaction.Transactional
import com.openfluency.auth.User
import com.openfluency.Constants
import com.openfluency.algorithm.*

@Transactional
class DeckService {

	def springSecurityService
	def algorithmService
	def flashcardInfoService

	/**
	* Retrieves the next flashcard that the user should view according SpacedRepetition
	* @param cardUsage: the usage for the flashcard being ranked
	* @param ranking that the user gave to this flashcard
	* @return the new usage for the next flashcard
	*/
	CardUsage getNextFlashcard(Deck deckInstance, String cardUsageId, Integer ranking, Integer rankingType) {

		log.info "deckInstance: ${deckInstance.id} - CardUsageID: ${cardUsageId} - Ranking: $ranking - Ranking Type: ${rankingType}"

		User theUser = User.load(springSecurityService.principal.id)
		CardUsage cardUsage = CardUsage.get(cardUsageId)
			
		if(cardUsage) {
			// Close the current CardUsage
			cardUsage.endTime = new Date()
			cardUsage.ranking = ranking
			cardUsage.save(failOnError: true)

			// Create the CardRanking for the card (or update it)
			CardRanking cardRanking = CardRanking.withCriteria {
				user {
					eq('id', springSecurityService.principal.id)
				}
				eq('flashcard', cardUsage.flashcard)
				}[0]

			// Save the card ranking
			if(!cardRanking) {
				cardRanking = new CardRanking(flashcard: cardUsage.flashcard, user: theUser)
			} 
			
			// Save the meaning depending on the type
			if(rankingType == Constants.MEANING) {
				log.info "Saving meaningRanking ${ranking}"
				cardRanking.meaningRanking = ranking
			}
			else if(rankingType == Constants.PRONUNCIATION) {
				log.info "Saving pronunciationRanking ${ranking}"
				cardRanking.pronunciationRanking = ranking
			}
			
			cardRanking.save(failOnError: true)

			//use the algo and card usage to update the flashcardInfo and Queue for the given card
			flashcardInfoService.updateViewedFlashcardInfo(deckInstance, cardUsage)
			
		}

		Flashcard flashcardInstance = flashcardInfoService.nextFlashcardInfo(theUser, deckInstance, rankingType).flashcard

		// Now create a new CardUsage for this new card and return it
		return new CardUsage(flashcard: flashcardInstance, 
			user: User.load(springSecurityService.principal.id), 
			ranking: null, rankingType: rankingType).save(failOnError: true)

	}

	/**
	* Calculate the progress that a particular user has made on a deck
	* @return 	Map that contains as keys the types of rankings that a user can give (Meaning, Pronunciation, etc)
	*			and as values the progress for each
	*/
	List getDeckProgress(Deck deckInstance) {
		// Progress calculation:
		// - When a user ranks a card, it gives him a certain number of points
		// 		- easy  	--> 3 points
		// 		- medium 	--> 2 points
		// 		- hard 		--> 1 points
		//		- unranked	--> 0 points
		// Then the progress is the total points for the user / max points
		Integer flashcardCount = deckInstance.flashcardCount
		List totalPoints = Flashcard.executeQuery("""
			SELECT sum(meaningRanking), sum(pronunciationRanking) FROM CardRanking 
			WHERE user.id = ?
			AND flashcard.id in (SELECT id FROM Flashcard WHERE deck.id = ?))
		""", [springSecurityService.principal.id, deckInstance.id])[0]
		
		Double meaningProgress = totalPoints[Constants.MEANING] ? totalPoints[Constants.MEANING]*100 / (flashcardCount*Constants.EASY) : 0
		Double pronunciationProgress = totalPoints[Constants.PRONUNCIATION] ? totalPoints[Constants.PRONUNCIATION]*100 / (flashcardCount*Constants.EASY) : 0

		return [meaningProgress.round(2), pronunciationProgress.round(2)]
	}

	/**
    * Create a new deck owned by the currently logged in user
    */
    Deck createDeck(String title, String description, String languageId, String sourceLanguageId, String cardServerName) {
    	User theUser = User.load(springSecurityService.principal.id)
    	
    	Deck deck = new Deck(title: title, 
    		description: description, 
    		owner: theUser, 
    		language: Language.load(languageId),
    		sourceLanguage: Language.load(sourceLanguageId),
    		cardServerName: cardServerName)
    	deck.save()

    	flashcardInfoService.resetDeckFlashcardInfo(theUser, deck)
    	
    	return deck
    }

    /**
    * Add a deck to my list of shared courses
    */
    Share addDeck(Deck deck) { 
    	User theUser = User.load(springSecurityService.principal.id)
    	flashcardInfoService.resetDeckFlashcardInfo(theUser, deck)
    	return new Share(deck: deck, receiver: theUser).save(flush: true);
    }

    Boolean removeDeck(Deck deck) {
    	User theUser = User.load(springSecurityService.principal.id)
    		
    	Share share = Share.findByReceiverAndDeck(theUser, deck)
    	if(share) {
    		try {
    			share.delete()
    			flashcardInfoService.removeDeckFlashcardInfo(theUser, deck)
    			return true
    		} 
    		catch(Exception e) {
    			return false
    		}
    	}
    }

    /**
    * Search for Decks
    */
    List<Deck> searchDecks(Long languageId, String keyword) {
    	log.info "Searching Decks with languageId: $languageId and Keywords: $keyword"

    	Deck.withCriteria {

            // Apply language criteria
            if(languageId) {
            	language {
            		eq('id', languageId)
            	}
            }

            // Search using keywords in the title or description
            if(keyword) {
            	or {
            		ilike("title", "%${keyword}%")
            		ilike("description", "%${keyword}%")
            	}
            }
        }
    }

    /**
    * Get a random flashcard from a deck where the given flashcard lives but is not the given flashcard
    */
    Flashcard getRandomFlashcard(Flashcard flashcardInstance) {
    	Flashcard.executeQuery('FROM Flashcard WHERE deck = ? AND id <> ? ORDER BY rand()', [flashcardInstance.deck, flashcardInstance.id], [max: 1])[0]
    }
}
