export interface DeviceIdentity {
  identityNumber: number;
  androidTestId: string;
  telephonyTestId: string;
  createdAt: number;
}

export interface RealIdResult {
  value: string;
  isRestricted: boolean;
  statusDetail: string;
}

export interface InjectionTestResult {
  originalId: string;
  currentId: string;
  injectedId: string;
  isSuccess: boolean;
  hookStatus: 'REAL_HOOK_SUCCESS' | 'SIMULATION_ONLY' | 'TARGET_NOT_PATCHED' | 'API_NOT_INTERCEPTED' | 'RETURN_MISMATCH' | 'INVALID_INPUT' | 'NOT_INSTALLED';
  targetPackage: string;
  failureReason?: string | null;
  timestamp: number;
}

export interface NpatchVerificationDetails {
  moduleDetected: boolean;
  targetPackage: string;
  targetProcess: string;
  hookEntryStatus: string;
  hookInstallationStatus: string;
  canaryStatus: string;
  lastHookTimestamp: number;
  finalResult: string;
  isVerified: boolean;
}

export interface HookInvocationLog {
  id: number;
  timestamp: number;
  callerPackage: string;
  targetApi: string;
  requestedParam: string;
  returnedValue: string;
  wasIntercepted: boolean;
  reason: string;
}

export type GenerationResult =
  | { type: 'success'; identity: DeviceIdentity }
  | { type: 'pool_exhausted'; message: string }
  | { type: 'error'; message: string };

export interface TargetDemoQueryLog {
  timestamp: number;
  androidId: string;
  telephonyId: string;
  pid: number;
}
