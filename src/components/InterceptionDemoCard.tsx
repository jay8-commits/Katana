import React, { useState, useEffect } from 'react';
import { Layers, ToggleLeft, ToggleRight, Play, Terminal, Fingerprint, Phone, CheckCircle2, XCircle } from 'lucide-react';
import { HookInterceptionBridge } from '../services/hookInterceptionBridge';
import { DeviceIdReader } from '../services/deviceReader';
import { DeviceIdentity } from '../types';

interface InterceptionDemoCardProps {
  currentIdentity: DeviceIdentity | null;
  onShowSnackbar: (msg: string) => void;
}

export const InterceptionDemoCard: React.FC<InterceptionDemoCardProps> = ({
  currentIdentity,
  onShowSnackbar,
}) => {
  const bridge = HookInterceptionBridge.getInstance();
  const [isActive, setIsActive] = useState<boolean>(bridge.getIsInterceptionActive());
  const [lastOutput, setLastOutput] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = bridge.subscribe(() => {
      setIsActive(bridge.getIsInterceptionActive());
    });
    return () => unsubscribe();
  }, [bridge]);

  const handleToggle = () => {
    const next = !isActive;
    setIsActive(next);
    bridge.setIsInterceptionActive(next);
    onShowSnackbar(
      next
        ? 'Interception layer ENABLED (Bytecode hook will substitute test IDs)'
        : 'Interception layer DISABLED (Original platform baseline will be returned)'
    );
  };

  const handleTestAndroidIdHook = () => {
    const result = bridge.interceptSettingsSecureGetString(
      'com.example.deviceidlab',
      'android_id',
      () => '91d04b7e8fa3c2d1'
    );
    setLastOutput(`Settings.Secure.getString(contentResolver, "android_id") => "${result}"`);
    onShowSnackbar(`Invoked Settings.Secure => ${result}`);
  };

  const handleTestTelephonyHook = () => {
    const result = bridge.interceptTelephonyGetDeviceId(
      'com.example.deviceidlab',
      () => 'Restricted (Android 10+ SecurityException)'
    );
    setLastOutput(`TelephonyManager.getDeviceId() => "${result}"`);
    onShowSnackbar(`Invoked TelephonyManager => ${result}`);
  };

  return (
    <div id="interception-demo-card" className="rounded-2xl bg-slate-900 border border-slate-800 p-5 sm:p-6 shadow-xl space-y-5">
      {/* Header with Switch */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-indigo-500/10 border border-indigo-500/30 text-indigo-400">
            <Layers className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              NPatch / Bytecode Interception Layer Sandbox
            </h2>
            <p className="text-xs text-slate-400">
              Interactive method interception simulator scoped strictly to target test package
            </p>
          </div>
        </div>

        {/* Toggle Switch */}
        <button
          id="toggle-interception-active-btn"
          onClick={handleToggle}
          className={`flex items-center gap-2 px-3.5 py-1.5 rounded-full border text-xs font-bold transition-all ${
            isActive
              ? 'bg-emerald-500/15 border-emerald-500/30 text-emerald-300'
              : 'bg-slate-800 border-slate-700 text-slate-400'
          }`}
        >
          {isActive ? <ToggleRight className="w-5 h-5 text-emerald-400" /> : <ToggleLeft className="w-5 h-5 text-slate-500" />}
          <span>{isActive ? 'INTERCEPTION ACTIVE' : 'INTERCEPTION DISABLED'}</span>
        </button>
      </div>

      {/* Interactive Trigger Sandbox */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <button
          id="btn-test-android-id-hook"
          onClick={handleTestAndroidIdHook}
          className="flex items-center justify-between p-4 rounded-xl bg-slate-950 hover:bg-slate-850 border border-slate-800 hover:border-sky-500/40 text-left transition-all group"
        >
          <div className="space-y-1">
            <div className="flex items-center gap-1.5 text-xs font-bold text-sky-400 group-hover:text-sky-300">
              <Fingerprint className="w-4 h-4" />
              <span>TEST SETTINGS.SECURE HOOK</span>
            </div>
            <div className="text-[11px] text-slate-400 font-mono">
              Invokes Settings.Secure.getString(ANDROID_ID)
            </div>
          </div>
          <div className="p-2 rounded-lg bg-slate-900 group-hover:bg-sky-500/20 text-slate-400 group-hover:text-sky-300">
            <Play className="w-4 h-4" />
          </div>
        </button>

        <button
          id="btn-test-telephony-hook"
          onClick={handleTestTelephonyHook}
          className="flex items-center justify-between p-4 rounded-xl bg-slate-950 hover:bg-slate-850 border border-slate-800 hover:border-emerald-500/40 text-left transition-all group"
        >
          <div className="space-y-1">
            <div className="flex items-center gap-1.5 text-xs font-bold text-emerald-400 group-hover:text-emerald-300">
              <Phone className="w-4 h-4" />
              <span>TEST TELEPHONYMANAGER HOOK</span>
            </div>
            <div className="text-[11px] text-slate-400 font-mono">
              Invokes TelephonyManager.getDeviceId()
            </div>
          </div>
          <div className="p-2 rounded-lg bg-slate-900 group-hover:bg-emerald-500/20 text-slate-400 group-hover:text-emerald-300">
            <Play className="w-4 h-4" />
          </div>
        </button>
      </div>

      {/* Output Console Display */}
      {lastOutput && (
        <div className="rounded-xl bg-slate-950 border border-slate-800 p-4 space-y-1.5 font-mono text-xs">
          <div className="flex items-center gap-2 text-purple-400 font-bold text-[11px]">
            <Terminal className="w-3.5 h-3.5" />
            <span>LAST METHOD EXECUTION OUTPUT:</span>
          </div>
          <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 text-sky-300 break-all font-semibold">
            {lastOutput}
          </div>
        </div>
      )}
    </div>
  );
};
