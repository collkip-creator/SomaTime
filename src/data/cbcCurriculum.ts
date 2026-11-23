import type {
  CBCLevelGroup,
  CBCLesson,
  CBCRubric,
  CbcCatalogEntry,
  ChildProfile,
  ClassStudentRecord,
  LearningArea,
  TeacherAssignment,
} from '../types/somatime';
import { CBC_EXTRA_LESSONS } from './cbcExtraLessons';
import {
  ALL_CBC_LEARNING_AREAS,
  CBC_CURRICULUM_TREE,
  buildCatalogForGrade,
  buildFullDirectoryCatalog,
  getCurriculumStats,
  getStrandsForSubject,
  getSubjectsForGrade,
} from './cbcFullCatalog';
import {
  buildGradeScaledLesson,
  getQuestionCountForGrade,
} from './cbcQuestionBank';

export {
  CBC_CURRICULUM_TREE,
  buildCatalogForGrade,
  getCurriculumStats,
  getQuestionCountForGrade,
  getStrandsForSubject,
  getSubjectsForGrade,
};

export const PARENT_PIN = '1234';

export const ASSETS = {
  aminaAvatar: '/src/assets/images/avatar_amina_1794996018825.jpg',
  brianAvatar: '/src/assets/images/avatar_brian_1794996019103.jpg',
  wallpaper: '/src/assets/images/kenya_savanna_wallpaper_1794996019501.jpg',
  learningBanner: '/src/assets/images/cbc_learning_banner_1794996019764.jpg',
};

export const ALL_LEARNING_AREAS: LearningArea[] = ALL_CBC_LEARNING_AREAS;


export const CBC_RUBRIC_META: Record<
  CBCRubric,
  {
    code: CBCRubric;
    label: string;
    shortLabel: string;
    description: string;
    badgeBg: string;
    badgeText: string;
    borderColor: string;
  }
> = {
  EE: {
    code: 'EE',
    label: 'Exceeding Expectations',
    shortLabel: 'Exceeding',
    description: 'Consistently demonstrates mastery and applies CBC concepts independently.',
    badgeBg: 'bg-emerald-50',
    badgeText: 'text-emerald-800',
    borderColor: 'border-emerald-200',
  },
  ME: {
    code: 'ME',
    label: 'Meeting Expectations',
    shortLabel: 'Meeting',
    description: 'Accurately completes strand competencies at grade standard.',
    badgeBg: 'bg-teal-50',
    badgeText: 'text-teal-800',
    borderColor: 'border-teal-200',
  },
  AE: {
    code: 'AE',
    label: 'Approaching Expectations',
    shortLabel: 'Approaching',
    description: 'Understands core ideas with occasional bilingual or guided support.',
    badgeBg: 'bg-amber-50',
    badgeText: 'text-amber-800',
    borderColor: 'border-amber-200',
  },
  BE: {
    code: 'BE',
    label: 'Below Expectations',
    shortLabel: 'Below',
    description: 'Needs focused revision on foundational sub-strands before advancing.',
    badgeBg: 'bg-rose-50',
    badgeText: 'text-rose-800',
    borderColor: 'border-rose-200',
  },
};

export function getLevelGroupForGrade(grade: number): CBCLevelGroup {
  if (grade <= 3) return 'Lower Primary';
  if (grade <= 6) return 'Upper Primary';
  if (grade <= 9) return 'Junior School';
  return 'Senior School';
}

export function getSeniorPathwayHint(grade: number): string | undefined {
  if (grade < 10) return undefined;
  return 'STEM, Social Sciences & Arts Pathways';
}

