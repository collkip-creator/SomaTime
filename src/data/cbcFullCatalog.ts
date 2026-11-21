import type {
  CBCLevelGroup,
  CbcCatalogEntry,
  CbcSubjectCurriculum,
  LearningArea,
} from '../types/somatime';

function levelGroupForGrade(grade: number): CBCLevelGroup {
  if (grade <= 3) return 'Lower Primary';
  if (grade <= 6) return 'Upper Primary';
  if (grade <= 9) return 'Junior School';
  return 'Senior School';
}

export const CBC_CURRICULUM_TREE: CbcSubjectCurriculum[] = [
  {
    learningArea: 'Mathematics',
    minGrade: 1,
    maxGrade: 12,
    strands: [
      {
        strand: 'Numbers',
        subStrands: [
          { subStrand: 'Pre-Number Activities & Whole Numbers (1–100)', minGrade: 1, maxGrade: 2, linkedLessonId: 'g1-math-coins' },
          { subStrand: 'Addition, Subtraction & Whole Numbers', minGrade: 1, maxGrade: 3, linkedLessonId: 'g1-math-coins' },
          { subStrand: 'Multiplication, Division & Place Value', minGrade: 3, maxGrade: 5, linkedLessonId: 'g4-math-fractions' },
          { subStrand: 'Fractions in Daily Life', minGrade: 4, maxGrade: 6, linkedLessonId: 'g4-math-fractions' },
          { subStrand: 'Decimals in Shopping & Measurement', minGrade: 5, maxGrade: 6, linkedLessonId: 'g5-math-decimals' },
          { subStrand: 'Percentages, Ratios & Proportions', minGrade: 6, maxGrade: 7, linkedLessonId: 'g6-math-percentages' },
          { subStrand: 'Integers, Squares & Indices', minGrade: 7, maxGrade: 9, linkedLessonId: 'g8-math-algebra' },
          { subStrand: 'Logarithms, Matrices & Vectors', minGrade: 10, maxGrade: 12, pathway: 'STEM & Social Sciences', linkedLessonId: 'g11-math-finance' },
        ],
      },
      {
        strand: 'Measurement',
        subStrands: [
          { subStrand: 'Kenyan Shilling Coins & Notes', minGrade: 1, maxGrade: 3, linkedLessonId: 'g1-math-coins' },
          { subStrand: 'Length, Mass, Capacity & Clock Time', minGrade: 2, maxGrade: 5, linkedLessonId: 'g3-math-measurement' },
          { subStrand: 'Perimeter, Area & Volume of Solids', minGrade: 5, maxGrade: 8, linkedLessonId: 'g3-math-measurement' },
          { subStrand: 'Speed, Distance, Time & Density', minGrade: 6, maxGrade: 9, linkedLessonId: 'g8-math-algebra' },
          { subStrand: 'Trigonometry & Bearings', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g11-math-finance' },
        ],
      },
      {
        strand: 'Geometry',
        subStrands: [
          { subStrand: '2D Shapes, Patterns & Straight Lines', minGrade: 1, maxGrade: 4, linkedLessonId: 'g3-math-measurement' },
          { subStrand: 'Angles, Triangles & Symmetry', minGrade: 4, maxGrade: 7, linkedLessonId: 'g4-math-fractions' },
          { subStrand: 'Geometrical Constructions & Scale Drawing', minGrade: 7, maxGrade: 9, linkedLessonId: 'g8-math-algebra' },
          { subStrand: 'Circle Theorems & 3D Geometry', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g11-math-finance' },
        ],
      },
      {
        strand: 'Algebra',
        subStrands: [
          { subStrand: 'Number Patterns & Simple Unknowns', minGrade: 4, maxGrade: 6, linkedLessonId: 'g5-math-decimals' },
          { subStrand: 'Algebraic Expressions & Linear Equations', minGrade: 7, maxGrade: 9, linkedLessonId: 'g8-math-algebra' },
          { subStrand: 'Linear Inequalities & Simultaneous Equations', minGrade: 8, maxGrade: 10, linkedLessonId: 'g8-math-algebra' },
          { subStrand: 'Quadratic Equations, Sequences & Calculus Intro', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g11-math-finance' },
        ],
      },
      {
        strand: 'Data Handling & Probability',
        subStrands: [
          { subStrand: 'Tally Marks, Pictographs & Bar Graphs', minGrade: 4, maxGrade: 6, linkedLessonId: 'g6-math-percentages' },
          { subStrand: 'Mean, Median, Mode & Pie Charts', minGrade: 7, maxGrade: 9, linkedLessonId: 'g8-math-algebra' },
          { subStrand: 'Grouped Statistics, Permutations & Probability', minGrade: 10, maxGrade: 12, pathway: 'STEM & Social Sciences', linkedLessonId: 'g11-math-finance' },
        ],
      },
      {
        strand: 'Commercial Arithmetic',
        subStrands: [
          { subStrand: 'Profit, Loss, Discounts & Commission', minGrade: 6, maxGrade: 9, linkedLessonId: 'g6-math-percentages' },
          { subStrand: 'Simple Interest & Hire Purchase', minGrade: 8, maxGrade: 10, linkedLessonId: 'g11-math-finance' },
          { subStrand: 'Compound Interest, Appreciation & Taxation', minGrade: 10, maxGrade: 12, pathway: 'STEM & Social Sciences', linkedLessonId: 'g11-math-finance' },
        ],
      },
    ],
  },
  {
    learningArea: 'English',
    minGrade: 1,
    maxGrade: 12,
    strands: [
      {
        strand: 'Listening & Speaking',
        subStrands: [
          { subStrand: 'Polite Expressions, Greetings & Phonics', minGrade: 1, maxGrade: 3, linkedLessonId: 'g1-eng-reading' },
          { subStrand: 'Pronunciation, Stress & Oral Narratives', minGrade: 4, maxGrade: 6, linkedLessonId: 'g6-eng-grammar' },
          { subStrand: 'Debates, Speeches & Conversational Etiquette', minGrade: 7, maxGrade: 9, linkedLessonId: 'g6-eng-grammar' },
          { subStrand: 'Public Speaking, Interviews & Discourse', minGrade: 10, maxGrade: 12, linkedLessonId: 'g6-eng-grammar' },
        ],
      },
      {
        strand: 'Reading',
        subStrands: [
          { subStrand: 'Sight Words, Short Stories & Picture Books', minGrade: 1, maxGrade: 3, linkedLessonId: 'g1-eng-reading' },
          { subStrand: 'Kenyan Folktales, Fluency & Vocabulary', minGrade: 4, maxGrade: 6, linkedLessonId: 'g6-eng-grammar' },
          { subStrand: 'Intensive Comprehension, Poetry & Novellas', minGrade: 7, maxGrade: 9, linkedLessonId: 'g6-eng-grammar' },
          { subStrand: 'Critical Literary Analysis & Set Books', minGrade: 10, maxGrade: 12, linkedLessonId: 'g6-eng-grammar' },
        ],
      },
      {
        strand: 'Grammar in Use',
        subStrands: [
          { subStrand: 'Nouns, Pronouns, Plurals & Articles', minGrade: 1, maxGrade: 3, linkedLessonId: 'g1-eng-reading' },
          { subStrand: 'Tenses, Prepositions & Conjunctions', minGrade: 4, maxGrade: 6, linkedLessonId: 'g6-eng-grammar' },
          { subStrand: 'Active/Passive Voice & Direct/Indirect Speech', minGrade: 7, maxGrade: 9, linkedLessonId: 'g6-eng-grammar' },
          { subStrand: 'Complex Clauses, Modifiers & Pragmatics', minGrade: 10, maxGrade: 12, linkedLessonId: 'g6-eng-grammar' },
        ],
      },
      {
        strand: 'Writing',
        subStrands: [
          { subStrand: 'Handwriting, Punctuation & Guided Sentences', minGrade: 1, maxGrade: 3, linkedLessonId: 'g1-eng-reading' },
          { subStrand: 'Paragraphs, Friendly Letters & Creative Stories', minGrade: 4, maxGrade: 6, linkedLessonId: 'g6-eng-grammar' },
          { subStrand: 'Formal Letters, Diaries & Book Reviews', minGrade: 7, maxGrade: 9, linkedLessonId: 'g6-eng-grammar' },
          { subStrand: 'Functional Reports, Essays & Memos', minGrade: 10, maxGrade: 12, linkedLessonId: 'g6-eng-grammar' },
        ],
      },
    ],
  },
  {
    learningArea: 'Kiswahili',
    minGrade: 1,
    maxGrade: 12,
    strands: [
      {
        strand: 'Kusikiliza na Kuzungumza',
        subStrands: [
          { subStrand: 'Maamkuzi, Adabu na Msamiati wa Nyumbani', minGrade: 1, maxGrade: 3, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Matamshi Bora, Hadithi na Majadiliano', minGrade: 4, maxGrade: 6, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Mazungumzo Rasmi, Mijadala na Mashairi', minGrade: 7, maxGrade: 9, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Ufahamu wa Kusikiliza na Fasihi Simulizi', minGrade: 10, maxGrade: 12, linkedLessonId: 'g2-kisw-msamiati' },
        ],
      },
      {
        strand: 'Kusoma',
        subStrands: [
          { subStrand: 'Kusoma Silabi, Maneno na Sentensi Fupi', minGrade: 1, maxGrade: 3, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Kusoma kwa Ufahamu na Methali za Kiswahili', minGrade: 4, maxGrade: 6, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Uchambuzi wa Riwaya, Tamthilia na Ushairi', minGrade: 7, maxGrade: 12, linkedLessonId: 'g2-kisw-msamiati' },
        ],
      },
      {
        strand: 'Sarufi na Matumizi ya Lugha',
        subStrands: [
          { subStrand: 'Nomino, Vitenzi na Umoja/Wingi', minGrade: 1, maxGrade: 4, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Ngeli za Nomino (A-WA, KI-VI, M-MI) na Nyakati', minGrade: 5, maxGrade: 9, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Mnyambuliko wa Vitenzi, Vihusishi na Sintaksia', minGrade: 8, maxGrade: 12, linkedLessonId: 'g2-kisw-msamiati' },
        ],
      },
      {
        strand: 'Kuandika (Insha)',
        subStrands: [
          { subStrand: 'Imla, Tahajia na Insha za Picha', minGrade: 1, maxGrade: 4, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Barua ya Kirafiki na Insha za Masimulizi', minGrade: 5, maxGrade: 7, linkedLessonId: 'g2-kisw-msamiati' },
          { subStrand: 'Barua Rasmi, Ripoti na Kumbukumbu', minGrade: 8, maxGrade: 12, linkedLessonId: 'g2-kisw-msamiati' },
        ],
      },
    ],
  },
  {
    learningArea: 'Environmental Activities',
    minGrade: 1,
    maxGrade: 3,
    strands: [
      {
        strand: 'Social Environment',
        subStrands: [
          { subStrand: 'My Home, Family & School Community', minGrade: 1, maxGrade: 2, linkedLessonId: 'g2-env-weather' },
          { subStrand: 'Community Leaders, Safe Roads & Child Rights', minGrade: 2, maxGrade: 3, linkedLessonId: 'g2-env-weather' },
          { subStrand: 'Cultural Events & Harmony in Kenya', minGrade: 1, maxGrade: 3, linkedLessonId: 'g2-env-weather' },
        ],
      },
      {
        strand: 'Natural Environment',
        subStrands: [
          { subStrand: 'Weather Patterns, Clouds & Seasonal Clothing', minGrade: 1, maxGrade: 3, linkedLessonId: 'g2-env-weather' },
          { subStrand: 'Soil Types, Domestic Animals & Plants', minGrade: 1, maxGrade: 3, linkedLessonId: 'g2-env-weather' },
          { subStrand: 'Light, Shadows & Sources of Heat', minGrade: 2, maxGrade: 3, linkedLessonId: 'g2-env-weather' },
        ],
      },
      {
        strand: 'Resources in Our Environment',
        subStrands: [
          { subStrand: 'Safe Drinking Water & Water Conservation', minGrade: 1, maxGrade: 3, linkedLessonId: 'g2-env-weather' },
          { subStrand: 'Caring for Trees & Waste Disposal', minGrade: 1, maxGrade: 3, linkedLessonId: 'g2-env-weather' },
          { subStrand: 'Saving Energy at Home and School', minGrade: 2, maxGrade: 3, linkedLessonId: 'g2-env-weather' },
        ],
      },
    ],
  },
  {
    learningArea: 'Science & Technology',
    minGrade: 4,
    maxGrade: 6,
    strands: [
      {
        strand: 'Living Things & Their Environment',
        subStrands: [
          { subStrand: 'Parts of Plants, Germination & Crops', minGrade: 4, maxGrade: 5, linkedLessonId: 'g4-scitech-matter' },
          { subStrand: 'Vertebrates, Invertebrates & Adaptations', minGrade: 4, maxGrade: 6, linkedLessonId: 'g4-scitech-matter' },
          { subStrand: 'Human Breathing, Digestive & Circulatory Systems', minGrade: 4, maxGrade: 6, linkedLessonId: 'g4-scitech-matter' },
        ],
      },
      {
        strand: 'Matter & Mixtures',
        subStrands: [
          { subStrand: 'Solids, Liquids & Gases (Changes of State)', minGrade: 4, maxGrade: 5, linkedLessonId: 'g4-scitech-matter' },
          { subStrand: 'Separating Mixtures (Filtration, Sieving, Magnets)', minGrade: 4, maxGrade: 6, linkedLessonId: 'g4-scitech-matter' },
          { subStrand: 'Properties of Air, Oxygen & Rusting', minGrade: 5, maxGrade: 6, linkedLessonId: 'g4-scitech-matter' },
        ],
      },
      {
        strand: 'Force, Energy & Simple Machines',
        subStrands: [
          { subStrand: 'Light Reflection, Shadows & Sound Waves', minGrade: 4, maxGrade: 6, linkedLessonId: 'g4-scitech-matter' },
          { subStrand: 'Heat Transfer (Conduction, Convection, Radiation)', minGrade: 5, maxGrade: 6, linkedLessonId: 'g4-scitech-matter' },
          { subStrand: 'Levers, Pulleys & Inclined Planes', minGrade: 5, maxGrade: 6, linkedLessonId: 'g4-scitech-matter' },
        ],
      },
      {
        strand: 'Digital Literacy & Computing',
        subStrands: [
          { subStrand: 'Digital Devices, Word Processing & Presentations', minGrade: 4, maxGrade: 5, linkedLessonId: 'g8-comp-algorithms' },
          { subStrand: 'Visual Coding Blocks & Safe Internet Use', minGrade: 4, maxGrade: 6, linkedLessonId: 'g8-comp-algorithms' },
        ],
      },
    ],
  },
  {
    learningArea: 'Integrated Science',
    minGrade: 7,
    maxGrade: 12,
    strands: [
      {
        strand: 'Scientific Investigation',
        subStrands: [
          { subStrand: 'Laboratory Apparatus, Hazard Symbols & Safety', minGrade: 7, maxGrade: 8, linkedLessonId: 'g7-sci-apparatus' },
          { subStrand: 'SI Units, Measurement & Experimental Design', minGrade: 7, maxGrade: 9, linkedLessonId: 'g7-sci-apparatus' },
          { subStrand: 'Mixtures, Solutions & Chromatography', minGrade: 7, maxGrade: 8, linkedLessonId: 'g4-scitech-matter' },
        ],
      },
      {
        strand: 'Mixtures, Elements & Compounds',
        subStrands: [
          { subStrand: 'Acids, Bases, Indicators & Neutralization', minGrade: 7, maxGrade: 9, linkedLessonId: 'g10-chem-acids' },
          { subStrand: 'Periodic Table, Elements & Chemical Equations', minGrade: 8, maxGrade: 10, linkedLessonId: 'g10-chem-acids' },
          { subStrand: 'Water Hardness, Rust Prevention & Extraction', minGrade: 8, maxGrade: 11, linkedLessonId: 'g10-chem-acids' },
        ],
      },
      {
        strand: 'Living Things & Environment',
        subStrands: [
          { subStrand: 'Human Reproductive, Excretory & Nervous Systems', minGrade: 7, maxGrade: 9, linkedLessonId: 'g9-sci-ecosystems' },
          { subStrand: 'Cell Structure, Diffusion & Osmosis', minGrade: 8, maxGrade: 10, linkedLessonId: 'g10-bio-cells' },
          { subStrand: 'Food Webs, Biodiversity & Savanna Conservation', minGrade: 8, maxGrade: 10, linkedLessonId: 'g9-sci-ecosystems' },
          { subStrand: 'Genetics, Biotechnology & Food Security', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g12-sci-genetics' },
        ],
      },
      {
        strand: 'Force, Energy & Sustainability',
        subStrands: [
          { subStrand: 'Static & Current Electricity, Magnets & Circuits', minGrade: 7, maxGrade: 9, linkedLessonId: 'g10-phy-electricity' },
          { subStrand: 'Pressure, Curved Mirrors & Electromagnetic Waves', minGrade: 8, maxGrade: 11, linkedLessonId: 'g10-phy-electricity' },
          { subStrand: 'Renewable Power Systems (Olkaria Geothermal)', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-stem-energy' },
        ],
      },
    ],
  },
  {
    learningArea: 'Social Studies',
    minGrade: 4,
    maxGrade: 9,
    strands: [
      {
        strand: 'Natural & Historic Built Environments',
        subStrands: [
          { subStrand: 'Compass Directions, Maps & Kenya Counties', minGrade: 4, maxGrade: 5, linkedLessonId: 'g6-sst-resources' },
          { subStrand: 'East African Rift Valley, Mountains & Lakes', minGrade: 5, maxGrade: 8, linkedLessonId: 'g6-sst-resources' },
          { subStrand: 'Climatic Regions & Historic Sites (Fort Jesus, Gedi)', minGrade: 6, maxGrade: 9, linkedLessonId: 'g6-sst-resources' },
        ],
      },
      {
        strand: 'People, Population & Social Organization',
        subStrands: [
          { subStrand: 'Kenyan Language Groups, Migration & Heritage', minGrade: 4, maxGrade: 6, linkedLessonId: 'g6-sst-resources' },
          { subStrand: 'Population Distribution & Urbanization in Kenya', minGrade: 6, maxGrade: 9, linkedLessonId: 'g6-sst-resources' },
          { subStrand: 'African Early Civilizations & Peacebuilding', minGrade: 7, maxGrade: 9, linkedLessonId: 'g10-hist-constitution' },
        ],
      },
      {
        strand: 'Resources & Economic Activities',
        subStrands: [
          { subStrand: 'Cash Crops, Livestock, Fisheries & Tourism', minGrade: 4, maxGrade: 7, linkedLessonId: 'g6-sst-resources' },
          { subStrand: 'Mombasa Port, SGR & East African Trade Corridors', minGrade: 5, maxGrade: 8, linkedLessonId: 'g6-sst-resources' },
          { subStrand: 'Mining, Wildlife Conservation & Blue Economy', minGrade: 6, maxGrade: 9, linkedLessonId: 'g6-sst-resources' },
        ],
      },
      {
        strand: 'Citizenship & Governance',
        subStrands: [
          { subStrand: 'National Symbols, Unity & Democratic Values', minGrade: 4, maxGrade: 6, linkedLessonId: 'g10-hist-constitution' },
          { subStrand: 'The Constitution of Kenya, Devolution & Human Rights', minGrade: 6, maxGrade: 9, linkedLessonId: 'g10-hist-constitution' },
          { subStrand: 'Regional Cooperation (EAC, AU & Global Citizenship)', minGrade: 7, maxGrade: 9, linkedLessonId: 'g10-hist-constitution' },
        ],
      },
    ],
  },
  {
    learningArea: 'Religious Education',
    minGrade: 1,
    maxGrade: 12,
    strands: [
      {
        strand: 'Creation & Sacred Scriptures',
        subStrands: [
          { subStrand: 'Appreciating God’s Creation & Caring for Life', minGrade: 1, maxGrade: 3, linkedLessonId: 'g5-cre-values' },
          { subStrand: 'Sacred Texts, Prophets & Moral Teachings', minGrade: 4, maxGrade: 6, linkedLessonId: 'g5-cre-values' },
          { subStrand: 'Faith Leaders, Covenant & Stewardship Responsibility', minGrade: 7, maxGrade: 9, linkedLessonId: 'g5-cre-values' },
          { subStrand: 'Scriptural Hermeneutics & Contemporary Ethics', minGrade: 10, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g5-cre-values' },
        ],
      },
      {
        strand: 'Moral Living, Integrity & Values',
        subStrands: [
          { subStrand: 'Obedience, Sharing & Gratitude at Home', minGrade: 1, maxGrade: 3, linkedLessonId: 'g5-cre-values' },
          { subStrand: 'Honesty, Kindness, Forgiveness & Peace', minGrade: 4, maxGrade: 7, linkedLessonId: 'g5-cre-values' },
          { subStrand: 'Peer Pressure Resistance, Leisure & Integrity', minGrade: 7, maxGrade: 9, linkedLessonId: 'g5-cre-values' },
          { subStrand: 'Social Justice, Work Ethics & Civic Virtue', minGrade: 10, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g5-cre-values' },
        ],
      },
      {
        strand: 'Worship, Community & Service',
        subStrands: [
          { subStrand: 'Prayer, Places of Worship & Religious Festivals', minGrade: 1, maxGrade: 5, linkedLessonId: 'g5-cre-values' },
          { subStrand: 'Community Service, Interfaith Respect & Charity', minGrade: 6, maxGrade: 12, linkedLessonId: 'g5-cre-values' },
        ],
      },
    ],
  },
  {
    learningArea: 'Agriculture & Nutrition',
    minGrade: 4,
    maxGrade: 9,
    strands: [
      {
        strand: 'Conservation of Resources',
        subStrands: [
          { subStrand: 'Soil Erosion Control, Mulching & Cover Crops', minGrade: 4, maxGrade: 6, linkedLessonId: 'g5-agri-kitchen-garden' },
          { subStrand: 'Organic Compost Manure & Water Harvesting', minGrade: 4, maxGrade: 8, linkedLessonId: 'g5-agri-kitchen-garden' },
          { subStrand: 'Agroforestry & Wildlife Coexistence', minGrade: 6, maxGrade: 9, linkedLessonId: 'g5-agri-kitchen-garden' },
        ],
      },
      {
        strand: 'Food Production Processes',
        subStrands: [
          { subStrand: 'Urban Kitchen & Vertical Sack Gardens', minGrade: 4, maxGrade: 7, linkedLessonId: 'g5-agri-kitchen-garden' },
          { subStrand: 'Drip Irrigation, Nursery Beds & Crop Pests', minGrade: 5, maxGrade: 9, linkedLessonId: 'g5-agri-kitchen-garden' },
          { subStrand: 'Small Domestic Animals & Poultry Care', minGrade: 6, maxGrade: 9, linkedLessonId: 'g5-agri-kitchen-garden' },
        ],
      },
      {
        strand: 'Hygiene & Nutrition',
        subStrands: [
          { subStrand: 'Balanced Kenyan Meals & Food Groups', minGrade: 4, maxGrade: 7, linkedLessonId: 'g5-agri-kitchen-garden' },
          { subStrand: 'Food Preservation, Kitchen Hygiene & Safety', minGrade: 5, maxGrade: 9, linkedLessonId: 'g5-agri-kitchen-garden' },
          { subStrand: 'Clothing Repair, Laundry & Stitches', minGrade: 4, maxGrade: 8, linkedLessonId: 'g5-agri-kitchen-garden' },
        ],
      },
    ],
  },
  {
    learningArea: 'Creative Arts',
    minGrade: 1,
    maxGrade: 12,
    strands: [
      {
        strand: 'Picture Making & Craft',
        subStrands: [
          { subStrand: 'Drawing, Shading, Collage & Kenyan Beadwork', minGrade: 1, maxGrade: 5, linkedLessonId: 'g5-arts-music' },
          { subStrand: 'Weaving, Pottery, Montage & Graphic Design', minGrade: 4, maxGrade: 8, linkedLessonId: 'g5-arts-music' },
          { subStrand: 'Sculpture, Printmaking & Visual Portfolio', minGrade: 8, maxGrade: 12, pathway: 'Arts & Sports Science', linkedLessonId: 'g5-arts-music' },
        ],
      },
      {
        strand: 'Performing Arts & Music',
        subStrands: [
          { subStrand: 'Action Songs, Clapping Rhythms & Percussion', minGrade: 1, maxGrade: 3, linkedLessonId: 'g5-arts-music' },
          { subStrand: 'Kenyan Folk Songs & Traditional Instruments (Nyatiti, Chivoti)', minGrade: 4, maxGrade: 8, linkedLessonId: 'g5-arts-music' },
          { subStrand: 'Music Notation, Descant Recorder & Drama', minGrade: 6, maxGrade: 12, pathway: 'Arts & Sports Science', linkedLessonId: 'g5-arts-music' },
        ],
      },
      {
        strand: 'Sports & Physical Fitness',
        subStrands: [
          { subStrand: 'Locomotor Skills, Skipping & Tag Games', minGrade: 1, maxGrade: 3, linkedLessonId: 'g5-arts-music' },
          { subStrand: 'Athletics Relays, Football, Netball & Swimming Safety', minGrade: 4, maxGrade: 9, linkedLessonId: 'g5-arts-music' },
          { subStrand: 'Sports Biomechanics, First Aid & Officiating', minGrade: 9, maxGrade: 12, pathway: 'Arts & Sports Science', linkedLessonId: 'g5-arts-music' },
        ],
      },
    ],
  },
  {
    learningArea: 'Pre-Technical Studies',
    minGrade: 7,
    maxGrade: 9,
    strands: [
      {
        strand: 'Foundations of Pre-Technical Studies',
        subStrands: [
          { subStrand: 'Workshop Safety, Fire Safety & PPE Gear', minGrade: 7, maxGrade: 9, linkedLessonId: 'g7-pretech-safety' },
          { subStrand: 'Digital Citizenship & Online Safety in Workshops', minGrade: 7, maxGrade: 9, linkedLessonId: 'g7-pretech-safety' },
        ],
      },
      {
        strand: 'Communication & Technical Drawing',
        subStrands: [
          { subStrand: 'Drawing Instruments, Line Types & Dimensioning', minGrade: 7, maxGrade: 9, linkedLessonId: 'g7-pretech-safety' },
          { subStrand: 'Plane Geometry, Orthographic & Oblique Projection', minGrade: 8, maxGrade: 9, linkedLessonId: 'g7-pretech-safety' },
        ],
      },
      {
        strand: 'Materials, Tools & Production',
        subStrands: [
          { subStrand: 'Timber, Metals, Plastics & Composite Materials', minGrade: 7, maxGrade: 9, linkedLessonId: 'g7-pretech-safety' },
          { subStrand: 'Measuring, Marking Out, Cutting & Joining Tools', minGrade: 7, maxGrade: 9, linkedLessonId: 'g7-pretech-safety' },
          { subStrand: 'Basic Electrical Circuits & Household Maintenance', minGrade: 8, maxGrade: 9, linkedLessonId: 'g10-phy-electricity' },
        ],
      },
    ],
  },
  {
    learningArea: 'Business Studies',
    minGrade: 7,
    maxGrade: 12,
    strands: [
      {
        strand: 'Business & Money Management',
        subStrands: [
          { subStrand: 'Human Wants, Scarcity & Kenyan Currency Functions', minGrade: 7, maxGrade: 8, linkedLessonId: 'g8-biz-enterprise' },
          { subStrand: 'Personal Budgeting, Mobile Money & Saving Goals', minGrade: 7, maxGrade: 10, linkedLessonId: 'g8-biz-enterprise' },
          { subStrand: 'Banking, SACCOs, Insurance & Microfinance', minGrade: 9, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g8-biz-enterprise' },
        ],
      },
      {
        strand: 'Entrepreneurship & Production',
        subStrands: [
          { subStrand: 'Entrepreneurial Qualities & Identifying Opportunities', minGrade: 7, maxGrade: 9, linkedLessonId: 'g8-biz-enterprise' },
          { subStrand: 'Business Plans, Marketing & Consumer Protection', minGrade: 8, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g8-biz-enterprise' },
        ],
      },
      {
        strand: 'Financial Accounting & Trade',
        subStrands: [
          { subStrand: 'Bookkeeping, Cash Books, Profit & Loss Accounts', minGrade: 8, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g8-biz-enterprise' },
          { subStrand: 'Balance Sheets, Taxation & International Trade', minGrade: 10, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g11-math-finance' },
        ],
      },
    ],
  },
  {
    learningArea: 'Computer Studies',
    minGrade: 7,
    maxGrade: 12,
    strands: [
      {
        strand: 'Computer Systems & Hardware',
        subStrands: [
          { subStrand: 'Input, Processing, Storage & Output Components', minGrade: 7, maxGrade: 9, linkedLessonId: 'g8-comp-algorithms' },
          { subStrand: 'Operating Systems, File Management & Troubleshooting', minGrade: 7, maxGrade: 11, linkedLessonId: 'g8-comp-algorithms' },
        ],
      },
      {
        strand: 'Data Communication & Cyber Safety',
        subStrands: [
          { subStrand: 'Computer Networks, Internet Protocols & Cloud Storage', minGrade: 8, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g8-comp-algorithms' },
          { subStrand: 'Cyber Hygiene, Encryption & Digital Ethics', minGrade: 7, maxGrade: 12, linkedLessonId: 'g8-comp-algorithms' },
        ],
      },
      {
        strand: 'Coding & Problem Solving',
        subStrands: [
          { subStrand: 'Algorithms, Flowcharts & Pseudocode Design', minGrade: 7, maxGrade: 10, linkedLessonId: 'g8-comp-algorithms' },
          { subStrand: 'Programming Constructs, Databases & Web Apps', minGrade: 9, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g8-comp-algorithms' },
        ],
      },
    ],
  },
  {
    learningArea: 'Biology',
    minGrade: 10,
    maxGrade: 12,
    pathway: 'STEM Pathway',
    strands: [
      {
        strand: 'Cell Biology & Physiology',
        subStrands: [
          { subStrand: 'Microscopy, Cell Ultrastructure & Organelles', minGrade: 10, maxGrade: 11, pathway: 'STEM Pathway', linkedLessonId: 'g10-bio-cells' },
          { subStrand: 'Diffusion, Osmosis & Active Transport', minGrade: 10, maxGrade: 11, pathway: 'STEM Pathway', linkedLessonId: 'g10-bio-cells' },
          { subStrand: 'Photosynthesis, Respiration & Enzyme Action', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-bio-cells' },
        ],
      },
      {
        strand: 'Transport, Gaseous Exchange & Homeostasis',
        subStrands: [
          { subStrand: 'Xylem, Phloem & Transpiration in Kenyan Crops', minGrade: 10, maxGrade: 11, pathway: 'STEM Pathway', linkedLessonId: 'g10-bio-cells' },
          { subStrand: 'Mammalian Heart, Immunity & Kidney Regulation', minGrade: 11, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-bio-cells' },
        ],
      },
      {
        strand: 'Ecology, Genetics & Evolution',
        subStrands: [
          { subStrand: 'Population Ecology, Savanna Biomes & Conservation', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g9-sci-ecosystems' },
          { subStrand: 'Mendelian Inheritance, DNA & Biotechnology', minGrade: 11, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g12-sci-genetics' },
        ],
      },
    ],
  },
  {
    learningArea: 'Chemistry',
    minGrade: 10,
    maxGrade: 12,
    pathway: 'STEM Pathway',
    strands: [
      {
        strand: 'Inorganic Chemistry & Atomic Structure',
        subStrands: [
          { subStrand: 'Atomic Structure, Isotopes & Chemical Bonding', minGrade: 10, maxGrade: 11, pathway: 'STEM Pathway', linkedLessonId: 'g10-chem-acids' },
          { subStrand: 'Periodic Trends (Alkali Metals, Halogens, Noble Gases)', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-chem-acids' },
        ],
      },
      {
        strand: 'Acids, Bases, Salts & Stoichiometry',
        subStrands: [
          { subStrand: 'pH Scale, Neutralization & Soil Acidity Management', minGrade: 10, maxGrade: 11, pathway: 'STEM Pathway', linkedLessonId: 'g10-chem-acids' },
          { subStrand: 'The Mole Concept, Molarity & Volumetric Analysis', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-chem-acids' },
        ],
      },
      {
        strand: 'Organic Chemistry & Electrochemistry',
        subStrands: [
          { subStrand: 'Alkanes, Alkenes, Alcohols & Polymers', minGrade: 11, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-chem-acids' },
          { subStrand: 'Redox Reactions, Electrolysis & Industrial Processes', minGrade: 11, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-chem-acids' },
        ],
      },
    ],
  },
  {
    learningArea: 'Physics',
    minGrade: 10,
    maxGrade: 12,
    pathway: 'STEM Pathway',
    strands: [
      {
        strand: 'Mechanics & Properties of Matter',
        subStrands: [
          { subStrand: 'Linear Motion, Newton’s Laws & Momentum', minGrade: 10, maxGrade: 11, pathway: 'STEM Pathway', linkedLessonId: 'g10-phy-electricity' },
          { subStrand: 'Work, Energy, Power & Fluid Pressure', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-stem-energy' },
        ],
      },
      {
        strand: 'Waves, Optics & Thermal Physics',
        subStrands: [
          { subStrand: 'Refraction, Lenses & Optical Instruments', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-phy-electricity' },
          { subStrand: 'Specific Heat Capacity & Geothermal Thermodynamics', minGrade: 10, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-stem-energy' },
        ],
      },
      {
        strand: 'Electricity, Magnetism & Modern Physics',
        subStrands: [
          { subStrand: 'Current, Voltage, Ohm’s Law & Parallel Circuits', minGrade: 10, maxGrade: 11, pathway: 'STEM Pathway', linkedLessonId: 'g10-phy-electricity' },
          { subStrand: 'Electromagnetic Induction, Transformers & Electronics', minGrade: 11, maxGrade: 12, pathway: 'STEM Pathway', linkedLessonId: 'g10-phy-electricity' },
        ],
      },
    ],
  },
  {
    learningArea: 'History & Citizenship',
    minGrade: 10,
    maxGrade: 12,
    pathway: 'Social Sciences',
    strands: [
      {
        strand: 'Kenyan History & Pan-Africanism',
        subStrands: [
          { subStrand: 'Pre-Colonial Communities, Coastal Trade & Struggle for Independence', minGrade: 10, maxGrade: 11, pathway: 'Social Sciences', linkedLessonId: 'g10-hist-constitution' },
          { subStrand: 'Post-Independence Nation Building & East African Integration', minGrade: 11, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g10-hist-constitution' },
        ],
      },
      {
        strand: 'Constitution, Devolution & Human Rights',
        subStrands: [
          { subStrand: '2010 Constitution, 47 Counties & Separation of Powers', minGrade: 10, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g10-hist-constitution' },
          { subStrand: 'Bill of Rights, Public Participation & Global Diplomacy', minGrade: 10, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g10-hist-constitution' },
        ],
      },
    ],
  },
  {
    learningArea: 'Geography',
    minGrade: 10,
    maxGrade: 12,
    pathway: 'Social Sciences',
    strands: [
      {
        strand: 'Physical Geography & Earth Processes',
        subStrands: [
          { subStrand: 'Faulting, Volcanicity & Rift Valley Formation', minGrade: 10, maxGrade: 11, pathway: 'Social Sciences', linkedLessonId: 'g10-geo-rift' },
          { subStrand: 'Weathering, River Drainage & Relief Rainfall', minGrade: 10, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g10-geo-rift' },
        ],
      },
      {
        strand: 'Human, Economic & Environmental Geography',
        subStrands: [
          { subStrand: 'Statistical Mapwork, GIS & Remote Sensing', minGrade: 10, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g10-geo-rift' },
          { subStrand: 'Sustainable Agriculture, Energy & Urban Planning', minGrade: 11, maxGrade: 12, pathway: 'Social Sciences', linkedLessonId: 'g6-sst-resources' },
        ],
      },
    ],
  },
];

export const ALL_CBC_LEARNING_AREAS: LearningArea[] = CBC_CURRICULUM_TREE.map(
  (s) => s.learningArea
);

export function getSubjectsForGrade(grade: number): LearningArea[] {
  return CBC_CURRICULUM_TREE.filter(
    (subject) => grade >= subject.minGrade && grade <= subject.maxGrade
  ).map((s) => s.learningArea);
}

export function getStrandsForSubject(
  area: LearningArea,
  gradeFilter?: number
): { strand: string; subStrands: { subStrand: string; levelHint: string; minGrade: number; maxGrade: number; linkedLessonId: string }[] }[] {
  const subject = CBC_CURRICULUM_TREE.find((s) => s.learningArea === area);
  if (!subject) return [];
  return subject.strands
    .map((strandNode) => {
      const matchingSubs = strandNode.subStrands
        .filter((sub) =>
          gradeFilter === undefined
            ? true
            : gradeFilter >= sub.minGrade && gradeFilter <= sub.maxGrade
        )
        .map((sub) => ({
          subStrand: sub.subStrand,
          levelHint:
            sub.minGrade === sub.maxGrade
              ? `Grade ${sub.minGrade}`
              : `Grades ${sub.minGrade}–${sub.maxGrade}`,
          minGrade: sub.minGrade,
          maxGrade: sub.maxGrade,
          linkedLessonId: sub.linkedLessonId,
        }));
      return {
        strand: strandNode.strand,
        subStrands: matchingSubs,
      };
    })
    .filter((s) => s.subStrands.length > 0);
}

export function buildCatalogForGrade(grade: number): CbcCatalogEntry[] {
  const levelGroup = levelGroupForGrade(grade);
  const entries: CbcCatalogEntry[] = [];
  CBC_CURRICULUM_TREE.forEach((subject) => {
    if (grade < subject.minGrade || grade > subject.maxGrade) return;
    subject.strands.forEach((strandNode) => {
      strandNode.subStrands.forEach((sub) => {
        if (grade >= sub.minGrade && grade <= sub.maxGrade) {
          entries.push({
            grade,
            levelGroup,
            pathway: sub.pathway ?? subject.pathway,
            learningArea: subject.learningArea,
            strand: strandNode.strand,
            subStrand: sub.subStrand,
            linkedLessonId: sub.linkedLessonId,
          });
        }
      });
    });
  });
  return entries;
}

export function buildFullDirectoryCatalog(): CbcCatalogEntry[] {
  const all: CbcCatalogEntry[] = [];
  for (let grade = 1; grade <= 12; grade++) {
    all.push(...buildCatalogForGrade(grade));
  }
  return all;
}

export function getCurriculumStats() {
  let strandCount = 0;
  let subStrandCount = 0;
  CBC_CURRICULUM_TREE.forEach((subject) => {
    strandCount += subject.strands.length;
    subject.strands.forEach((st) => {
      subStrandCount += st.subStrands.length;
    });
  });
  return {
    subjectCount: CBC_CURRICULUM_TREE.length,
    strandCount,
    subStrandCount,
  };
}
