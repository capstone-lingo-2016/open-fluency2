package com.openfluency.deck

import com.openfluency.language.Alphabet;
import com.openfluency.language.Language;
import com.openfluency.language.Pronunciation;
import com.openfluency.language.Unit;
import com.openfluency.language.Unit
import com.openfluency.language.UnitMapping
import com.openfluency.language.Pronunciation
import com.openfluency.media.*
import com.openfluency.flashcard.Share
import com.openfluency.language.Language
import com.openfluency.flashcard.Deck;
import static java.nio.file.StandardCopyOption.*
import java.nio.file.Files
import java.nio.file.Path
import org.apache.tools.ant.util.FileUtils;

import grails.transaction.Transactional

import java.io.File;
import java.util.regex.Pattern

import com.openfluency.auth.User
import com.openfluency.course.Chapter
import com.openfluency.Constants
import com.openfluency.algorithm.*

import cscie99.team2.lingolearn.server.anki.AnkiFile
import cscie99.team2.lingolearn.shared.Card
import cscie599.openfluency2.*

@Transactional
class PreviewDeckService {

	def springSecurityService
	def flashcardService
	def algorithmService
	def flashcardInfoService
	def mediaService
    def languageService
    def deckService
	def mediaTmpDir;
	def mediaDir;

	// This is for guessing how a deck should import
	Pattern isId = Pattern.compile("id",Pattern.CASE_INSENSITIVE)
	Pattern english = Pattern.compile("english",Pattern.CASE_INSENSITIVE)
	Pattern japanese = Pattern.compile("japanese|kanji",Pattern.CASE_INSENSITIVE)
	Pattern katakana = Pattern.compile("katakana",Pattern.CASE_INSENSITIVE)
	Pattern kana = Pattern.compile("hiragana|kana",Pattern.CASE_INSENSITIVE)
	Pattern romaji = Pattern.compile("romaji|romanji|roumaji",Pattern.CASE_INSENSITIVE)
	Pattern picture = Pattern.compile("picture|image",Pattern.CASE_INSENSITIVE)
	Pattern sound = Pattern.compile("sound",Pattern.CASE_INSENSITIVE)
	Pattern expression = Pattern.compile("expression|front",Pattern.CASE_INSENSITIVE)
	Pattern meaning = Pattern.compile("meaning|back",Pattern.CASE_INSENSITIVE)
	Pattern reading = Pattern.compile("reading",Pattern.CASE_INSENSITIVE)
	def langMap=["English":english,"Japanese":japanese,
		"Katakana":katakana, "Hiragana":kana, "Romaji":romaji ]
	def fieldMap=["Meaning":[english,meaning],"Literal":[expression,japanese],
		"Pronunciation":[kana,katakana,romaji,reading]]

	// This is a desperate guessing function - Should be a test function instead
	def importDeck(PreviewDeck previewDeckInstance, String mediaTmpDir, String mediaDir) {
		this.mediaTmpDir= mediaTmpDir
		this.mediaDir= mediaDir
		PreviewCard card= PreviewCard.findByDeck(previewDeckInstance,[max:1])
		HashMap<String,Integer> fieldIndices = new HashMap<String,Integer>();
		HashMap<Integer,String> alphaIndices = new HashMap<Integer,String>();
		int nfields = card.types.size()
		for (int i=0; i < nfields; i++) {
			def field = card.fields.get(i)
			def type = card.types.get(i)
			if (isId.matcher(field).matches()) continue;
			if ("Text".equals (type)) {
				langMap.each  {
					if (it.value.matcher( field ).matches()) {
						alphaIndices.put(i,it.key)
					}
				}
				fieldMap.each  {
					for (obj in it.value) {
						if (obj.matcher( field ).matches()) {
							fieldIndices.put(it.key,i)
						}
					}
				}
			} else if ("Sound".equals(type)) fieldIndices.put("Sound",i)
			else if ("Image".equals(type)) fieldIndices.put("Image",i)
		}
		if ("Kana" in fieldMap.keySet()){ // preferred
			fieldIndices.put("Pronunciation", fieldIndices.get("Kana"))
			alphaIndices.put(fieldIndices.get("Kana"), "Hiragana")
		} else if ("Katakana" in fieldMap.keySet()){
			fieldIndices.put("Pronunciation", fieldIndices.get("Katakana"))
			alphaIndices.put(fieldIndices.get("Kana"), "Katakana")
		}
		if (!fieldIndices.containsKey("Literal") && "Pronunciation" in fieldIndices.keySet())
			fieldIndices.put("Literal",fieldIndices.get("Pronunciation"))
		if (!fieldIndices.containsKey("Literal") || !fieldIndices.containsKey("Meaning")){
			println("There are no front and back assignments (such as expression,meaning,etc) for the cards")
			return null
		}
		return createOpenFluencyDeck(Language.findByName("English"), previewDeckInstance,
				fieldIndices, alphaIndices, algorithmService.cardServerNames()[0]) //getDefault())
	}


