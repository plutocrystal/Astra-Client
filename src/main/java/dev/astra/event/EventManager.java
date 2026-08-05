package dev.astra.event;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private static final Map<Class<? extends Event>, List<EventListener>> listeners = new ConcurrentHashMap<>();

    public static void register(Object object) {
        for (Method method : object.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHandler.class) && method.getParameterCount() == 1) {
                Class<?> parameterType = method.getParameterTypes()[0];
                if (Event.class.isAssignableFrom(parameterType)) {
                    method.setAccessible(true);
                    Class<? extends Event> eventClass = parameterType.asSubclass(Event.class);
                    listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(new EventListener(object, method));
                }
            }
        }
    }

    public static void unregister(Object object) {
        for (List<EventListener> list : listeners.values()) {
            list.removeIf(listener -> listener.instance == object);
        }
    }

    public static void call(Event event) {
        List<EventListener> list = listeners.get(event.getClass());
        if (list != null) {
            for (EventListener listener : list) {
                try {
                    listener.method.invoke(listener.instance, event);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class EventListener {
        private final Object instance;
        private final Method method;

        public EventListener(Object instance, Method method) {
            this.instance = instance;
            this.method = method;
        }
    }
}