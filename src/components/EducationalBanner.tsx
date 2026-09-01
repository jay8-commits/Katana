import React, { useState } from 'react';
import { BookOpen, ChevronDown, ChevronUp, Lock, RefreshCw, Cpu, Layers } from 'lucide-react';

export const EducationalBanner: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="rounded-2xl bg-gradient-to-r from-slate-900 via-slate-900 to-slate-800 border border-slate-800 p-5 shadow-lg">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3.5">
          <div className="p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400 mt-0.5">
            <BookOpen className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              Android Device Identity & Runtime Interception Architecture
              <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/20">
                Educational Lab
              </span>
            </h2>
            <p className="text-xs text-slate-400 mt-1 leading-relaxed max-w-3xl">
              Understand modern Android hardware restrictions, deterministic collision-resistant identity generation (1,000,000 pool), and single-patch dynamic runtime injection without re-patching or re-installing.
            </p>
          </div>
        </div>

        <button
          id="toggle-educational-banner-btn"
          onClick={() => setIsOpen(!isOpen)}
          className="flex items-center gap-1.5 text-xs font-semibold text-sky-400 hover:text-sky-300 py-1.5 px-3 rounded-lg bg-slate-800/80 hover:bg-slate-800 border border-slate-700/60 transition-colors shrink-0"
        >
          <span>{isOpen ? 'Hide Guide' : 'Read Guide'}</span>
          {isOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
        </button>
      </div>

      {isOpen && (
        <div className="mt-5 pt-5 border-t border-slate-800 grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
          {/* Card 1 */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-2">
            <div className="flex items-center gap-2 text-sky-400 font-bold">
              <Lock className="w-4 h-4" />
              <span>Android 10+ Hardware Restrictions</span>
            </div>
            <p className="text-slate-300 leading-relaxed">
              Since Android 10 (API 29), non-system applications cannot read non-resettable hardware identifiers like <strong>IMEI</strong> or <strong>Serial Number</strong> without <code className="text-sky-300 font-mono">READ_PRIVILEGED_PHONE_STATE</code>. Calls to <code className="text-slate-200 font-mono">TelephonyManager.getDeviceId()</code> throw a <code className="text-red-400 font-mono">SecurityException</code>.
            </p>
          </div>

          {/* Card 2 */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-2">
            <div className="flex items-center gap-2 text-emerald-400 font-bold">
              <RefreshCw className="w-4 h-4" />
              <span>1,000,000 Deterministic Pool</span>
            </div>
            <p className="text-slate-300 leading-relaxed">
              Every identity index (1..1,000,000) deterministically produces the exact same 16-hex Android ID and 15-digit IMEI via SHA-256 HMAC and TAC mapping. SQLite/IndexedDB uniqueness constraints guarantee zero duplicates across restarts.
            </p>
          </div>

          {/* Card 3 */}
          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-2">
            <div className="flex items-center gap-2 text-purple-400 font-bold">
              <Layers className="w-4 h-4" />
              <span>NPatch 1.0.7 Single-Patch IPC</span>
            </div>
            <p className="text-slate-300 leading-relaxed">
              The target application is patched only once. When new test IDs are selected in the controller, bytecode hooks dynamically retrieve the latest injected IDs via ContentProvider IPC in real-time, eliminating the need to repatch or reinstall.
            </p>
          </div>
        </div>
      )}
    </div>
  );
};
