import { InjectionTestResult, NpatchVerificationDetails, RealIdResult } from '../types';
import { HookInterceptionBridge } from './hookInterceptionBridge';

// Stable hardware baseline for the device
const BASELINE_ANDROID_ID = '91d04b7e8fa3c2d1';
const BASELINE_TELEPHONY_ID = 'Restricted (Android 10+ requires READ_PRIVILEGED_PHONE_STATE)';

export class DeviceIdReader {
  /**
   * Generates or reads real browser/device fingerprints
   */
  public static getBrowserDeviceInfo(): {
    userAgent: string;
    platform: string;
    screenResolution: string;
    hardwareConcurrency: number;
    timezone: string;
    canvasHash: string;
    webglRenderer: string;
  } {
    const canvas = document.createElement('canvas');
    const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
    let webglRenderer = 'Unknown WebGL';
    if (gl) {
      const dbg = (gl as WebGLRenderingContext).getExtension('WEBGL_debug_renderer_info');
      if (dbg) {
        webglRenderer = (gl as WebGLRenderingContext).getParameter(dbg.UNMASKED_RENDERER_WEBGL) || 'Generic WebGL';
      }
    }

    // Canvas 2D fingerprint hash
    const ctx = canvas.getContext('2d');
    let canvasHash = 'c4b8e219';
    if (ctx) {
      canvas.width = 200;
      canvas.height = 50;
      ctx.textBaseline = 'top';
      ctx.font = '14px Arial';
      ctx.fillStyle = '#f60';
      ctx.fillRect(125, 1, 62, 20);
      ctx.fillStyle = '#069';
      ctx.fillText('DeviceIdLab, 2026', 2, 15);
      const dataUri = canvas.toDataURL();
      let hash = 0;
      for (let i = 0; i < dataUri.length; i++) {
        hash = (hash << 5) - hash + dataUri.charCodeAt(i);
        hash |= 0;
      }
      canvasHash = Math.abs(hash).toString(16).padStart(8, '0');
    }

    return {
      userAgent: navigator.userAgent || 'Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro)',
      platform: navigator.platform || 'Linux armv8l',
      screenResolution: `${window.screen?.width || 1080}x${window.screen?.height || 2400} @ ${window.devicePixelRatio || 2.5}x`,
      hardwareConcurrency: navigator.hardwareConcurrency || 8,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
      canvasHash,
      webglRenderer,
    };
  }

  /**
   * Reads Android ID through HookInterceptionBridge
   * Matches DeviceIdReader.readAndroidId(context)
   */
  public static readAndroidId(callerPackage: string = 'com.example.deviceidlab'): RealIdResult {
    const bridge = HookInterceptionBridge.getInstance();

    const intercepted = bridge.interceptSettingsSecureGetString(
      callerPackage,
      'android_id',
      () => BASELINE_ANDROID_ID
    );

    const isIntercepted = bridge.getIsInterceptionActive() && intercepted !== BASELINE_ANDROID_ID;

    return {
      value: intercepted,
      isRestricted: false,
      statusDetail: isIntercepted
        ? 'NPatch 1.0.7 Bytecode Intercepted (Dynamic Runtime IPC)'
        : 'Retrieved via Settings.Secure.ANDROID_ID (Hardware Baseline)',
    };
  }

  /**
   * Reads Telephony Device ID (IMEI) through HookInterceptionBridge
   * Matches DeviceIdReader.readTelephonyDeviceId(context)
   */
  public static readTelephonyDeviceId(callerPackage: string = 'com.example.deviceidlab'): RealIdResult {
    const bridge = HookInterceptionBridge.getInstance();

    const intercepted = bridge.interceptTelephonyGetDeviceId(
      callerPackage,
      () => BASELINE_TELEPHONY_ID
    );

    const isIntercepted = bridge.getIsInterceptionActive() && intercepted !== BASELINE_TELEPHONY_ID;

    if (isIntercepted) {
      return {
        value: intercepted,
        isRestricted: false,
        statusDetail: 'Retrieved via TelephonyManager (NPatch 1.0.7 Intercepted)',
      };
    }

    return {
      value: 'Restricted (SecurityException: READ_PRIVILEGED_PHONE_STATE required on Android 10+)',
      isRestricted: true,
      statusDetail: 'Android 10+ blocked IMEI access: READ_PRIVILEGED_PHONE_STATE restricted to system/carrier apps',
    };
  }

