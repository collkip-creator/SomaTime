import { useEffect, useMemo, useRef, useState } from 'react';
import {
  BookOpen,
  Building2,
  CheckCircle2,
  ChevronDown,
  ChevronRight,
  Download,
  FileText,
  GraduationCap,
  KeyRound,
  LogOut,
  Plus,
  Search,
  Send,
  Trash2,
  Upload,
  UserPlus,
  Users,
} from 'lucide-react';
import {
  ALL_LEARNING_AREAS,
  CBC_CURRICULUM_TREE,
  CBC_RUBRIC_META,
  CLASS_STREAMS,
  getCurriculumStats,
  getLevelGroupForGrade,
  getStrandsForSubject,
} from '../data/cbcCurriculum';
import type {
  ActivityQuestion,
  CBCRubric,
  ClassStudentRecord,
  LearningArea,
  TeacherAccount,
  TeacherAssignment,
} from '../types/somatime';
import {
  downloadAssignmentCsvTemplate,
  formatFileSize,
  isSafeAttachmentUrl,
  parseUploadedQuestionFile,
} from '../utils/assignmentParser';

interface TeacherPortalProps {
  teacherAccount: TeacherAccount | null;
  assignments: TeacherAssignment[];
  classRoster: ClassStudentRecord[];
  onOpenTeacherAuth: () => void;
  onLogoutTeacher: () => void;
  onPushAssignment: (input: {
    classStream: string;
    school?: string;
    grade: number;
    learningArea: LearningArea;
    strand: string;
    subStrand: string;
    isCustom?: boolean;
    customTitle?: string;
    instructions?: string;
    attachmentName?: string;
    attachmentUrl?: string;
    attachmentMimeType?: string;
    attachmentSizeBytes?: number;
    customQuestions?: ActivityQuestion[];
  }) => void;
  onJumpToChildMode: () => void;
}

