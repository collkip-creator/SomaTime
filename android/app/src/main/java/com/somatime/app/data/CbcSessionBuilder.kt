package com.somatime.app.data

import com.somatime.app.model.CBCLesson
import com.somatime.app.model.LearningArea
import com.somatime.app.model.QuestionType
import com.somatime.app.model.QuizQuestion
import kotlin.math.abs

data class TaggedBonusQuestion(
    val learningArea: LearningArea,
    val levelGroup: String,
    val question: QuizQuestion
)

object CbcSessionBuilder {

    fun questionCountForGrade(grade: Int): Int = when (grade.coerceIn(1, 12)) {
        1 -> 3
        2 -> 4
        3 -> 4
        4 -> 5
        5 -> 5
        6 -> 6
        7 -> 7
        8 -> 7
        9 -> 8
        10 -> 9
        11 -> 9
        else -> 10
    }

    private val bonusQuestions: List<TaggedBonusQuestion> = listOf(
        // Lower Primary
        TaggedBonusQuestion(
            LearningArea.MATHEMATICS,
            "Lower Primary",
            QuizQuestion(
                id = "ext-lp-math-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Kamau counts in tens: 20, 30, 40, ___. What number comes next?",
                hint = "Hint: Add 10 to 40.",
                options = listOf("50", "45", "60", "41"),
                correctAnswer = "50",
                explanation = "Well done! Counting forward by tens after 40 gives 50."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.ENGLISH,
            "Lower Primary",
            QuizQuestion(
                id = "ext-lp-eng-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "What is the plural of the word \"mango\"?",
                hint = "Hint: Words ending in -o often add -es.",
                options = listOf("mangoes", "mangos", "mangoies", "mango"),
                correctAnswer = "mangoes",
                explanation = "Correct! One mango, many mangoes."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.KISWAHILI,
            "Lower Primary",
            QuizQuestion(
                id = "ext-lp-kisw-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Chagua wingi wa neno \"mtoto\" (Choose the plural of child):",
                hint = "Hint: Ngeli ya A-WA hubadilisha m- kuwa wa-.",
                options = listOf("watoto", "mitoto", "vitoto", "matoto"),
                correctAnswer = "watoto",
                explanation = "Vizuri! Umoja ni \"mtoto\" na wingi ni \"watoto\"."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.ENVIRONMENTAL_ACTIVITIES,
            "Lower Primary",
            QuizQuestion(
                id = "ext-lp-env-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which domestic animal gives Kenyan families fresh milk every morning?",
                hint = "Hint: A dairy farm animal.",
                options = listOf("Cow", "Chicken", "Cat", "Donkey"),
                correctAnswer = "Cow",
                explanation = "Correct! Dairy cows and goats provide nutritious milk."
            )
        ),

        // Upper Primary
        TaggedBonusQuestion(
            LearningArea.MATHEMATICS,
            "Upper Primary",
            QuizQuestion(
                id = "ext-up-math-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "What is the perimeter of a rectangular school garden measuring 12 m long and 8 m wide?",
                hint = "Hint: Add all four sides: 12 + 8 + 12 + 8.",
                options = listOf("40 m", "96 m", "20 m", "32 m"),
                correctAnswer = "40 m",
                explanation = "Spot on! Perimeter = 2 x (12 + 8) = 40 metres."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.MATHEMATICS,
            "Upper Primary",
            QuizQuestion(
                id = "ext-up-math-2",
                type = QuestionType.FILL_BLANK,
                prompt = "Convert 3 kilograms (kg) of sugar into grams (g).",
                hint = "Hint: 1 kg equals 1,000 grams.",
                blankPrefix = "3 kg =",
                blankSuffix = "grams",
                correctAnswer = "3000",
                acceptableAnswers = listOf("3000", "3,000"),
                explanation = "Correct! 3 x 1,000 = 3,000 grams."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.ENGLISH,
            "Upper Primary",
            QuizQuestion(
                id = "ext-up-eng-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Choose the correct past tense verb: \"Yesterday, our class ________ Lake Naivasha.\"",
                hint = "Hint: Regular verbs add -ed for past simple.",
                options = listOf("visited", "visits", "visiting", "will visit"),
                correctAnswer = "visited",
                explanation = "Well done! \"Yesterday\" signals past tense: \"visited\"."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.KISWAHILI,
            "Upper Primary",
            QuizQuestion(
                id = "ext-up-kisw-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Kamilisha methali hii: \"Haraka haraka haina ________.\"",
                hint = "Hint: Haste has no blessing (starts with B).",
                options = listOf("baraka", "harara", "faida", "hasara"),
                correctAnswer = "baraka",
                explanation = "Safi sana! \"Haraka haraka haina baraka\" hufunza subira na umakini."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.SCIENCE_TECH,
            "Upper Primary",
            QuizQuestion(
                id = "ext-up-sci-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which gas in the air do green plants absorb during photosynthesis to make food?",
                hint = "Hint: The gas humans breathe out.",
                options = listOf("Carbon dioxide", "Nitrogen", "Argon", "Helium"),
                correctAnswer = "Carbon dioxide",
                explanation = "Correct! Plants take in carbon dioxide through tiny leaf stomata."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.SOCIAL_STUDIES,
            "Upper Primary",
            QuizQuestion(
                id = "ext-up-sst-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which Kenyan city serves as the national capital and hosts the headquarters of UNEP?",
                hint = "Hint: Known as the Green City in the Sun.",
                options = listOf("Nairobi", "Kisumu", "Eldoret", "Nakuru"),
                correctAnswer = "Nairobi",
                explanation = "Correct! Nairobi is Kenya's capital and hosts the UN Environment Programme."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.AGRICULTURE_NUTRITION,
            "Upper Primary",
            QuizQuestion(
                id = "ext-up-agri-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which food group mainly repairs worn-out body tissues and builds strong muscles?",
                hint = "Hint: Found in beans, fish, eggs, and lean meat.",
                options = listOf("Proteins", "Carbohydrates", "Fats", "Water only"),
                correctAnswer = "Proteins",
                explanation = "Accurate! Beans, fish, milk, and eggs are rich in body-building proteins."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.RELIGIOUS_EDUCATION,
            "Upper Primary",
            QuizQuestion(
                id = "ext-up-re-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Why is planting trees around our school compound an act of good stewardship?",
                hint = "Hint: Think about protecting nature for future generations.",
                options = listOf(
                    "It conserves soil, shade, and clean air for all",
                    "It blocks rain from reaching rivers",
                    "It increases plastic litter",
                    "It removes oxygen from air"
                ),
                correctAnswer = "It conserves soil, shade, and clean air for all",
                explanation = "Well done! Stewardship means caring responsibly for the environment."
            )
        ),

        // Junior School
        TaggedBonusQuestion(
            LearningArea.MATHEMATICS,
            "Junior School",
            QuizQuestion(
                id = "ext-js-math-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Simplify the algebraic expression: 5a + 3b - 2a + 4b.",
                hint = "Hint: Combine like terms (a with a, b with b).",
                options = listOf("3a + 7b", "7a + 7b", "3a - b", "8a + 2b"),
                correctAnswer = "3a + 7b",
                explanation = "Spot on! (5a - 2a) + (3b + 4b) = 3a + 7b."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.MATHEMATICS,
            "Junior School",
            QuizQuestion(
                id = "ext-js-math-2",
                type = QuestionType.FILL_BLANK,
                prompt = "What is the square root of 144?",
                hint = "Hint: Which whole number multiplied by itself gives 144?",
                blankPrefix = "sqrt(144) =",
                blankSuffix = "",
                correctAnswer = "12",
                acceptableAnswers = listOf("12", "twelve"),
                explanation = "Correct! 12 x 12 = 144."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.INTEGRATED_SCIENCE,
            "Junior School",
            QuizQuestion(
                id = "ext-js-sci-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which separating technique recovers dissolved table salt crystals from salty water?",
                hint = "Hint: Heating turns liquid water into vapour, leaving salt crystals behind.",
                options = listOf("Evaporation", "Decanting", "Magnetism", "Sieving"),
                correctAnswer = "Evaporation",
                explanation = "Accurate! Evaporation removes water vapour and leaves pure salt crystals."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.INTEGRATED_SCIENCE,
            "Junior School",
            QuizQuestion(
                id = "ext-js-sci-2",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "What is the SI unit of electric current?",
                hint = "Hint: Symbol is A.",
                options = listOf("Ampere", "Volt", "Watt", "Joule"),
                correctAnswer = "Ampere",
                explanation = "Correct! Electric current is measured in Amperes (A)."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.COMPUTER_STUDIES,
            "Junior School",
            QuizQuestion(
                id = "ext-js-comp-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which cybersecurity habit best protects a learner's online school portal account?",
                hint = "Hint: Never share credentials and use unique characters.",
                options = listOf(
                    "Using a strong unique password and keeping it private",
                    "Writing the password on the classroom desk",
                    "Using 1234 on every website",
                    "Clicking unknown prize links"
                ),
                correctAnswer = "Using a strong unique password and keeping it private",
                explanation = "Great digital citizenship! Strong private passwords prevent unauthorized access."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.PRE_TECHNICAL_STUDIES,
            "Junior School",
            QuizQuestion(
                id = "ext-js-pt-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "In an isometric sketch, receding horizontal edges are drawn at what angle to the horizontal line?",
                hint = "Hint: Standard isometric angle used with a 30/60 set square.",
                options = listOf("30 degrees", "90 degrees", "15 degrees", "75 degrees"),
                correctAnswer = "30 degrees",
                explanation = "Spot on! Isometric drawings project horizontal axes at 30 degrees."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.BUSINESS_STUDIES,
            "Junior School",
            QuizQuestion(
                id = "ext-js-biz-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which Kenyan mobile money option lets a trader receive payments via a Till or Paybill number?",
                hint = "Hint: Lipa na M-Pesa merchant option.",
                options = listOf("Lipa na M-Pesa (Buy Goods / Paybill)", "Airplane Mode", "Bluetooth Pairing", "SIM PIN Lock"),
                correctAnswer = "Lipa na M-Pesa (Buy Goods / Paybill)",
                explanation = "Correct! Merchant Till and Paybill numbers streamline digital business records."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.ENGLISH,
            "Junior School",
            QuizQuestion(
                id = "ext-js-eng-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Identify the passive voice version of: \"The students planted fifty trees.\"",
                hint = "Hint: Make \"Fifty trees\" the subject of the sentence.",
                options = listOf(
                    "Fifty trees were planted by the students.",
                    "The students are planting fifty trees.",
                    "Fifty trees planted the students.",
                    "The students will plant fifty trees."
                ),
                correctAnswer = "Fifty trees were planted by the students.",
                explanation = "Well done! In passive voice, \"Fifty trees\" moves to subject position."
            )
        ),

        // Senior School
        TaggedBonusQuestion(
            LearningArea.MATHEMATICS,
            "Senior School",
            QuizQuestion(
                id = "ext-ss-math-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Solve the quadratic equation x^2 - 5x + 6 = 0.",
                hint = "Hint: Find two numbers whose sum is 5 and product is 6.",
                options = listOf("x = 2 or x = 3", "x = -2 or x = -3", "x = 1 or x = 6", "x = 5 or x = 6"),
                correctAnswer = "x = 2 or x = 3",
                explanation = "Correct! (x - 2)(x - 3) = 0 gives roots x = 2 and x = 3."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.MATHEMATICS,
            "Senior School",
            QuizQuestion(
                id = "ext-ss-math-2",
                type = QuestionType.FILL_BLANK,
                prompt = "In a right-angled triangle, if sin(theta) = 3/5, what is cos(theta) when theta is acute?",
                hint = "Hint: Use the 3-4-5 Pythagorean triple.",
                blankPrefix = "cos(theta) =",
                blankSuffix = "",
                correctAnswer = "4/5",
                acceptableAnswers = listOf("4/5", "0.8"),
                explanation = "Accurate! From 3^2 + 4^2 = 5^2, adjacent = 4, so cos(theta) = 4/5."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.BIOLOGY,
            "Senior School",
            QuizQuestion(
                id = "ext-ss-bio-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "During aerobic respiration inside mitochondria, glucose reacts with oxygen to release CO2, water, and ________.",
                hint = "Hint: Cellular energy molecule abbreviated ATP.",
                options = listOf("ATP energy", "Chlorophyll", "Lactic acid only", "Nitrogen gas"),
                correctAnswer = "ATP energy",
                explanation = "Spot on! Mitochondria synthesize ATP to power cellular activities."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.CHEMISTRY,
            "Senior School",
            QuizQuestion(
                id = "ext-ss-chem-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "How many moles of solute are present in 2 litres of a 0.5 M sodium chloride solution?",
                hint = "Hint: Moles = Molarity (mol/L) x Volume (L).",
                options = listOf("1.0 mol", "0.25 mol", "2.5 mol", "4.0 mol"),
                correctAnswer = "1.0 mol",
                explanation = "Correct! 0.5 mol/L x 2 L = 1.0 mole."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.PHYSICS,
            "Senior School",
            QuizQuestion(
                id = "ext-ss-phy-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "A matatu accelerates uniformly from rest to 20 m/s in 10 seconds. What is its acceleration?",
                hint = "Hint: a = (v - u) / t.",
                options = listOf("2 m/s^2", "200 m/s^2", "10 m/s^2", "0.5 m/s^2"),
                correctAnswer = "2 m/s^2",
                explanation = "Accurate! a = (20 - 0) / 10 = 2 m/s^2."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.GEOGRAPHY,
            "Senior School",
            QuizQuestion(
                id = "ext-ss-geo-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Why is Lake Naivasha freshwater unlike most other Kenyan Rift Valley lakes such as Magadi or Bogoria?",
                hint = "Hint: Think about underground water outflow keeping salts from concentrating.",
                options = listOf(
                    "It has underground outflow/seepage preventing salt buildup",
                    "It is located in the Indian Ocean",
                    "It freezes every July",
                    "It receives zero river inflow"
                ),
                correctAnswer = "It has underground outflow/seepage preventing salt buildup",
                explanation = "Well done! Subsurface seepage refreshes Lake Naivasha's water continuously."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.HISTORY_CITIZENSHIP,
            "Senior School",
            QuizQuestion(
                id = "ext-ss-hist-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Which principle of Kenyan public finance ensures citizens participate in county budget priorities before approval?",
                hint = "Hint: Open community consultation mandated by the 2010 Constitution.",
                options = listOf(
                    "Public participation and accountability",
                    "Secret taxation without notice",
                    "Single-person budgeting",
                    "Abolishing county assemblies"
                ),
                correctAnswer = "Public participation and accountability",
                explanation = "Spot on! Article 201 emphasizes openness, accountability, and public participation."
            )
        ),
        TaggedBonusQuestion(
            LearningArea.INTEGRATED_SCIENCE,
            "Senior School",
            QuizQuestion(
                id = "ext-ss-sci-1",
                type = QuestionType.MULTIPLE_CHOICE,
                prompt = "Why does geothermal power in Olkaria provide reliable baseload electricity compared to solar alone?",
                hint = "Hint: Underground earth heat is available day and night.",
                options = listOf(
                    "Geothermal steam flows 24/7 regardless of sunlight or clouds",
                    "Geothermal plants only work at noon",
                    "Geothermal steam requires imported coal",
                    "Turbines stop spinning at night"
                ),
                correctAnswer = "Geothermal steam flows 24/7 regardless of sunlight or clouds",
                explanation = "Correct! Geothermal energy delivers steady 24-hour clean baseload power."
            )
        )
    )

    fun scaleLessonForGrade(
        lesson: CBCLesson,
        learnerGrade: Int,
        allLessons: List<CBCLesson>,
        levelGroup: String
    ): CBCLesson {
        val targetCount = questionCountForGrade(learnerGrade)
        val chosen = mutableListOf<QuizQuestion>()
        val seenIds = mutableSetOf<String>()

        fun pushUnique(q: QuizQuestion) {
            if (chosen.size >= targetCount) return
            if (seenIds.add(q.id)) {
                chosen.add(q)
            }
        }

        // 1. Primary lesson/strand questions first
        lesson.questions.forEach(::pushUnique)
        if (chosen.size >= targetCount) {
            return lesson.copy(questions = chosen.take(targetCount))
        }

        // 2. Same subject + same level group bonus questions
        bonusQuestions
            .filter { it.learningArea == lesson.learningArea && it.levelGroup == levelGroup }
            .shuffled()
            .forEach { pushUnique(it.question) }

        // 3. Same learning area from other lessons (nearest grade first)
        allLessons
            .filter { it.learningArea == lesson.learningArea && it.id != lesson.id }
            .sortedBy { abs(it.grade - learnerGrade) }
            .forEach { it.questions.forEach(::pushUnique) }

        // 4. Other same-subject bonus questions
        bonusQuestions
            .filter { it.learningArea == lesson.learningArea }
            .shuffled()
            .forEach { pushUnique(it.question) }

        // 5. Same level group bonus + lessons (broadens CBC revision)
        bonusQuestions
            .filter { it.levelGroup == levelGroup }
            .shuffled()
            .forEach { pushUnique(it.question) }

        allLessons
            .filter { it.levelGroup == levelGroup }
            .shuffled()
            .forEach { it.questions.forEach(::pushUnique) }

        // 6. Safety net across nearest grades
        if (chosen.size < targetCount) {
            allLessons
                .sortedBy { abs(it.grade - learnerGrade) }
                .forEach { it.questions.forEach(::pushUnique) }
        }

        return lesson.copy(questions = chosen)
    }
}
