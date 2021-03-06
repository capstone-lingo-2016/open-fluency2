
package cscie99.team2.lingolearn.server.confuser;

import static org.junit.Assert.assertEquals
import org.junit.Test
import cscie99.team2.lingolearn.shared.Card
import cscie99.team2.lingolearn.shared.error.ConfuserException

/**
 * This class tests to make sure that the values being generated by the confuser
 * algorithm are valid.
 */
class ConfuserTest extends GroovyTestCase {
	
	// The confuser instance to use for tests
	private final Confuser confuser = new Confuser();

	/**
	 * This helper function checks to ensure that the results provided
	 * match the expected results.
	 *
	 * @param results The results from the test.
	 * @param expected The reference set of expected results.
	 */
	private void checkResults(List<String> results, String[] expected) {
		assertEquals(expected.length, results.size());
		for (String phrase : expected) {
			assertEquals(true, results.contains(phrase));
		}
	}
	
	/**
	 * Test to make sure vowels are elongated correctly for hiragana words.
	 */
	public void testHiraganaVowelElongationTest() {
		// Test to make sure え (picture) is extended correctly into ええ (yes)
		String[] expected = [ "ええ" ] as String[];
		checkResults(confuser.getHiraganaManipulation("え"), expected);
		
		// Test to make sure おばさん (aunt) is extended correctly
		expected = [ "おばあさん", "おばさあん" ] as String[];
		checkResults(confuser.getHiraganaManipulation("おばさん"), expected);
		
		// Test to make sure おばあさん (grandmother) is extended correctly
		expected = [ "おばあさあん" ]  as String[];
		checkResults(confuser.getHiraganaManipulation("おばあさん"), expected);
		
		// Test to make sure おじさん (uncle) is extended correctly
		expected = [ "おじいさん", "おじさあん" ]  as String[];
		checkResults(confuser.getHiraganaManipulation("おじさん"), expected);
		
		// Test to make sure おじいさん (grandfather) is extended correctly
		expected = [ "おじいさあん" ]  as String[];
		checkResults(confuser.getHiraganaManipulation("おじいさん"), expected);
		
		// Test to make sure しょうたい (invitation) is extended correctly, in
		// theory, nothing should be returned
		checkResults(confuser.getHiraganaManipulation("しょうたい"),  [] as String[] );
	}
	
	/**
	 * Test to make sure that we can extend kanji with valid hiragana.
	 */
	public void testKanjiBoundriesTest() throws ConfuserException {
		// Test to make sure 食べ難い (difficult to eat) is extended correctly
		Card card = new Card();
		card.setKanji("食べ難い");
		card.setHiragana("たべにくい");
		List<String> results = confuser.getKanjiBoundries(card);
		assertEquals(1, results.size());
		assertEquals("食べ難くい", results.get(0));

		// Test to make sure 青い (blue) is extended correctly
		card = new Card();
		card.setKanji("青い");
		card.setHiragana("あおい");
		results = confuser.getKanjiBoundries(card);
		assertEquals(1, results.size());
		assertEquals("青おい", results.get(0));

		// Test to make sure 話す (to speak) is extended correctly
		card = new Card();
		card.setKanji("話す");
		card.setHiragana("はなす");
		results = confuser.getKanjiBoundries(card);
		assertEquals(1, results.size());
		assertEquals("話なす", results.get(0));

		// Test to make sure お結び (rice ball) is extended correctly
		card = new Card();
		card.setKanji("お結び");
		card.setHiragana("おむすび");
		results = confuser.getKanjiBoundries(card);
		assertEquals(1, results.size());
		assertEquals("お結すび", results.get(0));

		// Test to make sure the する (to do) suffix is not extended
		card = new Card();
		card.setKanji("勉強する");
		card.setHiragana("べんきょうする");
		results = confuser.getKanjiBoundries(card);
		assertEquals(0, results.size());
		
		// Test to make sure 朝御飯 (breakfast) is not extended
		card = new Card();
		card.setKanji("朝御飯");
		card.setHiragana("あさごはん");
		results = confuser.getKanjiBoundries(card);
		assertEquals(0, results.size());
		
		// Test to make sure お菓子 (sweets, candy) is not extended
		card = new Card();
		card.setKanji("お菓子");
		card.setHiragana("おかし");
		results = confuser.getKanjiBoundries(card);
		assertEquals(0, results.size());
	}
	
