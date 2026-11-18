import { useMemo, useState } from 'react';
import { CheckCircle2, GraduationCap, Search, Send, Users } from 'lucide-react';
import {
  ALL_LEARNING_AREAS,
  CBC_DIRECTORY_CATALOG,
  CBC_RUBRIC_META,
  CLASS_STREAMS,
} from '../data/cbcCurriculum';
import type {
  CBCRubric,
  ClassStudentRecord,
  LearningArea,
  TeacherAssignment,
} from '../types/somatime';

interface TeacherPortalProps {
  assignments: TeacherAssignment[];
  classRoster: ClassStudentRecord[];
  onPushAssignment: (input: {
    classStream: string;
    grade: number;
    learningArea: LearningArea;
    strand: string;
    subStrand: string;
  }) => void;
  onJumpToChildMode: () => void;
}

export function TeacherPortal({
  assignments,
  classRoster,
  onPushAssignment,
  onJumpToChildMode,
}: TeacherPortalProps) {
  const [selectedStream, setSelectedStream] = useState(CLASS_STREAMS[0].stream);
  const [selectedArea, setSelectedArea] = useState<LearningArea>('Social Studies');
  const [selectedStrand, setSelectedStrand] = useState('Resources & Economic Activities');
  const [selectedSubStrand, setSelectedSubStrand] = useState('Cash Crops & Port Trade');
  const [rubricFilter, setRubricFilter] = useState<CBCRubric | 'ALL'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [justPushedTitle, setJustPushedTitle] = useState<string>('');

  const streamMeta = useMemo(
    () => CLASS_STREAMS.find((s) => s.stream === selectedStream) ?? CLASS_STREAMS[0],
    [selectedStream]
  );

  // Available strand suggestions based on Learning Area
  const strandOptions = useMemo(() => {
    const matches = CBC_DIRECTORY_CATALOG.filter((c) => c.learningArea === selectedArea);
    return matches.length > 0 ? matches : CBC_DIRECTORY_CATALOG.slice(0, 4);
  }, [selectedArea]);

  const handleSelectArea = (area: LearningArea) => {
    setSelectedArea(area);
    const first = CBC_DIRECTORY_CATALOG.find((c) => c.learningArea === area);
    if (first) {
      setSelectedStrand(first.strand);
      setSelectedSubStrand(first.subStrand);
    }
  };

  const handlePush = (e: React.FormEvent) => {
    e.preventDefault();
    onPushAssignment({
      classStream: streamMeta.stream,
      grade: streamMeta.grade,
      learningArea: selectedArea,
      strand: selectedStrand,
      subStrand: selectedSubStrand,
    });
    setJustPushedTitle(`${selectedSubStrand} pushed to ${streamMeta.stream}`);
  };

  const streamStudents = useMemo(() => {
    return classRoster.filter((s) => s.classStream === selectedStream);
  }, [classRoster, selectedStream]);

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
            Push Kenyan CBC priority assignments directly to learners&apos; home SomaTime lock screens
            and monitor class mastery.
          </p>
        </div>
        <div className="flex items-center gap-2 text-xs font-semibold text-slate-700">
          <Users className="h-4 w-4 text-teal-700" />
          <span>{streamMeta.teacher}</span>
          <span aria-hidden="true">·</span>
          <span className="font-mono tabular-nums">{streamMeta.studentsCount} learners</span>
        </div>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-12">
        {/* Left: Homework Push & Active Assignments */}
        <section
          aria-label="Homework push controls"
          className="space-y-6 lg:col-span-5"
        >
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
                  Synced students must finish this task before earning regular play time.
                </p>
              </div>
            </div>

            <div className="mt-5 space-y-4">
              <div>
                <label
                  htmlFor="stream-select"
                  className="block text-xs font-semibold text-slate-700"
                >
                  Grade &amp; Class Stream
                </label>
                <select
                  id="stream-select"
                  value={selectedStream}
                  onChange={(e) => setSelectedStream(e.target.value)}
                  className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                >
                  {CLASS_STREAMS.map((s) => (
                    <option key={s.stream} value={s.stream}>
                      {s.stream}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  htmlFor="area-select"
                  className="block text-xs font-semibold text-slate-700"
                >
                  CBC Learning Area
                </label>
                <select
                  id="area-select"
                  value={selectedArea}
                  onChange={(e) => handleSelectArea(e.target.value as LearningArea)}
                  className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                >
                  {ALL_LEARNING_AREAS.map((area) => (
                    <option key={area} value={area}>
                      {area}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label
                  htmlFor="strand-select"
                  className="block text-xs font-semibold text-slate-700"
                >
                  CBC Strand &amp; Sub-strand
                </label>
                <select
                  id="strand-select"
                  value={`${selectedStrand}|||${selectedSubStrand}`}
                  onChange={(e) => {
                    const [st, sub] = e.target.value.split('|||');
                    setSelectedStrand(st);
                    setSelectedSubStrand(sub);
                  }}
                  className="mt-1.5 w-full rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm font-medium text-slate-900 focus:border-teal-600 focus:bg-white focus:outline-none"
                >
                  {strandOptions.map((opt, idx) => (
                    <option
                      key={`${opt.strand}-${opt.subStrand}-${idx}`}
                      value={`${opt.strand}|||${opt.subStrand}`}
                    >
                      {opt.strand} — {opt.subStrand}
                    </option>
                  ))}
                </select>
              </div>

              <button
                type="submit"
                className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-teal-700 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-teal-800"
              >
                <Send className="h-4 w-4" />
                <span>Push Assignment to Students</span>
              </button>
            </div>

            {justPushedTitle && (
              <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 p-3.5 text-xs text-emerald-950">
                <div className="flex items-start gap-2">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 text-emerald-700 shrink-0" />
                  <div className="flex-1">
                    <p className="font-bold">Synced to Child Devices!</p>
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
                        {assign.classStream} · {assign.learningArea} · {assign.strand}
                      </p>
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
        <section
          aria-label="Class competency heatmap"
          className="space-y-6 lg:col-span-7"
        >
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
                        No students match the selected filter in {selectedStream}.
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
        </section>
      </div>
    </main>
  );
}
