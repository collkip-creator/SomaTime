package com.somatime.app.data

import com.somatime.app.model.CBCLesson
import com.somatime.app.model.CBCRubric
import com.somatime.app.model.CbcCatalogEntry
import com.somatime.app.model.ChildActivityLog
import com.somatime.app.model.ChildProfile
import com.somatime.app.model.ClassStudentRecord
import com.somatime.app.model.DeviceMode
import com.somatime.app.model.LearningArea
import com.somatime.app.model.MatchingPair
import com.somatime.app.model.PendingApproval
import com.somatime.app.model.QuestionType
import com.somatime.app.model.QuizQuestion
import com.somatime.app.model.TeacherAssignment

object CbcCurriculumRepository {

    const val PARENT_PIN = "1234"

    fun levelGroupForGrade(grade: Int): String = when {
        grade <= 3 -> "Lower Primary"
        grade <= 6 -> "Upper Primary"
        grade <= 9 -> "Junior School"
        else -> "Senior School"
    }

    val lessons: List<CBCLesson> = listOf(
        CBCLesson(
            id = "g4-math-fractions",
            title = "Fractions & Sharing KES",
            grade = 4,
            levelGroup = "Upper Primary",
            learningArea = LearningArea.MATHEMATICS,
            strand = "Numbers",
            subStrand = "Fractions in Daily Life",
            durationMins = 5,
            summary = "Represent unit and non-unit fractions using chapati shares and Kenyan Shillings (KES) word problems.",
            questions = listOf(
                QuizQuestion(
                    id = "g4-m-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Amina shares 1 whole chapati equally among 4 friends at lunch in Westlands. What fraction of the chapati does each friend receive?",
                    hint = "Hint: 1 part out of 4 equal parts is written with 1 on top and 4 on the bottom (one-quarter).",
                    options = listOf("1/2", "1/3", "1/4", "3/4"),
                    correctAnswer = "1/4",
                    explanation = "Well done! Dividing 1 whole into 4 equal parts gives each person 1/4."
                ),
                QuizQuestion(
                    id = "g4-m-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each fraction to its equivalent Kenyan Shillings (KES) share out of KES 100.",
                    hint = "Hint: Half of KES 100 is KES 50, and a quarter of KES 100 is KES 25.",
                    pairs = listOf(
                        MatchingPair("1/2 of KES 100", "KES 50"),
                        MatchingPair("1/4 of KES 100", "KES 25"),
                        MatchingPair("3/4 of KES 100", "KES 75")
                    ),
                    explanation = "Great work! 1/2 = KES 50, 1/4 = KES 25, and 3/4 = KES 75."
                ),
                QuizQuestion(
                    id = "g4-m-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "A vendor had 5/8 of a sack of kale (sukuma wiki) and sold 2/8 in the morning. What fraction remains?",
                    hint = "Hint: Subtract only the top numbers (5 - 2) because the denominator (8) is the same.",
                    blankPrefix = "Remaining fraction:",
                    blankSuffix = "of the sack",
                    correctAnswer = "3/8",
                    acceptableAnswers = listOf("3/8", "3 / 8"),
                    explanation = "Correct! When denominators are the same, subtract numerators: 5/8 - 2/8 = 3/8."
                )
            )
        ),
        CBCLesson(
            id = "g7-sci-apparatus",
            title = "Laboratory Apparatus & Safety",
            grade = 7,
            levelGroup = "Junior School",
            learningArea = LearningArea.INTEGRATED_SCIENCE,
            strand = "Scientific Investigation",
            subStrand = "Laboratory Apparatus & Instruments",
            durationMins = 5,
            summary = "Identify standard Junior School science apparatus, accurate volume measurement, and lab safety rules.",
            questions = listOf(
                QuizQuestion(
                    id = "g7-s-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "During a Junior School practical in Kisumu, Brian needs to measure exactly 45 ml of rainwater. Which apparatus gives the most accurate volume reading?",
                    hint = "Hint: Look for the tall graduated glass tube marked in millilitres (ml).",
                    options = listOf("Measuring cylinder", "Bunsen burner", "Evaporating dish", "Test tube rack"),
                    correctAnswer = "Measuring cylinder",
                    explanation = "Well done! A graduated measuring cylinder measures liquid volumes accurately."
                ),
                QuizQuestion(
                    id = "g7-s-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Integrated Science apparatus to its primary function in the lab.",
                    hint = "Hint: A Bunsen burner is used for heating; a thermometer checks temperature.",
                    pairs = listOf(
                        MatchingPair("Conical Flask", "Swirling liquids safely without spilling"),
                        MatchingPair("Thermometer", "Measuring temperature in °C"),
                        MatchingPair("Bunsen Burner", "Controlled heating with a blue flame")
                    ),
                    explanation = "Great job! Knowing apparatus functions keeps science practicals safe and precise."
                ),
                QuizQuestion(
                    id = "g7-s-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "To measure temperature in a Kenyan school laboratory, we read the thermometer in degrees ________.",
                    hint = "Hint: The metric temperature scale starting with the letter C.",
                    blankPrefix = "Unit: degrees",
                    blankSuffix = "(°C)",
                    correctAnswer = "Celsius",
                    acceptableAnswers = listOf("celsius", "centigrade", "c"),
                    explanation = "Accurate! Temperature in CBC Integrated Science labs is recorded in degrees Celsius (°C)."
                )
            )
        ),
        CBCLesson(
            id = "g2-kisw-msamiati",
            title = "Msamiati wa Nyumbani na Shuleni",
            grade = 2,
            levelGroup = "Lower Primary",
            learningArea = LearningArea.KISWAHILI,
            strand = "Kusikiliza na Kuzungumza",
            subStrand = "Msamiati wa Nyumbani",
            durationMins = 5,
            summary = "Jifunze maneno ya Kiswahili kuhusu vitu vya nyumbani na darasani kwa mifano rahisi.",
            questions = listOf(
                QuizQuestion(
                    id = "g2-k-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Katika sentensi: \"Kamau anasoma ______ kipya maktabani\", ni neno lipi linafaa?",
                    hint = "Hint (English): Something you read at the library (book).",
                    options = listOf("kitabu", "kikombe", "kiatu", "kijiko"),
                    correctAnswer = "kitabu",
                    explanation = "Hongera! \"Kitabu\" maana yake ni book, na kinasomwa maktabani."
                ),
                QuizQuestion(
                    id = "g2-k-2",
                    type = QuestionType.MATCHING,
                    prompt = "Oanisha maneno ya Kiswahili na maana zake kwa Kiingereza (Match Kiswahili to English).",
                    hint = "Hint: Mwalimu teaches in class; Shule is where learners go.",
                    pairs = listOf(
                        MatchingPair("Mwalimu", "Teacher"),
                        MatchingPair("Kikombe", "Cup"),
                        MatchingPair("Shule", "School")
                    ),
                    explanation = "Safi sana! Mwalimu = Teacher, Kikombe = Cup, Shule = School."
                ),
                QuizQuestion(
                    id = "g2-k-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Kamilisha salamu za asubuhi: \"Shikamoo!\" Jibu lake ni \"________!\"",
                    hint = "Hint: Respectful reply to Shikamoo (starts with M).",
                    blankPrefix = "Jibu:",
                    blankSuffix = "!",
                    correctAnswer = "Marahaba",
                    acceptableAnswers = listOf("marahaba"),
                    explanation = "Vizuri mno! Wakubwa hujibu \"Marahaba\" wanaposalimiwa \"Shikamoo\"."
                )
            )
        ),
        CBCLesson(
            id = "g6-sst-resources",
            title = "Kenyan Agriculture & Trade Corridors",
            grade = 6,
            levelGroup = "Upper Primary",
            learningArea = LearningArea.SOCIAL_STUDIES,
            strand = "Resources & Economic Activities",
            subStrand = "Cash Crops & Port Trade",
            durationMins = 5,
            summary = "Explore major Kenyan agricultural zones (Kericho tea, Mwea rice) and East African trade via Mombasa Port.",
            questions = listOf(
                QuizQuestion(
                    id = "g6-ss-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Which Kenyan county in the Rift Valley highlands is most famous for large-scale tea farming due to cool temperatures and high rainfall?",
                    hint = "Hint: This highland county in the Rift Valley is home to Kenya's largest tea estates.",
                    options = listOf("Kericho", "Turkana", "Garissa", "Kilifi"),
                    correctAnswer = "Kericho",
                    explanation = "Correct! Kericho's highland volcanic soils and reliable rainfall make it Kenya's leading tea region."
                ),
                QuizQuestion(
                    id = "g6-ss-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Kenyan economic centre to its primary resource or trade role.",
                    hint = "Hint: Mombasa is on the Indian Ocean coast; Mwea is an irrigation scheme.",
                    pairs = listOf(
                        MatchingPair("Port of Mombasa", "International sea imports & exports"),
                        MatchingPair("Mwea Scheme", "Irrigated rice farming"),
                        MatchingPair("Lake Victoria", "Inland freshwater fishing (Tilapia)")
                    ),
                    explanation = "Excellent! Kenya's diverse regions support agriculture, fisheries, and regional trade."
                ),
                QuizQuestion(
                    id = "g6-ss-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "The modern railway connecting Mombasa Port to Nairobi and Naivasha for faster cargo trade is called the ________.",
                    hint = "Hint: 3-letter abbreviation for Standard Gauge Railway.",
                    blankPrefix = "Abbreviation:",
                    blankSuffix = "(Standard Gauge Railway)",
                    correctAnswer = "SGR",
                    acceptableAnswers = listOf("sgr", "standard gauge railway"),
                    explanation = "Great job! The SGR connects Mombasa to Nairobi and Naivasha."
                )
            )
        ),
        CBCLesson(
            id = "g8-math-algebra",
            title = "Linear Equations & M-Pesa Budgeting",
            grade = 8,
            levelGroup = "Junior School",
            learningArea = LearningArea.MATHEMATICS,
            strand = "Algebra",
            subStrand = "Linear Equations in One Unknown",
            durationMins = 5,
            summary = "Solve single-variable linear equations grounded in Kenyan mobile money budgeting and school supply costs.",
            questions = listOf(
                QuizQuestion(
                    id = "g8-m-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Brian buys 3 geometric sets at KES x each and pays a KES 50 transport fare. His total spend is KES 500. Which equation models this?",
                    hint = "Hint: Multiply 3 items by price x, then add KES 50 to equal 500.",
                    options = listOf("3x + 50 = 500", "50x + 3 = 500", "3x - 50 = 500", "x + 53 = 500"),
                    correctAnswer = "3x + 50 = 500",
                    explanation = "Spot on! Three items at KES x give 3x, plus KES 50 fare equals KES 500."
                ),
                QuizQuestion(
                    id = "g8-m-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each linear equation to the correct value of x.",
                    hint = "Hint: Isolate x on one side by subtracting or dividing both sides equally.",
                    pairs = listOf(
                        MatchingPair("2x + 10 = 30", "x = 10"),
                        MatchingPair("5x = 125", "x = 25"),
                        MatchingPair("x - 40 = 60", "x = 100")
                    ),
                    explanation = "Well done! You balanced all three linear equations accurately."
                ),
                QuizQuestion(
                    id = "g8-m-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "From 3x + 50 = 500, what is the cost (x) of 1 geometric set in KES?",
                    hint = "Hint: Subtract 50 from 500 to get 450, then divide 450 by 3.",
                    blankPrefix = "x = KES",
                    blankSuffix = "",
                    correctAnswer = "150",
                    acceptableAnswers = listOf("150", "kes 150"),
                    explanation = "3x = 450, so x = 450 / 3 = KES 150."
                )
            )
        )
    )

