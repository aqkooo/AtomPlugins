package me.ejyqyl.tituls.model;

import me.ejyqyl.tituls.util.DateUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Titul {
    private final String id;
    private final String name;
    private final TitulType type;
    private final TitulRarity rarity;
    private Long obtainedAt;

    public Titul(@NotNull String id, @NotNull String name, @NotNull TitulType type, @NotNull TitulRarity rarity, @Nullable Long obtainedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.rarity = rarity;
        this.obtainedAt = obtainedAt;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public TitulType getType() {
        return type;
    }

    @NotNull
    public TitulRarity getRarity() {
        return rarity;
    }

    @Nullable
    public Long getObtainedAt() {
        return obtainedAt;
    }

    public void setObtainedAt(@Nullable Long obtainedAt) {
        this.obtainedAt = obtainedAt;
    }

    @NotNull
    public String getFormatted() {
        return DateUtil.format(obtainedAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Titul other)) return false;
        return Objects.equals(id, other.id) && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "Titul{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", rarity=" + rarity +
                ", obtainedAt=" + obtainedAt +
                '}';
    }
}