	/**
	 * Test to make sure kanji substitution works correctly.
	 */
	public void testKanjiSubsitutionTest() throws ConfuserException, IOException {
		// TODO Get more unit tests for this function, we need one where the
		// TODO replacement kanji is in the middle and one were the replacement
		// TODO kanji is at the end
		
		// Test to make sure 介護 (nursing) is manipulated correctly, in this
		// case, the first kanji will be replaced
		String[] expected = [ "价護", "堺護", "界護", "畍護", "疥護", "芥護"]  as String[];
		checkResults(confuser.getKanjiSubsitution("介護"), expected);
		
		// Test to make sure 党議拘束 (compulsory adherence to a party decision;
		// restrictions on party debate) is manipulated correctly, in this case
		// the second kanji will be replaced
		expected = ["党儀拘束", "党嶬拘束", "党犠拘束", "党礒拘束", "党義拘束", "党艤拘束", "党蟻拘束"]  as String[];
		checkResults(confuser.getKanjiSubsitution("党議拘束"), expected);
		
		// Test to make sure 動詞 (verb) is manipulated correctly, in this case
		// the last kanji will be replaced
		expected = [ "動伺", "動司", "動嗣", "動祠", "動笥", "動覗", "動飼" ]  as String[];
		checkResults(confuser.getKanjiSubsitution("動詞"), expected);
	}

	/**
	 * Test to make sure vowels are elongated correctly for katakana words.
	 */
	public void testKatakanaVowelElongationTest() {
		// Test to make sure コンピュータ (computer) is manipulated correctly
		String[] expected = [ "コーンピュータ", "コンピューター", "コンピュタ" ]  as String[];
		checkResults(confuser.getKatakanaManiuplation("コンピュータ"), expected);
		
		// Test to make sure プロジェクト (project) is being manipulated correctly
		expected = [ "プーロジェクト", "プロージェクト", "プロジェークト", "プロジェクート", "プロジェクトー" ] as String[];
		checkResults(confuser.getKatakanaManiuplation("プロジェクト"), expected);
		
		// Test to make sure エレベーター (elevator) is manipulated correctly
		expected = [ "エーレベーター", "エレーベーター", "エレベター", "エレベータ" ]  as String[];
		checkResults(confuser.getKatakanaManiuplation("エレベーター"), expected);
	}

	/**
	 * Test to make sure the small tsu character (ッ) is being manipulated
	 * correctly.
	 */
	public void testSmallTsuManipulation() {
		// Test to make sure ヒット (hit) is manipulated correctly
		List<String> results = confuser.getSmallTsuManiuplation("ヒット");
		assertEquals(1, results.size());
		assertEquals("ヒト", results.get(0));
		
		// Test to make sure ヒト  (person) is manipulated correctly
		results = confuser.getSmallTsuManiuplation("ヒト");
		assertEquals(1, results.size());
		assertEquals("ヒット", results.get(0));
		
		// Test to make sure としょかん (library) is manipulated correctly
		results = confuser.getSmallTsuManiuplation("としょかん");
		assertEquals(1, results.size());
		assertEquals("とっしょかん", results.get(0));
	}

	/**
	 * Test to make sure the n character (ん, ン) is being manipulated correctly
	 * for hiragana and katakana.
	 */
	public void testNManipulation() throws ConfuserException {
		// Test to make sure テニス (tennis) is manipulated correctly
		List<String> results = confuser.getNManipulation("テニス");
		assertEquals(1, results.size());
		assertEquals("テンニス", results.get(0));
		
		// Test to make sure なのか (seven days) is manipulated correctly
		results = confuser.getNManipulation("なのか");
		assertEquals(1, results.size());
		assertEquals("なんのか", results.get(0));
		
		// Test to make sure こんなん  (stress) is manipulated correctly
		results = confuser.getNManipulation("こんなん");
		assertEquals(1, results.size());
		assertEquals("こなん", results.get(0));
	}
}
