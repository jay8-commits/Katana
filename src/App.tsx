import React, { useState, useEffect } from 'react';
import { Navbar } from './components/Navbar';
import { EducationalBanner } from './components/EducationalBanner';
import { NpatchInjectionCard } from './components/NpatchInjectionCard';
import { RealDeviceCard } from './components/RealDeviceCard';
import { SimulatedIdentityCard } from './components/SimulatedIdentityCard';
import { InterceptionDemoCard } from './components/InterceptionDemoCard';
import { InvocationLogsCard } from './components/InvocationLogsCard';
import { TargetDemoModal } from './components/TargetDemoModal';
import { ResetConfirmDialog } from './components/ResetConfirmDialog';
import { DeviceIdentityManager } from './services/identityManager';
import { HookInterceptionBridge } from './services/hookInterceptionBridge';
import { DeviceIdentity } from './types';
import { CheckCircle, Info } from 'lucide-react';

export const App: React.FC = () => {
  const manager = DeviceIdentityManager.getInstance();
  const bridge = HookInterceptionBridge.getInstance();

  const [currentIdentity, setCurrentIdentity] = useState<DeviceIdentity | null>(
    manager.getCurrentIdentity()
  );
  const [isHookActive, setIsHookActive] = useState<boolean>(
    bridge.getIsInterceptionActive()
  );
  const [isTargetModalOpen, setIsTargetModalOpen] = useState<boolean>(false);
  const [isResetDialogOpen, setIsResetDialogOpen] = useState<boolean>(false);
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null);

  useEffect(() => {
    const unsubManager = manager.subscribe(() => {
      setCurrentIdentity(manager.getCurrentIdentity());
    });

    const unsubBridge = bridge.subscribe(() => {
      setIsHookActive(bridge.getIsInterceptionActive());
    });

    return () => {
      unsubManager();
      unsubBridge();
    };
  }, [manager, bridge]);

  const showSnackbar = (msg: string) => {
    setSnackbarMessage(msg);
  };

  useEffect(() => {
    if (snackbarMessage) {
      const timer = setTimeout(() => {
        setSnackbarMessage(null);
      }, 3200);
      return () => clearTimeout(timer);
    }
  }, [snackbarMessage]);

  const handleConfirmReset = () => {
    manager.resetDatabase();
    bridge.setInjectedIds('NPATCH_ANDROID_001', 'NPATCH_TELEPHONY_001');
    showSnackbar('Test identity database reset to 0 / 1,000,000');
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-sky-500 selection:text-white">
      {/* Top Navigation */}
      <Navbar
        isHookActive={isHookActive}
        onLaunchTargetDemo={() => setIsTargetModalOpen(true)}
        onResetDatabase={() => setIsResetDialogOpen(true)}
      />

      {/* Main Content Layout */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* Educational Architecture Guide */}
        <EducationalBanner />

        {/* Section 1: NPatch 1.0.7 Dynamic Hook Injection Lab (Primary Target Controller) */}
        <NpatchInjectionCard
          onLaunchTargetDemo={() => setIsTargetModalOpen(true)}
          onShowSnackbar={showSnackbar}
        />

        {/* Section 2: Real Device Identifiers (Standard OS Platform Query) */}
        <RealDeviceCard onShowSnackbar={showSnackbar} />

        {/* Section 3: 1,000,000 Simulated Identity Pool Manager */}
        <SimulatedIdentityCard
          onShowSnackbar={showSnackbar}
          onRequestReset={() => setIsResetDialogOpen(true)}
        />

        {/* Section 4: NPatch / Bytecode Method Interception Sandbox */}
        <InterceptionDemoCard
          currentIdentity={currentIdentity}
          onShowSnackbar={showSnackbar}
        />

        {/* Section 5: Real-time Invocation Logs */}
        <InvocationLogsCard onShowSnackbar={showSnackbar} />
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-slate-950 py-6 text-center text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>DeviceIdRandomizationLab - Kotlin/Android to React TypeScript Architecture</span>
          <span className="font-mono text-slate-400">NPatch 1.0.7 Dynamic IPC Engine</span>
        </div>
      </footer>

      {/* Standalone Target Demo Activity Modal */}
      <TargetDemoModal
        isOpen={isTargetModalOpen}
        onClose={() => setIsTargetModalOpen(false)}
        onShowSnackbar={showSnackbar}
      />

      {/* Reset Pool Confirmation Dialog */}
      <ResetConfirmDialog
        isOpen={isResetDialogOpen}
        onClose={() => setIsResetDialogOpen(false)}
        onConfirm={handleConfirmReset}
      />

      {/* Tactile Snackbar / Toast Notification */}
      {snackbarMessage && (
        <div className="fixed bottom-6 right-6 z-50 animate-in slide-in-from-bottom-5 fade-in duration-200">
          <div className="flex items-center gap-2.5 px-4 py-3 rounded-2xl bg-slate-900 border border-sky-500/40 text-slate-200 text-xs font-semibold shadow-2xl backdrop-blur-md">
            <Info className="w-4 h-4 text-sky-400 shrink-0" />
            <span>{snackbarMessage}</span>
          </div>
        </div>
      )}
    </div>
  );
};
