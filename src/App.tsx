import { useState } from 'react';
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

  const handleSelectRole = (nextRole: RoleView) => {
    if (store.handOffActive && store.roleView === 'child' && nextRole !== 'child') {
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
    if (pinIntent.kind === 'exit_handoff') {
      return {
        open: true,
        title: 'Exit Child Lock Mode',
        subtitle: `Enter Parent PIN (${store.parentAccount.pin}) to leave ${store.selectedChild.name}'s locked device mode.`,
        confirmLabel: 'Exit Lock Mode',
      };
    }
    if (pinIntent.kind === 'unlock_child') {
      return {
        open: true,
        title: 'Parent PIN Screen Unlock',
        subtitle: `Enter Parent PIN (${store.parentAccount.pin}) to grant +15 mins of watch time to ${store.selectedChild.name}.`,
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
      store.exitHandOffToParent();
      if (pinIntent.targetRole !== 'parent') {
        store.setRoleView(pinIntent.targetRole);
      }
    } else if (pinIntent.kind === 'unlock_child') {
      store.unlockWithParentPin(store.selectedChild.id, 15);
    } else if (pinIntent.kind === 'approve_minutes') {
      store.resolveApproval(store.selectedChild.id, pinIntent.approval.id, true);
    }
    setPinIntent(null);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <TopDemoBar
        roleView={store.roleView}
        handOffActive={store.handOffActive}
        selectedChild={store.selectedChild}
        onSelectRole={handleSelectRole}
        onOpenSetup={() => openSetup('choice')}
        onResetDemo={store.resetDemoData}
      />

      {store.roleView === 'parent' && (
        <ParentDashboard
          childrenList={store.children}
          selectedChild={store.selectedChild}
          parentPin={store.parentAccount.pin}
          cloudSyncOk={store.cloudSyncOk}
          cloudSyncMessage={store.cloudSyncMessage}
          onRetryCloudSync={() => void store.retryCloudSyncNow()}
          onSelectChild={store.setSelectedChildId}
          onUpdateChild={store.updateChildSettings}
          onRegeneratePairingCode={store.regeneratePairingCode}
          onOpenLearnerPair={() => openSetup('learner')}
          onOpenParentRegistration={() => openSetup('parent')}
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

      {store.roleView === 'child' && (
        <ChildMode
          child={store.selectedChild}
          childrenList={store.children}
          handOffActive={store.handOffActive}
          parentPin={store.parentAccount.pin}
          assignments={store.assignments}
          onSelectChild={store.setSelectedChildId}
          onSimulateTimeExpired={store.simulateTimeExpired}
          onOpenPinUnlock={() => setPinIntent({ kind: 'unlock_child' })}
          onOpenExitHandOff={() =>
            setPinIntent({ kind: 'exit_handoff', targetRole: 'parent' })
          }
          onOpenPairSetup={() => openSetup('learner')}
          onSelectActiveApp={store.selectActiveApp}
          onCompleteLesson={store.completeLesson}
        />
      )}

      {store.roleView === 'teacher' && (
        <TeacherPortal
          teacherAccount={store.teacherAccount}
          assignments={store.assignments}
          classRoster={store.classRoster}
          onOpenTeacherAuth={() => openSetup('teacher')}
          onLogoutTeacher={store.logoutTeacher}
          onPushAssignment={store.pushTeacherAssignment}
          onJumpToChildMode={() => store.setRoleView('child')}
        />
      )}

      <PinModal
        open={pinModalConfig.open}
        title={pinModalConfig.title}
        subtitle={pinModalConfig.subtitle}
        expectedPin={store.parentAccount.pin}
        confirmLabel={pinModalConfig.confirmLabel}
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
