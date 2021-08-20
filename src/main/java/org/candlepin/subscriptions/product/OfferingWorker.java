/*
 * Copyright Red Hat, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Red Hat trademarks are not licensed under GPLv3. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.subscriptions.product;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.candlepin.subscriptions.db.model.Offering;
import org.candlepin.subscriptions.task.TaskQueueProperties;
import org.candlepin.subscriptions.util.KafkaConsumerRegistry;
import org.candlepin.subscriptions.util.SeekableKafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile("capacity-ingress")
public class OfferingWorker extends SeekableKafkaConsumer {

  private final Timer syncTimer;
  private final OfferingSyncController controller;

  @Autowired
  protected OfferingWorker(
      @Qualifier("offeringSyncTasks") TaskQueueProperties taskQueueProperties,
      KafkaConsumerRegistry kafkaConsumerRegistry,
      MeterRegistry meterRegistry,
      OfferingSyncController controller) {
    super(taskQueueProperties, kafkaConsumerRegistry);
    this.syncTimer = meterRegistry.timer("swatch_offering_sync");
    this.controller = controller;
  }

  @KafkaListener(
      id = "#{__listener.groupId}",
      topics = "#{__listener.topic}",
      containerFactory = "offeringSyncListenerContainerFactory")
  public void receive(OfferingSyncTask task) {
    String sku = task.getSku();
    log.info("Sync for offeringSku={} triggered by OfferingSyncTask", sku);
    Timer.Sample syncTime = Timer.start();

    Optional<Offering> upstream = controller.getUpstreamOffering(sku);
    upstream.ifPresentOrElse(
        controller::syncOffering,
        () -> log.warn("offeringSku={} was not found in upstream service.", sku));

    Duration syncDuration = Duration.ofNanos(syncTime.stop(syncTimer));
    log.info(
        "Fetched and synced offeringSku={} in offeringSyncedTimeMillis={}",
        sku,
        syncDuration.toMillis());
  }
}
