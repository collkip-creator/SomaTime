import type {
  CBCLevelGroup,
  CBCLesson,
  CBCRubric,
  ChildProfile,
  ClassStudentRecord,
  LearningArea,
  TeacherAssignment,
} from '../types/somatime';

export const PARENT_PIN = '1234';

export const ASSETS = {
  aminaAvatar: '/src/assets/images/avatar_amina_1794996018825.jpg',
  brianAvatar: '/src/assets/images/avatar_brian_1794996019103.jpg',
  wallpaper: '/src/assets/images/kenya_savanna_wallpaper_1794996019501.jpg',
  learningBanner: '/src/assets/images/cbc_learning_banner_1794996019764.jpg',
};

export const ALL_LEARNING_AREAS: LearningArea[] = [
  'Mathematics',
  'English',
  'Kiswahili',
  'Science & Technology',
  'Integrated Science',
  'Social Studies',
  'Agriculture & Nutrition',
  'Creative Arts',
];

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
];

export const CBC_DIRECTORY_CATALOG: Array<{
  grade: number;
  levelGroup: CBCLevelGroup;
  pathway?: string;
  learningArea: LearningArea;
  strand: string;
  subStrand: string;
  linkedLessonId: string;
}> = [
  { grade: 1, levelGroup: 'Lower Primary', learningArea: 'English', strand: 'Listening & Speaking', subStrand: 'Polite Expressions', linkedLessonId: 'g1-eng-reading' },
  { grade: 2, levelGroup: 'Lower Primary', learningArea: 'Kiswahili', strand: 'Kusikiliza na Kuzungumza', subStrand: 'Msamiati wa Nyumbani', linkedLessonId: 'g2-kisw-msamiati' },
  { grade: 3, levelGroup: 'Lower Primary', learningArea: 'Creative Arts', strand: 'Performing & Visual Arts', subStrand: 'Kenyan Folk Songs & Rhythm', linkedLessonId: 'g2-kisw-msamiati' },
  { grade: 4, levelGroup: 'Upper Primary', learningArea: 'Mathematics', strand: 'Numbers', subStrand: 'Fractions in Daily Life', linkedLessonId: 'g4-math-fractions' },
  { grade: 4, levelGroup: 'Upper Primary', learningArea: 'Science & Technology', strand: 'Living Things', subStrand: 'Plants & Vertebrates', linkedLessonId: 'g5-agri-kitchen-garden' },
  { grade: 5, levelGroup: 'Upper Primary', learningArea: 'Agriculture & Nutrition', strand: 'Conservation of Resources', subStrand: 'Organic Compost & Sack Gardens', linkedLessonId: 'g5-agri-kitchen-garden' },
  { grade: 6, levelGroup: 'Upper Primary', learningArea: 'Social Studies', strand: 'Resources & Economic Activities', subStrand: 'Cash Crops & Port Trade', linkedLessonId: 'g6-sst-resources' },
  { grade: 7, levelGroup: 'Junior School', learningArea: 'Integrated Science', strand: 'Scientific Investigation', subStrand: 'Laboratory Apparatus & Instruments', linkedLessonId: 'g7-sci-apparatus' },
  { grade: 8, levelGroup: 'Junior School', learningArea: 'Mathematics', strand: 'Algebra', subStrand: 'Linear Equations in One Unknown', linkedLessonId: 'g8-math-algebra' },
  { grade: 9, levelGroup: 'Junior School', learningArea: 'Social Studies', strand: 'Citizenship & Governance', subStrand: 'Kenyan Constitution & Devolution', linkedLessonId: 'g6-sst-resources' },
  { grade: 10, levelGroup: 'Senior School', pathway: 'STEM Pathway', learningArea: 'Integrated Science', strand: 'Energy & Sustainability', subStrand: 'Renewable Power Systems', linkedLessonId: 'g10-stem-energy' },
  { grade: 11, levelGroup: 'Senior School', pathway: 'Social Sciences Pathway', learningArea: 'Social Studies', strand: 'Regional Integration', subStrand: 'East African Community Trade', linkedLessonId: 'g6-sst-resources' },
  { grade: 12, levelGroup: 'Senior School', pathway: 'Arts & Sports Science', learningArea: 'Creative Arts', strand: 'Digital Media & Design', subStrand: 'Kenyan Cultural Visual Storytelling', linkedLessonId: 'g1-eng-reading' },
];

export function generatePairingCode(): string {
  const digits = Math.floor(1000 + Math.random() * 9000);
  return `SOMA-${digits}`;
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
  },
];

export const INITIAL_ASSIGNMENTS: TeacherAssignment[] = [
  {
    id: 'assign-g6-sst',
    classStream: 'Grade 6 East - Nairobi Primary',
    grade: 6,
    learningArea: 'Social Studies',
    strand: 'Resources & Economic Activities',
    subStrand: 'Cash Crops & Port Trade',
    lessonId: 'g6-sst-resources',
    title: 'Kenyan Agriculture & Trade Corridors',
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
