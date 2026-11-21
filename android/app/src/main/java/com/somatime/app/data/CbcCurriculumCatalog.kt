package com.somatime.app.data

import com.somatime.app.model.CBCLesson
import com.somatime.app.model.CbcCatalogEntry
import com.somatime.app.model.CbcStrandNode
import com.somatime.app.model.CbcSubStrandNode
import com.somatime.app.model.CbcSubjectCurriculum
import com.somatime.app.model.LearningArea
import com.somatime.app.model.MatchingPair
import com.somatime.app.model.QuestionType
import com.somatime.app.model.QuizQuestion

object CbcCurriculumCatalog {

    private fun levelGroupForGrade(grade: Int): String = when {
        grade <= 3 -> "Lower Primary"
        grade <= 6 -> "Upper Primary"
        grade <= 9 -> "Junior School"
        else -> "Senior School"
    }

    val curriculumTree: List<CbcSubjectCurriculum> = listOf(
        CbcSubjectCurriculum(
            learningArea = LearningArea.MATHEMATICS,
            minGrade = 1,
            maxGrade = 12,
            strands = listOf(
                CbcStrandNode(
                    strand = "Numbers",
                    subStrands = listOf(
                        CbcSubStrandNode("Pre-Number Activities & Whole Numbers (1–100)", 1, 2, null, "g1-math-coins"),
                        CbcSubStrandNode("Addition, Subtraction & Whole Numbers", 1, 3, null, "g1-math-coins"),
                        CbcSubStrandNode("Multiplication, Division & Place Value", 3, 5, null, "g4-math-fractions"),
                        CbcSubStrandNode("Fractions in Daily Life", 4, 6, null, "g4-math-fractions"),
                        CbcSubStrandNode("Decimals in Shopping & Measurement", 5, 6, null, "g5-math-decimals"),
                        CbcSubStrandNode("Percentages, Ratios & Proportions", 6, 7, null, "g5-math-decimals"),
                        CbcSubStrandNode("Integers, Squares & Indices", 7, 9, null, "g8-math-algebra"),
                        CbcSubStrandNode("Logarithms, Matrices & Vectors", 10, 12, "STEM & Social Sciences", "g11-math-finance")
                    )
                ),
                CbcStrandNode(
                    strand = "Measurement",
                    subStrands = listOf(
                        CbcSubStrandNode("Kenyan Shilling Coins & Notes", 1, 3, null, "g1-math-coins"),
                        CbcSubStrandNode("Length, Mass, Capacity & Clock Time", 2, 5, null, "g3-math-measurement"),
                        CbcSubStrandNode("Perimeter, Area & Volume of Solids", 5, 8, null, "g3-math-measurement"),
                        CbcSubStrandNode("Speed, Distance, Time & Density", 6, 9, null, "g8-math-algebra"),
                        CbcSubStrandNode("Trigonometry & Bearings", 10, 12, "STEM Pathway", "g11-math-finance")
                    )
                ),
                CbcStrandNode(
                    strand = "Geometry",
                    subStrands = listOf(
                        CbcSubStrandNode("2D Shapes, Patterns & Straight Lines", 1, 4, null, "g3-math-measurement"),
                        CbcSubStrandNode("Angles, Triangles & Symmetry", 4, 7, null, "g4-math-fractions"),
                        CbcSubStrandNode("Geometrical Constructions & Scale Drawing", 7, 9, null, "g8-math-algebra"),
                        CbcSubStrandNode("Circle Theorems & 3D Geometry", 10, 12, "STEM Pathway", "g11-math-finance")
                    )
                ),
                CbcStrandNode(
                    strand = "Algebra",
                    subStrands = listOf(
                        CbcSubStrandNode("Number Patterns & Simple Unknowns", 4, 6, null, "g5-math-decimals"),
                        CbcSubStrandNode("Algebraic Expressions & Linear Equations", 7, 9, null, "g8-math-algebra"),
                        CbcSubStrandNode("Linear Inequalities & Simultaneous Equations", 8, 10, null, "g8-math-algebra"),
                        CbcSubStrandNode("Quadratic Equations, Sequences & Calculus Intro", 10, 12, "STEM Pathway", "g11-math-finance")
                    )
                ),
                CbcStrandNode(
                    strand = "Data Handling & Probability",
                    subStrands = listOf(
                        CbcSubStrandNode("Tally Marks, Pictographs & Bar Graphs", 4, 6, null, "g5-math-decimals"),
                        CbcSubStrandNode("Mean, Median, Mode & Pie Charts", 7, 9, null, "g8-math-algebra"),
                        CbcSubStrandNode("Grouped Statistics, Permutations & Probability", 10, 12, "STEM & Social Sciences", "g11-math-finance")
                    )
                ),
                CbcStrandNode(
                    strand = "Commercial Arithmetic",
                    subStrands = listOf(
                        CbcSubStrandNode("Profit, Loss, Discounts & Commission", 6, 9, null, "g5-math-decimals"),
                        CbcSubStrandNode("Simple Interest & Hire Purchase", 8, 10, null, "g11-math-finance"),
                        CbcSubStrandNode("Compound Interest, Appreciation & Taxation", 10, 12, "STEM & Social Sciences", "g11-math-finance")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.ENGLISH,
            minGrade = 1,
            maxGrade = 12,
            strands = listOf(
                CbcStrandNode(
                    strand = "Listening & Speaking",
                    subStrands = listOf(
                        CbcSubStrandNode("Polite Expressions, Greetings & Phonics", 1, 3, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Pronunciation, Stress & Oral Narratives", 4, 6, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Debates, Speeches & Conversational Etiquette", 7, 9, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Public Speaking, Interviews & Discourse", 10, 12, null, "g6-eng-grammar")
                    )
                ),
                CbcStrandNode(
                    strand = "Reading",
                    subStrands = listOf(
                        CbcSubStrandNode("Sight Words, Short Stories & Picture Books", 1, 3, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Kenyan Folktales, Fluency & Vocabulary", 4, 6, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Intensive Comprehension, Poetry & Novellas", 7, 9, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Critical Literary Analysis & Set Books", 10, 12, null, "g6-eng-grammar")
                    )
                ),
                CbcStrandNode(
                    strand = "Grammar in Use",
                    subStrands = listOf(
                        CbcSubStrandNode("Nouns, Pronouns, Plurals & Articles", 1, 3, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Tenses, Prepositions & Conjunctions", 4, 6, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Active/Passive Voice & Direct/Indirect Speech", 7, 9, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Complex Clauses, Modifiers & Pragmatics", 10, 12, null, "g6-eng-grammar")
                    )
                ),
                CbcStrandNode(
                    strand = "Writing",
                    subStrands = listOf(
                        CbcSubStrandNode("Handwriting, Punctuation & Guided Sentences", 1, 3, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Paragraphs, Friendly Letters & Creative Stories", 4, 6, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Formal Letters, Diaries & Book Reviews", 7, 9, null, "g6-eng-grammar"),
                        CbcSubStrandNode("Functional Reports, Essays & Memos", 10, 12, null, "g6-eng-grammar")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.KISWAHILI,
            minGrade = 1,
            maxGrade = 12,
            strands = listOf(
                CbcStrandNode(
                    strand = "Kusikiliza na Kuzungumza",
                    subStrands = listOf(
                        CbcSubStrandNode("Maamkuzi, Adabu na Msamiati wa Nyumbani", 1, 3, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Matamshi Bora, Hadithi na Majadiliano", 4, 6, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Mazungumzo Rasmi, Mijadala na Mashairi", 7, 9, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Ufahamu wa Kusikiliza na Fasihi Simulizi", 10, 12, null, "g2-kisw-msamiati")
                    )
                ),
                CbcStrandNode(
                    strand = "Kusoma",
                    subStrands = listOf(
                        CbcSubStrandNode("Kusoma Silabi, Maneno na Sentensi Fupi", 1, 3, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Kusoma kwa Ufahamu na Methali za Kiswahili", 4, 6, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Uchambuzi wa Riwaya, Tamthilia na Ushairi", 7, 12, null, "g2-kisw-msamiati")
                    )
                ),
                CbcStrandNode(
                    strand = "Sarufi na Matumizi ya Lugha",
                    subStrands = listOf(
                        CbcSubStrandNode("Nomino, Vitenzi na Umoja/Wingi", 1, 4, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Ngeli za Nomino (A-WA, KI-VI, M-MI) na Nyakati", 5, 9, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Mnyambuliko wa Vitenzi, Vihusishi na Sintaksia", 8, 12, null, "g2-kisw-msamiati")
                    )
                ),
                CbcStrandNode(
                    strand = "Kuandika (Insha)",
                    subStrands = listOf(
                        CbcSubStrandNode("Imla, Tahajia na Insha za Picha", 1, 4, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Barua ya Kirafiki na Insha za Masimulizi", 5, 7, null, "g2-kisw-msamiati"),
                        CbcSubStrandNode("Barua Rasmi, Ripoti na Kumbukumbu", 8, 12, null, "g2-kisw-msamiati")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.ENVIRONMENTAL_ACTIVITIES,
            minGrade = 1,
            maxGrade = 3,
            strands = listOf(
                CbcStrandNode(
                    strand = "Social Environment",
                    subStrands = listOf(
                        CbcSubStrandNode("My Home, Family & School Community", 1, 2, null, "g2-env-weather"),
                        CbcSubStrandNode("Community Leaders, Safe Roads & Child Rights", 2, 3, null, "g2-env-weather"),
                        CbcSubStrandNode("Cultural Events & Harmony in Kenya", 1, 3, null, "g2-env-weather")
                    )
                ),
                CbcStrandNode(
                    strand = "Natural Environment",
                    subStrands = listOf(
                        CbcSubStrandNode("Weather Patterns, Clouds & Seasonal Clothing", 1, 3, null, "g2-env-weather"),
                        CbcSubStrandNode("Soil Types, Domestic Animals & Plants", 1, 3, null, "g2-env-weather"),
                        CbcSubStrandNode("Light, Shadows & Sources of Heat", 2, 3, null, "g2-env-weather")
                    )
                ),
                CbcStrandNode(
                    strand = "Resources in Our Environment",
                    subStrands = listOf(
                        CbcSubStrandNode("Safe Drinking Water & Water Conservation", 1, 3, null, "g2-env-weather"),
                        CbcSubStrandNode("Caring for Trees & Waste Disposal", 1, 3, null, "g2-env-weather"),
                        CbcSubStrandNode("Saving Energy at Home and School", 2, 3, null, "g2-env-weather")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.SCIENCE_TECH,
            minGrade = 4,
            maxGrade = 6,
            strands = listOf(
                CbcStrandNode(
                    strand = "Living Things & Their Environment",
                    subStrands = listOf(
                        CbcSubStrandNode("Parts of Plants, Germination & Crops", 4, 5, null, "g4-scitech-matter"),
                        CbcSubStrandNode("Vertebrates, Invertebrates & Adaptations", 4, 6, null, "g4-scitech-matter"),
                        CbcSubStrandNode("Human Breathing, Digestive & Circulatory Systems", 4, 6, null, "g4-scitech-matter")
                    )
                ),
                CbcStrandNode(
                    strand = "Matter & Mixtures",
                    subStrands = listOf(
                        CbcSubStrandNode("Solids, Liquids & Gases (Changes of State)", 4, 5, null, "g4-scitech-matter"),
                        CbcSubStrandNode("Separating Mixtures (Filtration, Sieving, Magnets)", 4, 6, null, "g4-scitech-matter"),
                        CbcSubStrandNode("Properties of Air, Oxygen & Rusting", 5, 6, null, "g4-scitech-matter")
                    )
                ),
                CbcStrandNode(
                    strand = "Force, Energy & Simple Machines",
                    subStrands = listOf(
                        CbcSubStrandNode("Light Reflection, Shadows & Sound Waves", 4, 6, null, "g4-scitech-matter"),
                        CbcSubStrandNode("Heat Transfer (Conduction, Convection, Radiation)", 5, 6, null, "g4-scitech-matter"),
                        CbcSubStrandNode("Levers, Pulleys & Inclined Planes", 5, 6, null, "g4-scitech-matter")
                    )
                ),
                CbcStrandNode(
                    strand = "Digital Literacy & Computing",
                    subStrands = listOf(
                        CbcSubStrandNode("Digital Devices, Word Processing & Presentations", 4, 5, null, "g8-comp-algorithms"),
                        CbcSubStrandNode("Visual Coding Blocks & Safe Internet Use", 4, 6, null, "g8-comp-algorithms")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.INTEGRATED_SCIENCE,
            minGrade = 7,
            maxGrade = 12,
            strands = listOf(
                CbcStrandNode(
                    strand = "Scientific Investigation",
                    subStrands = listOf(
                        CbcSubStrandNode("Laboratory Apparatus, Hazard Symbols & Safety", 7, 8, null, "g7-sci-apparatus"),
                        CbcSubStrandNode("SI Units, Measurement & Experimental Design", 7, 9, null, "g7-sci-apparatus"),
                        CbcSubStrandNode("Mixtures, Solutions & Chromatography", 7, 8, null, "g4-scitech-matter")
                    )
                ),
                CbcStrandNode(
                    strand = "Mixtures, Elements & Compounds",
                    subStrands = listOf(
                        CbcSubStrandNode("Acids, Bases, Indicators & Neutralization", 7, 9, null, "g10-chem-acids"),
                        CbcSubStrandNode("Periodic Table, Elements & Chemical Equations", 8, 10, null, "g10-chem-acids"),
                        CbcSubStrandNode("Water Hardness, Rust Prevention & Extraction", 8, 11, null, "g10-chem-acids")
                    )
                ),
                CbcStrandNode(
                    strand = "Living Things & Environment",
                    subStrands = listOf(
                        CbcSubStrandNode("Human Reproductive, Excretory & Nervous Systems", 7, 9, null, "g9-sci-ecosystems"),
                        CbcSubStrandNode("Cell Structure, Diffusion & Osmosis", 8, 10, null, "g10-bio-cells"),
                        CbcSubStrandNode("Food Webs, Biodiversity & Savanna Conservation", 8, 10, null, "g9-sci-ecosystems"),
                        CbcSubStrandNode("Genetics, Biotechnology & Food Security", 10, 12, "STEM Pathway", "g12-sci-genetics")
                    )
                ),
                CbcStrandNode(
                    strand = "Force, Energy & Sustainability",
                    subStrands = listOf(
                        CbcSubStrandNode("Static & Current Electricity, Magnets & Circuits", 7, 9, null, "g10-phy-electricity"),
                        CbcSubStrandNode("Pressure, Curved Mirrors & Electromagnetic Waves", 8, 11, null, "g10-phy-electricity"),
                        CbcSubStrandNode("Renewable Power Systems (Olkaria Geothermal)", 10, 12, "STEM Pathway", "g10-stem-energy")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.SOCIAL_STUDIES,
            minGrade = 4,
            maxGrade = 9,
            strands = listOf(
                CbcStrandNode(
                    strand = "Natural & Historic Built Environments",
                    subStrands = listOf(
                        CbcSubStrandNode("Compass Directions, Maps & Kenya Counties", 4, 5, null, "g6-sst-resources"),
                        CbcSubStrandNode("East African Rift Valley, Mountains & Lakes", 5, 8, null, "g6-sst-resources"),
                        CbcSubStrandNode("Climatic Regions & Historic Sites (Fort Jesus, Gedi)", 6, 9, null, "g6-sst-resources")
                    )
                ),
                CbcStrandNode(
                    strand = "People, Population & Social Organization",
                    subStrands = listOf(
                        CbcSubStrandNode("Kenyan Language Groups, Migration & Heritage", 4, 6, null, "g6-sst-resources"),
                        CbcSubStrandNode("Population Distribution & Urbanization in Kenya", 6, 9, null, "g6-sst-resources"),
                        CbcSubStrandNode("African Early Civilizations & Peacebuilding", 7, 9, null, "g10-hist-constitution")
                    )
                ),
                CbcStrandNode(
                    strand = "Resources & Economic Activities",
                    subStrands = listOf(
                        CbcSubStrandNode("Cash Crops, Livestock, Fisheries & Tourism", 4, 7, null, "g6-sst-resources"),
                        CbcSubStrandNode("Mombasa Port, SGR & East African Trade Corridors", 5, 8, null, "g6-sst-resources"),
                        CbcSubStrandNode("Mining, Wildlife Conservation & Blue Economy", 6, 9, null, "g6-sst-resources")
                    )
                ),
                CbcStrandNode(
                    strand = "Citizenship & Governance",
                    subStrands = listOf(
                        CbcSubStrandNode("National Symbols, Unity & Democratic Values", 4, 6, null, "g10-hist-constitution"),
                        CbcSubStrandNode("The Constitution of Kenya, Devolution & Human Rights", 6, 9, null, "g10-hist-constitution"),
                        CbcSubStrandNode("Regional Cooperation (EAC, AU & Global Citizenship)", 7, 9, null, "g10-hist-constitution")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.RELIGIOUS_EDUCATION,
            minGrade = 1,
            maxGrade = 12,
            strands = listOf(
                CbcStrandNode(
                    strand = "Creation & Sacred Scriptures",
                    subStrands = listOf(
                        CbcSubStrandNode("Appreciating God's Creation & Caring for Life", 1, 3, null, "g5-cre-values"),
                        CbcSubStrandNode("Sacred Texts, Prophets & Moral Teachings", 4, 6, null, "g5-cre-values"),
                        CbcSubStrandNode("Faith Leaders, Covenant & Stewardship Responsibility", 7, 9, null, "g5-cre-values"),
                        CbcSubStrandNode("Scriptural Hermeneutics & Contemporary Ethics", 10, 12, "Social Sciences", "g5-cre-values")
                    )
                ),
                CbcStrandNode(
                    strand = "Moral Living, Integrity & Values",
                    subStrands = listOf(
                        CbcSubStrandNode("Obedience, Sharing & Gratitude at Home", 1, 3, null, "g5-cre-values"),
                        CbcSubStrandNode("Honesty, Kindness, Forgiveness & Peace", 4, 7, null, "g5-cre-values"),
                        CbcSubStrandNode("Peer Pressure Resistance, Leisure & Integrity", 7, 9, null, "g5-cre-values"),
                        CbcSubStrandNode("Social Justice, Work Ethics & Civic Virtue", 10, 12, "Social Sciences", "g5-cre-values")
                    )
                ),
                CbcStrandNode(
                    strand = "Worship, Community & Service",
                    subStrands = listOf(
                        CbcSubStrandNode("Prayer, Places of Worship & Religious Festivals", 1, 5, null, "g5-cre-values"),
                        CbcSubStrandNode("Community Service, Interfaith Respect & Charity", 6, 12, null, "g5-cre-values")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.AGRICULTURE_NUTRITION,
            minGrade = 4,
            maxGrade = 9,
            strands = listOf(
                CbcStrandNode(
                    strand = "Conservation of Resources",
                    subStrands = listOf(
                        CbcSubStrandNode("Soil Erosion Control, Mulching & Cover Crops", 4, 6, null, "g5-agri-kitchen-garden"),
                        CbcSubStrandNode("Organic Compost Manure & Water Harvesting", 4, 8, null, "g5-agri-kitchen-garden"),
                        CbcSubStrandNode("Agroforestry & Wildlife Coexistence", 6, 9, null, "g5-agri-kitchen-garden")
                    )
                ),
                CbcStrandNode(
                    strand = "Food Production Processes",
                    subStrands = listOf(
                        CbcSubStrandNode("Urban Kitchen & Vertical Sack Gardens", 4, 7, null, "g5-agri-kitchen-garden"),
                        CbcSubStrandNode("Drip Irrigation, Nursery Beds & Crop Pests", 5, 9, null, "g5-agri-kitchen-garden"),
                        CbcSubStrandNode("Small Domestic Animals & Poultry Care", 6, 9, null, "g5-agri-kitchen-garden")
                    )
                ),
                CbcStrandNode(
                    strand = "Hygiene & Nutrition",
                    subStrands = listOf(
                        CbcSubStrandNode("Balanced Kenyan Meals & Food Groups", 4, 7, null, "g5-agri-kitchen-garden"),
                        CbcSubStrandNode("Food Preservation, Kitchen Hygiene & Safety", 5, 9, null, "g5-agri-kitchen-garden"),
                        CbcSubStrandNode("Clothing Repair, Laundry & Stitches", 4, 8, null, "g5-agri-kitchen-garden")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.CREATIVE_ARTS,
            minGrade = 1,
            maxGrade = 12,
            strands = listOf(
                CbcStrandNode(
                    strand = "Picture Making & Craft",
                    subStrands = listOf(
                        CbcSubStrandNode("Drawing, Shading, Collage & Kenyan Beadwork", 1, 5, null, "g5-arts-music"),
                        CbcSubStrandNode("Weaving, Pottery, Montage & Graphic Design", 4, 8, null, "g5-arts-music"),
                        CbcSubStrandNode("Sculpture, Printmaking & Visual Portfolio", 8, 12, "Arts & Sports Science", "g5-arts-music")
                    )
                ),
                CbcStrandNode(
                    strand = "Performing Arts & Music",
                    subStrands = listOf(
                        CbcSubStrandNode("Action Songs, Clapping Rhythms & Percussion", 1, 3, null, "g5-arts-music"),
                        CbcSubStrandNode("Kenyan Folk Songs & Traditional Instruments (Nyatiti, Chivoti)", 4, 8, null, "g5-arts-music"),
                        CbcSubStrandNode("Music Notation, Descant Recorder & Drama", 6, 12, "Arts & Sports Science", "g5-arts-music")
                    )
                ),
                CbcStrandNode(
                    strand = "Sports & Physical Fitness",
                    subStrands = listOf(
                        CbcSubStrandNode("Locomotor Skills, Skipping & Tag Games", 1, 3, null, "g5-arts-music"),
                        CbcSubStrandNode("Athletics Relays, Football, Netball & Swimming Safety", 4, 9, null, "g5-arts-music"),
                        CbcSubStrandNode("Sports Biomechanics, First Aid & Officiating", 9, 12, "Arts & Sports Science", "g5-arts-music")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.PRE_TECHNICAL_STUDIES,
            minGrade = 7,
            maxGrade = 9,
            strands = listOf(
                CbcStrandNode(
                    strand = "Foundations of Pre-Technical Studies",
                    subStrands = listOf(
                        CbcSubStrandNode("Workshop Safety, Fire Safety & PPE Gear", 7, 9, null, "g7-pretech-safety"),
                        CbcSubStrandNode("Digital Citizenship & Online Safety in Workshops", 7, 9, null, "g7-pretech-safety")
                    )
                ),
                CbcStrandNode(
                    strand = "Communication & Technical Drawing",
                    subStrands = listOf(
                        CbcSubStrandNode("Drawing Instruments, Line Types & Dimensioning", 7, 9, null, "g7-pretech-safety"),
                        CbcSubStrandNode("Plane Geometry, Orthographic & Oblique Projection", 8, 9, null, "g7-pretech-safety")
                    )
                ),
                CbcStrandNode(
                    strand = "Materials, Tools & Production",
                    subStrands = listOf(
                        CbcSubStrandNode("Timber, Metals, Plastics & Composite Materials", 7, 9, null, "g7-pretech-safety"),
                        CbcSubStrandNode("Measuring, Marking Out, Cutting & Joining Tools", 7, 9, null, "g7-pretech-safety"),
                        CbcSubStrandNode("Basic Electrical Circuits & Household Maintenance", 8, 9, null, "g10-phy-electricity")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.BUSINESS_STUDIES,
            minGrade = 7,
            maxGrade = 12,
            strands = listOf(
                CbcStrandNode(
                    strand = "Business & Money Management",
                    subStrands = listOf(
                        CbcSubStrandNode("Human Wants, Scarcity & Kenyan Currency Functions", 7, 8, null, "g8-biz-enterprise"),
                        CbcSubStrandNode("Personal Budgeting, Mobile Money & Saving Goals", 7, 10, null, "g8-biz-enterprise"),
                        CbcSubStrandNode("Banking, SACCOs, Insurance & Microfinance", 9, 12, "Social Sciences", "g8-biz-enterprise")
                    )
                ),
                CbcStrandNode(
                    strand = "Entrepreneurship & Production",
                    subStrands = listOf(
                        CbcSubStrandNode("Entrepreneurial Qualities & Identifying Opportunities", 7, 9, null, "g8-biz-enterprise"),
                        CbcSubStrandNode("Business Plans, Marketing & Consumer Protection", 8, 12, "Social Sciences", "g8-biz-enterprise")
                    )
                ),
                CbcStrandNode(
                    strand = "Financial Accounting & Trade",
                    subStrands = listOf(
                        CbcSubStrandNode("Bookkeeping, Cash Books, Profit & Loss Accounts", 8, 12, "Social Sciences", "g8-biz-enterprise"),
                        CbcSubStrandNode("Balance Sheets, Taxation & International Trade", 10, 12, "Social Sciences", "g11-math-finance")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.COMPUTER_STUDIES,
            minGrade = 7,
            maxGrade = 12,
            strands = listOf(
                CbcStrandNode(
                    strand = "Computer Systems & Hardware",
                    subStrands = listOf(
                        CbcSubStrandNode("Input, Processing, Storage & Output Components", 7, 9, null, "g8-comp-algorithms"),
                        CbcSubStrandNode("Operating Systems, File Management & Troubleshooting", 7, 11, null, "g8-comp-algorithms")
                    )
                ),
                CbcStrandNode(
                    strand = "Data Communication & Cyber Safety",
                    subStrands = listOf(
                        CbcSubStrandNode("Computer Networks, Internet Protocols & Cloud Storage", 8, 12, "STEM Pathway", "g8-comp-algorithms"),
                        CbcSubStrandNode("Cyber Hygiene, Encryption & Digital Ethics", 7, 12, null, "g8-comp-algorithms")
                    )
                ),
                CbcStrandNode(
                    strand = "Coding & Problem Solving",
                    subStrands = listOf(
                        CbcSubStrandNode("Algorithms, Flowcharts & Pseudocode Design", 7, 10, null, "g8-comp-algorithms"),
                        CbcSubStrandNode("Programming Constructs, Databases & Web Apps", 9, 12, "STEM Pathway", "g8-comp-algorithms")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.BIOLOGY,
            minGrade = 10,
            maxGrade = 12,
            pathway = "STEM Pathway",
            strands = listOf(
                CbcStrandNode(
                    strand = "Cell Biology & Physiology",
                    subStrands = listOf(
                        CbcSubStrandNode("Microscopy, Cell Ultrastructure & Organelles", 10, 11, "STEM Pathway", "g10-bio-cells"),
                        CbcSubStrandNode("Diffusion, Osmosis & Active Transport", 10, 11, "STEM Pathway", "g10-bio-cells"),
                        CbcSubStrandNode("Photosynthesis, Respiration & Enzyme Action", 10, 12, "STEM Pathway", "g10-bio-cells")
                    )
                ),
                CbcStrandNode(
                    strand = "Transport, Gaseous Exchange & Homeostasis",
                    subStrands = listOf(
                        CbcSubStrandNode("Xylem, Phloem & Transpiration in Kenyan Crops", 10, 11, "STEM Pathway", "g10-bio-cells"),
                        CbcSubStrandNode("Mammalian Heart, Immunity & Kidney Regulation", 11, 12, "STEM Pathway", "g10-bio-cells")
                    )
                ),
                CbcStrandNode(
                    strand = "Ecology, Genetics & Evolution",
                    subStrands = listOf(
                        CbcSubStrandNode("Population Ecology, Savanna Biomes & Conservation", 10, 12, "STEM Pathway", "g9-sci-ecosystems"),
                        CbcSubStrandNode("Mendelian Inheritance, DNA & Biotechnology", 11, 12, "STEM Pathway", "g12-sci-genetics")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.CHEMISTRY,
            minGrade = 10,
            maxGrade = 12,
            pathway = "STEM Pathway",
            strands = listOf(
                CbcStrandNode(
                    strand = "Inorganic Chemistry & Atomic Structure",
                    subStrands = listOf(
                        CbcSubStrandNode("Atomic Structure, Isotopes & Chemical Bonding", 10, 11, "STEM Pathway", "g10-chem-acids"),
                        CbcSubStrandNode("Periodic Trends (Alkali Metals, Halogens, Noble Gases)", 10, 12, "STEM Pathway", "g10-chem-acids")
                    )
                ),
                CbcStrandNode(
                    strand = "Acids, Bases, Salts & Stoichiometry",
                    subStrands = listOf(
                        CbcSubStrandNode("pH Scale, Neutralization & Soil Acidity Management", 10, 11, "STEM Pathway", "g10-chem-acids"),
                        CbcSubStrandNode("The Mole Concept, Molarity & Volumetric Analysis", 10, 12, "STEM Pathway", "g10-chem-acids")
                    )
                ),
                CbcStrandNode(
                    strand = "Organic Chemistry & Electrochemistry",
                    subStrands = listOf(
                        CbcSubStrandNode("Alkanes, Alkenes, Alcohols & Polymers", 11, 12, "STEM Pathway", "g10-chem-acids"),
                        CbcSubStrandNode("Redox Reactions, Electrolysis & Industrial Processes", 11, 12, "STEM Pathway", "g10-chem-acids")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.PHYSICS,
            minGrade = 10,
            maxGrade = 12,
            pathway = "STEM Pathway",
            strands = listOf(
                CbcStrandNode(
                    strand = "Mechanics & Properties of Matter",
                    subStrands = listOf(
                        CbcSubStrandNode("Linear Motion, Newton's Laws & Momentum", 10, 11, "STEM Pathway", "g10-phy-electricity"),
                        CbcSubStrandNode("Work, Energy, Power & Fluid Pressure", 10, 12, "STEM Pathway", "g10-stem-energy")
                    )
                ),
                CbcStrandNode(
                    strand = "Waves, Optics & Thermal Physics",
                    subStrands = listOf(
                        CbcSubStrandNode("Refraction, Lenses & Optical Instruments", 10, 12, "STEM Pathway", "g10-phy-electricity"),
                        CbcSubStrandNode("Specific Heat Capacity & Geothermal Thermodynamics", 10, 12, "STEM Pathway", "g10-stem-energy")
                    )
                ),
                CbcStrandNode(
                    strand = "Electricity, Magnetism & Modern Physics",
                    subStrands = listOf(
                        CbcSubStrandNode("Current, Voltage, Ohm's Law & Parallel Circuits", 10, 11, "STEM Pathway", "g10-phy-electricity"),
                        CbcSubStrandNode("Electromagnetic Induction, Transformers & Electronics", 11, 12, "STEM Pathway", "g10-phy-electricity")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.HISTORY_CITIZENSHIP,
            minGrade = 10,
            maxGrade = 12,
            pathway = "Social Sciences",
            strands = listOf(
                CbcStrandNode(
                    strand = "Kenyan History & Pan-Africanism",
                    subStrands = listOf(
                        CbcSubStrandNode("Pre-Colonial Communities, Coastal Trade & Struggle for Independence", 10, 11, "Social Sciences", "g10-hist-constitution"),
                        CbcSubStrandNode("Post-Independence Nation Building & East African Integration", 11, 12, "Social Sciences", "g10-hist-constitution")
                    )
                ),
                CbcStrandNode(
                    strand = "Constitution, Devolution & Human Rights",
                    subStrands = listOf(
                        CbcSubStrandNode("2010 Constitution, 47 Counties & Separation of Powers", 10, 12, "Social Sciences", "g10-hist-constitution"),
                        CbcSubStrandNode("Bill of Rights, Public Participation & Global Diplomacy", 10, 12, "Social Sciences", "g10-hist-constitution")
                    )
                )
            )
        ),
        CbcSubjectCurriculum(
            learningArea = LearningArea.GEOGRAPHY,
            minGrade = 10,
            maxGrade = 12,
            pathway = "Social Sciences",
            strands = listOf(
                CbcStrandNode(
                    strand = "Physical Geography & Earth Processes",
                    subStrands = listOf(
                        CbcSubStrandNode("Faulting, Volcanicity & Rift Valley Formation", 10, 11, "Social Sciences", "g10-geo-rift"),
                        CbcSubStrandNode("Weathering, River Drainage & Relief Rainfall", 10, 12, "Social Sciences", "g10-geo-rift")
                    )
                ),
                CbcStrandNode(
                    strand = "Human, Economic & Environmental Geography",
                    subStrands = listOf(
                        CbcSubStrandNode("Statistical Mapwork, GIS & Remote Sensing", 10, 12, "Social Sciences", "g10-geo-rift"),
                        CbcSubStrandNode("Sustainable Agriculture, Energy & Urban Planning", 11, 12, "Social Sciences", "g6-sst-resources")
                    )
                )
            )
        )
    )

    val extraLessons: List<CBCLesson> = listOf(
        CBCLesson(
            id = "g2-env-weather",
            title = "Weather, Soil & Caring for Plants",
            grade = 2,
            levelGroup = "Lower Primary",
            learningArea = LearningArea.ENVIRONMENTAL_ACTIVITIES,
            strand = "Natural Environment",
            subStrand = "Weather Patterns & Plant Care",
            durationMins = 5,
            summary = "Observe daily weather conditions in Kenya, safe water, and caring for seedlings.",
            questions = listOf(
                QuizQuestion(
                    id = "g2-env-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "During the rainy season (Masika) in Kenya, which clothing keeps a Grade 2 learner dry walking to school?",
                    hint = "Hint: Wear something waterproof over your school uniform.",
                    options = listOf("Raincoat and gumboots", "Light vest only", "Sunglasses", "Woolen scarf only"),
                    correctAnswer = "Raincoat and gumboots",
                    explanation = "Well done! A raincoat and gumboots protect you from rain and muddy paths."
                ),
                QuizQuestion(
                    id = "g2-env-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each environmental care action to why it helps our community.",
                    hint = "Hint: Trees give shade; clean bins prevent litter.",
                    pairs = listOf(
                        MatchingPair("Watering tree seedlings", "Helps young trees grow strong"),
                        MatchingPair("Throwing litter in a bin", "Keeps the school compound clean"),
                        MatchingPair("Closing dripping taps", "Saves clean drinking water")
                    ),
                    explanation = "Awesome! Small daily actions protect our Kenyan environment."
                ),
                QuizQuestion(
                    id = "g2-env-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Boiling or filtering river water before drinking makes it ________ for our health.",
                    hint = "Hint: 4-letter word meaning free from germs.",
                    blankPrefix = "Water becomes",
                    blankSuffix = "to drink",
                    correctAnswer = "safe",
                    acceptableAnswers = listOf("safe", "clean", "pure"),
                    explanation = "Correct! Always treat or boil water so it is safe to drink."
                )
            )
        ),
        CBCLesson(
            id = "g4-scitech-matter",
            title = "States of Matter & Separating Mixtures",
            grade = 4,
            levelGroup = "Upper Primary",
            learningArea = LearningArea.SCIENCE_TECH,
            strand = "Matter & Mixtures",
            subStrand = "Solids, Liquids & Gases",
            durationMins = 5,
            summary = "Classify everyday materials into solids, liquids, and gases, and separate mixtures.",
            questions = listOf(
                QuizQuestion(
                    id = "g4-st-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "How can a cook best separate clean water from coarse river sand mixed in a bucket?",
                    hint = "Hint: Pouring the mixture through a clean cloth traps sand particles.",
                    options = listOf("Filtration", "Magnetism", "Winnowing", "Handpicking"),
                    correctAnswer = "Filtration",
                    explanation = "Correct! Filtration traps insoluble sand while letting clear water pass through."
                ),
                QuizQuestion(
                    id = "g4-st-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Kenyan kitchen item to its state of matter.",
                    hint = "Hint: Stones hold shape; milk flows; steam spreads in air.",
                    pairs = listOf(
                        MatchingPair("Maize cob", "Solid (fixed shape)"),
                        MatchingPair("Fresh milk", "Liquid (takes container shape)"),
                        MatchingPair("Kettle steam", "Gas (fills available space)")
                    ),
                    explanation = "Great classification of solids, liquids, and gases!"
                ),
                QuizQuestion(
                    id = "g4-st-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Using a magnet to pull iron nails out of dry sawdust works because iron is a ________ material.",
                    hint = "Hint: Starts with M.",
                    blankPrefix = "Iron is",
                    blankSuffix = "",
                    correctAnswer = "magnetic",
                    acceptableAnswers = listOf("magnetic", "metal"),
                    explanation = "Well done! Magnets attract magnetic metals such as iron and steel."
                )
            )
        ),
        CBCLesson(
            id = "g5-agri-kitchen-garden",
            title = "Urban Kitchen Gardens & Composting",
            grade = 5,
            levelGroup = "Upper Primary",
            learningArea = LearningArea.AGRICULTURE_NUTRITION,
            strand = "Conservation of Resources",
            subStrand = "Organic Compost & Sack Gardens",
            durationMins = 5,
            summary = "Design water-smart vertical sack gardens and turn kitchen peelings into organic compost.",
            questions = listOf(
                QuizQuestion(
                    id = "g5-ag-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Why are vertical sack gardens popular for growing kale and spinach in urban Kenyan homes?",
                    hint = "Hint: Sacks stack soil upwards and keep moisture close to plant roots.",
                    options = listOf(
                        "They save space and conserve water",
                        "They require heavy tractors",
                        "They block all sunlight",
                        "They only grow maize"
                    ),
                    correctAnswer = "They save space and conserve water",
                    explanation = "Well done! Vertical sack gardens let families grow fresh vegetables in small compounds."
                ),
                QuizQuestion(
                    id = "g5-ag-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each material to whether it belongs in a compost pit or recycling bin.",
                    hint = "Hint: Food peelings and dry leaves rot into manure; plastic does not decompose.",
                    pairs = listOf(
                        MatchingPair("Banana & potato peelings", "Compost manure pit"),
                        MatchingPair("Plastic soda bottle", "Recycling bin"),
                        MatchingPair("Dry maize stalks & leaves", "Compost brown layer")
                    ),
                    explanation = "Well sorted! Organic peelings and dry leaves create nutrient-rich compost."
                ),
                QuizQuestion(
                    id = "g5-ag-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Covering soil around crops with dry grass to reduce evaporation is called ________.",
                    hint = "Hint: Starts with the letter M.",
                    blankPrefix = "Practice:",
                    blankSuffix = "",
                    correctAnswer = "Mulching",
                    acceptableAnswers = listOf("mulching", "mulch"),
                    explanation = "Correct! Mulching keeps soil moist and suppresses weeds naturally."
                )
            )
        ),
        CBCLesson(
            id = "g5-cre-values",
            title = "Stewardship, Honesty & Community Harmony",
            grade = 5,
            levelGroup = "Upper Primary",
            learningArea = LearningArea.RELIGIOUS_EDUCATION,
            strand = "Moral Living, Integrity & Values",
            subStrand = "Honesty, Kindness & Stewardship",
            durationMins = 5,
            summary = "Practice integrity, respect for elders, and responsible care of shared property.",
            questions = listOf(
                QuizQuestion(
                    id = "g5-re-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Wanjiku finds a classmate's lost KES 100 note near the school gate. What value does she practice by handing it to the teacher?",
                    hint = "Hint: Being truthful and returning what belongs to others.",
                    options = listOf("Honesty and integrity", "Selfishness", "Carelessness", "Boasting"),
                    correctAnswer = "Honesty and integrity",
                    explanation = "Well done! Returning lost property shows honesty and care for others."
                ),
                QuizQuestion(
                    id = "g5-re-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Kenyan CBC value to its everyday action.",
                    hint = "Hint: Stewardship means caring for creation; Unity means working together.",
                    pairs = listOf(
                        MatchingPair("Stewardship", "Planting trees and protecting rivers"),
                        MatchingPair("Respect", "Listening politely when elders speak"),
                        MatchingPair("Compassion", "Sharing stationery with a friend in need")
                    ),
                    explanation = "Great job! Values guide peaceful living in school and community."
                ),
                QuizQuestion(
                    id = "g5-re-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Helping neighbours or cleaning a community hall shows the spirit of ________.",
                    hint = "Hint: Working together as one community (starts with U).",
                    blankPrefix = "Value:",
                    blankSuffix = "",
                    correctAnswer = "Unity",
                    acceptableAnswers = listOf("unity", "service", "kindness", "harambee"),
                    explanation = "Spot on! Unity and service strengthen Kenyan communities."
                )
            )
        ),
        CBCLesson(
            id = "g5-arts-music",
            title = "Kenyan Folk Songs, Beadwork & Rhythm",
            grade = 5,
            levelGroup = "Upper Primary",
            learningArea = LearningArea.CREATIVE_ARTS,
            strand = "Performing Arts & Music",
            subStrand = "Kenyan Folk Songs & Traditional Instruments",
            durationMins = 5,
            summary = "Recognize traditional Kenyan musical instruments, rhythm patterns, and bead patterns.",
            questions = listOf(
                QuizQuestion(
                    id = "g5-art-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Which Kenyan traditional string instrument from Western Kenya has eight strings?",
                    hint = "Hint: Famous Luo eight-string lyre.",
                    options = listOf("Nyatiti", "Kayamba", "Chivoti", "Isukuti drum"),
                    correctAnswer = "Nyatiti",
                    explanation = "Correct! The Nyatiti is an iconic Kenyan traditional lyre."
                ),
                QuizQuestion(
                    id = "g5-art-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Kenyan traditional instrument to how it produces sound.",
                    hint = "Hint: Kayamba is shaken; Chivoti is blown; Isukuti is struck.",
                    pairs = listOf(
                        MatchingPair("Isukuti", "Membrane drum struck by hand"),
                        MatchingPair("Chivoti", "Bamboo flute blown with air"),
                        MatchingPair("Kayamba", "Reed shaker filled with seeds")
                    ),
                    explanation = "Bravo! You know Kenya's percussion, wind, and shaker instruments."
                ),
                QuizQuestion(
                    id = "g5-art-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "A steady repeated beat pattern in a song or dance is called ________.",
                    hint = "Hint: Starts with R.",
                    blankPrefix = "Musical element:",
                    blankSuffix = "",
                    correctAnswer = "Rhythm",
                    acceptableAnswers = listOf("rhythm", "tempo", "beat"),
                    explanation = "Well done! Rhythm organizes beats in music and movement."
                )
            )
        ),
        CBCLesson(
            id = "g6-eng-grammar",
            title = "Reading Comprehension & Active Voice",
            grade = 6,
            levelGroup = "Upper Primary",
            learningArea = LearningArea.ENGLISH,
            strand = "Grammar in Use",
            subStrand = "Tenses, Prepositions & Sentence Structure",
            durationMins = 5,
            summary = "Master subject-verb agreement, story vocabulary, and clear formal sentences.",
            questions = listOf(
                QuizQuestion(
                    id = "g6-e-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Choose the correct sentence: \"Neither the teacher nor the pupils ________ late for assembly.\"",
                    hint = "Hint: With neither...nor, the verb agrees with the closest noun (pupils).",
                    options = listOf("were", "was", "is", "has been"),
                    correctAnswer = "were",
                    explanation = "Spot on! Pupils is plural, so we use \"were\"."
                ),
                QuizQuestion(
                    id = "g6-e-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each vocabulary word to its accurate synonym.",
                    hint = "Hint: Diligent means hardworking.",
                    pairs = listOf(
                        MatchingPair("Diligent", "Hardworking"),
                        MatchingPair("Preserve", "Protect and keep safe"),
                        MatchingPair("Generous", "Willing to share kindly")
                    ),
                    explanation = "Great vocabulary work!"
                ),
                QuizQuestion(
                    id = "g6-e-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Complete the preposition: \"We travelled from Nairobi to Mombasa ________ train.\"",
                    hint = "Hint: 2-letter preposition used for modes of transport.",
                    blankPrefix = "Travelled",
                    blankSuffix = "train",
                    correctAnswer = "by",
                    acceptableAnswers = listOf("by", "via"),
                    explanation = "Correct! We say \"by train\", \"by bus\", or \"by air\"."
                )
            )
        ),
        CBCLesson(
            id = "g7-pretech-safety",
            title = "Workshop Safety & Technical Drawing",
            grade = 7,
            levelGroup = "Junior School",
            learningArea = LearningArea.PRE_TECHNICAL_STUDIES,
            strand = "Foundations of Pre-Technical Studies",
            subStrand = "Workshop Safety & PPE Gear",
            durationMins = 5,
            summary = "Practice workshop PPE rules, technical drawing lines, and measuring tools.",
            questions = listOf(
                QuizQuestion(
                    id = "g7-pt-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Which PPE protects your eyes from wood shavings or metal sparks in a school workshop?",
                    hint = "Hint: Clear protective eyewear.",
                    options = listOf("Safety goggles", "Cotton apron", "Gumboots", "Ear muffs"),
                    correctAnswer = "Safety goggles",
                    explanation = "Correct! Safety goggles shield eyes from dust and sparks."
                ),
                QuizQuestion(
                    id = "g7-pt-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Pre-Technical tool to its workshop use.",
                    hint = "Hint: Try square checks 90-degree corners.",
                    pairs = listOf(
                        MatchingPair("Try square", "Checking right angles (90°)"),
                        MatchingPair("Tape measure", "Measuring room or timber length"),
                        MatchingPair("Hacksaw", "Cutting metal rods and PVC pipes")
                    ),
                    explanation = "Great mastery of Junior School Pre-Technical tools!"
                ),
                QuizQuestion(
                    id = "g7-pt-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "In technical drawing, a thin dark line showing visible outer edges is an ________ line.",
                    hint = "Hint: Starts with O.",
                    blankPrefix = "Line type:",
                    blankSuffix = "line",
                    correctAnswer = "outline",
                    acceptableAnswers = listOf("outline", "object"),
                    explanation = "Correct! An outline defines the visible edges of a drawing."
                )
            )
        ),
        CBCLesson(
            id = "g8-comp-algorithms",
            title = "Safe Computing, Data & Algorithms",
            grade = 8,
            levelGroup = "Junior School",
            learningArea = LearningArea.COMPUTER_STUDIES,
            strand = "Coding & Problem Solving",
            subStrand = "Algorithms, Flowcharts & Pseudocode Design",
            durationMins = 5,
            summary = "Build step-by-step algorithms, recognize hardware, and practice strong password hygiene.",
            questions = listOf(
                QuizQuestion(
                    id = "g8-cs-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "In a flowchart, which geometric shape represents a Decision (Yes/No condition)?",
                    hint = "Hint: 4-sided tilted shape.",
                    options = listOf("Diamond", "Oval", "Rectangle", "Circle"),
                    correctAnswer = "Diamond",
                    explanation = "Spot on! A diamond shows a branching decision in a flowchart."
                ),
                QuizQuestion(
                    id = "g8-cs-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each computing concept to its example.",
                    hint = "Hint: Keyboard enters data; CPU processes instructions.",
                    pairs = listOf(
                        MatchingPair("Keyboard & Scanner", "Input Devices"),
                        MatchingPair("CPU (Processor)", "Executes program instructions"),
                        MatchingPair("Two-Factor Auth", "Extra security beyond a password")
                    ),
                    explanation = "Awesome computer studies fluency!"
                ),
                QuizQuestion(
                    id = "g8-cs-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "A clear step-by-step set of instructions designed to solve a problem is an ________.",
                    hint = "Hint: Starts with A.",
                    blankPrefix = "Term:",
                    blankSuffix = "",
                    correctAnswer = "Algorithm",
                    acceptableAnswers = listOf("algorithm"),
                    explanation = "Correct! Every program starts from a structured algorithm."
                )
            )
        ),
        CBCLesson(
            id = "g8-biz-enterprise",
            title = "Entrepreneurship, Budgeting & Saving",
            grade = 8,
            levelGroup = "Junior School",
            learningArea = LearningArea.BUSINESS_STUDIES,
            strand = "Entrepreneurship & Production",
            subStrand = "Business Plans, Marketing & Consumer Protection",
            durationMins = 5,
            summary = "Calculate profit vs loss for Kenyan micro-enterprises and practice smart budgeting.",
            questions = listOf(
                QuizQuestion(
                    id = "g8-bz-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "A student club spends KES 800 on beads and sells bracelets for KES 1,250. What is their profit?",
                    hint = "Hint: Profit = Selling Price minus Cost Price.",
                    options = listOf("KES 450", "KES 350", "KES 800", "KES 2,050"),
                    correctAnswer = "KES 450",
                    explanation = "Correct! KES 1,250 - KES 800 = KES 450 profit."
                ),
                QuizQuestion(
                    id = "g8-bz-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each business term to its Kenyan definition.",
                    hint = "Hint: Capital starts a venture; Budget plans income and expenses.",
                    pairs = listOf(
                        MatchingPair("Capital", "Money or assets used to start a business"),
                        MatchingPair("Budget", "Plan balancing expected income & expenses"),
                        MatchingPair("Needs vs Wants", "Food & shelter before luxury items")
                    ),
                    explanation = "Great entrepreneurial thinking!"
                ),
                QuizQuestion(
                    id = "g8-bz-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "A written document describing a business idea, customers, and financial plan is a business ________.",
                    hint = "Hint: 4-letter word starting with P.",
                    blankPrefix = "Business",
                    blankSuffix = "",
                    correctAnswer = "plan",
                    acceptableAnswers = listOf("plan"),
                    explanation = "Spot on! A business plan keeps entrepreneurs organized."
                )
            )
        ),
        CBCLesson(
            id = "g10-bio-cells",
            title = "Cell Ultrastructure & Transport in Plants",
            grade = 10,
            levelGroup = "Senior School",
            learningArea = LearningArea.BIOLOGY,
            strand = "Cell Biology & Physiology",
            subStrand = "Microscopy, Cell Ultrastructure & Organelles",
            durationMins = 5,
            summary = "Distinguish plant organelles and apply osmosis & xylem transport to Kenyan crops.",
            questions = listOf(
                QuizQuestion(
                    id = "g10-bio-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Which organelle inside a maize leaf cell converts light energy into glucose?",
                    hint = "Hint: Contains green chlorophyll pigment.",
                    options = listOf("Chloroplast", "Mitochondrion", "Ribosome", "Golgi body"),
                    correctAnswer = "Chloroplast",
                    explanation = "Accurate! Chloroplasts house chlorophyll and run photosynthesis."
                ),
                QuizQuestion(
                    id = "g10-bio-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each plant biology structure to its function.",
                    hint = "Hint: Xylem moves water up; Phloem transports sugars.",
                    pairs = listOf(
                        MatchingPair("Xylem vessels", "Transport water & mineral salts from roots"),
                        MatchingPair("Phloem sieve tubes", "Translocate sugars from leaves"),
                        MatchingPair("Guard cells", "Regulate opening & closing of stomata")
                    ),
                    explanation = "Excellent Senior School Biology mastery!"
                ),
                QuizQuestion(
                    id = "g10-bio-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Water movement across a semi-permeable membrane from high to low water potential is ________.",
                    hint = "Hint: Starts with O.",
                    blankPrefix = "Process:",
                    blankSuffix = "",
                    correctAnswer = "Osmosis",
                    acceptableAnswers = listOf("osmosis"),
                    explanation = "Correct! Osmosis drives water uptake into root hair cells."
                )
            )
        ),
        CBCLesson(
            id = "g10-chem-acids",
            title = "Atomic Structure, Acids, Bases & Salts",
            grade = 10,
            levelGroup = "Senior School",
            learningArea = LearningArea.CHEMISTRY,
            strand = "Acids, Bases, Salts & Stoichiometry",
            subStrand = "pH Scale, Neutralization & Soil Acidity Management",
            durationMins = 5,
            summary = "Interpret pH values in soil testing and balance neutralization reactions.",
            questions = listOf(
                QuizQuestion(
                    id = "g10-ch-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "A tea farmer in Kericho records soil pH 4.8. Which substance neutralizes excess soil acidity?",
                    hint = "Hint: A basic calcium compound applied as agricultural lime.",
                    options = listOf("Calcium hydroxide / Agricultural lime", "Hydrochloric acid", "Vinegar", "Rainwater"),
                    correctAnswer = "Calcium hydroxide / Agricultural lime",
                    explanation = "Spot on! Agricultural lime neutralizes acidic soils."
                ),
                QuizQuestion(
                    id = "g10-ch-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each pH value to its chemical nature.",
                    hint = "Hint: pH 7 is neutral; below 7 is acidic.",
                    pairs = listOf(
                        MatchingPair("pH 2 (Lemon juice)", "Strong / moderate acid"),
                        MatchingPair("pH 7 (Pure water)", "Neutral solution"),
                        MatchingPair("pH 12 (Wood ash)", "Alkaline base")
                    ),
                    explanation = "Great command of the pH scale!"
                ),
                QuizQuestion(
                    id = "g10-ch-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "An acid reacts with a base to produce a salt and ________.",
                    hint = "Hint: H2O.",
                    blankPrefix = "Acid + Base -> Salt +",
                    blankSuffix = "",
                    correctAnswer = "Water",
                    acceptableAnswers = listOf("water", "h2o"),
                    explanation = "Correct! Neutralization forms a salt and water."
                )
            )
        ),
        CBCLesson(
            id = "g10-phy-electricity",
            title = "Kinematics, Light & Ohm's Law Circuits",
            grade = 10,
            levelGroup = "Senior School",
            learningArea = LearningArea.PHYSICS,
            strand = "Electricity, Magnetism & Modern Physics",
            subStrand = "Current, Voltage, Ohm's Law & Parallel Circuits",
            durationMins = 5,
            summary = "Apply V = IR to solar circuits and compare series vs parallel wiring.",
            questions = listOf(
                QuizQuestion(
                    id = "g10-ph-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "A 12 V solar battery drives 2 A through an LED bulb. What is the bulb's resistance?",
                    hint = "Hint: Using Ohm's Law, R = V / I.",
                    options = listOf("6 ohms", "24 ohms", "10 ohms", "14 ohms"),
                    correctAnswer = "6 ohms",
                    explanation = "Accurate! R = V / I = 12 V / 2 A = 6 ohms."
                ),
                QuizQuestion(
                    id = "g10-ph-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each electrical quantity to its SI unit.",
                    hint = "Hint: Amperes measure current; Volts measure voltage.",
                    pairs = listOf(
                        MatchingPair("Electric Current (I)", "Ampere (A)"),
                        MatchingPair("Potential Difference (V)", "Volt (V)"),
                        MatchingPair("Resistance (R)", "Ohm")
                    ),
                    explanation = "Excellent Senior Physics precision!"
                ),
                QuizQuestion(
                    id = "g10-ph-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Household lights in Kenya are wired in ________ so one blown bulb does not switch off others.",
                    hint = "Hint: Opposite of series wiring.",
                    blankPrefix = "Circuit type:",
                    blankSuffix = "",
                    correctAnswer = "parallel",
                    acceptableAnswers = listOf("parallel"),
                    explanation = "Correct! Parallel branches operate independently."
                )
            )
        ),
        CBCLesson(
            id = "g10-geo-rift",
            title = "Internal Landforming, Climate & Drainage",
            grade = 10,
            levelGroup = "Senior School",
            learningArea = LearningArea.GEOGRAPHY,
            strand = "Physical Geography & Earth Processes",
            subStrand = "Faulting, Volcanicity & Rift Valley Formation",
            durationMins = 5,
            summary = "Analyze faulting in the Rift Valley, Kenyan lakes, and relief rainfall.",
            questions = listOf(
                QuizQuestion(
                    id = "g10-geo-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Which internal earth process formed the Gregory Rift Valley across Kenya?",
                    hint = "Hint: Tensional forces pulled crustal blocks apart.",
                    options = listOf("Faulting", "Glaciation", "Wave deposition", "Aeolian erosion"),
                    correctAnswer = "Faulting",
                    explanation = "Spot on! Tensional faulting formed the Kenyan Rift Valley."
                ),
                QuizQuestion(
                    id = "g10-geo-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each Kenyan physical feature to its origin.",
                    hint = "Hint: Mt. Kenya is volcanic; Lake Victoria is a downwarped basin.",
                    pairs = listOf(
                        MatchingPair("Mount Kenya", "Composite volcanic mountain"),
                        MatchingPair("Lake Nakuru", "Rift Valley tectonic lake"),
                        MatchingPair("Lake Victoria", "Crustal downwarping basin")
                    ),
                    explanation = "Great Senior School Geography analysis!"
                ),
                QuizQuestion(
                    id = "g10-geo-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Rainfall caused when moist air rises up windward slopes such as Mt. Kenya is called ________ rainfall.",
                    hint = "Hint: Starts with R or O.",
                    blankPrefix = "Type:",
                    blankSuffix = "rainfall",
                    correctAnswer = "Relief",
                    acceptableAnswers = listOf("relief", "orographic"),
                    explanation = "Correct! Relief rainfall nourishes Kenya's highland tea zones."
                )
            )
        ),
        CBCLesson(
            id = "g10-hist-constitution",
            title = "Kenyan Sovereignty, Devolution & Rights",
            grade = 10,
            levelGroup = "Senior School",
            learningArea = LearningArea.HISTORY_CITIZENSHIP,
            strand = "Constitution, Devolution & Human Rights",
            subStrand = "2010 Constitution, 47 Counties & Separation of Powers",
            durationMins = 5,
            summary = "Examine Kenya's three arms of government, 47 counties, and Bill of Rights.",
            questions = listOf(
                QuizQuestion(
                    id = "g10-hc-1",
                    type = QuestionType.MULTIPLE_CHOICE,
                    prompt = "Under the Constitution of Kenya 2010, how many devolved county governments operate?",
                    hint = "Hint: One less than 48.",
                    options = listOf("47", "42", "8", "52"),
                    correctAnswer = "47",
                    explanation = "Correct! Kenya has 47 devolved county governments."
                ),
                QuizQuestion(
                    id = "g10-hc-2",
                    type = QuestionType.MATCHING,
                    prompt = "Match each arm of Kenya's national government to its mandate.",
                    hint = "Hint: Parliament makes laws; Judiciary interprets laws.",
                    pairs = listOf(
                        MatchingPair("Legislature (Parliament)", "Enacts laws & oversees revenue"),
                        MatchingPair("Executive", "Implements policies & public service"),
                        MatchingPair("Judiciary", "Interprets laws & resolves disputes")
                    ),
                    explanation = "Excellent civic literacy!"
                ),
                QuizQuestion(
                    id = "g10-hc-3",
                    type = QuestionType.FILL_BLANK,
                    prompt = "Chapter Four of the 2010 Constitution protects freedoms and is called the ________ of Rights.",
                    hint = "Hint: 4-letter word starting with B.",
                    blankPrefix = "The",
                    blankSuffix = "of Rights",
                    correctAnswer = "Bill",
                    acceptableAnswers = listOf("bill"),
                    explanation = "Accurate! The Bill of Rights guarantees fundamental freedoms."
                )
            )
        )
    )

    fun subjectsForGrade(grade: Int): List<LearningArea> =
        curriculumTree
            .filter { grade in it.minGrade..it.maxGrade }
            .map { it.learningArea }

    fun strandsForSubject(
        area: LearningArea,
        gradeFilter: Int? = null
    ): List<CbcStrandNode> {
        val subject = curriculumTree.firstOrNull { it.learningArea == area } ?: return emptyList()
        return subject.strands.mapNotNull { strandNode ->
            val filteredSubs = if (gradeFilter == null) {
                strandNode.subStrands
            } else {
                strandNode.subStrands.filter { gradeFilter in it.minGrade..it.maxGrade }
            }
            if (filteredSubs.isEmpty()) null else CbcStrandNode(strandNode.strand, filteredSubs)
        }
    }

    fun buildCatalogForGrade(grade: Int): List<CbcCatalogEntry> {
        val levelGroup = levelGroupForGrade(grade)
        val entries = mutableListOf<CbcCatalogEntry>()
        curriculumTree.forEach { subject ->
            if (grade in subject.minGrade..subject.maxGrade) {
                subject.strands.forEach { strandNode ->
                    strandNode.subStrands.forEach { sub ->
                        if (grade in sub.minGrade..sub.maxGrade) {
                            entries.add(
                                CbcCatalogEntry(
                                    grade = grade,
                                    levelGroup = levelGroup,
                                    pathway = sub.pathway ?: subject.pathway,
                                    learningArea = subject.learningArea,
                                    strand = strandNode.strand,
                                    subStrand = sub.subStrand,
                                    linkedLessonId = sub.linkedLessonId
                                )
                            )
                        }
                    }
                }
            }
        }
        return entries
    }

    fun buildFullCatalog(): List<CbcCatalogEntry> =
        (1..12).flatMap { buildCatalogForGrade(it) }
}
