import { useMemo, useState } from 'react';
import {
  ArrowLeft,
  Award,
  CheckCircle2,
  Clock,
  HelpCircle,
  Sparkles,
  XCircle,
} from 'lucide-react';
import type { CBCLesson, CBCRubric } from '../types/somatime';

interface LessonPlayerProps {
  lesson: CBCLesson;
  exchangeRateMins: number;
  requireParentApproval: boolean;
  isTeacherPriority: boolean;
  onComplete: (scorePercent: number) => {
    minutesEarned: number;
    rubric: CBCRubric;
    queuedForApproval: boolean;
    cappedOut: boolean;
  };
  onExit: () => void;
}

export function LessonPlayer({
  lesson,
  exchangeRateMins,
  requireParentApproval,
  isTeacherPriority,
  onComplete,
  onExit,
}: LessonPlayerProps) {
  const [stepIndex, setStepIndex] = useState(0);
  const [showHint, setShowHint] = useState(false);

  // Per-question answer states
  const [selectedMcq, setSelectedMcq] = useState<string>('');
  const [blankValue, setBlankValue] = useState<string>('');
  const [selectedLeft, setSelectedLeft] = useState<string | null>(null);
  const [matchedMap, setMatchedMap] = useState<Record<string, string>>({});

  const [submittedCurrent, setSubmittedCurrent] = useState(false);
  const [isCorrectCurrent, setIsCorrectCurrent] = useState(false);
  const [correctCount, setCorrectCount] = useState(0);
  const [rewardSummary, setRewardSummary] = useState<{
    scorePercent: number;
    minutesEarned: number;
    rubric: CBCRubric;
    queuedForApproval: boolean;
    cappedOut: boolean;
  } | null>(null);

  const currentQ = lesson.questions[stepIndex];
  const totalQuestions = lesson.questions.length;

  const shuffledRightOptions = useMemo(() => {
    if (!currentQ?.pairs) return [];
    const rights = currentQ.pairs.map((p) => p.right);
    // Deterministic rotate so order differs from left column
    return [...rights.slice(1), rights[0]];
  }, [currentQ]);

  const handleRightClick = (rightVal: string) => {
    if (submittedCurrent || !selectedLeft) return;
    setMatchedMap((prev) => ({ ...prev, [selectedLeft]: rightVal }));
    setSelectedLeft(null);
  };

  const canCheckAnswer = useMemo(() => {
    if (!currentQ) return false;
    if (currentQ.type === 'multiple_choice') return Boolean(selectedMcq);
    if (currentQ.type === 'fill_blank') return blankValue.trim().length > 0;
    if (currentQ.type === 'matching') {
      return (
        Boolean(currentQ.pairs) &&
        Object.keys(matchedMap).length === (currentQ.pairs?.length ?? 0)
      );
    }
    return false;
  }, [currentQ, selectedMcq, blankValue, matchedMap]);

  const handleCheck = () => {
    if (!currentQ || submittedCurrent) return;
    let correct = false;

    if (currentQ.type === 'multiple_choice') {
      correct = selectedMcq === currentQ.correctAnswer;
    } else if (currentQ.type === 'fill_blank') {
      const normalized = blankValue.trim().toLowerCase();
      const accepted = currentQ.acceptableAnswers ?? [
        (currentQ.correctAnswer ?? '').toLowerCase(),
      ];
      correct = accepted.some((ans) => ans.toLowerCase() === normalized);
    } else if (currentQ.type === 'matching' && currentQ.pairs) {
      correct = currentQ.pairs.every((p) => matchedMap[p.left] === p.right);
    }

    setSubmittedCurrent(true);
    setIsCorrectCurrent(correct);
    if (correct) {
      setCorrectCount((c) => c + 1);
    }
  };

  const handleNext = () => {
    if (stepIndex + 1 < totalQuestions) {
      setStepIndex((i) => i + 1);
      setShowHint(false);
      setSelectedMcq('');
      setBlankValue('');
      setSelectedLeft(null);
      setMatchedMap({});
      setSubmittedCurrent(false);
      setIsCorrectCurrent(false);
    } else {
      const scorePercent = Math.round((correctCount / totalQuestions) * 100);
      const outcome = onComplete(scorePercent);
      setRewardSummary({
        scorePercent,
        ...outcome,
      });
    }
  };

  if (rewardSummary) {
    return (
      <div className="rounded-3xl border border-emerald-200 bg-white p-6 sm:p-8">
        <div className="mx-auto max-w-md text-center">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-3xl bg-emerald-100 text-emerald-700">
            <Award className="h-8 w-8" />
          </div>
          <p className="mt-4 text-xs font-bold text-emerald-700">
            Great Job! Lesson Complete
          </p>
          <h2 className="mt-1 font-display text-2xl font-extrabold text-slate-900">
            {rewardSummary.cappedOut
              ? 'Daily Screen Cap Reached!'
              : rewardSummary.queuedForApproval
                ? `+${rewardSummary.minutesEarned} Mins Sent for Parent PIN`
                : `+${rewardSummary.minutesEarned} Mins Earned!`}
          </h2>
          <p className="mt-2 text-sm text-slate-600">
            You completed <strong className="text-slate-900">{lesson.title}</strong> with{' '}
            <span className="font-mono font-bold text-slate-900">
              {rewardSummary.scorePercent}%
            </span>{' '}
            (CBC Rubric: <strong className="text-emerald-800">{rewardSummary.rubric}</strong>).
          </p>

          <div className="mt-5 grid grid-cols-2 gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-left">
            <div>
              <p className="text-xs text-slate-500">Activity Reward</p>
              <p className="mt-0.5 font-mono text-lg font-bold text-emerald-700 tabular-nums">
                {rewardSummary.cappedOut ? '0 mins (Cap)' : `+${rewardSummary.minutesEarned} mins`}
              </p>
            </div>
            <div>
              <p className="text-xs text-slate-500">Device Status</p>
              <p className="mt-0.5 text-sm font-bold text-slate-900">
                {rewardSummary.queuedForApproval
                  ? 'Awaiting Parent PIN'
                  : rewardSummary.cappedOut
                    ? 'Learning Logged'
                    : 'Apps Unlocked'}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onExit}
            className="mt-6 flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-emerald-600 px-5 py-3 text-sm font-bold text-white transition-colors hover:bg-emerald-700"
          >
            <Sparkles className="h-4 w-4" />
            <span>Return to Android Home Screen</span>
          </button>
        </div>
      </div>
    );
  }

  const progressPct = Math.round(((stepIndex + (submittedCurrent ? 1 : 0)) / totalQuestions) * 100);

  return (
    <div className="rounded-3xl border border-slate-200 bg-white p-5 sm:p-7">
      {/* Header & Progress */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <button
          type="button"
          onClick={onExit}
          className="flex min-h-[40px] items-center gap-1.5 rounded-xl border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>Back to Curriculum</span>
        </button>
        <div className="text-right text-xs text-slate-600">
          <span className="font-semibold text-slate-900">
            Grade {lesson.grade} · {lesson.learningArea}
          </span>
          <span aria-hidden="true"> · </span>
          <span className="font-mono tabular-nums">
            Question {stepIndex + 1} of {totalQuestions}
          </span>
        </div>
      </div>

      {isTeacherPriority && (
        <div className="mt-3 rounded-xl border border-amber-200 bg-amber-50 px-3.5 py-2 text-xs font-semibold text-amber-900">
          Teacher Priority Assignment · Complete to unlock regular screen time
        </div>
      )}

      <div className="mt-4">
        <div className="flex items-center justify-between text-xs text-slate-500">
          <span>
            {lesson.strand} / {lesson.subStrand}
          </span>
          <span className="flex items-center gap-1 font-mono tabular-nums">
            <Clock className="h-3.5 w-3.5" />+{exchangeRateMins}m reward
            {requireParentApproval ? ' (PIN approval)' : ''}
          </span>
        </div>
        <div className="mt-2 h-2.5 w-full overflow-hidden rounded-full bg-slate-100">
          <div
            className="h-full rounded-full bg-emerald-600 transition-all duration-300"
            style={{ width: `${Math.max(12, progressPct)}%` }}
          />
        </div>
      </div>

      {/* Question Stage */}
      <div className="mt-6">
        <h2 className="font-display text-lg font-bold text-slate-900 sm:text-xl">
          {currentQ.prompt}
        </h2>

        {/* Optional Visual Fraction Diagram */}
        {currentQ.visualFraction && (
          <div className="mt-4 rounded-2xl border border-blue-100 bg-blue-50/70 p-4">
            <div className="flex items-center justify-between text-xs font-semibold text-blue-900">
              <span>Visual Chapati Fraction Bar</span>
              <span className="font-mono">{currentQ.visualFraction.label}</span>
            </div>
            <div className="mt-2.5 grid grid-cols-4 gap-2">
              {Array.from({ length: currentQ.visualFraction.denominator }).map((_, i) => (
                <div
                  key={i}
                  className={`h-8 rounded-xl border ${
                    i < currentQ.visualFraction!.numerator
                      ? 'border-amber-500 bg-amber-400'
                      : 'border-blue-200 bg-white'
                  }`}
                />
              ))}
            </div>
          </div>
        )}

        {/* Study Hint Toggle */}
        <div className="mt-4">
          <button
            type="button"
            onClick={() => setShowHint((v) => !v)}
            className="inline-flex min-h-[40px] items-center gap-1.5 rounded-xl border border-amber-200 bg-amber-50 px-3 py-1.5 text-xs font-semibold text-amber-900 hover:bg-amber-100"
          >
            <HelpCircle className="h-4 w-4" />
            <span>{showHint ? 'Hide Study Hint' : 'Show Study Hint'}</span>
          </button>
          {showHint && (
            <p className="mt-2 rounded-xl border border-amber-200 bg-amber-50/60 p-3 text-xs font-medium text-amber-950">
              {currentQ.bilingualHint}
            </p>
          )}
        </div>

        {/* Question Type 1: Multiple Choice */}
        {currentQ.type === 'multiple_choice' && currentQ.options && (
          <div className="mt-5 grid grid-cols-1 gap-2.5 sm:grid-cols-2">
            {currentQ.options.map((opt) => {
              const selected = selectedMcq === opt;
              const isAnswer = opt === currentQ.correctAnswer;
              let btnStyle =
                'border-slate-200 bg-slate-50 text-slate-900 hover:border-blue-400 hover:bg-blue-50/50';
              if (submittedCurrent) {
                if (isAnswer) {
                  btnStyle = 'border-emerald-600 bg-emerald-50 text-emerald-950';
                } else if (selected && !isAnswer) {
                  btnStyle = 'border-rose-500 bg-rose-50 text-rose-950';
                }
              } else if (selected) {
                btnStyle = 'border-blue-600 bg-blue-50 text-blue-950';
              }

              return (
                <button
                  key={opt}
                  type="button"
                  disabled={submittedCurrent}
                  onClick={() => setSelectedMcq(opt)}
                  className={`flex min-h-[52px] items-center justify-between rounded-2xl border px-4 py-3 text-left text-sm font-semibold transition-colors ${btnStyle}`}
                >
                  <span>{opt}</span>
                  {submittedCurrent && isAnswer && (
                    <CheckCircle2 className="h-4 w-4 text-emerald-600 shrink-0" />
                  )}
                  {submittedCurrent && selected && !isAnswer && (
                    <XCircle className="h-4 w-4 text-rose-600 shrink-0" />
                  )}
                </button>
              );
            })}
          </div>
        )}

        {/* Question Type 2: Matching Pairs */}
        {currentQ.type === 'matching' && currentQ.pairs && (
          <div className="mt-5 space-y-3">
            <p className="text-xs text-slate-600">
              Tap an item on the left, then tap its matching meaning on the right.
            </p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div className="space-y-2">
                {currentQ.pairs.map((pair) => {
                  const matchedRight = matchedMap[pair.left];
                  const isActive = selectedLeft === pair.left;
                  return (
                    <button
                      key={pair.id}
                      type="button"
                      disabled={submittedCurrent}
                      onClick={() => setSelectedLeft(pair.left)}
                      className={`flex min-h-[50px] w-full flex-col justify-center rounded-2xl border px-3.5 py-2.5 text-left text-xs font-semibold transition-colors ${
                        isActive
                          ? 'border-blue-600 bg-blue-50 text-blue-950'
                          : matchedRight
                            ? 'border-emerald-300 bg-emerald-50/60 text-slate-900'
                            : 'border-slate-200 bg-slate-50 text-slate-900 hover:bg-slate-100'
                      }`}
                    >
                      <span>{pair.left}</span>
                      {matchedRight && (
                        <span className="mt-0.5 text-[11px] font-medium text-emerald-800">
                          → {matchedRight}
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>

              <div className="space-y-2">
                {shuffledRightOptions.map((rightOpt) => {
                  const alreadyUsed = Object.values(matchedMap).includes(rightOpt);
                  return (
                    <button
                      key={rightOpt}
                      type="button"
                      disabled={submittedCurrent || !selectedLeft}
                      onClick={() => handleRightClick(rightOpt)}
                      className={`flex min-h-[50px] w-full items-center rounded-2xl border px-3.5 py-2.5 text-left text-xs font-semibold transition-colors ${
                        selectedLeft
                          ? 'border-blue-300 bg-white text-slate-900 hover:bg-blue-50'
                          : alreadyUsed
                            ? 'border-emerald-200 bg-emerald-50/40 text-slate-600'
                            : 'border-slate-200 bg-slate-50 text-slate-700'
                      }`}
                    >
                      {rightOpt}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* Question Type 3: Fill in the Blank */}
        {currentQ.type === 'fill_blank' && (
          <div className="mt-5 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="flex flex-wrap items-center gap-2 text-sm font-medium text-slate-800">
              {currentQ.blankPrefix && <span>{currentQ.blankPrefix}</span>}
              <input
                type="text"
                disabled={submittedCurrent}
                value={blankValue}
                onChange={(e) => setBlankValue(e.target.value)}
                placeholder="Type answer..."
                className="min-w-[140px] flex-1 rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 font-mono text-sm font-semibold text-slate-900 focus:border-blue-600 focus:outline-none"
              />
              {currentQ.blankSuffix && <span>{currentQ.blankSuffix}</span>}
            </div>
          </div>
        )}

        {/* Feedback Banner */}
        {submittedCurrent && (
          <div
            className={`mt-5 rounded-2xl border p-4 ${
              isCorrectCurrent
                ? 'border-emerald-200 bg-emerald-50 text-emerald-950'
                : 'border-amber-200 bg-amber-50 text-amber-950'
            }`}
          >
            <div className="flex items-start gap-2.5">
              {isCorrectCurrent ? (
                <CheckCircle2 className="mt-0.5 h-5 w-5 text-emerald-700 shrink-0" />
              ) : (
                <XCircle className="mt-0.5 h-5 w-5 text-amber-700 shrink-0" />
              )}
              <div>
                <p className="text-sm font-bold">
                  {isCorrectCurrent
                    ? 'Well Done! Correct Answer'
                    : `Not quite! Expected answer: ${
                        currentQ.correctAnswer ?? 'See matched pairs below'
                      }`}
                </p>
                <p className="mt-1 text-xs leading-relaxed">{currentQ.explanation}</p>
              </div>
            </div>
          </div>
        )}

        {/* Primary Action Footer */}
        <div className="mt-6 flex justify-end">
          {!submittedCurrent ? (
            <button
              type="button"
              disabled={!canCheckAnswer}
              onClick={handleCheck}
              className="flex min-h-[48px] items-center justify-center rounded-2xl bg-blue-700 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-blue-800 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-500"
            >
              Check Answer
            </button>
          ) : (
            <button
              type="button"
              onClick={handleNext}
              className="flex min-h-[48px] items-center justify-center rounded-2xl bg-emerald-600 px-6 py-3 text-sm font-bold text-white transition-colors hover:bg-emerald-700"
            >
              {stepIndex + 1 < totalQuestions ? 'Next Question' : 'Claim Screen Time Reward'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