export const CBC_LESSONS: CBCLesson[] = [
  {
    id: 'g4-math-fractions',
    title: 'Fractions & Sharing KES',
    swahiliTitle: 'Fractions & Sharing KES',
    grade: 4,
    levelGroup: 'Upper Primary',
    learningArea: 'Mathematics',
    strand: 'Numbers',
    subStrand: 'Fractions in Daily Life',
    durationMins: 5,
    summary:
      'Represent unit and non-unit fractions using chapati shares and Kenyan Shillings (KES) word problems.',
    questions: [
      {
        id: 'g4-m-1',
        type: 'multiple_choice',
        prompt:
          'Amina shares 1 whole chapati equally among 4 friends at lunch in Westlands. What fraction of the chapati does each friend receive?',
        bilingualHint: 'Hint: 1 part out of 4 equal parts is written with 1 on top and 4 on the bottom (one-quarter).',
        visualFraction: {
          numerator: 1,
          denominator: 4,
          label: '1 out of 4 equal parts',
        },
        options: ['1/2', '1/3', '1/4', '3/4'],
        correctAnswer: '1/4',
        explanation:
          'Well done! Dividing 1 whole into 4 equal parts gives each person 1/4 (one-quarter).',
      },
      {
        id: 'g4-m-2',
        type: 'matching',
        prompt: 'Match each fraction to its equivalent Kenyan Shillings (KES) share out of KES 100.',
        bilingualHint: 'Hint: Half of KES 100 is KES 50, and a quarter of KES 100 is KES 25.',
        pairs: [
          { id: 'p1', left: '1/2 of KES 100', right: 'KES 50' },
          { id: 'p2', left: '1/4 of KES 100', right: 'KES 25' },
          { id: 'p3', left: '3/4 of KES 100', right: 'KES 75' },
        ],
        explanation:
          'Great work! 1/2 = KES 50, 1/4 = KES 25, and 3/4 = KES 75.',
      },
      {
        id: 'g4-m-3',
        type: 'fill_blank',
        prompt:
          'A vendor had 5/8 of a sack of kale (sukuma wiki) and sold 2/8 in the morning. What fraction remains?',
        bilingualHint: 'Hint: Subtract only the top numbers (5 - 2) because the denominator (8) is the same.',
        blankPrefix: 'Remaining fraction:',
        blankSuffix: 'of the sack',
        correctAnswer: '3/8',
        acceptableAnswers: ['3/8', '3 / 8'],
        explanation:
          'Correct! When denominators are the same, subtract the numerators: 5/8 - 2/8 = 3/8.',
      },
    ],
  },
  {
    id: 'g7-sci-apparatus',
    title: 'Laboratory Apparatus & Safety',
    swahiliTitle: 'Laboratory Apparatus & Safety',
    grade: 7,
    levelGroup: 'Junior School',
    learningArea: 'Integrated Science',
    strand: 'Scientific Investigation',
    subStrand: 'Laboratory Apparatus & Instruments',
    durationMins: 5,
    summary:
      'Identify standard Junior School science apparatus, accurate volume measurement, and lab safety rules.',
    questions: [
      {
        id: 'g7-s-1',
        type: 'multiple_choice',
        prompt:
          'During a Junior School practical in Kisumu, Brian needs to measure exactly 45 ml of rainwater. Which apparatus gives the most accurate volume reading?',
        bilingualHint: 'Hint: Look for the tall graduated glass tube marked in millilitres (ml).',
        options: [
          'Measuring cylinder',
          'Bunsen burner',
          'Evaporating dish',
          'Test tube rack',
        ],
        correctAnswer: 'Measuring cylinder',
        explanation:
          'Well done! A graduated measuring cylinder is calibrated specifically to measure liquid volumes accurately.',
      },
      {
        id: 'g7-s-2',
        type: 'matching',
        prompt: 'Match each Integrated Science apparatus to its primary function in the lab.',
        bilingualHint: 'Hint: A Bunsen burner is used for heating; a thermometer checks temperature.',
        pairs: [
          { id: 's1', left: 'Conical Flask', right: 'Swirling liquids safely without spilling' },
          { id: 's2', left: 'Thermometer', right: 'Measuring temperature in °C' },
          { id: 's3', left: 'Bunsen Burner', right: 'Controlled heating with a blue flame' },
        ],
        explanation:
          'Great job! Knowing apparatus functions keeps every Junior School science practical safe and precise.',
      },
      {
        id: 'g7-s-3',
        type: 'fill_blank',
        prompt:
          'To measure temperature in a Kenyan school laboratory, we read the meniscus on a thermometer in degrees ________.',
        bilingualHint: 'Hint: The metric temperature scale starting with the letter C.',
        blankPrefix: 'Unit: degrees',
        blankSuffix: '(°C)',
        correctAnswer: 'Celsius',
        acceptableAnswers: ['celsius', 'centigrade', 'c'],
        explanation:
          'Accurate! Temperature in CBC Integrated Science labs is recorded in degrees Celsius (°C).',
      },
    ],
  },
  {
    id: 'g2-kisw-msamiati',
    title: 'Msamiati wa Nyumbani na Shuleni',
    swahiliTitle: 'Vocabulary: Home & School',
    grade: 2,
    levelGroup: 'Lower Primary',
    learningArea: 'Kiswahili',
    strand: 'Kusikiliza na Kuzungumza',
    subStrand: 'Msamiati wa Nyumbani',
    durationMins: 5,
    summary:
      'Jifunze maneno ya Kiswahili kuhusu vitu vya nyumbani na darasani kwa picha na mifano rahisi.',
    questions: [
      {
        id: 'g2-k-1',
        type: 'multiple_choice',
        prompt: 'Katika sentensi: "Kamau anasoma ______ kipya maktabani", ni neno lipi linafaa?',
        bilingualHint: 'Hint (English): Something you read at the library (book).',
        options: ['kitabu', 'kikombe', 'kiatu', 'kijiko'],
        correctAnswer: 'kitabu',
        explanation: 'Hongera! "Kitabu" maana yake ni book, na kinasomwa maktabani.',
      },
      {
        id: 'g2-k-2',
        type: 'matching',
        prompt: 'Oanisha maneno ya Kiswahili na maana zake kwa Kiingereza (Match Kiswahili to English).',
        bilingualHint: 'Hint: Mwalimu teaches in class; Shule is where learners go.',
        pairs: [
          { id: 'k1', left: 'Mwalimu', right: 'Teacher' },
          { id: 'k2', left: 'Kikombe', right: 'Cup' },
          { id: 'k3', left: 'Shule', right: 'School' },
        ],
        explanation: 'Safi sana! Mwalimu = Teacher, Kikombe = Cup, Shule = School.',
      },
      {
        id: 'g2-k-3',
        type: 'fill_blank',
        prompt: 'Kamilisha salamu za asubuhi: "Shikamoo!" Jibu lake ni "________!"',
        bilingualHint: 'Hint: Respectful reply to Shikamoo (starts with M).',
        blankPrefix: 'Jibu:',
        blankSuffix: '!',
        correctAnswer: 'Marahaba',
        acceptableAnswers: ['marahaba'],
        explanation: 'Vizuri mno! Wakubwa hujibu "Marahaba" wanaposalimiwa "Shikamoo".',
      },
    ],
  },
  {
    id: 'g6-sst-resources',
    title: 'Kenyan Agriculture & Trade Corridors',
    swahiliTitle: 'Kenyan Agriculture & Trade Corridors',
    grade: 6,
    levelGroup: 'Upper Primary',
    learningArea: 'Social Studies',
    strand: 'Resources & Economic Activities',
    subStrand: 'Cash Crops & Port Trade',
    durationMins: 5,
    summary:
      'Explore major Kenyan agricultural zones (Kericho tea, Mwea rice) and East African trade via Mombasa Port.',
    questions: [
      {
        id: 'g6-ss-1',
        type: 'multiple_choice',
        prompt:
          'Which Kenyan county in the Rift Valley highlands is most famous for large-scale tea farming due to cool temperatures and high rainfall?',
        bilingualHint: 'Hint: This highland county in the Rift Valley is home to Kenya’s largest tea estates.',
        options: ['Kericho', 'Turkana', 'Garissa', 'Kilifi'],
        correctAnswer: 'Kericho',
        explanation:
          'Correct! Kericho’s highland volcanic soils and reliable rainfall make it Kenya’s leading tea-growing region.',
      },
      {
        id: 'g6-ss-2',
        type: 'matching',
        prompt: 'Match each Kenyan economic centre to its primary resource or trade role.',
        bilingualHint: 'Hint: Mombasa is on the Indian Ocean coast; Mwea is an irrigation scheme.',
        pairs: [
          { id: 'ss1', left: 'Port of Mombasa', right: 'International sea imports & exports' },
          { id: 'ss2', left: 'Mwea Scheme', right: 'Irrigated rice farming' },
          { id: 'ss3', left: 'Lake Victoria', right: 'Inland freshwater fishing (Tilapia)' },
        ],
        explanation:
          'Excellent! Kenya’s diverse regions support agriculture, inland fisheries, and regional trade.',
      },
      {
        id: 'g6-ss-3',
        type: 'fill_blank',
        prompt:
          'The modern railway connecting Mombasa Port to Nairobi and Naivasha for faster cargo trade is called the ________.',
        bilingualHint: 'Hint: 3-letter abbreviation for Standard Gauge Railway.',
        blankPrefix: 'Abbreviation:',
        blankSuffix: '(Standard Gauge Railway)',
        correctAnswer: 'SGR',
        acceptableAnswers: ['sgr', 'standard gauge railway'],
        explanation:
          'Great job! The SGR (Madaraka Express / freight line) connects Mombasa to the interior.',
      },
    ],
  },
  {
    id: 'g8-math-algebra',
    title: 'Linear Equations & M-Pesa Budgeting',
    swahiliTitle: 'Linear Equations & M-Pesa Budgeting',
    grade: 8,
    levelGroup: 'Junior School',
    learningArea: 'Mathematics',
    strand: 'Algebra',
    subStrand: 'Linear Equations in One Unknown',
    durationMins: 5,
    summary:
      'Solve single-variable linear equations grounded in Kenyan mobile money budgeting and school supply costs.',
    questions: [
      {
        id: 'g8-m-1',
        type: 'multiple_choice',
        prompt:
          'Brian buys 3 geometric sets at KES x each and pays a KES 50 transport fare. His total spend is KES 500. Which equation models this?',
        bilingualHint: 'Hint: Multiply 3 items by price x, then add KES 50 to equal 500.',
        options: ['3x + 50 = 500', '50x + 3 = 500', '3x - 50 = 500', 'x + 53 = 500'],
        correctAnswer: '3x + 50 = 500',
        explanation: 'Spot on! Three items at KES x give 3x, plus KES 50 fare equals KES 500.',
      },
      {
        id: 'g8-m-2',
        type: 'matching',
        prompt: 'Match each linear equation to the correct value of x.',
        bilingualHint: 'Hint: Isolate x on one side by subtracting or dividing both sides equally.',
        pairs: [
          { id: 'a1', left: '2x + 10 = 30', right: 'x = 10' },
          { id: 'a2', left: '5x = 125', right: 'x = 25' },
          { id: 'a3', left: 'x - 40 = 60', right: 'x = 100' },
        ],
        explanation: 'Well done! You balanced all three linear equations accurately.',
      },
      {
        id: 'g8-m-3',
        type: 'fill_blank',
        prompt: 'From 3x + 50 = 500, what is the cost (x) of 1 geometric set in KES?',
        bilingualHint: 'Hint: Subtract 50 from 500 to get 450, then divide 450 by 3.',
        blankPrefix: 'x = KES',
        blankSuffix: '',
        correctAnswer: '150',
        acceptableAnswers: ['150', 'kes 150'],
        explanation: '3x = 450, so x = 450 ÷ 3 = KES 150.',
      },
    ],
  },
  {
    id: 'g1-eng-reading',
    title: 'Everyday Greetings & Phonics',
    swahiliTitle: 'Everyday Greetings & Phonics',
    grade: 1,
    levelGroup: 'Lower Primary',
    learningArea: 'English',
    strand: 'Listening & Speaking',
    subStrand: 'Polite Expressions',
    durationMins: 5,
    summary: 'Use polite greetings and match everyday classroom objects in Grade 1 English.',
    questions: [
      {
        id: 'g1-e-1',
        type: 'multiple_choice',
        prompt: 'When your teacher hands you an exercise book in class, what is the polite thing to say?',
        bilingualHint: 'Hint: Two words used to show gratitude.',
        options: ['Thank you', 'Goodbye', 'Excuse me', 'Good night'],
        correctAnswer: 'Thank you',
        explanation: 'Wonderful! Saying "Thank you" shows respect and kindness.',
      },
      {
        id: 'g1-e-2',
        type: 'matching',
        prompt: 'Match each greeting to the right time of day.',
        bilingualHint: 'Hint: Morning is before noon; afternoon is after lunch; evening is near sunset.',
        pairs: [
          { id: 'e1', left: '7:30 AM (Assembly)', right: 'Good morning' },
          { id: 'e2', left: '1:15 PM (Lunch)', right: 'Good afternoon' },
          { id: 'e3', left: '6:30 PM (Home)', right: 'Good evening' },
        ],
        explanation: 'Great match! You chose the right greeting for every part of the day.',
      },
      {
        id: 'g1-e-3',
        type: 'fill_blank',
        prompt: 'Complete the polite request: "________ I borrow your ruler?"',
        bilingualHint: 'Hint: A 3-letter polite word starting with M used to ask permission.',
        blankPrefix: '',
        blankSuffix: 'I borrow your ruler?',
        correctAnswer: 'May',
        acceptableAnswers: ['may', 'can'],
        explanation: 'Polite! "May I borrow your ruler?" is courteous classroom English.',
      },
    ],
  },
  {
    id: 'g5-agri-kitchen-garden',
    title: 'Urban Kitchen Gardens & Composting',
    swahiliTitle: 'Urban Kitchen Gardens & Composting',
    grade: 5,
    levelGroup: 'Upper Primary',
    learningArea: 'Agriculture & Nutrition',
    strand: 'Conservation of Resources',
    subStrand: 'Organic Compost & Sack Gardens',
    durationMins: 5,
    summary:
      'Design water-smart vertical sack gardens and turn kitchen peelings into organic compost.',
    questions: [
      {
        id: 'g5-ag-1',
        type: 'multiple_choice',
        prompt:
          'Why are vertical sack gardens popular for growing kale and spinach in urban Nairobi homes?',
        bilingualHint: 'Hint: Sacks stack soil upwards and keep moisture close to plant roots.',
        options: [
          'They save space and conserve water',
          'They require heavy tractors',
          'They block all sunlight',
          'They only grow maize',
        ],
        correctAnswer: 'They save space and conserve water',
        explanation:
          'Well done! Vertical sack gardens let families grow fresh vegetables even in small compounds.',
      },
      {
        id: 'g5-ag-2',
        type: 'matching',
        prompt: 'Match each material to whether it belongs in a compost pit or recycling bin.',
        bilingualHint: 'Hint: Food peelings and dry leaves rot into manure; plastic does not decompose.',
        pairs: [
          { id: 'ag1', left: 'Banana & potato peelings', right: 'Compost manure pit' },
          { id: 'ag2', left: 'Plastic soda bottle', right: 'Recycling bin' },
          { id: 'ag3', left: 'Dry maize stalks & leaves', right: 'Compost brown layer' },
        ],
        explanation: 'Well sorted! Organic peelings and dry leaves create nutrient-rich compost.',
      },
      {
        id: 'g5-ag-3',
        type: 'fill_blank',
        prompt:
          'Covering soil around crops with dry grass to reduce evaporation is called ________.',
        bilingualHint: 'Hint: An agricultural conservation practice starting with the letter M.',
        blankPrefix: 'Practice:',
        blankSuffix: '',
        correctAnswer: 'Mulching',
        acceptableAnswers: ['mulching', 'mulch'],
        explanation: 'Correct! Mulching keeps soil moist and suppresses weeds naturally.',
      },
    ],
  },
  {
    id: 'g10-stem-energy',
    title: 'Geothermal Energy in Olkaria',
    swahiliTitle: 'Geothermal Energy in Olkaria',
    grade: 10,
    levelGroup: 'Senior School',
    pathway: 'STEM Pathway',
    learningArea: 'Integrated Science',
    strand: 'Energy & Sustainability',
    subStrand: 'Renewable Power Systems',
    durationMins: 5,
    summary:
      'Analyze how Kenya’s Rift Valley geothermal steam plants convert underground heat into clean grid electricity.',
    questions: [
      {
        id: 'g10-st-1',
        type: 'multiple_choice',
        prompt:
          'Which energy transformation sequence occurs inside an Olkaria geothermal power station?',
        bilingualHint: 'Hint: Underground heat boils water into steam, which spins moving blades to drive a generator.',
        options: [
          'Thermal → Mechanical → Electrical',
          'Chemical → Light → Sound',
          'Solar → Nuclear → Thermal',
          'Elastic → Potential → Chemical',
        ],
        correctAnswer: 'Thermal → Mechanical → Electrical',
        explanation:
          'Accurate! Underground thermal steam turns turbines (mechanical), driving generators (electrical).',
      },
      {
        id: 'g10-st-2',
        type: 'matching',
        prompt: 'Match each Kenyan clean energy source to its primary location.',
        bilingualHint: 'Hint: Olkaria is near Naivasha; Seven Forks dams sit along the Tana River.',
        pairs: [
          { id: 'st1', left: 'Geothermal Steam', right: 'Olkaria, Naivasha' },
          { id: 'st2', left: 'Wind Turbines', right: 'Lake Turkana Wind Power' },
          { id: 'st3', left: 'Hydroelectric Dams', right: 'Seven Forks (Tana River)' },
        ],
        explanation: 'Excellent Senior School STEM mastery of Kenya’s renewable energy mix!',
      },
      {
        id: 'g10-st-3',
        type: 'fill_blank',
        prompt:
          'The rotating machine pushed by high-pressure steam inside a power plant is called a ________.',
        bilingualHint: 'Hint: Starts with the letter T.',
        blankPrefix: 'Component:',
        blankSuffix: '',
        correctAnswer: 'Turbine',
        acceptableAnswers: ['turbine', 'steam turbine'],
        explanation: 'Correct! High-pressure steam spins the turbine blades connected to the alternator.',
      },
    ],
  },
  {
    id: 'g1-math-coins',
    title: 'Counting & Kenyan Shilling Coins',
    swahiliTitle: 'Counting & Kenyan Shilling Coins',
    grade: 1,
    levelGroup: 'Lower Primary',
    learningArea: 'Mathematics',
    strand: 'Numbers',
    subStrand: 'Whole Numbers & KES Coins',
    durationMins: 5,
    summary: 'Count objects up to 20 and recognize KES 1, KES 5, KES 10, and KES 20 coins.',
    questions: [
      {
        id: 'g1-m-1',
        type: 'multiple_choice',
        prompt: 'Amina has two KES 5 coins in her pocket. How much money does she have altogether?',
        bilingualHint: 'Hint: Add 5 + 5 together.',
        options: ['KES 10', 'KES 5', 'KES 15', 'KES 20'],
        correctAnswer: 'KES 10',
        explanation: 'Well done! KES 5 + KES 5 = KES 10.',
      },
      {
        id: 'g1-m-2',
        type: 'matching',
        prompt: 'Match each counting group to the right total.',
        bilingualHint: 'Hint: Count carefully in ones and tens.',
        pairs: [
          { id: 'g1m1', left: '4 mangoes + 3 mangoes', right: '7 mangoes' },
          { id: 'g1m2', left: '10 pencils + 2 pencils', right: '12 pencils' },
          { id: 'g1m3', left: '9 books - 4 books', right: '5 books' },
        ],
        explanation: 'Awesome counting! You solved all three sums accurately.',
      },
      {
        id: 'g1-m-3',
        type: 'fill_blank',
        prompt: 'Count forward by 1: 14, 15, 16, ________.',
        bilingualHint: 'Hint: The number right after 16.',
        blankPrefix: 'Next number:',
        blankSuffix: '',
        correctAnswer: '17',
        acceptableAnswers: ['17', 'seventeen'],
        explanation: 'Correct! 17 comes right after 16.',
      },
    ],
  },
  {
    id: 'g3-math-measurement',
    title: 'Kenyan Time, Mass & Metres',
    swahiliTitle: 'Kenyan Time, Mass & Metres',
    grade: 3,
    levelGroup: 'Lower Primary',
    learningArea: 'Mathematics',
    strand: 'Measurement',
    subStrand: 'Length, Mass & Clock Time',
    durationMins: 5,
    summary: 'Measure classroom lengths in metres, market produce in kilograms, and read half-past times.',
    questions: [
      {
        id: 'g3-m-1',
        type: 'multiple_choice',
        prompt: 'A shopkeeper weighs 2 kg of maize flour and 3 kg of beans in Nakuru. What is the total mass?',
        bilingualHint: 'Hint: Add 2 kg and 3 kg.',
        options: ['5 kg', '6 kg', '1 kg', '4 kg'],
        correctAnswer: '5 kg',
        explanation: 'Great! 2 kg + 3 kg = 5 kg.',
      },
      {
        id: 'g3-m-2',
        type: 'matching',
        prompt: 'Match each item to its best Kenyan CBC measurement unit.',
        bilingualHint: 'Hint: Sugar is weighed in kg; milk is measured in litres; cloth in metres.',
        pairs: [
          { id: 'g3m1', left: 'Packet of sugar', right: 'Kilograms (kg)' },
          { id: 'g3m2', left: 'Bottle of milk', right: 'Litres (L)' },
          { id: 'g3m3', left: 'Classroom wall', right: 'Metres (m)' },
        ],
        explanation: 'Spot on! You picked the right metric unit for mass, capacity, and length.',
      },
      {
        id: 'g3-m-3',
        type: 'fill_blank',
        prompt: 'How many minutes are in 1 full hour?',
        bilingualHint: 'Hint: A clock face has 60 small minute steps.',
        blankPrefix: '1 hour =',
        blankSuffix: 'minutes',
        correctAnswer: '60',
        acceptableAnswers: ['60', 'sixty'],
        explanation: 'Correct! There are 60 minutes in 1 hour.',
      },
    ],
  },
  {
    id: 'g5-math-decimals',
    title: 'Decimals & Market Change (KES)',
    swahiliTitle: 'Decimals & Market Change (KES)',
    grade: 5,
    levelGroup: 'Upper Primary',
    learningArea: 'Mathematics',
    strand: 'Numbers',
    subStrand: 'Decimals in Shopping',
    durationMins: 5,
    summary: 'Add and subtract decimals up to 2 decimal places using Kenyan retail prices.',
    questions: [
      {
        id: 'g5-m-1',
        type: 'multiple_choice',
        prompt: 'A loaf of bread costs KES 65.50 and milk costs KES 60.00. What is the total bill?',
        bilingualHint: 'Hint: Add 65 + 60, then keep .50.',
        options: ['KES 125.50', 'KES 120.50', 'KES 135.50', 'KES 115.50'],
        correctAnswer: 'KES 125.50',
        explanation: 'Correct! 65.50 + 60.00 = KES 125.50.',
      },
      {
        id: 'g5-m-2',
        type: 'matching',
        prompt: 'Match each fraction to its decimal equivalent.',
        bilingualHint: 'Hint: Divide the top number by 10 or 100.',
        pairs: [
          { id: 'g5m1', left: '1/2', right: '0.5' },
          { id: 'g5m2', left: '1/4', right: '0.25' },
          { id: 'g5m3', left: '3/10', right: '0.3' },
        ],
        explanation: 'Great work converting fractions to decimals!',
      },
      {
        id: 'g5-m-3',
        type: 'fill_blank',
        prompt: 'Evaluate: 4.75 + 1.25 = ________.',
        bilingualHint: 'Hint: 0.75 + 0.25 makes 1 whole.',
        blankPrefix: 'Answer:',
        blankSuffix: '',
        correctAnswer: '6',
        acceptableAnswers: ['6', '6.0', '6.00'],
        explanation: 'Well done! 4.75 + 1.25 = 6.00.',
      },
    ],
  },
  {
    id: 'g6-math-percentages',
    title: 'Percentages & Farm Harvests',
    swahiliTitle: 'Percentages & Farm Harvests',
    grade: 6,
    levelGroup: 'Upper Primary',
    learningArea: 'Mathematics',
    strand: 'Numbers',
    subStrand: 'Percentages & Ratios',
    durationMins: 5,
    summary: 'Convert fractions to percentages and calculate discounts at Kenyan shops.',
    questions: [
      {
        id: 'g6-m-1',
        type: 'multiple_choice',
        prompt: 'A Grade 6 learner scores 18 out of 20 in a Mathematics quiz. What percentage is that?',
        bilingualHint: 'Hint: Multiply 18/20 by 100.',
        options: ['90%', '80%', '85%', '95%'],
        correctAnswer: '90%',
        explanation: 'Excellent! (18 ÷ 20) × 100 = 90%.',
      },
      {
        id: 'g6-m-2',
        type: 'matching',
        prompt: 'Match each percentage to its fraction.',
        bilingualHint: 'Hint: Percent means out of 100.',
        pairs: [
          { id: 'g6m1', left: '50%', right: '1/2' },
          { id: 'g6m2', left: '25%', right: '1/4' },
          { id: 'g6m3', left: '75%', right: '3/4' },
        ],
        explanation: 'Well done! Understanding percent-fraction links makes shopping math fast.',
      },
      {
        id: 'g6-m-3',
        type: 'fill_blank',
        prompt: 'A school bag priced at KES 1,000 has a 10% discount. How many shillings do you save?',
        bilingualHint: 'Hint: 10% is 1/10 of 1,000.',
        blankPrefix: 'Savings: KES',
        blankSuffix: '',
        correctAnswer: '100',
        acceptableAnswers: ['100', 'kes 100'],
        explanation: 'Spot on! 10% of KES 1,000 = KES 100 saved.',
      },
    ],
  },
  {
    id: 'g9-sci-ecosystems',
    title: 'Kenyan Ecosystems & Conservation',
    swahiliTitle: 'Kenyan Ecosystems & Conservation',
    grade: 9,
    levelGroup: 'Junior School',
    learningArea: 'Integrated Science',
    strand: 'Living Things & Environment',
    subStrand: 'Food Webs & Biodiversity',
    durationMins: 5,
    summary: 'Analyze savanna food chains, trophic levels, and wetland conservation in Kenya.',
    questions: [
      {
        id: 'g9-s-1',
        type: 'multiple_choice',
        prompt: 'In a Maasai Mara savanna food chain (Acacia → Giraffe → Lion), what ecological role does the Acacia tree play?',
        bilingualHint: 'Hint: Plants make food from sunlight using photosynthesis.',
        options: ['Producer', 'Primary consumer', 'Secondary consumer', 'Decomposer'],
        correctAnswer: 'Producer',
        explanation: 'Correct! Green plants are producers because they convert sunlight into chemical energy.',
      },
      {
        id: 'g9-s-2',
        type: 'matching',
        prompt: 'Match each Kenyan ecosystem organism to its trophic role.',
        bilingualHint: 'Hint: Herbivores eat plants; fungi recycle dead matter.',
        pairs: [
          { id: 'g9s1', left: 'Savanna Grass', right: 'Producer' },
          { id: 'g9s2', left: 'Zebra', right: 'Primary Consumer' },
          { id: 'g9s3', left: 'Soil Bacteria & Fungi', right: 'Decomposer' },
        ],
        explanation: 'Great job! Every trophic level keeps the Kenyan savanna ecosystem balanced.',
      },
      {
        id: 'g9-s-3',
        type: 'fill_blank',
        prompt: 'The process by which green leaves use sunlight, water, and carbon dioxide to make glucose is called ________.',
        bilingualHint: 'Hint: Starts with photo-.',
        blankPrefix: 'Process:',
        blankSuffix: '',
        correctAnswer: 'Photosynthesis',
        acceptableAnswers: ['photosynthesis'],
        explanation: 'Accurate! Photosynthesis produces glucose and oxygen.',
      },
    ],
  },
  {
    id: 'g11-math-finance',
    title: 'SACCO Savings & Compound Interest',
    swahiliTitle: 'SACCO Savings & Compound Interest',
    grade: 11,
    levelGroup: 'Senior School',
    pathway: 'STEM & Social Sciences',
    learningArea: 'Mathematics',
    strand: 'Commercial Arithmetic',
    subStrand: 'Compound Interest & Appreciation',
    durationMins: 5,
    summary: 'Model Kenyan SACCO savings growth using compound interest formulas.',
    questions: [
      {
        id: 'g11-m-1',
        type: 'multiple_choice',
        prompt: 'KES 10,000 is saved at 10% p.a. compound interest for 2 years. What is the amount after 2 years?',
        bilingualHint: 'Hint: Year 1 = 11,000; Year 2 adds 10% of 11,000.',
        options: ['KES 12,100', 'KES 12,000', 'KES 11,100', 'KES 12,500'],
        correctAnswer: 'KES 12,100',
        explanation: 'Correct! 10,000 × (1.1)² = KES 12,100.',
      },
      {
        id: 'g11-m-2',
        type: 'matching',
        prompt: 'Match each financial term to its Kenyan CBC meaning.',
        bilingualHint: 'Hint: Principal is starting cash; Appreciation means rising value.',
        pairs: [
          { id: 'g11m1', left: 'Principal (P)', right: 'Initial amount invested or borrowed' },
          { id: 'g11m2', left: 'Appreciation', right: 'Increase in asset value over time' },
          { id: 'g11m3', left: 'Depreciation', right: 'Decrease in asset value over time' },
        ],
        explanation: 'Excellent mastery of Senior School commercial arithmetic!',
      },
      {
        id: 'g11-m-3',
        type: 'fill_blank',
        prompt: 'How much compound interest alone was earned on KES 10,000 if the final amount is KES 12,100?',
        bilingualHint: 'Hint: Interest = Amount - Principal.',
        blankPrefix: 'Interest: KES',
        blankSuffix: '',
        correctAnswer: '2100',
        acceptableAnswers: ['2100', '2,100', 'kes 2100'],
        explanation: 'Spot on! 12,100 - 10,000 = KES 2,100.',
      },
    ],
  },
  {
    id: 'g12-sci-genetics',
    title: 'Biotechnology & Drought-Tolerant Crops',
    swahiliTitle: 'Biotechnology & Drought-Tolerant Crops',
    grade: 12,
    levelGroup: 'Senior School',
    pathway: 'STEM Pathway',
    learningArea: 'Integrated Science',
    strand: 'Genetics & Biotechnology',
    subStrand: 'Selective Breeding & Food Security',
    durationMins: 5,
    summary: 'Examine how KALRO research improves maize and sorghum resilience across Kenyan climates.',
    questions: [
      {
        id: 'g12-s-1',
        type: 'multiple_choice',
        prompt: 'Why do Kenyan agricultural researchers breed drought-tolerant maize varieties for semi-arid counties?',
        bilingualHint: 'Hint: Think about reliable harvests when rainfall is low.',
        options: [
          'To improve food security during erratic rainfall',
          'To increase soil erosion',
          'To eliminate pollination',
          'To stop photosynthesis',
        ],
        correctAnswer: 'To improve food security during erratic rainfall',
        explanation: 'Accurate! Drought-tolerant traits protect harvests in ASAL regions.',
      },
      {
        id: 'g12-s-2',
        type: 'matching',
        prompt: 'Match each genetics concept to its definition.',
        bilingualHint: 'Hint: DNA carries genetic code; phenotype is the observable trait.',
        pairs: [
          { id: 'g12s1', left: 'Genotype', right: 'Genetic makeup of an organism' },
          { id: 'g12s2', left: 'Phenotype', right: 'Observable physical characteristics' },
          { id: 'g12s3', left: 'DNA', right: 'Molecule carrying hereditary instructions' },
        ],
        explanation: 'Well done! Clear understanding of genetics and biotechnology.',
      },
      {
        id: 'g12-s-3',
        type: 'fill_blank',
        prompt: 'In Kenya, the national agricultural research body developing resilient seeds is abbreviated ________.',
        bilingualHint: 'Hint: Kenya Agricultural and Livestock Research Organization (5 letters).',
        blankPrefix: 'Organization:',
        blankSuffix: '',
        correctAnswer: 'KALRO',
        acceptableAnswers: ['kalro'],
        explanation: 'Correct! KALRO leads agricultural research and resilient seed development in Kenya.',
      },
    ],
  },
  ...CBC_EXTRA_LESSONS,
];

