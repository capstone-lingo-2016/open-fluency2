package com.openfluency.course

import com.openfluency.Constants
import com.openfluency.flashcard.Flashcard
import com.openfluency.language.Language
import com.openfluency.flashcard.Deck
import com.openfluency.auth.User
import grails.transaction.Transactional

@Transactional
class QuizService {

	def deckService
	def springSecurityService

	Quiz createQuiz(String title, Date liveTime, Integer maxCardTime, Integer testElement, List flashcardIds, Course courseInstance) {

		// Create the quiz
		Quiz quizInstance = new Quiz(
			course: courseInstance, 
			title: title, 
			testElement: testElement, 
			enabled: true, 
			liveTime: liveTime, 
			maxCardTime: maxCardTime
			).save(failOnError: true)

		if(quizInstance.hasErrors()) {
			return quizInstance
		}

        // Now create the questions for each flashcard
        Random rand = new Random() // randomize the options for the questions
        
        flashcardIds.each {
        	Flashcard flashcardInstance = Flashcard.get(it)

            // First create the question itself
            Question question = new Question(quiz: quizInstance, flashcard: flashcardInstance).save()
            
            // Now create a number of options - right now it's hard coded to 3 but it can be easily user defined
            int maxOptions = 3
            (1..maxOptions).each {
            	new QuestionOption(question: question, flashcard: deckService.getRandomFlashcard(flashcardInstance)).save()
            }
        }

        return quizInstance
    }

    /**
    * Initialize the quiz: create an answer for every question in the quiz for the logged student
    * @return true on success - for now this cannot fail
    */
    Answer startQuiz(Quiz quizInstance, String sessionId) {

    	// First check that the user hasn't started this course yet
    	Integer answeredQuestions = Answer.executeQuery("""
    		SELECT count(id) FROM Answer WHERE user.id = ? AND question.quiz.id = ?
    		""", [springSecurityService.principal.id, quizInstance.id])[0]

    	// This will continue the test where they left off. We have to check if this is allowed
    	if(answeredQuestions > 0) {
    		return nextQuestion(quizInstance)
    	}

    	// This is the first time this user starts a test so create all the answers
    	quizInstance.questions.each {
    		new Answer(
    			user: User.load(springSecurityService.principal.id),
    			question: it,
    			selection: null, // not answered yet
    			status: Constants.NOT_ANSWERED,
    			sessionId: sessionId
    			).save(flush: true)
    	}

    	return nextQuestion(quizInstance)
    }

    /**
	* Get the next question in the queue that has not been answered already
	*/
	Answer nextQuestion(Quiz quizInstance) {
    	// Find the next answer in the quiz that has not yet been answered
    	Answer answer = Answer.createCriteria().list(max: 1) {
    		user { 
    			eq('id', springSecurityService.principal.id) 
    		}
    		question { 
    			eq('quiz', quizInstance) 
    		}
    		eq('status', Constants.NOT_ANSWERED) 
    		}[0]

    	// Change the status of this answer to viewed
    	if(answer) {
    		// Here it might be a good idea to change the session so that the user can continue the test
    		answer.status = Constants.VIEWED
    		answer.save()
    	} 

    	return answer
    }

    /**
    * Answer a question - it will only be answered if the status is VIEWED and the session is the same
    */
    Boolean answerQuestion(Answer answer, Long selection, String sessionId) {

		// Check that the answer can actually be answered
		if(answer.status == Constants.VIEWED && answer.sessionId == sessionId) {
			answer.selection = Flashcard.load(selection)
			answer.status = Constants.ANSWERED
			answer.save()
			return true
		}

		return false
	}

	/**
	* Get answers by logged student
	*/
	List<Answer> getAnswersByLoggedUser(Quiz quizInstance) {
		return getAnswers(quizInstance, User.load(springSecurityService.principal.id))
	}

	/**
	* Return all the answers by a given student
	*/
	List<Answer> getAnswers(Quiz quizInstance, User userInstance) {
		return Answer.withCriteria {
			eq('user', userInstance)
			question {
				eq('quiz', quizInstance)
			}
		}
	}

	/**
	* Finalize the course and create a grade for the student, only if the student has viewed all the questions
	*/
	Grade finalizeQuiz(Quiz quizInstance) {
		// Check if the number of answers that a user viewed or answered is the same as the number of questions in the quiz
		Integer completedQuestions = Answer.executeQuery("""
			SELECT count(id) 
			FROM Answer 
			WHERE (status = ? OR status = ?) AND question.quiz.id = ? AND user.id = ?
			""",
			[Constants.VIEWED, Constants.ANSWERED, quizInstance.id, springSecurityService.principal.id])[0]

		if(quizInstance.countQuestions() == completedQuestions) {
			Integer correctAnswers = Answer.executeQuery("""
				SELECT count(id) 
				FROM Answer 
				WHERE status = ? AND question.quiz.id = ? AND user.id = ? AND selection.id = question.flashcard.id
				""", [Constants.ANSWERED, quizInstance.id, springSecurityService.principal.id])[0]

			return new Grade(user: User.load(springSecurityService.principal.id), correctAnswers: correctAnswers, quiz: quizInstance).save()
		}

		// The user has not completed the course yet
		return null
	}

	Grade getGrade(Quiz quizInstance) {
		return Grade.findByUserAndQuiz(User.load(springSecurityService.principal.id), quizInstance)
	}
}