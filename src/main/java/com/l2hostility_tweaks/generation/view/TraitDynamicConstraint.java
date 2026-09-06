package com.l2hostility_tweaks.generation.view;

import java.util.List;
import java.util.Objects;

public record TraitDynamicConstraint(String type, List<String> arguments) {
    public TraitDynamicConstraint {
        type = Objects.requireNonNull(type);
        arguments = List.copyOf(arguments);
    }
}