	// This should move the file to a new directory and return the new name
	// When the upload is successful, delete the preview deck and all media
	String remapMedia(String media) {
		String prefix = "web-app" + File.separator
		MediaFileMap.remapMedia(media, prefix + this.mediaTmpDir, prefix + this.mediaDir )
		return "OpenFluency" + File.separator  + this.mediaDir + File.separator + media
	}

	// create open fluency deck from PreviewDeck with
	@Transactional
	def createOpenFluencyDeck(Language sourceLanguage, PreviewDeck previewDeckInstance,
		HashMap<String,Integer> fieldIndices, HashMap<String,Integer> alphaIndices, String cardServerName){
		Deck deckInstance = deckService.createDeck(previewDeckInstance.name,
			previewDeckInstance.description, previewDeckInstance.language.id.toString(),sourceLanguage.id.toString(),cardServerName);
			
		def previewCardInstances= PreviewCard.findAllByDeck(previewDeckInstance)
		Language lang = previewDeckInstance.language
		Language lang2 = sourceLanguage
		for ( card in previewCardInstances) {
			// Remove html
			//if ("Text".equals(card.types.get(i)))
			  // unit = unit.replaceAll("\\<.*?>"," ");
			String symbolString = card.units.get( fieldIndices.get("Literal"));
			String meaningString = card.units.get( fieldIndices.get("Meaning"));
			String imageURL = null
			String pronunciationString = null
			String audioInstanceId=null
			String audioURL = null
			String alphabet1 = null
			String alphabet2 = null
			Alphabet alphabetp = null

			if (symbolString == null || symbolString.length() < 1 || meaningString == null || meaningString.length() < 1)
				continue // Skip if missing data
			try { // Missing media is OK
				imageURL = card.units.get( fieldIndices.get("Image"));
				imageURL = remapMedia(imageURL)
			} catch (Exception e) {}
			try { audioURL = card.units.get( fieldIndices.get("Sound"));
				audioURL = remapMedia(audioURL)
			} catch (Exception e){}
			try { pronunciationString = card.units.get( fieldIndices.get("Pronunciation")); } catch (Exception e){}
			try { alphabet1 = alphaIndices.get(fieldIndices.get("Literal")); } catch (Exception e){}
			try { alphabet2 = alphaIndices.get(fieldIndices.get("Meaning")); } catch (Exception e){}
			try { def al = alphaIndices.get(fieldIndices.get("Pronunciation"));
				alphabetp = Alphabet.findByName(al)
				if (alphabetp == null) {
					alphabetp = Alphabet.findByLanguage(lang)
				}
			} catch (Exception e){}
					
			// Objects to build flashcard
			Unit symbol = languageService.getUnit(symbolString, lang )
			Unit meaning = languageService.getUnit(meaningString, deckInstance.sourceLanguage)

			if (pronunciationString != null && pronunciationString.length() > 0) {
				Pronunciation pronunciation
				def audioInstance=null
				try {
					if (alphabetp != null)
						pronunciation =  languageService.getPronunciationAlphabet(pronunciationString, symbol, alphabetp)
					else
						pronunciation = languageService.getPronunciation(pronunciationString, symbol, deckInstance.language)
					if (audioURL != null) {
						audioInstance = mediaService.createAudio(audioURL,null, pronunciation.id.toString())
						audioInstanceId= audioInstance.id.toString()
					}
					if (pronunciationString == null || pronunciationString.length() < 1) {
						println(symbol +" has no pronunciation " +  alphabetp)
						pronunciationString = null
					} else
						pronunciationString = pronunciation.id.toString()
				} catch (Exception e) {
					println("Problem with audio, skip the audio")
					
				}
			} else {
				pronunciationString = null
			}
			UnitMapping unitMapping = languageService.getUnitMapping(symbol, meaning)
			flashcardService.createFlashcard(symbol.id.toString(), unitMapping.id.toString(), pronunciationString, imageURL, audioInstanceId, deckInstance.id.toString())
		}
		return deckInstance
	}
	
