import { useState } from 'react';
import { GraduationCap, Link2, ShieldCheck, Smartphone, Sparkles } from 'lucide-react';
import { ChildMode } from './components/ChildMode';
import { ParentDashboard } from './components/ParentDashboard';
import { PinModal } from './components/PinModal';
import { SetupOnboardingModal, type SetupModalTab } from './components/SetupOnboardingModal';
import { TeacherPortal } from './components/TeacherPortal';
import { TopDemoBar } from './components/TopDemoBar';
import { useSomaTimeStore } from './hooks/useSomaTimeStore';
import type { PendingApproval, RoleView } from './types/somatime';

type PinIntent =
  | { kind: 'exit_handoff'; targetRole: RoleView }
  | { kind: 'unlock_child' }
  | { kind: 'approve_minutes'; approval: PendingApproval }
  | null;

export default function App() {
  const store = useSomaTimeStore();
  const [pinIntent, setPinIntent] = useState<PinIntent>(null);
  const [setupOpen, setSetupOpen] = useState(false);
  const [setupInitialTab, setSetupInitialTab] = useState<SetupModalTab>('choice');
  const activeChild = store.selectedChild;

  const isChildLockEnforced = Boolean(
    activeChild &&
      (store.handOffActive || (activeChild.isPaired && activeChild.deviceMode === 'own_phone'))
  );

  const handleSelectRole = (nextRole: RoleView) => {
    if (isChildLockEnforced && store.roleView === 'child' && nextRole !== 'child') {
      setPinIntent({ kind: 'exit_handoff', targetRole: nextRole });
      return;
    }
    store.setRoleView(nextRole);
  };

  const openSetup = (tab: SetupModalTab) => {
    setSetupInitialTab(tab);
    setSetupOpen(true);
  };

  const pinModalConfig = (() => {
    if (!pinIntent) {
      return { open: false, title: '', subtitle: '', confirmLabel: 'Verify PIN' };
    }
    const childName = activeChild?.name ?? 'Learner';
    if (pinIntent.kind === 'exit_handoff') {
      return {
        open: true,
        title: 'Exit Child Lock Mode',
        subtitle: `Enter your 4-digit Parent PIN to return from ${childName}'s child lock.`,
        confirmLabel:
          pinIntent.targetRole === 'teacher'
            ? 'Exit to Teacher Portal'
            : 'Exit to Parent Dashboard',
      };
    }
    if (pinIntent.kind === 'unlock_child') {
      return {
        open: true,
        title: 'Parent PIN Screen Unlock',
        subtitle: `Enter your 4-digit Parent PIN to grant +15 mins of watch time to ${childName}.`,
        confirmLabel: 'Unlock +15 Mins',
      };
    }
    return {
      open: true,
      title: 'Approve Earned Minutes',
      subtitle: `Confirm +${pinIntent.approval.minutesEarned} mins for "${pinIntent.approval.lessonTitle}".`,
      confirmLabel: 'Approve & Unlock',
    };
  })();

  const handlePinSuccess = () => {
    if (!pinIntent) return;
    if (pinIntent.kind === 'exit_handoff') {
      store.exitHandOffTo(pinIntent.targetRole);
    } else if (pinIntent.kind === 'unlock_child' && activeChild) {
      store.unlockWithParentPin(activeChild.id, 15);
    } else if (pinIntent.kind === 'approve_minutes' && activeChild) {
      store.resolveApproval(activeChild.id, pinIntent.approval.id, true);
    }
    setPinIntent(null);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <TopDemoBar
        roleView={store.roleView}
        handOffActive={store.handOffActive}
        selectedChild={activeChild}
        onSelectRole={handleSelectRole}
        onOpenSetup={() => openSetup('choice')}
        onResetDemo={store.resetDemoData}
      />

      {store.roleView === 'parent' && activeChild && (
        <ParentDashboard
          childrenList={store.children}
          selectedChild={activeChild}
          parentName={store.parentAccount.name}
          parentPin={store.parentAccount.pin}
          parentContact={store.parentAccount.contact}
          teacherAccount={store.teacherAccount}
          cloudSyncOk={store.cloudSyncOk}
          cloudSyncMessage={store.cloudSyncMessage}
          onRetryCloudSync={() => void store.retryCloudSyncNow()}
          onSelectChild={store.setSelectedChildId}
          onUpdateParentPin={store.updateParentPin}
          onActivateChildLicense={store.activateChildLicense}
          onActivateAllUnpaidLicenses={store.activateAllUnpaidLicenses}
          onUpdateChild={store.updateChildSettings}
          onRegeneratePairingCode={store.regeneratePairingCode}
          onOpenLearnerPair={() => openSetup('learner')}
          onOpenParentRegistration={() => openSetup('parent')}
          onOpenTeacherPortal={() => store.setRoleView('teacher')}
          onOpenTeacherSetup={() => openSetup('teacher')}
          onAddChild={store.addChildProfile}
          onActivateHandOff={store.activateHandOffMode}
          onRequestApprovalPin={(approval) =>
            setPinIntent({ kind: 'approve_minutes', approval })
          }
          onDeclineApproval={(childId, approvalId) =>
            store.resolveApproval(childId, approvalId, false)
          }
        />
      )}

      {store.roleView === 'parent' && !activeChild && (
        <main className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
          <div className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm sm:p-10">
            <div className="flex items-center gap-3">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-teal-700 text-white">
                <ShieldCheck className="h-6 w-6" />
              </div>
              <div>
                <p className="text-xs font-bold uppercase tracking-wider text-teal-700">
                  Fresh Start
                </p>
                <h1 className="font-display text-2xl font-extrabold text-slate-900">
                  Welcome to SomaTime
                </h1>
              </div>
            </div>
            <p className="mt-3 max-w-2xl text-sm text-slate-600">
              Choose how you would like to begin: register as a parent and set your own 4-digit
              PIN, pair a learner&apos;s phone using a parent code, or sign in / register as a
              teacher.
            </p>

            <div className="mt-7 grid gap-4 sm:grid-cols-3">
              <button
                type="button"
                onClick={() => openSetup('parent')}
                className="flex flex-col items-start rounded-2xl border border-teal-200 bg-teal-50/60 p-5 text-left transition-colors hover:border-teal-400 hover:bg-teal-50"
              >
                <ShieldCheck className="h-6 w-6 text-teal-700" />
                <p className="mt-3 font-display text-base font-bold text-slate-900">
                  Register Parent &amp; Child
                </p>
                <p className="mt-1 text-xs text-slate-600">
                  Create your parent PIN, add your child&apos;s grade &amp; school, and generate a
                  pairing code.
                </p>
              </button>

              <button
                type="button"
                onClick={() => openSetup('learner')}
                className="flex flex-col items-start rounded-2xl border border-emerald-200 bg-emerald-50/60 p-5 text-left transition-colors hover:border-emerald-400 hover:bg-emerald-50"
              >
                <Link2 className="h-6 w-6 text-emerald-700" />
                <p className="mt-3 font-display text-base font-bold text-slate-900">
                  Pair Learner Phone
                </p>
                <p className="mt-1 text-xs text-slate-600">
                  Already licensed on a parent phone? Enter your SOMA-XXXX pairing code.
                </p>
              </button>

              <button
                type="button"
                onClick={() => openSetup('teacher')}
                className="flex flex-col items-start rounded-2xl border border-amber-200 bg-amber-50/60 p-5 text-left transition-colors hover:border-amber-400 hover:bg-amber-50"
              >
                <GraduationCap className="h-6 w-6 text-amber-700" />
                <p className="mt-3 font-display text-base font-bold text-slate-900">
                  Teacher Registration
                </p>
                <p className="mt-1 text-xs text-slate-600">
                  Register your school and CBC subjects or sign in with your Teacher Code.
                </p>
              </button>
            </div>

            <div className="mt-6 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-5">
              <p className="text-xs text-slate-500">
                Cloud status: <strong className="text-slate-700">{store.cloudSyncMessage}</strong>
              </p>
              <button
                type="button"
                onClick={store.loadDemoData}
                className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                <Sparkles className="h-3.5 w-3.5 text-teal-700" />
                <span>Load Sample Demo Profiles</span>
              </button>
            </div>
          </div>
        </main>
      )}

      {store.roleView === 'child' && activeChild && (
        <ChildMode
          child={activeChild}
          childrenList={store.children}
          handOffActive={store.handOffActive}
          assignments={store.assignments}
          displayedKeys={store.displayedKeysByChild[activeChild.id] ?? []}
          displayedQuizIds={store.displayedQuizIdsByChild[activeChild.id] ?? []}
          onSelectChild={store.setSelectedChildId}
          onSimulateTimeExpired={store.simulateTimeExpired}
          onOpenPinUnlock={() => setPinIntent({ kind: 'unlock_child' })}
          onOpenExitHandOff={() =>
            setPinIntent({ kind: 'exit_handoff', targetRole: 'parent' })
          }
          onOpenPairSetup={() => openSetup('learner')}
          onSelectActiveApp={store.selectActiveApp}
          onRecordDisplayedAssignment={store.recordDisplayedAssignment}
          onCompleteLesson={store.completeLesson}
        />
      )}

      {store.roleView === 'child' && !activeChild && (
        <main className="mx-auto max-w-xl px-4 py-12 sm:px-6">
          <div className="rounded-3xl border border-slate-200 bg-white p-6 text-center shadow-sm sm:p-8">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-700">
              <Smartphone className="h-6 w-6" />
            </div>
            <h1 className="mt-4 font-display text-xl font-extrabold text-slate-900">
              No Learner Profile Yet
            </h1>
            <p className="mt-2 text-sm text-slate-600">
              Pair this device with a parent&apos;s SOMA code or register as a parent first to set
              your child&apos;s grade and CBC learning rules.
            </p>
            <div className="mt-6 flex flex-col gap-2.5 sm:flex-row sm:justify-center">
              <button
                type="button"
                onClick={() => openSetup('learner')}
                className="inline-flex min-h-[42px] items-center justify-center gap-2 rounded-xl bg-teal-700 px-4 py-2 text-xs font-bold text-white hover:bg-teal-800"
              >
                <Link2 className="h-4 w-4" />
                <span>Pair with Parent Code</span>
              </button>
              <button
                type="button"
                onClick={() => openSetup('parent')}
                className="inline-flex min-h-[42px] items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                <span>Register Parent &amp; Child</span>
              </button>
            </div>
          </div>
        </main>
      )}

      {store.roleView === 'teacher' && (
        <TeacherPortal
          teacherAccount={store.teacherAccount}
          parentAccount={store.parentAccount}
          defaultChildSchool={activeChild?.school}
          defaultChildGrade={activeChild?.grade}
          assignments={store.assignments}
          classRoster={store.classRoster}
          onOpenTeacherAuth={() => openSetup('teacher')}
          onQuickLinkParentAsTeacher={async ({ school, grade, subjects }) => {
            await store.registerTeacher({
              fullName: store.parentAccount.name || 'Teacher',
              school,
              tscNumber: '',
              pin: store.parentAccount.pin || '1234',
              subjects,
              grade,
              classStream: `Grade ${grade} East - ${school}`,
            });
          }}
          onLogoutTeacher={store.logoutTeacher}
          onPushAssignment={store.pushTeacherAssignment}
          onJumpToChildMode={() => store.setRoleView('child')}
        />
      )}

      <PinModal
        open={pinModalConfig.open}
        title={pinModalConfig.title}
        subtitle={pinModalConfig.subtitle}
        expectedPin={store.parentAccount.pin || '1234'}
        confirmLabel={pinModalConfig.confirmLabel}
        exitTargetRole={
          pinIntent?.kind === 'exit_handoff' && pinIntent.targetRole === 'teacher'
            ? 'teacher'
            : 'parent'
        }
        onChangeExitTargetRole={
          pinIntent?.kind === 'exit_handoff'
            ? (role) => setPinIntent({ kind: 'exit_handoff', targetRole: role })
            : undefined
        }
        onSuccess={handlePinSuccess}
        onCancel={() => setPinIntent(null)}
      />

      <SetupOnboardingModal
        key={`${setupOpen}-${setupInitialTab}`}
        open={setupOpen}
        initialTab={setupInitialTab}
        defaultParentName={store.parentAccount.name}
        defaultParentContact={store.parentAccount.contact}
        defaultParentPin={store.parentAccount.pin}
        childrenList={store.children}
        registeredTeachers={store.registeredTeachers}
        onRegisterParentAndChild={store.registerParentAndChild}
        onPairLearnerCode={store.pairLearnerByCode}
        onRegisterTeacher={store.registerTeacher}
        onLoginTeacher={store.loginTeacher}
        onClose={() => setSetupOpen(false)}
      />
    </div>
  );
}
