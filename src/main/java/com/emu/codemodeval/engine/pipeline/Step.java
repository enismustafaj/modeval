package com.emu.codemodeval.engine.pipeline;

/** One stage of the validation pipeline: reads what earlier steps left in the context and adds its own result. */
public interface Step {

    void run(PipelineContext ctx);
}
