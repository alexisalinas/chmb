package com.banamex.cops.update_offer.kafka;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** Contadores simples del flujo (enviados, descartados, fallidos). */
@Component
public class KafkaMetrics {

    private final AtomicLong sent = new AtomicLong();
    private final AtomicLong noRows = new AtomicLong();
    private final AtomicLong invalid = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    public void incSent() { sent.incrementAndGet(); }
    public void incNoRows() { noRows.incrementAndGet(); }
    public void incInvalid() { invalid.incrementAndGet(); }
    public void incFailed() { failed.incrementAndGet(); }
    public void incRejected() { rejected.incrementAndGet(); }

    public long getSent() { return sent.get(); }
    public long getNoRows() { return noRows.get(); }
    public long getInvalid() { return invalid.get(); }
    public long getFailed() { return failed.get(); }
    public long getRejected() { return rejected.get(); }
}