    val catalog: List<CbcCatalogEntry> = listOf(
        CbcCatalogEntry(1, "Lower Primary", null, LearningArea.ENGLISH, "Listening & Speaking", "Polite Expressions", "g4-math-fractions"),
        CbcCatalogEntry(2, "Lower Primary", null, LearningArea.KISWAHILI, "Kusikiliza na Kuzungumza", "Msamiati wa Nyumbani", "g2-kisw-msamiati"),
        CbcCatalogEntry(3, "Lower Primary", null, LearningArea.CREATIVE_ARTS, "Performing & Visual Arts", "Kenyan Folk Songs & Rhythm", "g2-kisw-msamiati"),
        CbcCatalogEntry(4, "Upper Primary", null, LearningArea.MATHEMATICS, "Numbers", "Fractions in Daily Life", "g4-math-fractions"),
        CbcCatalogEntry(5, "Upper Primary", null, LearningArea.AGRICULTURE_NUTRITION, "Conservation of Resources", "Organic Compost & Sack Gardens", "g6-sst-resources"),
        CbcCatalogEntry(6, "Upper Primary", null, LearningArea.SOCIAL_STUDIES, "Resources & Economic Activities", "Cash Crops & Port Trade", "g6-sst-resources"),
        CbcCatalogEntry(7, "Junior School", null, LearningArea.INTEGRATED_SCIENCE, "Scientific Investigation", "Laboratory Apparatus & Instruments", "g7-sci-apparatus"),
        CbcCatalogEntry(8, "Junior School", null, LearningArea.MATHEMATICS, "Algebra", "Linear Equations in One Unknown", "g8-math-algebra"),
        CbcCatalogEntry(9, "Junior School", null, LearningArea.SOCIAL_STUDIES, "Citizenship & Governance", "Kenyan Constitution & Devolution", "g6-sst-resources"),
        CbcCatalogEntry(10, "Senior School", "STEM Pathway", LearningArea.INTEGRATED_SCIENCE, "Energy & Sustainability", "Renewable Power Systems", "g7-sci-apparatus"),
        CbcCatalogEntry(11, "Senior School", "Social Sciences Pathway", LearningArea.SOCIAL_STUDIES, "Regional Integration", "East African Community Trade", "g6-sst-resources"),
        CbcCatalogEntry(12, "Senior School", "Arts & Sports Science", LearningArea.CREATIVE_ARTS, "Digital Media & Design", "Kenyan Cultural Visual Storytelling", "g4-math-fractions")
    )