export const CBC_DIRECTORY_CATALOG: CbcCatalogEntry[] = buildFullDirectoryCatalog();

export interface SubjectStrandOption {
  strand: string;
  subStrand: string;
  levelHint: string;
  minGrade: number;
  maxGrade: number;
  linkedLessonId: string;
}

export const CBC_SUBJECT_STRANDS: Record<LearningArea, SubjectStrandOption[]> =
  CBC_CURRICULUM_TREE.reduce(
    (acc, subject) => {
      acc[subject.learningArea] = subject.strands.flatMap((st) =>
        st.subStrands.map((sub) => ({
          strand: st.strand,
          subStrand: sub.subStrand,
          levelHint:
            sub.minGrade === sub.maxGrade
              ? `Grade ${sub.minGrade}`
              : `Grades ${sub.minGrade}–${sub.maxGrade}`,
          minGrade: sub.minGrade,
          maxGrade: sub.maxGrade,
          linkedLessonId: sub.linkedLessonId,
        }))
      );
      return acc;
    },
    {} as Record<LearningArea, SubjectStrandOption[]>
  );

export function generatePairingCode(): string {
  const digits = Math.floor(1000 + Math.random() * 9000);
  return `SOMA-${digits}`;
}

export function generateTeacherCode(): string {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let suffix = '';
  for (let i = 0; i < 6; i += 1) {
    suffix += alphabet[Math.floor(Math.random() * alphabet.length)];
  }
  return `TCH-${suffix}`;
}

