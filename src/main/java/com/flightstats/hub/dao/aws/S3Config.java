package com.flightstats.hub.dao.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class S3Config {
    private final static Logger logger = LoggerFactory.getLogger(S3Config.class);

    private final AmazonS3 s3Client;
    private final CuratorLock curatorLock;
    private final Dao<ChannelConfig> channelConfigDao;
    private final String s3BucketName;
    private ChannelService channelService;

    @Inject
    public S3Config(AmazonS3 s3Client, S3BucketName s3BucketName, CuratorLock curatorLock,
                    @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao, ChannelService channelService) {
        this.s3Client = s3Client;
        this.curatorLock = curatorLock;
        this.channelConfigDao = channelConfigDao;
        this.channelService = channelService;
        this.s3BucketName = s3BucketName.getS3BucketName();
        HubServices.register(new S3ConfigInit());
    }

    private void run() {
        try {
            doWork();
        } catch (Exception e) {
            logger.warn("unable to update config", e);
        }
    }

    private void doWork() {
        logger.info("starting work");
        Iterable<ChannelConfig> channels = channelConfigDao.getAll(false);
        S3ConfigLockable lockable = new S3ConfigLockable(channels);
        curatorLock.runWithLock(lockable, "/S3ConfigLock", 1, TimeUnit.MINUTES);
    }

    private class S3ConfigInit extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            Random random = new Random();
            long minutes = TimeUnit.HOURS.toMinutes(6);
            long delayMinutes = minutes + (long) random.nextInt((int) minutes);
            logger.info("scheduling S3Config with delay " + delayMinutes);
            return Scheduler.newFixedDelaySchedule(0, delayMinutes, TimeUnit.MINUTES);
        }

    }

    private class S3ConfigLockable implements Lockable {
        final Iterable<ChannelConfig> configurations;

        private S3ConfigLockable(Iterable<ChannelConfig> configurations) {
            this.configurations = configurations;
        }

        @Override
        public void runWithLock() throws Exception {
            updateTtlDays();
            updateMaxItems();
        }

        private void updateMaxItems() {
            logger.info("updating max items");
            for (ChannelConfig config : configurations) {
                if (config.getMaxItems() > 0 && !config.getKeepForever()) {
                    updateMaxItems(config);
                }
            }
        }

        private void updateMaxItems(ChannelConfig config) {
            String name = config.getDisplayName();
            logger.info("updating max items for channel {}", name);
            ActiveTraces.start("S3Config.updateMaxItems", name);
            Optional<ContentKey> optional = channelService.getLatest(name, false);
            if (optional.isPresent()) {
                ContentKey latest = optional.get();
                if (latest.getTime().isAfter(TimeUtil.now().minusDays(1))) {
                    updateMaxItems(config, latest);
                }
            }
            ActiveTraces.end();
            logger.info("completed max items for channel {}", name);

        }

        private void updateMaxItems(ChannelConfig config, ContentKey latest) {
            SortedSet<ContentKey> keys = new TreeSet<>();
            keys.add(latest);
            String name = config.getDisplayName();
            DirectionQuery query = DirectionQuery.builder()
                    .channelName(name)
                    .startKey(latest)
                    .next(false)
                    .stable(false)
                    .earliestTime(config.getTtlTime())
                    .count((int) (config.getMaxItems() - 1))
                    .build();
            keys.addAll(channelService.query(query));
            if (keys.size() == config.getMaxItems()) {
                ContentKey limitKey = keys.first();
                logger.info("deleting keys before {}", limitKey);
                channelService.deleteBefore(name, limitKey);
            }
        }

        private void updateTtlDays() {
            logger.info("updateTtlDays");
            ActiveTraces.start("S3Config.updateTtlDays");
            int maxRules = HubProperties.getProperty("s3.maxRules", 1000);
            List<BucketLifecycleConfiguration.Rule> rules = new ArrayList<>();
            if (maxRules == 0) {
                rules.add(new BucketLifecycleConfiguration.Rule()
                        .withId("OneDay")
                        .withExpirationInDays(1)
                        .withStatus(BucketLifecycleConfiguration.ENABLED));
            } else {
                rules = S3ConfigStrategy.apportion(configurations, new DateTime(), maxRules);
            }
            logger.info("updating {} rules with ttl life cycle ", rules.size());
            logger.trace("updating {} ", rules);

            if (!rules.isEmpty()) {
                BucketLifecycleConfiguration lifecycleConfig = new BucketLifecycleConfiguration(rules);
                s3Client.setBucketLifecycleConfiguration(s3BucketName, lifecycleConfig);
            }
            ActiveTraces.end();
        }
    }

}
