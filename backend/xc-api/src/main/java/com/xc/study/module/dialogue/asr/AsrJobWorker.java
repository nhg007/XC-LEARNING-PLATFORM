package com.xc.study.module.dialogue.asr;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.asr", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
public class AsrJobWorker {

    private final AsrJobProcessor asrJobProcessor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AsrJobWorker(AsrJobProcessor asrJobProcessor) {
        this.asrJobProcessor = asrJobProcessor;
    }

    @Scheduled(
            initialDelayString = "${app.asr.initial-delay-ms:5000}",
            fixedDelayString = "${app.asr.poll-delay-ms:5000}"
    )
    public void processPendingJobs() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            asrJobProcessor.processPendingBatch();
        } finally {
            running.set(false);
        }
    }
}
