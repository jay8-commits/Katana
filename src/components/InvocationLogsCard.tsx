import React, { useState, useEffect } from 'react';
import { Terminal, Trash2, Search, CheckCircle, XCircle, Filter, Copy, Check } from 'lucide-react';
import { HookInterceptionBridge } from '../services/hookInterceptionBridge';
import { HookInvocationLog } from '../types';

interface InvocationLogsCardProps {
  onShowSnackbar: (msg: string) => void;
}

export const InvocationLogsCard: React.FC<InvocationLogsCardProps> = ({ onShowSnackbar }) => {
  const bridge = HookInterceptionBridge.getInstance();
  const [logs, setLogs] = useState<HookInvocationLog[]>(bridge.getLogs());
  const [filterType, setFilterType] = useState<'all' | 'intercepted' | 'passthrough'>('all');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [copiedId, setCopiedId] = useState<number | null>(null);

  useEffect(() => {
    const unsubscribe = bridge.subscribe(() => {
      setLogs(bridge.getLogs());
    });
    return () => unsubscribe();
  }, [bridge]);

  const handleClearLogs = () => {
    bridge.clearLogs();
    onShowSnackbar('Invocation logs cleared');
  };

  const handleCopyLog = (log: HookInvocationLog) => {
    const text = JSON.stringify(log, null, 2);
    navigator.clipboard.writeText(text);
    setCopiedId(log.id);
    setTimeout(() => setCopiedId(null), 1500);
    onShowSnackbar('Copied log event payload');
  };

  const filteredLogs = logs.filter((log) => {
    if (filterType === 'intercepted' && !log.wasIntercepted) return false;
    if (filterType === 'passthrough' && log.wasIntercepted) return false;
    if (searchQuery.trim()) {
      const q = searchQuery.toLowerCase();
      return (
        log.callerPackage.toLowerCase().includes(q) ||
        log.targetApi.toLowerCase().includes(q) ||
        log.returnedValue.toLowerCase().includes(q) ||
        log.reason.toLowerCase().includes(q)
      );
    }
    return true;
  });

  return (
    <div id="invocation-logs-card" className="rounded-2xl bg-slate-900 border border-slate-800 p-5 sm:p-6 shadow-xl space-y-4">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-slate-800 text-sky-400 border border-slate-700">
            <Terminal className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-100 flex items-center gap-2">
              Interception Invocation Logs
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-slate-800 text-sky-300 border border-slate-700">
                {logs.length} Total Events
              </span>
            </h2>
            <p className="text-xs text-slate-400">
              Live audit stream of all method hook interceptions and pass-through events
            </p>
          </div>
        </div>

        {/* Clear Logs Button */}
        {logs.length > 0 && (
          <button
            id="clear-logs-btn"
            onClick={handleClearLogs}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-red-950/40 text-slate-300 hover:text-red-400 border border-slate-700 hover:border-red-800/40 text-xs font-semibold transition-all self-start sm:self-auto"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Clear Logs</span>
          </button>
        )}
      </div>

      {/* Filter and Search Bar */}
      {logs.length > 0 && (
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2 pt-1">
          {/* Search Box */}
          <div className="relative flex-1">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search API, package, returned value, or reason..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-sky-500"
            />
          </div>

          {/* Filter Chips */}
          <div className="flex items-center gap-1.5 self-end sm:self-auto">
            <button
              onClick={() => setFilterType('all')}
              className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                filterType === 'all'
                  ? 'bg-sky-500/20 border-sky-500 text-sky-300'
                  : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              All ({logs.length})
            </button>
            <button
              onClick={() => setFilterType('intercepted')}
              className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                filterType === 'intercepted'
                  ? 'bg-emerald-500/20 border-emerald-500 text-emerald-300'
                  : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              Intercepted ({logs.filter((l) => l.wasIntercepted).length})
            </button>
            <button
              onClick={() => setFilterType('passthrough')}
              className={`px-2.5 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                filterType === 'passthrough'
                  ? 'bg-amber-500/20 border-amber-500 text-amber-300'
                  : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              Passed Through ({logs.filter((l) => !l.wasIntercepted).length})
            </button>
          </div>
        </div>
      )}

      {/* Logs Table / List */}
      {filteredLogs.length > 0 ? (
        <div className="space-y-2 max-h-96 overflow-y-auto pr-1">
          {filteredLogs.map((log) => (
            <div
              key={log.id}
              className={`p-3 rounded-xl border text-xs font-mono space-y-1.5 transition-all ${
                log.wasIntercepted
                  ? 'bg-emerald-950/20 border-emerald-500/30'
                  : 'bg-slate-950 border-slate-800'
              }`}
            >
              <div className="flex flex-wrap items-center justify-between gap-2 text-[11px]">
                <div className="flex items-center gap-2">
                  <span
                    className={`inline-flex items-center gap-1 px-2 py-0.5 rounded font-bold ${
                      log.wasIntercepted
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                        : 'bg-slate-800 text-slate-400 border border-slate-700'
                    }`}
                  >
                    {log.wasIntercepted ? (
                      <>
                        <CheckCircle className="w-3 h-3 text-emerald-400" />
                        <span>INTERCEPTED</span>
                      </>
                    ) : (
                      <>
                        <XCircle className="w-3 h-3 text-slate-400" />
                        <span>PASS-THROUGH</span>
                      </>
                    )}
                  </span>
                  <span className="text-sky-400 font-bold">{log.targetApi}</span>
                  {log.requestedParam !== 'NONE' && (
                    <span className="text-slate-400">({log.requestedParam})</span>
                  )}
                </div>

                <div className="flex items-center gap-2 text-slate-500">
                  <span>{new Date(log.timestamp).toLocaleTimeString()}</span>
                  <button
                    onClick={() => handleCopyLog(log)}
                    className="text-slate-400 hover:text-sky-300 p-1 rounded hover:bg-slate-800"
                    title="Copy payload"
                  >
                    {copiedId === log.id ? (
                      <Check className="w-3 h-3 text-emerald-400" />
                    ) : (
                      <Copy className="w-3 h-3" />
                    )}
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] pt-1">
                <div>
                  <span className="text-slate-500">Caller: </span>
                  <span className="text-slate-300">{log.callerPackage}</span>
                </div>
                <div>
                  <span className="text-slate-500">Returned: </span>
                  <span className="text-sky-300 font-bold break-all">{log.returnedValue}</span>
                </div>
              </div>

              <div className="text-[11px] text-slate-400 pt-0.5 border-t border-slate-800/60">
                <span className="text-slate-500">Reason: </span>
                <span>{log.reason}</span>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="p-8 rounded-xl bg-slate-950 border border-dashed border-slate-800 text-center text-xs text-slate-500">
          {logs.length === 0
            ? 'No interception logs recorded yet. Invoke methods above or launch Target Demo.'
            : 'No logs match your filter/search criteria.'}
        </div>
      )}
    </div>
  );
};