export function getCatalogTrackingKey(
  grade: number,
  learningArea: LearningArea,
  strand: string,
  subStrand: string
): string {
  return `cat:${grade}:${learningArea}:${strand}:${subStrand}`;
}

export interface PickedAssignmentResult {
  lesson: CBCLesson;
  isPriority: boolean;
  trackingKey: string;
  quizId: string;
  wrappedCycle: boolean;
}

export function scaleLessonQuestionsForGrade(lesson: CBCLesson, grade: number): CBCLesson {
  return buildGradeScaledLesson(lesson, grade, CBC_LESSONS, getLevelGroupForGrade(grade));
}

export function buildLessonFromTeacherAssignment(
  assign: TeacherAssignment,
  learnerGrade: number
): CBCLesson {
  const base =
    CBC_LESSONS.find((l) => l.id === assign.lessonId) ??
    CBC_LESSONS.find((l) => l.grade === learnerGrade && l.learningArea === assign.learningArea) ??
    CBC_LESSONS.find((l) => l.learningArea === assign.learningArea) ??
    CBC_LESSONS[0];

  const hasCustomQuestions =
    Array.isArray(assign.customQuestions) && assign.customQuestions.length > 0;
  const hasAttachment = Boolean(assign.attachmentUrl || assign.attachmentName);

  if (assign.isCustom || hasCustomQuestions || hasAttachment) {
    const fallbackWorksheetQuestion = {
      id: `${assign.id}-ws-1`,
      type: 'multiple_choice' as const,
      prompt: `Have you reviewed ${
        assign.attachmentName
          ? `your teacher's uploaded material ("${assign.attachmentName}")`
          : `"${assign.title}"`
      } and completed the task instructions?`,
      bilingualHint:
        assign.instructions?.trim() ||
        'Open the uploaded material above, follow your teacher’s notes, then confirm completion.',
      options: [
        'Yes, I reviewed the material and completed the task',
        'I still need to read the teacher instructions',
      ],
      correctAnswer: 'Yes, I reviewed the material and completed the task',
      explanation: `Great work completing ${assign.teacherName || 'your teacher'}'s custom assignment!`,
    };

    return {
      id: assign.lessonId,
      assignmentId: assign.id,
      isCustom: true,
      title: assign.title,
      swahiliTitle: assign.title,
      grade: assign.grade,
      levelGroup: getLevelGroupForGrade(assign.grade),
      learningArea: assign.learningArea,
      strand: assign.strand,
      subStrand: assign.subStrand,
      durationMins: 5,
      summary:
        assign.instructions?.trim() ||
        `Custom ${assign.learningArea} assignment from ${assign.teacherName || 'Class Teacher'}.`,
      instructions: assign.instructions,
      attachmentName: assign.attachmentName,
      attachmentUrl: assign.attachmentUrl,
      attachmentMimeType: assign.attachmentMimeType,
      attachmentSizeBytes: assign.attachmentSizeBytes,
      questions: hasCustomQuestions ? assign.customQuestions! : [fallbackWorksheetQuestion],
    };
  }

  const contextual: CBCLesson = {
    ...base,
    assignmentId: assign.id,
    title: assign.title || `${assign.subStrand} (${base.title})`,
    swahiliTitle: assign.title || base.swahiliTitle,
    grade: assign.grade,
    levelGroup: getLevelGroupForGrade(assign.grade),
    learningArea: assign.learningArea,
    strand: assign.strand,
    subStrand: assign.subStrand,
  };

  return scaleLessonQuestionsForGrade(contextual, learnerGrade);
}

