package ru.iamdvz.divizionsc.platform;

/**
 * Дескриптор запланированной задачи, не зависящий от платформы (Paper/Folia).
 */
public interface TaskHandle {

    TaskHandle CANCELLED = new TaskHandle() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };

    void cancel();

    boolean isCancelled();
}
