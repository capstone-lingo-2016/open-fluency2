<!DOCTYPE html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title>OpenFluency</title>
</head>
<body>
    <div class="container chapter-practice">
        <div class="row">
            <ul class="breadcrumb">
                <li>
                    <a href="${createLink(uri:'/') }">Home</a>
                </li>
                <li>
                    <g:link action="search" controller="course" >Courses</g:link>
                </li>
                <li>
                    <g:link action="search" controller="course" >Search for Course</g:link>
                </li>
                <li>
                    <g:link action="show" controller="course" id="${chapterInstance.course.id}">
                        ${chapterInstance.course.getCourseNumber()}: ${chapterInstance.course.title}
                    </g:link>
                </li>
                <li>
                    <g:link action="show" controller="chapter" id="${chapterInstance.id}">${chapterInstance.title}</g:link>
                </li>
                <li>
                    <a href="#">Practice</a>
                </li>
            </ul>

            <div class="col-lg-12">
                <div class="chapter-header text-center">
                    <h1>${chapterInstance.title}</h1>
                </div>
            </div>
        </div>
        <g:render template="/deck/practiceCards" model="[id: chapterInstance.id, deckInstance: chapterInstance.deck, cardUsageInstance: cardUsageInstance, controller: 'chapter']"/>
    </div>
    
    <!-- end container -->
    <g:javascript>initializePracticeCards();</g:javascript>
</body>
</html>