    val classStreams = listOf(
        "Grade 6 East - Nairobi Primary",
        "Grade 4 West - Nairobi Primary",
        "Grade 8 North - Kilimani Junior"
    )

    fun generatePairingCode(): String {
        val num = (1000..9999).random()
        return "SOMA-$num"
    }

    fun initialChildren(): List<ChildProfile> = listOf(
        ChildProfile(
            id = "amina",
            name = "Amina Wanjiku",
            grade = 4,
            school = "Nairobi Primary School",
            classStream = "Grade 4 West - Nairobi Primary",
            deviceMode = DeviceMode.HAND_OFF,
            pairingCode = "SOMA-4012",
            isPaired = false,
            dailyCapMins = 90,
            exchangeRateMins = 15,
            mandatorySubjects = listOf(LearningArea.MATHEMATICS, LearningArea.KISWAHILI),
            requireParentApproval = false,
            availableScreenSeconds = 0,
            usedScreenTodayMins = 35,
            learnedMinutesToday = 25,
            isLocked = true,
            completedLessonIds = listOf("g2-kisw-msamiati"),
            pendingApprovals = emptyList(),
            activityLogs = listOf(
                ChildActivityLog(
                    id = "log-a-1",
                    lessonId = "g2-kisw-msamiati",
                    lessonTitle = "Msamiati wa Nyumbani na Shuleni",
                    learningArea = LearningArea.KISWAHILI,
                    scorePercent = 100,
                    rubricEarned = CBCRubric.EE,
                    minutesEarned = 15,
                    status = "Credited",
                    timestamp = "08:40 AM"
                )
            ),
            rubricByArea = mapOf(
                LearningArea.MATHEMATICS to CBCRubric.ME,
                LearningArea.KISWAHILI to CBCRubric.EE,
                LearningArea.ENGLISH to CBCRubric.ME,
                LearningArea.SCIENCE_TECH to CBCRubric.AE
            ),
            streakDays = 5
        ),
        ChildProfile(
            id = "brian",
            name = "Brian Ochieng",
            grade = 8,
            school = "Kilimani Junior School",
            classStream = "Grade 8 North - Kilimani Junior",
            deviceMode = DeviceMode.OWN_PHONE,
            pairingCode = "SOMA-8009",
            isPaired = true,
            dailyCapMins = 120,
            exchangeRateMins = 15,
            mandatorySubjects = listOf(LearningArea.MATHEMATICS, LearningArea.INTEGRATED_SCIENCE),
            requireParentApproval = true,
            availableScreenSeconds = 420,
            usedScreenTodayMins = 45,
            learnedMinutesToday = 30,
            isLocked = false,
            completedLessonIds = listOf("g7-sci-apparatus"),
            pendingApprovals = listOf(
                PendingApproval(
                    id = "pend-b-1",
                    lessonTitle = "Linear Equations & M-Pesa Budgeting",
                    learningArea = LearningArea.MATHEMATICS,
                    minutesEarned = 15,
                    scorePercent = 100,
                    completedAt = "09:50 AM"
                )
            ),
            activityLogs = listOf(
                ChildActivityLog(
                    id = "log-b-1",
                    lessonId = "g7-sci-apparatus",
                    lessonTitle = "Laboratory Apparatus & Safety",
                    learningArea = LearningArea.INTEGRATED_SCIENCE,
                    scorePercent = 92,
                    rubricEarned = CBCRubric.EE,
                    minutesEarned = 15,
                    status = "Credited",
                    timestamp = "08:15 AM"
                )
            ),
            rubricByArea = mapOf(
                LearningArea.MATHEMATICS to CBCRubric.EE,
                LearningArea.INTEGRATED_SCIENCE to CBCRubric.EE,
                LearningArea.ENGLISH to CBCRubric.ME,
                LearningArea.KISWAHILI to CBCRubric.AE
            ),
            streakDays = 9
        )
    )

