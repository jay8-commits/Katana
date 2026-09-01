import { HookInvocationLog } from '../types';

const STORAGE_KEY_INTERCEPTION_ACTIVE = 'device_id_lab_interception_active';
const STORAGE_KEY_ACTIVE_ANDROID_ID = 'device_id_lab_active_android_id';
const STORAGE_KEY_ACTIVE_TELEPHONY_ID = 'device_id_lab_active_telephony_id';
const STORAGE_KEY_CANARY_ACTIVE = 'device_id_lab_canary_active';

export class HookInterceptionBridge {
  private static instance: HookInterceptionBridge;
  private isInterceptionActive: boolean = true;
  private activeAndroidId: string = 'NPATCH_ANDROID_001';
  private activeTelephonyId: string = 'NPATCH_TELEPHONY_001';
  private isFrameworkCanaryActive: boolean = true;
  private invocationLogs: HookInvocationLog[] = [];
  private subscribers: Array<() => void> = [];
  private lastTargetReadAndroid: string = 'None';
  private lastTargetReadTelephony: string = 'None';
  private lastInterceptedAndroidId: string = 'None';
  private lastInterceptedTelephonyId: string = 'None';
  private targetProcessDetected: boolean = true;
  private targetPid: number = 28419;

  private constructor() {
    this.loadFromStorage();
  }

  public static getInstance(): HookInterceptionBridge {
    if (!HookInterceptionBridge.instance) {
      HookInterceptionBridge.instance = new HookInterceptionBridge();
    }
    return HookInterceptionBridge.instance;
  }

  private loadFromStorage(): void {
    try {
      const active = localStorage.getItem(STORAGE_KEY_INTERCEPTION_ACTIVE);
      if (active !== null) this.isInterceptionActive = active === 'true';

      const aId = localStorage.getItem(STORAGE_KEY_ACTIVE_ANDROID_ID);
      if (aId) this.activeAndroidId = aId;

      const tId = localStorage.getItem(STORAGE_KEY_ACTIVE_TELEPHONY_ID);
      if (tId) this.activeTelephonyId = tId;

      const canary = localStorage.getItem(STORAGE_KEY_CANARY_ACTIVE);
      if (canary !== null) this.isFrameworkCanaryActive = canary === 'true';
    } catch (e) {
      console.warn('Failed loading interception settings:', e);
    }
  }

  private saveToStorage(): void {
    try {
      localStorage.setItem(STORAGE_KEY_INTERCEPTION_ACTIVE, String(this.isInterceptionActive));
      localStorage.setItem(STORAGE_KEY_ACTIVE_ANDROID_ID, this.activeAndroidId);
      localStorage.setItem(STORAGE_KEY_ACTIVE_TELEPHONY_ID, this.activeTelephonyId);
      localStorage.setItem(STORAGE_KEY_CANARY_ACTIVE, String(this.isFrameworkCanaryActive));
    } catch (e) {
      console.warn('Failed saving interception settings:', e);
    }
  }

  public subscribe(callback: () => void): () => void {
    this.subscribers.push(callback);
    return () => {
      this.subscribers = this.subscribers.filter(cb => cb !== callback);
    };
  }

  private notify(): void {
    this.subscribers.forEach(cb => cb());
  }

  public getIsInterceptionActive(): boolean {
    return this.isInterceptionActive;
  }

  public setIsInterceptionActive(active: boolean): void {
    this.isInterceptionActive = active;
    this.saveToStorage();
    this.notify();
  }

  public getIsFrameworkCanaryActive(): boolean {
    return this.isFrameworkCanaryActive;
  }

  public setIsFrameworkCanaryActive(active: boolean): void {
    this.isFrameworkCanaryActive = active;
    this.saveToStorage();
    this.notify();
  }

  public getActiveAndroidId(): string {
    return this.activeAndroidId;
  }

  public getActiveTelephonyId(): string {
    return this.activeTelephonyId;
  }

  public setInjectedIds(androidId: string, telephonyId: string): void {
    this.activeAndroidId = androidId;
    this.activeTelephonyId = telephonyId;
    this.saveToStorage();
    this.notify();
  }

  public getLogs(): HookInvocationLog[] {
    return [...this.invocationLogs];
  }

  public clearLogs(): void {
    this.invocationLogs = [];
    this.notify();
  }