export function TeacherPortal({
  teacherAccount,
  assignments,
  classRoster,
  onOpenTeacherAuth,
  onLogoutTeacher,
  onPushAssignment,
  onJumpToChildMode,
}: TeacherPortalProps) {
  const curriculumStats = useMemo(() => getCurriculumStats(), []);
  const defaultArea: LearningArea = teacherAccount?.subjects[0] ?? 'Mathematics';
  const defaultStrands = getStrandsForSubject(defaultArea);
  const defaultFirstStrand = defaultStrands[0]?.strand ?? 'Numbers';
  const defaultFirstSub = defaultStrands[0]?.subStrands[0]?.subStrand ?? 'Fractions in Daily Life';

  const [schoolName, setSchoolName] = useState(
    teacherAccount?.school ?? 'Nairobi Primary School'
  );
  const [targetGrade, setTargetGrade] = useState<number>(teacherAccount?.grade ?? 6);
  const [classStream, setClassStream] = useState(
    teacherAccount?.classStream ?? CLASS_STREAMS[0].stream
  );
  const [filterStrandsByGrade, setFilterStrandsByGrade] = useState(false);
  const [selectedArea, setSelectedArea] = useState<LearningArea>(defaultArea);
  const [selectedStrand, setSelectedStrand] = useState(defaultFirstStrand);
  const [selectedSubStrand, setSelectedSubStrand] = useState(defaultFirstSub);
  const [useCustomStrand, setUseCustomStrand] = useState(false);
  const [customStrand, setCustomStrand] = useState('');
  const [customSubStrand, setCustomSubStrand] = useState('');

  // Full CBC Curriculum Directory Explorer state
  const [catalogSearch, setCatalogSearch] = useState('');
  const [expandedSubject, setExpandedSubject] = useState<LearningArea | null>(defaultArea);

  const [rosterStreamFilter, setRosterStreamFilter] = useState(CLASS_STREAMS[0].stream);
  const [rubricFilter, setRubricFilter] = useState<CBCRubric | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [justPushedTitle, setJustPushedTitle] = useState<string>('');

  // Custom Assignment Authoring & File Upload state
  const [composerMode, setComposerMode] = useState<'catalog' | 'custom'>('catalog');
  const [customTitle, setCustomTitle] = useState('');
  const [customInstructions, setCustomInstructions] = useState('');
  const [customQuestions, setCustomQuestions] = useState<ActivityQuestion[]>([]);
  const [uploadedFileMeta, setUploadedFileMeta] = useState<{
    name: string;
    url: string;
    mimeType: string;
    sizeBytes: number;
  } | null>(null);
  const [uploadBusy, setUploadBusy] = useState(false);
  const [uploadFeedback, setUploadFeedback] = useState('');
  const [uploadError, setUploadError] = useState('');
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // New Question Builder inputs
  const [draftType, setDraftType] = useState<'multiple_choice' | 'fill_blank'>('multiple_choice');
  const [draftPrompt, setDraftPrompt] = useState('');
  const [draftOptions, setDraftOptions] = useState<[string, string, string, string]>(['', '', '', '']);
  const [draftCorrectIndex, setDraftCorrectIndex] = useState(0);
  const [draftBlankAnswer, setDraftBlankAnswer] = useState('');
  const [draftHint, setDraftHint] = useState('');
  const [draftExplanation, setDraftExplanation] = useState('');
  const [builderError, setBuilderError] = useState('');

  // Sync defaults whenever the signed-in teacher changes
  useEffect(() => {
    if (!teacherAccount) return;
    setSchoolName(teacherAccount.school);
    setTargetGrade(teacherAccount.grade);
    setClassStream(teacherAccount.classStream);
    const firstArea = teacherAccount.subjects[0] ?? 'Mathematics';
    setSelectedArea(firstArea);
    setExpandedSubject(firstArea);
    const grouped = getStrandsForSubject(firstArea);
    if (grouped.length > 0) {
      setSelectedStrand(grouped[0].strand);
      setSelectedSubStrand(grouped[0].subStrands[0]?.subStrand ?? '');
    }
    setUseCustomStrand(false);
  }, [teacherAccount]);

  const mySubjects = teacherAccount?.subjects ?? [];
  const otherSubjects = useMemo(
    () => ALL_LEARNING_AREAS.filter((a) => !mySubjects.includes(a)),
    [mySubjects]
  );

  // Cascading Strands for selectedArea (optionally filtered by targetGrade)
  const groupedStrands = useMemo(() => {
    const filtered = getStrandsForSubject(
      selectedArea,
      filterStrandsByGrade ? targetGrade : undefined
    );
    return filtered.length > 0 ? filtered : getStrandsForSubject(selectedArea);
  }, [selectedArea, filterStrandsByGrade, targetGrade]);

  const subStrandOptions = useMemo(() => {
    const match = groupedStrands.find((s) => s.strand === selectedStrand);
    return match?.subStrands ?? groupedStrands[0]?.subStrands ?? [];
  }, [groupedStrands, selectedStrand]);

  // Keep selectedStrand and selectedSubStrand valid when grade filter changes
  useEffect(() => {
    if (groupedStrands.length === 0) return;
    const strandGroup = groupedStrands.find((s) => s.strand === selectedStrand);
    if (!strandGroup) {
      const nextStrand = groupedStrands[0];
      setSelectedStrand(nextStrand.strand);
      setSelectedSubStrand(nextStrand.subStrands[0]?.subStrand ?? '');
    } else if (!strandGroup.subStrands.some((sub) => sub.subStrand === selectedSubStrand)) {
      setSelectedSubStrand(strandGroup.subStrands[0]?.subStrand ?? '');
    }
  }, [groupedStrands, selectedStrand, selectedSubStrand]);

  const handleSelectArea = (area: LearningArea) => {
    setSelectedArea(area);
    setExpandedSubject(area);
    const grouped = getStrandsForSubject(
      area,
      filterStrandsByGrade ? targetGrade : undefined
    );
    const list = grouped.length > 0 ? grouped : getStrandsForSubject(area);
    if (list.length > 0) {
      setSelectedStrand(list[0].strand);
      setSelectedSubStrand(list[0].subStrands[0]?.subStrand ?? '');
    }
  };

  const handleSelectStrand = (strandName: string) => {
    setSelectedStrand(strandName);
    const match = groupedStrands.find((s) => s.strand === strandName);
    if (match && match.subStrands.length > 0) {
      setSelectedSubStrand(match.subStrands[0].subStrand);
    }
  };

  const handleQuickPickFromMatrix = (
    area: LearningArea,
    strand: string,
    subStrand: string
  ) => {
    setUseCustomStrand(false);
    setSelectedArea(area);
    setSelectedStrand(strand);
    setSelectedSubStrand(subStrand);
  };

  const filteredCurriculumSubjects = useMemo(() => {
    const q = catalogSearch.trim().toLowerCase();
    return CBC_CURRICULUM_TREE.map((subj) => {
      const areaMatches = subj.learningArea.toLowerCase().includes(q);
      const matchingStrands = subj.strands
        .map((st) => {
          const strandMatches = st.strand.toLowerCase().includes(q);
          const matchingSubs = st.subStrands.filter(
            (sub) =>
              !q ||
              areaMatches ||
              strandMatches ||
              sub.subStrand.toLowerCase().includes(q)
          );
          return { ...st, subStrands: matchingSubs };
        })
        .filter((st) => st.subStrands.length > 0);
      return { ...subj, strands: matchingStrands };
    }).filter((subj) => subj.strands.length > 0);
  }, [catalogSearch]);

  const handleFilePicked = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadError('');
    setUploadFeedback('');

    if (file.size > 5 * 1024 * 1024) {
      setUploadError('File exceeds 5 MB maximum upload size.');
      e.target.value = '';
      return;
    }

    const lower = file.name.toLowerCase();
    const isTextQuestionFile =
      lower.endsWith('.csv') || lower.endsWith('.json') || lower.endsWith('.txt');

    setUploadBusy(true);
    try {
      if (isTextQuestionFile) {
        const text = await file.text();
        const parsed = parseUploadedQuestionFile(file.name, text);
        if (parsed.questions.length > 0) {
          setCustomQuestions((prev) => [...prev, ...parsed.questions].slice(0, 15));
          setUploadFeedback(
            `Imported ${parsed.questions.length} question(s) from "${file.name}". Review or edit below before publishing.`
          );
        } else if (parsed.warnings.length > 0) {
          setUploadFeedback(parsed.warnings[0]);
        }
      }

      const dataBase64 = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : '');
        reader.onerror = () => reject(new Error('Could not read selected file.'));
        reader.readAsDataURL(file);
      });

      const res = await fetch('/api/assignments/upload', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fileName: file.name,
          mimeType: file.type || 'application/octet-stream',
          dataBase64,
        }),
      });
      const json = (await res.json()) as {
        ok?: boolean;
        error?: string;
        fileName?: string;
        url?: string;
        mimeType?: string;
        sizeBytes?: number;
      };

      if (!res.ok || !json.ok || !json.url) {
        setUploadError(json.error || 'Upload failed. Check file format and try again.');
        return;
      }

      setUploadedFileMeta({
        name: json.fileName || file.name,
        url: json.url,
        mimeType: json.mimeType || file.type || 'application/octet-stream',
        sizeBytes: json.sizeBytes || file.size,
      });

      if (!customTitle.trim()) {
        const baseName = file.name.replace(/\.[^.]+$/, '').replace(/[_-]+/g, ' ').trim();
        if (baseName) setCustomTitle(baseName.slice(0, 100));
      }

      if (!isTextQuestionFile) {
        setUploadFeedback(
          `Uploaded "${json.fileName || file.name}" (${formatFileSize(json.sizeBytes || file.size)}). Add questions below or publish as a worksheet assignment.`
        );
      }
    } catch {
      setUploadError('Unable to upload file right now. Check connection and retry.');
    } finally {
      setUploadBusy(false);
      e.target.value = '';
    }
  };

  const handleAddDraftQuestion = () => {
    setBuilderError('');
    const prompt = draftPrompt.trim();
    if (!prompt) {
      setBuilderError('Enter a question prompt before adding.');
      return;
    }
    if (customQuestions.length >= 15) {
      setBuilderError('Maximum 15 custom questions per assignment reached.');
      return;
    }

    if (draftType === 'multiple_choice') {
      const cleaned = draftOptions.map((o) => o.trim());
      const nonEmpty = cleaned.filter(Boolean);
      const chosenAnswer = cleaned[draftCorrectIndex] || '';
      if (nonEmpty.length < 2) {
        setBuilderError('Provide at least 2 answer options for a multiple-choice question.');
        return;
      }
      if (!chosenAnswer) {
        setBuilderError('Select a non-empty option as the correct answer.');
        return;
      }
      const newQ: ActivityQuestion = {
        id: `tq-${Date.now()}-${customQuestions.length + 1}`,
        type: 'multiple_choice',
        prompt,
        bilingualHint: draftHint.trim() || 'Think carefully through each choice.',
        options: nonEmpty,
        correctAnswer: chosenAnswer,
        explanation: draftExplanation.trim() || `Correct answer: ${chosenAnswer}`,
      };
      setCustomQuestions((prev) => [...prev, newQ]);
    } else {
      const ans = draftBlankAnswer.trim();
      if (!ans) {
        setBuilderError('Enter the expected answer for the fill-in-the-blank question.');
        return;
      }
      const newQ: ActivityQuestion = {
        id: `tq-${Date.now()}-${customQuestions.length + 1}`,
        type: 'fill_blank',
        prompt,
        bilingualHint: draftHint.trim() || 'Type your answer carefully.',
        blankPrefix: 'Answer:',
        blankSuffix: '',
        correctAnswer: ans,
        acceptableAnswers: [ans.toLowerCase()],
        explanation: draftExplanation.trim() || `Expected answer: ${ans}`,
      };
      setCustomQuestions((prev) => [...prev, newQ]);
    }

    setDraftPrompt('');
    setDraftOptions(['', '', '', '']);
    setDraftCorrectIndex(0);
    setDraftBlankAnswer('');
    setDraftHint('');
    setDraftExplanation('');
  };

  const handlePush = (e: React.FormEvent) => {
    e.preventDefault();
    setBuilderError('');
    const finalStrand = useCustomStrand
      ? customStrand.trim() || selectedStrand
      : selectedStrand;
    const finalSubStrand = useCustomStrand
      ? customSubStrand.trim() || selectedSubStrand
      : selectedSubStrand;

    if (composerMode === 'custom') {
      const titleClean = customTitle.trim();
      if (!titleClean) {
        setBuilderError('Enter a title for your custom assignment.');
        return;
      }
      if (customQuestions.length === 0 && !uploadedFileMeta) {
        setBuilderError('Add at least 1 custom question or upload a worksheet/question file.');
        return;
      }

      onPushAssignment({
        classStream: classStream.trim() || `Grade ${targetGrade} East - ${schoolName}`,
        school: schoolName.trim() || 'Nairobi Primary School',
        grade: targetGrade,
        learningArea: selectedArea,
        strand: finalStrand,
        subStrand: finalSubStrand,
        isCustom: true,
        customTitle: titleClean,
        instructions: customInstructions.trim(),
        attachmentName: uploadedFileMeta?.name,
        attachmentUrl: uploadedFileMeta?.url,
        attachmentMimeType: uploadedFileMeta?.mimeType,
        attachmentSizeBytes: uploadedFileMeta?.sizeBytes,
        customQuestions,
      });

      setJustPushedTitle(
        `Custom Assignment "${titleClean}" (${customQuestions.length} Qs${
          uploadedFileMeta ? ` · ${uploadedFileMeta.name}` : ''
        }) pushed to Grade ${targetGrade} at ${schoolName}`
      );
      setCustomTitle('');
      setCustomInstructions('');
      setCustomQuestions([]);
      setUploadedFileMeta(null);
      setUploadFeedback('');
      return;
    }

    onPushAssignment({
      classStream: classStream.trim() || `Grade ${targetGrade} East - ${schoolName}`,
      school: schoolName.trim() || 'Nairobi Primary School',
      grade: targetGrade,
      learningArea: selectedArea,
      strand: finalStrand,
      subStrand: finalSubStrand,
    });
    setJustPushedTitle(
      `${selectedArea} · ${finalStrand} (${finalSubStrand}) pushed to Grade ${targetGrade} at ${schoolName}`
    );
  };

  const streamMeta = useMemo(
    () => CLASS_STREAMS.find((s) => s.stream === rosterStreamFilter) ?? CLASS_STREAMS[0],
    [rosterStreamFilter]
  );

  const streamStudents = useMemo(() => {
    return classRoster.filter((s) => s.classStream === rosterStreamFilter);
  }, [classRoster, rosterStreamFilter]);

  const filteredStudents = useMemo(() => {
    return streamStudents.filter((s) => {
      const matchesRubric = rubricFilter === 'ALL' || s.rubric === rubricFilter;
      const q = searchQuery.trim().toLowerCase();
      const matchesQuery =
        !q ||
        s.name.toLowerCase().includes(q) ||
        s.admissionNo.toLowerCase().includes(q);
      return matchesRubric && matchesQuery;
    });
  }, [streamStudents, rubricFilter, searchQuery]);

  const rubricCounts = useMemo(() => {
    const counts: Record<CBCRubric, number> = { EE: 0, ME: 0, AE: 0, BE: 0 };
    streamStudents.forEach((s) => {
      counts[s.rubric] += 1;
    });
    return counts;
  }, [streamStudents]);

  return (
    <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:py-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="font-display text-2xl font-bold tracking-tight text-slate-900 sm:text-3xl">
            Teacher &amp; School Admin Portal
          </h1>
          <p className="mt-1 text-sm text-slate-600">
            Register with your school &amp; CBC subjects, sign in, and push priority strand
            assignments directly to learners&apos; SomaTime screens.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {teacherAccount ? (
            <>
              <button
                type="button"
                onClick={onOpenTeacherAuth}
                className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-bold text-slate-800 hover:bg-slate-50"
              >
                <UserPlus className="h-4 w-4 text-teal-700" />
                <span>Switch / Register Teacher</span>
              </button>
              <button
                type="button"
                onClick={onLogoutTeacher}
                className="flex min-h-[42px] items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-rose-50 hover:text-rose-700"
              >
                <LogOut className="h-4 w-4" />
                <span>Sign Out</span>
              </button>
            </>
          ) : (
            <button
              type="button"
              onClick={onOpenTeacherAuth}
              className="flex min-h-[44px] items-center gap-2 rounded-2xl bg-amber-600 px-4 py-2 text-xs font-bold text-white hover:bg-amber-700"
            >
              <UserPlus className="h-4 w-4" />
              <span>Register or Sign In as Teacher</span>
            </button>
          )}
        </div>
      </div>

      {/* Active Teacher Profile Banner OR Registration Callout */}
      {teacherAccount ? (
        <div className="mt-5 rounded-3xl border border-teal-200 bg-teal-50/70 p-4 sm:p-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="space-y-1">
              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-lg bg-teal-700 px-2.5 py-1 font-mono text-xs font-bold text-white">
                  {teacherAccount.teacherCode}
                </span>
                <span className="font-display text-base font-extrabold text-slate-900">
                  {teacherAccount.fullName}
                </span>
                {teacherAccount.tscNumber && (
                  <span className="font-mono text-xs font-semibold text-slate-600">
                    · {teacherAccount.tscNumber}
                  </span>
                )}
              </div>
              <p className="flex flex-wrap items-center gap-2 text-xs font-medium text-slate-700">
                <Building2 className="h-3.5 w-3.5 text-teal-700" />
                <span>{teacherAccount.school}</span>
                <span aria-hidden="true">·</span>
                <span>
                  Grade {teacherAccount.grade} ({getLevelGroupForGrade(teacherAccount.grade)})
                </span>
                <span aria-hidden="true">·</span>
                <span>{teacherAccount.classStream}</span>
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-1.5">
              <span className="text-[11px] font-bold text-teal-900">Taught Subjects:</span>
              {teacherAccount.subjects.map((subj) => (
                <span
                  key={subj}
                  className="rounded-lg border border-teal-300 bg-white px-2.5 py-1 text-xs font-bold text-teal-900"
                >
                  {subj}
                </span>
              ))}
            </div>
          </div>
        </div>
      ) : (
        <div className="mt-5 flex flex-wrap items-center justify-between gap-4 rounded-3xl border border-amber-200 bg-amber-50 p-4 sm:p-5">
          <div className="flex items-start gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-amber-600 text-white shrink-0">
              <KeyRound className="h-5 w-5" />
            </div>
            <div>
              <h2 className="font-display text-sm font-bold text-slate-900">
                No Teacher Signed In Yet
              </h2>
              <p className="mt-0.5 text-xs text-slate-700">
                Register your Teacher Name, School, Primary Grade, and CBC Subject Areas to get a
                Teacher Code (<span className="font-mono font-bold">TCH-XXXX</span>) so assignments
                sync automatically to learners in your school and grade.
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onOpenTeacherAuth}
            className="min-h-[42px] rounded-xl bg-amber-600 px-4 py-2 text-xs font-bold text-white hover:bg-amber-700 shrink-0"
          >
            Register / Sign In Now →
          </button>
        </div>
      )}

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Left: Homework Push & Active Assignments */}
        <section aria-label="Homework push controls" className="space-y-6 lg:col-span-5">
          <form
            onSubmit={handlePush}
            className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6"
          >
            <div className="flex items-center gap-2.5">
              <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-teal-50 text-teal-700">
                <GraduationCap className="h-5 w-5" />
              </div>
              <div>
                <h2 className="font-display text-lg font-bold text-slate-900">
                  Push CBC Priority Assignment
                </h2>
                <p className="text-xs text-slate-600">
                  Pick a CBC strand or author &amp; upload your own custom assignment.
                </p>
              </div>
            </div>

            {/* Mode Switcher: CBC Quick Push vs Custom Assignment & Upload */}
            <div className="mt-4 grid grid-cols-2 gap-1.5 rounded-2xl bg-slate-100 p-1">
              <button
                type="button"
                onClick={() => {
                  setComposerMode('catalog');
                  setBuilderError('');
                }}
                className={`min-h-[38px] rounded-xl px-3 py-1.5 text-xs font-bold transition-colors whitespace-nowrap ${
                  composerMode === 'catalog'
                    ? 'bg-white text-teal-900 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                CBC Strand Pick
              </button>
              <button
                type="button"
                onClick={() => {
                  setComposerMode('custom');
                  setBuilderError('');
                }}
                className={`min-h-[38px] rounded-xl px-3 py-1.5 text-xs font-bold transition-colors whitespace-nowrap ${
                  composerMode === 'custom'
                    ? 'bg-white text-teal-900 shadow-sm'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                Custom &amp; Upload
              </button>
            </div>

            <div className="mt-5 space-y-4">
              <div>
                <label
                  htmlFor="teacher-school-input"
                  className="block text-xs font-semibold text-slate-700"
                >
                  School Name (Synced to Learners at This School)
                </label>
                <input
                  id="teacher-school-input"
                  type="text"
                  required
                  value={schoolName}
                  onChange={(e) => setSchoolName(e.target.value)}
                  className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                <div>
                  <label
                    htmlFor="grade-select"
                    className="block text-xs font-semibold text-slate-700"
                  >
                    Target Grade (1–12)
                  </label>
                  <select
                    id="grade-select"
                    value={targetGrade}
                    onChange={(e) => {
                      const g = Number(e.target.value);
                      setTargetGrade(g);
                      setClassStream(`Grade ${g} East - ${schoolName}`);
                    }}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                  >
                    {Array.from({ length: 12 }, (_, idx) => idx + 1).map((g) => (
                      <option key={g} value={g}>
                        Grade {g} ({getLevelGroupForGrade(g)})
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label
                    htmlFor="stream-input"
                    className="block text-xs font-semibold text-slate-700"
                  >
                    Class Stream
                  </label>
                  <input
                    id="stream-input"
                    type="text"
                    required
                    value={classStream}
                    onChange={(e) => setClassStream(e.target.value)}
                    className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between gap-2">
                  <label
                    htmlFor="area-select"
                    className="block text-xs font-semibold text-slate-700"
                  >
                    1. Subject / CBC Learning Area ({ALL_LEARNING_AREAS.length} loaded)
                  </label>
                  <label className="flex cursor-pointer items-center gap-1.5 text-[11px] font-semibold text-slate-600">
                    <input
                      type="checkbox"
                      checked={filterStrandsByGrade}
                      onChange={(e) => setFilterStrandsByGrade(e.target.checked)}
                      className="rounded border-slate-300 text-teal-700 focus:ring-teal-600"
                    />
                    <span>Grade {targetGrade} only</span>
                  </label>
                </div>
                <select
                  id="area-select"
                  value={selectedArea}
                  onChange={(e) => handleSelectArea(e.target.value as LearningArea)}
                  className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                >
                  {mySubjects.length > 0 ? (
                    <>
                      <optgroup label="Your Registered Subject Areas">
                        {mySubjects.map((area) => (
                          <option key={`my-${area}`} value={area}>
                            ★ {area}
                          </option>
                        ))}
                      </optgroup>
                      {otherSubjects.length > 0 && (
                        <optgroup label="All Other CBC Learning Areas">
                          {otherSubjects.map((area) => (
                            <option key={`other-${area}`} value={area}>
                              {area}
                            </option>
                          ))}
                        </optgroup>
                      )}
                    </>
                  ) : (
                    ALL_LEARNING_AREAS.map((area) => (
                      <option key={area} value={area}>
                        {area}
                      </option>
                    ))
                  )}
                </select>
              </div>

              <div>
                <div className="flex items-center justify-between">
                  <label
                    htmlFor="strand-select"
                    className="block text-xs font-semibold text-slate-700"
                  >
                    2. CBC Strand ({groupedStrands.length} strands)
                  </label>
                  <button
                    type="button"
                    onClick={() => setUseCustomStrand((v) => !v)}
                    className="text-[11px] font-bold text-teal-700 hover:underline"
                  >
                    {useCustomStrand ? 'Pick from CBC list' : '+ Custom Strand'}
                  </button>
                </div>

                {!useCustomStrand ? (
                  <div className="mt-1.5 space-y-3">
                    <select
                      id="strand-select"
                      value={selectedStrand}
                      onChange={(e) => handleSelectStrand(e.target.value)}
                      className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                    >
                      {groupedStrands.map((st) => (
                        <option key={st.strand} value={st.strand}>
                          {st.strand} ({st.subStrands.length} sub-strands)
                        </option>
                      ))}
                    </select>

                    <div>
                      <label
                        htmlFor="substrand-select"
                        className="block text-xs font-semibold text-slate-700"
                      >
                        3. CBC Sub-strand ({subStrandOptions.length} topics)
                      </label>
                      <select
                        id="substrand-select"
                        value={selectedSubStrand}
                        onChange={(e) => setSelectedSubStrand(e.target.value)}
                        className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                      >
                        {subStrandOptions.map((sub) => (
                          <option key={sub.subStrand} value={sub.subStrand}>
                            {sub.subStrand} ({sub.levelHint})
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>
                ) : (
                  <div className="mt-1.5 grid grid-cols-1 gap-2 sm:grid-cols-2">
                    <input
                      type="text"
                      required
                      placeholder="Strand (e.g. Numbers)"
                      value={customStrand}
                      onChange={(e) => setCustomStrand(e.target.value)}
                      className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                    />
                    <input
                      type="text"
                      required
                      placeholder="Sub-strand (e.g. Decimals)"
                      value={customSubStrand}
                      onChange={(e) => setCustomSubStrand(e.target.value)}
                      className="w-full rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                    />
                  </div>
                )}
              </div>

              {composerMode === 'custom' && (
                <div className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50/80 p-4">
                  <div>
                    <h3 className="font-display text-sm font-bold text-slate-900">
                      Custom Assignment Details &amp; File Upload
                    </h3>
                    <p className="mt-0.5 text-[11px] text-slate-600">
                      Upload a CSV/JSON/TXT question bank for instant import, or attach a PDF, image,
                      or DOCX worksheet for learners.
                    </p>
                  </div>

                  <div>
                    <label
                      htmlFor="custom-assign-title"
                      className="block text-xs font-semibold text-slate-700"
                    >
                      Assignment Title
                    </label>
                    <input
                      id="custom-assign-title"
                      type="text"
                      value={customTitle}
                      onChange={(e) => setCustomTitle(e.target.value)}
                      placeholder="e.g. Week 4 Fractions & M-Pesa Word Problems"
                      className="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-sm font-medium text-slate-900 focus:border-teal-600 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label
                      htmlFor="custom-assign-instructions"
                      className="block text-xs font-semibold text-slate-700"
                    >
                      Instructions for Learner (Optional)
                    </label>
                    <textarea
                      id="custom-assign-instructions"
                      rows={2}
                      value={customInstructions}
                      onChange={(e) => setCustomInstructions(e.target.value)}
                      placeholder="Write clear steps or revision notes..."
                      className="mt-1 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs text-slate-900 focus:border-teal-600 focus:outline-none"
                    />
                  </div>

                  {/* File Upload Zone */}
                  <div className="rounded-xl border border-dashed border-teal-300 bg-white p-3.5">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <p className="text-xs font-bold text-slate-900">
                          Upload Worksheet or Question File
                        </p>
                        <p className="text-[11px] text-slate-500">
                          PDF, PNG/JPG/WEBP, DOCX, CSV, JSON, or TXT (max 5 MB)
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={downloadAssignmentCsvTemplate}
                        className="inline-flex items-center gap-1 text-[11px] font-bold text-teal-700 hover:underline"
                      >
                        <Download className="h-3.5 w-3.5" />
                        <span>CSV Template</span>
                      </button>
                    </div>

                    <input
                      ref={fileInputRef}
                      type="file"
                      accept=".pdf,.png,.jpg,.jpeg,.webp,.csv,.json,.txt,.docx"
                      onChange={(e) => void handleFilePicked(e)}
                      className="sr-only"
                    />
                    <button
                      type="button"
                      disabled={uploadBusy}
                      onClick={() => fileInputRef.current?.click()}
                      className="mt-2.5 flex min-h-[42px] w-full items-center justify-center gap-2 rounded-xl border border-teal-200 bg-teal-50/70 px-3 py-2 text-xs font-bold text-teal-900 transition-colors hover:bg-teal-100 disabled:opacity-60"
                    >
                      <Upload className="h-4 w-4" />
                      <span>
                        {uploadBusy ? 'Uploading & Reading File...' : 'Choose File to Upload'}
                      </span>
                    </button>

                    {uploadedFileMeta && (
                      <div className="mt-2.5 flex items-center justify-between gap-2 rounded-xl border border-emerald-200 bg-emerald-50/70 px-3 py-2 text-xs">
                        <div className="flex min-w-0 items-center gap-2">
                          <FileText className="h-4 w-4 text-emerald-700 shrink-0" />
                          <div className="min-w-0">
                            <p className="truncate font-semibold text-slate-900">
                              {uploadedFileMeta.name}
                            </p>
                            <p className="text-[11px] text-slate-600">
                              {formatFileSize(uploadedFileMeta.sizeBytes)} · Attached
                            </p>
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => setUploadedFileMeta(null)}
                          aria-label="Remove uploaded file"
                          className="rounded-lg p-1.5 text-slate-500 hover:bg-white hover:text-rose-700"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    )}

                    {uploadFeedback && (
                      <p className="mt-2 text-[11px] font-medium text-emerald-800">
                        {uploadFeedback}
                      </p>
                    )}
                    {uploadError && (
                      <p className="mt-2 text-[11px] font-semibold text-rose-700">{uploadError}</p>
                    )}
                  </div>

                  {/* Interactive Question Authoring */}
                  <div className="space-y-3 border-t border-slate-200 pt-3">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-xs font-bold text-slate-900">
                        Custom Questions ({customQuestions.length}/15)
                      </p>
                      <div className="flex items-center gap-1 rounded-lg bg-white p-0.5 border border-slate-200">
                        <button
                          type="button"
                          onClick={() => setDraftType('multiple_choice')}
                          className={`rounded-md px-2 py-1 text-[11px] font-semibold ${
                            draftType === 'multiple_choice'
                              ? 'bg-teal-700 text-white'
                              : 'text-slate-600'
                          }`}
                        >
                          Multiple Choice
                        </button>
                        <button
                          type="button"
                          onClick={() => setDraftType('fill_blank')}
                          className={`rounded-md px-2 py-1 text-[11px] font-semibold ${
                            draftType === 'fill_blank' ? 'bg-teal-700 text-white' : 'text-slate-600'
                          }`}
                        >
                          Fill in Blank
                        </button>
                      </div>
                    </div>

                    <input
                      type="text"
                      value={draftPrompt}
                      onChange={(e) => setDraftPrompt(e.target.value)}
                      placeholder="Question prompt (e.g. What is 25% of KES 400?)"
                      className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 focus:border-teal-600 focus:outline-none"
                    />

                    {draftType === 'multiple_choice' ? (
                      <div className="space-y-1.5">
                        <p className="text-[11px] font-medium text-slate-600">
                          Options (select the circle for the correct answer):
                        </p>
                        {draftOptions.map((opt, idx) => (
                          <div key={idx} className="flex items-center gap-2">
                            <input
                              type="radio"
                              name="draft-correct-option"
                              checked={draftCorrectIndex === idx}
                              onChange={() => setDraftCorrectIndex(idx)}
                              aria-label={`Mark option ${idx + 1} as correct`}
                              className="h-4 w-4 text-teal-700 focus:ring-teal-600"
                            />
                            <input
                              type="text"
                              value={opt}
                              onChange={(e) => {
                                const next: [string, string, string, string] = [...draftOptions] as [
                                  string,
                                  string,
                                  string,
                                  string,
                                ];
                                next[idx] = e.target.value;
                                setDraftOptions(next);
                              }}
                              placeholder={`Option ${idx + 1}${idx < 2 ? ' (required)' : ' (optional)'}`}
                              className="flex-1 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-900 focus:border-teal-600 focus:outline-none"
                            />
                          </div>
                        ))}
                      </div>
                    ) : (
                      <input
                        type="text"
                        value={draftBlankAnswer}
                        onChange={(e) => setDraftBlankAnswer(e.target.value)}
                        placeholder="Expected correct answer (e.g. 100 or Photosynthesis)"
                        className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs text-slate-900 focus:border-teal-600 focus:outline-none"
                      />
                    )}

                    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                      <input
                        type="text"
                        value={draftHint}
                        onChange={(e) => setDraftHint(e.target.value)}
                        placeholder="Study hint (optional)"
                        className="w-full rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-900 focus:border-teal-600 focus:outline-none"
                      />
                      <input
                        type="text"
                        value={draftExplanation}
                        onChange={(e) => setDraftExplanation(e.target.value)}
                        placeholder="Feedback explanation (optional)"
                        className="w-full rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs text-slate-900 focus:border-teal-600 focus:outline-none"
                      />
                    </div>

                    {builderError && (
                      <p className="text-[11px] font-semibold text-rose-700">{builderError}</p>
                    )}

                    <button
                      type="button"
                      onClick={handleAddDraftQuestion}
                      className="flex min-h-[40px] w-full items-center justify-center gap-1.5 rounded-xl border border-slate-300 bg-white px-3 py-2 text-xs font-bold text-slate-800 hover:bg-slate-100"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      <span>Add Question to Assignment</span>
                    </button>

                    {customQuestions.length > 0 && (
                      <div className="space-y-2 pt-1">
                        {customQuestions.map((q, idx) => (
                          <div
                            key={q.id}
                            className="flex items-start justify-between gap-2 rounded-xl border border-slate-200 bg-white p-2.5 text-xs"
                          >
                            <div className="min-w-0">
                              <p className="font-semibold text-slate-900">
                                <span className="font-mono text-teal-800">
                                  {String(idx + 1).padStart(2, '0')}.
                                </span>{' '}
                                {q.prompt}
                              </p>
                              <p className="mt-0.5 text-[11px] text-slate-500">
                                {q.type === 'multiple_choice' ? 'Multiple Choice' : 'Fill in Blank'} ·
                                Answer: <span className="font-semibold">{q.correctAnswer}</span>
                              </p>
                            </div>
                            <button
                              type="button"
                              onClick={() =>
                                setCustomQuestions((prev) => prev.filter((item) => item.id !== q.id))
                              }
                              aria-label={`Remove question ${idx + 1}`}
                              className="rounded-lg p-1 text-slate-400 hover:bg-rose-50 hover:text-rose-700 shrink-0"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}

              <button
                type="submit"
                className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-teal-700 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-teal-800"
              >
                <Send className="h-4 w-4" />
                <span>
                  {composerMode === 'custom'
                    ? 'Publish Custom Assignment & Upload'
                    : 'Push Assignment to Students'}
                </span>
              </button>
            </div>

            {justPushedTitle && (
              <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-3.5 text-xs text-emerald-950">
                <div className="flex items-start gap-2">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 text-emerald-700 shrink-0" />
                  <div className="flex-1">
                    <p className="font-bold">Synced to Cloud &amp; Learner Devices!</p>
                    <p className="mt-0.5">{justPushedTitle}</p>
                    <button
                      type="button"
                      onClick={onJumpToChildMode}
                      className="mt-2 font-bold text-teal-800 underline hover:text-teal-950"
                    >
                      Preview Priority Notice in Child Mode →
                    </button>
                  </div>
                </div>
              </div>
            )}
          </form>

          {/* Pushed Assignments Feed */}
          <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
            <h3 className="font-display text-base font-bold text-slate-900">
              Active Priority Assignments
            </h3>
            <div className="mt-3 divide-y divide-slate-100">
              {assignments.map((assign) => (
                <div key={assign.id} className="py-3 first:pt-0 last:pb-0">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-bold text-slate-900">{assign.title}</p>
                      <p className="mt-0.5 text-xs text-slate-600">
                        {assign.school ? `${assign.school} · ` : ''}
                        Grade {assign.grade} · {assign.learningArea} · {assign.strand}
                      </p>
                      {(assign.isCustom ||
                        assign.customQuestions?.length ||
                        assign.attachmentName) && (
                        <p className="mt-0.5 text-[11px] font-medium text-slate-700">
                          Custom Task
                          {assign.customQuestions?.length
                            ? ` · ${assign.customQuestions.length} teacher question(s)`
                            : ''}
                          {assign.attachmentName ? ` · File: ${assign.attachmentName}` : ''}
                        </p>
                      )}
                      {assign.attachmentUrl && isSafeAttachmentUrl(assign.attachmentUrl) && (
                        <a
                          href={assign.attachmentUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="mt-1 inline-flex items-center gap-1 text-[11px] font-bold text-teal-700 hover:underline"
                        >
                          <FileText className="h-3 w-3" />
                          <span>Open Uploaded Material</span>
                        </a>
                      )}
                      {assign.teacherName && (
                        <p className="mt-0.5 text-[11px] font-medium text-teal-800">
                          Pushed by {assign.teacherName}
                          {assign.teacherCode ? ` (${assign.teacherCode})` : ''}
                        </p>
                      )}
                    </div>
                    <span className="font-mono text-xs font-semibold text-teal-800 tabular-nums shrink-0">
                      {assign.pushedAt}
                    </span>
                  </div>
                  <p className="mt-1 text-xs text-slate-500">
                    {assign.completedByChildIds.length > 0
                      ? `Completed by ${assign.completedByChildIds.length} synced child profile(s)`
                      : 'Pending completion on synced child devices'}
                  </p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Right: Class Competency Heatmap & Roster */}
        <section aria-label="Class competency heatmap" className="space-y-6 lg:col-span-7">
          <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="font-display text-lg font-bold text-slate-900">
                  Class Competency Heatmap
                </h2>
                <p className="text-xs text-slate-600">
                  {streamMeta.stream} · Official Kenyan CBC Rubric Distribution
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Users className="h-4 w-4 text-teal-700" />
                <select
                  aria-label="Filter class roster stream"
                  value={rosterStreamFilter}
                  onChange={(e) => setRosterStreamFilter(e.target.value)}
                  className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-semibold text-slate-800 focus:border-teal-600 focus:bg-white focus:outline-none"
                >
                  {CLASS_STREAMS.map((s) => (
                    <option key={s.stream} value={s.stream}>
                      {s.stream}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* 4-Rubric Summary Strip */}
            <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
              {(['EE', 'ME', 'AE', 'BE'] as CBCRubric[]).map((code) => {
                const meta = CBC_RUBRIC_META[code];
                const count = rubricCounts[code];
                return (
                  <button
                    key={code}
                    type="button"
                    onClick={() => setRubricFilter((f) => (f === code ? 'ALL' : code))}
                    className={`rounded-2xl border p-3 text-left transition-colors ${
                      rubricFilter === code
                        ? 'border-slate-900 bg-slate-900 text-white'
                        : `${meta.borderColor} ${meta.badgeBg} ${meta.badgeText}`
                    }`}
                  >
                    <div className="flex items-baseline justify-between">
                      <span className="font-mono text-xs font-extrabold">{code}</span>
                      <span className="font-mono text-xl font-bold tabular-nums">{count}</span>
                    </div>
                    <p
                      className={`mt-1 text-[11px] font-semibold ${
                        rubricFilter === code ? 'text-slate-200' : ''
                      }`}
                    >
                      {meta.shortLabel}
                    </p>
                  </button>
                );
              })}
            </div>

            {/* Search & Filter Row */}
            <div className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search learner name or admission no..."
                  className="min-h-[42px] w-full rounded-xl border border-slate-200 bg-slate-50 pl-9 pr-3 text-xs text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
              {rubricFilter !== 'ALL' && (
                <button
                  type="button"
                  onClick={() => setRubricFilter('ALL')}
                  className="min-h-[42px] rounded-xl border border-slate-200 px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                >
                  Show All Rubrics
                </button>
              )}
            </div>

            {/* Competency Table */}
            <div className="mt-4 overflow-x-auto">
              <table className="w-full border-collapse text-left text-xs">
                <thead>
                  <tr className="border-b border-slate-200 text-slate-500">
                    <th className="py-3 pr-3 font-semibold">Adm No</th>
                    <th className="py-3 pr-3 font-semibold">Learner</th>
                    <th className="py-3 pr-3 font-semibold">CBC Rubric</th>
                    <th className="py-3 pr-3 text-right font-semibold">Score</th>
                    <th className="py-3 pr-3 text-right font-semibold">Study</th>
                    <th className="py-3 text-right font-semibold">Homework</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filteredStudents.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="py-6 text-center text-slate-500">
                        No students match the selected filter in {rosterStreamFilter}.
                      </td>
                    </tr>
                  ) : (
                    filteredStudents.map((stu) => {
                      const meta = CBC_RUBRIC_META[stu.rubric];
                      return (
                        <tr key={stu.id} className="hover:bg-slate-50">
                          <td className="py-3 pr-3 font-mono text-slate-500 tabular-nums">
                            {stu.admissionNo}
                          </td>
                          <td className="py-3 pr-3 font-semibold text-slate-900">{stu.name}</td>
                          <td className="py-3 pr-3">
                            <span
                              className={`inline-flex items-center gap-1 rounded-lg border px-2 py-0.5 font-semibold ${meta.badgeBg} ${meta.badgeText} ${meta.borderColor}`}
                            >
                              <span className="font-mono font-bold">{meta.code}</span>
                              <span>·</span>
                              <span>{meta.shortLabel}</span>
                            </span>
                          </td>
                          <td className="py-3 pr-3 text-right font-mono font-bold text-slate-900 tabular-nums">
                            {stu.scorePercent}%
                          </td>
                          <td className="py-3 pr-3 text-right font-mono text-slate-600 tabular-nums">
                            {stu.learningMins}m
                          </td>
                          <td className="py-3 text-right font-semibold">
                            {stu.assignmentDone ? (
                              <span className="text-emerald-700">Submitted</span>
                            ) : (
                              <span className="text-amber-700">Pending</span>
                            )}
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {/* Complete KICD CBC Subjects, Strands & Sub-strands Explorer */}
          <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700">
                  <BookOpen className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="font-display text-lg font-bold text-slate-900">
                    All CBC Subjects, Strands &amp; Sub-strands
                  </h2>
                  <p className="text-xs text-slate-600">
                    {curriculumStats.subjectCount} Learning Areas · {curriculumStats.strandCount}{' '}
                    Strands · {curriculumStats.subStrandCount} Sub-strands (Grades 1–12). Tap any
                    sub-strand to load it into the assignment form.
                  </p>
                </div>
              </div>
              <div className="relative w-full sm:w-64">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={catalogSearch}
                  onChange={(e) => setCatalogSearch(e.target.value)}
                  placeholder="Filter subject, strand, topic..."
                  className="min-h-[40px] w-full rounded-xl border border-slate-200 bg-slate-50 pl-9 pr-3 text-xs text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                />
              </div>
            </div>

            <div className="mt-4 space-y-2.5 max-h-[520px] overflow-y-auto pr-1">
              {filteredCurriculumSubjects.map((subject) => {
                const isOpen =
                  catalogSearch.trim().length > 0 || expandedSubject === subject.learningArea;
                const totalSubCount = subject.strands.reduce(
                  (acc, st) => acc + st.subStrands.length,
                  0
                );
                const isTaughtByTeacher = mySubjects.includes(subject.learningArea);

                return (
                  <div
                    key={subject.learningArea}
                    className={`rounded-2xl border transition-colors ${
                      selectedArea === subject.learningArea
                        ? 'border-teal-300 bg-teal-50/30'
                        : 'border-slate-200 bg-slate-50/50'
                    }`}
                  >
                    <button
                      type="button"
                      onClick={() =>
                        setExpandedSubject((prev) =>
                          prev === subject.learningArea ? null : subject.learningArea
                        )
                      }
                      className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left"
                    >
                      <div className="flex flex-wrap items-center gap-2">
                        {isOpen ? (
                          <ChevronDown className="h-4 w-4 text-teal-700 shrink-0" />
                        ) : (
                          <ChevronRight className="h-4 w-4 text-slate-500 shrink-0" />
                        )}
                        <span className="font-display text-sm font-bold text-slate-900">
                          {isTaughtByTeacher ? `★ ${subject.learningArea}` : subject.learningArea}
                        </span>
                        <span className="text-[11px] font-medium text-slate-500">
                          Grades {subject.minGrade}–{subject.maxGrade}
                          {subject.pathway ? ` · ${subject.pathway}` : ''}
                        </span>
                      </div>
                      <span className="font-mono text-[11px] font-semibold text-teal-800 shrink-0">
                        {subject.strands.length} strands · {totalSubCount} sub-strands
                      </span>
                    </button>

                    {isOpen && (
                      <div className="border-t border-slate-200/80 bg-white px-4 py-3 space-y-3 rounded-b-2xl">
                        {subject.strands.map((st) => (
                          <div key={st.strand}>
                            <p className="text-xs font-bold text-slate-800">{st.strand}</p>
                            <div className="mt-1.5 flex flex-wrap gap-1.5">
                              {st.subStrands.map((sub) => {
                                const isActivePick =
                                  !useCustomStrand &&
                                  selectedArea === subject.learningArea &&
                                  selectedStrand === st.strand &&
                                  selectedSubStrand === sub.subStrand;
                                return (
                                  <button
                                    key={sub.subStrand}
                                    type="button"
                                    onClick={() =>
                                      handleQuickPickFromMatrix(
                                        subject.learningArea,
                                        st.strand,
                                        sub.subStrand
                                      )
                                    }
                                    className={`rounded-xl border px-2.5 py-1.5 text-left text-[11px] font-medium transition-colors ${
                                      isActivePick
                                        ? 'border-teal-700 bg-teal-700 text-white'
                                        : 'border-slate-200 bg-slate-50 text-slate-700 hover:border-teal-500 hover:bg-teal-50'
                                    }`}
                                  >
                                    <span>{sub.subStrand}</span>{' '}
                                    <span
                                      className={`font-mono text-[10px] ${
                                        isActivePick ? 'text-teal-100' : 'text-slate-500'
                                      }`}
                                    >
                                      (G{sub.minGrade}–{sub.maxGrade})
                                    </span>
                                  </button>
                                );
                              })}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
