package com.emu.codemodeval.engine.steps.dependentrepos;

import com.emu.codemodeval.codemod.model.Ecosystem;
import com.emu.codemodeval.engine.steps.dependentrepos.model.DependentRepo;
import java.util.List;

/** A package index that knows which repos depend on a package. */
public interface DependentsIndex {

    List<DependentRepo> dependents(Ecosystem ecosystem, String packageName);
}
