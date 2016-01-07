package com.temenos.interaction.jdbc.producer;

/*
 * JDBC producer class.
 * 
 * This wraps a spring Jdbc template. Its called with OData parameters and recovers the requested information from a SQL server.
 */

/*
 * #%L
 * interaction-jdbc-producer
 * %%
 * Copyright (C) 2012 - 2013 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.odata4j.producer.EntityQueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jndi.JndiTemplate;

import com.temenos.interaction.authorization.command.data.AccessProfile;
import com.temenos.interaction.authorization.command.data.FieldName;
import com.temenos.interaction.authorization.command.data.OrderBy;
import com.temenos.interaction.authorization.command.data.RowFilter;
import com.temenos.interaction.authorization.command.util.ODataParser;
import com.temenos.interaction.authorization.command.util.ODataParser.UnsupportedQueryOperationException;
import com.temenos.interaction.core.command.InteractionContext;
import com.temenos.interaction.core.entity.Entity;
import com.temenos.interaction.core.entity.EntityProperties;
import com.temenos.interaction.core.entity.EntityProperty;
import com.temenos.interaction.core.resource.CollectionResource;
import com.temenos.interaction.core.resource.EntityResource;
import com.temenos.interaction.jdbc.ServerMode;
import com.temenos.interaction.jdbc.exceptions.JdbcException;
import com.temenos.interaction.jdbc.producer.sql.ColumnTypesMap;
import com.temenos.interaction.jdbc.producer.sql.SqlBuilder;
import com.temenos.interaction.jdbc.producer.sql.SqlBuilderFactory;

public class JdbcProducer {
    // Somewhere to store connection
    private JdbcTemplate template;

    // Current compatibility mode.
    private ServerMode serverMode;
    private ServerMode h2ServerMode = null;

    private final static Logger logger = LoggerFactory.getLogger(JdbcProducer.class);

    /*
     * Constructor called when a DataSource object to be obtained from Jndi.
     */
    public JdbcProducer(JndiTemplate jndiTemplate, String dataSourceName) throws ClassNotFoundException, JdbcException,
            NamingException {
        this((DataSource) jndiTemplate.lookup(dataSourceName));
    }

    /*
     * Constructor called when a DataSource object is available.
     */
    public JdbcProducer(DataSource dataSource) throws ClassNotFoundException, JdbcException {
        template = new JdbcTemplate(dataSource);
        serverMode = getServerMode();
    }

    /*
     * Constructor USED IN TESTING.
     * 
     * When using H2 there does not appear to be any way to read the server's
     * current compatibility mode. So enable the test to pass this as a
     * parameter. If it turns out that this information can be read from the
     * server remove this constructor and add the read to getServerMode().
     */
    public JdbcProducer(DataSource dataSource, ServerMode h2ServerMode) throws ClassNotFoundException, JdbcException {
        template = new JdbcTemplate(dataSource);
        this.h2ServerMode = h2ServerMode;
        serverMode = getServerMode();
    }

    /*
     * Query method for raw SQL commands
     */
    public SqlRowSet query(String command) {
        return template.queryForRowSet(command);
    }

    /*
     * Query method for interaction context parameters returning collection of
     * entities.
     */
    public CollectionResource<Entity> queryEntities(String tableName, InteractionContext ctx, String returnEntityType)
            throws UnsupportedQueryOperationException, JdbcException, Exception {
        SqlRowSet rowSet = query(tableName, null, ctx);
        return buildCollectionResource(returnEntityType, rowSet);
    }

    /*
     * Query method for interaction context parameters returning a single
     * entity.
     */
    public EntityResource<Entity> queryEntity(String tableName, String key, InteractionContext ctx,
            String returnEntityType) throws UnsupportedQueryOperationException, JdbcException, Exception {
        SqlRowSet rowSet = query(tableName, key, ctx);
        return createEntityResource(returnEntityType, rowSet);
    }

    /*
     * Query method for interaction context parameters returning raw sql data.
     * 
     * If given a key will return a single row.
     * 
     * If given a null key will return all rows.
     */
    public SqlRowSet query(String tableName, String key, InteractionContext ctx)
            throws UnsupportedQueryOperationException, JdbcException, Exception {
        // Not much point selecting from a null table
        if (null == tableName) {
            logger.error("Jdbc producer cannot select from null table.");
            throw (new JdbcException(Status.INTERNAL_SERVER_ERROR, "Null table name"));
        }

        // Get column types from Jdbc. We need these both for constructing the
        // command and processing it's result set.
        // We need the primary key for row ordering.
        // TODO Eventually this should be cached.
        ColumnTypesMap colTypesMap = new ColumnTypesMap(this, tableName, true);

        // Unpack the commands $filter and $select terms.
        AccessProfile accessProfile = getAccessProfile(ctx);

        // Get top and skip parameters (null if not specified).
        MultivaluedMap<String, String> queryParams = ctx.getQueryParameters();
        String top = queryParams.getFirst(ODataParser.TOP_KEY);
        String skip = queryParams.getFirst(ODataParser.SKIP_KEY);

        List<OrderBy> orderBy = ODataParser.parseOrderBy(queryParams.getFirst(ODataParser.ORDERBY_KEY));

        // Build an SQL command from an appropiate builder
        SqlBuilder sqlBuilder = SqlBuilderFactory.getSqlBuilder(tableName, key, accessProfile, colTypesMap, top, skip,
                orderBy, serverMode);
        String sqlCommand = sqlBuilder.getCommand();

        logger.info("Jdbc producer about to execute \"" + sqlCommand + "\"");

        // Execute the SQL command
        return query(sqlCommand);
    }

    public DataSource getDataSource() {
        return template.getDataSource();
    }

    /*
     * Method to unpack a contexts $filter and $select terms. For now use the
     * parser from authorization module.
     * 
     * TODO At some point the parser should probably be moved into it's own
     * module.
     */
    private AccessProfile getAccessProfile(InteractionContext ctx) throws UnsupportedQueryOperationException {
        EntityQueryInfo queryInfo = ODataParser.getEntityQueryInfo(ctx);
        List<RowFilter> filters = ODataParser.parseFilter(queryInfo.filter);
        Set<FieldName> selects = ODataParser.parseSelect(queryInfo.select);
        return new AccessProfile(filters, selects);
    }

    /*
     * Convert result to a single entry.
     */
    EntityResource<Entity> createEntityResource(String entityType, SqlRowSet rowSet) throws JdbcException {

        // Extract the returned column names. May be a subset of the ones
        // requested.
        String[] columnNames = rowSet.getMetaData().getColumnNames();

        // Set cursor to first row
        if (!rowSet.next()) {
            throw (new JdbcException(Status.NOT_FOUND, "Row not found. Entry with given key possibly not present."));
        }

        // Build up properties for this row
        EntityProperties properties = new EntityProperties();

        // For all columns in this row.
        for (String columnName : columnNames) {
            Object value = rowSet.getObject(columnName);

            // Only return non null values
            if (null != value) {
                // Add object to the property. getObject() returns an object
                // with the correct java type for each sql type. So we don't
                // need to cast.
                properties.setProperty(new EntityProperty(columnName, value));
            }
        }

        // Make an entity
        Entity entity = new Entity(entityType, properties);

        // Make an entity resource
        EntityResource<Entity> entityResource = new EntityResource<Entity>(entityType, entity);

        // Check for additional rows. Not expected for a 'single'
        // command.
        if (rowSet.next()) {
            throw (new JdbcException(Status.INTERNAL_SERVER_ERROR, "Multiple rows returned for a single entity"));
        }

        return entityResource;
    }

    /*
     * Convert result set into a collection of entities.
     */
    private CollectionResource<Entity> buildCollectionResource(String entityType, SqlRowSet rowSet) {
        List<EntityResource<Entity>> results = new ArrayList<EntityResource<Entity>>();

        // Extract the returned column names. May be a subset of the ones
        // requested.
        String[] columnNames = rowSet.getMetaData().getColumnNames();

        // For all rows returned add an entity to the collection.
        while (rowSet.next()) {
            EntityProperties properties = new EntityProperties();

            // For all columns in this row.
            for (String columnName : columnNames) {
                Object value = rowSet.getObject(columnName);

                // Only return non null values
                if (null != value) {
                    // Add object to the property. getObject() returns an object
                    // with the correct java type for each sql type. So we don't
                    // need to cast.
                    properties.setProperty(new EntityProperty(columnName, value));
                }
            }

            // Create entity.
            // Note: Despite the variable name the first arg of both these is
            // the entity type name. Not it's key.
            Entity entity = new Entity(entityType, properties);
            results.add(new EntityResource<Entity>(entity.getName(), entity));
        }

        // Note: This line looks a bit odd but the {} at the end is required.
        return new CollectionResource<Entity>(results) {
        };
    }

    /*
     * Utility to work out the current server mode.
     * 
     * This is probably untestable.
     */
    private ServerMode getServerMode() throws JdbcException {
        // If a server compatability mode has been passed use it.
        if (null != h2ServerMode) {
            return h2ServerMode;
        }

        // Look for real servers
        String url = null;

        // Get the connection URL
        Connection connection = null;
        try {
            connection = template.getDataSource().getConnection();
        } catch (SQLException ex) {
            throw (new JdbcException(Status.INTERNAL_SERVER_ERROR, "Could get connection to datasource. ", ex));
        }

        try {
            url = connection.getMetaData().getURL();
        } catch (SQLException ex) {
            throw (new JdbcException(Status.INTERNAL_SERVER_ERROR, "Could not get server URL. ", ex));
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                throw (new JdbcException(Status.INTERNAL_SERVER_ERROR, "Could not close connection to datasource. ", ex));
            }
        }

        // Extract server type from URL
        String[] tokens = url.split(":");

        if (tokens != null && tokens.length > 1 && tokens[1] != null) {
            String serverType = tokens[1];
            if ("oracle".equals(serverType)) {
                return ServerMode.ORACLE;
            }

            if ("mssql".equals(serverType) || "sqlserver".equals(serverType)) {
                return ServerMode.MSSQL;
            }

            if ("h2".equals(serverType)) {
                logger.warn("Running under H2 but no server compatibility mode specified. Defaulting to emulated MSSQL mode.");
                return ServerMode.H2_MSSQL;
            }

            throw (new JdbcException(Status.PRECONDITION_FAILED, "JDBC Server type \"" + serverType
                    + "\" not supported."));
        }

        throw (new JdbcException(Status.INTERNAL_SERVER_ERROR,
                "Failed to detect JDBC server type from connection URL \"" + url.toString() + "\"."));
    }
}
