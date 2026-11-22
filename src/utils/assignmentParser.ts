import type { ActivityQuestion } from '../types/somatime';

export const SAMPLE_CSV_TEMPLATE = [
  'type,prompt,options,answer,hint,explanation',
  'multiple_choice,"Amina buys 3 exercise books at KES 40 each. How much does she pay?","KES 80|KES 100|KES 120|KES 160","KES 120","Multiply 3 by 40.","3 × KES 40 = KES 120."',
  'multiple_choice,"Which gas do green plants take in during photosynthesis?","Oxygen|Carbon dioxide|Nitrogen|Argon","Carbon dioxide","Plants absorb it from the air through stomata.","Plants use carbon dioxide, water, and sunlight to make food."',
  'fill_blank,"What is 3/4 of KES 200?","","150","Divide 200 by 4 then multiply by 3.","(200 ÷ 4) × 3 = KES 150."',
].join('\n');

export function isSafeAttachmentUrl(rawUrl?: string | null): boolean {
  if (!rawUrl || typeof rawUrl !== 'string') return false;
  const trimmed = rawUrl.trim();
  if (!trimmed) return false;
  if (trimmed.startsWith('/api/assignments/files/')) return true;
  try {
    const parsed = new URL(trimmed);
    return parsed.protocol === 'https:';
  } catch {
    return false;
  }
}