  public getStatusDiagnostics() {
    return {
      currentAndroidTestId: this.activeAndroidId,
      currentTelephonyTestId: this.activeTelephonyId,
      lastInterceptedAndroidId: this.lastInterceptedAndroidId,
      lastInterceptedTelephonyId: this.lastInterceptedTelephonyId,
      lastTargetReadAndroid: this.lastTargetReadAndroid,
      lastTargetReadTelephony: this.lastTargetReadTelephony,
      isTargetDetected: this.targetProcessDetected,
      targetPid: this.targetPid,
      isFrameworkHookActive: this.isFrameworkCanaryActive && this.isInterceptionActive,
    };
  }

  public recordTargetRead(androidId: string, telephonyId: string, pid?: number) {
    this.lastTargetReadAndroid = androidId;
    this.lastTargetReadTelephony = telephonyId;
    if (pid) this.targetPid = pid;
    this.targetProcessDetected = true;
    this.notify();
  }

  public logInvocation(log: Omit<HookInvocationLog, 'id' | 'timestamp'>): void {
    const fullLog: HookInvocationLog = {
      id: Date.now() + Math.random(),
      timestamp: Date.now(),
      ...log,
    };
    this.invocationLogs.unshift(fullLog);
    if (this.invocationLogs.length > 50) {
      this.invocationLogs.pop();
    }
    this.notify();
  }

  /**
   * Intercepts `Settings.Secure.getString(ContentResolver, name)`.
   * Replicates DeviceIdHookDemo.kt
   */
  public interceptSettingsSecureGetString(
    callerPackage: string,
    settingName: string,
    originalProvider: () => string
  ): string {
    const originalValue = originalProvider();
    const isTarget = callerPackage === 'com.example.deviceidlab' || callerPackage === 'com.example.targetdemo';

    if (!isTarget) {
      this.logInvocation({
        callerPackage,
        targetApi: 'Settings.Secure.getString()',
        requestedParam: settingName,
        returnedValue: originalValue || 'null',
        wasIntercepted: false,
        reason: `Caller package is not target (${callerPackage}). Passed through untouched.`,
      });
      return originalValue;
    }

    if (!this.isInterceptionActive) {
      this.logInvocation({
        callerPackage,
        targetApi: 'Settings.Secure.getString()',
        requestedParam: settingName,
        returnedValue: originalValue || 'null',
        wasIntercepted: false,
        reason: 'Interception layer is currently disabled in lab controller settings.',
      });
      return originalValue;
    }

    if (settingName.toLowerCase() === 'android_id') {
      const substituted = this.activeAndroidId;
      this.lastInterceptedAndroidId = substituted;
      this.logInvocation({
        callerPackage,
        targetApi: 'Settings.Secure.getString()',
        requestedParam: settingName,
        returnedValue: substituted,
        wasIntercepted: true,
        reason: `NPatch 1.0.7 Bytecode Hook substituted simulated test Android ID.`,
      });
      return substituted;
    }

    this.logInvocation({
      callerPackage,
      targetApi: 'Settings.Secure.getString()',
      requestedParam: settingName,
      returnedValue: originalValue || 'null',
      wasIntercepted: false,
      reason: `Setting key '${settingName}' is not ANDROID_ID. Original platform value returned.`,
    });
    return originalValue;
  }

  /**
   * Intercepts `TelephonyManager.getDeviceId()` / `TelephonyManager.getImei()`.
   * Replicates DeviceIdHookDemo.kt
   */
  public interceptTelephonyGetDeviceId(
    callerPackage: string,
    originalProvider: () => string
  ): string {
    const isTarget = callerPackage === 'com.example.deviceidlab' || callerPackage === 'com.example.targetdemo';
    const originalValue = originalProvider();

    if (!isTarget) {
      this.logInvocation({
        callerPackage,
        targetApi: 'TelephonyManager.getDeviceId()',
        requestedParam: 'NONE',
        returnedValue: originalValue,
        wasIntercepted: false,
        reason: `Caller package is not target (${callerPackage}). Passed through.`,
      });
      return originalValue;
    }

    if (!this.isInterceptionActive) {
      this.logInvocation({
        callerPackage,
        targetApi: 'TelephonyManager.getDeviceId()',
        requestedParam: 'NONE',
        returnedValue: originalValue,
        wasIntercepted: false,
        reason: 'Interception layer is currently disabled in settings.',
      });
      return originalValue;
    }

    const substituted = this.activeTelephonyId;
    this.lastInterceptedTelephonyId = substituted;
    this.logInvocation({
      callerPackage,
      targetApi: 'TelephonyManager.getDeviceId()',
      requestedParam: 'NONE',
      returnedValue: substituted,
      wasIntercepted: true,
      reason: `NPatch 1.0.7 substituted simulated test Telephony ID (bypassing restriction for test APK).`,
    });
    return substituted;
  }
}
