package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import me.prettyprint.cassandra.serializers.StringSerializer;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import me.prettyprint.hector.api.query.ColumnQuery;
import me.prettyprint.hector.api.query.QueryResult;

import java.util.Collection;

import static com.flightstats.datahub.dao.CassandraChannelsCollection.DATA_HUB_COLUMN_FAMILY_NAME;

/**
 * This class encapsulates access to the payload item linkages.
 */
public class CassandraLinkagesCollection {

	private final static String NEXT_SUFFIX = "_next";
	private final static String PREVIOUS_SUFFIX = "_previous";

	private final CassandraConnector connector;
	private final HectorFactoryWrapper hector;
	private final DataHubKeyRenderer keyRenderer;
	private final RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy;

	@Inject
	public CassandraLinkagesCollection(CassandraConnector connector, HectorFactoryWrapper hector, DataHubKeyRenderer keyRenderer, RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> rowKeyStrategy) {
		this.connector = connector;
		this.hector = hector;
		this.keyRenderer = keyRenderer;
		this.rowKeyStrategy = rowKeyStrategy;
	}

	public void updateLinkages(String channelName, DataHubKey insertedKey, DataHubKey lastUpdatedKey) {
		if (lastUpdatedKey == null) {        // First thing being inserted into this channel, no latest, so eject!  No linkages yet.
			return;
		}

		StringSerializer keySerializer = StringSerializer.get();

		String insertedKeyString = keyRenderer.keyToString(insertedKey);
		String lastUpdatedKeyString = keyRenderer.keyToString(lastUpdatedKey);

		Mutator<String> mutator = connector.buildMutator(keySerializer);
		String lastUpdatedRowKey = rowKeyStrategy.buildKey(channelName, lastUpdatedKey);
		String insertedRowKey = rowKeyStrategy.buildKey(channelName, insertedKey);

		insertPreviousLinkage(insertedKeyString, lastUpdatedKeyString, mutator, insertedRowKey);
		insertNextLinage(insertedKeyString, lastUpdatedKeyString, mutator, lastUpdatedRowKey);
		mutator.execute();
	}

	public void delete(String channelName, Collection<DataHubKey> keys) {
		Mutator<String> mutator = connector.buildMutator(StringSerializer.get());
		for (DataHubKey key : keys) {
			String rowKey = rowKeyStrategy.buildKey(channelName, key);
			String columnKey = keyRenderer.keyToString(key);
			String previousRowKey = buildPreviousRowKey(rowKey);
			String nextRowKey = buildNextRowKey(rowKey);
			mutator.addDeletion(nextRowKey, DATA_HUB_COLUMN_FAMILY_NAME, columnKey, StringSerializer.get());
			mutator.addDeletion(previousRowKey, DATA_HUB_COLUMN_FAMILY_NAME, columnKey, StringSerializer.get());
		}
		mutator.execute();
	}

	private void insertPreviousLinkage(String insertedKeyString, String lastUpdatedKeyString, Mutator<String> mutator, String rowKey) {
		HColumn<String, String> keyToPreviousColumn = hector.createColumn(insertedKeyString, lastUpdatedKeyString, StringSerializer.get(),
				StringSerializer.get());
		String previousRowKey = buildPreviousRowKey(rowKey);
		mutator.addInsertion(previousRowKey, DATA_HUB_COLUMN_FAMILY_NAME, keyToPreviousColumn);
	}

	private void insertNextLinage(String insertedKeyString, String lastUpdatedKeyString, Mutator<String> mutator, String rowKey) {
		HColumn<String, String> keyToNextColumn = hector.createColumn(lastUpdatedKeyString, insertedKeyString, StringSerializer.get(),
				StringSerializer.get());
		String nextRowKey = buildNextRowKey(rowKey);
		mutator.addInsertion(nextRowKey, DATA_HUB_COLUMN_FAMILY_NAME, keyToNextColumn);
	}

	private String buildPreviousRowKey(String rowKey) {
		return rowKey + PREVIOUS_SUFFIX;
	}

	private String buildNextRowKey(String rowKey) {
		return rowKey + NEXT_SUFFIX;
	}

	public Optional<DataHubKey> findPreviousKey(String channelName, DataHubKey key) {
		String previousRowKey = buildPreviousRowKey(rowKeyStrategy.buildKey(channelName, key));
		return queryLinkage(previousRowKey, key);
	}

	public Optional<DataHubKey> findNextKey(String channelName, DataHubKey key) {
		String nextRowKey = buildNextRowKey(rowKeyStrategy.buildKey(channelName, key));
		return queryLinkage(nextRowKey, key);
	}

	private Optional<DataHubKey> queryLinkage(String rowKey, DataHubKey key) {
		Keyspace keyspace = connector.getKeyspace();
		ColumnQuery<String, String, String> rawQuery = hector.createColumnQuery(keyspace, StringSerializer.get(), StringSerializer.get(),
				StringSerializer.get());
		ColumnQuery<String, String, String> columnQuery = rawQuery
				.setKey(rowKey)
				.setColumnFamily(DATA_HUB_COLUMN_FAMILY_NAME)
				.setName(keyRenderer.keyToString(key));
		QueryResult<HColumn<String, String>> result = columnQuery.execute();
		HColumn<String, String> column = result.get();
		if (column == null) {
			return Optional.absent();
		}
		return Optional.of(keyRenderer.fromString(column.getValue()));
	}

	public boolean isLinkageRowKey(String key) {
		return key.endsWith(NEXT_SUFFIX) || key.endsWith(PREVIOUS_SUFFIX);
	}
}