export function formatFileSize(bytes?: number): string {
  if (!bytes || bytes <= 0) return '0 KB';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function downloadAssignmentCsvTemplate(): void {
  const blob = new Blob([SAMPLE_CSV_TEMPLATE], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'somatime_assignment_template.csv';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function parseCsvRows(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = '';
  let inQuotes = false;

  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    const next = text[i + 1];

    if (inQuotes) {
      if (ch === '"' && next === '"') {
        cell += '"';
        i += 1;
      } else if (ch === '"') {
        inQuotes = false;
      } else {
        cell += ch;
      }
      continue;
    }

    if (ch === '"') {
      inQuotes = true;
    } else if (ch === ',') {
      row.push(cell.trim());
      cell = '';
    } else if (ch === '\n' || ch === '\r') {
      if (ch === '\r' && next === '\n') i += 1;
      row.push(cell.trim());
      if (row.some((c) => c.length > 0)) rows.push(row);
      row = [];
      cell = '';
    } else {
      cell += ch;
    }
  }

  row.push(cell.trim());
  if (row.some((c) => c.length > 0)) rows.push(row);
  return rows;
}

function makeQuestionId(index: number): string {
  return `tq-${Date.now()}-${index + 1}`;
}

export function parseUploadedQuestionFile(
  fileName: string,
  content: string
): { questions: ActivityQuestion[]; warnings: string[] } {
  const lower = fileName.toLowerCase();
  const warnings: string[] = [];
  const trimmed = content.trim();
  if (!trimmed) {
    return { questions: [], warnings: ['Uploaded file is empty.'] };
  }

  // 1. JSON format
  if (lower.endsWith('.json') || trimmed.startsWith('[') || trimmed.startsWith('{')) {
    try {
      const parsed = JSON.parse(trimmed) as unknown;
      const rawList = Array.isArray(parsed)
        ? parsed
        : parsed && typeof parsed === 'object' && Array.isArray((parsed as { questions?: unknown[] }).questions)
          ? (parsed as { questions: unknown[] }).questions
          : [];

      const questions: ActivityQuestion[] = [];
      rawList.slice(0, 15).forEach((item, idx) => {
        if (!item || typeof item !== 'object') return;
        const rec = item as Record<string, unknown>;
        const prompt = String(rec.prompt || rec.question || '').trim().slice(0, 320);
        const answer = String(rec.correctAnswer || rec.answer || '').trim().slice(0, 140);
        if (!prompt || !answer) return;

        const rawOpts = Array.isArray(rec.options)
          ? rec.options.map((o) => String(o).trim()).filter(Boolean).slice(0, 4)
          : [];

        const isMcq = rawOpts.length >= 2 || rec.type === 'multiple_choice';
        if (isMcq && rawOpts.length >= 2) {
          const finalOptions = rawOpts.includes(answer) ? rawOpts : [answer, ...rawOpts.slice(0, 3)];
          questions.push({
            id: makeQuestionId(idx),
            type: 'multiple_choice',
            prompt,
            bilingualHint: String(rec.bilingualHint || rec.hint || 'Read carefully before choosing.').slice(0, 220),
            options: finalOptions,
            correctAnswer: answer,
            explanation: String(rec.explanation || `Correct answer: ${answer}`).slice(0, 260),
          });
        } else {
          questions.push({
            id: makeQuestionId(idx),
            type: 'fill_blank',
            prompt,
            bilingualHint: String(rec.bilingualHint || rec.hint || 'Type the exact answer carefully.').slice(0, 220),
            blankPrefix: String(rec.blankPrefix || 'Answer:').slice(0, 40),
            blankSuffix: String(rec.blankSuffix || '').slice(0, 40),
            correctAnswer: answer,
            acceptableAnswers: [answer.toLowerCase()],
            explanation: String(rec.explanation || `Expected answer: ${answer}`).slice(0, 260),
          });
        }
      });

      if (questions.length === 0) {
        warnings.push('No valid questions found in JSON. Expected { prompt, options?, answer } items.');
      }
      return { questions, warnings };
    } catch {
      warnings.push('Could not parse JSON file. Check JSON syntax.');
      return { questions: [], warnings };
    }
  }

  // 2. CSV format
  if (lower.endsWith('.csv')) {
    const rows = parseCsvRows(trimmed);
    if (rows.length === 0) {
      return { questions: [], warnings: ['CSV file has no rows.'] };
    }

    const firstRowLower = rows[0].map((c) => c.toLowerCase());
    const hasHeader =
      firstRowLower.some((c) => c.includes('prompt') || c.includes('question')) &&
      firstRowLower.some((c) => c.includes('answer'));
    const dataRows = hasHeader ? rows.slice(1) : rows;

    const questions: ActivityQuestion[] = [];
    dataRows.slice(0, 15).forEach((cols, idx) => {
      let typeHint = '';
      let prompt = '';
      let optionsRaw = '';
      let answer = '';
      let hint = '';
      let explanation = '';

      if (cols.length >= 4 && ['multiple_choice', 'mcq', 'fill_blank', 'blank'].includes(cols[0].toLowerCase())) {
        [typeHint, prompt, optionsRaw, answer, hint = '', explanation = ''] = cols;
      } else if (cols.length >= 6 && !cols[1].includes('|') && !cols[1].includes(';')) {
        // Support prompt, optA, optB, optC, optD, answer, [hint]
        prompt = cols[0];
        const opts = [cols[1], cols[2], cols[3], cols[4]].filter(Boolean);
        optionsRaw = opts.join('|');
        answer = cols[5] || '';
        hint = cols[6] || '';
        explanation = cols[7] || '';
      } else {
        prompt = cols[0] || '';
        optionsRaw = cols[1] || '';
        answer = cols[2] || '';
        hint = cols[3] || '';
        explanation = cols[4] || '';
      }

      prompt = prompt.trim().slice(0, 320);
      answer = answer.trim().slice(0, 140);
      if (!prompt || !answer) return;

      const options = optionsRaw
        .split(/[|;]/)
        .map((s) => s.trim())
        .filter(Boolean)
        .slice(0, 4);

      const forceBlank = typeHint.toLowerCase().includes('blank');
      if (!forceBlank && options.length >= 2) {
        const finalOptions = options.includes(answer) ? options : [answer, ...options.slice(0, 3)];
        questions.push({
          id: makeQuestionId(idx),
          type: 'multiple_choice',
          prompt,
          bilingualHint: (hint || 'Think through each option carefully.').slice(0, 220),
          options: finalOptions,
          correctAnswer: answer,
          explanation: (explanation || `Correct answer: ${answer}`).slice(0, 260),
        });
      } else {
        questions.push({
          id: makeQuestionId(idx),
          type: 'fill_blank',
          prompt,
          bilingualHint: (hint || 'Check spelling or units before checking.').slice(0, 220),
          blankPrefix: 'Answer:',
          blankSuffix: '',
          correctAnswer: answer,
          acceptableAnswers: [answer.toLowerCase()],
          explanation: (explanation || `Expected answer: ${answer}`).slice(0, 260),
        });
      }
    });

    if (questions.length === 0) {
      warnings.push('No valid questions imported from CSV. Download the CSV template for the exact columns.');
    }
    return { questions, warnings };
  }

  // 3. Plain text / Markdown Q&A format
  const blocks = trimmed
    .split(/\n\s*\n+/)
    .map((b) => b.trim())
    .filter(Boolean);

  const questions: ActivityQuestion[] = [];
  blocks.slice(0, 15).forEach((block, idx) => {
    const lines = block
      .split(/\r?\n/)
      .map((l) => l.trim())
      .filter(Boolean);
    if (lines.length === 0) return;

    let prompt = lines[0].replace(/^(Q\d*[:.)]\s*|\d+[.)]\s*)/i, '').trim().slice(0, 320);
    const options: string[] = [];
    let answer = '';
    let hint = '';
    let explanation = '';

    for (let i = 1; i < lines.length; i += 1) {
      const line = lines[i];
      const optMatch = line.match(/^([A-Da-d])[).:\]]\s*(.+)$/);
      if (optMatch) {
        let optText = optMatch[2].trim();
        if (optText.endsWith('*')) {
          optText = optText.slice(0, -1).trim();
          answer = optText;
        }
        options.push(optText);
        continue;
      }
      if (/^answer\s*[:=-]/i.test(line)) {
        const rawAns = line.replace(/^answer\s*[:=-]\s*/i, '').trim();
        const letterIdx = ['A', 'B', 'C', 'D'].indexOf(rawAns.toUpperCase());
        if (letterIdx >= 0 && options[letterIdx]) {
          answer = options[letterIdx];
        } else {
          answer = rawAns;
        }
        continue;
      }
      if (/^hint\s*[:=-]/i.test(line)) {
        hint = line.replace(/^hint\s*[:=-]\s*/i, '').trim();
        continue;
      }
      if (/^explanation\s*[:=-]/i.test(line)) {
        explanation = line.replace(/^explanation\s*[:=-]\s*/i, '').trim();
        continue;
      }
      if (options.length === 0 && !answer) {
        prompt = `${prompt} ${line}`.slice(0, 320);
      }
    }

    if (!prompt || !answer) return;
    if (options.length >= 2) {
      const finalOptions = options.includes(answer) ? options : [answer, ...options.slice(0, 3)];
      questions.push({
        id: makeQuestionId(idx),
        type: 'multiple_choice',
        prompt,
        bilingualHint: (hint || 'Read carefully before selecting your answer.').slice(0, 220),
        options: finalOptions.slice(0, 4),
        correctAnswer: answer,
        explanation: (explanation || `Correct answer: ${answer}`).slice(0, 260),
      });
    } else {
      questions.push({
        id: makeQuestionId(idx),
        type: 'fill_blank',
        prompt,
        bilingualHint: (hint || 'Type your answer in the blank field.').slice(0, 220),
        blankPrefix: 'Answer:',
        blankSuffix: '',
        correctAnswer: answer,
        acceptableAnswers: [answer.toLowerCase()],
        explanation: (explanation || `Expected answer: ${answer}`).slice(0, 260),
      });
    }
  });

  if (questions.length === 0) {
    warnings.push(
      'Could not detect Q:/Answer: blocks in the text file. You can still attach the file as a worksheet and add questions below.'
    );
  }
  return { questions, warnings };
}
