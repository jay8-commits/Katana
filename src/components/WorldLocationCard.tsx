import React, { useState, useEffect } from 'react';
import {
  Globe,
  MapPin,
  Compass,
  Navigation,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Sparkles,
  Search,
  Shuffle,
  ShieldAlert,
  ArrowRight,
  Clock,
  Radio,
  Layers,
  Activity
} from 'lucide-react';
import { WorldLocationManager } from '../services/worldLocationManager';
import { WorldwideLocationProfile } from '../types';
import { CityRecord } from '../data/worldCities';

interface WorldLocationCardProps {
  onShowSnackbar: (msg: string) => void;
}

export const WorldLocationCard: React.FC<WorldLocationCardProps> = ({ onShowSnackbar }) => {
  const locationManager = WorldLocationManager.getInstance();

  const [activeProfile, setActiveProfile] = useState<WorldwideLocationProfile>(
    locationManager.getActiveProfile()
  );
  const [previousProfile, setPreviousProfile] = useState<WorldwideLocationProfile | null>(
    locationManager.getPreviousProfile()
  );

  // Selection mode: 'manual' | 'random_world' | 'random_country'
  const [selectionMode, setSelectionMode] = useState<'manual' | 'random_world' | 'random_country'>('manual');

  // Filter & Search states
  const [selectedRegion, setSelectedRegion] = useState<string>('All');
  const [selectedCountry, setSelectedCountry] = useState<string>('Japan');
  const [selectedCityId, setSelectedCityId] = useState<string>('as_tok');
  const [citySearchQuery, setCitySearchQuery] = useState<string>('');
  const [seedInput, setSeedInput] = useState<string>('42');

  const regions = ['All', ...locationManager.getAllRegions()];
  const countries = locationManager.getAllCountries();

  // Filter cities for manual selection
  const filteredCities = locationManager.getAllCities().filter((city) => {
    const matchesRegion = selectedRegion === 'All' || city.region === selectedRegion;
    const matchesSearch =
      citySearchQuery.trim() === '' ||
      city.city.toLowerCase().includes(citySearchQuery.toLowerCase()) ||
      city.country.toLowerCase().includes(citySearchQuery.toLowerCase()) ||
      city.region.toLowerCase().includes(citySearchQuery.toLowerCase());
    return matchesRegion && matchesSearch;
  });

  // Consistency validation
  const validation = locationManager.validateProfileConsistency(activeProfile);

  useEffect(() => {
    const unsubscribe = locationManager.subscribe(() => {
      setActiveProfile(locationManager.getActiveProfile());
      setPreviousProfile(locationManager.getPreviousProfile());
    });
    return () => unsubscribe();
  }, [locationManager]);

  const handleSelectManual = (cityId: string) => {
    setSelectedCityId(cityId);
    try {
      const profile = locationManager.selectManualCity(cityId);
      locationManager.activateProfile(profile);
      onShowSnackbar(`Activated Manual Location: ${profile.city}, ${profile.country}`);
    } catch (e: any) {
      onShowSnackbar(`Error: ${e.message}`);
    }
  };

  const handleRandomWorld = () => {
    const seed = seedInput ? parseInt(seedInput, 10) : undefined;
    const profile = locationManager.selectRandomWorldCity(seed);
    locationManager.activateProfile(profile);
    onShowSnackbar(`Random World City Activated: ${profile.city}, ${profile.country}`);
  };

  const handleRandomInCountry = () => {
    const seed = seedInput ? parseInt(seedInput, 10) : undefined;
    try {
      const profile = locationManager.selectRandomCityInCountry(selectedCountry, seed);
      locationManager.activateProfile(profile);
      onShowSnackbar(`Random City in ${selectedCountry}: ${profile.city}`);
    } catch (e: any) {
      onShowSnackbar(`Error: ${e.message}`);
    }
  };

  const handleQuickPreset = (cityId: string) => {
    try {
      const profile = locationManager.selectManualCity(cityId);
      locationManager.activateProfile(profile);
      setSelectedCityId(cityId);
      onShowSnackbar(`Applied Preset: ${profile.city}, ${profile.country}`);
    } catch (e: any) {
      onShowSnackbar(`Error: ${e.message}`);
    }
  };

  const handleResetDefault = () => {
    locationManager.clearProfile();
    setSelectedCityId('as_tok');
    onShowSnackbar('Location profile reset to default (Tokyo, Japan)');
  };

  return (
    <div className="rounded-2xl bg-slate-900/90 border border-slate-800 p-6 space-y-6 shadow-xl relative overflow-hidden backdrop-blur-sm">
      {/* Background Accent Gradient */}
      <div className="absolute top-0 right-0 w-96 h-96 bg-cyan-500/5 rounded-full blur-3xl pointer-events-none" />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
            <Globe className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              Worldwide Location &amp; Synthetic IP Subsystem
              <span className="px-2 py-0.5 text-[10px] font-semibold bg-cyan-500/15 border border-cyan-500/30 text-cyan-300 rounded-full">
                80+ CITIES / 9 REGIONS
              </span>
            </h2>
            <p className="text-xs text-slate-400">
              Coherent geographic profiles: City ↔ Country ↔ Coordinates ↔ Timezone ↔ RFC 5737 Synthetic Test IP
            </p>
          </div>
        </div>

        {/* Quick Reset */}
        <button
          onClick={handleResetDefault}
          className="flex items-center gap-2 px-3 py-1.5 text-xs font-medium text-slate-400 hover:text-slate-200 bg-slate-800/60 hover:bg-slate-800 border border-slate-700/60 rounded-xl transition-all"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Reset to Tokyo Default</span>
        </button>
      </div>

      {/* Active Profile Banner */}
      <div className="rounded-xl bg-slate-950/70 border border-cyan-500/30 p-5 space-y-4">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 border-b border-slate-800/80 pb-3">
          <div className="flex items-center gap-2.5">
            <MapPin className="w-5 h-5 text-cyan-400 shrink-0" />
            <div>
              <div className="text-sm font-semibold text-slate-200">
                ACTIVE TEST LOCATION:{' '}
                <span className="text-cyan-300 font-bold text-base">
                  {activeProfile.city}, {activeProfile.country}
                </span>{' '}
                <span className="text-slate-400 text-xs">({activeProfile.countryCode})</span>
              </div>
              <div className="text-xs text-slate-400 flex items-center gap-2 pt-0.5">
                <span>Region: <strong className="text-slate-300">{activeProfile.region}</strong></span>
                <span>•</span>
                <span>Profile ID: <code className="text-cyan-400 font-mono text-[11px]">{activeProfile.profileId}</code></span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span className="px-2.5 py-1 text-xs font-bold rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              STATUS: {activeProfile.state}
            </span>
            <span className={`px-2.5 py-1 text-xs font-bold rounded-lg border flex items-center gap-1.5 ${
              validation.isConsistent
                ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40'
                : 'bg-red-500/20 text-red-300 border-red-500/40'
            }`}>
              <CheckCircle2 className="w-3.5 h-3.5" />
              {validation.isConsistent ? 'PROFILE CONSISTENCY: PASS' : 'PROFILE CONSISTENCY: FAIL'}
            </span>
          </div>
        </div>

        {/* Profile Details Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 text-xs">
          <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800">
            <div className="text-slate-500 text-[10px] font-semibold uppercase">Latitude</div>
            <div className="font-mono text-cyan-300 font-bold text-sm">{activeProfile.latitude.toFixed(4)}°</div>
          </div>
          <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800">
            <div className="text-slate-500 text-[10px] font-semibold uppercase">Longitude</div>
            <div className="font-mono text-cyan-300 font-bold text-sm">{activeProfile.longitude.toFixed(4)}°</div>
          </div>
          <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800">
            <div className="text-slate-500 text-[10px] font-semibold uppercase">IANA Timezone</div>
            <div className="font-mono text-slate-200 font-semibold truncate">{activeProfile.timezone}</div>
          </div>
          <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800">
            <div className="text-slate-500 text-[10px] font-semibold uppercase">Synthetic Test IP</div>
            <div className="font-mono text-amber-300 font-bold truncate">{activeProfile.syntheticIp}</div>
            <div className="text-[9px] text-slate-500 font-mono">RFC 5737 TEST-NET-3</div>
          </div>
          <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800">
            <div className="text-slate-500 text-[10px] font-semibold uppercase">Sensor Accuracy</div>
            <div className="font-mono text-slate-200 font-semibold">{activeProfile.accuracy}m</div>
          </div>
          <div className="p-2.5 rounded-lg bg-slate-900/80 border border-slate-800">
            <div className="text-slate-500 text-[10px] font-semibold uppercase">Altitude / Speed</div>
            <div className="font-mono text-slate-200 font-semibold">{activeProfile.altitude}m / {activeProfile.speed}m/s</div>
          </div>
        </div>

        {/* IP Scope Notice & Verification Status */}
        <div className="rounded-lg bg-amber-950/20 border border-amber-500/30 p-3 text-xs text-amber-300/90 space-y-1">
          <div className="flex items-center gap-2 font-semibold text-amber-300">
            <ShieldAlert className="w-4 h-4 shrink-0" />
            <span>CRITICAL ARCHITECTURAL DISTINCTION: Synthetic IP vs. Actual Public IP</span>
          </div>
          <p className="text-[11px] text-amber-200/80 leading-relaxed">
            The <strong>Synthetic Test IP</strong> ({activeProfile.syntheticIp}) is a deterministic application-level simulation parameter (RFC 5737) stored in the test profile.
            It does <strong>NOT</strong> modify physical network egress, interface sockets, or actual public IP routing.
            Actual public IP is determined strictly by the physical Wi-Fi/cellular connection, carrier gateway, or active VPN.
          </p>
          <div className="pt-1 text-[11px] text-slate-400 flex flex-wrap items-center gap-x-4 gap-y-1">
            <span>PUBLIC INTERNET GEOLOCATION: <strong className="text-slate-300">NOT_MODIFIED</strong></span>
            <span>•</span>
            <span>PHYSICAL ANDROID RUNTIME: <strong className="text-slate-300">NOT_PERFORMED</strong></span>
            <span>•</span>
            <span>MULTI-PROFILE SWITCHING: <strong className="text-emerald-400">INSTANT (NO REPATCH)</strong></span>
          </div>
        </div>
      </div>

      {/* Quick Presets Bar */}
      <div className="space-y-2">
        <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center gap-1.5">
          <Sparkles className="w-3.5 h-3.5 text-cyan-400" />
          <span>Quick City Presets</span>
        </div>
        <div className="flex flex-wrap gap-2">
          {[
            { id: 'as_tok', name: 'Tokyo, Japan', flag: '🇯🇵' },
            { id: 'na_nyc', name: 'New York, US', flag: '🇺🇸' },
            { id: 'eu_lon', name: 'London, UK', flag: '🇬🇧' },
            { id: 'as_mnl', name: 'Manila, Philippines', flag: '🇵🇭' },
            { id: 'eu_par', name: 'Paris, France', flag: '🇫🇷' },
            { id: 'oc_syd', name: 'Sydney, Australia', flag: '🇦🇺' },
            { id: 'sa_sao', name: 'São Paulo, Brazil', flag: '🇧🇷' },
            { id: 'af_cai', name: 'Cairo, Egypt', flag: '🇪🇬' },
            { id: 'me_dxb', name: 'Dubai, UAE', flag: '🇦🇪' },
          ].map((preset) => (
            <button
              key={preset.id}
              onClick={() => handleQuickPreset(preset.id)}
              className={`px-3 py-1.5 text-xs font-medium rounded-xl border transition-all flex items-center gap-1.5 ${
                activeProfile.city.toLowerCase() === preset.name.split(',')[0].toLowerCase()
                  ? 'bg-cyan-500/20 border-cyan-500 text-cyan-200 shadow-sm'
                  : 'bg-slate-800/60 border-slate-700 text-slate-300 hover:bg-slate-800 hover:border-slate-600'
              }`}
            >
              <span>{preset.flag}</span>
              <span>{preset.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Mode Tabs */}
      <div className="border-b border-slate-800 flex items-center gap-2">
        <button
          onClick={() => setSelectionMode('manual')}
          className={`px-4 py-2.5 text-xs font-bold border-b-2 transition-all flex items-center gap-2 ${
            selectionMode === 'manual'
              ? 'border-cyan-400 text-cyan-300'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Search className="w-3.5 h-3.5" />
          <span>Mode 1: Manual City Selection</span>
        </button>

        <button
          onClick={() => setSelectionMode('random_world')}
          className={`px-4 py-2.5 text-xs font-bold border-b-2 transition-all flex items-center gap-2 ${
            selectionMode === 'random_world'
              ? 'border-cyan-400 text-cyan-300'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Shuffle className="w-3.5 h-3.5" />
          <span>Mode 2: Random World City</span>
        </button>

        <button
          onClick={() => setSelectionMode('random_country')}
          className={`px-4 py-2.5 text-xs font-bold border-b-2 transition-all flex items-center gap-2 ${
            selectionMode === 'random_country'
              ? 'border-cyan-400 text-cyan-300'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <Compass className="w-3.5 h-3.5" />
          <span>Mode 3: Random Country → City</span>
        </button>
      </div>

      {/* Mode 1: Manual City Selection View */}
      {selectionMode === 'manual' && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {/* Region Filter */}
            <div>
              <label className="block text-xs font-medium text-slate-400 mb-1">Filter by Region</label>
              <select
                value={selectedRegion}
                onChange={(e) => setSelectedRegion(e.target.value)}
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
              >
                {regions.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </div>

            {/* City Search */}
            <div className="sm:col-span-2">
              <label className="block text-xs font-medium text-slate-400 mb-1">Search City / Country</label>
              <div className="relative">
                <input
                  type="text"
                  placeholder="e.g. Tokyo, London, Singapore, Brazil..."
                  value={citySearchQuery}
                  onChange={(e) => setCitySearchQuery(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 pl-9 text-xs text-slate-200 placeholder:text-slate-600 focus:outline-none focus:border-cyan-500"
                />
                <Search className="w-4 h-4 text-slate-500 absolute left-3 top-2.5" />
              </div>
            </div>
          </div>

          {/* City Catalog Grid */}
          <div className="max-h-64 overflow-y-auto pr-1 space-y-1.5 scrollbar-thin scrollbar-thumb-slate-700">
            {filteredCities.map((c) => {
              const isSelected = activeProfile.city.toLowerCase() === c.city.toLowerCase();
              return (
                <div
                  key={c.id}
                  onClick={() => handleSelectManual(c.id)}
                  className={`p-3 rounded-xl border flex items-center justify-between gap-3 cursor-pointer transition-all ${
                    isSelected
                      ? 'bg-cyan-950/40 border-cyan-500/80 text-cyan-200'
                      : 'bg-slate-950/50 border-slate-800/80 hover:bg-slate-800/60 hover:border-slate-700 text-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-slate-900 border border-slate-800 flex items-center justify-center text-xs font-bold text-slate-400">
                      {c.countryCode}
                    </div>
                    <div>
                      <div className="font-semibold text-xs flex items-center gap-2">
                        <span>{c.city}</span>
                        <span className="text-slate-400 font-normal">({c.country})</span>
                        <span className="px-1.5 py-0.2 rounded text-[10px] bg-slate-800 text-slate-400">{c.region}</span>
                      </div>
                      <div className="text-[11px] font-mono text-slate-500 flex items-center gap-3">
                        <span>Lat: {c.latitude.toFixed(4)}°</span>
                        <span>Lng: {c.longitude.toFixed(4)}°</span>
                        <span>TZ: {c.timezone}</span>
                      </div>
                    </div>
                  </div>

                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleSelectManual(c.id);
                    }}
                    className={`px-3 py-1 text-xs font-medium rounded-lg border transition-all ${
                      isSelected
                        ? 'bg-cyan-500 text-slate-950 font-bold border-cyan-400'
                        : 'bg-slate-800 text-slate-300 border-slate-700 hover:bg-cyan-600 hover:text-white'
                    }`}
                  >
                    {isSelected ? 'Active' : 'Select & Activate'}
                  </button>
                </div>
              );
            })}
            {filteredCities.length === 0 && (
              <div className="text-center py-8 text-xs text-slate-500">
                No cities found matching &quot;{citySearchQuery}&quot; in region {selectedRegion}.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Mode 2: Random World City View */}
      {selectionMode === 'random_world' && (
        <div className="space-y-4 rounded-xl bg-slate-950/60 border border-slate-800 p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <h3 className="text-xs font-bold text-slate-200 flex items-center gap-2">
                <Shuffle className="w-4 h-4 text-cyan-400" />
                Random Global City Selection
              </h3>
              <p className="text-[11px] text-slate-400">
                Draws a random city from the 80+ worldwide database and allocates a unique RFC 5737 synthetic test IP.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <div>
                <label className="block text-[10px] text-slate-500 font-mono uppercase">Deterministic Seed (Optional)</label>
                <input
                  type="number"
                  value={seedInput}
                  onChange={(e) => setSeedInput(e.target.value)}
                  placeholder="e.g. 42"
                  className="w-24 bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 font-mono focus:outline-none focus:border-cyan-500"
                />
              </div>

              <button
                onClick={handleRandomWorld}
                className="px-4 py-2 text-xs font-bold rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 flex items-center gap-2 transition-all mt-4 sm:mt-0"
              >
                <Shuffle className="w-3.5 h-3.5" />
                <span>Draw &amp; Activate City</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Mode 3: Random Country → City View */}
      {selectionMode === 'random_country' && (
        <div className="space-y-4 rounded-xl bg-slate-950/60 border border-slate-800 p-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="space-y-1">
              <h3 className="text-xs font-bold text-slate-200 flex items-center gap-2">
                <Compass className="w-4 h-4 text-cyan-400" />
                Random City Within Target Country
              </h3>
              <p className="text-[11px] text-slate-400">
                Choose a country, then pick a random city within that country.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <div>
                <label className="block text-[10px] text-slate-500 font-mono uppercase">Target Country</label>
                <select
                  value={selectedCountry}
                  onChange={(e) => setSelectedCountry(e.target.value)}
                  className="bg-slate-900 border border-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
                >
                  {countries.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>

              <button
                onClick={handleRandomInCountry}
                className="px-4 py-2 text-xs font-bold rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 flex items-center gap-2 transition-all mt-4 sm:mt-0"
              >
                <Shuffle className="w-3.5 h-3.5" />
                <span>Pick City in {selectedCountry}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Multi-Profile Lifecycle & Transition Card */}
      <div className="rounded-xl bg-slate-950/50 border border-slate-800/80 p-4 space-y-3">
        <div className="flex items-center justify-between text-xs font-semibold text-slate-400">
          <span className="flex items-center gap-1.5">
            <Activity className="w-4 h-4 text-cyan-400" />
            Profile Lifecycle &amp; Multi-Profile Transition State
          </span>
          <span className="text-[10px] text-slate-500">DYNAMIC IPC - NO APP RESTART REQUIRED</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
          {/* Active Profile */}
          <div className="p-3 rounded-lg bg-slate-900/90 border border-emerald-500/40 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-emerald-400 uppercase tracking-wide">Current Active Profile</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-300">ACTIVE</span>
            </div>
            <div className="font-semibold text-slate-200">
              {activeProfile.city}, {activeProfile.country}
            </div>
            <div className="font-mono text-[11px] text-slate-400">
              Coords: {activeProfile.latitude.toFixed(4)}, {activeProfile.longitude.toFixed(4)} | IP: {activeProfile.syntheticIp}
            </div>
          </div>

          {/* Previously Consumed Profile */}
          <div className="p-3 rounded-lg bg-slate-900/90 border border-slate-800 space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wide">Previous Profile</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-400">
                {previousProfile ? previousProfile.state : 'NONE'}
              </span>
            </div>
            {previousProfile ? (
              <>
                <div className="font-semibold text-slate-400">
                  {previousProfile.city}, {previousProfile.country}
                </div>
                <div className="font-mono text-[11px] text-slate-500">
                  Coords: {previousProfile.latitude.toFixed(4)}, {previousProfile.longitude.toFixed(4)} | IP: {previousProfile.syntheticIp}
                </div>
              </>
            ) : (
              <div className="text-slate-600 text-xs italic">No profile consumed yet. Switch profiles to record state transition.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
