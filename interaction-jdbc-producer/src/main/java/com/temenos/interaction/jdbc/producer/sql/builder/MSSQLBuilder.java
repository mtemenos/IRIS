package com.temenos.interaction.jdbc.producer.sql.builder;

/*
 * #%L
 * interaction-jdbc-producer
 * %%
 * Copyright (C) 2012 - 2015 Temenos Holdings N.V.
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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.temenos.interaction.authorization.command.data.AccessProfile;
import com.temenos.interaction.authorization.command.data.OrderBy;
import com.temenos.interaction.jdbc.ServerMode;
import com.temenos.interaction.jdbc.producer.sql.ColumnTypesMap;
import com.temenos.interaction.jdbc.producer.sql.SqlBuilder;

/**
 * Implementation to build SQL Statement for MS SQL Server
 *
 * @author sjunejo
 *
 */
public class MSSQLBuilder extends SqlBuilder {

    private final static Logger logger = LoggerFactory.getLogger(MSSQLBuilder.class);

    /**
     * Constructor to build a SQL Server Command Builder
     */
    public MSSQLBuilder(String tableName, String keyValue, AccessProfile accessProfile, ColumnTypesMap colTypesMap,
            String top, String skip, List<OrderBy> orderBy) {
        super(tableName, keyValue, accessProfile, colTypesMap, top, skip, orderBy);
    }

    @Override
    public void setCompatibilityMode() {
        this.serverMode = ServerMode.MSSQL;
        this.serverIsEmulated = false;
    };

    @Override
    public String getCommand() {
        // Build SQL Server command
        StringBuilder builder = new StringBuilder("SELECT");
        addSelects(builder);
        addFromTerm(builder);
        addWhereTerms(builder);
        addOrderByTerms(builder);

        // Package the inner SQL command in an outer SQL command.
        addTopAndSkip(builder);

        return builder.toString();
    }

    /*
     * Add $top and $skip components for this server type.
     * 
     * This is messy. To support pagination an inner select is wrapped by an
     * outer select. For more information search online for "oracle pagination".
     */
    @Override
    protected void addTopAndSkip(StringBuilder builder) {
        if ((null == top) && (null == skip)) {
            // Nothing to do
            return;
        }

        // Add where clauses
        addSkip(builder);
        addTop(builder);
    }

    private void addSkip(StringBuilder builder) {
        int skipAsInt = 0;
        try {
            skipAsInt = skip == null ? SKIP_ROWS_DEFAULT : Integer.parseInt(skip);
        } catch (NumberFormatException nfe) {
            skipAsInt = SKIP_ROWS_DEFAULT;
            logger.warn("Invalid value provided to skip rows", nfe);
        }
        builder.append(" OFFSET " + skipAsInt + " ROWS");
    }

    private void addTop(StringBuilder builder) {
        int maxRow = 0;
        try {
            // Work out max row
            maxRow = top == null ? MAX_ROWS_DEFAULT : Integer.parseInt(top);
        } catch (NumberFormatException nfe) {
            maxRow = MAX_ROWS_DEFAULT;
            logger.warn("Invalid value provided to fetch top rows", nfe);
        }
        builder.append(" FETCH NEXT " + maxRow + " ROWS ONLY");
    }
}