export function pickNextUnseenAssignment(params: {
  child: ChildProfile;
  pendingAssignments: TeacherAssignment[];
  displayedKeys: string[];
  displayedQuizIds: string[];
}): PickedAssignmentResult {
  const { child, pendingAssignments, displayedKeys, displayedQuizIds } = params;
  const learnerGrade = child.grade;

  // 1. Prioritize pending Teacher Assignments for the learner's grade
  if (pendingAssignments.length > 0) {
    const lastAssignKey = [...displayedKeys].reverse().find((k) => k.startsWith('assign:'));
    const unseenAssigns = pendingAssignments.filter(
      (a) => !displayedKeys.includes(`assign:${a.id}`)
    );
    const pool =
      unseenAssigns.length > 0
        ? unseenAssigns
        : pendingAssignments.filter((a) => `assign:${a.id}` !== lastAssignKey).length > 0
          ? pendingAssignments.filter((a) => `assign:${a.id}` !== lastAssignKey)
          : pendingAssignments;

    const chosen = pool[Math.floor(Math.random() * pool.length)];
    const builtLesson = buildLessonFromTeacherAssignment(chosen, learnerGrade);

    return {
      lesson: builtLesson,
      isPriority: true,
      trackingKey: `assign:${chosen.id}`,
      quizId: chosen.lessonId,
      wrappedCycle: false,
    };
  }

  // 2. Build grade-appropriate Kenyan CBC catalog pool
  const gradeCatalog = buildCatalogForGrade(learnerGrade);
  const mandatoryAvailable =
    child.mandatorySubjects.length > 0 &&
    gradeCatalog.some((entry) => child.mandatorySubjects.includes(entry.learningArea));
  const mandatoryDoneToday =
    !mandatoryAvailable ||
    child.activityLogs.some((log) => child.mandatorySubjects.includes(log.learningArea));

  const pool =
    !mandatoryDoneToday && mandatoryAvailable
      ? gradeCatalog.filter((entry) => child.mandatorySubjects.includes(entry.learningArea))
      : gradeCatalog;

  const fallbackPool = pool.length > 0 ? pool : CBC_DIRECTORY_CATALOG;
  const lastCatKey = [...displayedKeys].reverse().find((k) => k.startsWith('cat:'));
  const lastQuizId = displayedQuizIds[displayedQuizIds.length - 1];

  const keyOf = (e: CbcCatalogEntry) =>
    getCatalogTrackingKey(learnerGrade, e.learningArea, e.strand, e.subStrand);

  // Tier 1: unseen sub-strand AND unseen underlying quiz questions
  const tier1 = fallbackPool.filter(
    (e) => !displayedKeys.includes(keyOf(e)) && !displayedQuizIds.includes(e.linkedLessonId)
  );
  // Tier 2: unseen sub-strand AND not the exact previous quiz questions
  const tier2 = fallbackPool.filter(
    (e) => !displayedKeys.includes(keyOf(e)) && e.linkedLessonId !== lastQuizId
  );
  // Tier 3: any unseen sub-strand
  const tier3 = fallbackPool.filter((e) => !displayedKeys.includes(keyOf(e)));
  // Tier 4: cycle exhausted -> wrap around avoiding immediate back-to-back repeat
  const tier4NoRepeat = fallbackPool.filter(
    (e) => keyOf(e) !== lastCatKey && e.linkedLessonId !== lastQuizId
  );
  const tier4DiffKey = fallbackPool.filter((e) => keyOf(e) !== lastCatKey);

  const candidates =
    tier1.length > 0
      ? tier1
      : tier2.length > 0
        ? tier2
        : tier3.length > 0
          ? tier3
          : tier4NoRepeat.length > 0
            ? tier4NoRepeat
            : tier4DiffKey.length > 0
              ? tier4DiffKey
              : fallbackPool;

  const wrappedCycle = tier1.length === 0 && tier2.length === 0 && tier3.length === 0;
  const chosenEntry = candidates[Math.floor(Math.random() * candidates.length)];

  const base =
    CBC_LESSONS.find((l) => l.id === chosenEntry.linkedLessonId) ??
    CBC_LESSONS.find((l) => l.grade === learnerGrade && l.learningArea === chosenEntry.learningArea) ??
    CBC_LESSONS.find((l) => l.learningArea === chosenEntry.learningArea) ??
    CBC_LESSONS[0];

  const contextual: CBCLesson =
    base.learningArea === chosenEntry.learningArea &&
    base.strand === chosenEntry.strand &&
    base.subStrand === chosenEntry.subStrand &&
    base.grade === learnerGrade
      ? base
      : {
          ...base,
          title: `${chosenEntry.subStrand} Practice`,
          swahiliTitle: `${chosenEntry.subStrand} Practice`,
          grade: learnerGrade,
          levelGroup: chosenEntry.levelGroup,
          pathway: chosenEntry.pathway,
          learningArea: chosenEntry.learningArea,
          strand: chosenEntry.strand,
          subStrand: chosenEntry.subStrand,
        };

  return {
    lesson: scaleLessonQuestionsForGrade(contextual, learnerGrade),
    isPriority: false,
    trackingKey: keyOf(chosenEntry),
    quizId: base.id,
    wrappedCycle,
  };
}