  /**
   * Verifies NPatch 1.0.7 injection status
   * Matches DeviceIdReader.verifyNpatchInjection(context, targetPackage)
   */
  public static verifyNpatchInjection(targetPackage: string): NpatchVerificationDetails {
    const bridge = HookInterceptionBridge.getInstance();
    const isCanaryActive = bridge.getIsFrameworkCanaryActive();
    const isInterceptionActive = bridge.getIsInterceptionActive();
    const isVerified = isCanaryActive && isInterceptionActive && (targetPackage === 'com.example.deviceidlab' || targetPackage === 'com.example.targetdemo');

    const finalResult = isVerified ? 'INJECTION VERIFIED' : 'INJECTION NOT DETECTED';

    return {
      moduleDetected: true,
      targetPackage: targetPackage || 'com.example.deviceidlab',
      targetProcess: `${targetPackage}:main`,
      hookEntryStatus: isVerified ? 'INITIALIZED (NPatchHookEntry)' : 'NOT_LOADED',
      hookInstallationStatus: isVerified ? 'SUCCESS (2/2 Methods Hooked)' : 'PENDING',
      canaryStatus: isCanaryActive ? 'ACTIVE (Canary Method Replaced)' : 'INACTIVE (Default Canary Unhooked)',
      lastHookTimestamp: Date.now(),
      finalResult,
      isVerified,
    };
  }

  /**
   * Performs real-time injection verification
   * Matches DeviceIdReader.performInjectionTest(context, targetRandomId, targetPackage)
   */
  public static performInjectionTest(
    targetRandomId: string,
    targetPackage: string = 'com.example.deviceidlab'
  ): InjectionTestResult {
    if (!targetRandomId || targetRandomId.trim() === '') {
      return {
        originalId: BASELINE_ANDROID_ID,
        currentId: '',
        injectedId: '',
        isSuccess: false,
        hookStatus: 'INVALID_INPUT',
        targetPackage,
        failureReason: 'Injected ID is empty. Please enter or generate a valid 16-character hex ID.',
        timestamp: Date.now(),
      };
    }

    const bridge = HookInterceptionBridge.getInstance();
    bridge.setInjectedIds(targetRandomId, bridge.getActiveTelephonyId());

    const isSelf = targetPackage === 'com.example.deviceidlab' || targetPackage === 'com.example.targetdemo';

    if (!isSelf) {
      return {
        originalId: 'Target Hardware Baseline',
        currentId: 'Unreachable (App not found)',
        injectedId: targetRandomId,
        isSuccess: false,
        hookStatus: 'NOT_INSTALLED',
        targetPackage,
        failureReason: `Target package '${targetPackage}' is not installed in current test suite sandbox.`,
        timestamp: Date.now(),
      };
    }

    const isRealHook = bridge.getIsFrameworkCanaryActive();
    const isInterceptionActive = bridge.getIsInterceptionActive();

    const currentId = isInterceptionActive ? targetRandomId : BASELINE_ANDROID_ID;
    const isSuccess = currentId === targetRandomId && isRealHook && isInterceptionActive;

    const hookStatus = isSuccess
      ? 'REAL_HOOK_SUCCESS'
      : !isRealHook
      ? 'TARGET_NOT_PATCHED'
      : !isInterceptionActive
      ? 'API_NOT_INTERCEPTED'
      : 'RETURN_MISMATCH';

    const failureReason = !isSuccess
      ? !isRealHook
        ? `Returned Android ID matches baseline '${BASELINE_ANDROID_ID}'. NPatch bytecode hook is not active. Ensure APK is patched with NPatch 1.0.7.`
        : !isInterceptionActive
        ? `Returned Android ID matches baseline '${BASELINE_ANDROID_ID}'. Interception toggle is disabled.`
        : `Returned Android ID '${currentId}' does not match target '${targetRandomId}'.`
      : null;

    return {
      originalId: BASELINE_ANDROID_ID,
      currentId,
      injectedId: targetRandomId,
      isSuccess,
      hookStatus,
      targetPackage,
      failureReason,
      timestamp: Date.now(),
    };
  }
}
