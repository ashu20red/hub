package com.flightstats.datahub.replication;

import com.flightstats.datahub.app.config.GuiceContextListenerFactory;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.google.common.base.Optional;
import com.sun.jersey.api.client.Client;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * This is an integration test that relies on http://hub.svc.dev/channel/testy1 being actively updated.
 * It could be changed to fire up the DataHub locally and run against that, which may make more sense.
 */
public class ChannelUtilsTest {

    private static final String ROOT_URL = "http://hub.svc.dev/channel";
    private static final String CHANNEL_URL = ROOT_URL + "/testy1";
    private static final String NON_CHANNEL_URL = ROOT_URL + "/blahFoobar";
    private static ChannelUtils channelUtils;

    @BeforeClass
    public static void setupClass() throws Exception {
        Client followClient = GuiceContextListenerFactory.DatahubCommonModule.buildJerseyClient();
        Client noRedirectsClient = GuiceContextListenerFactory.DatahubCommonModule.buildJerseyClientNoRedirects();
        channelUtils = new ChannelUtils(noRedirectsClient, followClient);
    }

    @Test
    public void testGetLatestSequence() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(CHANNEL_URL);
        assertTrue(latestSequence.isPresent());
        System.out.println("latest " + latestSequence.get());
        assertTrue(latestSequence.get() > 213320);
    }

    @Test
    public void testGetLatestSequenceNoChannel() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(NON_CHANNEL_URL);
        assertFalse(latestSequence.isPresent());
    }

    @Test
    public void testGetConfiguration() throws Exception {
        Optional<ChannelConfiguration> configuration = channelUtils.getConfiguration(CHANNEL_URL);
        assertTrue(configuration.isPresent());
        assertEquals("testy1", configuration.get().getName());
        assertEquals(86400000L, (long) configuration.get().getTtlMillis());
    }

    @Test
    public void testGetConfigurationMissing() throws Exception {
        Optional<ChannelConfiguration> configuration = channelUtils.getConfiguration(NON_CHANNEL_URL);
        assertFalse(configuration.isPresent());
    }

    @Test
    public void testGetContent() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(CHANNEL_URL);
        Optional<Content> optionalContent = channelUtils.getContent(CHANNEL_URL, latestSequence.get());
        assertTrue(optionalContent.isPresent());
        Content content = optionalContent.get();
        assertTrue(content.getData().length > 0);
        assertTrue(new DateTime(content.getMillis()).isAfter(new DateTime().minusMinutes(5)));
    }

    @Test
    public void testGetCreationDate() throws Exception {
        Optional<Long> latestSequence = channelUtils.getLatestSequence(CHANNEL_URL);
        Optional<DateTime> optionalDate = channelUtils.getCreationDate(CHANNEL_URL, latestSequence.get());
        assertTrue(optionalDate.isPresent());
        DateTime dateTime = optionalDate.get();
        assertTrue(dateTime.isAfter(new DateTime().minusMinutes(5)));
    }

    @Test
    public void testGetChannels() throws Exception {
        Set<String> channels = channelUtils.getChannels(ROOT_URL);
        assertNotNull(channels);
        assertTrue(channels.size() > 10);
        assertTrue(channels.contains("http://hub.svc.dev/channel/testy10"));
    }

    @Test
    public void testGetChannelsSlash() throws Exception {
        Set<String> channels = channelUtils.getChannels(ROOT_URL + "/");
        assertNotNull(channels);
        assertTrue(channels.size() > 10);
    }

    @Test
    public void testNoChannels() throws Exception {
        Set<String> channels = channelUtils.getChannels("http://nothing.svc.dev/channel");
        assertNotNull(channels);
        assertTrue(channels.isEmpty());
    }

}
