import React, { useState } from 'react';
import {
  Zap,
  Smartphone,
  Bug,
  RefreshCw,
  Play,
  CheckCircle2,
  AlertCircle,
  Copy,
  Check,
  Cpu,
  Fingerprint,
  Phone,
  Layers,
  Terminal
} from 'lucide-react';
import { InjectionTestResult, NpatchVerificationDetails } from '../types';
import { DeviceIdReader } from '../services/deviceReader';
import { HookInterceptionBridge } from '../services/hookInterceptionBridge';
import { generateAndroidTestId, generateTelephonyTestId } from '../utils/crypto';

interface NpatchInjectionCardProps {
  onLaunchTargetDemo: () => void;
  onShowSnackbar: (msg: string) => void;
}

export const NpatchInjectionCard: React.FC<NpatchInjectionCardProps> = ({
  onLaunchTargetDemo,
  onShowSnackbar,
}) => {
  const bridge = HookInterceptionBridge.getInstance();
  const diagnostics = bridge.getStatusDiagnostics();

  const [targetPackage, setTargetPackage] = useState<string>('com.example.deviceidlab');
  const [injectedAndroidId, setInjectedAndroidId] = useState<string>(bridge.getActiveAndroidId());
  const [injectedTelephonyId, setInjectedTelephonyId] = useState<string>(bridge.getActiveTelephonyId());
  const [testCounter, setTestCounter] = useState<number>(1);
  const [testResult, setTestResult] = useState<InjectionTestResult | null>(null);
  const [verificationDetails, setVerificationDetails] = useState<NpatchVerificationDetails | null>(null);
  const [isTesting, setIsTesting] = useState<boolean>(false);
  const [isVerifying, setIsVerifying] = useState<boolean>(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);

  const handleCopy = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 1500);
    onShowSnackbar(`Copied ${field} to clipboard`);
  };

  const handleNextSequential = () => {
    const nextCount = testCounter + 1;
    setTestCounter(nextCount);
    const nextAndroid = `NPATCH_ANDROID_${String(nextCount).padStart(3, '0')}`;
    const nextTelephony = `NPATCH_TELEPHONY_${String(nextCount).padStart(3, '0')}`;
    setInjectedAndroidId(nextAndroid);
    setInjectedTelephonyId(nextTelephony);
    bridge.setInjectedIds(nextAndroid, nextTelephony);
    onShowSnackbar(`Runtime IDs updated (#${nextCount}) - No repatching needed`);
  };

  const handleRandomize = () => {
    const seed = 1 + Math.floor(Math.random() * 1000000);
    const randAndroid = generateAndroidTestId(seed);
    const randTelephony = generateTelephonyTestId(seed);
    setInjectedAndroidId(randAndroid);
    setInjectedTelephonyId(randTelephony);
    bridge.setInjectedIds(randAndroid, randTelephony);
    onShowSnackbar(`Runtime IDs randomized (Seed #${seed}) - Dynamic IPC updated`);
  };

  const handleTestInjection = () => {
    setIsTesting(true);
    bridge.setInjectedIds(injectedAndroidId, injectedTelephonyId);

    setTimeout(() => {
      const result = DeviceIdReader.performInjectionTest(injectedAndroidId, targetPackage);
      setTestResult(result);
      setIsTesting(false);

      if (result.isSuccess) {
        onShowSnackbar(`SUCCESS: Android ID verified as ${result.currentId}`);
      } else {
        onShowSnackbar(`FAILED: ${result.failureReason || 'Method was not intercepted'}`);
      }
    }, 250);
  };

  const handleVerifyNpatch = () => {
    setIsVerifying(true);
    setTimeout(() => {
      const details = DeviceIdReader.verifyNpatchInjection(targetPackage);
      setVerificationDetails(details);
      setIsVerifying(false);
      onShowSnackbar(
        details.isVerified
          ? `VERIFIED: NPatch hook active for ${details.targetPackage}`
          : `INJECTION NOT DETECTED: Running in standard/unhooked mode`
      );
    }, 250);
  };

  const isFrameworkActive = bridge.getIsFrameworkCanaryActive() && bridge.getIsInterceptionActive();
  const isAndroidPass =
    testResult?.isSuccess === true ||
    (diagnostics.lastTargetReadAndroid === diagnostics.currentAndroidTestId &&
      diagnostics.lastTargetReadAndroid !== 'None');

  const isTelephonyPass =
    (diagnostics.lastTargetReadTelephony === diagnostics.currentTelephonyTestId &&
      diagnostics.lastTargetReadTelephony !== 'None') ||
    (diagnostics.lastInterceptedTelephonyId === diagnostics.currentTelephonyTestId &&
      diagnostics.lastInterceptedTelephonyId !== 'None');

  return (
    <div
      id="npatch-injection-card"
      className="rounded-2xl bg-slate-900 border-2 border-sky-500/30 p-5 sm:p-6 shadow-xl space-y-5 transition-all"
    >
      {/* Header with Title and Hook Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-sky-500/10 border border-sky-500/30 text-sky-400">
            <Zap className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              NPatch 1.0.7 Dynamic Hook Injection Lab
            </h2>
            <p className="text-xs text-slate-400">
              Runtime ContentProvider IPC & Method Interception Controller
            </p>
          </div>
        </div>

        {/* Canary Hook Status Badge */}
        <div className="flex items-center gap-2">
          <div
            className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-bold border ${
              isFrameworkActive
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
                : 'bg-amber-500/10 border-amber-500/30 text-amber-400'
            }`}
          >
            <span
              className={`w-2.5 h-2.5 rounded-full ${
                isFrameworkActive ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'
              }`}
            />
            <span>{isFrameworkActive ? 'BYTECODE HOOK ACTIVE' : 'HOOK INACTIVE'}</span>
          </div>

          <button
            id="verify-npatch-btn"
            onClick={handleVerifyNpatch}
            disabled={isVerifying}
            className="px-2.5 py-1.5 text-xs font-semibold rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 transition-colors"
            title="Verify framework canary hook"
          >
            {isVerifying ? 'Verifying...' : 'Verify Canary'}
          </button>
        </div>
      </div>

      {/* Dynamic Runtime Architecture Info Banner */}
      <div className="rounded-xl bg-sky-950/40 border border-sky-800/40 p-3.5 flex items-start gap-3 text-xs text-sky-200">
        <Layers className="w-4 h-4 text-sky-400 shrink-0 mt-0.5" />
        <div>
          <span className="font-bold text-sky-300">Single-patch architecture: </span>
          Target APK is patched once. All new test IDs (Android ID & Telephony Device ID) update at runtime via ContentProvider IPC without repatching or reinstalling the application.
        </div>
      </div>

      {/* Target Package Selection */}
      <div className="space-y-2">
        <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider">
          Target Package Scope
        </label>
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
          <input
            id="target-package-input"
            type="text"
            value={targetPackage}
            onChange={(e) => setTargetPackage(e.target.value)}
            placeholder="e.g. com.example.deviceidlab"
            className="flex-1 px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-slate-200 text-xs font-mono focus:outline-none focus:border-sky-500"
          />

          {/* Quick Target Chips */}
          <div className="flex items-center gap-1.5">
            <button
              id="chip-target-self"
              onClick={() => setTargetPackage('com.example.deviceidlab')}
              className={`px-3 py-2 rounded-xl text-xs font-semibold border transition-all ${
                targetPackage === 'com.example.deviceidlab'
                  ? 'bg-sky-500/20 border-sky-500 text-sky-300'
                  : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-slate-200'
              }`}
            >
              Self (DeviceIdLab)
            </button>
            <button
              id="chip-target-demo"
              onClick={() => setTargetPackage('com.example.targetdemo')}
              className={`px-3 py-2 rounded-xl text-xs font-semibold border transition-all ${
                targetPackage === 'com.example.targetdemo'
                  ? 'bg-sky-500/20 border-sky-500 text-sky-300'
                  : 'bg-slate-800 border-slate-700 text-slate-400 hover:text-slate-200'
              }`}
            >
              com.example.targetdemo
            </button>
          </div>
        </div>
      </div>

      {/* Injected ID Fields (Android ID & Telephony ID) */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* 1. Android Test ID */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs">
            <label className="font-semibold text-slate-300 flex items-center gap-1.5">
              <Fingerprint className="w-3.5 h-3.5 text-sky-400" />
              <span>1. Injected Android Test ID</span>
            </label>
            <button
              onClick={() => handleCopy(injectedAndroidId, 'Android ID')}
              className="text-slate-400 hover:text-sky-300 flex items-center gap-1 font-mono text-[11px]"
            >
              {copiedField === 'Android ID' ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
              <span>{copiedField === 'Android ID' ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <input
            id="injected-android-id-input"
            type="text"
            value={injectedAndroidId}
            onChange={(e) => {
              setInjectedAndroidId(e.target.value);
              bridge.setInjectedIds(e.target.value, injectedTelephonyId);
            }}
            placeholder="e.g. NPATCH_ANDROID_001 or 16-hex ID"
            className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-sky-400 font-mono text-xs font-bold tracking-wide focus:outline-none focus:border-sky-500"
          />
        </div>

        {/* 2. Telephony Test ID */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs">
            <label className="font-semibold text-slate-300 flex items-center gap-1.5">
              <Phone className="w-3.5 h-3.5 text-emerald-400" />
              <span>2. Injected Telephony Test ID</span>
            </label>
            <button
              onClick={() => handleCopy(injectedTelephonyId, 'Telephony ID')}
              className="text-slate-400 hover:text-emerald-300 flex items-center gap-1 font-mono text-[11px]"
            >
              {copiedField === 'Telephony ID' ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
              <span>{copiedField === 'Telephony ID' ? 'Copied' : 'Copy'}</span>
            </button>
          </div>
          <input
            id="injected-telephony-id-input"
            type="text"
            value={injectedTelephonyId}
            onChange={(e) => {
              setInjectedTelephonyId(e.target.value);
              bridge.setInjectedIds(injectedAndroidId, e.target.value);
            }}
            placeholder="e.g. NPATCH_TELEPHONY_001 or 15-digit IMEI"
            className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-700 text-emerald-400 font-mono text-xs font-bold tracking-wide focus:outline-none focus:border-emerald-500"
          />
        </div>
      </div>

      {/* Generator & Test Action Buttons */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-1">
        {/* Next Sequential IDs */}
        <button
          id="generate-next-sequential-btn"
          onClick={handleNextSequential}
          className="flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 transition-all shadow-sm"
        >
          <RefreshCw className="w-3.5 h-3.5 text-sky-400" />
          <span>NEXT TEST IDS</span>
        </button>

        {/* Randomize IDs */}
        <button
          id="generate-random-hex-btn"
          onClick={handleRandomize}
          className="flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold border border-slate-700 transition-all shadow-sm"
        >
          <Cpu className="w-3.5 h-3.5 text-purple-400" />
          <span>RANDOMIZE IDS</span>
        </button>

        {/* Launch Target Demo App */}
        <button
          id="launch-target-demo-btn"
          onClick={onLaunchTargetDemo}
          className="flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-xl bg-indigo-600/30 hover:bg-indigo-600/40 text-indigo-200 text-xs font-bold border border-indigo-500/40 transition-all shadow-sm"
        >
          <Play className="w-3.5 h-3.5 text-indigo-400" />
          <span>LAUNCH TARGET</span>
        </button>

        {/* Test ID Injection */}
        <button
          id="test-id-injection-btn"
          onClick={handleTestInjection}
          disabled={isTesting}
          className="flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-xl bg-sky-600 hover:bg-sky-500 text-white text-xs font-bold shadow-md transition-all disabled:opacity-50"
        >
          <Bug className="w-3.5 h-3.5" />
          <span>{isTesting ? 'TESTING...' : 'TEST ID INJECTION'}</span>
        </button>
      </div>

      {/* Terminal Diagnostic Dashboard (Matching exact user spec) */}
      <div className="rounded-xl bg-slate-950 border border-slate-800 p-4 font-mono text-xs space-y-2 text-slate-300">
        <div className="flex items-center justify-between border-b border-slate-800 pb-2 text-purple-400 font-bold">
          <span className="flex items-center gap-1.5">
            <Terminal className="w-3.5 h-3.5" />
            --- NPATCH 1.0.7 RUNTIME DUAL-ID STATUS ---
          </span>
          <span className="text-[10px] text-slate-500">REALTIME DIAGNOSTICS</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-y-1.5 gap-x-4 pt-1">
          <div>
            <span className="text-slate-500">MODULE STATUS: </span>
            <span className={isFrameworkActive ? 'text-emerald-400 font-semibold' : 'text-slate-400 font-semibold'}>
              {isFrameworkActive ? 'ACTIVE (Runtime Loaded)' : 'INACTIVE / STANDALONE'}
            </span>
          </div>
          <div>
            <span className="text-slate-500">TARGET PROCESS: </span>
            <span className={diagnostics.isTargetDetected ? 'text-emerald-400 font-semibold' : 'text-slate-400'}>
              {diagnostics.isTargetDetected ? `DETECTED (PID ${diagnostics.targetPid})` : 'NOT DETECTED'}
            </span>
          </div>
          <div>
            <span className="text-slate-500">HOOK STATUS: </span>
            <span className={isFrameworkActive ? 'text-emerald-400 font-semibold' : 'text-slate-400'}>
              {isFrameworkActive ? 'ACTIVE (Dual Hooked)' : 'PENDING'}
            </span>
          </div>
        </div>

        {/* Section 1: Android ID Verification */}
        <div className="pt-2 border-t border-slate-800/80 space-y-1">
          <div className="text-sky-400 font-bold text-[11px]">
            --- [1] ANDROID ID VERIFICATION ---
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-1 gap-x-4">
            <div>
              <span className="text-slate-500">CURRENT TEST ID: </span>
              <span className="text-sky-300 font-bold">{diagnostics.currentAndroidTestId}</span>
            </div>
            <div>
              <span className="text-slate-500">LAST INTERCEPTED ID: </span>
              <span className="text-amber-300 font-bold">{diagnostics.lastInterceptedAndroidId}</span>
            </div>
            <div>
              <span className="text-slate-500">LAST TARGET READ: </span>
              <span
                className={
                  diagnostics.lastTargetReadAndroid === diagnostics.currentAndroidTestId &&
                  diagnostics.lastTargetReadAndroid !== 'None'
                    ? 'text-emerald-400 font-bold'
                    : 'text-slate-400'
                }
              >
                {diagnostics.lastTargetReadAndroid}
              </span>
            </div>
            <div>
              <span className="text-slate-500">ANDROID ID STATUS: </span>
              <span
                className={`font-bold ${
                  isAndroidPass
                    ? 'text-emerald-400'
                    : testResult
                    ? 'text-red-400'
                    : 'text-slate-400'
                }`}
              >
                {isAndroidPass
                  ? 'PASS (REAL_HOOK_SUCCESS)'
                  : testResult
                  ? `FAIL (${testResult.hookStatus})`
                  : 'PENDING'}
              </span>
            </div>
          </div>
        </div>

        {/* Section 2: Telephony Device ID Verification */}
        <div className="pt-2 border-t border-slate-800/80 space-y-1">
          <div className="text-emerald-400 font-bold text-[11px]">
            --- [2] TELEPHONY DEVICE ID VERIFICATION ---
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-y-1 gap-x-4">
            <div>
              <span className="text-slate-500">CURRENT TEST ID: </span>
              <span className="text-emerald-300 font-bold">{diagnostics.currentTelephonyTestId}</span>
            </div>
            <div>
              <span className="text-slate-500">LAST INTERCEPTED ID: </span>
              <span className="text-amber-300 font-bold">{diagnostics.lastInterceptedTelephonyId}</span>
            </div>
            <div>
              <span className="text-slate-500">LAST TARGET READ: </span>
              <span
                className={
                  diagnostics.lastTargetReadTelephony === diagnostics.currentTelephonyTestId &&
                  diagnostics.lastTargetReadTelephony !== 'None'
                    ? 'text-emerald-400 font-bold'
                    : 'text-slate-400'
                }
              >
                {diagnostics.lastTargetReadTelephony}
              </span>
            </div>
            <div>
              <span className="text-slate-500">TELEPHONY ID STATUS: </span>
              <span
                className={`font-bold ${
                  isTelephonyPass ? 'text-emerald-400' : 'text-amber-300'
                }`}
              >
                {isTelephonyPass
                  ? 'PASS (REAL_HOOK_SUCCESS)'
                  : diagnostics.isTargetDetected
                  ? 'RESTRICTED / PENDING QUERY'
                  : 'PENDING'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Live Test Results Card (if tested) */}
      {testResult && (
        <div
          className={`rounded-xl p-4 border text-xs space-y-2 ${
            testResult.isSuccess
              ? 'bg-emerald-950/30 border-emerald-500/40 text-emerald-200'
              : 'bg-red-950/30 border-red-500/40 text-red-200'
          }`}
        >
          <div className="flex items-center gap-2 font-bold text-sm">
            {testResult.isSuccess ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-400" />
            ) : (
              <AlertCircle className="w-5 h-5 text-red-400" />
            )}
            <span>
              {testResult.isSuccess
                ? `HOOK SUBSTITUTION SUCCESS: [${testResult.hookStatus}]`
                : `HOOK VERIFICATION FAILED: [${testResult.hookStatus}]`}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 pt-1 font-mono">
            <div>
              <span className="opacity-70">Hardware Baseline: </span>
              <span className="font-semibold">{testResult.originalId}</span>
            </div>
            <div>
              <span className="opacity-70">Target Injected: </span>
              <span className="font-semibold text-sky-300">{testResult.injectedId}</span>
            </div>
            <div>
              <span className="opacity-70">Actual API Return: </span>
              <span className={`font-bold ${testResult.isSuccess ? 'text-emerald-300' : 'text-red-300'}`}>
                {testResult.currentId}
              </span>
            </div>
          </div>

          {testResult.failureReason && (
            <div className="text-red-300 pt-1 border-t border-red-800/40">
              <span className="font-bold">Failure Reason: </span>
              {testResult.failureReason}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
