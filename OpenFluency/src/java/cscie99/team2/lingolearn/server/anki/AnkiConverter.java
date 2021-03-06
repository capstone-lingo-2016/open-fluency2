package cscie99.team2.lingolearn.server.anki;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import cscie99.team2.lingolearn.server.anki.error.AnkiException;
import cscie99.team2.lingolearn.shared.Card;
import cscie99.team2.lingolearn.shared.Deck;

public class AnkiConverter {
	
	private String ankiFilename;
	private String outputFilename;
	private AnkiFile ankiFile;
	private boolean parseSucceeded;
	private boolean writeSucceeded;
	private ArrayList<String> statusMessages;
	
	private BufferedWriter writer;
	private CardWriter cardWriter;
	
	public AnkiConverter( String ankiFilename ){
		this(ankiFilename, null);
		this.outputFilename = generateOutputFilename();
	}
	
	public AnkiConverter( String ankiFilename, String outputFilename ){
		
		this.ankiFilename = ankiFilename;
		this.outputFilename = outputFilename;
		this.statusMessages = new ArrayList<String>();
		
		parseSucceeded = parseAnkiFile();
		cardWriter = CardWriterFactory.getCardWriter();
	}
	
	/**
	 * Write the Anki Cards and Decks to the output file that was
	 * specified during initialization.  Each Card will be written
	 * as a line to the output file based on the CardWriter returned
	 * by the CardWriterFactory
	 * 
	 * @return true if writing succeeds, false otherwise.
	 */
	public boolean writeOutputFile(){
		try{
			writer = new BufferedWriter( new FileWriter(outputFilename));
			Collection<Deck> parsedDecks= ankiFile.getParsedDecks().values();
			Iterator<Deck> deckItr = parsedDecks.iterator();
			while( deckItr.hasNext() ){
				Deck deck = deckItr.next();
				
				Iterator<Card> cardItr = deck.getCardList().iterator();
				while( cardItr.hasNext() ){
						Card card = cardItr.next();
						String cardOutputLine = cardWriter.writeCard(card, deck);
						writer.write(cardOutputLine);
				}
			}
			
			writer.close();
			writeSucceeded = true;
			return true;
		}catch( IOException ioe ){
			// record error messages 
			String errorMessage = "An error occurred writing to the output file: " 
					+ this.outputFilename;
			
			addStatusMessage(errorMessage);
			writeSucceeded = false;
			return false;
		}
	}
	
	/**
	 * Parse the Anki File specified during initialization.  If
	 * the anki File was not successfully parsed, an AnkiException will
	 * be caught.
	 * @return true if the Anki File was successfully parsed, false otherwise.
	 */
	public boolean parseAnkiFile(){
		try{
			ankiFile = new AnkiFile(ankiFilename);
			addStatusMessage( getParseSuccessMessage() );
			return true;
		}catch( AnkiException | IOException ae ){
			addStatusMessage( ae.getMessage() );
			return false;
		}
	}
	
	public boolean parseSucceeded() {
		return parseSucceeded;
	}

	public boolean writeSucceeded() {
		return writeSucceeded;
	}

	public Iterator<String> getStatusMessages(){
		return this.statusMessages.iterator();
	}
	
	public String getAnkiFilename() {
		return ankiFilename;
	}

	public String getOutputFilename() {
		return outputFilename;
	}

	public void setOutputFilename(String outputFilename) {
		this.outputFilename = outputFilename;
	}
	
	private String generateOutputFilename(){
		String newOutFilename = this.ankiFilename + "_output.csv";
		
		return newOutFilename;
	}
	
	/*
	 * Generate a success message based on the number of decks and
	 * Cards Parsed.
	 */
	private String getParseSuccessMessage(){
		String message = "The Anki file was successfully parsed.";
		message += "\nTotal Decks Parsed: " + ankiFile.getTotalDeckModels();
		message += "\nTotal Cards Parsed: " + ankiFile.getTotalCards();
		message += "\n";
		
		return message;
	}
	
	private void addStatusMessage(String message ){
		statusMessages.add(message);
	}
	
	/* ****************** MAIN **************************************/
}