	@Transactional
	def createPreviewDeck(String fullPath, String mediaDir, String filename, String description, Language language, Document document){
		AnkiFile anki = new AnkiFile(fullPath,mediaDir)
		def nCards = anki.totalCards
		def folder = anki.getTmpDir()
		def decks = anki.getDeckIterator()
		def cardfields = anki.getModels().values()
		// A place to hold all the cards
		def user= User.load(springSecurityService.principal.id)
		user.id = springSecurityService.principal.id
		//def owner = springSecurityService.currentUser
		PreviewDeck previewDeckInstance = new PreviewDeck(owner: user, filename: filename, name: filename, description:description,language:language,document: document,mediaDir: mediaDir);
		previewDeckInstance.save(flush:true)
		def nfields = anki.fieldNames.size()
		while (decks.hasNext()) {
			def deck = decks.next();
			ArrayList cardList =  deck.getCardList()
			// Save each card
			def ncards = cardList.size();
			for (int i=0;i<ncards;i++) {
				Card card = cardList.get(i)
				def fieldTypes = anki.fieldTypes;
				def fieldNames = anki.fieldNames;
				PreviewCard pc=new PreviewCard(deck: previewDeckInstance)
				for (String cardfield in card.fields) {
					pc.addToUnits(cardfield.take(255)) }
				for (String fieldtype in fieldTypes) {
					 pc.addToTypes(fieldtype) }
				for (String fieldname in fieldNames) {
					 pc.addToFields(fieldname) }
				pc.save flush:true
			}
		}
		return previewDeckInstance;
	}


	/**
	 * Creates an arraylist of all the fields imported
	 * @param fieldList
	 * @param card - contains all the fields in the card
	 * @param alphabet
	 * @return
	 */
	def createPreviewUnits(fieldList,card){
		ArrayList<Unit> units=new ArrayList<Unit>()
		int i=0;
		for (field in card.fields) {
			def unit = new Unit(alphabet: alphabetList.get(i), literal: field);
			units.add(unit);
			i++;
		}
		return units;
	}
	/**
	* Retrieves the next flashcard that the user should preview 
	* @param cardUsage: the usage for the flashcard being ranked
	* @param ranking that the user gave to this flashcard
	* @return the new usage for the next flashcard
	*/
	PreviewCard getNextFlashcard(PreviewDeck deckInstance, int cardId,int ccount) {
		def cards =
		PreviewCard.findAll("from PreviewCard as pc where " +
					 "deck = :deck " +
					 "order by pc.id asc",
					 [deck: deckInstance, max: ccount, offset: cardId])
		return cards;	
	}

	/**
    * Create a new deck owned by the currently logged in user
    */
    PreviewDeck createDeck(String title, String description, String languageId) {
    	User theUser = User.load(springSecurityService.principal.id)
    	PreviewDeck deck = new PreviewDeck(title: title, 
    		description: description, 
    		owner: theUser, 
    		language: Language.load(languageId))
    	deck.save()
    	return deck
    }

    /** 
    * Update an existing deck
    */
    void updateDeck(PreviewDeck deckInstance, String name, String description, String languageId, String cardServerName) {
    	deckInstance.name = name
    	deckInstance.description = description
    	deckInstance.language = Language.load(languageId)
    	deckInstance.save()
    }   	

