import React, { useState, useEffect } from 'react';
import {
  ArrowLeft,
  Smartphone,
  Phone,
  RefreshCw,
  Info,
  Shield,
  Clock,
  Cpu,
  Layers,
  CheckCircle2,
  AlertTriangle,
  X
} from 'lucide-react';
import { HookInterceptionBridge } from '../services/hookInterceptionBridge';
import { DeviceIdReader } from '../services/deviceReader';

interface TargetDemoModalProps {
  isOpen: boolean;
  onClose: () => void;
  onShowSnackbar: (msg: string) => void;
}

export const TargetDemoModal: React.FC<TargetDemoModalProps> = ({
  isOpen,
  onClose,
  onShowSnackbar,
}) => {
  const bridge = HookInterceptionBridge.getInstance();

  const [currentAndroidId, setCurrentAndroidId] = useState<string>('91d04b7e8fa3c2d1');
  const [currentTelephonyId, setCurrentTelephonyId] = useState<string>('Restricted (Android 10+)');
  const [lastQueryTime, setLastQueryTime] = useState<number>(Date.now());
  const [pid] = useState<number>(28419);
  const [isQuerying, setIsQuerying] = useState<boolean>(false);

  const performQueries = () => {
    setIsQuerying(true);
    setTimeout(() => {
      const androidRes = DeviceIdReader.readAndroidId('com.example.targetdemo');
      const telephonyRes = DeviceIdReader.readTelephonyDeviceId('com.example.targetdemo');

      setCurrentAndroidId(androidRes.value);
      setCurrentTelephonyId(telephonyRes.value);
      const now = Date.now();
      setLastQueryTime(now);

      // Record target read in bridge for verification diagnostics
      bridge.recordTargetRead(androidRes.value, telephonyRes.value, pid);

      setIsQuerying(false);
      onShowSnackbar('Target demo application re-queried all platform APIs');
    }, 150);
  };

  useEffect(() => {
    if (isOpen) {
      performQueries();
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const isAndroidIntercepted = currentAndroidId !== '91d04b7e8fa3c2d1' && currentAndroidId !== 'null';
  const isTelephonyIntercepted = !currentTelephonyId.includes('Restricted') && !currentTelephonyId.includes('Unavailable');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="w-full max-w-3xl max-h-[90vh] bg-slate-900 border-2 border-slate-700 rounded-3xl shadow-2xl overflow-hidden flex flex-col">
        {/* Top App Bar */}
        <div className="flex items-center justify-between px-6 py-4 bg-slate-950 border-b border-slate-800">
          <div className="flex items-center gap-3">
            <button
              id="target-back-button"
              onClick={onClose}
              className="p-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-800 transition-colors"
              title="Back to Lab Controller"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-slate-100">
                  Target Demo Application
                </h2>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-emerald-950 text-emerald-300 border border-emerald-800">
                  Isolated Process
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Independent Hardware & Identity API Query Sandbox (<code className="text-sky-300">com.example.targetdemo</code>)
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-5 flex-1">
          {/* Target Info Banner */}
          <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800 text-xs text-slate-300 space-y-1.5">
            <div className="flex items-center gap-2 font-bold text-sky-400 text-sm">
              <Info className="w-4 h-4" />
              <span>Target Application Runtime Environment</span>
            </div>
            <p className="leading-relaxed text-slate-300">
              This independent target process invokes official platform APIs directly (<code className="text-sky-300 font-mono">Settings.Secure.getString</code> for Android ID, <code className="text-emerald-300 font-mono">TelephonyManager.getDeviceId</code> for IMEI). Under NPatch 1.0.7, calls are intercepted transparently at the Android framework layer.
            </p>
          </div>

          {/* Android ID Read Output Card */}
          <div className="rounded-2xl bg-slate-950 border border-slate-800 p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-400">
                <Smartphone className="w-4 h-4 text-sky-400" />
                <span>1. ANDROID ID READ OUTPUT</span>
              </div>
              <span
                className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded-full border ${
                  isAndroidIntercepted
                    ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300'
                    : 'bg-slate-800 border-slate-700 text-slate-400'
                }`}
              >
                {isAndroidIntercepted ? 'NPATCH HOOK ACTIVE' : 'BASELINE (UNHOOKED)'}
              </span>
            </div>

            <div className="p-4 rounded-xl bg-slate-900 border border-slate-800/80 space-y-1.5">
              <div className="text-[11px] font-mono text-slate-400">
                Settings.Secure.getString(contentResolver, ANDROID_ID):
              </div>
              <div
                id="target-android-id-text"
                className="text-lg sm:text-xl font-mono font-bold text-sky-400 tracking-wide break-all"
              >
                {currentAndroidId}
              </div>
            </div>
          </div>

          {/* Telephony Device ID Read Output Card */}
          <div className="rounded-2xl bg-slate-950 border border-slate-800 p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-400">
                <Phone className="w-4 h-4 text-emerald-400" />
                <span>2. TELEPHONY DEVICE ID READ OUTPUT</span>
              </div>
              <span
                className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded-full border ${
                  isTelephonyIntercepted
                    ? 'bg-emerald-500/15 border-emerald-500/40 text-emerald-300'
                    : 'bg-amber-500/15 border-amber-500/40 text-amber-300'
                }`}
              >
                {isTelephonyIntercepted ? 'NPATCH HOOK ACTIVE' : 'RESTRICTED / UNHOOKED'}
              </span>
            </div>

            <div className="p-4 rounded-xl bg-slate-900 border border-slate-800/80 space-y-1.5">
              <div className="text-[11px] font-mono text-slate-400">
                TelephonyManager.getDeviceId() / getImei():
              </div>
              <div
                id="target-telephony-id-text"
                className={`text-base sm:text-lg font-mono font-bold tracking-wide break-all ${
                  isTelephonyIntercepted ? 'text-emerald-400' : 'text-amber-300/90'
                }`}
              >
                {currentTelephonyId}
              </div>
            </div>

            {/* Query Metadata */}
            <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] font-mono text-slate-500 pt-1">
              <div className="flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5" />
                <span>Last Query: {new Date(lastQueryTime).toLocaleTimeString()}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <Cpu className="w-3.5 h-3.5" />
                <span>PID: {pid} (target-isolated)</span>
              </div>
            </div>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="p-6 bg-slate-950 border-t border-slate-800 flex flex-col sm:flex-row gap-3">
          <button
            id="target-refresh-button"
            onClick={performQueries}
            disabled={isQuerying}
            className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs shadow-md transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isQuerying ? 'animate-spin' : ''}`} />
            <span>{isQuerying ? 'QUERYING APIS...' : 'RE-QUERY ALL IDENTIFIERS'}</span>
          </button>

          <button
            id="return-to-lab-button"
            onClick={onClose}
            className="px-6 py-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold text-xs border border-slate-700 transition-all"
          >
            RETURN TO LAB CONTROLLER
          </button>
        </div>
      </div>
    </div>
  );
};