export const MONTHLY_LICENSE_KES = 30;
export const MONTHLY_LICENSE_PLAN_LABEL = 'Monthly Child Guard · KES 30/mo';

export function buildLicenseExpiryIso(daysFromNow = 30): string {
  const dt = new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000);
  return dt.toISOString().slice(0, 10);
}

export const INITIAL_CHILDREN: ChildProfile[] = [
  {
    id: 'amina',
    name: 'Amina Wanjiku',
    grade: 4,
    school: 'Nairobi Primary School',
    classStream: 'Grade 4 West - Nairobi Primary',
    avatarUrl: ASSETS.aminaAvatar,
    deviceMode: 'hand_off',
    pairingCode: 'SOMA-4012',
    isPaired: false,
    backgroundGuardActive: false,
    dailyCapMins: 90,
    exchangeRateMins: 15,
    mandatorySubjects: ['Mathematics', 'Kiswahili'],
    requireParentApproval: false,
    availableScreenSeconds: 0,
    usedScreenTodayMins: 35,
    learnedMinutesToday: 25,
    isLocked: true,
    activeAppName: null,
    completedLessonIds: ['g2-kisw-msamiati'],
    pendingApprovals: [],
    activityLogs: [
      {
        id: 'log-a-1',
        lessonId: 'g2-kisw-msamiati',
        lessonTitle: 'Msamiati wa Nyumbani na Shuleni',
        learningArea: 'Kiswahili',
        scorePercent: 100,
        rubricEarned: 'EE',
        minutesEarned: 15,
        status: 'approved',
        timestamp: '08:40 AM',
      },
    ],
    rubricByArea: {
      Mathematics: 'ME',
      Kiswahili: 'EE',
      English: 'ME',
      'Science & Technology': 'AE',
      'Social Studies': 'ME',
      'Agriculture & Nutrition': 'ME',
    },
    streakDays: 5,
    licenseActive: false,
    licensePlanLabel: MONTHLY_LICENSE_PLAN_LABEL,
    licenseExpiresAt: null,
    licensePaymentRef: null,
    linkedTeacherCodes: ['TCH-4821'],
    linkedTeachers: [
      {
        teacherCode: 'TCH-4821',
        fullName: 'Ms. Grace Njeri',
        school: 'Nairobi Primary School',
        tscNumber: 'TSC-482910',
        grade: 6,
        classStream: 'Grade 6 East - Nairobi Primary',
        subjects: ['Social Studies', 'Mathematics'],
      },
    ],
  },
  {
    id: 'brian',
    name: 'Brian Ochieng',
    grade: 8,
    school: 'Kilimani Junior School',
    classStream: 'Grade 8 North - Kilimani Junior',
    avatarUrl: ASSETS.brianAvatar,
    deviceMode: 'own_phone',
    pairingCode: 'SOMA-8009',
    isPaired: true,
    backgroundGuardActive: true,
    dailyCapMins: 120,
    exchangeRateMins: 15,
    mandatorySubjects: ['Mathematics', 'Integrated Science'],
    requireParentApproval: true,
    availableScreenSeconds: 420,
    usedScreenTodayMins: 45,
    learnedMinutesToday: 30,
    isLocked: false,
    activeAppName: 'YouTube',
    completedLessonIds: ['g7-sci-apparatus'],
    pendingApprovals: [
      {
        id: 'pend-b-1',
        lessonTitle: 'Linear Equations & M-Pesa Budgeting',
        learningArea: 'Mathematics',
        minutesEarned: 15,
        scorePercent: 100,
        completedAt: '09:50 AM',
      },
    ],
    activityLogs: [
      {
        id: 'log-b-1',
        lessonId: 'g7-sci-apparatus',
        lessonTitle: 'Laboratory Apparatus & Safety',
        learningArea: 'Integrated Science',
        scorePercent: 92,
        rubricEarned: 'EE',
        minutesEarned: 15,
        status: 'approved',
        timestamp: '08:15 AM',
      },
    ],
    rubricByArea: {
      Mathematics: 'EE',
      'Integrated Science': 'EE',
      English: 'ME',
      Kiswahili: 'AE',
      'Social Studies': 'ME',
    },
    streakDays: 9,
    licenseActive: true,
    licensePlanLabel: MONTHLY_LICENSE_PLAN_LABEL,
    licenseExpiresAt: buildLicenseExpiryIso(30),
    licensePaymentRef: 'PSK-SOMA8009',
    linkedTeacherCodes: ['TCH-4821'],
    linkedTeachers: [
      {
        teacherCode: 'TCH-4821',
        fullName: 'Ms. Grace Njeri',
        school: 'Nairobi Primary School',
        tscNumber: 'TSC-482910',
        grade: 6,
        classStream: 'Grade 6 East - Nairobi Primary',
        subjects: ['Social Studies', 'Mathematics'],
      },
    ],
  },
];

