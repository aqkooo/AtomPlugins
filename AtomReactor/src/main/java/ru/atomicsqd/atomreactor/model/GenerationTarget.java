package ru.atomicsqd.atomreactor.model;

/**
 * Defines who receives income from the reactor.
 */
public enum GenerationTarget {
    /**
     * Owner receives money globally anywhere on the server, provided they are online.
     */
    OWNER_GLOBAL,

    /**
     * Owner receives money ONLY if they are physically within the reactor's configured radius.
     */
    OWNER_IN_RADIUS,

    /**
     * All players standing within the reactor's radius receive income.
     */
    ALL_IN_RADIUS,

    /**
     * Income accumulates in the reactor's internal vault/storage and can be collected via the GUI.
     */
    INTERNAL_VAULT;

    public static GenerationTarget fromString(String name) {
        if (name == null) return OWNER_GLOBAL;
        try {
            return GenerationTarget.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return OWNER_GLOBAL;
        }
    }
}
