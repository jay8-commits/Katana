import React from 'react';
import { Shield, Sparkles, Smartphone, RotateCcw } from 'lucide-react';

interface NavbarProps {
  isHookActive: boolean;
  onLaunchTargetDemo: () => void;
  onResetDatabase: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  isHookActive,
  onLaunchTargetDemo,
  onResetDatabase,
}) => {
  return (
    <header className="sticky top-0 z-40 bg-slate-900/90 backdrop-blur-md border-b border-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand & Subtitle */}
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-sky-500/10 border border-sky-500/20 text-sky-400">
              <Shield className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-bold text-slate-100 tracking-tight">
                  DeviceIdRandomizationLab
                </h1>
                <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded-full bg-sky-950 text-sky-300 border border-sky-800/50">
                  v1.0.7
                </span>
              </div>
              <p className="text-xs text-slate-400">
                NPatch 1.0.7 Dynamic Runtime Control & 1,000,000 Pool Laboratory
              </p>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="flex items-center gap-3">
            {/* Status Chip */}
            <div className="hidden sm:flex items-center gap-2 px-3 py-1 rounded-full bg-slate-800/80 border border-slate-700 text-xs">
              <span className={`w-2 h-2 rounded-full ${isHookActive ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`} />
              <span className={`font-semibold ${isHookActive ? 'text-emerald-300' : 'text-amber-300'}`}>
                {isHookActive ? 'HOOK RUNTIME ACTIVE' : 'STANDALONE MODE'}
              </span>
            </div>

            {/* Launch Target Demo Button */}
            <button
              id="nav-launch-target-btn"
              onClick={onLaunchTargetDemo}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-sky-600 hover:bg-sky-500 text-white text-xs font-semibold shadow-sm transition-all"
            >
              <Smartphone className="w-4 h-4" />
              <span className="hidden sm:inline">Launch</span> Target Demo
            </button>

            {/* Reset Database Button */}
            <button
              id="nav-reset-db-btn"
              onClick={onResetDatabase}
              title="Reset Test Database"
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-red-950/40 text-slate-300 hover:text-red-400 border border-slate-700 hover:border-red-800/50 text-xs font-medium transition-all"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span className="hidden md:inline">Reset Pool</span>
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
