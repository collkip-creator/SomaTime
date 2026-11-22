import type {
  ActivityQuestion,
  CBCLevelGroup,
  CBCLesson,
  LearningArea,
} from '../types/somatime';

export interface TaggedBonusQuestion {
  learningArea: LearningArea;
  levelGroup: CBCLevelGroup;
  question: ActivityQuestion;
}

/**
 * Scales the number of questions per learning session by CBC Grade:
 * Grade 1 starts at 3 questions and increases progressively through Grade 12 (10 questions).
 */
export function getQuestionCountForGrade(grade: number): number {
  const safeGrade = Math.max(1, Math.min(12, Math.round(grade)));
  const counts: Record<number, number> = {
    1: 3,
    2: 4,
    3: 4,
    4: 5,
    5: 5,
    6: 6,
    7: 7,
    8: 7,
    9: 8,
    10: 9,
    11: 9,
    12: 10,
  };
  return counts[safeGrade] ?? 5;
}

export const CBC_BONUS_QUESTIONS: TaggedBonusQuestion[] = [
  // Lower Primary (Grades 1–3)
  {
    learningArea: 'Mathematics',
    levelGroup: 'Lower Primary',
    question: {
      id: 'ext-lp-math-1',
      type: 'multiple_choice',
      prompt: 'Kamau counts in tens: 20, 30, 40, ___. What number comes next?',
      bilingualHint: 'Hint: Add 10 to 40.',
      options: ['50', '45', '60', '41'],
      correctAnswer: '50',
      explanation: 'Well done! Counting forward by tens after 40 gives 50.',
    },
  },
  {
    learningArea: 'English',
    levelGroup: 'Lower Primary',
    question: {
      id: 'ext-lp-eng-1',
      type: 'multiple_choice',
      prompt: 'What is the plural of the word "mango"?',
      bilingualHint: 'Hint: Words ending in -o often add -es.',
      options: ['mangoes', 'mangos', 'mangoies', 'mango'],
      correctAnswer: 'mangoes',
      explanation: 'Correct! One mango, many mangoes.',
    },
  },
  {
    learningArea: 'Kiswahili',
    levelGroup: 'Lower Primary',
    question: {
      id: 'ext-lp-kisw-1',
      type: 'multiple_choice',
      prompt: 'Chagua wingi wa neno "mtoto" (Choose the plural of child):',
      bilingualHint: 'Hint: Ngeli ya A-WA hubadilisha m- kuwa wa-.',
      options: ['watoto', 'mitoto', 'vitoto', 'matoto'],
      correctAnswer: 'watoto',
      explanation: 'Vizuri! Umoja ni "mtoto" na wingi ni "watoto".',
    },
  },
  {
    learningArea: 'Environmental Activities',
    levelGroup: 'Lower Primary',
    question: {
      id: 'ext-lp-env-1',
      type: 'multiple_choice',
      prompt: 'Which domestic animal gives Kenyan families fresh milk every morning?',
      bilingualHint: 'Hint: A dairy farm animal.',
      options: ['Cow', 'Chicken', 'Cat', 'Donkey'],
      correctAnswer: 'Cow',
      explanation: 'Correct! Dairy cows and goats provide nutritious milk.',
    },
  },

  // Upper Primary (Grades 4–6)
  {
    learningArea: 'Mathematics',
    levelGroup: 'Upper Primary',
    question: {
      id: 'ext-up-math-1',
      type: 'multiple_choice',
      prompt: 'What is the perimeter of a rectangular school garden measuring 12 m long and 8 m wide?',
      bilingualHint: 'Hint: Add all four sides: 12 + 8 + 12 + 8.',
      options: ['40 m', '96 m', '20 m', '32 m'],
      correctAnswer: '40 m',
      explanation: 'Spot on! Perimeter = 2 × (12 + 8) = 40 metres.',
    },
  },
  {
    learningArea: 'Mathematics',
    levelGroup: 'Upper Primary',
    question: {
      id: 'ext-up-math-2',
      type: 'fill_blank',
      prompt: 'Convert 3 kilograms (kg) of sugar into grams (g).',
      bilingualHint: 'Hint: 1 kg equals 1,000 grams.',
      blankPrefix: '3 kg =',
      blankSuffix: 'grams',
      correctAnswer: '3000',
      acceptableAnswers: ['3000', '3,000'],
      explanation: 'Correct! 3 × 1,000 = 3,000 grams.',
    },
  },
  {
    learningArea: 'English',
    levelGroup: 'Upper Primary',
    question: {
      id: 'ext-up-eng-1',
      type: 'multiple_choice',
      prompt: 'Choose the correct past tense verb: "Yesterday, our class ________ Lake Naivasha."',
      bilingualHint: 'Hint: Regular verbs add -ed for past simple.',
      options: ['visited', 'visits', 'visiting', 'will visit'],
      correctAnswer: 'visited',
      explanation: 'Well done! "Yesterday" signals past tense: "visited".',
    },
  },
  {
    learningArea: 'Kiswahili',
    levelGroup: 'Upper Primary',
    question: {
      id: 'ext-up-kisw-1',
      type: 'multiple_choice',
      prompt: 'Kamilisha methali hii: "Haraka haraka haina ________."',
      bilingualHint: 'Hint: Haste has no blessing (starts with B).',
      options: ['baraka', 'harara', 'faida', 'hasara'],
      correctAnswer: 'baraka',
      explanation: 'Safi sana! "Haraka haraka haina baraka" hufunza subira na umakini.',
    },
  },
  {
    learningArea: 'Science & Technology',
    levelGroup: 'Upper Primary',
    question: {
      id: 'ext-up-sci-1',
      type: 'multiple_choice',
      prompt: 'Which gas in the air do green plants absorb during photosynthesis to make food?',
      bilingualHint: 'Hint: Not oxygen—the gas humans breathe out.',
      options: ['Carbon dioxide', 'Nitrogen', 'Argon', 'Helium'],
      correctAnswer: 'Carbon dioxide',
      explanation: 'Correct! Plants take in carbon dioxide through tiny leaf stomata.',
    },
  },
  {
    learningArea: 'Social Studies',
    levelGroup: 'Upper Primary',
    question: {
      id: 'ext-up-sst-1',
      type: 'multiple_choice',
      prompt: 'Which Kenyan city serves as the national capital and hosts the headquarters of UNEP?',
      bilingualHint: 'Hint: Known as the Green City in the Sun.',
      options: ['Nairobi', 'Kisumu', 'Eldoret', 'Nakuru'],
      correctAnswer: 'Nairobi',
      explanation: 'Correct! Nairobi is Kenya’s capital and hosts the UN Environment Programme.',
    },
  },
  {
    learningArea: 'Agriculture & Nutrition',
    levelGroup: 'Upper Primary',
    question: {
      id: 'ext-up-agri-1',
      type: 'multiple_choice',
      prompt: 'Which food group mainly repairs worn-out body tissues and builds strong muscles?',
      bilingualHint: 'Hint: Found in beans, fish, eggs, and lean meat.',
      options: ['Proteins', 'Carbohydrates', 'Fats', 'Water only'],
      correctAnswer: 'Proteins',
      explanation: 'Accurate! Beans, fish, milk, and eggs are rich in body-building proteins.',
    },
  },
  {
    learningArea: 'Religious Education',
    levelGroup: 'Upper Primary',
    question: {
      id: 'ext-up-re-1',
      type: 'multiple_choice',
      prompt: 'Why is planting trees around our school compound an act of good stewardship?',
      bilingualHint: 'Hint: Think about protecting nature for future generations.',
      options: [
        'It conserves soil, shade, and clean air for all',
        'It blocks rain from reaching rivers',
        'It increases plastic litter',
        'It removes oxygen from air',
      ],
      correctAnswer: 'It conserves soil, shade, and clean air for all',
      explanation: 'Well done! Stewardship means caring responsibly for the environment.',
    },
  },

  // Junior School (Grades 7–9)
  {
    learningArea: 'Mathematics',
    levelGroup: 'Junior School',
    question: {
      id: 'ext-js-math-1',
      type: 'multiple_choice',
      prompt: 'Simplify the algebraic expression: 5a + 3b - 2a + 4b.',
      bilingualHint: 'Hint: Combine like terms (a with a, b with b).',
      options: ['3a + 7b', '7a + 7b', '3a - b', '8a + 2b'],
      correctAnswer: '3a + 7b',
      explanation: 'Spot on! (5a - 2a) + (3b + 4b) = 3a + 7b.',
    },
  },
  {
    learningArea: 'Mathematics',
    levelGroup: 'Junior School',
    question: {
      id: 'ext-js-math-2',
      type: 'fill_blank',
      prompt: 'What is the square root of 144?',
      bilingualHint: 'Hint: Which whole number multiplied by itself gives 144?',
      blankPrefix: '√144 =',
      blankSuffix: '',
      correctAnswer: '12',
      acceptableAnswers: ['12', 'twelve'],
      explanation: 'Correct! 12 × 12 = 144.',
    },
  },
  {
    learningArea: 'Integrated Science',
    levelGroup: 'Junior School',
    question: {
      id: 'ext-js-sci-1',
      type: 'multiple_choice',
      prompt: 'Which separating technique recovers dissolved table salt crystals from salty water?',
      bilingualHint: 'Hint: Heating turns liquid water into vapour, leaving salt crystals behind.',
      options: ['Evaporation', 'Decanting', 'Magnetism', 'Sieving'],
      correctAnswer: 'Evaporation',
      explanation: 'Accurate! Evaporation removes water vapour and leaves pure salt crystals.',
    },
  },
  {
    learningArea: 'Integrated Science',
    levelGroup: 'Junior School',
    question: {
      id: 'ext-js-sci-2',
      type: 'multiple_choice',
      prompt: 'What is the SI unit of electric current?',
      bilingualHint: 'Hint: Symbol is A.',
      options: ['Ampere', 'Volt', 'Watt', 'Joule'],
      correctAnswer: 'Ampere',
      explanation: 'Correct! Electric current is measured in Amperes (A).',
    },
  },
  {
    learningArea: 'Computer Studies',
    levelGroup: 'Junior School',
    question: {
      id: 'ext-js-comp-1',
      type: 'multiple_choice',
      prompt: 'Which cybersecurity habit best protects a learner’s online school portal account?',
      bilingualHint: 'Hint: Never share credentials and use unique characters.',
      options: [
        'Using a strong unique password and keeping it private',
        'Writing the password on the classroom desk',
        'Using "1234" on every website',
        'Clicking unknown prize links',
      ],
      correctAnswer: 'Using a strong unique password and keeping it private',
      explanation: 'Great digital citizenship! Strong private passwords prevent unauthorized access.',
    },
  },
  {
    learningArea: 'Pre-Technical Studies',
    levelGroup: 'Junior School',
    question: {
      id: 'ext-js-pt-1',
      type: 'multiple_choice',
      prompt: 'In an isometric sketch, receding horizontal edges are commonly drawn at what angle to the horizontal line?',
      bilingualHint: 'Hint: Standard isometric angle used with a 30°/60° set square.',
      options: ['30°', '90°', '15°', '75°'],
      correctAnswer: '30°',
      explanation: 'Spot on! Isometric drawings project horizontal axes at 30°.',
    },
  },
  {
    learningArea: 'Business Studies',
    levelGroup: 'Junior School',
    question: {
      id: 'ext-js-biz-1',
      type: 'multiple_choice',
      prompt: 'Which Kenyan mobile money action lets a trader receive payments via a 6-digit Till or Paybill number?',
      bilingualHint: 'Hint: Lipa na M-Pesa merchant option.',
      options: ['Lipa na M-Pesa (Buy Goods / Paybill)', 'Airplane Mode', 'Bluetooth Pairing', 'SIM PIN Lock'],
      correctAnswer: 'Lipa na M-Pesa (Buy Goods / Paybill)',
      explanation: 'Correct! Merchant Till and Paybill numbers streamline digital business records.',
    },
  },
  {
    learningArea: 'English',
    levelGroup: 'Junior School',
    question: {
      id: 'ext-js-eng-1',
      type: 'multiple_choice',
      prompt: 'Identify the passive voice version of: "The students planted fifty trees."',
      bilingualHint: 'Hint: Make "Fifty trees" the subject of the sentence.',
      options: [
        'Fifty trees were planted by the students.',
        'The students are planting fifty trees.',
        'Fifty trees planted the students.',
        'The students will plant fifty trees.',
      ],
      correctAnswer: 'Fifty trees were planted by the students.',
      explanation: 'Well done! In passive voice, the receiver ("Fifty trees") moves to subject position.',
    },
  },

  // Senior School (Grades 10–12)
  {
    learningArea: 'Mathematics',
    levelGroup: 'Senior School',
    question: {
      id: 'ext-ss-math-1',
      type: 'multiple_choice',
      prompt: 'Solve the quadratic equation x² - 5x + 6 = 0.',
      bilingualHint: 'Hint: Find two numbers whose sum is 5 and product is 6.',
      options: ['x = 2 or x = 3', 'x = -2 or x = -3', 'x = 1 or x = 6', 'x = 5 or x = 6'],
      correctAnswer: 'x = 2 or x = 3',
      explanation: 'Correct! (x - 2)(x - 3) = 0 gives roots x = 2 and x = 3.',
    },
  },
  {
    learningArea: 'Mathematics',
    levelGroup: 'Senior School',
    question: {
      id: 'ext-ss-math-2',
      type: 'fill_blank',
      prompt: 'In a right-angled triangle, if sin(θ) = 3/5, what is cos(θ) when θ is acute?',
      bilingualHint: 'Hint: Use the 3-4-5 Pythagorean triple (cos = adjacent / hypotenuse).',
      blankPrefix: 'cos(θ) =',
      blankSuffix: '',
      correctAnswer: '4/5',
      acceptableAnswers: ['4/5', '0.8'],
      explanation: 'Accurate! From 3² + 4² = 5², adjacent = 4, so cos(θ) = 4/5 (or 0.8).',
    },
  },
  {
    learningArea: 'Biology',
    levelGroup: 'Senior School',
    question: {
      id: 'ext-ss-bio-1',
      type: 'multiple_choice',
      prompt: 'During aerobic respiration inside mitochondria, glucose reacts with oxygen to release carbon dioxide, water, and ________.',
      bilingualHint: 'Hint: Cellular energy molecule abbreviated ATP.',
      options: ['ATP energy', 'Chlorophyll', 'Lactic acid only', 'Nitrogen gas'],
      correctAnswer: 'ATP energy',
      explanation: 'Spot on! Mitochondria synthesize ATP to power cellular activities.',
    },
  },
  {
    learningArea: 'Chemistry',
    levelGroup: 'Senior School',
    question: {
      id: 'ext-ss-chem-1',
      type: 'multiple_choice',
      prompt: 'How many moles of solute are present in 2 litres of a 0.5 M sodium chloride solution?',
      bilingualHint: 'Hint: Moles = Molarity (mol/L) × Volume (L).',
      options: ['1.0 mol', '0.25 mol', '2.5 mol', '4.0 mol'],
      correctAnswer: '1.0 mol',
      explanation: 'Correct! 0.5 mol/L × 2 L = 1.0 mole.',
    },
  },
  {
    learningArea: 'Physics',
    levelGroup: 'Senior School',
    question: {
      id: 'ext-ss-phy-1',
      type: 'multiple_choice',
      prompt: 'A matatu accelerates uniformly from rest to 20 m/s in 10 seconds. What is its acceleration?',
      bilingualHint: 'Hint: a = (v - u) ÷ t.',
      options: ['2 m/s²', '200 m/s²', '10 m/s²', '0.5 m/s²'],
      correctAnswer: '2 m/s²',
      explanation: 'Accurate! a = (20 - 0) ÷ 10 = 2 m/s².',
    },
  },
  {
    learningArea: 'Geography',
    levelGroup: 'Senior School',
    question: {
      id: 'ext-ss-geo-1',
      type: 'multiple_choice',
      prompt: 'Why is Lake Naivasha freshwater unlike most other Kenyan Rift Valley lakes such as Magadi or Bogoria?',
      bilingualHint: 'Hint: Think about underground water outflow keeping salts from concentrating.',
      options: [
        'It has underground outflow/seepage preventing salt buildup',
        'It is located in the Indian Ocean',
        'It freezes every July',
        'It receives zero river inflow',
      ],
      correctAnswer: 'It has underground outflow/seepage preventing salt buildup',
      explanation: 'Well done! Subsurface seepage refreshes Lake Naivasha’s water continuously.',
    },
  },
  {
    learningArea: 'History & Citizenship',
    levelGroup: 'Senior School',
    question: {
      id: 'ext-ss-hist-1',
      type: 'multiple_choice',
      prompt: 'Which principle of Kenyan public finance ensures citizens participate in county budget priorities before approval?',
      bilingualHint: 'Hint: Open community consultation mandated by the 2010 Constitution.',
      options: [
        'Public participation and accountability',
        'Secret taxation without notice',
        'Single-person budgeting',
        'Abolishing county assemblies',
      ],
      correctAnswer: 'Public participation and accountability',
      explanation: 'Spot on! Article 201 emphasizes openness, accountability, and public participation.',
    },
  },
  {
    learningArea: 'Integrated Science',
    levelGroup: 'Senior School',
    question: {
      id: 'ext-ss-sci-1',
      type: 'multiple_choice',
      prompt: 'Why does geothermal power in Olkaria provide reliable baseload electricity compared to solar alone?',
      bilingualHint: 'Hint: Underground earth heat is available day and night.',
      options: [
        'Geothermal steam flows 24/7 regardless of sunlight or clouds',
        'Geothermal plants only work at noon',
        'Geothermal steam requires imported coal',
        'Turbines stop spinning at night',
      ],
      correctAnswer: 'Geothermal steam flows 24/7 regardless of sunlight or clouds',
      explanation: 'Correct! Geothermal energy delivers steady 24-hour clean baseload power.',
    },
  },
];