    Boolean removeDeck(PreviewDeck deck) {
    	User theUser = User.load(springSecurityService.principal.id)
		
    }

    /**
    * Search for Decks
    */
    List<PreviewDeck> searchDecks(Long languageId, String keyword) {
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
            		ilike("name", "%${keyword}%")
            		ilike("description", "%${keyword}%")
            	}
            }
        }
    }

    /**
    * Delete deck
    */
    void deleteDeck(PreviewDeck deckInstance) {
    	// First delete all flashcards
    	PreviewCard.findAllByDeck(deckInstance).each {
    		it.delete(it)
    	}
		// Delete all associated media
		try {
			File mediaDir= new File(deckInstance.mediaDir)
			FileUtils.delete(mediaDir)
		} catch (IOException) {
			println("Cannot delete media")
		}
    	// Now delete it
    	deckInstance.delete()
    }

    
    PreviewCard getRandomFlashcard(PreviewCard flashcardInstance) {
    	PreviewCard.executeQuery('FROM PreviewCard WHERE deck = ? AND id <> ? ORDER BY rand()', [flashcardInstance.deck, flashcardInstance.id], [max: 1])[0]
    }

    /**
    * Get a random flashcard from a deck where the given flashcard lives but is not any of the given flashcards
    */
    PreviewCard getRandomFlashcard(PreviewCard flashcardInstance, def flashcardIds) {
        def query = "FROM PreviewCard WHERE deck.id = " + flashcardInstance.deck.id + " AND id NOT IN (" + flashcardIds.join(",") + ") ORDER BY rand()"
        PreviewCard.executeQuery(query, [max: 1])[0]
    }

    /**
    * Load a deck from a CSV - returns a list with any errors that might have happened during upload
    */
    List loadFlashcardsFromCSV(PreviewDeck deckInstance, def f) {
        List result

        if(f.fileItem){
            // Create a temporary file with the uploaded contents
            def extension = f.fileItem.name.lastIndexOf('.').with {it != -1 ? f.fileItem.name.substring(it + 1) : f.fileItem.name}
            def outputFile = new File("${new Date().time}.${extension}")
            f.transferTo(outputFile)

            // Validate the file first
            result = validateCSV(outputFile.path)
            if(!result.isEmpty()) {
                return result
            }
            
            // Everything looks ok, lets save
            new File(outputFile.path).toCsvReader(['skipLines':1]).eachLine { tokens ->
                String symbolString = tokens[0]
                String meaningString = tokens[1]
                String pronunciationString = tokens[2]
                String imageURL = tokens[3]

                // Objects to build flashcard
                Unit symbol = languageService.getUnit(symbolString, deckInstance.language)
                Unit meaning = languageService.getUnit(meaningString, deckInstance.sourceLanguage)
                Pronunciation pronunciation = languageService.getPronunciation(pronunciationString, symbol, deckInstance.language)
                UnitMapping unitMapping = languageService.getUnitMapping(symbol, meaning)
                
                // Now build the card
				/********* DO NOT USE *********/
                flashcardService.createFlashcard(symbol.id.toString(), unitMapping.id.toString(), pronunciation.id.toString(), imageURL, null, deckInstance.id.toString())
            }

            // Cleanup
            outputFile.delete()
        } 
        else {
            result << "File not found"
        }

        return result
    }

    /**
    * Check that each row has a unit, a meaning and a pronunciation
    * Returns a list with any errors
    */
    List validateCSV(String filePath) {
        List result = []
        int i = 0
        new File(filePath).toCsvReader(['skipLines':1]).eachLine { tokens ->

            // Check that there's a meaning a pronunciation and a symbol
            if(!tokens[0]) {
                result << "Row ${i} is missing a symbol"
            }

            if(!tokens[1]) {
                result << "Row ${i} is missing a meaning"   
            }
            
            if(!tokens[2]) {
                result << "Row ${i} is missing a pronunciation"
            }

            i++
        }

        return result
    }
}