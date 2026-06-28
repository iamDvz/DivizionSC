package ru.iamdvz.dscmeg.util;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class StateMachineSupport {

    private static final Map<Class<?>, CachedInvoker> CACHE = new ConcurrentHashMap<>();

    private StateMachineSupport() {
    }

    public static void playState(ActiveModel model, String stateName, double speed, Logger log) {
        if (stateName == null || stateName.isEmpty()) {
            return;
        }
        try {
            Object handler = invoke(model, "getStateMachineHandler");
            if (handler == null) {
                handler = invoke(model, "getAnimationStateMachine");
            }
            if (handler == null) {
                log.warning("[DSC_MEG] State machine handler not found for animation-state '" + stateName + "'");
                return;
            }
            CachedInvoker invoker = CACHE.computeIfAbsent(handler.getClass(), CachedInvoker::resolve);
            if (invoker == null || !invoker.invoke(handler, stateName, speed)) {
                log.warning("[DSC_MEG] Could not play animation-state '" + stateName + "'.");
            }
        } catch (Throwable t) {
            log.warning("[DSC_MEG] Failed to play animation-state '" + stateName + "': " + t.getMessage());
        }
    }

    private static Object invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private record CachedInvoker(Method withSpeed, Method stringOnly) {
        static CachedInvoker resolve(Class<?> handlerClass) {
            for (String name : new String[]{"setState", "playState", "transitionTo", "setAnimationState"}) {
                try {
                    Method withSpeed = handlerClass.getMethod(name, String.class, double.class);
                    return new CachedInvoker(withSpeed, null);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    Method stringOnly = handlerClass.getMethod(name, String.class);
                    return new CachedInvoker(null, stringOnly);
                } catch (NoSuchMethodException ignored) {
                }
            }
            return null;
        }

        boolean invoke(Object handler, String stateName, double speed) {
            try {
                if (withSpeed != null) {
                    withSpeed.invoke(handler, stateName, speed);
                    return true;
                }
                if (stringOnly != null) {
                    stringOnly.invoke(handler, stateName);
                    return true;
                }
            } catch (Throwable ignored) {
            }
            return false;
        }
    }
}
