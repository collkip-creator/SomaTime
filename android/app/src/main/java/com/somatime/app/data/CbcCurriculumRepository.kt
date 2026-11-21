package com.somatime.app.data

import com.somatime.app.model.CBCLesson
import com.somatime.app.model.CBCRubric
import com.somatime.app.model.CbcCatalogEntry
import com.somatime.app.model.CbcStrandNode
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
        ),
        CBCLesson(
            id = "g1-math-coins",
            title = "Counting & Kenyan Shilling Coins",
            grade = 1,
            levelGroup = "Lower Primary",
            learningArea = LearningArea.MATHEMATICS,
            strand = "Numbers",
            subStrand = "Whole Numbers & KES Coins",
            durationMins = 5,
            summary = "Count objects up to 20 and recognize KES 1, KES 5, KES 10, and KES 20 coins.",
            questions = listOf(
                QuizQuestion(
                    id = "g1-m-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Amina has two KES 5 coins in her pocket. How much money does she have altogether?",
                    hint = "Hint: Add 5 + 5 together.",
                    options = listOf("KES 10", "KES 5", "KES 15", "KES 20"),
                    correctAnswer = "KES 10",
                    explanation = "Well done! KES 5 + KES 5 = KES 10."
                ),
                QuizQuestion(
                    id = "g1-m-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each counting group to the right total.",
                    hint = "Hint: Count carefully in ones and tens.",
                    pairs = listOf(
                        MatchingPair("4 mangoes + 3 mangoes", "7 mangoes"),
                        MatchingPair("10 pencils + 2 pencils", "12 pencils"),
                        MatchingPair("9 books - 4 books", "5 books")
                    ),
                    explanation = "Awesome counting! You solved all three sums accurately."
                ),
                QuizQuestion(
                    id = "g1-m-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Count forward by 1: 14, 15, 16, ________.",
                    hint = "Hint: The number right after 16.",
                    blankPrefix = "Next number:",
                    blankSuffix = "",
                    correctAnswer = "17",
                    acceptableAnswers = listOf("17", "seventeen"),
                    explanation = "Correct! 17 comes right after 16."
                )
            )
        ),
        CBCLesson(
            id = "g3-math-measurement",
            title = "Kenyan Time, Mass & Metres",
            grade = 3,
            levelGroup = "Lower Primary",
            learningArea = LearningArea.MATHEMATICS,
            strand = "Measurement",
            subStrand = "Length, Mass & Clock Time",
            durationMins = 5,
            summary = "Measure classroom lengths in metres, market produce in kilograms, and read hours.",
            questions = listOf(
                QuizQuestion(
                    id = "g3-m-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "A shopkeeper weighs 2 kg of maize flour and 3 kg of beans in Nakuru. What is the total mass?",
                    hint = "Hint: Add 2 kg and 3 kg.",
                    options = listOf("5 kg", "6 kg", "1 kg", "4 kg"),
                    correctAnswer = "5 kg",
                    explanation = "Great! 2 kg + 3 kg = 5 kg."
                ),
                QuizQuestion(
                    id = "g3-m-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each item to its best Kenyan CBC measurement unit.",
                    hint = "Hint: Sugar is weighed in kg; milk in litres; cloth in metres.",
                    pairs = listOf(
                        MatchingPair("Packet of sugar", "Kilograms (kg)"),
                        MatchingPair("Bottle of milk", "Litres (L)"),
                        MatchingPair("Classroom wall", "Metres (m)")
                    ),
                    explanation = "Spot on! You picked the right metric units."
                ),
                QuizQuestion(
                    id = "g3-m-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "How many minutes are in 1 full hour?",
                    hint = "Hint: A clock face has 60 small minute steps.",
                    blankPrefix = "1 hour =",
                    blankSuffix = "minutes",
                    correctAnswer = "60",
                    acceptableAnswers = listOf("60", "sixty"),
                    explanation = "Correct! There are 60 minutes in 1 hour."
                )
            )
        ),
        CBCLesson(
            id = "g5-math-decimals",
            title = "Decimals & Market Change (KES)",
            grade = 5,
            levelGroup = "Upper Primary",
            learningArea = LearningArea.MATHEMATICS,
            strand = "Numbers",
            subStrand = "Decimals in Shopping",
            durationMins = 5,
            summary = "Add and subtract decimals up to 2 decimal places using Kenyan retail prices.",
            questions = listOf(
                QuizQuestion(
                    id = "g5-m-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "A loaf of bread costs KES 65.50 and milk costs KES 60.00. What is the total bill?",
                    hint = "Hint: Add 65 + 60, then keep .50.",
                    options = listOf("KES 125.50", "KES 120.50", "KES 135.50", "KES 115.50"),
                    correctAnswer = "KES 125.50",
                    explanation = "Correct! 65.50 + 60.00 = KES 125.50."
                ),
                QuizQuestion(
                    id = "g5-m-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each fraction to its decimal equivalent.",
                    hint = "Hint: Divide the top number by 10 or 100.",
                    pairs = listOf(
                        MatchingPair("1/2", "0.5"),
                        MatchingPair("1/4", "0.25"),
                        MatchingPair("3/10", "0.3")
                    ),
                    explanation = "Great work converting fractions to decimals!"
                ),
                QuizQuestion(
                    id = "g5-m-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Evaluate: 4.75 + 1.25 = ________.",
                    hint = "Hint: 0.75 + 0.25 makes 1 whole.",
                    blankPrefix = "Answer:",
                    blankSuffix = "",
                    correctAnswer = "6",
                    acceptableAnswers = listOf("6", "6.0", "6.00"),
                    explanation = "Well done! 4.75 + 1.25 = 6.00."
                )
            )
        ),
        CBCLesson(
            id = "g9-sci-ecosystems",
            title = "Kenyan Ecosystems & Conservation",
            grade = 9,
            levelGroup = "Junior School",
            learningArea = LearningArea.INTEGRATED_SCIENCE,
            strand = "Living Things & Environment",
            subStrand = "Food Webs & Biodiversity",
            durationMins = 5,
            summary = "Analyze savanna food chains, trophic levels, and wetland conservation in Kenya.",
            questions = listOf(
                QuizQuestion(
                    id = "g9-s-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "In a Maasai Mara savanna food chain (Acacia -> Giraffe -> Lion), what role does the Acacia tree play?",
                    hint = "Hint: Plants make food from sunlight using photosynthesis.",
                    options = listOf("Producer", "Primary consumer", "Secondary consumer", "Decomposer"),
                    correctAnswer = "Producer",
                    explanation = "Correct! Green plants are producers because they convert sunlight into energy."
                ),
                QuizQuestion(
                    id = "g9-s-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Kenyan ecosystem organism to its trophic role.",
                    hint = "Hint: Herbivores eat plants; fungi recycle dead matter.",
                    pairs = listOf(
                        MatchingPair("Savanna Grass", "Producer"),
                        MatchingPair("Zebra", "Primary Consumer"),
                        MatchingPair("Soil Bacteria & Fungi", "Decomposer")
                    ),
                    explanation = "Great job! Every trophic level keeps the savanna balanced."
                ),
                QuizQuestion(
                    id = "g9-s-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "The process by which green leaves use sunlight and water to make glucose is called ________.",
                    hint = "Hint: Starts with photo-.",
                    blankPrefix = "Process:",
                    blankSuffix = "",
                    correctAnswer = "Photosynthesis",
                    acceptableAnswers = listOf("photosynthesis"),
                    explanation = "Accurate! Photosynthesis produces glucose and oxygen."
                )
            )
        ),
        CBCLesson(
            id = "g10-stem-energy",
            title = "Geothermal Energy in Olkaria",
            grade = 10,
            levelGroup = "Senior School",
            learningArea = LearningArea.INTEGRATED_SCIENCE,
            strand = "Energy & Sustainability",
            subStrand = "Renewable Power Systems",
            durationMins = 5,
            summary = "Analyze how Kenya's Rift Valley geothermal plants convert underground heat into electricity.",
            questions = listOf(
                QuizQuestion(
                    id = "g10-st-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Which energy transformation occurs inside an Olkaria geothermal power station?",
                    hint = "Hint: Underground heat boils water into steam, turning turbines to generate electricity.",
                    options = listOf(
                        "Thermal -> Mechanical -> Electrical",
                        "Chemical -> Light -> Sound",
                        "Solar -> Nuclear -> Thermal",
                        "Elastic -> Potential -> Chemical"
                    ),
                    correctAnswer = "Thermal -> Mechanical -> Electrical",
                    explanation = "Accurate! Thermal steam turns turbines (mechanical), driving generators (electrical)."
                ),
                QuizQuestion(
                    id = "g10-st-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Kenyan clean energy source to its primary location.",
                    hint = "Hint: Olkaria is near Naivasha; Seven Forks is on Tana River.",
                    pairs = listOf(
                        MatchingPair("Geothermal Steam", "Olkaria, Naivasha"),
                        MatchingPair("Wind Turbines", "Lake Turkana Wind Power"),
                        MatchingPair("Hydroelectric Dams", "Seven Forks (Tana River)")
                    ),
                    explanation = "Excellent Senior School STEM mastery!"
                ),
                QuizQuestion(
                    id = "g10-st-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "The rotating machine pushed by high-pressure steam inside a power plant is called a ________.",
                    hint = "Hint: Starts with the letter T.",
                    blankPrefix = "Component:",
                    blankSuffix = "",
                    correctAnswer = "Turbine",
                    acceptableAnswers = listOf("turbine", "steam turbine"),
                    explanation = "Correct! High-pressure steam spins the turbine blades."
                )
            )
        ),
        CBCLesson(
            id = "g11-math-finance",
            title = "SACCO Savings & Compound Interest",
            grade = 11,
            levelGroup = "Senior School",
            learningArea = LearningArea.MATHEMATICS,
            strand = "Commercial Arithmetic",
            subStrand = "Compound Interest & Appreciation",
            durationMins = 5,
            summary = "Model Kenyan SACCO savings growth using compound interest formulas.",
            questions = listOf(
                QuizQuestion(
                    id = "g11-m-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "KES 10,000 is saved at 10% p.a. compound interest for 2 years. What is the final amount?",
                    hint = "Hint: Year 1 = 11,000; Year 2 adds 10% of 11,000.",
                    options = listOf("KES 12,100", "KES 12,000", "KES 11,100", "KES 12,500"),
                    correctAnswer = "KES 12,100",
                    explanation = "Correct! 10,000 x (1.1)^2 = KES 12,100."
                ),
                QuizQuestion(
                    id = "g11-m-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each financial term to its Kenyan CBC meaning.",
                    hint = "Hint: Principal is starting cash; Appreciation means rising value.",
                    pairs = listOf(
                        MatchingPair("Principal (P)", "Initial amount invested or borrowed"),
                        MatchingPair("Appreciation", "Increase in asset value over time"),
                        MatchingPair("Depreciation", "Decrease in asset value over time")
                    ),
                    explanation = "Excellent mastery of commercial arithmetic!"
                ),
                QuizQuestion(
                    id = "g11-m-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "How much compound interest alone was earned on KES 10,000 if the final amount is KES 12,100?",
                    hint = "Hint: Interest = Amount - Principal.",
                    blankPrefix = "Interest: KES",
                    blankSuffix = "",
                    correctAnswer = "2100",
                    acceptableAnswers = listOf("2100", "2,100", "kes 2100"),
                    explanation = "Spot on! 12,100 - 10,000 = KES 2,100."
                )
            )
        ),
        CBCLesson(
            id = "g12-sci-genetics",
            title = "Biotechnology & Drought-Tolerant Crops",
            grade = 12,
            levelGroup = "Senior School",
            learningArea = LearningArea.INTEGRATED_SCIENCE,
            strand = "Genetics & Biotechnology",
            subStrand = "Selective Breeding & Food Security",
            durationMins = 5,
            summary = "Examine how KALRO research improves maize and sorghum resilience across Kenyan climates.",
            questions = listOf(
                QuizQuestion(
                    id = "g12-s-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Why do Kenyan researchers breed drought-tolerant maize varieties for semi-arid counties?",
                    hint = "Hint: Think about reliable harvests when rainfall is low.",
                    options = listOf(
                        "To improve food security during erratic rainfall",
                        "To increase soil erosion",
                        "To eliminate pollination",
                        "To stop photosynthesis"
                    ),
                    correctAnswer = "To improve food security during erratic rainfall",
                    explanation = "Accurate! Drought-tolerant traits protect harvests in ASAL regions."
                ),
                QuizQuestion(
                    id = "g12-s-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each genetics concept to its definition.",
                    hint = "Hint: DNA carries genetic code; phenotype is the observable trait.",
                    pairs = listOf(
                        MatchingPair("Genotype", "Genetic makeup of an organism"),
                        MatchingPair("Phenotype", "Observable physical characteristics"),
                        MatchingPair("DNA", "Molecule carrying hereditary instructions")
                    ),
                    explanation = "Well done! Clear understanding of genetics and biotechnology."
                ),
                QuizQuestion(
                    id = "g12-s-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "In Kenya, the national agricultural research body developing resilient seeds is abbreviated ________.",
                    hint = "Hint: Kenya Agricultural and Livestock Research Organization (5 letters).",
                    blankPrefix = "Organization:",
                    blankSuffix = "",
                    correctAnswer = "KALRO",
                    acceptableAnswers = listOf("kalro"),
                    explanation = "Correct! KALRO leads agricultural research in Kenya."
                )
            )
        )
    ) + CbcCurriculumCatalog.extraLessons

    val catalog: List<CbcCatalogEntry> by lazy {
        CbcCurriculumCatalog.buildFullCatalog()
    }

    data class SubjectStrandOption(
        val strand: String,
        val subStrand: String,
        val levelHint: String
    )

    fun subjectsForGrade(grade: Int): List<LearningArea> =
        CbcCurriculumCatalog.subjectsForGrade(grade)

    fun groupedStrandsForArea(area: LearningArea, gradeFilter: Int? = null): List<CbcStrandNode> =
        CbcCurriculumCatalog.strandsForSubject(area, gradeFilter)

    fun strandsForArea(area: LearningArea): List<SubjectStrandOption> =
        CbcCurriculumCatalog.strandsForSubject(area).flatMap { st ->
            st.subStrands.map { sub ->
                SubjectStrandOption(
                    strand = st.strand,
                    subStrand = sub.subStrand,
                    levelHint = sub.levelHint
                )
            }
        }

    fun curriculumStats(): Triple<Int, Int, Int> {
        val subjects = CbcCurriculumCatalog.curriculumTree.size
        val strands = CbcCurriculumCatalog.curriculumTree.sumOf { it.strands.size }
        val subStrands = CbcCurriculumCatalog.curriculumTree.sumOf { subj ->
            subj.strands.sumOf { it.subStrands.size }
        }
        return Triple(subjects, strands, subStrands)
    }

    val classStreams = listOf(
        "Grade 6 East - Nairobi Primary",
        "Grade 4 West - Nairobi Primary",
        "Grade 8 North - Kilimani Junior"
    )

    fun generatePairingCode(): String {
        val num = (1000..9999).random()
        return "SOMA-$num"
    }

    fun generateTeacherCode(): String {
        val num = (1000..9999).random()
        return "TCH-$num"
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
