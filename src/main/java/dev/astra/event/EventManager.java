package dev.astra.event;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventManager {
    private static final Map<Class<? extends Event>, List<EventListener>> listeners = new ConcurrentHashMap<>();

    public static void register(Object object) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class) && method.getParameterCount() == 1) {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(parameterType)) {
                    method.setAccessible(true);
                    try {
                        MethodHandle handle = lookup.unreflect(method);
                        Class<? extends Event> eventClass = parameterType.asSubclass(Event.class);
                        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(new EventListener(object, handle));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void unregister(Object object) {
        for (List<EventListener> list : listeners.values()) {
            synchronized (list) {
                list.removeIf(listener -> listener.instance == object);
            }
        }
    }

    public static void call(Event event) {
        List<EventListener> list = listeners.get(event.getClass());
        if (list != null) {
            synchronized (list) {
                for (EventListener listener : list) {
                    try {
                        listener.handle.invoke(listener.instance, event);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static class EventListener {
        private final Object instance;
        private final MethodHandle handle;
        public EventListener(Object instance, MethodHandle handle) {
            this.instance = instance;
            this.handle = handle;
        }
    }
}
