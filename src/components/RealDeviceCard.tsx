import React, { useState } from 'react';
import { Smartphone, Phone, RefreshCw, Lock, CheckCircle2, AlertTriangle, Monitor, Cpu } from 'lucide-react';
import { DeviceIdReader } from '../services/deviceReader';
import { RealIdResult } from '../types';

interface RealDeviceCardProps {
  onShowSnackbar: (msg: string) => void;
}

export const RealDeviceCard: React.FC<RealDeviceCardProps> = ({ onShowSnackbar }) => {
  const [androidIdResult, setAndroidIdResult] = useState<RealIdResult>(DeviceIdReader.readAndroidId());
  const [telephonyResult, setTelephonyResult] = useState<RealIdResult>(DeviceIdReader.readTelephonyDeviceId());
  const [browserInfo, setBrowserInfo] = useState(DeviceIdReader.getBrowserDeviceInfo());
  const [isRefreshing, setIsRefreshing] = useState(false);

  const handleRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => {
      setAndroidIdResult(DeviceIdReader.readAndroidId());
      setTelephonyResult(DeviceIdReader.readTelephonyDeviceId());
      setBrowserInfo(DeviceIdReader.getBrowserDeviceInfo());
      setIsRefreshing(false);
      onShowSnackbar('Real device platform identifiers refreshed from system');
    }, 200);
  };

  return (
    <div id="real-device-card" className="rounded-2xl bg-slate-900 border border-slate-800 p-5 sm:p-6 shadow-xl space-y-5">
      {/* Title & Refresh */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-slate-800 text-sky-400 border border-slate-700">
            <Smartphone className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              Real Device Identifiers
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                OS Platform Query
              </span>
            </h2>
            <p className="text-xs text-slate-400">
              Direct queries to standard Android platform & system hardware APIs
            </p>
          </div>
        </div>

        <button
          id="refresh-real-ids-btn"
          onClick={handleRefresh}
          disabled={isRefreshing}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 transition-all shadow-sm"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-sky-400' : ''}`} />
          <span>Refresh</span>
        </button>
      </div>

      {/* Identifiers Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Android ID Card */}
        <div className="rounded-xl bg-slate-950 border border-slate-800 p-4 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 font-bold text-sky-400">
              <Smartphone className="w-4 h-4" />
              <span>Settings.Secure.ANDROID_ID</span>
            </div>
            <span className="text-[10px] px-2 py-0.5 rounded bg-emerald-950/60 text-emerald-400 border border-emerald-800/40 font-mono">
              64-bit Hex
            </span>
          </div>

          <div className="p-3 rounded-lg bg-slate-900 border border-slate-800">
            <div className="text-xs text-slate-400 font-mono mb-1">
              Settings.Secure.getString(contentResolver, ANDROID_ID):
            </div>
            <div className="text-sm sm:text-base font-mono font-bold text-sky-300 break-all">
              {androidIdResult.value}
            </div>
          </div>

          <div className="flex items-center gap-1.5 text-[11px] text-slate-400">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
            <span>{androidIdResult.statusDetail}</span>
          </div>
        </div>

        {/* Telephony Device ID Card */}
        <div className="rounded-xl bg-slate-950 border border-slate-800 p-4 space-y-2">
          <div className="flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 font-bold text-amber-400">
              <Phone className="w-4 h-4" />
              <span>TelephonyManager.getDeviceId() / IMEI</span>
            </div>
            <span className="text-[10px] px-2 py-0.5 rounded bg-red-950/60 text-red-400 border border-red-800/40 font-mono">
              Restricted (API 29+)
            </span>
          </div>

          <div className="p-3 rounded-lg bg-slate-900 border border-slate-800">
            <div className="text-xs text-slate-400 font-mono mb-1">
              telephonyManager.getImei() / deviceId:
            </div>
            <div className="text-xs sm:text-sm font-mono font-semibold text-amber-300/90 break-all">
              {telephonyResult.value}
            </div>
          </div>

          <div className="flex items-center gap-1.5 text-[11px] text-slate-400">
            <AlertTriangle className="w-3.5 h-3.5 text-amber-400 shrink-0" />
            <span>{telephonyResult.statusDetail}</span>
          </div>
        </div>
      </div>

      {/* Hardware & Browser Telemetry Inspection */}
      <div className="rounded-xl bg-slate-950/60 border border-slate-800/80 p-4 text-xs space-y-2">
        <div className="flex items-center gap-2 text-slate-300 font-bold">
          <Monitor className="w-4 h-4 text-purple-400" />
          <span>Client Environment Telemetry (Comparison with Mobile OS Sandbox)</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-2 pt-1 font-mono text-[11px]">
          <div className="p-2 rounded bg-slate-900/80 border border-slate-800">
            <span className="text-slate-500 block">Screen Resolution:</span>
            <span className="text-slate-300 font-semibold">{browserInfo.screenResolution}</span>
          </div>
          <div className="p-2 rounded bg-slate-900/80 border border-slate-800">
            <span className="text-slate-500 block">Canvas 2D Hash:</span>
            <span className="text-sky-300 font-semibold">{browserInfo.canvasHash}</span>
          </div>
          <div className="p-2 rounded bg-slate-900/80 border border-slate-800">
            <span className="text-slate-500 block">Hardware Concurrency:</span>
            <span className="text-purple-300 font-semibold">{browserInfo.hardwareConcurrency} Cores</span>
          </div>
          <div className="p-2 rounded bg-slate-900/80 border border-slate-800">
            <span className="text-slate-500 block">WebGL Renderer:</span>
            <span className="text-slate-300 font-semibold truncate block" title={browserInfo.webglRenderer}>
              {browserInfo.webglRenderer}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
