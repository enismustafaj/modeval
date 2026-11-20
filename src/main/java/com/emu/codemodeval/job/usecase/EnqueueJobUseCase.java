package com.emu.codemodeval.job.usecase;

import com.emu.codemodeval.job.model.Job;
import com.emu.codemodeval.job.model.JobQueuedEvent;
import com.emu.codemodeval.job.persistence.JobRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnqueueJobUseCase {

    private final JobRepository repository;
    private final ApplicationEventPublisher events;

    /** Joins the caller's transaction, so a codemod and its job are stored together or not at all. */
    @Transactional
    public Job enqueue(UUID codemodId) {
        Job job = repository.save(com.emu.codemodeval.job.persistence.Job.queued(codemodId)).toDomain();
        events.publishEvent(new JobQueuedEvent(job.id()));
        return job;
    }
}
