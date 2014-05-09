<%@ page import="com.openfluency.media.Audio" %>
<div class="flashcard animated slideInRight">
	<div class="panel panel-default">
		<div class="panel-body">
			<div class="flashcard-header" style="background-image: url(${flashcardInstance.image?.url});">
				<div class="card-actions">
					<g:if test="${isOwner}">
						<g:link action="delete" controller="flashcard" id="${flashcardInstance.id}" class="btn btn-danger btn-xs flashcard-delete-${flashcardInstance.id}">
							<span class="glyphicon glyphicon-remove"></span>
						</g:link>
					</g:if>
					<g:if test="${practicing}">
						<a href="#" id="customizationBtn" title="Add your own image or audio pronunciation file to help you learn!">
							<span class="glyphicon glyphicon-pencil"></span>
						</a>
					</g:if>
				</div>
				<h1 class="flashcard-unit">${flashcardInstance?.primaryUnit.print}</h1>
			</div>
		</div>
		<div class="panel-footer">
			<div class="pronunciation">
				pronounced "${flashcardInstance?.pronunciation.literal}"
				<g:if test="${!audioSysId}">
					<g:set var="audioSysId" value="${flashcardInstance?.audio?.id}" />
				</g:if>

				<g:if test="${audioSysId}">
					<g:set var="audioSource" value="/OpenFluency/audio/sourceAudio/"/>
					<g:set var="audioId" value="${audioSysId}"/>
					<span class="play-audio glyphicon glyphicon-volume-up"></span>
					<audio class="flashcard-audio hidden" id="flashcard-audio-${audioId}" src="${audioSource + audioId}" controls></audio>
				</g:if>
			</div>
			<div class="meaning">${flashcardInstance?.secondaryUnit.print}</div>
		</div>
	</div>
</div>