export const INITIAL_ASSIGNMENTS: TeacherAssignment[] = [
  {
    id: 'assign-g6-sst',
    classStream: 'Grade 6 East - Nairobi Primary',
    school: 'Nairobi Primary School',
    grade: 6,
    learningArea: 'Social Studies',
    strand: 'Resources & Economic Activities',
    subStrand: 'Cash Crops & Port Trade',
    lessonId: 'g6-sst-resources',
    title: 'Kenyan Agriculture & Trade Corridors',
    teacherName: 'Ms. Grace Njeri',
    teacherCode: 'TCH-4821',
    dueLabel: 'Today · 5:00 PM',
    pushedAt: '08:00 AM',
    completedByChildIds: [],
  },
];

export const CLASS_STREAMS = [
  {
    stream: 'Grade 6 East - Nairobi Primary',
    grade: 6,
    teacher: 'Ms. Grace Njeri',
    studentsCount: 38,
  },
  {
    stream: 'Grade 4 West - Nairobi Primary',
    grade: 4,
    teacher: 'Mr. David Kiprop',
    studentsCount: 34,
  },
  {
    stream: 'Grade 8 North - Kilimani Junior',
    grade: 8,
    teacher: 'Mr. Peter Mwangi',
    studentsCount: 41,
  },
];