function shuffleArray<T>(items: T[]): T[] {
  const copy = [...items];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

/**
 * Expands a lesson's questions array to match the target question count for the learner's grade
 * (3 questions for Grade 1, scaling progressively up to 10 questions for Grade 12),
 * never duplicating question IDs inside the session.
 */
export function buildGradeScaledLesson(
  lesson: CBCLesson,
  learnerGrade: number,
  allLessons: CBCLesson[],
  levelGroup: CBCLevelGroup
): CBCLesson {
  const targetCount = getQuestionCountForGrade(learnerGrade);
  const chosen: ActivityQuestion[] = [];
  const seenIds = new Set<string>();

  const pushUnique = (q: ActivityQuestion) => {
    if (chosen.length >= targetCount) return;
    if (seenIds.has(q.id)) return;
    seenIds.add(q.id);
    chosen.push(q);
  };

  // 1. Primary strand/lesson questions first
  lesson.questions.forEach(pushUnique);

  if (chosen.length >= targetCount) {
    return { ...lesson, questions: chosen.slice(0, targetCount) };
  }

  // 2. Same learning area + same levelGroup bonus questions
  shuffleArray(
    CBC_BONUS_QUESTIONS.filter(
      (b) => b.learningArea === lesson.learningArea && b.levelGroup === levelGroup
    )
  ).forEach((b) => pushUnique(b.question));

  // 3. Same learning area from other lessons (nearest grade first)
  const sameSubjectLessons = [...allLessons]
    .filter((l) => l.learningArea === lesson.learningArea && l.id !== lesson.id)
    .sort((a, b) => Math.abs(a.grade - learnerGrade) - Math.abs(b.grade - learnerGrade));
  sameSubjectLessons.forEach((l) => l.questions.forEach(pushUnique));

  // 4. Other same-subject bonus questions across adjacent grades
  shuffleArray(
    CBC_BONUS_QUESTIONS.filter((b) => b.learningArea === lesson.learningArea)
  ).forEach((b) => pushUnique(b.question));

  // 5. Same levelGroup CBC bonus questions + lessons (broadens multi-subject revision)
  shuffleArray(
    CBC_BONUS_QUESTIONS.filter((b) => b.levelGroup === levelGroup)
  ).forEach((b) => pushUnique(b.question));

  shuffleArray(allLessons.filter((l) => l.levelGroup === levelGroup)).forEach((l) =>
    l.questions.forEach(pushUnique)
  );

  // 6. Safety net across nearest-grade lessons if still below targetCount
  if (chosen.length < targetCount) {
    [...allLessons]
      .sort((a, b) => Math.abs(a.grade - learnerGrade) - Math.abs(b.grade - learnerGrade))
      .forEach((l) => l.questions.forEach(pushUnique));
  }

  return {
    ...lesson,
    questions: chosen,
  };
}
