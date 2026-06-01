package io.yourPath.utils;

import java.util.Objects;

public class Resolution {
    public final int width;
    public final int height;

    public Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public String label() {
        return width + "x" + height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resolution)) return false;
        Resolution r = (Resolution) o;
        return width == r.width && height == r.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }
}
