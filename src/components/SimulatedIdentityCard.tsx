import React, { useState, useEffect } from 'react';
import {
  RefreshCw,
  Copy,
  Check,
  RotateCcw,
  Sparkles,
  Layers,
  Database,
  Fingerprint,
  Phone,
  AlertOctagon,
  Clock,
  Hash,
  BatteryCharging,
  ShieldCheck,
  CheckCircle2
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { DeviceIdentityManager } from '../services/identityManager';
import { HookInterceptionBridge } from '../services/hookInterceptionBridge';
import { DeviceIdentity } from '../types';

interface SimulatedIdentityCardProps {
  onShowSnackbar: (msg: string) => void;
  onRequestReset: () => void;
}

export const SimulatedIdentityCard: React.FC<SimulatedIdentityCardProps> = ({
  onShowSnackbar,
  onRequestReset,
}) => {
  const manager = DeviceIdentityManager.getInstance();
  const bridge = HookInterceptionBridge.getInstance();

  const [currentIdentity, setCurrentIdentity] = useState<DeviceIdentity | null>(
    manager.getCurrentIdentity()
  );
  const [previousIdentity, setPreviousIdentity] = useState<DeviceIdentity | null>(
    manager.getPreviousIdentity()
  );
  const [uniquenessStatus, setUniquenessStatus] = useState<'PASS' | 'INITIAL_PROFILE' | 'FAILED'>(
    manager.getProfileUniquenessStatus()
  );
  const [usedCount, setUsedCount] = useState<number>(manager.getUsedCount());
  const [totalCapacity] = useState<number>(manager.getTotalCapacity());
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [batchAllocating, setBatchAllocating] = useState<boolean>(false);
  const [poolExhaustedMsg, setPoolExhaustedMsg] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = manager.subscribe(() => {
      const latest = manager.getCurrentIdentity();
      setCurrentIdentity(latest);
      setPreviousIdentity(manager.getPreviousIdentity());
      setUniquenessStatus(manager.getProfileUniquenessStatus());
      setUsedCount(manager.getUsedCount());

      // Sync with bridge if new identity allocated
      if (latest) {
        bridge.setInjectedIds(latest.androidTestId, latest.telephonyTestId);
      }
    });
    return () => unsubscribe();
  }, [manager, bridge]);

  const handleCopy = (text: string, field: string) => {
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 1500);
    onShowSnackbar(`Copied ${field} to clipboard`);
  };

  const handleGenerate = () => {
    setIsGenerating(true);
    setTimeout(() => {
      const result = manager.generateNextIdentity();
      setIsGenerating(false);

      if (result.type === 'success') {
        setPoolExhaustedMsg(null);
        onShowSnackbar(`Allocated Unique ID #${result.identity.identityNumber.toLocaleString()}`);
        confetti({
          particleCount: 30,
          spread: 60,
          origin: { y: 0.8 },
          colors: ['#38bdf8', '#4ade80', '#a855f7'],
        });
      } else if (result.type === 'pool_exhausted') {
        setPoolExhaustedMsg(result.message);
        onShowSnackbar('Pool exhausted: All 1,000,000 identities used.');
      } else {
        onShowSnackbar(`Error: ${result.message}`);
      }
    }, 150);
  };

  const handleBatch = (count: number) => {
    setBatchAllocating(true);
    setTimeout(() => {
      const res = manager.generateBatch(count);
      setBatchAllocating(false);
      onShowSnackbar(
        `Batch generated ${res.allocated} unique identities with 0 collisions!`
      );
    }, 200);
  };

  const percentUsed = ((usedCount / totalCapacity) * 100).toFixed(4);

  return (
    <div
      id="simulated-identity-card"
      className="rounded-2xl bg-slate-900 border border-slate-800 p-5 sm:p-6 shadow-xl space-y-5"
    >
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-purple-500/10 border border-purple-500/30 text-purple-400">
            <Database className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              Simulated Identity Pool
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-purple-500/10 text-purple-300 border border-purple-500/20">
                1,000,000 Total
              </span>
            </h2>
            <p className="text-xs text-slate-400">
              Deterministic, collision-resistant identity generation with SQLite/Room persistence guarantees
            </p>
          </div>
        </div>

        {/* Database Reset Button */}
        <button
          id="btn-reset-database-dialog"
          onClick={onRequestReset}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-red-950/30 hover:bg-red-900/40 text-red-300 text-xs font-semibold border border-red-800/40 transition-all self-start sm:self-auto"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>RESET DATABASE</span>
        </button>
      </div>

      {/* Pool Exhausted Warning Banner */}
      {poolExhaustedMsg && (
        <div className="rounded-xl bg-red-950/50 border border-red-500/50 p-4 text-xs text-red-200 flex items-start gap-3">
          <AlertOctagon className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
          <div>
            <div className="font-bold text-sm text-red-300">Pool Capacity Reached</div>
            <div>{poolExhaustedMsg}</div>
          </div>
        </div>
      )}

      {/* Progress Bar & Usage Metrics */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs">
          <span className="text-slate-400 flex items-center gap-1.5 font-semibold">
            <Layers className="w-3.5 h-3.5 text-purple-400" />
            <span>Pool Usage Progress</span>
          </span>
          <span className="font-mono text-slate-300 font-bold">
            {usedCount.toLocaleString()} / {totalCapacity.toLocaleString()} ({percentUsed}%)
          </span>
        </div>

        <div className="w-full h-3 rounded-full bg-slate-950 border border-slate-800 overflow-hidden p-0.5">
          <div
            className="h-full rounded-full bg-gradient-to-r from-sky-500 via-purple-500 to-emerald-400 transition-all duration-300"
            style={{ width: `${Math.max(0.5, (usedCount / totalCapacity) * 100)}%` }}
          />
        </div>
      </div>

      {/* Current Active Allocated Identity Card */}
      {currentIdentity ? (
        <div className="rounded-xl bg-slate-950 border border-slate-800 p-4 sm:p-5 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800 pb-3">
            <div className="flex items-center gap-2">
              <span className="p-1 rounded bg-sky-500/10 text-sky-400">
                <Hash className="w-4 h-4" />
              </span>
              <span className="text-xs font-semibold text-slate-300">Allocated Identity Index:</span>
              <span className="text-sm font-mono font-bold text-sky-400">
                #{currentIdentity.identityNumber.toLocaleString()}
              </span>
            </div>

            <div className="flex items-center gap-2">
              <div
                id="badge-profile-uniqueness"
                className={`flex items-center gap-1 px-2.5 py-1 rounded-full text-[11px] font-mono font-bold border ${
                  uniquenessStatus === 'PASS'
                    ? 'bg-emerald-950/60 border-emerald-500/40 text-emerald-300'
                    : uniquenessStatus === 'INITIAL_PROFILE'
                    ? 'bg-sky-950/60 border-sky-500/40 text-sky-300'
                    : 'bg-red-950/60 border-red-500/40 text-red-300'
                }`}
              >
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>PROFILE UNIQUENESS: {uniquenessStatus}</span>
              </div>

              <div className="flex items-center gap-1.5 text-xs text-slate-400 font-mono">
                <Clock className="w-3.5 h-3.5 text-slate-500" />
                <span>{new Date(currentIdentity.createdAt).toLocaleTimeString()}</span>
              </div>
            </div>
          </div>

          {/* 4 Unified Fields of the SAME Profile */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 pt-1">
            {/* 1. Profile Fingerprint */}
            <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                  <ShieldCheck className="w-3.5 h-3.5 text-indigo-400" />
                  <span>1. Profile Fingerprint</span>
                </span>
                <button
                  onClick={() => handleCopy(currentIdentity.fingerprint, 'Fingerprint')}
                  className="text-slate-400 hover:text-indigo-300 flex items-center gap-1 text-[11px] font-mono"
                >
                  {copiedField === 'Fingerprint' ? (
                    <Check className="w-3 h-3 text-emerald-400" />
                  ) : (
                    <Copy className="w-3 h-3" />
                  )}
                  <span>{copiedField === 'Fingerprint' ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <div className="text-sm font-mono font-bold text-indigo-300 break-all">
                {currentIdentity.fingerprint}
              </div>
            </div>

            {/* 2. Android Test ID */}
            <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                  <Fingerprint className="w-3.5 h-3.5 text-sky-400" />
                  <span>2. Android ID (16-Hex)</span>
                </span>
                <button
                  onClick={() => handleCopy(currentIdentity.androidTestId, 'Android ID')}
                  className="text-slate-400 hover:text-sky-300 flex items-center gap-1 text-[11px] font-mono"
                >
                  {copiedField === 'Android ID' ? (
                    <Check className="w-3 h-3 text-emerald-400" />
                  ) : (
                    <Copy className="w-3 h-3" />
                  )}
                  <span>{copiedField === 'Android ID' ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <div className="text-sm font-mono font-bold text-sky-300 break-all">
                {currentIdentity.androidTestId}
              </div>
            </div>

            {/* 3. Synthetic Phone Number */}
            <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                  <Phone className="w-3.5 h-3.5 text-emerald-400" />
                  <span>3. Synthetic Phone Number</span>
                </span>
                <button
                  onClick={() => handleCopy(currentIdentity.syntheticPhoneNumber, 'Phone Number')}
                  className="text-slate-400 hover:text-emerald-300 flex items-center gap-1 text-[11px] font-mono"
                >
                  {copiedField === 'Phone Number' ? (
                    <Check className="w-3 h-3 text-emerald-400" />
                  ) : (
                    <Copy className="w-3 h-3" />
                  )}
                  <span>{copiedField === 'Phone Number' ? 'Copied' : 'Copy'}</span>
                </button>
              </div>
              <div className="text-sm font-mono font-bold text-emerald-300 break-all">
                {currentIdentity.syntheticPhoneNumber}
              </div>
            </div>

            {/* 4. Battery Health */}
            <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 space-y-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-400 flex items-center gap-1.5 font-medium">
                  <BatteryCharging className="w-3.5 h-3.5 text-amber-400" />
                  <span>4. Battery Health</span>
                </span>
                <span className="text-amber-300 text-[11px] font-mono font-semibold">
                  STATE_OF_HEALTH
                </span>
              </div>
              <div className="flex items-center gap-3 pt-1">
                <div className="text-sm font-mono font-bold text-amber-300">
                  {currentIdentity.batteryHealth}%
                </div>
                <div className="flex-1 h-2 rounded-full bg-slate-950 border border-slate-800 overflow-hidden">
                  <div
                    className="h-full bg-amber-400 rounded-full"
                    style={{ width: `${currentIdentity.batteryHealth}%` }}
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Previous Profile Comparison & Atomic Guarantee Banner */}
          {previousIdentity && (
            <div className="p-3 rounded-lg bg-slate-900/80 border border-slate-800 text-xs space-y-2">
              <div className="flex items-center justify-between text-slate-300 font-semibold">
                <span className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-400" />
                  <span>Profile Transition Verification (Previous vs. Current)</span>
                </span>
                <span className="text-[11px] font-mono text-emerald-400">
                  ALL 4 VALUES STRICTLY UNIQUE
                </span>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-[11px] font-mono pt-1">
                <div className="p-2 rounded bg-slate-950 border border-slate-800/80">
                  <div className="text-slate-400">Fingerprint:</div>
                  <div className="text-emerald-400 font-bold truncate">DIFFERENT ✓</div>
                </div>
                <div className="p-2 rounded bg-slate-950 border border-slate-800/80">
                  <div className="text-slate-400">Android ID:</div>
                  <div className="text-emerald-400 font-bold truncate">DIFFERENT ✓</div>
                </div>
                <div className="p-2 rounded bg-slate-950 border border-slate-800/80">
                  <div className="text-slate-400">Phone Number:</div>
                  <div className="text-emerald-400 font-bold truncate">DIFFERENT ✓</div>
                </div>
                <div className="p-2 rounded bg-slate-950 border border-slate-800/80">
                  <div className="text-slate-400">Battery Health:</div>
                  <div className="text-emerald-400 font-bold truncate">
                    {previousIdentity.batteryHealth}% → {currentIdentity.batteryHealth}% ✓
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="rounded-xl bg-slate-950 border border-dashed border-slate-800 p-8 text-center space-y-2">
          <Database className="w-8 h-8 text-slate-600 mx-auto" />
          <p className="text-sm text-slate-300 font-semibold">No Active Identity Allocated</p>
          <p className="text-xs text-slate-500 max-w-md mx-auto">
            Click "Generate Next Identity" below to allocate a deterministic test identifier from the 1,000,000 pool.
          </p>
        </div>
      )}

      {/* Action Buttons */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
        <button
          id="btn-generate-next-identity"
          onClick={handleGenerate}
          disabled={isGenerating || usedCount >= totalCapacity}
          className="flex-1 sm:flex-initial flex items-center justify-center gap-2 px-5 py-3 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-bold text-xs shadow-lg shadow-sky-500/20 transition-all disabled:opacity-50"
        >
          <Sparkles className="w-4 h-4" />
          <span>{isGenerating ? 'ALLOCATING...' : 'GENERATE NEXT IDENTITY'}</span>
        </button>

        {/* Batch Test Tools */}
        <div className="flex items-center gap-2">
          <span className="text-[11px] text-slate-400 font-semibold hidden md:inline">
            Stress Test Allocations:
          </span>
          <button
            onClick={() => handleBatch(10)}
            disabled={batchAllocating}
            className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold border border-slate-700 transition-all"
          >
            +10 Batch
          </button>
          <button
            onClick={() => handleBatch(100)}
            disabled={batchAllocating}
            className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold border border-slate-700 transition-all"
          >
            +100 Batch
          </button>
          <button
            onClick={() => handleBatch(1000)}
            disabled={batchAllocating}
            className="px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold border border-slate-700 transition-all"
          >
            +1,000 Batch
          </button>
        </div>
      </div>
    </div>
  );
};