    fun initialAssignments(): List<TeacherAssignment> = listOf(
        TeacherAssignment(
            id = "assign-g6-sst",
            classStream = "Grade 6 East - Nairobi Primary",
            grade = 6,
            learningArea = LearningArea.SOCIAL_STUDIES,
            strand = "Resources & Economic Activities",
            subStrand = "Cash Crops & Port Trade",
            lessonId = "g6-sst-resources",
            title = "Kenyan Agriculture & Trade Corridors",
            dueLabel = "Today · 5:00 PM",
            pushedAt = "08:00 AM",
            completedByChildIds = emptyList()
        )
    )

    fun initialClassRoster(): List<ClassStudentRecord> = listOf(
        ClassStudentRecord("stu-1", "Amina Wanjiku", "NPS-4012", "Grade 4 West - Nairobi Primary", CBCRubric.EE, 94, 42, true),
        ClassStudentRecord("stu-2", "Kiprop Kipchoge", "NPS-4018", "Grade 4 West - Nairobi Primary", CBCRubric.ME, 78, 30, true),
        ClassStudentRecord("stu-3", "Zawadi Hassan", "NPS-4025", "Grade 4 West - Nairobi Primary", CBCRubric.AE, 61, 18, false),
        ClassStudentRecord("stu-4", "Faith Muthoni", "NPS-6004", "Grade 6 East - Nairobi Primary", CBCRubric.EE, 96, 48, true),
        ClassStudentRecord("stu-5", "Otieno Kevin", "NPS-6011", "Grade 6 East - Nairobi Primary", CBCRubric.ME, 82, 35, true),
        ClassStudentRecord("stu-6", "Chebet Mercy", "NPS-6019", "Grade 6 East - Nairobi Primary", CBCRubric.AE, 58, 22, false),
        ClassStudentRecord("stu-7", "Baraka Njoroge", "NPS-6027", "Grade 6 East - Nairobi Primary", CBCRubric.BE, 41, 12, false),
        ClassStudentRecord("stu-8", "Brian Ochieng", "KJS-8009", "Grade 8 North - Kilimani Junior", CBCRubric.EE, 92, 45, true),
        ClassStudentRecord("stu-9", "Wambui Ndungu", "KJS-8014", "Grade 8 North - Kilimani Junior", CBCRubric.ME, 80, 34, true),
        ClassStudentRecord("stu-10", "Abdirahman Ali", "KJS-8022", "Grade 8 North - Kilimani Junior", CBCRubric.AE, 64, 20, false)
    )
}
