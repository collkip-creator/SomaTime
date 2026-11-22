import { GraduationCap, Link2, Lock, RotateCcw, ShieldCheck, Smartphone } from 'lucide-react';
import type { ChildProfile, RoleView } from '../types/somatime';

interface TopDemoBarProps {
  roleView: RoleView;
  handOffActive: boolean;
  selectedChild: ChildProfile | null;
  onSelectRole: (role: RoleView) => void;
  onOpenSetup: () => void;
  onResetDemo: () => void;
}

export function TopDemoBar({
  roleView,
  handOffActive,
  selectedChild,
  onSelectRole,
  onOpenSetup,
  onResetDemo,
}: TopDemoBarProps) {
  const roles: Array<{
    id: RoleView;
    label: string;
    shortLabel: string;
    icon: typeof ShieldCheck;
  }> = [
    { id: 'parent', label: 'Parent Dashboard', shortLabel: 'Parent', icon: ShieldCheck },
    { id: 'child', label: 'Child Mode', shortLabel: 'Child', icon: Smartphone },
    { id: 'teacher', label: 'Teacher Portal', shortLabel: 'Teacher', icon: GraduationCap },
  ];

  const isLearnerLoggedIn =
    roleView === 'child' &&
    Boolean(
      selectedChild &&
        (handOffActive || (selectedChild.isPaired && selectedChild.deviceMode === 'own_phone'))
    );

  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur-md">
      <div className="mx-auto flex max-w-7xl items-center justify-between gap-3 px-4 py-2.5 sm:px-6">
        <div className="flex items-center gap-2.5 shrink-0">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-teal-700 font-display text-base font-extrabold text-white">
            S
          </div>
          <span className="font-display text-lg font-extrabold tracking-tight text-slate-900 whitespace-nowrap">
            SomaTime
          </span>
        </div>

        {isLearnerLoggedIn && selectedChild ? (
          <div className="flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-3.5 py-2 text-xs font-bold text-emerald-900">
            <Smartphone className="h-3.5 w-3.5 text-emerald-700" />
            <span>
              {handOffActive ? 'Shared Child Lock' : 'Learner Mode'} ·{' '}
              {selectedChild.name.split(' ')[0]} · Grade {selectedChild.grade}
            </span>
          </div>
        ) : (
          <nav
            aria-label="Role switcher"
            className="flex items-center gap-1 rounded-xl bg-slate-100 p-1"
          >
            {roles.map((r) => {
              const Icon = r.icon;
              const active = roleView === r.id;
              const lockedOut = isLearnerLoggedIn && r.id !== 'child';
              return (
                <button
                  key={r.id}
                  type="button"
                  onClick={() => onSelectRole(r.id)}
                  className={`flex min-h-[38px] items-center gap-1.5 rounded-lg px-2.5 py-1.5 text-xs font-semibold transition-colors whitespace-nowrap shrink-0 sm:px-3.5 ${
                    active
                      ? 'bg-white text-slate-900 shadow-sm'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  {lockedOut ? (
                    <Lock className="h-3.5 w-3.5 text-amber-600" />
                  ) : (
                    <Icon className="h-3.5 w-3.5" />
                  )}
                  <span className="hidden sm:inline">{r.label}</span>
                  <span className="sm:hidden">{r.shortLabel}</span>
                </button>
              );
            })}
          </nav>
        )}

        <div className="flex items-center gap-2 shrink-0">
          {selectedChild ? (
            <span className="hidden text-xs text-slate-600 xl:inline whitespace-nowrap">
              Active: <strong className="font-semibold text-slate-900">{selectedChild.name}</strong> · Grade{' '}
              {selectedChild.grade}
            </span>
          ) : (
            <span className="hidden text-xs text-slate-500 xl:inline whitespace-nowrap">
              Fresh Start · No profiles yet
            </span>
          )}
          {!isLearnerLoggedIn && (
            <>
              <button
                type="button"
                onClick={onOpenSetup}
                className="flex min-h-[38px] items-center gap-1.5 rounded-xl bg-teal-700 px-3 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-teal-800 whitespace-nowrap shrink-0"
              >
                <Link2 className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">Setup &amp; Pair</span>
                <span className="sm:hidden">Pair</span>
              </button>
              <button
                type="button"
                onClick={onResetDemo}
                title="Reset all users data"
                className="flex min-h-[38px] items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition-colors hover:bg-slate-50 hover:text-slate-900 whitespace-nowrap shrink-0"
              >
                <RotateCcw className="h-3.5 w-3.5" />
                <span className="hidden md:inline">Reset Data</span>
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
