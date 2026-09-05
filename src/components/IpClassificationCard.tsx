import React from 'react';
import { Network, ShieldAlert, CheckCircle2, XCircle, Info, ExternalLink, Terminal } from 'lucide-react';
import { IpTierItem } from '../types';

interface IpClassificationCardProps {
  syntheticIp: string;
}

export const IpClassificationCard: React.FC<IpClassificationCardProps> = ({ syntheticIp }) => {
  const ipTiers: IpTierItem[] = [
    {
      tier: 'Local Loopback IP',
      example: '127.0.0.1 / ::1',
      description: 'Host-only loopback interface used for intra-device process communication.',
      isSynthetic: false,
      modifiesNetworkEgress: false,
      publicGeolocatable: false
    },
    {
      tier: 'Private LAN / Wi-Fi IP',
      example: '192.168.1.145 / 10.0.0.22',
      description: 'Local area network address assigned to wlan0 / rmnet0 interface by router DHCP.',
      isSynthetic: false,
      modifiesNetworkEgress: false,
      publicGeolocatable: false
    },
    {
      tier: 'Synthetic Test IP (RFC 5737)',
      example: `${syntheticIp} (TEST-NET-3)`,
      description: 'Deterministic test address space reserved by IETF for documentation and simulation.',
      isSynthetic: true,
      modifiesNetworkEgress: false,
      publicGeolocatable: false
    },
    {
      tier: 'Actual Public Egress IP',
      example: 'e.g. 198.51.x.x / Carrier NAT / ISP IP',
      description: 'The real routable IP observed by external web servers during HTTP/TCP handshakes.',
      isSynthetic: false,
      modifiesNetworkEgress: true,
      publicGeolocatable: true
    }
  ];

  return (
    <div className="rounded-2xl bg-slate-900/90 border border-slate-800 p-6 space-y-6 shadow-xl relative overflow-hidden backdrop-blur-sm">
      <div className="flex items-center gap-3 border-b border-slate-800/80 pb-4">
        <div className="w-10 h-10 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
          <Network className="w-5 h-5" />
        </div>
        <div>
          <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
            IP Source Classification &amp; Network Egress Clarification
          </h2>
          <p className="text-xs text-slate-400">
            Clear taxonomy distinguishing synthetic test IP values from physical network interfaces and public routing
          </p>
        </div>
      </div>

      {/* Warning Card */}
      <div className="rounded-xl bg-amber-950/30 border border-amber-500/40 p-4 space-y-2 text-xs">
        <div className="flex items-center gap-2 text-amber-300 font-bold">
          <ShieldAlert className="w-4 h-4 text-amber-400 shrink-0" />
          <span>REALITY CHECK: Synthetic Test IP ≠ Actual Public IP</span>
        </div>
        <p className="text-amber-200/80 leading-relaxed text-[11px]">
          In mobile application security testing, setting a test location and synthetic IP allows verification that target
          application logic and analytics handlers correctly consume test profile data. However, bytecode hooks (NPatch/LSPosed)
          <strong> do not route physical packets</strong> or change your carrier/Wi-Fi router&apos;s egress NAT. Any claims that an
          Xposed hook alone changed your real public Internet IP are technically false.
        </p>
      </div>

      {/* Tier Breakdown Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs border-collapse">
          <thead>
            <tr className="border-b border-slate-800 text-slate-400 font-semibold uppercase text-[10px]">
              <th className="pb-3 pr-4">Network Tier</th>
              <th className="pb-3 px-4">Address / Scope</th>
              <th className="pb-3 px-4">Target Application Role</th>
              <th className="pb-3 px-4 text-center">Hook Modifiable?</th>
              <th className="pb-3 pl-4 text-center">Public Geolocation?</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60 font-mono">
            {ipTiers.map((tier) => (
              <tr key={tier.tier} className="hover:bg-slate-800/30 transition-colors">
                <td className="py-3 pr-4 font-sans font-semibold text-slate-200">{tier.tier}</td>
                <td className="py-3 px-4 text-cyan-300 font-mono">{tier.example}</td>
                <td className="py-3 px-4 font-sans text-slate-400 text-[11px] leading-relaxed max-w-xs">
                  {tier.description}
                </td>
                <td className="py-3 px-4 text-center">
                  {tier.isSynthetic ? (
                    <span className="inline-flex items-center gap-1 text-emerald-400 font-sans font-bold text-[11px]">
                      <CheckCircle2 className="w-3.5 h-3.5" /> YES (Simulated)
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-slate-500 font-sans text-[11px]">
                      <XCircle className="w-3.5 h-3.5" /> NO (OS / Hardware)
                    </span>
                  )}
                </td>
                <td className="py-3 pl-4 text-center">
                  {tier.publicGeolocatable ? (
                    <span className="inline-flex items-center gap-1 text-amber-400 font-sans font-bold text-[11px]">
                      YES (ISP / MaxMind)
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-slate-500 font-sans text-[11px]">
                      NO (RFC 5737 / Local)
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Standards Citation */}
      <div className="rounded-xl bg-slate-950/60 border border-slate-800 p-3.5 flex items-center justify-between text-xs text-slate-400">
        <div className="flex items-center gap-2">
          <Info className="w-4 h-4 text-cyan-400 shrink-0" />
          <span>
            Complies with <strong>IETF RFC 5737</strong> (IPv4 Address Blocks Reserved for Documentation and Simulation).
          </span>
        </div>
        <span className="font-mono text-[11px] text-cyan-400 bg-cyan-950/60 px-2 py-0.5 rounded border border-cyan-800/40">
          203.0.113.0/24 TEST-NET-3
        </span>
      </div>
    </div>
  );
};