export const INITIAL_CLASS_ROSTER: ClassStudentRecord[] = [
  {
    id: 'stu-1',
    name: 'Amina Wanjiku',
    admissionNo: 'NPS-4012',
    classStream: 'Grade 4 West - Nairobi Primary',
    rubric: 'EE',
    scorePercent: 94,
    learningMins: 42,
    assignmentDone: true,
  },
  {
    id: 'stu-2',
    name: 'Kiprop Kipchoge',
    admissionNo: 'NPS-4018',
    classStream: 'Grade 4 West - Nairobi Primary',
    rubric: 'ME',
    scorePercent: 78,
    learningMins: 30,
    assignmentDone: true,
  },
  {
    id: 'stu-3',
    name: 'Zawadi Hassan',
    admissionNo: 'NPS-4025',
    classStream: 'Grade 4 West - Nairobi Primary',
    rubric: 'AE',
    scorePercent: 61,
    learningMins: 18,
    assignmentDone: false,
  },
  {
    id: 'stu-4',
    name: 'Faith Muthoni',
    admissionNo: 'NPS-6004',
    classStream: 'Grade 6 East - Nairobi Primary',
    rubric: 'EE',
    scorePercent: 96,
    learningMins: 48,
    assignmentDone: true,
  },
  {
    id: 'stu-5',
    name: 'Otieno Kevin',
    admissionNo: 'NPS-6011',
    classStream: 'Grade 6 East - Nairobi Primary',
    rubric: 'ME',
    scorePercent: 82,
    learningMins: 35,
    assignmentDone: true,
  },
  {
    id: 'stu-6',
    name: 'Chebet Mercy',
    admissionNo: 'NPS-6019',
    classStream: 'Grade 6 East - Nairobi Primary',
    rubric: 'AE',
    scorePercent: 58,
    learningMins: 22,
    assignmentDone: false,
  },
  {
    id: 'stu-7',
    name: 'Baraka Njoroge',
    admissionNo: 'NPS-6027',
    classStream: 'Grade 6 East - Nairobi Primary',
    rubric: 'BE',
    scorePercent: 41,
    learningMins: 12,
    assignmentDone: false,
  },
  {
    id: 'stu-8',
    name: 'Brian Ochieng',
    admissionNo: 'KJS-8009',
    classStream: 'Grade 8 North - Kilimani Junior',
    rubric: 'EE',
    scorePercent: 92,
    learningMins: 45,
    assignmentDone: true,
  },
  {
    id: 'stu-9',
    name: 'Wambui Ndungu',
    admissionNo: 'KJS-8014',
    classStream: 'Grade 8 North - Kilimani Junior',
    rubric: 'ME',
    scorePercent: 80,
    learningMins: 34,
    assignmentDone: true,
  },
  {
    id: 'stu-10',
    name: 'Abdirahman Ali',
    admissionNo: 'KJS-8022',
    classStream: 'Grade 8 North - Kilimani Junior',
    rubric: 'AE',
    scorePercent: 64,
    learningMins: 20,
    assignmentDone: false,
  },
];
